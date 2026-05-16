package com.keenon.peanut.supermarket.vision;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.util.Log;

import org.tensorflow.lite.Interpreter;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

/**
 * On-device object detector built on the base TFLite Java Interpreter.
 * Supports EfficientDet-Lite0/1/2 and SSD MobileNetV3 — input size and
 * output tensor shapes are read from the loaded model at runtime so no
 * constants need changing when the model file changes.
 */
public class ObjectDetectorHelper {

  private static final String TAG = "ObjDetHelper";
  private static final String MODEL_PATH = "models/efficientdet_lite0.tflite";

  private static final int MAX_RESULTS = 10;

  // Lower threshold so both COCO and Open Images V7 (which outputs lower scores) are detected.
  private static final float SCORE_THRESHOLD           = 0.35f;
  private static final float SCORE_THRESHOLD_FULL_FRAME = 0.28f;

  // Custom ROI crop fractions — set from UI sliders (0.0 = no crop, 1.0 = full crop).
  // Default: full frame (no crop on any side).
  private float roiLeft   = 0f;
  private float roiTop    = 0f;
  private float roiRight  = 0f;
  private float roiBottom = 0f;

  // 1 frame to confirm = immediate appearance; 2 frames to vanish = fast removal without flicker.
  private static final int CONFIRM_FRAMES = 1;
  private static final int VANISH_FRAMES  = 2;

  // Label aliases: the model often calls a phone "tv" because both are flat black rectangles.
  private static final Map<String, String> LABEL_ALIASES = new HashMap<>();
  static {
    LABEL_ALIASES.put("tv",     "cell phone");
    LABEL_ALIASES.put("laptop", "cell phone");
  }

  // Fallback COCO 90-entry label map used when the model has no embedded metadata labels.
  private static final String[] COCO_LABELS_FALLBACK = {
      "person", "bicycle", "car", "motorcycle", "airplane", "bus", "train", "truck", "boat",
      "traffic light", "fire hydrant", "???", "stop sign", "parking meter", "bench", "bird", "cat",
      "dog", "horse", "sheep", "cow", "elephant", "bear", "zebra", "giraffe", "???", "backpack",
      "umbrella", "???", "???", "handbag", "tie", "suitcase", "frisbee", "skis", "snowboard",
      "sports ball", "kite", "baseball bat", "baseball glove", "skateboard", "surfboard",
      "tennis racket", "bottle", "???", "wine glass", "cup", "fork", "knife", "spoon", "bowl",
      "banana", "apple", "sandwich", "orange", "broccoli", "carrot", "hot dog", "pizza", "donut",
      "cake", "chair", "couch", "potted plant", "bed", "???", "dining table", "???", "???",
      "toilet", "???", "tv", "laptop", "mouse", "remote", "keyboard", "cell phone", "microwave",
      "oven", "toaster", "sink", "refrigerator", "???", "book", "clock", "vase", "scissors",
      "teddy bear", "hair drier", "toothbrush"
  };

  private final Context appContext;
  private final String modelPath;
  private Interpreter interpreter;
  private boolean loadFailed;

  // Labels loaded from model metadata (Open Images V7 = 600 labels; COCO = 90).
  // Falls back to COCO_LABELS_FALLBACK if metadata extraction fails.
  private String[] runtimeLabels = COCO_LABELS_FALLBACK;

  // Input/output dimensions — set after model load by introspecting tensor shapes.
  private int inputSize    = 320;  // fallback; overwritten in ensureLoaded()
  private int numDetections = 25;  // fallback; overwritten in ensureLoaded()

  // I/O buffers — allocated after load when we know the real sizes.
  private ByteBuffer   inputBuffer;
  private int[]        inputPixels;
  private float[][][]  outLocations;
  private float[][]    outClasses;
  private float[][]    outScores;
  private float[]      outCount;

