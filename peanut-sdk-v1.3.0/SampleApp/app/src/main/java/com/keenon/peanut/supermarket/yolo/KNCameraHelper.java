package com.keenon.peanut.supermarket.yolo;

import java.io.File;

/**
 * Thin helper wrapping the shim KNCameraNative at com.keenon.component.camera —
 * which is the exact package path libcamerasdk.so was compiled against.
 */
public final class KNCameraHelper {

    public static final int FORMAT_JPEG = 0;

    private KNCameraHelper() {}

    /** @return true when /dev/videoN exists and is writable by this process. */
    public static boolean canOpenCamera(int deviceIndex) {
        return new File("/dev/video" + deviceIndex).canWrite();
    }

    public static void openCamera(int deviceIndex, int width, int height, int format,
                                  KNCameraCallback cb) {
        if (cb == null) return;
        try {
            long fd = com.keenon.component.camera.KNCameraNative.getInstance()
                    .createCamera(deviceIndex, width, height, format);
            if (fd <= 0) {
                cb.onFailure((int) fd, "camera open failed fd=" + fd);
                return;
            }
            int bufLen = com.keenon.component.camera.KNCameraNative.getInstance()
                    .getCameraBufferLength(fd);
            if (bufLen <= 0) {
                try {
                    com.keenon.component.camera.KNCameraNative.getInstance().closeCamera(fd);
                } catch (Throwable ignored) { }
                cb.onFailure(-2, "invalid native buffer length: " + bufLen);
                return;
            }
            cb.onSuccess(deviceIndex, fd, bufLen);
        } catch (UnsatisfiedLinkError e) {
            cb.onFailure(-1, "libcamerasdk not loaded: " + e.getMessage());
        } catch (Throwable t) {
            String msg = t.getMessage();
            cb.onFailure(-3, msg != null ? msg : ("openCamera: " + t.getClass().getSimpleName()));
        }
    }

    public static void initCaptureData(long fd, byte[] buffer) {
        com.keenon.component.camera.KNCameraNative.getInstance().initDataAddr(fd, buffer);
    }

    public static void captureCamera(long fd, KNCaptureCallback cb) {
        try {
            com.keenon.component.camera.KNCameraNative.getInstance().setCaptureCallback(cb);
            com.keenon.component.camera.KNCameraNative.getInstance().scheduleCaptureCamera(fd);
        } catch (UnsatisfiedLinkError e) {
            if (cb != null) cb.onFailure(fd, -1, -1, "libcamerasdk not loaded: " + e.getMessage());
        } catch (Throwable t) {
            if (cb != null) {
                String m = t.getMessage();
                cb.onFailure(fd, -1, -3, m != null ? m : t.getClass().getSimpleName());
            }
        }
    }

    public static void closeCamera(long fd) {
        com.keenon.component.camera.KNCameraNative.getInstance().closeCamera(fd);
    }

    public static void releaseCamera(long fd, byte[] buffer) {
        com.keenon.component.camera.KNCameraNative.getInstance().releaseCamera(fd, buffer);
    }

    public static int queryCameraState(long fd) {
        return com.keenon.component.camera.KNCameraNative.getInstance().qeuryCameraState(fd);
    }

    /** Callback for openCamera. */
    public interface KNCameraCallback {
        void onSuccess(int deviceIndex, long fd, int bufferLength);
        void onFailure(int errorCode, String message);
    }
}
