package com.keenon.peanut.supermarket.vision;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.util.Log;

import com.keenon.sdk.base.model.KNBox;
import com.keenon.sdk.yolo.KNYoloExecutor;
import com.keenon.peanut.supermarket.yolo.CanpanSharedNativeModel;
import com.keenon.peanut.supermarket.yolo.NcnnGate;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Counts food items on a Keenon tray using the same canpan_detection NCNN YOLOv5 model
 * that the production KeenonDiner app uses — so it has the same accuracy and the same
 * background-rejection behaviour.
 *
 * How KeenonDiner rejects background objects (legs, floor, cables):
 *   1. canpan_detection was TRAINED on ceiling-camera tray images → already biased against
 *      non-tray objects. Only fires on things that look like food plates from above.
 *   2. CIRCULAR ROI FILTER (KNPlateFilter) — after YOLO runs, any detected box whose
 *      centre-point falls OUTSIDE a circle (centre 324.9,229.22 radius 265 in a 640×480 frame)
 *      is discarded. The circle is sized to cover exactly the tray disc.
 *   3. LABEL FILTER — only label index 1 ("plate") is counted; label 0 ("ros_empty") ignored.
 *
 * We replicate steps 2 & 3 here exactly. The circle parameters match KNModelFactory exactly.
 * The user can also adjust the circle via {@link #setCircle} to fine-tune for their tray size.
 */
public class TrayPlateCounter {

    private static final String TAG = "TrayPlateCounter";

    // Model files — same ones used by KeenonDiner production app
    private static final String PARAM_ASSET = "yolo/canpan_detection_v1.1.5-0-g0000000.param";
    private static final String BIN_ASSET   = "yolo/canpan_detection_v1.1.5-0-g0000000.bin";

    // Model inference — match KeenonDiner KNModelFactory.buildPlateModel() exactly:
    // type 0 (YOLO_MODEL_PLATE), init(5) slots, 640×480 model input (KNPlateImg).
    private static final int    MODEL_TYPE    = 0;
    private static final int    MODEL_INIT_SZ = 5;
    private static final double DEFAULT_CONF  = 0.40; // KeenonDiner default (plates only)
    private static final String INPUT_TAG     = "data";
    private static final int    INPUT_ID      = 0;
    private static final String EXTRACT_TAG   = "output";
    private static final int    MODEL_W       = 640;
    private static final int    MODEL_H       = 480;

    /**
     * Detection confidence threshold. Default 0.40 matches KeenonDiner production.
     * Lower this (e.g. 0.20) when you need to count non-plate objects (wallets, dark items)
     * — the canpan model was trained on food plates, so non-plate objects produce lower scores.
     */
    private double confThreshold = DEFAULT_CONF;

    // Circular ROI — from KNModelFactory.buildPlateModel():
    //   circle(324.9f, 229.22f, 265.0f)
    // Centre is slightly right-of-mid and slightly above mid in a 640×480 frame.
    // This exactly covers the tray disc as seen from the ceiling camera.
    // These are in the 640×480 model-input coordinate space.
    private float circleX      = 324.9f;
    private float circleY      = 229.22f;
    private float circleRadius = 265.0f;

    private static final int MAX_JPEG_BYTES = 2 * 1024 * 1024;
    private static final int JPEG_QUALITY   = 85;

    private final Context appContext;
    private KNYoloExecutor executor;
    private String paramPath, binPath;
    private boolean initialized  = false;
    private boolean initFailed   = false;
    private volatile boolean modelReady = false;
    private boolean loggedFirstFrame = false;

    public TrayPlateCounter(Context context) {
        this.appContext = context.getApplicationContext();
    }

    /**
     * Override the circular tray mask. Call with the centre and radius that matches
     * your physical tray in 640×480 pixel coordinates.
     * Default matches KeenonDiner production: centre (324.9, 229.22) radius 265.
     */
    public void setCircle(float cx, float cy, float radius) {
        this.circleX      = cx;
        this.circleY      = cy;
        this.circleRadius = radius;
        Log.i(TAG, "Circle updated: (" + cx + "," + cy + ") r=" + radius);
    }

    /**
     * Adjust YOLO confidence threshold (0.10–0.90). Lower = more detections, more false positives.
     * Use 0.40 for plates (default), 0.20–0.30 for dark/non-plate objects like wallets.
     */
    public void setConfidence(double conf) {
        this.confThreshold = Math.max(0.10, Math.min(0.90, conf));
        Log.i(TAG, "Confidence threshold = " + this.confThreshold);
    }

    public double getConfidence() { return confThreshold; }

    public boolean isReady() { return modelReady; }

    /**
     * Load the NCNN model. Must be called on a background thread — blocks until done.
     */
    public synchronized boolean ensureLoaded() {
        if (initialized) return true;
        if (initFailed)  return false;
        try {
            // One native init for model type 0 — shared with YoloDetectorHelper TRAY (see CanpanSharedNativeModel).
            if (!CanpanSharedNativeModel.acquire(appContext)) {
                initFailed = true;
                return false;
            }
            executor  = CanpanSharedNativeModel.getExecutor();
            paramPath = CanpanSharedNativeModel.getParamPath();
            binPath   = CanpanSharedNativeModel.getBinPath();
            initialized = true;
            modelReady  = true;
            Log.i(TAG, "canpan_detection ready (shared native)");
            return true;
        } catch (Throwable t) {
            Log.e(TAG, "ensureLoaded failed", t);
            initFailed = true;
            return false;
        }
    }

    public synchronized void close() {
        if (initialized) {
            CanpanSharedNativeModel.release();
            executor    = null;
            initialized = false;
            modelReady  = false;
        }
    }

    /**
     * Count food plates in one NV21 camera frame.
     *
     * Pipeline (mirrors KeenonDiner exactly):
     *   NV21 → JPEG → canonicalise → NCNN canpan_detection → circular ROI filter → plate count
     *
     * @param nv21   raw NV21 bytes from Camera preview callback
     * @param width  frame width  (typically 640 for tray cameras)
     * @param height frame height (typically 480 for tray cameras)
     * @return number of plates detected inside the tray circle, or -1 if model not ready
     */
    public int count(byte[] nv21, int width, int height) {
        if (!modelReady || executor == null) return -1;
        int need = width * height * 3 / 2;
        if (nv21 == null || nv21.length < need) return -1;
        byte[] jpeg = nv21ToJpeg(nv21, width, height);
        if (jpeg == null) return -1;
        return countFromJpeg(jpeg, width, height);
    }

    /**
     * Count from an already-encoded JPEG (used by native camera path).
     */
    public synchronized int countFromJpeg(byte[] jpeg, int srcW, int srcH) {
        if (!modelReady || executor == null || jpeg == null) return -1;
        if (jpeg.length > MAX_JPEG_BYTES) return -1;
        if (!isLikelyJpeg(jpeg)) return -1;

        if (!isJpegDecodable(jpeg)) return -1;
        byte[] canonical = canonicalise(jpeg);
        if (canonical == null || !isLikelyJpeg(canonical) || !isJpegDecodable(canonical)) return -1;
        logFirstFrame(jpeg, canonical, srcW, srcH);

        try {
            KNBox[] boxes = runDetect(canonical, srcW, srcH);
            return applyCircleFilter(boxes);
        } catch (Throwable t) {
            Log.w(TAG, "countFromJpeg threw", t);
            return -1;
        }
    }

    /** Single-threaded JNI entry — never overlap detectData calls (process-wide). */
    private KNBox[] runDetect(byte[] canonical, int srcW, int srcH) {
        if (!modelReady || executor == null || canonical == null) return null;
        // RK3288 / libncnn: from_pixels SIGSEGV when JPEG intrinsic size != (w,h) passed
        // to detectData, or after native "initModel" during a marginal frame. Always feed
        // a freshly encoded 640×480 JPEG so buffer layout matches MODEL_W×MODEL_H exactly.
        byte[] modelJpeg = jpegResizedToModelInput(canonical);
        if (modelJpeg == null) {
            Log.w(TAG, "runDetect: skip — could not build " + MODEL_W + "×" + MODEL_H + " model JPEG");
            return null;
        }
        synchronized (NcnnGate.LOCK) {
            return executor.detectData(
                    MODEL_TYPE, modelJpeg, modelJpeg.length,
                    confThreshold,
                    paramPath, binPath,
                    INPUT_TAG, INPUT_ID, EXTRACT_TAG,
                    MODEL_W, MODEL_H, MODEL_W, MODEL_H);
        }
    }

    /**
     * Decode JPEG and re-encode at exactly {@link #MODEL_W}×{@link #MODEL_H} so native
     * {@code from_pixels} never sees a width/height/stride mismatch (crash on rk3288).
     */
    private static byte[] jpegResizedToModelInput(byte[] jpeg) {
        if (jpeg == null || jpeg.length < 4) return null;
        Bitmap decoded = null;
        Bitmap scaled = null;
        try {
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inPreferredConfig = Bitmap.Config.ARGB_8888;
            decoded = BitmapFactory.decodeByteArray(jpeg, 0, jpeg.length, opts);
            if (decoded == null) return null;
            final int w = decoded.getWidth();
            final int h = decoded.getHeight();
            if (w <= 0 || h <= 0) return null;
            if (w == MODEL_W && h == MODEL_H) {
                scaled = decoded;
                decoded = null;
            } else {
                scaled = Bitmap.createScaledBitmap(decoded, MODEL_W, MODEL_H, true);
                if (scaled == null) return null;
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream(Math.max(32768, jpeg.length / 2 + 1024));
            if (!scaled.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)) return null;
            return out.toByteArray();
        } catch (Throwable t) {
            Log.w(TAG, "jpegResizedToModelInput failed", t);
            return null;
        } finally {
            if (decoded != null && !decoded.isRecycled()) decoded.recycle();
            if (scaled != null && scaled != decoded && !scaled.isRecycled()) scaled.recycle();
        }
    }

    /** Detailed result for one frame — includes raw boxes for UI logging. */
    public static final class CountResult {
        public final int count;            // filtered count (inside circle, plate only)
        public final int rawBoxCount;      // raw YOLO detections before filtering
        public final List<String> logLines;  // human-readable per-box log
        public CountResult(int count, int rawBoxCount, List<String> logLines) {
            this.count = count;
            this.rawBoxCount = rawBoxCount;
            this.logLines = logLines;
        }
    }

    /**
     * Detailed counting: returns count plus per-box log lines for live debugging.
     */
    public synchronized CountResult countDetailed(byte[] nv21, int width, int height) {
        if (!modelReady || executor == null) {
            return new CountResult(-1, 0, Collections.singletonList("model not ready"));
        }
        int need = width * height * 3 / 2;
        if (nv21 == null || nv21.length < need) {
            return new CountResult(-1, 0, Collections.singletonList(
                    "nv21 size mismatch: need " + need + " have " + (nv21 == null ? 0 : nv21.length)));
        }
        byte[] jpeg = nv21ToJpeg(nv21, width, height);
        if (jpeg == null) {
            return new CountResult(-1, 0, Collections.singletonList("nv21->jpeg failed"));
        }
        if (!isJpegDecodable(jpeg)) {
            return new CountResult(-1, 0, Collections.singletonList("jpeg not decodable"));
        }
        byte[] canonical = canonicalise(jpeg);
        if (canonical == null) {
            return new CountResult(-1, 0, Collections.singletonList("canonicalise failed"));
        }
        logFirstFrame(jpeg, canonical, width, height);
        try {
            KNBox[] boxes = runDetect(canonical, width, height);
            return buildDetailed(boxes);
        } catch (Throwable t) {
            return new CountResult(-1, 0,
                    Collections.singletonList("detect threw: " + t.getClass().getSimpleName()));
        }
    }

    /**
     * Same as {@link #countDetailed(byte[], int, int)} but input is already JPEG (e.g. Keenon native camera).
     */
    public synchronized CountResult countDetailedFromJpeg(byte[] jpeg, int width, int height) {
        if (!modelReady || executor == null) {
            return new CountResult(-1, 0, Collections.singletonList("model not ready"));
        }
        if (jpeg == null) {
            return new CountResult(-1, 0, Collections.singletonList("jpeg null"));
        }
        if (jpeg.length > MAX_JPEG_BYTES) {
            return new CountResult(-1, 0, Collections.singletonList("jpeg too large"));
        }
        if (!isLikelyJpeg(jpeg) && !isJpegStart(jpeg)) {
            return new CountResult(-1, 0, Collections.singletonList("not a JPEG buffer"));
        }
        if (!isJpegDecodable(jpeg) && !isJpegStart(jpeg)) {
            return new CountResult(-1, 0, Collections.singletonList("jpeg not decodable"));
        }
        byte[] canonical = canonicalise(jpeg);
        if (canonical == null) {
            return new CountResult(-1, 0, Collections.singletonList("canonicalise failed"));
        }
        logFirstFrame(jpeg, canonical, width, height);
        try {
            KNBox[] boxes = runDetect(canonical, width, height);
            return buildDetailed(boxes);
        } catch (Throwable t) {
            return new CountResult(-1, 0,
                    Collections.singletonList("detect threw: " + t.getClass().getSimpleName()));
        }
    }

    private CountResult buildDetailed(KNBox[] boxes) {
        List<String> lines = new ArrayList<>();
        if (boxes == null || boxes.length == 0) {
            lines.add("no raw boxes");
            return new CountResult(0, 0, lines);
        }
        String[] labels = {"ros_empty", "plate"};
        int count = 0;
        for (KNBox b : boxes) {
            if (b == null) continue;
            float cx = (b.x0 + b.x1) / 2f;
            float cy = (b.y0 + b.y1) / 2f;
            double dist = Math.sqrt(Math.pow(circleX - cx, 2) + Math.pow(circleY - cy, 2));
            String lbl = (b.label >= 0 && b.label < labels.length) ? labels[b.label] : "?";
            boolean inCircle = dist <= circleRadius;
            boolean isPlate  = b.label == 1;
            boolean counted  = inCircle && isPlate;
            if (counted) count++;
            lines.add(String.format(java.util.Locale.US,
                    "%s %d%% cx=%d cy=%d d=%d %s %s",
                    lbl, (int)(b.getScore() * 100), (int)cx, (int)cy, (int)dist,
                    inCircle ? "IN" : "OUT",
                    counted ? "✓" : "✗"));
        }
        return new CountResult(count, boxes.length, lines);
    }

    /**
     * Circular ROI post-filter — exactly what KeenonDiner's KNPlateFilter does.
     *
     * Only count a box if:
     *   1. Its centre point is inside the tray circle
     *   2. Its label index == 1 ("plate") — not 0 ("ros_empty")
     */
    private int applyCircleFilter(KNBox[] boxes) {
        if (boxes == null || boxes.length == 0) return 0;
        int count = 0;
        for (KNBox box : boxes) {
            if (box == null) continue;
            float cx = (box.x0 + box.x1) / 2f;
            float cy = (box.y0 + box.y1) / 2f;
            double dist = Math.sqrt(
                    Math.pow(circleX - cx, 2) + Math.pow(circleY - cy, 2));
            if (dist <= circleRadius && box.label == 1) {
                count++;
                Log.d(TAG, "plate at (" + cx + "," + cy + ") dist=" + (int)dist
                        + " score=" + box.getScore());
            } else {
                Log.d(TAG, "rejected box label=" + box.label + " dist=" + (int)dist
                        + " (outside circle or ros_empty)");
            }
        }
        return count;
    }

    // ---- JPEG helpers ----

    private static byte[] nv21ToJpeg(byte[] nv21, int width, int height) {
        try {
            YuvImage yuv = new YuvImage(nv21, ImageFormat.NV21, width, height, null);
            ByteArrayOutputStream out = new ByteArrayOutputStream(nv21.length / 4);
            yuv.compressToJpeg(new Rect(0, 0, width, height), JPEG_QUALITY, out);
            return out.toByteArray();
        } catch (Exception e) {
            Log.w(TAG, "nv21ToJpeg failed", e);
            return null;
        }
    }

    private static int[] jpegDecodedSize(byte[] jpeg) {
        if (jpeg == null || jpeg.length < 2) return null;
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(jpeg, 0, jpeg.length, o);
        if (o.outWidth <= 0 || o.outHeight <= 0) return null;
        return new int[]{o.outWidth, o.outHeight};
    }

    private synchronized void logFirstFrame(byte[] raw, byte[] canonical, int passedW, int passedH) {
        if (loggedFirstFrame) return;
        loggedFirstFrame = true;
        int[] rawSize = jpegDecodedSize(raw);
        int[] canSize = jpegDecodedSize(canonical);
        Log.i(TAG, "first frame: raw=" + (rawSize != null ? rawSize[0] + "x" + rawSize[1] : "?")
                + " canonical=" + (canSize != null ? canSize[0] + "x" + canSize[1] : "?")
                + " passed=" + passedW + "x" + passedH
                + " model=" + MODEL_W + "x" + MODEL_H);
    }

    /**
     * Re-encode JPEG so OpenCV imdecode inside {@code detectData} gets clean markers.
     * Does not change resolution — KeenonDiner uses 640×480 for canpan_detection.
     */
    private static byte[] canonicalise(byte[] jpeg) {
        Bitmap bmp = null;
        try {
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inPreferredConfig = Bitmap.Config.RGB_565;
            bmp = BitmapFactory.decodeByteArray(jpeg, 0, jpeg.length, opts);
            if (bmp == null || bmp.getWidth() <= 0 || bmp.getHeight() <= 0) return null;
            ByteArrayOutputStream out = new ByteArrayOutputStream(Math.max(16384, jpeg.length / 2));
            if (!bmp.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)) return null;
            return out.toByteArray();
        } catch (Throwable t) {
            Log.w(TAG, "canonicalise failed", t);
            return null;
        } finally {
            if (bmp != null && !bmp.isRecycled()) bmp.recycle();
        }
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

    private static boolean isLikelyJpeg(byte[] b) {
        return b.length >= 4
                && b[0] == (byte) 0xFF && b[1] == (byte) 0xD8
                && b[b.length - 2] == (byte) 0xFF && b[b.length - 1] == (byte) 0xD9;
    }

    /** Native camera may omit EOI; still try decode if SOI is present. */
    private static boolean isJpegStart(byte[] b) {
        return b.length >= 2 && b[0] == (byte) 0xFF && b[1] == (byte) 0xD8;
    }

    // ---- Debug helpers ----

    /** Return a human-readable description of all raw YOLO boxes (before circle filter). */
    public synchronized List<String> debugRawBoxes(byte[] nv21, int width, int height) {
        if (!modelReady || executor == null) return Collections.emptyList();
        byte[] jpeg = nv21ToJpeg(nv21, width, height);
        if (jpeg == null) return Collections.emptyList();
        byte[] canonical = canonicalise(jpeg);
        if (canonical == null) return Collections.emptyList();
        try {
            KNBox[] boxes = runDetect(canonical, width, height);
            if (boxes == null) return Collections.emptyList();
            List<String> lines = new ArrayList<>();
            String[] labels = {"ros_empty", "plate"};
            for (KNBox b : boxes) {
                if (b == null) continue;
                float cx = (b.x0 + b.x1) / 2f;
                float cy = (b.y0 + b.y1) / 2f;
                double dist = Math.sqrt(Math.pow(circleX - cx, 2) + Math.pow(circleY - cy, 2));
                String lbl = (b.label >= 0 && b.label < labels.length) ? labels[b.label] : "?";
                boolean inside = dist <= circleRadius;
                lines.add(lbl + " " + (int)(b.getScore()*100) + "% cx=" + (int)cx
                        + " cy=" + (int)cy + " dist=" + (int)dist
                        + (inside ? " IN" : " OUT"));
            }
            return lines;
        } catch (Throwable t) {
            return Collections.emptyList();
        }
    }
}