  // ---- Temporal smoothing state ----
  private final Map<String, Integer> candidateFrames  = new LinkedHashMap<>();
  private final Map<String, Integer> vanishCounters   = new LinkedHashMap<>();
  private final Map<String, Float>   confirmedLabels  = new LinkedHashMap<>();
  private final Map<String, Integer> confirmedCounts  = new LinkedHashMap<>();
  private final Map<String, Float>   confirmedCenterX = new LinkedHashMap<>();

  public ObjectDetectorHelper(Context context) {
    this(context, MODEL_PATH);
  }

  public ObjectDetectorHelper(Context context, String modelPath) {
    this.appContext = context.getApplicationContext();
    this.modelPath  = modelPath;
  }

  /** Set custom ROI crop. Each value is the fraction to remove from that edge (0.0–0.49). */
  public synchronized void setRoi(float left, float top, float right, float bottom) {
    roiLeft   = Math.max(0f, Math.min(0.49f, left));
    roiTop    = Math.max(0f, Math.min(0.49f, top));
    roiRight  = Math.max(0f, Math.min(0.49f, right));
    roiBottom = Math.max(0f, Math.min(0.49f, bottom));
  }

  public synchronized boolean ensureLoaded() {
    if (interpreter != null) return true;
    if (loadFailed) return false;
    try {
      MappedByteBuffer model = loadModel(appContext, modelPath);
      Interpreter.Options opts = new Interpreter.Options();
      opts.setNumThreads(2);
      interpreter = new Interpreter(model, opts);

      // --- Read actual input tensor shape: [1, H, W, 3] ---
      int[] inShape = interpreter.getInputTensor(0).shape();
      // inShape is [batch, height, width, channels]
      inputSize = (inShape != null && inShape.length >= 3) ? inShape[1] : 320;

      // --- Read actual output tensor shapes ---
      // EfficientDet-Lite outputs: 0=locations[1,N,4], 1=classes[1,N], 2=scores[1,N], 3=count[1]
      // SSD MobileNet outputs the same layout but N may differ.
      int[] locShape = interpreter.getOutputTensor(0).shape(); // [1, N, 4]
      numDetections = (locShape != null && locShape.length >= 2) ? locShape[1] : 25;

      Log.i(TAG, "Loaded " + modelPath + " — inputSize=" + inputSize
              + " numDetections=" + numDetections);

      // Allocate buffers sized for this specific model.
      inputBuffer  = ByteBuffer.allocateDirect(inputSize * inputSize * 3);
      inputBuffer.order(ByteOrder.nativeOrder());
      inputPixels  = new int[inputSize * inputSize];
      outLocations = new float[1][numDetections][4];
      outClasses   = new float[1][numDetections];
      outScores    = new float[1][numDetections];
      outCount     = new float[1];

      // Extract label map from model metadata (works for both COCO and Open Images V7).
      runtimeLabels = loadLabelsFromMetadata(appContext, modelPath);
      Log.i(TAG, "Labels loaded: " + runtimeLabels.length + " classes from " + modelPath);

      return true;
    } catch (Throwable t) {
      loadFailed = true;
      Log.e(TAG, "Failed to load " + modelPath, t);
      return false;
    }
  }

  /**
   * Detect objects in an NV21 frame, restricted to the centre ROI (supermarket tray).
   * Returns the temporally-smoothed stable detection list (no flicker).
   */
  public List<Result> detect(byte[] nv21, int width, int height, int rotationDegrees) {
    return detect(nv21, width, height, rotationDegrees, true);
  }

