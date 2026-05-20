package com.keenon.peanut.supermarket.fragment;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Typeface;
import android.hardware.Camera;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.keenon.peanut.sample.R;
import com.keenon.peanut.supermarket.vision.TrayCountingHelper;
import com.keenon.peanut.supermarket.widget.TrayRoiOverlayView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Shared OpenCV tray counting (Camera1 + {@link SurfaceView} + {@link TrayCountingHelper}) used by
 * {@link Count2Fragment} and optionally {@link CountFragment} when the user picks OpenCV engine.
 */
public final class OpenCvTrayCountHelper implements SurfaceHolder.Callback {

    private static final int TRAY_CAMERA_COUNT = 3;
    private static final long DETECT_INTERVAL_MS = 300L;

    private static final class FrameRoi {
        final int cx, cy, rx, ry, frameW, frameH;

        FrameRoi(int cx, int cy, int rx, int ry, int frameW, int frameH) {
            this.cx = cx;
            this.cy = cy;
            this.rx = rx;
            this.ry = ry;
            this.frameW = frameW;
            this.frameH = frameH;
        }
    }

    private final Fragment host;
    private final String tag;
    private final int reqCamera;

    private FrameLayout previewContainer;
    private SurfaceView surfaceView;
    private TextView statusText;
    private TextView placeholder;
    private TextView bigNumber;
    private TextView statusLine;
    private LinearLayout indexRow;
    private Button btnStart;
    private Button btnStop;
    private TrayRoiOverlayView roiOverlay;
    private TextView roiPrompt;
    private LinearLayout roiRow;
    private Button btnRoiConfirm;
    private Button btnRoiReset;

    private final List<Button> indexButtons = new ArrayList<>();
    private FrameRoi[] savedRoiPerCamera;
    private boolean roiConfirmed;

    private Camera camera;
    private int currentCameraId;
    private int cameraCount;
    private boolean previewing;
    private boolean surfaceReady;
    private boolean startRequested;
    private int previewWidth;
    private int previewHeight;

    private TrayCountingHelper[] trayHelpers;
    private TrayCountingHelper.BoxLayout uniformLayout = TrayCountingHelper.BoxLayout.GRID_2x2;

    private HandlerThread detectThread;
    private Handler detectHandler;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final AtomicBoolean detectBusy = new AtomicBoolean(false);
    private long lastDetectAt;

    public OpenCvTrayCountHelper(@NonNull Fragment host, @NonNull String logTag, int requestCameraCode) {
        this.host = host;
        this.tag = logTag;
        this.reqCamera = requestCameraCode;
    }

    /** Inflate-independent: call from {@code Fragment#onViewCreated(View, Bundle)}. */
    public void bindViews(@NonNull View root) {
        previewContainer = root.findViewById(R.id.count_preview_container);
        surfaceView = root.findViewById(R.id.count_surface);
        statusText = root.findViewById(R.id.count_status);
        placeholder = root.findViewById(R.id.count_placeholder);
        bigNumber = root.findViewById(R.id.count_big_number);
        statusLine = root.findViewById(R.id.count_status_line);
        indexRow = root.findViewById(R.id.count_index_row);
        roiOverlay = root.findViewById(R.id.count_roi_overlay);
        roiPrompt = root.findViewById(R.id.count_roi_prompt);
        roiRow = root.findViewById(R.id.count_roi_row);
        btnRoiConfirm = root.findViewById(R.id.btn_count_roi_confirm);
        btnRoiReset = root.findViewById(R.id.btn_count_roi_reset);
        btnStart = root.findViewById(R.id.btn_count_start);
        btnStop = root.findViewById(R.id.btn_count_stop);

        savedRoiPerCamera = new FrameRoi[TRAY_CAMERA_COUNT];

        if (roiOverlay != null) {
            roiOverlay.setListener(new TrayRoiOverlayView.Listener() {
                @Override
                public void onStepChanged(TrayRoiOverlayView.TapStep step) {
                    updateRoiPromptForStep(step);
                    if (btnRoiConfirm != null) {
                        btnRoiConfirm.setEnabled(step == TrayRoiOverlayView.TapStep.DONE
                                && roiOverlay.getEllipse() != null);
                    }
                }

                @Override
                public void onEllipseReady(TrayRoiOverlayView.Ellipse ellipse) {
                }
            });
        }
        if (btnRoiConfirm != null) btnRoiConfirm.setOnClickListener(v -> confirmRoi());
        if (btnRoiReset != null) btnRoiReset.setOnClickListener(v -> resetRoi());

        recreateTrayHelpers();
        if (surfaceView != null) {
            surfaceView.getHolder().addCallback(this);
        }

        cameraCount = Math.max(1, Camera.getNumberOfCameras());
        rebuildIndexButtons();
    }

