package com.keenon.peanut.supermarket.yolo;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

import androidx.annotation.Nullable;

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
 * Wraps Keenon's hand_detect_v1.0.2 NCNN model (extracted from KeenonDiner APK).
 *
 * Detects hands in a JPEG frame and returns their bounding boxes.
 * The primary use is determining which side of the frame (left/right) a hand
 * reached in from — this drives the robot's scan direction when an item is picked.
 *
 * Model slot: 4 (KNModelEnum.YOLO_MODEL_PALM in KeenonDiner)
 * Input layer: "data", id=0
 * Output layer: "output"
 * Model dimensions: 640×480
 * Confidence threshold: 0.9 (Keenon uses very high threshold — model tends to
 *                              over-fire at lower values)
 * MODEL_SIZE: 5 (slots 0-4; matches KeenonDiner's setModelSize(5))
 */
public final class HandDetectorHelper {

    private static final String TAG = "HandDetectorHelper";

    private static final String PARAM_ASSET = "yolo/hand_detect_v1.0.2-0-g0000000.param";
    private static final String BIN_ASSET   = "yolo/hand_detect_v1.0.2-0-g0000000.bin";

    // Must share the executor slot space with body (slot 1) — total size 5.
    private static final int    MODEL_TYPE  = 4;
    private static final int    MODEL_SIZE  = 5;
    private static final double CONF_THRESH = 0.9;
    private static final String INPUT_TAG   = "data";
    private static final int    INPUT_ID    = 0;
    private static final String EXTRACT_TAG = "output";
    private static final int    MODEL_W     = 640;
    private static final int    MODEL_H     = 480;

    /** Direction a hand was detected on. */
    public enum HandSide { LEFT, RIGHT, UNKNOWN }

    /** Single detected hand. */
    public static final class Result {
        public final float score;
        /** Normalised centre (0=left/top edge, 1=right/bottom edge). */
        public final float centerX;
        public final float centerY;
        public final float x1, y1, x2, y2;

        Result(float score, float x1, float y1, float x2, float y2) {
            this.score   = score;
            this.x1 = x1; this.y1 = y1; this.x2 = x2; this.y2 = y2;
            this.centerX = (x1 + x2) / 2f;
            this.centerY = (y1 + y2) / 2f;
        }
    }

    private final Context appContext;
    private KNYoloExecutor executor;
    private String paramPath;
    private String binPath;
    private boolean initialized = false;
    private boolean initFailed  = false;

    public HandDetectorHelper(Context context) {
        this.appContext = context.getApplicationContext();
    }

    /**
     * Must be called once on a background thread before any detect() calls.
     * Returns true if the model loaded successfully.
     */
    public synchronized boolean ensureLoaded() {
        if (initialized) return true;
        if (initFailed)  return false;
        try {
            paramPath = copyAssetToFiles(PARAM_ASSET);
            binPath   = copyAssetToFiles(BIN_ASSET);
            synchronized (NcnnGate.LOCK) {
                executor = new KNYoloExecutor();
                executor.init(MODEL_SIZE);
                executor.initModel(MODEL_TYPE, paramPath, binPath);
            }
            initialized = true;
            Log.i(TAG, "hand_detect model ready — " + paramPath);
            return true;
        } catch (Throwable t) {
            Log.e(TAG, "ensureLoaded failed", t);
            initFailed = true;
            return false;
        }
    }

    public boolean isReady() { return initialized; }

    /**
     * Run hand detection on a JPEG byte array.
     * Returns a list of detected hand bounding boxes, or empty list if none / error.
     *
     * @param jpeg      Valid JPEG bytes (must start with 0xFF 0xD8)
     * @param srcWidth  Frame width in pixels
     * @param srcHeight Frame height in pixels
     */
    public synchronized List<Result> detect(byte[] jpeg, int srcWidth, int srcHeight) {
        if (!initialized || executor == null) return Collections.emptyList();
        if (jpeg == null || jpeg.length < 4) return Collections.emptyList();
        if (!NcnnModelJpeg.isLikelyJpeg(jpeg) && !NcnnModelJpeg.isJpegDecodable(jpeg)) {
            return Collections.emptyList();
        }

        byte[] modelJpeg = NcnnModelJpeg.forModelInput(jpeg);
        if (modelJpeg == null) {
            Log.w(TAG, "detect: skip — could not build " + MODEL_W + "x" + MODEL_H + " JPEG");
            return Collections.emptyList();
        }

        try {
            final KNBox[] boxes;
            synchronized (NcnnGate.LOCK) {
                boxes = executor.detectData(
                        MODEL_TYPE, modelJpeg, modelJpeg.length,
                        CONF_THRESH,
                        paramPath, binPath,
                        INPUT_TAG, INPUT_ID, EXTRACT_TAG,
                        NcnnModelJpeg.JNI_SRC_W,
                        NcnnModelJpeg.JNI_SRC_H,
                        NcnnModelJpeg.MODEL_W,
                        NcnnModelJpeg.MODEL_H);
            }

            if (boxes == null || boxes.length == 0) return Collections.emptyList();

            List<Result> results = new ArrayList<>();
            for (KNBox b : boxes) {
                if (b == null || b.getScore() < CONF_THRESH) continue;
                results.add(new Result(b.getScore(), b.x0, b.y0, b.x1, b.y1));
            }
            if (!results.isEmpty()) {
                Log.d(TAG, "Hands detected: " + results.size());
            }
            return results;
        } catch (Throwable t) {
            Log.w(TAG, "detect() threw", t);
            return Collections.emptyList();
        }
    }

    /**
     * Convenience: returns which side of the frame the most confident hand is on.
     * Returns UNKNOWN if no hands detected.
     */
    public synchronized HandSide detectSide(byte[] jpeg, int srcWidth, int srcHeight) {
        List<Result> results = detect(jpeg, srcWidth, srcHeight);
        if (results.isEmpty()) return HandSide.UNKNOWN;
        Result best = results.get(0);
        for (Result r : results) {
            if (r.score > best.score) best = r;
        }
        return best.centerX < 0.5f ? HandSide.LEFT : HandSide.RIGHT;
    }

    /**
     * Returns the normalised centre-x of the most confident hand, or -1 if no hand found.
     * 0.0 = far left, 1.0 = far right.
     */
    public synchronized float detectCenterX(byte[] jpeg, int srcWidth, int srcHeight) {
        float[] xy = detectCenterXY(jpeg, srcWidth, srcHeight);
        return xy != null ? xy[0] : -1f;
    }

    /**
     * Normalised centre of the highest-confidence hand, or {@code null} if none.
     * {@code [0]=centerX}, {@code [1]=centerY}.
     */
    @Nullable
    public synchronized float[] detectCenterXY(byte[] jpeg, int srcWidth, int srcHeight) {
        List<Result> results = detect(jpeg, srcWidth, srcHeight);
        if (results.isEmpty()) return null;
        Result best = results.get(0);
        for (Result r : results) {
            if (r.score > best.score) best = r;
        }
        return new float[] {best.centerX, best.centerY};
    }

    public synchronized void close() {
        if (initialized && executor != null) {
            try {
                synchronized (NcnnGate.LOCK) {
                    executor.destroyModel();
                }
            } catch (Throwable ignored) {}
            executor    = null;
            initialized = false;
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
