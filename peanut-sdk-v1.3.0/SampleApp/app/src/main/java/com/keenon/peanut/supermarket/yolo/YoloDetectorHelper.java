package com.keenon.peanut.supermarket.yolo;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import com.keenon.sdk.base.model.KNBox;
import com.keenon.sdk.yolo.KNYoloExecutor;

import java.io.File;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Wraps the KeenonDiner NCNN YOLOv5 pipeline (via KNYoloExecutor shim) to run
 * object detection on JPEG frames.
 *
 * Two model modes:
 *   TRAY  — canpan_detection (2 classes: ros_empty, plate). For robot tray cameras.
 *   COCO  — YOLOv5s-COCO (80 classes: phone, cup, bottle, person, etc.). For general detection.
 *
 * Switch via setMode(). Model files live in assets/yolo/ and are copied on first use.
 * For COCO mode, place yolov5s.param and yolov5s.bin in assets/yolo/ (see README).
 */
public final class YoloDetectorHelper {

    private static final String TAG = "YoloDetectorHelper";

    public enum Mode { TRAY, COCO }

    // ---- Tray model (KeenonDiner canpan_detection) ----
    private static final String TRAY_PARAM_ASSET = "yolo/canpan_detection_v1.1.5-0-g0000000.param";
    private static final String TRAY_BIN_ASSET   = "yolo/canpan_detection_v1.1.5-0-g0000000.bin";
    /** KeenonDiner {@code KNModelEnum.YOLO_MODEL_PLATE} — must match production slot 0. */
    private static final int    TRAY_MODEL_TYPE  = 0;
    private static final int    TRAY_MODEL_SIZE  = 5;
    private static final double TRAY_CONF_THRESH = 0.55;
    private static final String[] TRAY_LABELS    = {"ros_empty", "plate"};

    // ---- COCO model (YOLOv5s exported to NCNN) ----
    // Place yolov5s.param + yolov5s.bin in assets/yolo/ to enable this mode.
    // Download from: https://github.com/nihui/ncnn-assets/tree/master/models
    private static final String COCO_PARAM_ASSET = "yolo/yolov5s.param";
    private static final String COCO_BIN_ASSET   = "yolo/yolov5s.bin";
    private static final int    COCO_MODEL_TYPE  = 26;  // different slot to avoid clash with tray model
    private static final int    COCO_MODEL_SIZE  = 28;
    private static final double COCO_CONF_THRESH = 0.45;
    public static final String[] COCO_LABELS = {
        "person","bicycle","car","motorcycle","airplane","bus","train","truck","boat",
        "traffic light","fire hydrant","stop sign","parking meter","bench","bird","cat",
        "dog","horse","sheep","cow","elephant","bear","zebra","giraffe","backpack",
        "umbrella","handbag","tie","suitcase","frisbee","skis","snowboard","sports ball",
        "kite","baseball bat","baseball glove","skateboard","surfboard","tennis racket",
        "bottle","wine glass","cup","fork","knife","spoon","bowl","banana","apple",
        "sandwich","orange","broccoli","carrot","hot dog","pizza","donut","cake","chair",
        "couch","potted plant","bed","dining table","toilet","tv","laptop","mouse",
        "remote","keyboard","cell phone","microwave","oven","toaster","sink","refrigerator",
        "book","clock","vase","scissors","teddy bear","hair drier","toothbrush"
    };

    // ---- Shared constants ----
    private static final int    MODEL_W            = 640;
    private static final int    MODEL_H            = 640;  // COCO YOLOv5s uses 640×640
    private static final int    TRAY_MODEL_H       = 480;  // tray model uses 640×480
    private static final int    MAX_JPEG_BYTES     = 2 * 1024 * 1024;
    private static final int    CANONICAL_JPEG_QUALITY = 85;
    private static final String INPUT_TAG          = "images";  // YOLOv5s NCNN input layer name
    private static final String TRAY_INPUT_TAG     = "data";    // tray model input layer name
    private static final int    INPUT_ID           = 0;
    private static final String EXTRACT_TAG        = "output";

    /** Currently active labels — set by mode. */
    public static String[] LABELS = TRAY_LABELS;

    private final Context appContext;
    private KNYoloExecutor executor;
    private String paramPath;
    private String binPath;
    private boolean initialized = false;
    private boolean initFailed  = false;
    private Mode currentMode    = Mode.TRAY;

    public YoloDetectorHelper(Context context) {
        this.appContext = context.getApplicationContext();
    }

