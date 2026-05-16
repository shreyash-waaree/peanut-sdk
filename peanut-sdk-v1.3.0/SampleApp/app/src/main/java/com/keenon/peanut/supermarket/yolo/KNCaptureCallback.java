package com.keenon.peanut.supermarket.yolo;

/** Callback for a single JPEG frame captured via the Keenon native camera SDK. */
public interface KNCaptureCallback {
    void onSuccess(long cameraFd, int status);
    void onFailure(long cameraFd, int status, int errorCode, String message);
}
