package com.keenon.peanut.supermarket.fragment;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.graphics.Typeface;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.Date;
import java.util.Deque;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.keenon.peanut.sample.R;
import com.keenon.peanut.supermarket.vision.TrayCountingHelper;
import com.keenon.peanut.supermarket.vision.TrayPlateCounter;
import com.keenon.peanut.supermarket.widget.TrayRoiOverlayView;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Developer "Count" tab.
 *
 * <p><strong>NCNN</strong> uses the canpan YOLO model (same family as production). <strong>OpenCV</strong>
 * uses classical grid / absdiff counting (same algorithm as Count2) — no neural network on that path.
 * NCNN can crash on some RK boards when JNI runs concurrently; OpenCV avoids {@code libncnn} entirely.</p>
 *
 * <p>Camera: <strong>Android Camera1 only</strong> — {@link android.hardware.Camera} with a
 * {@link android.view.TextureView} and {@code setPreviewTexture} so NV21 preview callbacks work on
 * RockChip (same pattern as {@link com.keenon.peanut.sample.receiver.RobotCameraStreamer} and the
 * YOLO tab’s Android camera tray path). Ceiling tray indices <strong>0, 1, 2</strong> map to Android
 * camera ids 0–2; Keenon {@code KNCameraHelper} / {@code /dev/video*} is <strong>not</strong> used here
 * so Count does not compete with native camera apps.</p>
 *
 * How background is rejected (mirrors KeenonDiner KNPlateFilter exactly):
 *   - canpan_detection was trained on ceiling-camera tray images → biased against non-tray objects
 *   - After YOLO runs, any box whose centre is outside a circle (324.9, 229.22) r=265 is discarded
 *   - Only label=1 ("plate") is counted; label=0 ("ros_empty") is ignored
 *
 * The CIRCLE sliders let you adjust the tray circle if your tray size/position differs.
 */
public class CountFragment extends Fragment implements TextureView.SurfaceTextureListener {

    private static final String TAG = "CountFragment";
    private static final int REQ_CAMERA = 2202;
    private static final int REQ_CAMERA_OPENCV = 2205;

    private static final int ENGINE_NCNN = 0;
    private static final int ENGINE_OPENCV = 1;

    private static final long DETECT_INTERVAL_MS = 500L;
    private static final int  TRAY_CAMERA_COUNT  = 3;
    /** RockChip HAL needs time between release() and open() on another index. */
    private static final long CAMERA_HAL_SETTLE_MS = 700L;

    // ---- UI ----
    private FrameLayout  previewContainer;
    private TextureView  texturePreview;
    private TextView     statusText;
    private TextView     placeholder;
    private TextView     bigNumber;
    private TextView     statusLine;
    private LinearLayout indexRow;
    private Button       btnStart, btnStop, btnCrop, btnLogs;
    private LinearLayout roiPanel;
    // Circle sliders: CX, CY as % of frame (0–100), Radius as % of frame width (0–60)
    private SeekBar      seekCx, seekCy, seekRadius, seekSens;
    private TextView     txtCx, txtCy, txtRadius, txtSens;
    private View         roiBorder;
    private ScrollView   logsScroll;
    private TextView     logsText;
    private Spinner      countEngineSpinner;
    private SurfaceView  countSurface;
    private TrayRoiOverlayView countRoiOverlay;
    private View         countOcvGridScroll;
    private RadioGroup   countOcvGridGroup;
    private OpenCvTrayCountHelper openCvHelper;
    private boolean      suppressEngineSpinner;
    private int          lastEngineSelection = ENGINE_NCNN;
    private boolean      logsVisible = false;
    private final Deque<String> logBuffer = new ArrayDeque<>();
    private static final int LOG_MAX_LINES = 80;
    private final SimpleDateFormat logTimeFmt = new SimpleDateFormat("HH:mm:ss.SSS", java.util.Locale.US);
    private int frameSeq = 0;

    private boolean isOpenCvEngine() {
        return lastEngineSelection == ENGINE_OPENCV;
    }

    private void applyEngineUi() {
        boolean cv = isOpenCvEngine();
        if (texturePreview != null) {
            texturePreview.setVisibility(cv ? View.GONE : View.VISIBLE);
        }
        if (countSurface != null) {
            countSurface.setVisibility(cv ? View.VISIBLE : View.GONE);
        }
        if (countRoiOverlay != null && !cv) {
            countRoiOverlay.setVisibility(View.GONE);
        }
        if (countOcvGridScroll != null) {
            countOcvGridScroll.setVisibility(cv ? View.VISIBLE : View.GONE);
        }
        if (roiPanel != null) {
            if (cv) {
                roiPanel.setVisibility(View.GONE);
            } else {
                roiPanel.setVisibility(roiPanelVisible ? View.VISIBLE : View.GONE);
            }
        }
        if (btnCrop != null) {
            btnCrop.setVisibility(cv ? View.GONE : View.VISIBLE);
        }
        if (!cv && openCvHelper != null) {
            currentCameraId = openCvHelper.getCurrentCameraId();
        }
    }

