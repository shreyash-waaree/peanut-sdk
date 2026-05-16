package com.keenon.peanut.supermarket.yolo;

import android.util.Log;

/**
 * JNI bridge into libcamerasdk.so — mirrors the KeenonDiner KNCameraNative exactly.
 * The library name "camerasdk" matches the BUILD_ARTIFACTID used in the original app.
 */
public final class KNCameraNative {

    private static final String TAG = "KNCameraNative";

    private volatile boolean isCapturing = false;
    private KNCaptureCallback mCaptureCallback;

    // libcamerasdk.so is loaded by com.keenon.component.camera.KNCameraNative shim

    // ---- Singleton ----

    private static final KNCameraNative INSTANCE = new KNCameraNative();

    private KNCameraNative() {}

    public static KNCameraNative getInstance() {
        return INSTANCE;
    }

    // ---- Native declarations (must match JNI symbols in libcamerasdk.so) ----

    protected native long openCamera(String deviceIndex, int width, int height, int format, int maxTryCount);
    protected native int getCameraBufferLength(long cameraFd);
    protected native void initDataAddr(long cameraFd, byte[] buffer);
    protected native int reOpenCamera(long cameraFd, int format);
    protected native int releaseCamera(long cameraFd, byte[] buffer);
    protected native void closeCamera(long cameraFd);
    protected native void closeCameras(long[] fds, int count);
    protected native int qeuryCameraState(long cameraFd);
    private native void captureCamera(long cameraFd);
    protected native void nativeCmd(String cmd);

    // ---- Called by JNI on capture result ----

    protected void nativeSuccess(long fd, int status) {
        KNCaptureCallback cb = mCaptureCallback;
        if (cb != null) cb.onSuccess(fd, status);
    }

    protected void nativeFailure(long fd, int status, int errorCode) {
        KNCaptureCallback cb = mCaptureCallback;
        if (cb != null) cb.onFailure(fd, status, errorCode, "camera");
    }

    // ---- Public helpers ----

    public void setCaptureCallback(KNCaptureCallback cb) {
        mCaptureCallback = cb;
    }

    /** Opens camera by integer device index (maps to /dev/videoN internally). */
    public long createCamera(int deviceIndex, int width, int height, int format) {
        return openCamera(String.valueOf(deviceIndex), width, height, format, 4);
    }

    public void scheduleCaptureCamera(long fd) {
        Log.d(TAG, "capture fd=" + fd);
        captureCamera(fd);
    }
}