    /**
     * Switch detection mode. Must be called before ensureLoaded(), or call close() first
     * then ensureLoaded() again to reload with the new model.
     */
    public synchronized void setMode(Mode mode) {
        if (mode == currentMode) return;
        if (initialized) {
            close();
        }
        currentMode = mode;
        initFailed  = false;
        LABELS = (mode == Mode.COCO) ? COCO_LABELS : TRAY_LABELS;
        Log.i(TAG, "Mode switched to " + mode);
    }

    public Mode getMode() { return currentMode; }

    /**
     * Must be called once on a background thread BEFORE any detect() calls.
     * Blocks until the model is fully loaded into NCNN memory.
     */
    public synchronized boolean ensureLoaded() {
        if (initialized) return true;
        if (initFailed)  return false;
        try {
            if (currentMode == Mode.TRAY) {
                if (!CanpanSharedNativeModel.acquire(appContext)) {
                    initFailed = true;
                    return false;
                }
                executor  = CanpanSharedNativeModel.getExecutor();
                paramPath = CanpanSharedNativeModel.getParamPath();
                binPath   = CanpanSharedNativeModel.getBinPath();
                initialized = true;
                Log.i(TAG, "NCNN YOLOv5 ready [TRAY] — shared canpan — " + paramPath);
                return true;
            }

            String paramAsset = COCO_PARAM_ASSET;
            String binAsset   = COCO_BIN_ASSET;
            int modelType     = COCO_MODEL_TYPE;
            int modelSize     = COCO_MODEL_SIZE;

            paramPath = copyAssetToFiles(paramAsset);
            binPath   = copyAssetToFiles(binAsset);
            synchronized (NcnnGate.LOCK) {
                executor = new KNYoloExecutor();
                executor.init(modelSize);
                executor.initModel(modelType, paramPath, binPath);
            }
            initialized = true;
            Log.i(TAG, "NCNN YOLOv5 ready [COCO] — " + paramPath);
            return true;
        } catch (Throwable t) {
            Log.e(TAG, "ensureLoaded failed [" + currentMode + "]", t);
            initFailed = true;
            return false;
        }
    }

    /** Returns true if the model is already loaded (no blocking). */
    public boolean isReady() {
        return initialized;
    }

    public List<Result> detect(byte[] jpeg) {
        int h = (currentMode == Mode.COCO) ? MODEL_W : TRAY_MODEL_H;
        return detect(jpeg, MODEL_W, h);
    }

    /**
     * Run detection on a JPEG byte array.
     * detectData() uses OpenCV imdecode() internally — it requires valid JPEG bytes.
     */
    public synchronized List<Result> detect(byte[] jpeg, int srcWidth, int srcHeight) {
        if (jpeg == null || jpeg.length < 4) return Collections.emptyList();
        if (jpeg.length > MAX_JPEG_BYTES) {
            Log.w(TAG, "detect() skipped — jpeg too large: " + jpeg.length);
            return Collections.emptyList();
        }
        if (srcWidth <= 0 || srcHeight <= 0 || srcWidth > 1920 || srcHeight > 1920) {
            Log.w(TAG, "detect() skipped — invalid frame size " + srcWidth + "x" + srcHeight);
            return Collections.emptyList();
        }
        if (!isLikelyJpeg(jpeg)) {
            Log.w(TAG, "detect() skipped — invalid JPEG markers (len=" + jpeg.length + ")");
            return Collections.emptyList();
        }
        if (!isJpegDecodable(jpeg)) {
            Log.w(TAG, "detect() skipped — JPEG decode bounds failed (len=" + jpeg.length + ")");
            return Collections.emptyList();
        }
        byte[] canonicalJpeg = canonicalizeJpeg(jpeg);
        if (canonicalJpeg == null || canonicalJpeg.length < 4) {
            Log.w(TAG, "detect() skipped — JPEG canonicalization failed");
            return Collections.emptyList();
        }
        if (!isLikelyJpeg(canonicalJpeg)) {
            Log.w(TAG, "detect() skipped — canonical JPEG markers invalid");
            return Collections.emptyList();
        }
        if (!ensureLoaded()) return Collections.emptyList();

        int modelType   = (currentMode == Mode.COCO) ? COCO_MODEL_TYPE  : TRAY_MODEL_TYPE;
        double confThr  = (currentMode == Mode.COCO) ? COCO_CONF_THRESH : TRAY_CONF_THRESH;
        String inputTag = (currentMode == Mode.COCO) ? INPUT_TAG        : TRAY_INPUT_TAG;
        int modelH      = (currentMode == Mode.COCO) ? MODEL_W          : TRAY_MODEL_H;
        String[] labels = (currentMode == Mode.COCO) ? COCO_LABELS      : TRAY_LABELS;

        int jniSrcW =
                srcWidth == MODEL_W ? NcnnModelJpeg.JNI_SRC_W : srcWidth;
        int jniSrcH = srcHeight;

        try {
            KNBox[] boxes;
            synchronized (NcnnGate.LOCK) {
                boxes = executor.detectData(
                        modelType, canonicalJpeg, canonicalJpeg.length,
                        confThr,
                        paramPath, binPath,
                        inputTag, INPUT_ID, EXTRACT_TAG,
                        jniSrcW, jniSrcH, MODEL_W, modelH);
            }

            if (boxes == null || boxes.length == 0) return Collections.emptyList();

            List<Result> results = new ArrayList<>();
            for (KNBox b : boxes) {
                if (b == null) continue;
                float score = b.getScore();
                if (score < confThr) continue;
                int cls = b.label;
                String label = (cls >= 0 && cls < labels.length) ? labels[cls] : "class_" + cls;
                results.add(new Result(label, score, b.x0, b.y0, b.x1, b.y1));
            }
            Log.d(TAG, "detect[" + currentMode + "]: " + results.size() + " results from "
                    + srcWidth + "x" + srcHeight);
            return results;
        } catch (Throwable t) {
            Log.w(TAG, "detect() threw", t);
            return Collections.emptyList();
        }
    }