    @Nullable
    private static TrayCountingHelper.BoxLayout layoutForOcvGridId(int checkedId) {
        if (checkedId == R.id.count_ocv_grid_2x2) return TrayCountingHelper.BoxLayout.GRID_2x2;
        if (checkedId == R.id.count_ocv_grid_3x3) return TrayCountingHelper.BoxLayout.GRID_3x3;
        if (checkedId == R.id.count_ocv_grid_4x4) return TrayCountingHelper.BoxLayout.GRID_4x4;
        if (checkedId == R.id.count_ocv_grid_2x3) return TrayCountingHelper.BoxLayout.GRID_2x3;
        return null;
    }

    private final List<Button> indexButtons = new java.util.ArrayList<>();

    // ---- Camera ----
    private Camera  camera;
    private int     currentCameraId = 0;
    private int     cameraCount;
    private boolean previewing, surfaceReady, startRequested;
    private int     previewWidth  = 640;
    private int     previewHeight = 480;
    /** Bumped on release/switch so stale HAL error callbacks are ignored. */
    private int     cameraSession = 0;
    private Runnable pendingCameraSwitch;

    // ---- Detection ----
    /** Single shared NCNN model for all tray cameras (same canpan_detection weights). */
    private TrayPlateCounter plateCounter;
    private HandlerThread detectThread;
    private Handler       detectHandler;
    private final Handler mainHandler  = new Handler(Looper.getMainLooper());
    /** Blocks overlapping NCNN calls — libyolov5 crashes on re-entry (mirrors YoloFragment). */
    private final AtomicBoolean detectInFlight = new AtomicBoolean(false);
    private long lastDetectAt;
    private volatile boolean modelReady = false;

    // ---- Circle ROI state (in 640×480 pixel space) ----
    // Default matches KNModelFactory.buildPlateModel() exactly
    private float circleCx     = 324.9f;
    private float circleCy     = 229.22f;
    private float circleRadius = 265.0f;
    private boolean roiPanelVisible = false;

    // ---- Item count tracking ----
    private int lastCount = -1;