  /**
   * @param useCenterTrayRoi when {@code true}, apply the user-defined ROI crop (tray focus).
   *                         when {@code false}, use the full frame — for person search on camera 3.
   */
  public List<Result> detect(byte[] nv21, int width, int height, int rotationDegrees,
                             boolean useCenterTrayRoi) {
    if (!ensureLoaded()) return Collections.emptyList();

    Bitmap full = nv21ToBitmap(nv21, width, height, rotationDegrees);
    if (full == null) return Collections.emptyList();

    final Bitmap roiBmp;
    if (useCenterTrayRoi) {
      roiBmp = cropRegion(full, roiLeft, roiTop, roiRight, roiBottom);
      if (roiBmp != full) full.recycle();
    } else {
      roiBmp = full;
    }
    if (roiBmp == null) return Collections.emptyList();

    float minScore = useCenterTrayRoi ? SCORE_THRESHOLD : SCORE_THRESHOLD_FULL_FRAME;

    Bitmap resized = null;
    try {
      resized = Bitmap.createScaledBitmap(roiBmp, inputSize, inputSize, true);
      fillInputBuffer(resized);

      Object[] inputs = new Object[]{inputBuffer};
      Map<Integer, Object> outputs = new HashMap<>();
      outputs.put(0, outLocations);
      outputs.put(1, outClasses);
      outputs.put(2, outScores);
      outputs.put(3, outCount);

      interpreter.runForMultipleInputsOutputs(inputs, outputs);

      int valid = Math.min((int) outCount[0], numDetections);
      Map<String, Integer> frameCounts  = new LinkedHashMap<>();
      Map<String, Float>   frameScores  = new LinkedHashMap<>();
      Map<String, Float>   frameCenterX = new LinkedHashMap<>();
      for (int i = 0; i < valid; i++) {
        float score = outScores[0][i];
        if (score < minScore) continue;
        int classIdx = (int) outClasses[0][i];
        if (classIdx < 0 || classIdx >= runtimeLabels.length) continue;
        String rawLabel = runtimeLabels[classIdx];
        if (rawLabel == null || rawLabel.isEmpty() || "???".equals(rawLabel)) continue;
        String label = LABEL_ALIASES.containsKey(rawLabel) ? LABEL_ALIASES.get(rawLabel) : rawLabel;
        Integer prevCount = frameCounts.get(label);
        frameCounts.put(label, prevCount == null ? 1 : prevCount + 1);
        Float prevScore = frameScores.get(label);
        if (prevScore == null || score > prevScore) frameScores.put(label, score);
        // outLocations format: [top, left, bottom, right] normalised 0..1
        float cx = (outLocations[0][i][1] + outLocations[0][i][3]) / 2f;
        Float prevCx = frameCenterX.get(label);
        frameCenterX.put(label, prevCx == null ? cx : prevCx + cx);
      }
      // Average centerX across multiple detections of same label
      for (String lbl : frameCenterX.keySet()) {
        Integer cnt = frameCounts.get(lbl);
        if (cnt != null && cnt > 1) frameCenterX.put(lbl, frameCenterX.get(lbl) / cnt);
      }

      updateSmoothing(frameCounts, frameScores, frameCenterX);

      List<Result> results = new ArrayList<>();
      for (Map.Entry<String, Integer> e : confirmedCounts.entrySet()) {
        String label = e.getKey();
        float score = confirmedLabels.containsKey(label) ? confirmedLabels.get(label) : 0f;
        float cx = confirmedCenterX.containsKey(label) ? confirmedCenterX.get(label) : -1f;
        int count = e.getValue();
        for (int n = 0; n < count && results.size() < MAX_RESULTS; n++) {
          results.add(new Result(label, score, cx));
        }
      }
      return results;

    } catch (Throwable t) {
      Log.w(TAG, "detect() failed", t);
      return Collections.emptyList();
    } finally {
      if (resized != null && resized != roiBmp) resized.recycle();
      if (roiBmp != null) roiBmp.recycle();
    }
  }

  /** Reset temporal smoothing state without unloading the model. */
  public synchronized void resetSmoothing() {
    candidateFrames.clear();
    vanishCounters.clear();
    confirmedLabels.clear();
    confirmedCounts.clear();
    confirmedCenterX.clear();
  }

