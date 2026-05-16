package com.keenon.component.camera;

import android.util.Log;

/**
 * Shim at the exact package path that libcamerasdk.so was compiled against.
 * The native library's JNI callbacks are hardcoded to find:
 *   com/keenon/component/camera/KNCameraNative.nativeSuccess
 *   com/keenon/component/camera/KNCameraNative.nativeFailure
 * This class must live here so dlopen + JNI FindClass succeeds.
 *
 * It forwards all callbacks to our actual wrapper in the supermarket.yolo package.
 */
public final class KNCameraNative {

    private static final String TAG = "KNCameraNative";

    private volatile boolean isCapturing = false;
    private com.keenon.peanut.supermarket.yolo.KNCaptureCallback mCaptureCallback;

    static {
        try {
            System.loadLibrary("camerasdk");
            Log.i(TAG, "libcamerasdk.so loaded OK");
        } catch (UnsatisfiedLinkError e) {
            Log.e(TAG, "libcamerasdk.so load FAILED: " + e.getMessage());
        }
    }

    // ---- Singleton ----
    private static final KNCameraNative INSTANCE = new KNCameraNative();
    private KNCameraNative() {}
    public static KNCameraNative getInstance() { return INSTANCE; }

    // ---- Native declarations — symbols live in libcamerasdk.so ----
    public native long openCamera(String deviceIndex, int w, int h, int fmt, int maxTry);
    public native int  getCameraBufferLength(long fd);
    public native void initDataAddr(long fd, byte[] buf);
    public native int  reOpenCamera(long fd, int fmt);
    public native int  releaseCamera(long fd, byte[] buf);
    public native void closeCamera(long fd);
    public native void closeCameras(long[] fds, int count);
    public native int  qeuryCameraState(long fd);
    private native void captureCamera(long fd);
    public native void nativeCmd(String cmd);

    // ---- JNI callbacks — native lib calls these by name on THIS class ----
    // Must be public/protected and match the exact signature the C code uses.

    protected void nativeSuccess(long fd, int status) {
        Log.d(TAG, "nativeSuccess fd=" + fd + " status=" + status);
        com.keenon.peanut.supermarket.yolo.KNCaptureCallback cb = mCaptureCallback;
        if (cb != null) cb.onSuccess(fd, status);
    }

    protected void nativeFailure(long fd, int status, int errorCode) {
        Log.w(TAG, "nativeFailure fd=" + fd + " status=" + status + " err=" + errorCode);
        com.keenon.peanut.supermarket.yolo.KNCaptureCallback cb = mCaptureCallback;
        if (cb != null) cb.onFailure(fd, status, errorCode, "camera");
    }

    // ---- Public API ----

    public void setCaptureCallback(com.keenon.peanut.supermarket.yolo.KNCaptureCallback cb) {
        mCaptureCallback = cb;
    }

    public long createCamera(int deviceIndex, int w, int h, int fmt) {
        return openCamera(String.valueOf(deviceIndex), w, h, fmt, 4);
    }

    public void scheduleCaptureCamera(long fd) {
        Log.d(TAG, "scheduleCaptureCamera fd=" + fd);
        captureCamera(fd);
    }
}