    // ---- Lifecycle ----

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_dev_count, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);

        previewContainer = v.findViewById(R.id.count_preview_container);
        texturePreview   = v.findViewById(R.id.count_preview);
        statusText       = v.findViewById(R.id.count_status);
        placeholder      = v.findViewById(R.id.count_placeholder);
        bigNumber        = v.findViewById(R.id.count_big_number);
        statusLine       = v.findViewById(R.id.count_status_line);
        indexRow         = v.findViewById(R.id.count_index_row);
        btnStart         = v.findViewById(R.id.btn_count_start);
        btnStop          = v.findViewById(R.id.btn_count_stop);
        btnCrop          = v.findViewById(R.id.btn_count_crop);
        btnLogs          = v.findViewById(R.id.btn_count_logs);
        roiPanel         = v.findViewById(R.id.count_roi_panel);
        roiBorder        = v.findViewById(R.id.count_roi_border);
        seekCx           = v.findViewById(R.id.seek_roi_left);    // re-used as CX
        seekCy           = v.findViewById(R.id.seek_roi_top);     // re-used as CY
        seekRadius       = v.findViewById(R.id.seek_roi_right);   // re-used as Radius
        seekSens         = v.findViewById(R.id.seek_roi_bottom);  // re-used as Sensitivity
        txtCx            = v.findViewById(R.id.txt_roi_left);
        txtCy            = v.findViewById(R.id.txt_roi_top);
        txtRadius        = v.findViewById(R.id.txt_roi_right);
        txtSens          = v.findViewById(R.id.txt_roi_bottom);
        logsScroll       = v.findViewById(R.id.count_logs_scroll);
        logsText         = v.findViewById(R.id.count_logs_text);

        // Configure SeekBar ranges
        if (seekCx     != null) seekCx.setMax(100);       // CX: 0–100% of frame width
        if (seekCy     != null) seekCy.setMax(100);       // CY: 0–100% of frame height
        if (seekRadius != null) seekRadius.setMax(60);    // Radius: 0–60% of frame width
        if (seekSens   != null) seekSens.setMax(80);      // Sensitivity: 0–80 (= conf 0.10–0.90)

        // Set initial positions matching KeenonDiner defaults: (324.9/640*100=50, 229.22/480*100=47, 265/640*100=41)
        if (seekCx     != null) seekCx.setProgress(50);
        if (seekCy     != null) seekCy.setProgress(47);
        if (seekRadius != null) seekRadius.setProgress(41);
        if (seekSens   != null) seekSens.setProgress(30); // default conf 0.10 + 30/100 = 0.40
        updateCircleLabels();

        plateCounter = new TrayPlateCounter(requireContext());

        if (texturePreview != null) {
            texturePreview.setSurfaceTextureListener(this);
        }
        btnStart.setOnClickListener(view -> requestStart());
        btnStop.setOnClickListener(view -> stopPreview());
        btnCrop.setOnClickListener(view -> toggleCirclePanel());
        btnLogs.setOnClickListener(view -> toggleLogsPanel());

        SeekBar.OnSeekBarChangeListener circleListener = new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean fromUser) {
                updateCircleLabels();
                if (fromUser) applyCircle();
            }
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s) { applyCircle(); }
        };
        if (seekCx     != null) seekCx.setOnSeekBarChangeListener(circleListener);
        if (seekCy     != null) seekCy.setOnSeekBarChangeListener(circleListener);
        if (seekRadius != null) seekRadius.setOnSeekBarChangeListener(circleListener);

        SeekBar.OnSeekBarChangeListener sensListener = new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean fromUser) {
                updateCircleLabels();
                if (fromUser) applySensitivity();
            }
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s) { applySensitivity(); }
        };
        if (seekSens != null) seekSens.setOnSeekBarChangeListener(sensListener);

        countEngineSpinner = v.findViewById(R.id.count_engine_spinner);
        countSurface = v.findViewById(R.id.count_surface);
        countRoiOverlay = v.findViewById(R.id.count_roi_overlay);
        countOcvGridScroll = v.findViewById(R.id.count_ocv_grid_scroll);
        countOcvGridGroup = v.findViewById(R.id.count_ocv_grid_group);

        openCvHelper = new OpenCvTrayCountHelper(this, "TrayCountOpenCV", REQ_CAMERA_OPENCV);
        openCvHelper.bindViews(v);

        if (countEngineSpinner != null) {
            ArrayAdapter<String> ad = new ArrayAdapter<>(requireContext(),
                    android.R.layout.simple_spinner_item,
                    java.util.Arrays.asList(
                            getString(R.string.count_engine_ncnn),
                            getString(R.string.count_engine_opencv)));
            ad.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            countEngineSpinner.setAdapter(ad);
            suppressEngineSpinner = true;
            countEngineSpinner.setSelection(ENGINE_NCNN);
            suppressEngineSpinner = false;
            countEngineSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                    if (suppressEngineSpinner) return;
                    boolean busy = previewing || (openCvHelper != null && openCvHelper.isPreviewing());
                    if (busy) {
                        Toast.makeText(requireContext(), R.string.count_engine_switch_hint, Toast.LENGTH_SHORT).show();
                        suppressEngineSpinner = true;
                        parent.setSelection(lastEngineSelection);
                        suppressEngineSpinner = false;
                        return;
                    }
                    lastEngineSelection = position;
                    applyEngineUi();
                }

                @Override
                public void onNothingSelected(android.widget.AdapterView<?> parent) {
                }
            });
        }

        if (countOcvGridGroup != null) {
            countOcvGridGroup.setOnCheckedChangeListener((group, checkedId) -> {
                TrayCountingHelper.BoxLayout layout = layoutForOcvGridId(checkedId);
                if (layout != null && openCvHelper != null) {
                    openCvHelper.setUniformSlotLayout(layout);
                }
            });
        }

        applyEngineUi();

        cameraCount = Math.max(1, Camera.getNumberOfCameras());
        rebuildIndexButtons();
    }

    // ---- Circle ROI panel ----

    private void toggleCirclePanel() {
        roiPanelVisible = !roiPanelVisible;
        roiPanel.setVisibility(roiPanelVisible ? View.VISIBLE : View.GONE);
        btnCrop.setText(roiPanelVisible ? "HIDE CIRCLE" : "CIRCLE");
        updateCircleOverlay();
    }

    private void updateCircleLabels() {
        int cxPct = seekCx     != null ? seekCx.getProgress()     : 50;
        int cyPct = seekCy     != null ? seekCy.getProgress()     : 47;
        int rPct  = seekRadius != null ? seekRadius.getProgress() : 41;
        int sPct  = seekSens   != null ? seekSens.getProgress()   : 30;
        double conf = 0.10 + (sPct / 100.0);
        if (txtCx     != null) txtCx.setText("CX " + cxPct + "%");
        if (txtCy     != null) txtCy.setText("CY " + cyPct + "%");
        if (txtRadius != null) txtRadius.setText("R " + rPct + "%");
        if (txtSens   != null) txtSens.setText(String.format(java.util.Locale.US, "%.2f", conf));
    }

    private void applySensitivity() {
        if (plateCounter == null || seekSens == null) return;
        double conf = 0.10 + (seekSens.getProgress() / 100.0);
        plateCounter.setConfidence(conf);
        appendLog(String.format(java.util.Locale.US,
                "sensitivity → conf=%.2f (lower = more detections, more false positives)", conf));
    }

    private void toggleLogsPanel() {
        logsVisible = !logsVisible;
        if (logsScroll != null) logsScroll.setVisibility(logsVisible ? View.VISIBLE : View.GONE);
        btnLogs.setText(logsVisible ? "HIDE LOGS" : "LOGS");
        if (logsVisible) appendLog("logs panel opened");
    }

    /** Append one or more log lines to the UI buffer (max 80 lines kept). */
    private void appendLog(String line) {
        if (line == null) return;
        String stamped = "[" + logTimeFmt.format(new Date()) + "] " + line;
        synchronized (logBuffer) {
            logBuffer.addLast(stamped);
            while (logBuffer.size() > LOG_MAX_LINES) logBuffer.pollFirst();
        }
        Log.i(TAG, line);
        if (logsText != null && logsVisible) {
            StringBuilder sb = new StringBuilder();
            synchronized (logBuffer) {
                for (String l : logBuffer) sb.append(l).append('\n');
            }
            logsText.setText(sb.toString());
            if (logsScroll != null) {
                logsScroll.post(() -> logsScroll.fullScroll(View.FOCUS_DOWN));
            }
        }
    }

    private void applyCircle() {
        int cxPct = seekCx     != null ? seekCx.getProgress()     : 50;
        int cyPct = seekCy     != null ? seekCy.getProgress()     : 47;
        int rPct  = seekRadius != null ? seekRadius.getProgress() : 41;
        circleCx     = cxPct / 100f * previewWidth;
        circleCy     = cyPct / 100f * previewHeight;
        circleRadius = rPct  / 100f * previewWidth;
        pushCircleToCounters();
        updateCircleOverlay();
    }

    private void pushCircleToCounters() {
        if (plateCounter == null) return;
        plateCounter.setCircle(circleCx, circleCy, circleRadius);
    }

    private void updateCircleOverlay() {
        // Show an orange rectangle that approximates the circle bounding box over the preview
        if (roiBorder == null || previewContainer == null) return;
        if (!roiPanelVisible || !previewing) {
            roiBorder.setVisibility(View.GONE);
            return;
        }
        previewContainer.post(() -> {
            if (previewContainer == null || roiBorder == null) return;
            int pw = previewContainer.getWidth();
            int ph = previewContainer.getHeight();
            if (pw <= 0 || ph <= 0 || previewWidth <= 0 || previewHeight <= 0) return;
            // Map circle from frame coords to preview view coords
            float scaleX = (float) pw / previewWidth;
            float scaleY = (float) ph / previewHeight;
            int x0 = Math.round((circleCx - circleRadius) * scaleX);
            int y0 = Math.round((circleCy - circleRadius) * scaleY);
            int x1 = Math.round((circleCx + circleRadius) * scaleX);
            int y1 = Math.round((circleCy + circleRadius) * scaleY);
            x0 = Math.max(0, x0); y0 = Math.max(0, y0);
            x1 = Math.min(pw, x1); y1 = Math.min(ph, y1);
            int rw = x1 - x0, rh = y1 - y0;
            if (rw <= 0 || rh <= 0) { roiBorder.setVisibility(View.GONE); return; }
            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(rw, rh);
            lp.leftMargin = x0;
            lp.topMargin  = y0;
            roiBorder.setLayoutParams(lp);
            roiBorder.setVisibility(View.VISIBLE);
        });
    }

    // ---- Camera index buttons ----

    private void rebuildIndexButtons() {
        indexRow.removeAllViews();
        indexButtons.clear();
        int dpMargin = (int) (6 * getResources().getDisplayMetrics().density);
        int exposed = Math.min(cameraCount, TRAY_CAMERA_COUNT);
        for (int i = 0; i < exposed; i++) {
            Button b = new Button(getContext());
            b.setText(String.valueOf(i));
            b.setTypeface(b.getTypeface(), Typeface.BOLD);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    (int) (64 * getResources().getDisplayMetrics().density),
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMargins(i == 0 ? 0 : dpMargin, 0, 0, 0);
            b.setLayoutParams(lp);
            final int idx = i;
            b.setOnClickListener(view -> onSelectCamera(idx));
            indexRow.addView(b);
            indexButtons.add(b);
        }
        highlightActiveIndex();
    }

    private void highlightActiveIndex() {
        int activeCam = (isOpenCvEngine() && openCvHelper != null)
                ? openCvHelper.getCurrentCameraId() : currentCameraId;
        for (int i = 0; i < indexButtons.size(); i++) {
            Button b = indexButtons.get(i);
            if (i == activeCam && (previewing || (openCvHelper != null && openCvHelper.isPreviewing()))) {
                b.setBackgroundColor(Color.parseColor("#F59E0B"));
                b.setTextColor(Color.BLACK);
            } else {
                b.setBackground(null);
                b.setTextColor(Color.WHITE);
            }
        }
    }

    private void onSelectCamera(int index) {
        if (isOpenCvEngine()) {
            if (openCvHelper != null) {
                openCvHelper.selectTrayCamera(index);
            }
            highlightActiveIndex();
            return;
        }
        if (index == currentCameraId && previewing) return;
        currentCameraId = index;
        lastCount = -1;
        if (previewing) {
            switchCamera(index);
        } else {
            highlightActiveIndex();
            statusText.setText(getString(R.string.cam_status_running, index) + " (tap Start)");
        }
    }

    /** Release current camera, wait for HAL, then open the requested tray index only. */
    private void switchCamera(int index) {
        int exposed = Math.min(cameraCount, TRAY_CAMERA_COUNT);
        if (index < 0 || index >= exposed) return;
        cancelPendingCameraSwitch();
        final int session = ++cameraSession;
        startRequested = true;
        statusText.setText("Switching to camera " + index + "…");
        highlightActiveIndex();
        releaseCameraHardware();
        stopDetectThread();
        previewing = false;
        modelReady = false;
        pendingCameraSwitch = () -> openCameraAfterSettle(session, index);
        mainHandler.postDelayed(pendingCameraSwitch, CAMERA_HAL_SETTLE_MS);
    }

    private void openCameraAfterSettle(int session, int index) {
        pendingCameraSwitch = null;
        if (session != cameraSession || !startRequested || !isAdded()) return;
        if (isOpenCvEngine()) return;
        if (texturePreview == null || texturePreview.getSurfaceTexture() == null) {
            statusText.setText("Waiting for preview surface…");
            return;
        }
        if (tryStartPreviewOnCamera(index)) return;
        Toast.makeText(requireContext(),
                "Camera " + index + " unavailable after switch. Try again or use another index.",
                Toast.LENGTH_LONG).show();
        stopPreview();
    }

    private void cancelPendingCameraSwitch() {
        if (pendingCameraSwitch != null) {
            mainHandler.removeCallbacks(pendingCameraSwitch);
            pendingCameraSwitch = null;
        }
    }

    // ---- Lifecycle ----

    @Override public void onPause() { stopPreview(); super.onPause(); }

    @Override
    public void onDestroyView() {
        cancelPendingCameraSwitch();
        cameraSession++;
        if (plateCounter != null) {
            plateCounter.close();
            plateCounter = null;
        }
        if (openCvHelper != null) {
            openCvHelper.destroy();
            openCvHelper = null;
        }
        super.onDestroyView();
    }

    // ---- Camera start/stop ----

    private void requestStart() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA},
                    isOpenCvEngine() ? REQ_CAMERA_OPENCV : REQ_CAMERA);
            return;
        }
        startRequested = true;
        if (isOpenCvEngine()) {
            if (openCvHelper != null) {
                openCvHelper.userRequestStart();
            }
            return;
        }
        if (surfaceReady) startNcnnPreview();
        else statusText.setText("Waiting for preview surface…");
    }

    @Override
    public void onRequestPermissionsResult(int rc, @NonNull String[] perms, @NonNull int[] grants) {
        if (rc == REQ_CAMERA_OPENCV) {
            if (openCvHelper != null) {
                openCvHelper.onRequestPermissionsResult(rc, perms, grants);
            }
            return;
        }
        if (rc == REQ_CAMERA) {
            if (grants.length > 0 && grants[0] == PackageManager.PERMISSION_GRANTED) requestStart();
            else Toast.makeText(requireContext(), R.string.cam_no_permission, Toast.LENGTH_SHORT).show();
        }
    }

    private void startNcnnPreview() {
        if (isOpenCvEngine()) return;
        if (previewing) return;
        if (texturePreview == null || texturePreview.getSurfaceTexture() == null) return;
        int exposed = Math.min(cameraCount, TRAY_CAMERA_COUNT);
        if (exposed <= 0) {
            Toast.makeText(requireContext(), "No cameras on device", Toast.LENGTH_SHORT).show();
            return;
        }
        int firstTry = currentCameraId;
        if (firstTry < 0 || firstTry >= exposed) firstTry = 0;

        for (int offset = 0; offset < exposed; offset++) {
            int camId = (firstTry + offset) % exposed;
            if (tryStartPreviewOnCamera(camId)) {
                if (camId != firstTry) {
                    lastCount = -1;
                    highlightActiveIndex();
                    Toast.makeText(requireContext(),
                            "Camera " + firstTry + " unavailable — using tray camera " + camId,
                            Toast.LENGTH_LONG).show();
                }
                return;
            }
        }
        Toast.makeText(requireContext(),
                "Tray cameras 0–" + (exposed - 1) + " unavailable. Check another app is not using the camera.",
                Toast.LENGTH_LONG).show();
        highlightActiveIndex();
    }

    /**
     * Opens one tray camera index. Returns false if HAL rejects parameters or preview cannot start.
     * RockChip Keenon builds often throw "get parameters failed (empty parameter)" — we keep HAL defaults.
     * Uses {@link TextureView} + {@link Camera#setPreviewTexture} so preview callbacks work on RK3288.
     */
    private boolean tryStartPreviewOnCamera(int camId) {
        if (isOpenCvEngine()) return false;
        if (camId < 0 || camId >= Camera.getNumberOfCameras()) return false;
        if (texturePreview == null) return false;
        SurfaceTexture surfaceTexture = texturePreview.getSurfaceTexture();
        if (surfaceTexture == null) {
            Log.w(TAG, "tryStartPreviewOnCamera: SurfaceTexture not ready");
            return false;
        }
        releaseCameraHardware();
        previewWidth = 640;
        previewHeight = 480;
        final int session = cameraSession;
        try {
            camera = Camera.open(camId);
            camera.setErrorCallback((error, cam) -> mainHandler.post(() -> {
                if (session != cameraSession || cam != camera) return;
                String msg = error == Camera.CAMERA_ERROR_SERVER_DIED
                        ? "Camera " + camId + " HAL crashed (wait, then try another index)"
                        : "Camera error " + error;
                Log.e(TAG, "Camera " + camId + " error callback: " + error);
                Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show();
                stopPreview();
            }));

            Camera.Parameters params = safeGetParameters(camera);
            if (params != null) {
                Camera.Size size = chooseBestSize(params.getSupportedPreviewSizes());
                if (size != null) {
                    params.setPreviewSize(size.width, size.height);
                    previewWidth = size.width;
                    previewHeight = size.height;
                }
                params.setPreviewFormat(ImageFormat.NV21);
                List<String> modes = params.getSupportedFocusModes();
                if (modes != null && modes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
                    params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
                }
                safeSetParameters(camera, params);
            } else {
                Log.w(TAG, "Camera " + camId + " returned empty parameters — using HAL defaults");
            }

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

            surfaceTexture.setDefaultBufferSize(previewWidth, previewHeight);

            Camera.CameraInfo info = new Camera.CameraInfo();
            Camera.getCameraInfo(camId, info);
            camera.setDisplayOrientation(info.orientation);
            camera.setPreviewTexture(surfaceTexture);

            startDetectThread();
            currentCameraId = camId;

            camera.startPreview();
            previewing = true;
            if (texturePreview != null) {
                texturePreview.setVisibility(View.VISIBLE);
            }
            placeholder.setVisibility(View.GONE);
            btnStart.setEnabled(false);
            btnStop.setEnabled(true);

            loadModelAsync();
            statusText.setText(getString(R.string.cam_status_running, camId)
                    + " · " + previewWidth + "×" + previewHeight + " · loading model…");
            resizeSurfaceToAspect();
            highlightActiveIndex();
            return true;
        } catch (IOException | RuntimeException e) {
            Log.e(TAG, "tryStartPreviewOnCamera(" + camId + ") failed", e);
            releaseCameraHardware();
            return false;
        }
    }

    private void loadModelAsync() {
        // Apply current circle + sensitivity to the shared counter
        pushCircleToCounters();
        applySensitivity();
        final int camId = currentCameraId;
        appendLog("loading canpan_detection_v1.1.5 for camera " + camId + "…");
        appendLog(String.format(java.util.Locale.US,
                "circle: cx=%.1f cy=%.1f r=%.1f  (in 640×480 model space)",
                circleCx, circleCy, circleRadius));
        detectHandler.post(() -> {
            if (plateCounter == null) return;
            long t0 = System.currentTimeMillis();
            boolean ok = plateCounter.ensureLoaded();
            long dt = System.currentTimeMillis() - t0;
            modelReady = ok;
            mainHandler.post(() -> {
                if (!isAdded()) return;
                appendLog("model load " + (ok ? "OK" : "FAILED") + " in " + dt + "ms"
                        + " · conf=" + plateCounter.getConfidence());
                if (!previewing) return;
                statusText.setText(getString(R.string.cam_status_running, camId)
                        + " · " + previewWidth + "×" + previewHeight
                        + (ok ? " · canpan ready" : " · model FAILED"));
                if (ok && previewing) {
                    appendLog("preview: Android Camera1 + TextureView + NV21 (ceiling cams 0–2)");
                    attachCallback();
                }
            });
        });
    }

    private void stopPreview() {
        if (isOpenCvEngine() && openCvHelper != null) {
            startRequested = false;
            cancelPendingCameraSwitch();
            cameraSession++;
            openCvHelper.userStopPreview();
            highlightActiveIndex();
            return;
        }
        startRequested = false;
        cancelPendingCameraSwitch();
        cameraSession++;
        releaseCamera();
        if (roiBorder != null) roiBorder.setVisibility(View.GONE);
        placeholder.setVisibility(View.VISIBLE);
        btnStart.setEnabled(true);
        btnStop.setEnabled(false);
        statusText.setText(R.string.cam_status_idle);
        highlightActiveIndex();
    }

    /** Release Camera1 handle only — used during index switch before HAL settle delay. */
    private void releaseCameraHardware() {
        if (texturePreview != null && !isOpenCvEngine()) {
            texturePreview.setVisibility(View.VISIBLE);
        }
        if (camera != null) {
            try { camera.setErrorCallback(null); } catch (Exception ignored) {}
            try { camera.setPreviewCallbackWithBuffer(null); } catch (Exception ignored) {}
            try { camera.setPreviewCallback(null); } catch (Exception ignored) {}
            try { camera.stopPreview(); } catch (Exception ignored) {}
            try { camera.release(); } catch (Exception ignored) {}
            camera = null;
        }
        detectInFlight.set(false);
        modelReady = false;
    }

    private void releaseCamera() {
        releaseCameraHardware();
        previewing = false;
        lastCount  = -1;
        stopDetectThread();
        if (bigNumber  != null) bigNumber.setText("--");
        if (statusLine != null) { statusLine.setVisibility(View.GONE); statusLine.setText(""); }
    }

    // ---- Detection thread ----

    private void startDetectThread() {
        if (detectThread != null) return;
        detectThread = new HandlerThread("TrayCount-NCNN");
        detectThread.start();
        detectHandler = new Handler(detectThread.getLooper());
        lastDetectAt = 0L;
    }

    private void stopDetectThread() {
        if (detectHandler != null) { detectHandler.removeCallbacksAndMessages(null); detectHandler = null; }
        if (detectThread  != null) { detectThread.quitSafely(); detectThread = null; }
        detectInFlight.set(false);
    }

    private void attachCallback() {
        if (camera == null) return;
        try {
            camera.setPreviewCallback(null);
            camera.setOneShotPreviewCallback(null);
        } catch (Exception ignored) {}
        int bufSize = previewWidth * previewHeight * 3 / 2;
        camera.addCallbackBuffer(new byte[bufSize]);
        camera.addCallbackBuffer(new byte[bufSize]);
        camera.setPreviewCallbackWithBuffer((data, cam) -> onPreviewFrame(data, cam, true));
    }

    private void onPreviewFrame(byte[] data, Camera cam, boolean withBuffer) {
        if (data == null || cam == null) return;
        long now = SystemClock.uptimeMillis();
        if (detectInFlight.get() || now - lastDetectAt < DETECT_INTERVAL_MS || !modelReady) {
            if (withBuffer) try { cam.addCallbackBuffer(data); } catch (Exception ignored) {}
            return;
        }
        if (!detectInFlight.compareAndSet(false, true)) {
            if (withBuffer) try { cam.addCallbackBuffer(data); } catch (Exception ignored) {}
            return;
        }
        lastDetectAt = now;
        final int w = previewWidth, h = previewHeight;
        final Handler bg = detectHandler;
        if (bg == null) {
            detectInFlight.set(false);
            if (withBuffer) try { cam.addCallbackBuffer(data); } catch (Exception ignored) {}
            return;
        }
        bg.post(() -> {
            TrayPlateCounter.CountResult result = null;
            try {
                if (plateCounter != null && plateCounter.isReady()) {
                    result = plateCounter.countDetailed(data, w, h);
                }
            } catch (Throwable t) {
                Log.w(TAG, "count failed", t);
            } finally {
                if (withBuffer && camera != null) {
                    try { camera.addCallbackBuffer(data); } catch (Exception ignored) {}
                }
                detectInFlight.set(false);
            }
            final TrayPlateCounter.CountResult finalResult = result;
            mainHandler.post(() -> {
                if (!isAdded() || !previewing) return;
                renderCount(finalResult);
            });
        });
    }

    private void renderCount(@Nullable TrayPlateCounter.CountResult r) {
        if (bigNumber == null) return;
        if (r == null) return;
        if (r.count < 0) {
            if (r.logLines != null && !r.logLines.isEmpty() && frameSeq % 10 == 0) {
                appendLog("detect: " + r.logLines.get(0));
            }
            return;
        }

        frameSeq++;
        bigNumber.setText(String.valueOf(r.count));
        if (lastCount < 0) {
            appendLog("first count=" + r.count + " · raw boxes=" + r.rawBoxCount);
        }

        // Log every Nth frame to avoid flooding (UI updates throttled to every ~5 frames)
        boolean isHeartbeat = (frameSeq % 5 == 0);
        boolean isChange    = lastCount >= 0 && r.count != lastCount;

        if (isChange) {
            String event = r.count > lastCount
                    ? "Delivered +" + (r.count - lastCount)
                    : "Picked up -" + (lastCount - r.count);
            statusLine.setText(event);
            statusLine.setVisibility(View.VISIBLE);
            mainHandler.postDelayed(() -> {
                if (statusLine != null) statusLine.setVisibility(View.GONE);
            }, 3000L);
            // Always log the event with full per-box breakdown
            appendLog("============================================");
            appendLog(">>> " + event + " (was " + lastCount + ", now " + r.count + ")");
            appendLog("frame #" + frameSeq + " · raw boxes: " + r.rawBoxCount
                    + " · counted: " + r.count);
            if (r.logLines != null) {
                for (String l : r.logLines) appendLog("  " + l);
            }
        } else if (isHeartbeat && logsVisible) {
            // Lightweight heartbeat — only when logs panel is open, to avoid wasted CPU
            String hb = "frame #" + frameSeq + " · count=" + r.count
                    + " · raw=" + r.rawBoxCount;
            if (r.rawBoxCount > 0 && r.logLines != null && !r.logLines.isEmpty()) {
                hb += " · " + r.logLines.get(0);
            }
            appendLog(hb);
        }

        lastCount = r.count;
    }

    // ---- TextureView callbacks (Camera1 preview) ----

    @Override
    public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surface, int width, int height) {
        if (isOpenCvEngine()) return;
        surfaceReady = true;
        if (startRequested && !previewing) startNcnnPreview();
    }

    @Override
    public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surface, int width, int height) {
        if (isOpenCvEngine()) return;
        if (camera == null) return;
        try {
            camera.stopPreview();
            surface.setDefaultBufferSize(previewWidth, previewHeight);
            camera.setPreviewTexture(surface);
            camera.startPreview();
        } catch (Exception e) {
            Log.e(TAG, "onSurfaceTextureSizeChanged restart failed", e);
        }
    }

    @Override
    public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surface) {
        surfaceReady = false;
        releaseCamera();
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surface) {
        // not used
    }

    // ---- Helpers ----

    private void resizeSurfaceToAspect() {
        if (previewWidth <= 0 || previewHeight <= 0 || previewContainer == null || texturePreview == null) {
            return;
        }
        previewContainer.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override public void onGlobalLayout() {
                        int cw = previewContainer.getWidth(), ch = previewContainer.getHeight();
                        if (cw <= 0 || ch <= 0) return;
                        previewContainer.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                        float asp = (float) previewWidth / previewHeight;
                        int tw, th;
                        if (cw / (float) ch > asp) { th = ch; tw = Math.round(ch * asp); }
                        else { tw = cw; th = Math.round(cw / asp); }
                        if (texturePreview == null) return;
                        ViewGroup.LayoutParams lp = texturePreview.getLayoutParams();
                        lp.width = tw; lp.height = th;
                        texturePreview.setLayoutParams(lp);
                    }
                });
        previewContainer.requestLayout();
    }

    /**
     * Prefer largest size ≤640×480 — matches {@link com.keenon.peanut.sample.receiver.RobotCameraStreamer}
     * and avoids RockChip mediaserver crashes when switching tray cameras.
     */
    private Camera.Size chooseBestSize(List<Camera.Size> sizes) {
        if (sizes == null || sizes.isEmpty()) return null;
        Camera.Size best = null;
        for (Camera.Size s : sizes) {
            if (s.width <= 640 && s.height <= 480) {
                if (best == null || s.width * s.height > best.width * best.height) best = s;
            }
        }
        if (best != null) return best;
        best = sizes.get(0);
        for (Camera.Size s : sizes) {
            if (s.width * s.height < best.width * best.height) best = s;
        }
        return best;
    }

    /** RockChip tray HAL may throw instead of returning metadata — do not crash the tab. */
    private Camera.Parameters safeGetParameters(Camera cam) {
        try {
            return cam.getParameters();
        } catch (RuntimeException e) {
            Log.w(TAG, "getParameters failed for camera " + currentCameraId, e);
            return null;
        }
    }

    private void safeSetParameters(Camera cam, Camera.Parameters params) {
        try {
            cam.setParameters(params);
        } catch (RuntimeException e) {
            Log.w(TAG, "setParameters failed, continuing with HAL defaults", e);
        }
    }

    private Camera.Size safeGetCurrentPreviewSize(Camera cam) {
        try {
            Camera.Parameters params = cam.getParameters();
            if (params != null) return params.getPreviewSize();
        } catch (RuntimeException e) {
            Log.w(TAG, "Unable to read current preview size", e);
        }
        return null;
    }
}