  public synchronized void close() {
    if (interpreter != null) {
      try { interpreter.close(); } catch (Throwable ignored) {}
      interpreter = null;
    }
    loadFailed    = false;
    runtimeLabels = COCO_LABELS_FALLBACK;
    candidateFrames.clear();
    vanishCounters.clear();
    confirmedLabels.clear();
    confirmedCounts.clear();
    confirmedCenterX.clear();
  }

  // ---------- temporal smoothing ----------

  private void updateSmoothing(Map<String, Integer> frameCounts, Map<String, Float> frameScores,
                               Map<String, Float> frameCenterX) {
    for (Map.Entry<String, Integer> e : frameCounts.entrySet()) {
      String label = e.getKey();
      int frameCount = e.getValue();
      float score = frameScores.containsKey(label) ? frameScores.get(label) : 0f;
      vanishCounters.remove(label);

      float cx = frameCenterX.containsKey(label) ? frameCenterX.get(label) : -1f;
      if (confirmedLabels.containsKey(label)) {
        confirmedLabels.put(label, score);
        confirmedCounts.put(label, frameCount);
        if (cx >= 0) confirmedCenterX.put(label, cx);
      } else {
        Integer cnt = candidateFrames.get(label);
        int newCnt = (cnt == null ? 0 : cnt) + 1;
        candidateFrames.put(label, newCnt);
        if (newCnt >= CONFIRM_FRAMES) {
          confirmedLabels.put(label, score);
          confirmedCounts.put(label, frameCount);
          if (cx >= 0) confirmedCenterX.put(label, cx);
          candidateFrames.remove(label);
          Log.d(TAG, "Confirmed: " + label + " x" + frameCount + " @ " + (int)(score * 100) + "%");
        }
      }
    }

    List<String> toCheck = new ArrayList<>(confirmedLabels.keySet());
    for (String label : toCheck) {
      if (!frameCounts.containsKey(label)) {
        Integer gone = vanishCounters.get(label);
        int newGone = (gone == null ? 0 : gone) + 1;
        vanishCounters.put(label, newGone);
        if (newGone >= VANISH_FRAMES) {
          confirmedLabels.remove(label);
          confirmedCounts.remove(label);
          confirmedCenterX.remove(label);
          vanishCounters.remove(label);
          Log.d(TAG, "Removed: " + label);
        }
      }
    }

    List<String> staleCandidates = new ArrayList<>(candidateFrames.keySet());
    for (String label : staleCandidates) {
      if (!frameCounts.containsKey(label)) candidateFrames.remove(label);
    }
  }

  // ---------- helpers ----------

  /** Crop src by removing the given fraction from each edge. Returns src unchanged if all zeros. */
  private static Bitmap cropRegion(Bitmap src, float left, float top, float right, float bottom) {
    if (left == 0f && top == 0f && right == 0f && bottom == 0f) return src;
    int sw = src.getWidth(), sh = src.getHeight();
    int x0 = Math.round(sw * left);
    int y0 = Math.round(sh * top);
    int x1 = sw - Math.round(sw * right);
    int y1 = sh - Math.round(sh * bottom);
    int cw = x1 - x0, ch = y1 - y0;
    if (cw <= 0 || ch <= 0) return src;
    return Bitmap.createBitmap(src, x0, y0, cw, ch);
  }

  private void fillInputBuffer(Bitmap bmp) {
    inputBuffer.rewind();
    bmp.getPixels(inputPixels, 0, inputSize, 0, 0, inputSize, inputSize);
    for (int p : inputPixels) {
      inputBuffer.put((byte) ((p >> 16) & 0xFF)); // R
      inputBuffer.put((byte) ((p >> 8)  & 0xFF)); // G
      inputBuffer.put((byte) ( p        & 0xFF)); // B
    }
  }

  private static MappedByteBuffer loadModel(Context ctx, String assetPath) throws IOException {
    AssetFileDescriptor fd = ctx.getAssets().openFd(assetPath);
    try (FileInputStream in = new FileInputStream(fd.getFileDescriptor())) {
      FileChannel ch = in.getChannel();
      return ch.map(FileChannel.MapMode.READ_ONLY, fd.getStartOffset(), fd.getDeclaredLength());
    } finally {
      fd.close();
    }
  }

