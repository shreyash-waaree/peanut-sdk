package com.keenon.peanut.supermarket.yolo;

import android.content.Context;
import android.util.Log;

import com.keenon.sdk.yolo.KNYoloExecutor;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Single native load for KeenonDiner canpan_detection (YOLO model type 0).
 * <p>{@link com.keenon.sdk.yolo.KNYoloExecutor} / libyolov5 use process-wide state for slot 0.
 * If {@link com.keenon.peanut.supermarket.vision.TrayPlateCounter} and
 * {@link YoloDetectorHelper} (TRAY mode) each call {@code initModel(0, …)}, the second init
 * corrupts NCNN and the next {@code detectData} crashes in {@code from_pixels} (SIGSEGV on RK3288).</p>
 *
 * <p>Call {@link #acquire(Context)} once per feature that needs canpan; pair with
 * {@link #release()} when that feature is done. Reference count drops to zero before
 * {@code destroyModel} runs.</p>
 */
public final class CanpanSharedNativeModel {

    private static final String TAG = "CanpanShared";

    private static final String PARAM_ASSET = "yolo/canpan_detection_v1.1.5-0-g0000000.param";
    private static final String BIN_ASSET   = "yolo/canpan_detection_v1.1.5-0-g0000000.bin";
    private static final int    MODEL_TYPE  = 0;
    private static final int    MODEL_INIT  = 5;

    private static final Object LOCK = new Object();
    private static KNYoloExecutor executor;
    private static String paramPath;
    private static String binPath;
    private static int refCount;

    private CanpanSharedNativeModel() {}

    /**
     * Increments ref count and ensures canpan is loaded into native memory.
     *
     * @return {@code false} if init failed (caller should set initFailed / not use executor)
     */
    public static boolean acquire(Context appContext) {
        Context ctx = appContext.getApplicationContext();
        synchronized (LOCK) {
            if (executor != null) {
                refCount++;
                Log.d(TAG, "acquire (ref=" + refCount + ") — reusing loaded canpan");
                return true;
            }
            synchronized (NcnnGate.LOCK) {
                try {
                    paramPath = copyAssetToFiles(ctx, PARAM_ASSET);
                    binPath   = copyAssetToFiles(ctx, BIN_ASSET);
                    executor  = new KNYoloExecutor();
                    executor.init(MODEL_INIT);
                    executor.initModel(MODEL_TYPE, paramPath, binPath);
                    refCount = 1;
                    Log.i(TAG, "canpan native model loaded (ref=1)");
                    return true;
                } catch (Throwable t) {
                    Log.e(TAG, "acquire failed", t);
                    executor = null;
                    paramPath = null;
                    binPath = null;
                    refCount = 0;
                    return false;
                }
            }
        }
    }

    /** One {@link #acquire} must be paired with each {@link #release}. */
    public static void release() {
        synchronized (LOCK) {
            if (refCount <= 0) {
                return;
            }
            refCount--;
            if (refCount > 0) {
                Log.d(TAG, "release (ref=" + refCount + ") — keeping native model");
                return;
            }
            synchronized (NcnnGate.LOCK) {
                if (executor != null) {
                    try {
                        executor.destroyModel();
                    } catch (Throwable ignored) {}
                    executor = null;
                }
                paramPath = null;
                binPath = null;
                Log.i(TAG, "canpan native model destroyed (ref=0)");
            }
        }
    }

    public static KNYoloExecutor getExecutor() {
        return executor;
    }

    public static String getParamPath() {
        return paramPath;
    }

    public static String getBinPath() {
        return binPath;
    }

    private static String copyAssetToFiles(Context ctx, String assetPath) throws IOException {
        File dest = new File(ctx.getFilesDir(), assetPath);
        if (dest.exists() && dest.length() > 0) {
            return dest.getAbsolutePath();
        }
        File parent = dest.getParentFile();
        if (parent != null) {
            //noinspection ResultOfMethodCallIgnored
            parent.mkdirs();
        }
        try (InputStream in = ctx.getAssets().open(assetPath);
             FileOutputStream out = new FileOutputStream(dest)) {
            byte[] buf = new byte[65536];
            int n;
            while ((n = in.read(buf)) != -1) {
                out.write(buf, 0, n);
            }
        }
        Log.d(TAG, "Copied " + assetPath + " → " + dest.getAbsolutePath());
        return dest.getAbsolutePath();
    }
}
