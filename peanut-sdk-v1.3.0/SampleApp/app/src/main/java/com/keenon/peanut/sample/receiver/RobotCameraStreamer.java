package com.keenon.peanut.sample.receiver;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Base64;
import android.util.Log;

import androidx.core.content.ContextCompat;

import org.java_websocket.WebSocket;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * Captures Camera1 preview frames, encodes JPEG, sends JSON to a single WebSocket client.
 * Protocol matches RemoteController {@code CameraActivity}:
 * {@code {"type":"camera_frame","camera_id":n,"width":w,"height":h,"jpeg":"<base64>"}}
 */
public final class RobotCameraStreamer {

    private static final String TAG = "RobotCameraStreamer";
    private static final int JPEG_QUALITY = 65;
    /** Minimum time between encoded frames (limits bandwidth / UI load on phone). */
    private static final int MIN_FRAME_INTERVAL_MS = 100;

    private final HandlerThread cameraThread;
    private final Handler cameraHandler;

    private Camera camera;
    private SurfaceTexture surfaceTexture;
    private int previewWidth;
    private int previewHeight;
    private volatile WebSocket activeRecipient;
    private volatile int activeCameraId = -1;
    private long lastFrameWallTime;

    public RobotCameraStreamer() {
        cameraThread = new HandlerThread("RobotCamera");
        cameraThread.start();
        cameraHandler = new Handler(cameraThread.getLooper());
    }

    public int getCameraCount() {
        return Camera.getNumberOfCameras();
    }

    /**
     * Stop streaming if this socket was receiving frames.
     */
    public void onWebSocketClosed(WebSocket conn) {
        cameraHandler.post(() -> {
            if (conn != null && conn == activeRecipient) {
                releaseCameraLocked();
            }
        });
    }

    public void stop(Context appContext) {
        cameraHandler.post(this::releaseCameraLocked);
    }

    public void release() {
        cameraHandler.post(() -> {
            releaseCameraLocked();
            cameraThread.quitSafely();
        });
    }