  private static Bitmap nv21ToBitmap(byte[] nv21, int width, int height, int rotation) {
    try {
      YuvImage yuv = new YuvImage(nv21, ImageFormat.NV21, width, height, null);
      ByteArrayOutputStream out = new ByteArrayOutputStream(nv21.length / 4);
      yuv.compressToJpeg(new Rect(0, 0, width, height), 85, out);
      byte[] jpeg = out.toByteArray();
      Bitmap bmp = BitmapFactory.decodeByteArray(jpeg, 0, jpeg.length);
      if (bmp == null) return null;
      if (rotation != 0) {
        Matrix m = new Matrix();
        m.postRotate(rotation);
        Bitmap rotated = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), m, true);
        if (rotated != bmp) bmp.recycle();
        return rotated;
      }
      return bmp;
    } catch (Throwable t) {
      Log.w(TAG, "nv21ToBitmap failed", t);
      return null;
    }
  }

  /**
   * Reads the label list embedded in the TFLite model file.
   *
   * TFLite models with metadata are ZIP archives. Assets are not seekable when read via
   * getAssets().open(), so we copy the file to internal storage first, then use ZipFile
   * (which is random-access) to reliably read the embedded .txt label file.
   *
   * Works for EfficientDet-Lite COCO (90 labels) and Open Images V7 (601 labels).
   * Falls back to COCO_LABELS_FALLBACK if the model has no embedded labels.
   */
  private static String[] loadLabelsFromMetadata(Context ctx, String assetPath) {
    // Copy asset to a temp file so ZipFile can seek it
    File tmpFile = null;
    try {
      String safeName = assetPath.replace('/', '_');
      tmpFile = new File(ctx.getCacheDir(), safeName + ".zip_tmp");
      if (!tmpFile.exists() || tmpFile.length() == 0) {
        try (InputStream in = ctx.getAssets().open(assetPath);
             FileOutputStream out = new FileOutputStream(tmpFile)) {
          byte[] buf = new byte[65536];
          int n;
          while ((n = in.read(buf)) != -1) out.write(buf, 0, n);
        }
      }
      // Now open as ZipFile (random-access, reliable)
      try (ZipFile zip = new ZipFile(tmpFile)) {
        java.util.Enumeration<? extends ZipEntry> entries = zip.entries();
        while (entries.hasMoreElements()) {
          ZipEntry entry = entries.nextElement();
          String name = entry.getName();
          if (name.endsWith(".txt")) {
            Log.d(TAG, "Found label file in model ZIP: " + name);
            List<String> labels = new ArrayList<>();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(zip.getInputStream(entry)))) {
              String line;
              while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                labels.add(trimmed.isEmpty() ? "???" : trimmed);
              }
            }
            if (!labels.isEmpty()) {
              Log.i(TAG, "Loaded " + labels.size() + " labels from " + name + " in " + assetPath);
              return labels.toArray(new String[0]);
            }
          }
        }
      }
      Log.w(TAG, "No .txt label file found in " + assetPath + " — using COCO fallback");
      return COCO_LABELS_FALLBACK;
    } catch (Throwable t) {
      Log.w(TAG, "loadLabelsFromMetadata failed for " + assetPath + " — using COCO fallback", t);
      return COCO_LABELS_FALLBACK;
    }
  }

  /** One detected object — label, confidence, and normalised bbox centre X in [0..1]. */
  public static final class Result {
    public final String label;
    public final float  score;
    /** Normalised horizontal centre of the bounding box (0 = left edge, 1 = right edge). -1 if unknown. */
    public final float  centerX;

    public Result(String label, float score) {
      this.label = label; this.score = score; this.centerX = -1f;
    }

    public Result(String label, float score, float centerX) {
      this.label = label; this.score = score; this.centerX = centerX;
    }
  }
}