    /** Same slot grid for all tray cameras (0–2). */
    public void setUniformSlotLayout(@NonNull TrayCountingHelper.BoxLayout layout) {
        uniformLayout = layout;
        if (roiOverlay != null && roiConfirmed) {
            roiOverlay.setSlotGrid(layout.rows, layout.cols);
        }
        recreateTrayHelpers();
        if (previewing && roiConfirmed) {
            for (int i = 0; i < TRAY_CAMERA_COUNT; i++) {
                FrameRoi r = savedRoiPerCamera[i];
                if (r != null && trayHelpers != null && trayHelpers[i] != null) {
                    trayHelpers[i].setRoiEllipse(r.frameW, r.frameH, r.cx, r.cy, r.rx, r.ry);
                }
            }
        }
    }

    @NonNull
    public TrayCountingHelper.BoxLayout getUniformLayout() {
        return uniformLayout;
    }

    private void recreateTrayHelpers() {
        if (trayHelpers != null) {
            for (TrayCountingHelper h : trayHelpers) {
                if (h != null) h.close();
            }
        }
        trayHelpers = new TrayCountingHelper[TRAY_CAMERA_COUNT];
        for (int i = 0; i < TRAY_CAMERA_COUNT; i++) {
            trayHelpers[i] = new TrayCountingHelper(i, uniformLayout);
            FrameRoi r = savedRoiPerCamera != null ? savedRoiPerCamera[i] : null;
            if (r != null) {
                trayHelpers[i].setRoiEllipse(r.frameW, r.frameH, r.cx, r.cy, r.rx, r.ry);
            }
        }
    }

    public void destroy() {
        stopPreview();
        if (surfaceView != null) {
            try {
                surfaceView.getHolder().removeCallback(this);
            } catch (Exception ignored) {
            }
        }
        if (trayHelpers != null) {
            for (TrayCountingHelper h : trayHelpers) {
                if (h != null) h.close();
            }
            trayHelpers = null;
        }
    }