    public synchronized void close() {
        if (!initialized || executor == null) {
            return;
        }
        if (currentMode == Mode.TRAY) {
            CanpanSharedNativeModel.release();
        } else {
            try {
                synchronized (NcnnGate.LOCK) {
                    executor.destroyModel();
                }
            } catch (Throwable ignored) {}
        }
        executor = null;
        initialized = false;
    }

    // ---- Asset → internal storage copy ----

    private static boolean isLikelyJpeg(byte[] jpeg) {
        return jpeg.length >= 4
                && jpeg[0] == (byte) 0xFF
                && jpeg[1] == (byte) 0xD8
                && jpeg[jpeg.length - 2] == (byte) 0xFF
                && jpeg[jpeg.length - 1] == (byte) 0xD9;
    }

    private static boolean isJpegDecodable(byte[] jpeg) {
        try {
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inJustDecodeBounds = true;
            BitmapFactory.decodeByteArray(jpeg, 0, jpeg.length, opts);
            return opts.outWidth > 0 && opts.outHeight > 0;
        } catch (Throwable t) {
            return false;
        }
    }

    private static byte[] canonicalizeJpeg(byte[] jpeg) {
        Bitmap bitmap = null;
        try {
            bitmap = BitmapFactory.decodeByteArray(jpeg, 0, jpeg.length);
            if (bitmap == null || bitmap.getWidth() <= 0 || bitmap.getHeight() <= 0) {
                return null;
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream(Math.max(16384, jpeg.length / 2));
            boolean ok = bitmap.compress(Bitmap.CompressFormat.JPEG, CANONICAL_JPEG_QUALITY, out);
            if (!ok) return null;
            return out.toByteArray();
        } catch (Throwable t) {
            return null;
        } finally {
            if (bitmap != null && !bitmap.isRecycled()) {
                bitmap.recycle();
            }
        }
    }

    private String copyAssetToFiles(String assetPath) throws IOException {
        File dest = new File(appContext.getFilesDir(), assetPath);
        if (dest.exists() && dest.length() > 0) return dest.getAbsolutePath();
        //noinspection ResultOfMethodCallIgnored
        dest.getParentFile().mkdirs();
        AssetManager am = appContext.getAssets();
        try (InputStream in = am.open(assetPath);
             FileOutputStream out = new FileOutputStream(dest)) {
            byte[] buf = new byte[65536];
            int n;
            while ((n = in.read(buf)) != -1) out.write(buf, 0, n);
        }
        Log.d(TAG, "Copied " + assetPath + " → " + dest.getAbsolutePath());
        return dest.getAbsolutePath();
    }

    // ---- Result ----

    public static final class Result {
        public final String label;
        public final float  score;
        public final float  x1, y1, x2, y2;

        public Result(String label, float score, float x1, float y1, float x2, float y2) {
            this.label = label; this.score = score;
            this.x1 = x1; this.y1 = y1; this.x2 = x2; this.y2 = y2;
        }
    }
}