    /**
     * @param switchAck if true, send {@code ACK:CAMERA_SWITCH:id}; else {@code ACK:CAMERA_START:id}
     */
    public void startCamera(Context appContext, int cameraId, WebSocket recipient, boolean switchAck) {
        cameraHandler.post(() -> {
            if (appContext == null || recipient == null) {
                return;
            }
            Context ctx = appContext.getApplicationContext();
            if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) {
                safeSend(recipient, "ERR:CAMERA:PERMISSION_DENIED");
                Log.w(TAG, "CAMERA permission not granted");
                return;
            }
            if (cameraId < 0 || cameraId >= Camera.getNumberOfCameras()) {
                safeSend(recipient, "ERR:CAMERA:INVALID_ID:" + cameraId);
                return;
            }

            releaseCameraLocked();

            Camera cam;
            try {
                cam = Camera.open(cameraId);
            } catch (RuntimeException e) {
                Log.e(TAG, "Camera.open failed for id " + cameraId, e);
                safeSend(recipient, "ERR:CAMERA_OPEN:" + cameraId);
                return;
            }

            try {
                // Some robot camera HAL builds return empty/invalid parameters and can crash
                // mediaserver on setParameters. Keep defaults when parameter probing fails.
                Camera.Parameters params = safeGetParameters(cam);
                if (params != null) {
                    Camera.Size size = choosePreviewSize(params.getSupportedPreviewSizes());
                    previewWidth = size.width;
                    previewHeight = size.height;
                    params.setPreviewSize(previewWidth, previewHeight);
                    params.setPreviewFormat(ImageFormat.NV21);
                    safeSetParameters(cam, params);
                }
                if (previewWidth <= 0 || previewHeight <= 0) {
                    Camera.Size currentSize = safeGetCurrentPreviewSize(cam);
                    if (currentSize != null) {
                        previewWidth = currentSize.width;
                        previewHeight = currentSize.height;
                    } else {
                        previewWidth = 640;
                        previewHeight = 480;
                    }
                }

                surfaceTexture = new SurfaceTexture(10);
                surfaceTexture.setDefaultBufferSize(previewWidth, previewHeight);
                cam.setPreviewTexture(surfaceTexture);
            } catch (IOException | RuntimeException e) {
                Log.e(TAG, "Camera configure failed", e);
                cam.release();
                safeSend(recipient, "ERR:CAMERA_CONFIGURE:" + cameraId);
                return;
            }

            activeRecipient = recipient;
            activeCameraId = cameraId;
            lastFrameWallTime = 0L;

            camera = cam;
            cam.setPreviewCallback((data, c) -> onPreviewFrame(data, c, cameraId));

            try {
                cam.startPreview();
            } catch (RuntimeException e) {
                Log.e(TAG, "startPreview failed", e);
                releaseCameraLocked();
                safeSend(recipient, "ERR:CAMERA_PREVIEW:" + cameraId);
                return;
            }

            if (switchAck) {
                safeSend(recipient, "ACK:CAMERA_SWITCH:" + cameraId);
            } else {
                safeSend(recipient, "ACK:CAMERA_START:" + cameraId);
            }
            Log.i(TAG, "Camera " + cameraId + " streaming " + previewWidth + "x" + previewHeight);
        });
    }

    private void onPreviewFrame(byte[] data, Camera cam, int cameraId) {
        if (data == null || cam != camera || activeRecipient == null) {
            return;
        }
        WebSocket recipient = activeRecipient;
        if (!recipient.isOpen()) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now - lastFrameWallTime < MIN_FRAME_INTERVAL_MS) {
            return;
        }
        lastFrameWallTime = now;

        final int w = previewWidth;
        final int h = previewHeight;
        cameraHandler.post(() -> encodeJpegAndSend(data, w, h, cameraId, recipient));
    }

    private void encodeJpegAndSend(byte[] nv21, int width, int height, int cameraId, WebSocket recipient) {
        if (recipient == null || !recipient.isOpen() || nv21 == null) {
            return;
        }
        try {
            YuvImage yuv = new YuvImage(nv21, ImageFormat.NV21, width, height, null);
            ByteArrayOutputStream baos = new ByteArrayOutputStream(nv21.length / 4);
            yuv.compressToJpeg(new Rect(0, 0, width, height), JPEG_QUALITY, baos);
            byte[] jpeg = baos.toByteArray();
            String b64 = Base64.encodeToString(jpeg, Base64.NO_WRAP);

            JSONObject o = new JSONObject();
            o.put("type", "camera_frame");
            o.put("camera_id", cameraId);
            o.put("width", width);
            o.put("height", height);
            o.put("jpeg", b64);
            recipient.send(o.toString());
        } catch (Exception e) {
            Log.e(TAG, "encode/send frame failed", e);
        }
    }

    private void releaseCameraLocked() {
        if (camera != null) {
            try {
                camera.setPreviewCallback(null);
            } catch (Exception ignored) {
            }
            try {
                camera.stopPreview();
            } catch (Exception ignored) {
            }
            try {
                camera.release();
            } catch (Exception ignored) {
            }
            camera = null;
        }
        if (surfaceTexture != null) {
            try {
                surfaceTexture.release();
            } catch (Exception ignored) {
            }
            surfaceTexture = null;
        }
        activeRecipient = null;
        activeCameraId = -1;
    }

    private static Camera.Size choosePreviewSize(List<Camera.Size> sizes) {
        if (sizes == null || sizes.isEmpty()) {
            throw new IllegalStateException("No preview sizes");
        }
        Camera.Size best = null;
        for (Camera.Size s : sizes) {
            if (s.width <= 640 && s.height <= 480) {
                if (best == null || (s.width * s.height) > (best.width * best.height)) {
                    best = s;
                }
            }
        }
        if (best != null) {
            return best;
        }
        // Fallback: smallest size to reduce bandwidth
        best = sizes.get(0);
        for (Camera.Size s : sizes) {
            if (s.width * s.height < best.width * best.height) {
                best = s;
            }
        }
        return best;
    }

    private static Camera.Parameters safeGetParameters(Camera camera) {
        try {
            return camera.getParameters();
        } catch (RuntimeException e) {
            Log.w(TAG, "getParameters failed, fallback to HAL defaults", e);
            return null;
        }
    }

    private static void safeSetParameters(Camera camera, Camera.Parameters params) {
        try {
            camera.setParameters(params);
        } catch (RuntimeException e) {
            Log.w(TAG, "setParameters failed, fallback to HAL defaults", e);
        }
    }

    private static Camera.Size safeGetCurrentPreviewSize(Camera camera) {
        try {
            Camera.Parameters params = camera.getParameters();
            if (params != null) {
                return params.getPreviewSize();
            }
        } catch (RuntimeException e) {
            Log.w(TAG, "Unable to read fallback preview size", e);
        }
        return null;
    }

    private static void safeSend(WebSocket conn, String payload) {
        if (conn == null || payload == null) {
            return;
        }
        try {
            if (conn.isOpen()) {
                conn.send(payload);
            }
        } catch (Exception e) {
            Log.e(TAG, "send failed", e);
        }
    }
}