    public void onPause() {
        stopPreview();
    }

    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == reqCamera) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                requestStart();
            } else {
                Toast.makeText(host.requireContext(), R.string.cam_no_permission, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void rebuildIndexButtons() {
        if (indexRow == null) return;
        indexRow.removeAllViews();
        indexButtons.clear();
        float d = host.getResources().getDisplayMetrics().density;
        int dpMargin = (int) (6 * d);
        int exposed = Math.min(cameraCount, TRAY_CAMERA_COUNT);
        for (int i = 0; i < exposed; i++) {
            Button b = new Button(host.getContext());
            b.setText(String.valueOf(i));
            b.setTypeface(b.getTypeface(), Typeface.BOLD);
            LinearLayout.LayoutParams lp =
                    new LinearLayout.LayoutParams((int) (64 * d), LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMargins(i == 0 ? 0 : dpMargin, 0, 0, 0);
            b.setLayoutParams(lp);
            final int idx = i;
            b.setOnClickListener(v -> onSelectCamera(idx));
            indexRow.addView(b);
            indexButtons.add(b);
        }
        highlightActiveIndex();
    }

    private void highlightActiveIndex() {
        for (int i = 0; i < indexButtons.size(); i++) {
            Button b = indexButtons.get(i);
            if (i == currentCameraId && previewing) {
                b.setBackgroundColor(Color.parseColor("#F59E0B"));
                b.setTextColor(Color.BLACK);
            } else {
                b.setBackground(null);
                b.setTextColor(Color.WHITE);
            }
        }
    }

    public boolean isPreviewing() {
        return previewing;
    }

    public int getCurrentCameraId() {
        return currentCameraId;
    }

    /** Switch tray camera while previewing or update selection when idle. */
    public void selectTrayCamera(int index) {
        onSelectCamera(index);
    }

    private void onSelectCamera(int index) {
        if (index == currentCameraId && previewing) return;
        currentCameraId = index;
        roiConfirmed = false;
        if (previewing) {
            stopPreview();
            startRequested = true;
            startPreview();
        } else {
            highlightActiveIndex();
            if (statusText != null) {
                statusText.setText(host.getString(R.string.cam_status_running, index) + " (tap Start to preview)");
            }
        }
    }

    private void enterRoiSelection() {
        roiConfirmed = false;
        FrameRoi saved = (currentCameraId >= 0 && currentCameraId < TRAY_CAMERA_COUNT)
                ? savedRoiPerCamera[currentCameraId] : null;
        if (saved != null && previewWidth > 0 && previewHeight > 0 && roiOverlay != null) {
            TrayRoiOverlayView.Ellipse e = frameToView(saved);
            if (e != null) {
                roiOverlay.restoreEllipse(e, true);
                roiOverlay.setVisibility(View.VISIBLE);
                roiOverlay.setSlotGrid(uniformLayout.rows, uniformLayout.cols);
                applyRoiToActiveHelper(saved);
                roiConfirmed = true;
                if (roiRow != null) roiRow.setVisibility(View.GONE);
                if (roiPrompt != null) roiPrompt.setVisibility(View.GONE);
                return;
            }
        }
        if (roiOverlay != null) {
            roiOverlay.resetTaps();
            roiOverlay.setVisibility(View.VISIBLE);
        }
        if (roiRow != null) roiRow.setVisibility(View.VISIBLE);
        if (btnRoiConfirm != null) btnRoiConfirm.setEnabled(false);
        if (roiPrompt != null) {
            roiPrompt.setVisibility(View.VISIBLE);
            updateRoiPromptForStep(TrayRoiOverlayView.TapStep.TOP);
        }
    }

    private void exitRoiSelection() {
        if (roiOverlay != null) roiOverlay.setVisibility(View.GONE);
        if (roiRow != null) roiRow.setVisibility(View.GONE);
        if (roiPrompt != null) roiPrompt.setVisibility(View.GONE);
    }

    private void confirmRoi() {
        if (roiOverlay == null) return;
        TrayRoiOverlayView.Ellipse e = roiOverlay.getEllipse();
        if (e == null) return;
        FrameRoi frameRoi = viewToFrame(e);
        if (frameRoi == null) return;
        savedRoiPerCamera[currentCameraId] = frameRoi;
        applyRoiToActiveHelper(frameRoi);
        roiOverlay.lockEllipse();
        roiOverlay.setSlotGrid(uniformLayout.rows, uniformLayout.cols);
        if (roiRow != null) roiRow.setVisibility(View.GONE);
        if (roiPrompt != null) roiPrompt.setVisibility(View.GONE);
        roiConfirmed = true;
        if (statusText != null) statusText.setText("Tray shape set — calibrating…");
    }

    private void resetRoi() {
        if (currentCameraId >= 0 && currentCameraId < TRAY_CAMERA_COUNT) {
            savedRoiPerCamera[currentCameraId] = null;
        }
        roiConfirmed = false;
        if (trayHelpers != null && currentCameraId >= 0 && currentCameraId < trayHelpers.length
                && trayHelpers[currentCameraId] != null) {
            trayHelpers[currentCameraId].clearRoi();
            trayHelpers[currentCameraId].reset();
        }
        if (bigNumber != null) bigNumber.setText("--");
        if (statusLine != null) {
            statusLine.setVisibility(View.GONE);
            statusLine.setText("");
        }
        if (roiOverlay != null) {
            roiOverlay.resetTaps();
            roiOverlay.setVisibility(View.VISIBLE);
        }
        if (roiRow != null) roiRow.setVisibility(View.VISIBLE);
        if (btnRoiConfirm != null) btnRoiConfirm.setEnabled(false);
        if (roiPrompt != null) {
            roiPrompt.setVisibility(View.VISIBLE);
            updateRoiPromptForStep(TrayRoiOverlayView.TapStep.TOP);
        }
    }

    private void updateRoiPromptForStep(TrayRoiOverlayView.TapStep step) {
        if (roiPrompt == null) return;
        int id;
        switch (step) {
            case TOP:
                id = R.string.count_roi_prompt_top;
                break;
            case BOTTOM:
                id = R.string.count_roi_prompt_bottom;
                break;
            case LEFT:
                id = R.string.count_roi_prompt_left;
                break;
            case RIGHT:
                id = R.string.count_roi_prompt_right;
                break;
            case DONE:
            default:
                id = R.string.count_roi_prompt_done;
                break;
        }
        roiPrompt.setText(id);
    }

    @Nullable
    private FrameRoi viewToFrame(TrayRoiOverlayView.Ellipse e) {
        if (roiOverlay == null) return null;
        int viewW = roiOverlay.getWidth();
        int viewH = roiOverlay.getHeight();
        if (viewW <= 0 || viewH <= 0 || previewWidth <= 0 || previewHeight <= 0) return null;
        float sx = (float) previewWidth / viewW;
        float sy = (float) previewHeight / viewH;
        return new FrameRoi(
                Math.round(e.cx * sx),
                Math.round(e.cy * sy),
                Math.max(1, Math.round(e.rx * sx)),
                Math.max(1, Math.round(e.ry * sy)),
                previewWidth, previewHeight);
    }

    @Nullable
    private TrayRoiOverlayView.Ellipse frameToView(FrameRoi r) {
        if (roiOverlay == null) return null;
        int viewW = roiOverlay.getWidth();
        int viewH = roiOverlay.getHeight();
        if (viewW <= 0 || viewH <= 0 || r.frameW <= 0 || r.frameH <= 0) return null;
        float sx = (float) viewW / r.frameW;
        float sy = (float) viewH / r.frameH;
        return new TrayRoiOverlayView.Ellipse(r.cx * sx, r.cy * sy, r.rx * sx, r.ry * sy);
    }

    private void applyRoiToActiveHelper(FrameRoi r) {
        if (trayHelpers == null) return;
        if (currentCameraId < 0 || currentCameraId >= TRAY_CAMERA_COUNT) return;
        TrayCountingHelper h = trayHelpers[currentCameraId];
        if (h == null) return;
        h.setRoiEllipse(r.frameW, r.frameH, r.cx, r.cy, r.rx, r.ry);
    }

    private void requestStart() {
        if (ContextCompat.checkSelfPermission(host.requireContext(), Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            host.requestPermissions(new String[]{Manifest.permission.CAMERA}, reqCamera);
            return;
        }
        startRequested = true;
        if (surfaceReady) {
            startPreview();
        } else if (statusText != null) {
            statusText.setText("Waiting for preview surface…");
        }
    }

    /** Called by host fragment when user taps Start. */
    public void userRequestStart() {
        requestStart();
    }

    /** Called by host fragment when user taps Stop. */
    public void userStopPreview() {
        stopPreview();
    }

    private void startPreview() {
        if (previewing || surfaceView == null) return;
        SurfaceHolder holder = surfaceView.getHolder();
        if (holder.getSurface() == null) return;
        if (currentCameraId < 0 || currentCameraId >= Camera.getNumberOfCameras()) {
            Toast.makeText(host.requireContext(), "Invalid camera id " + currentCameraId, Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            camera = Camera.open(currentCameraId);

            camera.setErrorCallback((error, cam) -> {
                Log.e(tag, "Camera hardware error: " + error + " on camera " + currentCameraId);
                mainHandler.post(() -> {
                    String msg = error == Camera.CAMERA_ERROR_SERVER_DIED
                            ? "Camera " + currentCameraId + " HAL crashed (try a different camera index)"
                            : "Camera hardware error " + error;
                    Toast.makeText(host.requireContext(), msg, Toast.LENGTH_LONG).show();
                    releaseCamera();
                    stopPreview();
                });
            });

            Camera.Parameters params = safeGetParameters(camera);
            if (params == null) {
                Log.w(tag, "Camera " + currentCameraId + " returned empty parameters — skipping");
                Toast.makeText(host.requireContext(),
                        "Camera " + currentCameraId + " unavailable. Try a different index.",
                        Toast.LENGTH_LONG).show();
                releaseCamera();
                return;
            }
            Camera.Size size = chooseSize(params.getSupportedPreviewSizes());
            if (size != null) {
                params.setPreviewSize(size.width, size.height);
                previewWidth = size.width;
                previewHeight = size.height;
            }
            List<String> modes = params.getSupportedFocusModes();
            if (modes != null && modes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
                params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
            }
            params.setPreviewFormat(ImageFormat.NV21);
            safeSetParameters(camera, params);

            if (previewWidth <= 0 || previewHeight <= 0) {
                Camera.Size activeSize = safeGetCurrentPreviewSize(camera);
                if (activeSize != null) {
                    previewWidth = activeSize.width;
                    previewHeight = activeSize.height;
                } else {
                    previewWidth = 640;
                    previewHeight = 480;
                }
            }
            Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
            Camera.getCameraInfo(currentCameraId, cameraInfo);
            camera.setDisplayOrientation(cameraInfo.orientation);
            camera.setPreviewDisplay(holder);

            startDetectThread();
            attachDetectionCallback();

            camera.startPreview();
            previewing = true;
            if (placeholder != null) placeholder.setVisibility(View.GONE);
            if (btnStart != null) btnStart.setEnabled(false);
            if (btnStop != null) btnStop.setEnabled(true);
            if (statusText != null) {
                statusText.setText(host.getString(R.string.cam_status_running, currentCameraId)
                        + " · " + previewWidth + "×" + previewHeight + " · OpenCV");
            }
            resizeSurfaceToAspect();
            highlightActiveIndex();
            enterRoiSelection();
        } catch (IOException | RuntimeException e) {
            Log.e(tag, "startPreview failed", e);
            Toast.makeText(host.requireContext(), "Camera open failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
            releaseCamera();
            highlightActiveIndex();
        }
    }

    public void stopPreview() {
        startRequested = false;
        releaseCamera();
        if (placeholder != null) placeholder.setVisibility(View.VISIBLE);
        if (btnStart != null) btnStart.setEnabled(true);
        if (btnStop != null) btnStop.setEnabled(false);
        if (statusText != null) statusText.setText(R.string.cam_status_idle);
        highlightActiveIndex();
    }

    private void releaseCamera() {
        if (camera != null) {
            try {
                camera.setPreviewCallbackWithBuffer(null);
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
        previewing = false;
        stopDetectThread();
        if (bigNumber != null) bigNumber.setText("--");
        if (statusLine != null) {
            statusLine.setVisibility(View.GONE);
            statusLine.setText("");
        }
        if (roiOverlay != null) exitRoiSelection();
        roiConfirmed = false;
    }

    private void startDetectThread() {
        if (detectThread != null) return;
        detectThread = new HandlerThread(tag);
        detectThread.start();
        detectHandler = new Handler(detectThread.getLooper());
        lastDetectAt = 0L;
    }

    private void stopDetectThread() {
        if (detectHandler != null) {
            detectHandler.removeCallbacksAndMessages(null);
            detectHandler = null;
        }
        if (detectThread != null) {
            detectThread.quitSafely();
            detectThread = null;
        }
        detectBusy.set(false);
    }

    private void attachDetectionCallback() {
        if (camera == null) return;
        int bufSize = previewWidth * previewHeight * 3 / 2;
        camera.addCallbackBuffer(new byte[bufSize]);
        camera.addCallbackBuffer(new byte[bufSize]);
        camera.setPreviewCallbackWithBuffer(this::onPreviewFrame);
    }

    private void onPreviewFrame(byte[] data, Camera cam) {
        if (data == null || cam == null) return;
        long now = SystemClock.uptimeMillis();
        if (detectBusy.get() || now - lastDetectAt < DETECT_INTERVAL_MS) {
            try {
                cam.addCallbackBuffer(data);
            } catch (Exception ignored) {
            }
            return;
        }
        detectBusy.set(true);
        lastDetectAt = now;
        final int w = previewWidth;
        final int h = previewHeight;
        final Handler h2 = detectHandler;
        if (h2 == null) {
            detectBusy.set(false);
            try {
                cam.addCallbackBuffer(data);
            } catch (Exception ignored) {
            }
            return;
        }
        h2.post(() -> {
            TrayCountingHelper.Result r = null;
            if (roiConfirmed
                    && currentCameraId >= 0 && currentCameraId < TRAY_CAMERA_COUNT
                    && trayHelpers != null && trayHelpers[currentCameraId] != null) {
                r = trayHelpers[currentCameraId].process(data, w, h);
            }
            final TrayCountingHelper.Result result = r;
            mainHandler.post(() -> renderResult(result));
            if (camera != null) {
                try {
                    camera.addCallbackBuffer(data);
                } catch (Exception ignored) {
                }
            }
            detectBusy.set(false);
        });
    }

    private void renderResult(@Nullable TrayCountingHelper.Result r) {
        if (!host.isAdded()) return;
        if (r == null) {
            if (bigNumber != null) bigNumber.setText("--");
            if (statusLine != null) statusLine.setVisibility(View.GONE);
            return;
        }
        if (r.calibrating) {
            if (bigNumber != null) bigNumber.setText("--");
            if (statusLine != null) {
                statusLine.setText(r.overlay);
                statusLine.setVisibility(View.VISIBLE);
            }
        } else {
            if (bigNumber != null) bigNumber.setText(r.count + " / " + r.maxSlots);
            if (statusLine != null) {
                if (r.overlay != null && r.overlay.contains("error")) {
                    statusLine.setText(r.overlay);
                    statusLine.setVisibility(View.VISIBLE);
                } else if (r.event != TrayCountingHelper.EventType.NONE) {
                    String verb = r.event == TrayCountingHelper.EventType.DELIVERED ? "Delivered" : "Picked up";
                    statusLine.setText(verb + " " + r.eventDelta);
                    statusLine.setVisibility(View.VISIBLE);
                } else {
                    statusLine.setVisibility(View.GONE);
                }
            }
        }
    }

    private void resizeSurfaceToAspect() {
        if (previewWidth <= 0 || previewHeight <= 0 || previewContainer == null || surfaceView == null) return;
        previewContainer.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        int w = previewContainer.getWidth();
                        int h = previewContainer.getHeight();
                        if (w <= 0 || h <= 0) return;
                        previewContainer.getViewTreeObserver().removeOnGlobalLayoutListener(this);

                        float camAspect = (float) previewWidth / previewHeight;
                        int targetW;
                        int targetH;
                        if (w / (float) h > camAspect) {
                            targetH = h;
                            targetW = Math.round(h * camAspect);
                        } else {
                            targetW = w;
                            targetH = Math.round(w / camAspect);
                        }
                        ViewGroup.LayoutParams lp = surfaceView.getLayoutParams();
                        lp.width = targetW;
                        lp.height = targetH;
                        surfaceView.setLayoutParams(lp);
                    }
                });
        previewContainer.requestLayout();
    }

    private Camera.Size chooseSize(List<Camera.Size> sizes) {
        if (sizes == null || sizes.isEmpty()) return null;
        Camera.Size best = null;
        for (Camera.Size s : sizes) {
            if (s.width <= 1280 && s.height <= 960) {
                if (best == null || (s.width * s.height) > (best.width * best.height)) {
                    best = s;
                }
            }
        }
        return best != null ? best : sizes.get(0);
    }

    private Camera.Parameters safeGetParameters(Camera camera) {
        try {
            return camera.getParameters();
        } catch (RuntimeException e) {
            Log.w(tag, "getParameters failed", e);
            return null;
        }
    }

    private void safeSetParameters(Camera camera, Camera.Parameters params) {
        try {
            camera.setParameters(params);
        } catch (RuntimeException e) {
            Log.w(tag, "setParameters failed, continue with defaults", e);
        }
    }

    private Camera.Size safeGetCurrentPreviewSize(Camera camera) {
        try {
            Camera.Parameters params = camera.getParameters();
            if (params != null) {
                return params.getPreviewSize();
            }
        } catch (RuntimeException e) {
            Log.w(tag, "Unable to read current preview size", e);
        }
        return null;
    }

    @Override
    public void surfaceCreated(@NonNull SurfaceHolder holder) {
        surfaceReady = true;
        if (startRequested && !previewing) {
            startPreview();
        }
    }

    @Override
    public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {
        if (camera != null) {
            try {
                camera.stopPreview();
                camera.setPreviewDisplay(holder);
                camera.startPreview();
            } catch (Exception e) {
                Log.e(tag, "surfaceChanged preview restart failed", e);
            }
        }
    }

    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
        surfaceReady = false;
        releaseCamera();
    }
}
