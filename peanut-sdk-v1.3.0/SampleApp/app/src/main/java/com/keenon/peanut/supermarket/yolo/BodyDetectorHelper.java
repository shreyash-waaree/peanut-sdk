package com.keenon.peanut.supermarket.yolo;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import com.keenon.sdk.base.model.KNBox;
import com.keenon.sdk.yolo.KNYoloExecutor;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Wraps Keenon's body_detection_v1.0.1 NCNN model (extracted from KeenonDiner APK).
 *
 * This model was custom-trained by Keenon specifically for detecting human bodies
 * at close range from the robot's perspective — much more reliable than COCO "person"
 * class for this use case.
 *
 * Used during the person-search phase: after an item is picked, the robot rotates
 * and uses this model on camera 3 frames to confirm a person is in front of it,
 * then faces them for feedback.
 *
 * Model slot: 1  (KNModelEnum.YOLO_MODEL_BODY in KeenonDiner)
 * Labels: ["person"]
 * Input layer: "data", id=0   (no .input() call in KNModelFactory — uses default)
 * Output layer: "output"
 * Model dimensions: 512×512
 * Confidence threshold: 0.25 (Keenon uses low threshold — model is conservative)
 * MODEL_SIZE: 5 (slots 0-4; matches KeenonDiner's setModelSize(5))
 */
public final class BodyDetectorHelper {

    private static final String TAG = "BodyDetectorHelper";

    private static final String PARAM_ASSET = "yolo/body_detection_v1.0.1-0-g00000000.param";
    private static final String BIN_ASSET   = "yolo/body_detection_v1.0.1-0-g00000000.bin";

    private static final int    MODEL_TYPE  = 1;
    private static final int    MODEL_SIZE  = 5;
    private static final double CONF_THRESH = 0.25;
    private static final String INPUT_TAG   = "data";
    private static final int    INPUT_ID    = 0;
    private static final String EXTRACT_TAG = "output";
    private static final int    MODEL_W     = 512;
    private static final int    MODEL_H     = 512;

    /** Single detected person. */
    public static final class Result {
        public final float score;
        /** Normalised centre-x (0=left, 1=right). */
        public final float centerX;
        public final float x1, y1, x2, y2;

        Result(float score, float x1, float y1, float x2, float y2) {
            this.score   = score;
            this.x1 = x1; this.y1 = y1; this.x2 = x2; this.y2 = y2;
            this.centerX = (x1 + x2) / 2f;
        }
    }

    private final Context appContext;
    private KNYoloExecutor executor;
    private String paramPath;
    private String binPath;
    private boolean initialized = false;
    private boolean initFailed  = false;

    public BodyDetectorHelper(Context context) {
        this.appContext = context.getApplicationContext();
    }

    /**
     * Must be called once on a background thread before any detect() calls.
     */
    public synchronized boolean ensureLoaded() {
        if (initialized) return true;
        if (initFailed)  return false;
        try {
            paramPath = copyAssetToFiles(PARAM_ASSET);
            binPath   = copyAssetToFiles(BIN_ASSET);
            executor  = new KNYoloExecutor();
            executor.init(MODEL_SIZE);
            executor.initModel(MODEL_TYPE, paramPath, binPath);
            initialized = true;
            Log.i(TAG, "body_detection model ready — " + paramPath);
            return true;
        } catch (Throwable t) {
            Log.e(TAG, "ensureLoaded failed", t);
            initFailed = true;
            return false;
        }
    }

    public boolean isReady() { return initialized; }

    /**
     * Detect bodies/persons in a JPEG frame.
     *
     * @param jpeg      Valid JPEG bytes
     * @param srcWidth  Frame width in pixels
     * @param srcHeight Frame height in pixels
     * @return List of detected person bounding boxes; empty if none found.
     */
    public synchronized List<Result> detect(byte[] jpeg, int srcWidth, int srcHeight) {
        if (!initialized || executor == null) return Collections.emptyList();
        if (jpeg == null || jpeg.length < 4) return Collections.emptyList();
        if (!isLikelyJpeg(jpeg)) return Collections.emptyList();

        byte[] canonical = canonicalizeJpeg(jpeg);
        if (canonical == null || !isLikelyJpeg(canonical)) return Collections.emptyList();

        try {
            KNBox[] boxes = executor.detectData(
                    MODEL_TYPE, canonical, canonical.length,
                    CONF_THRESH,
                    paramPath, binPath,
                    INPUT_TAG, INPUT_ID, EXTRACT_TAG,
                    srcWidth, srcHeight, MODEL_W, MODEL_H);

            if (boxes == null || boxes.length == 0) return Collections.emptyList();

            List<Result> results = new ArrayList<>();
            for (KNBox b : boxes) {
                if (b == null || b.getScore() < CONF_THRESH) continue;
                results.add(new Result(b.getScore(), b.x0, b.y0, b.x1, b.y1));
            }
            if (!results.isEmpty()) {
                Log.d(TAG, "Bodies detected: " + results.size()
                        + " best=" + (int)(results.get(0).score * 100) + "%");
            }
            return results;
        } catch (Throwable t) {
            Log.w(TAG, "detect() threw", t);
            return Collections.emptyList();
        }
    }

    /**
     * Returns true if at least one person is detected with confidence ≥ threshold.
     */
    public synchronized boolean isPersonVisible(byte[] jpeg, int srcWidth, int srcHeight) {
        return !detect(jpeg, srcWidth, srcHeight).isEmpty();
    }

    public synchronized void close() {
        if (initialized && executor != null) {
            try { executor.destroyModel(); } catch (Throwable ignored) {}
            executor    = null;
            initialized = false;
        }
    }

    private static boolean isLikelyJpeg(byte[] b) {
        return b.length >= 4
                && b[0] == (byte) 0xFF && b[1] == (byte) 0xD8
                && b[b.length - 2] == (byte) 0xFF && b[b.length - 1] == (byte) 0xD9;
    }

    private static byte[] canonicalizeJpeg(byte[] jpeg) {
        Bitmap bmp = null;
        try {
            bmp = BitmapFactory.decodeByteArray(jpeg, 0, jpeg.length);
            if (bmp == null || bmp.getWidth() <= 0) return null;
            ByteArrayOutputStream out = new ByteArrayOutputStream(jpeg.length);
            bmp.compress(Bitmap.CompressFormat.JPEG, 85, out);
            return out.toByteArray();
        } catch (Throwable t) {
            return null;
        } finally {
            if (bmp != null && !bmp.isRecycled()) bmp.recycle();
        }
    }

    private String copyAssetToFiles(String assetPath) throws IOException {
        File dest = new File(appContext.getFilesDir(), assetPath);
        if (dest.exists() && dest.length() > 0) return dest.getAbsolutePath();
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
}
