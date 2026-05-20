package com.keenon.peanut.supermarket.fragment;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.YuvImage;
import android.view.Gravity;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.view.LayoutInflater;
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
import com.keenon.peanut.supermarket.SupermarketActivity;
import com.keenon.peanut.supermarket.feedback.TrayPickFeedbackCoordinator;
import com.keenon.peanut.supermarket.util.TtsHelper;
import com.keenon.peanut.supermarket.vision.TrayCountingHelper;
import com.keenon.peanut.supermarket.widget.TrayRoiOverlayView;
import com.keenon.peanut.supermarket.yolo.HandDetectorHelper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Developer "Count" tab â€” continuous OpenCV-based tray-item counter.
 *
 * <p>Pure classical image processing: no ML model, no object-detection labels. Each
 * tray camera (0/1/2) is handled by its own {@link TrayCountingHelper} instance so
 * switching cameras preserves per-tray state. The on-screen overlay shows a single
 * big number â€” the current count â€” plus a small secondary line for calibration
 * progress and pick/delivery events.
 *
 * <p>This fragment intentionally does NOT run the TFLite object detector (unlike
 * {@link CameraFragment}). It's a focused diagnostic for the counting algorithm.
 */
public class AdvancedCountFragment extends Fragment implements SurfaceHolder.Callback,
    TrayPickFeedbackCoordinator.PickFeedbackHost {

  private static final int PERSON_CAMERA_ID = 3;
  private static final long HAND_TRACK_INTERVAL_MS = 350L;

  private static final String TAG = "AdvancedCountFragment";

  private static final int REQ_CAMERA = 2202;

  /** Stored ROI in CAMERA-FRAME pixel coordinates (so it survives view-size changes). */
  protected static final class FrameRoi {
    final int cx, cy, rx, ry, frameW, frameH;
    FrameRoi(int cx, int cy, int rx, int ry, int frameW, int frameH) {
      this.cx = cx; this.cy = cy; this.rx = rx; this.ry = ry;
      this.frameW = frameW; this.frameH = frameH;
    }
  }

  /** Soft cap on frame processing (~8 FPS) â€” fast calibration for demos. */
  private static final long DETECT_INTERVAL_MS = 80L;
  /** Delay after release before re-open (robot HAL needs time to unlock). */
  private static final long CAMERA_OPEN_DELAY_MS = 700L;
  private static final int MAX_CAMERA_OPEN_ATTEMPTS = 4;

  /** Camera IDs that look down at the three robot trays. Cam 3 is the front-facing camera. */
  private static final int TRAY_CAMERA_COUNT = 3;

  /** Override in subclasses for isolated prefs (e.g. Count 2.0). */
  protected String getCountTrayPrefsName() {
    return "count_tray_prefs";
  }

  /**
   * Called when a customer pickup is detected ({@link TrayCountingHelper.EventType#PICKED_UP}).
   * Override in Count4 to log approach angle from tray center.
   */
  protected void onCustomerTrayPick(int prevCount, int newCount, @NonNull TrayCountingHelper.Result r) {
    // default: no-op
  }

  protected int getTrayCameraId() {
    return currentCameraId;
  }

  protected float getLastHandNormX() {
    return lastHandNormX;
  }

  protected float getLastHandNormY() {
    return lastHandNormY;
  }

  @Nullable
  protected FrameRoi getActiveTrayRoi() {
    if (savedRoiPerCamera == null
        || currentCameraId < 0
        || currentCameraId >= TRAY_CAMERA_COUNT) {
      return null;
    }
    return savedRoiPerCamera[currentCameraId];
  }
  private static final String PREF_KEY_SLOTS_CAM = "slots_cam_";
  private static final String PREF_KEY_MODE_CAM = "mode_cam_";

  /** What the tray is being used for right now (per camera). */
  public enum TrayOperationMode {
    /** Staff is placing products on the tray â€” count increases only. */
    STOCKING,
    /** Tray is full / ready â€” customer picks trigger review + robot turn. */
    CUSTOMER_READY
  }
  /** Default slot counts per tray camera: cam0=4, cam1=9, cam2=6. */
  private static final int[] DEFAULT_SLOTS_PER_CAMERA = {4, 9, 6};

  // ---- UI views ----
  private FrameLayout previewContainer;
  private SurfaceView surfaceView;
  private TextView statusText;
  private TextView placeholder;
  private TextView bigNumber;       // big count digit (top-right)
  private TextView statusLine;      // small status line (bottom-left, calibration / events / errors)
  private TextView modeBanner;
  private Button btnModeStocking;
  private Button btnModeCustomer;
  private LinearLayout indexRow;
  private Button btnStart, btnStop;
  private final java.util.List<Button> indexButtons = new java.util.ArrayList<>();
  private final java.util.List<Button> slotCountButtons = new java.util.ArrayList<>();
  /** User-selected slot totals (2, 4, 6, or 9) per tray camera. */
  private int[] slotsPerCamera;
  /** Per-camera tray mode: loading vs ready for customer pickup. */
  private TrayOperationMode[] modePerCamera;
  // ROI selection
  private TrayRoiOverlayView roiOverlay;
  private TextView roiPrompt;
  private LinearLayout roiRow;
  private Button btnRoiConfirm, btnRoiReset, btnRoiEdit;
  private int roiLayoutRetryCount;
  /** Per-camera saved ROI (camera-frame coords). Null = not yet set for that camera. */
  private FrameRoi[] savedRoiPerCamera;
  /** Has the active camera's ROI been confirmed (locked) yet? Gates detection. */
  private boolean roiConfirmed;
  /** True once the surface + ROI overlay share the letterboxed preview size. */
  private boolean previewLayoutReady;
  /** Letterboxed preview view size (used for ROI mapping when getWidth() is still 0). */
  private int previewLayoutW;
  private int previewLayoutH;

  // ---- Camera state ----
  private Camera camera;
  private int currentCameraId = 0;
  private int cameraCount;
  private boolean previewing;
  private boolean surfaceReady;
  private boolean startRequested;
  private int previewWidth;
  private int previewHeight;
  /** {@link Camera#setDisplayOrientation(int)} value for the active camera. */
  private int displayOrientation;
  private boolean frontFacing;
  private Runnable pendingOpenRunnable;
  @Nullable private Runnable pendingOpenOnReady;
  /** True while letterboxing resizes the SurfaceView â€” avoid stop/start in surfaceChanged. */
  private boolean layoutResizing;
  private int halRecoveryAttempts;
  private int previewGeneration;
  @Nullable private Runnable pendingAttachCallbacks;

  // ---- Counting state ----
  /** One independent counting helper per tray camera (cams 0/1/2). Index = camera ID. */
  private TrayCountingHelper[] trayHelpers;

  private HandlerThread detectThread;
  private Handler detectHandler;
  private final Handler mainHandler = new Handler(Looper.getMainLooper());
  private final AtomicBoolean detectBusy = new AtomicBoolean(false);
  private long lastDetectAt;

  private final TtsHelper ttsHelper = new TtsHelper();
  private TrayPickFeedbackCoordinator pickFeedback;
  /** Pauses frame counting while robot rotates / feedback dialog is open. */
  private boolean pausedForPickFeedback;
  private int trayCameraBeforePick;
  private TrayPickFeedbackCoordinator.PersonFrameSink personFrameSink;
  private HandDetectorHelper handDetector;
  /** Written only on {@link #handPipelineHandler} â€” NCNN must not be called from TrayCount thread. */
  private volatile float lastHandNormX = -1f;
  private volatile float lastHandNormY = -1f;
  private volatile long lastHandDetectAt;
  /** Cross-thread: main sets after calib, pipeline reads before YOLO. */
  private volatile long handDetectAllowedAfterMs;
  private boolean wasCalibrating;
  /** Single serial queue for load + hand_detect â€” avoids SIGSEGV when init/detect threads differ */
  private HandlerThread handPipelineThread;
  private Handler handPipelineHandler;

  // ---- Lifecycle ----

  @Nullable
  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_dev_count, container, false);
  }

  @Override
  public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(v, savedInstanceState);

    previewContainer = v.findViewById(R.id.count_preview_container);
    surfaceView = v.findViewById(R.id.count_surface);
    statusText = v.findViewById(R.id.count_status);
    placeholder = v.findViewById(R.id.count_placeholder);
    bigNumber = v.findViewById(R.id.count_big_number);
    statusLine = v.findViewById(R.id.count_status_line);
    modeBanner = v.findViewById(R.id.count_mode_banner);
    btnModeStocking = v.findViewById(R.id.btn_count_mode_stocking);
    btnModeCustomer = v.findViewById(R.id.btn_count_mode_customer);
    indexRow = v.findViewById(R.id.count_index_row);
    roiOverlay = v.findViewById(R.id.count_roi_overlay);
    roiPrompt = v.findViewById(R.id.count_roi_prompt);
    roiRow = v.findViewById(R.id.count_roi_row);
    btnRoiConfirm = v.findViewById(R.id.btn_count_roi_confirm);
    btnRoiReset = v.findViewById(R.id.btn_count_roi_reset);
    btnRoiEdit = v.findViewById(R.id.btn_count_roi_edit);

    savedRoiPerCamera = new FrameRoi[TRAY_CAMERA_COUNT];

    roiOverlay.setListener(new TrayRoiOverlayView.Listener() {
      @Override
      public void onStepChanged(TrayRoiOverlayView.TapStep step) {
        updateRoiPromptForStep(step);
        btnRoiConfirm.setEnabled(step == TrayRoiOverlayView.TapStep.DONE
            && roiOverlay.getEllipse() != null);
      }
      @Override
      public void onEllipseReady(TrayRoiOverlayView.Ellipse ellipse) {
        // No-op â€” the Confirm button now becomes enabled via onStepChanged.
      }
    });
    btnRoiConfirm.setOnClickListener(view -> confirmRoi());
    btnRoiReset.setOnClickListener(view -> resetRoi());
    if (btnRoiEdit != null) {
      btnRoiEdit.setOnClickListener(view -> resetRoi());
    }

    loadSlotsPerCamera();
    loadModesPerCamera();
    // Only construct helpers for tray cameras (OpenCV loads once â€” see TrayCountingHelper).
    trayHelpers = new TrayCountingHelper[TRAY_CAMERA_COUNT];
    for (int i = 0; i < TRAY_CAMERA_COUNT; i++) {
      trayHelpers[i] =
          new TrayCountingHelper(i, TrayCountingHelper.BoxLayout.fromSlotCount(slotsPerCamera[i]));
    }

    wireSlotCountButton(v.findViewById(R.id.btn_count_slots_2), 2);
    wireSlotCountButton(v.findViewById(R.id.btn_count_slots_4), 4);
    wireSlotCountButton(v.findViewById(R.id.btn_count_slots_6), 6);
    wireSlotCountButton(v.findViewById(R.id.btn_count_slots_9), 9);
    highlightSlotCount();

    btnModeStocking.setOnClickListener(view -> setTrayMode(TrayOperationMode.STOCKING));
    btnModeCustomer.setOnClickListener(view -> setTrayMode(TrayOperationMode.CUSTOMER_READY));
    highlightTrayMode();
    updateModeBanner();

    btnStart = v.findViewById(R.id.btn_count_start);
    btnStop = v.findViewById(R.id.btn_count_stop);

    surfaceView.getHolder().addCallback(this);

    btnStart.setOnClickListener(view -> requestStart());
    btnStop.setOnClickListener(view -> stopPreview());

    cameraCount = Math.max(1, Camera.getNumberOfCameras());
    rebuildIndexButtons();

    ttsHelper.initialize(requireContext());
    handPipelineThread = new HandlerThread("HandPipeline");
    handPipelineThread.start();
    handPipelineHandler = new Handler(handPipelineThread.getLooper());

    pickFeedback = createPickFeedbackCoordinator();
  }

  /** Subclasses return non-null to override pick-review rotation timing (see Count 2.0). */
  @Nullable
  protected TrayPickFeedbackCoordinator.Timing pickFeedbackTiming() {
    return null;
  }

  /** Subclasses (e.g. Count 2.0) may plug different pick-review timing. */
  protected TrayPickFeedbackCoordinator createPickFeedbackCoordinator() {
    TrayPickFeedbackCoordinator.Timing timing = pickFeedbackTiming();
    return new TrayPickFeedbackCoordinator(
        requireContext(),
        this,
        ttsHelper,
        this::onPickFeedbackFinished,
        timing);
  }

  protected void onPickFeedbackFinished(int remainingCount) {
    pausedForPickFeedback = false;
    if (trayHelpers != null
        && currentCameraId >= 0
        && currentCameraId < TRAY_CAMERA_COUNT
        && trayHelpers[currentCameraId] != null) {
      trayHelpers[currentCameraId].acknowledgePickCount(remainingCount);
    }
    if (bigNumber != null && trayHelpers != null && currentCameraId >= 0
        && currentCameraId < TRAY_CAMERA_COUNT) {
      TrayCountingHelper h = trayHelpers[currentCameraId];
      if (h != null) {
        bigNumber.setText(remainingCount + " / " + h.getSlotCount());
      }
    }
    if (statusLine != null) {
      statusLine.setText(getString(R.string.count_resume_after_feedback, remainingCount));
      statusLine.setVisibility(View.VISIBLE);
    }
  }

  // ---- PickFeedbackHost ----

  @Override
  public Fragment getHostFragment() {
    return this;
  }

  @Override
  public boolean isSdkReady() {
    return getActivity() instanceof SupermarketActivity
        && ((SupermarketActivity) getActivity()).isSdkReady();
  }

  @Override
  public int getTrayCameraIdToRestore() {
    return trayCameraBeforePick;
  }

  @Override
  public void switchToPersonCamera(Runnable onReady) {
    trayCameraBeforePick = currentCameraId;
    if (currentCameraId == PERSON_CAMERA_ID && previewing) {
      if (onReady != null) onReady.run();
      return;
    }
    if (previewing) {
      releaseCameraOnly();
      startRequested = true;
      pendingOpenOnReady = onReady;
      mainHandler.postDelayed(
          () -> openCameraInternal(PERSON_CAMERA_ID, 0, onReady), CAMERA_OPEN_DELAY_MS);
    } else {
      currentCameraId = PERSON_CAMERA_ID;
      if (onReady != null) onReady.run();
    }
  }

  @Override
  public void restoreTrayCamera(int cameraId, Runnable onReady) {
    personFrameSink = null;
    currentCameraId = Math.max(0, Math.min(cameraId, TRAY_CAMERA_COUNT - 1));
    if (previewing) {
      releaseCameraOnly();
      startRequested = true;
      mainHandler.postDelayed(() -> {
        startPreview();
        highlightActiveIndex();
        highlightSlotCount();
        highlightTrayMode();
        updateModeBanner();
        if (onReady != null) onReady.run();
      }, CAMERA_OPEN_DELAY_MS);
    } else if (onReady != null) {
      onReady.run();
    }
  }

  @Override
  public void setPersonFrameSink(TrayPickFeedbackCoordinator.PersonFrameSink sink) {
    personFrameSink = sink;
  }

  @Override
  public float getTrackedHandNormX() {
    return lastHandNormX;
  }

  private void loadModesPerCamera() {
    modePerCamera = new TrayOperationMode[TRAY_CAMERA_COUNT];
    SharedPreferences prefs =
        requireContext().getSharedPreferences(getCountTrayPrefsName(), Context.MODE_PRIVATE);
    for (int i = 0; i < TRAY_CAMERA_COUNT; i++) {
      int ord = prefs.getInt(PREF_KEY_MODE_CAM + i, TrayOperationMode.STOCKING.ordinal());
      modePerCamera[i] = ord == TrayOperationMode.CUSTOMER_READY.ordinal()
          ? TrayOperationMode.CUSTOMER_READY
          : TrayOperationMode.STOCKING;
    }
  }

  private void saveModeForCamera(int cameraId, TrayOperationMode mode) {
    requireContext()
        .getSharedPreferences(getCountTrayPrefsName(), Context.MODE_PRIVATE)
        .edit()
        .putInt(PREF_KEY_MODE_CAM + cameraId, mode.ordinal())
        .apply();
  }

  private TrayOperationMode getTrayMode() {
    if (modePerCamera == null || currentCameraId < 0 || currentCameraId >= TRAY_CAMERA_COUNT) {
      return TrayOperationMode.STOCKING;
    }
    return modePerCamera[currentCameraId];
  }

  private void setTrayMode(TrayOperationMode mode) {
    if (currentCameraId < 0 || currentCameraId >= TRAY_CAMERA_COUNT) return;
    if (modePerCamera[currentCameraId] == mode) return;
    modePerCamera[currentCameraId] = mode;
    saveModeForCamera(currentCameraId, mode);
    if (mode == TrayOperationMode.CUSTOMER_READY) {
      ensureHandDetectorLazy();
    }
    highlightTrayMode();
    updateModeBanner();
    if (statusLine != null) {
      int msgId = mode == TrayOperationMode.STOCKING
          ? R.string.count_mode_banner_stocking
          : R.string.count_mode_banner_customer;
      statusLine.setText(msgId);
      statusLine.setVisibility(View.VISIBLE);
    }
  }

  private void highlightTrayMode() {
    if (btnModeStocking == null || btnModeCustomer == null) return;
    TrayOperationMode mode = getTrayMode();
    styleModeButton(btnModeStocking, mode == TrayOperationMode.STOCKING, true);
    styleModeButton(btnModeCustomer, mode == TrayOperationMode.CUSTOMER_READY, false);
  }

  private void styleModeButton(Button b, boolean selected, boolean stocking) {
    if (selected) {
      b.setBackgroundColor(Color.parseColor(stocking ? "#43A047" : "#1E88E5"));
      b.setTextColor(Color.WHITE);
    } else {
      b.setBackgroundResource(R.drawable.bg_count_btn_default);
      b.setTextColor(Color.parseColor("#E8EAED"));
    }
  }

  private void updateModeBanner() {
    if (modeBanner == null) return;
    if (!previewing) {
      modeBanner.setVisibility(View.GONE);
      return;
    }
    TrayOperationMode mode = getTrayMode();
    if (mode == TrayOperationMode.STOCKING) {
      modeBanner.setText(R.string.count_mode_banner_stocking);
      modeBanner.setBackgroundColor(0xCC1B5E20);
    } else {
      modeBanner.setText(R.string.count_mode_banner_customer);
      modeBanner.setBackgroundColor(0xCC1565C0);
    }
    modeBanner.setVisibility(View.VISIBLE);
  }

  private void loadSlotsPerCamera() {
    slotsPerCamera = new int[TRAY_CAMERA_COUNT];
    SharedPreferences prefs =
        requireContext().getSharedPreferences(getCountTrayPrefsName(), Context.MODE_PRIVATE);
    for (int i = 0; i < TRAY_CAMERA_COUNT; i++) {
      int saved = prefs.getInt(PREF_KEY_SLOTS_CAM + i, DEFAULT_SLOTS_PER_CAMERA[i]);
      slotsPerCamera[i] =
          TrayCountingHelper.BoxLayout.isSupportedSlotCount(saved)
              ? saved
              : DEFAULT_SLOTS_PER_CAMERA[i];
    }
  }

  private void saveSlotsForCamera(int cameraId, int slotCount) {
    requireContext()
        .getSharedPreferences(getCountTrayPrefsName(), Context.MODE_PRIVATE)
        .edit()
        .putInt(PREF_KEY_SLOTS_CAM + cameraId, slotCount)
        .apply();
  }

  private void wireSlotCountButton(Button button, int slotCount) {
    button.setOnClickListener(view -> onSelectSlotCount(slotCount));
    slotCountButtons.add(button);
  }

  private void onSelectSlotCount(int slotCount) {
    if (currentCameraId < 0 || currentCameraId >= TRAY_CAMERA_COUNT) return;
    if (slotsPerCamera[currentCameraId] == slotCount) return;
    slotsPerCamera[currentCameraId] = slotCount;
    saveSlotsForCamera(currentCameraId, slotCount);
    if (trayHelpers != null && trayHelpers[currentCameraId] != null) {
      trayHelpers[currentCameraId].setBoxLayout(
          TrayCountingHelper.BoxLayout.fromSlotCount(slotCount));
      FrameRoi saved = savedRoiPerCamera[currentCameraId];
      if (saved != null) {
        applyRoiToActiveHelper(saved);
      }
    }
    highlightSlotCount();
    if (roiOverlay != null) {
      roiOverlay.setSlotRects(null);
    }
    bigNumber.setText("--");
    statusLine.setText(R.string.count_slots_changed);
    statusLine.setVisibility(View.VISIBLE);
    Toast.makeText(requireContext(), R.string.count_slots_changed, Toast.LENGTH_SHORT).show();
  }

  private void highlightSlotCount() {
    if (slotsPerCamera == null || currentCameraId < 0 || currentCameraId >= TRAY_CAMERA_COUNT) {
      return;
    }
    int selected = slotsPerCamera[currentCameraId];
    int[] options = {2, 4, 6, 9};
    for (int i = 0; i < slotCountButtons.size() && i < options.length; i++) {
      stylePanelButton(slotCountButtons.get(i), options[i] == selected);
    }
  }

  private void rebuildIndexButtons() {
    indexRow.removeAllViews();
    indexButtons.clear();
    int dpMargin = (int) (4 * getResources().getDisplayMetrics().density);
    int exposed = Math.min(cameraCount, TRAY_CAMERA_COUNT);
    for (int i = 0; i < exposed; i++) {
      Button b = new Button(getContext(), null, R.style.CountCompactButton);
      b.setText(getString(R.string.count_camera_btn, i));
      b.setTypeface(b.getTypeface(), Typeface.BOLD);
      LinearLayout.LayoutParams lp =
          new LinearLayout.LayoutParams(
              LinearLayout.LayoutParams.MATCH_PARENT,
              LinearLayout.LayoutParams.WRAP_CONTENT);
      if (i > 0) lp.topMargin = dpMargin;
      b.setLayoutParams(lp);
      final int idx = i;
      b.setOnClickListener(view -> onSelectCamera(idx));
      indexRow.addView(b);
      indexButtons.add(b);
    }
    highlightActiveIndex();
  }

  private void stylePanelButton(Button b, boolean selected) {
    if (selected) {
      b.setBackgroundResource(R.drawable.bg_count_btn_primary);
      b.setTextColor(Color.parseColor("#1A1D24"));
    } else {
      b.setBackgroundResource(R.drawable.bg_count_btn_default);
      b.setTextColor(Color.parseColor("#E8EAED"));
    }
  }

  private void highlightActiveIndex() {
    for (int i = 0; i < indexButtons.size(); i++) {
      stylePanelButton(indexButtons.get(i), i == currentCameraId);
    }
  }

  private void onSelectCamera(int index) {
    if (index < 0 || index >= TRAY_CAMERA_COUNT) return;
    if (index == currentCameraId && previewing) return;
    currentCameraId = index;
    // Each camera has its own ROI; reset confirmation state for the new one.
    roiConfirmed = false;
    if (previewing) {
      cancelPendingCameraOpen();
      releaseCameraOnly();
      startRequested = true;
      statusText.setText(getString(R.string.cam_status_opening, index));
      final int openId = index;
      pendingOpenRunnable = () -> openCameraInternal(openId, 0, null);
      mainHandler.postDelayed(pendingOpenRunnable, CAMERA_OPEN_DELAY_MS);
    } else {
      highlightActiveIndex();
      highlightSlotCount();
      highlightTrayMode();
      updateModeBanner();
      statusText.setText(
          getString(R.string.cam_status_running, index) + " (tap Start to preview)");
    }
  }

  // ---------- ROI selection flow ----------

  /**
   * Called after the camera preview becomes visible. Shows the ROI overlay so the user can
   * outline the oval tray. If we already have a saved ROI for this camera, restore + lock
   * it and skip straight to calibration.
   */
  private void enterRoiSelection() {
    roiConfirmed = false;
    FrameRoi saved = (currentCameraId >= 0 && currentCameraId < TRAY_CAMERA_COUNT)
        ? savedRoiPerCamera[currentCameraId] : null;
    if (saved != null && previewWidth > 0 && previewHeight > 0 && isRoiMappingReady()) {
      // Convert saved frame-coords back to view-coords for the overlay.
      TrayRoiOverlayView.Ellipse e = frameToView(saved);
      if (e != null) {
        roiOverlay.restoreEllipse(e, /*locked=*/true);
        roiOverlay.setVisibility(View.VISIBLE);
        // Re-map through the CURRENT view size â€” stale frame coords from an earlier
        // layout (top-left shrink bug) were breaking the OpenCV mask and calibration.
        FrameRoi fresh = viewToFrame(roiOverlay.getEllipse());
        if (fresh != null) {
          savedRoiPerCamera[currentCameraId] = fresh;
          applyRoiToActiveHelper(fresh);
        } else {
          applyRoiToActiveHelper(saved);
        }
        if (trayHelpers != null && trayHelpers[currentCameraId] != null) {
          trayHelpers[currentCameraId].beginCalibration();
        }
        roiConfirmed = true;
        refreshSlotOverlayFromHelper();
        roiRow.setVisibility(View.GONE);
        roiPrompt.setVisibility(View.GONE);
        updateRoiEditButtonVisibility();
        return;
      }
    }
    // Fresh selection.
    roiOverlay.resetTaps();
    roiOverlay.setVisibility(View.VISIBLE);
    roiRow.setVisibility(View.VISIBLE);
    btnRoiConfirm.setEnabled(false);
    roiPrompt.setVisibility(View.VISIBLE);
    updateRoiPromptForStep(TrayRoiOverlayView.TapStep.TOP);
    updateRoiEditButtonVisibility();
  }

  private void exitRoiSelection() {
    roiOverlay.setVisibility(View.GONE);
    roiRow.setVisibility(View.GONE);
    roiPrompt.setVisibility(View.GONE);
  }

  private void confirmRoi() {
    if (!isRoiMappingReady()) {
      roiOverlay.post(this::confirmRoi);
      return;
    }
    TrayRoiOverlayView.Ellipse e = roiOverlay.getEllipse();
    if (e == null) {
      Toast.makeText(requireContext(), R.string.count_roi_confirm_failed, Toast.LENGTH_SHORT).show();
      return;
    }
    FrameRoi frameRoi = viewToFrame(e);
    if (frameRoi == null) {
      Toast.makeText(requireContext(), R.string.count_roi_confirm_failed, Toast.LENGTH_SHORT).show();
      return;
    }
    TrayCountingHelper helper = trayHelpers != null && currentCameraId >= 0
        && currentCameraId < TRAY_CAMERA_COUNT ? trayHelpers[currentCameraId] : null;
    if (helper == null || !helper.isReady()) {
      Toast.makeText(requireContext(), R.string.count_opencv_failed, Toast.LENGTH_LONG).show();
      return;
    }
    savedRoiPerCamera[currentCameraId] = frameRoi;
    applyRoiToActiveHelper(frameRoi);
    if (previewWidth > 0 && previewHeight > 0) {
      helper.syncRoiToFrameSize(previewWidth, previewHeight);
    }
    helper.beginCalibration();
    wasCalibrating = true;
    handDetectAllowedAfterMs = 0L;
    lastDetectAt = 0L;
    roiOverlay.lockEllipse();
    roiRow.setVisibility(View.GONE);
    roiPrompt.setVisibility(View.GONE);
    roiConfirmed = true;
    refreshSlotOverlayFromHelper();
    bigNumber.setText("0%");
    statusLine.setText(R.string.count_calibrating_hint);
    statusLine.setVisibility(View.VISIBLE);
    statusText.setText("Tray shape set â€” calibratingâ€¦");
    updateRoiEditButtonVisibility();
  }

  private void resetRoi() {
    if (currentCameraId >= 0 && currentCameraId < TRAY_CAMERA_COUNT) {
      savedRoiPerCamera[currentCameraId] = null;
    }
    roiConfirmed = false;
    if (trayHelpers != null && trayHelpers[currentCameraId] != null) {
      trayHelpers[currentCameraId].clearRoi();
      trayHelpers[currentCameraId].reset();
    }
    bigNumber.setText("--");
    statusLine.setVisibility(View.GONE);
    roiOverlay.resetTaps();
    roiOverlay.setVisibility(View.VISIBLE);
    roiRow.setVisibility(View.VISIBLE);
    btnRoiConfirm.setEnabled(false);
    roiPrompt.setVisibility(View.VISIBLE);
    updateRoiPromptForStep(TrayRoiOverlayView.TapStep.TOP);
    updateRoiEditButtonVisibility();
  }

  private void updateRoiPromptForStep(TrayRoiOverlayView.TapStep step) {
    if (roiPrompt == null) return;
    int id;
    switch (step) {
      case TOP:    id = R.string.count_roi_prompt_top;    break;
      case BOTTOM: id = R.string.count_roi_prompt_bottom; break;
      case LEFT:   id = R.string.count_roi_prompt_left;   break;
      case RIGHT:  id = R.string.count_roi_prompt_right;  break;
      case DONE:
      default:     id = R.string.count_roi_prompt_done;   break;
    }
    roiPrompt.setText(id);
  }

  /**
   * Overlay must have a real size before mapping view â†” frame.
   * SurfaceView often reports 0Ã—0 until the surface is created â€” use overlay size only.
   */
  private boolean isRoiMappingReady() {
    return previewing && roiMapViewW() > 0 && roiMapViewH() > 0;
  }

  private int roiMapViewW() {
    if (roiOverlay != null && roiOverlay.getWidth() > 0) {
      return roiOverlay.getWidth();
    }
    if (previewLayoutW > 0) return previewLayoutW;
    return surfaceView != null ? surfaceView.getWidth() : 0;
  }

  private int roiMapViewH() {
    if (roiOverlay != null && roiOverlay.getHeight() > 0) {
      return roiOverlay.getHeight();
    }
    if (previewLayoutH > 0) return previewLayoutH;
    return surfaceView != null ? surfaceView.getHeight() : 0;
  }

  /** Convert overlay-view ellipse coords â†’ camera-frame ellipse coords. */
  @Nullable
  private FrameRoi viewToFrame(TrayRoiOverlayView.Ellipse e) {
    int viewW = roiMapViewW();
    int viewH = roiMapViewH();
    if (viewW <= 0 || viewH <= 0 || previewWidth <= 0 || previewHeight <= 0) return null;
    float[] center = new float[2];
    float[] edgeX = new float[2];
    float[] edgeY = new float[2];
    mapViewToFrame(e.cx, e.cy, viewW, viewH, center);
    mapViewToFrame(e.cx + e.rx, e.cy, viewW, viewH, edgeX);
    mapViewToFrame(e.cx, e.cy + e.ry, viewW, viewH, edgeY);
    int cx = Math.round(center[0]);
    int cy = Math.round(center[1]);
    int rx = Math.max(1, Math.round(Math.abs(edgeX[0] - center[0])));
    int ry = Math.max(1, Math.round(Math.abs(edgeY[1] - center[1])));
    return new FrameRoi(cx, cy, rx, ry, previewWidth, previewHeight);
  }

  /** Convert saved camera-frame ellipse coords â†’ overlay-view ellipse coords. */
  @Nullable
  private TrayRoiOverlayView.Ellipse frameToView(FrameRoi r) {
    int viewW = roiMapViewW();
    int viewH = roiMapViewH();
    if (viewW <= 0 || viewH <= 0 || r.frameW <= 0 || r.frameH <= 0) return null;
    float[] center = new float[2];
    float[] edgeX = new float[2];
    float[] edgeY = new float[2];
    mapFrameToView(r.cx, r.cy, viewW, viewH, center);
    mapFrameToView(r.cx + r.rx, r.cy, viewW, viewH, edgeX);
    mapFrameToView(r.cx, r.cy + r.ry, viewW, viewH, edgeY);
    float cx = center[0];
    float cy = center[1];
    float rx = Math.max(8f, Math.abs(edgeX[0] - cx));
    float ry = Math.max(8f, Math.abs(edgeY[1] - cy));
    return new TrayRoiOverlayView.Ellipse(cx, cy, rx, ry);
  }

  /**
   * Map a touch point on the ROI overlay to camera NV21 frame coordinates.
   * Accounts for {@link #displayOrientation} and letterboxing (overlay matches surface size).
   */
  private void mapViewToFrame(float vx, float vy, int viewW, int viewH, float[] outXY) {
    float u = vx / viewW;
    float v = vy / viewH;
    float fw = previewWidth;
    float fh = previewHeight;
    float fx;
    float fy;
    switch (displayOrientation) {
      case 90:
        fx = v * fw;
        fy = (1f - u) * fh;
        break;
      case 180:
        fx = (1f - u) * fw;
        fy = (1f - v) * fh;
        break;
      case 270:
        fx = (1f - v) * fw;
        fy = u * fh;
        break;
      case 0:
      default:
        fx = u * fw;
        fy = v * fh;
        break;
    }
    if (frontFacing) {
      fx = fw - fx;
    }
    outXY[0] = clamp(fx, 0f, fw - 1f);
    outXY[1] = clamp(fy, 0f, fh - 1f);
  }

  private void mapFrameToView(float fx, float fy, int viewW, int viewH, float[] outXY) {
    float fw = previewWidth;
    float fh = previewHeight;
    if (frontFacing) {
      fx = fw - fx;
    }
    float u;
    float v;
    switch (displayOrientation) {
      case 90:
        u = 1f - fy / fh;
        v = fx / fw;
        break;
      case 180:
        u = 1f - fx / fw;
        v = 1f - fy / fh;
        break;
      case 270:
        u = fy / fh;
        v = 1f - fx / fw;
        break;
      case 0:
      default:
        u = fx / fw;
        v = fy / fh;
        break;
    }
    outXY[0] = clamp(u * viewW, 0f, viewW - 1f);
    outXY[1] = clamp(v * viewH, 0f, viewH - 1f);
  }

  private static float clamp(float v, float lo, float hi) {
    return Math.max(lo, Math.min(hi, v));
  }

  private void updateSlotOverlay(@Nullable TrayCountingHelper.Result r) {
    if (r != null && r.slotBounds != null && r.slotBounds.length >= 4) {
      applySlotBoundsToOverlay(r.slotBounds);
    } else if (roiConfirmed) {
      refreshSlotOverlayFromHelper();
    } else if (roiOverlay != null) {
      roiOverlay.setSlotRects(null);
    }
  }

  private void applySlotBoundsToOverlay(int[] b) {
    if (roiOverlay == null || b == null || b.length < 4) {
      if (roiOverlay != null) roiOverlay.setSlotRects(null);
      return;
    }
    int viewW = roiMapViewW();
    int viewH = roiMapViewH();
    if (viewW <= 0 || viewH <= 0) return;
    try {
      java.util.List<RectF> rects = new java.util.ArrayList<>();
      float[] tmp = new float[2];
      for (int i = 0; i + 3 < b.length; i += 4) {
        mapFrameToView(b[i], b[i + 1], viewW, viewH, tmp);
        float x1 = tmp[0];
        float y1 = tmp[1];
        mapFrameToView(b[i] + b[i + 2], b[i + 1] + b[i + 3], viewW, viewH, tmp);
        float x2 = tmp[0];
        float y2 = tmp[1];
        if (!Float.isFinite(x1) || !Float.isFinite(y1) || !Float.isFinite(x2) || !Float.isFinite(y2)) {
          continue;
        }
        float left = Math.min(x1, x2);
        float top = Math.min(y1, y2);
        float right = Math.max(x1, x2);
        float bottom = Math.max(y1, y2);
        if (right - left < 2f || bottom - top < 2f) continue;
        rects.add(new RectF(left, top, right, bottom));
      }
      roiOverlay.setSlotRects(rects.isEmpty() ? null : rects);
    } catch (Throwable t) {
      Log.e(TAG, "applySlotBoundsToOverlay failed", t);
      roiOverlay.setSlotRects(null);
    }
  }

  private void applyRoiToActiveHelper(FrameRoi r) {
    if (trayHelpers == null) return;
    if (currentCameraId < 0 || currentCameraId >= TRAY_CAMERA_COUNT) return;
    TrayCountingHelper h = trayHelpers[currentCameraId];
    if (h == null) return;
    h.setRoiEllipse(r.frameW, r.frameH, r.cx, r.cy, r.rx, r.ry);
    Log.i(TAG, "ROI cam" + currentCameraId + " frame " + r.frameW + "x" + r.frameH
        + " ellipse " + r.cx + "," + r.cy + " r=" + r.rx + "," + r.ry);
  }

  /** Draw slot grid on the ROI overlay (preview grid before/during calibration). */
  private void refreshSlotOverlayFromHelper() {
    if (!roiConfirmed || trayHelpers == null) return;
    if (currentCameraId < 0 || currentCameraId >= TRAY_CAMERA_COUNT) return;
    TrayCountingHelper h = trayHelpers[currentCameraId];
    if (h == null) return;
    applySlotBoundsToOverlay(h.getPreviewSlotBounds());
  }

  @Override
  public void onResume() {
    super.onResume();
    cameraCount = Math.max(1, Camera.getNumberOfCameras());
    if (currentCameraId >= TRAY_CAMERA_COUNT) {
      currentCameraId = 0;
    }
    rebuildIndexButtons();
    highlightActiveIndex();
  }

  @Override
  public void onPause() {
    cancelPendingCameraOpen();
    stopPreview();
    super.onPause();
  }

  private void requestStart() {
    if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
        != PackageManager.PERMISSION_GRANTED) {
      requestPermissions(new String[] {Manifest.permission.CAMERA}, REQ_CAMERA);
      return;
    }
    startRequested = true;
    if (surfaceReady) {
      startPreview();
    } else {
      statusText.setText("Waiting for preview surfaceâ€¦");
    }
  }

  @Override
  public void onRequestPermissionsResult(
      int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    if (requestCode == REQ_CAMERA) {
      if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
        requestStart();
      } else {
        Toast.makeText(requireContext(), R.string.cam_no_permission, Toast.LENGTH_SHORT).show();
      }
    }
  }

  private void cancelPendingCameraOpen() {
    if (pendingOpenRunnable != null) {
      mainHandler.removeCallbacks(pendingOpenRunnable);
      pendingOpenRunnable = null;
    }
    pendingOpenOnReady = null;
    cancelAttachDetectionCallback();
  }

  private void cancelAttachDetectionCallback() {
    previewGeneration++;
    if (pendingAttachCallbacks != null) {
      mainHandler.removeCallbacks(pendingAttachCallbacks);
      pendingAttachCallbacks = null;
    }
  }

  private void startPreview() {
    if (previewing) return;
    SurfaceHolder holder = surfaceView.getHolder();
    if (holder == null || holder.getSurface() == null) {
      return;
    }
    if (currentCameraId < 0 || currentCameraId >= TRAY_CAMERA_COUNT) {
      currentCameraId = 0; // tray tab only uses cameras 0â€“2
    }
    if (currentCameraId >= Camera.getNumberOfCameras()) {
      Toast.makeText(requireContext(), "Invalid camera id " + currentCameraId, Toast.LENGTH_SHORT)
          .show();
      return;
    }
    cancelPendingCameraOpen();
    releaseCamera();
    startRequested = true;
    statusText.setText(getString(R.string.cam_status_opening, currentCameraId));
    final int openId = currentCameraId;
    pendingOpenOnReady = null;
    pendingOpenRunnable = () -> openCameraInternal(openId, 0, null);
    mainHandler.postDelayed(pendingOpenRunnable, CAMERA_OPEN_DELAY_MS);
  }

  /**
   * Opens a camera after a release delay. Sets preview display before getParameters
   * (required on some Keenon builds) and retries if HAL was still locked.
   */
  private void openCameraInternal(int camId, int attempt, @Nullable Runnable onOpened) {
    pendingOpenRunnable = null;
    if (!startRequested || !isAdded() || previewing) return;
    SurfaceHolder holder = surfaceView.getHolder();
    if (holder == null || holder.getSurface() == null) return;

  try {
      if (camera != null) {
        releaseCamera();
      }
      Log.i(TAG, "Opening camera " + camId + " attempt " + (attempt + 1));
      camera = Camera.open(camId);
      if (camera == null) {
        throw new RuntimeException("Camera.open returned null");
      }

      final int activeCam = camId;
      camera.setErrorCallback((error, cam) -> {
        Log.e(TAG, "Camera hardware error: " + error + " on camera " + activeCam);
        mainHandler.post(() -> handleCameraHalError(activeCam, error));
      });

      Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
      Camera.getCameraInfo(camId, cameraInfo);
      displayOrientation = cameraInfo.orientation;
      frontFacing = cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT;
      camera.setDisplayOrientation(displayOrientation);
      camera.setPreviewDisplay(holder);

      Camera.Parameters params = safeGetParameters(camera);
      if (params == null && attempt + 1 < MAX_CAMERA_OPEN_ATTEMPTS) {
        Log.w(TAG, "Camera " + camId + " getParameters failed â€” retry");
        releaseCamera();
        mainHandler.postDelayed(
            () -> openCameraInternal(camId, attempt + 1, onOpened), CAMERA_OPEN_DELAY_MS);
        return;
      }
      if (params == null) {
        Log.w(TAG, "Camera " + camId + " getParameters still null â€” using HAL defaults");
      }

      if (params != null) {
        Camera.Size size = chooseSize(params.getSupportedPreviewSizes());
        if (size != null) {
          params.setPreviewSize(size.width, size.height);
          previewWidth = size.width;
          previewHeight = size.height;
        }
        // Do not set focus mode on tray cameras â€” some Keenon HAL builds crash mediaserver.
        params.setPreviewFormat(ImageFormat.NV21);
        safeSetParameters(camera, params);
      }

      syncPreviewDimensionsFromCamera();
      if (previewWidth <= 0 || previewHeight <= 0) {
        previewWidth = 640;
        previewHeight = 480;
      }

      startDetectThread();
      camera.startPreview();
      previewing = true;
      currentCameraId = camId;
      placeholder.setVisibility(View.GONE);
      btnStart.setEnabled(false);
      btnStop.setEnabled(true);
      statusText.setText(
          getString(R.string.cam_status_running, currentCameraId)
              + " Â· "
              + previewWidth
              + "Ã—"
              + previewHeight);
      previewLayoutReady = false;
      roiLayoutRetryCount = 0;
      updateRoiEditButtonVisibility();
      halRecoveryAttempts = 0;
      resizeSurfaceToAspect();
      attachDetectionCallbackDeferred();
      mainHandler.postDelayed(this::ensureRoiUiVisible, 500L);
      highlightActiveIndex();
      highlightSlotCount();
      highlightTrayMode();
      updateModeBanner();
      if (onOpened != null) {
        onOpened.run();
      }
      pendingOpenOnReady = null;
    } catch (IOException | RuntimeException e) {
      Log.e(TAG, "openCameraInternal failed cam=" + camId + " attempt=" + attempt, e);
      releaseCamera();
      if (attempt + 1 < MAX_CAMERA_OPEN_ATTEMPTS) {
        mainHandler.postDelayed(
            () -> openCameraInternal(camId, attempt + 1, onOpened), CAMERA_OPEN_DELAY_MS);
        return;
      }
      pendingOpenOnReady = null;
      int n = Camera.getNumberOfCameras();
      Toast.makeText(
              requireContext(),
              getString(R.string.count_camera_unavailable, camId, n),
              Toast.LENGTH_LONG)
          .show();
      startRequested = false;
      highlightActiveIndex();
    }
  }

  private void resizeSurfaceToAspect() {
    if (previewWidth <= 0 || previewHeight <= 0 || previewContainer == null) return;
    ViewTreeObserver vto = previewContainer.getViewTreeObserver();
    vto.addOnGlobalLayoutListener(
        new ViewTreeObserver.OnGlobalLayoutListener() {
          @Override
          public void onGlobalLayout() {
            int w = previewContainer.getWidth();
            int h = previewContainer.getHeight();
            if (w <= 0 || h <= 0) return;
            previewContainer.getViewTreeObserver().removeOnGlobalLayoutListener(this);

            // Aspect of preview as shown on screen (after setDisplayOrientation).
            float camAspect = displayAspect();
            int targetW;
            int targetH;
            if (w / (float) h > camAspect) {
              targetH = h;
              targetW = Math.round(h * camAspect);
            } else {
              targetW = w;
              targetH = Math.round(w / camAspect);
            }
            applyPreviewLayout(targetW, targetH);
            refreshRoiOverlayAfterLayout();
          }
        });
    previewContainer.requestLayout();
  }

  private float displayAspect() {
    if (previewWidth <= 0 || previewHeight <= 0) return 4f / 3f;
    if (displayOrientation == 90 || displayOrientation == 270) {
      return (float) previewHeight / previewWidth;
    }
    return (float) previewWidth / previewHeight;
  }

  private void applyPreviewLayout(int targetW, int targetH) {
    layoutResizing = true;
    previewLayoutW = targetW;
    previewLayoutH = targetH;
    FrameLayout.LayoutParams lp =
        new FrameLayout.LayoutParams(targetW, targetH, Gravity.CENTER);
    surfaceView.setLayoutParams(lp);
    if (roiOverlay != null) {
      roiOverlay.setLayoutParams(new FrameLayout.LayoutParams(targetW, targetH, Gravity.CENTER));
    }
    surfaceView.post(() -> layoutResizing = false);
  }

  private void handleCameraHalError(int camId, int error) {
    releaseCamera();
    stopPreview();
    if (error == Camera.CAMERA_ERROR_SERVER_DIED && halRecoveryAttempts < 2 && isAdded()) {
      halRecoveryAttempts++;
      int next = camId + 1;
      if (next < TRAY_CAMERA_COUNT) {
        currentCameraId = next;
        highlightActiveIndex();
      }
      Toast.makeText(
              requireContext(),
              getString(R.string.count_camera_hal_recovering, camId),
              Toast.LENGTH_LONG)
          .show();
      startRequested = true;
      mainHandler.postDelayed(this::startPreview, 1500L);
      return;
    }
    String msg = error == Camera.CAMERA_ERROR_SERVER_DIED
        ? getString(R.string.count_camera_hal_crashed, camId)
        : "Camera hardware error " + error;
    Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show();
  }

  /** Re-map saved ROI after letterbox layout; first entry into ROI selection for this preview. */
  private void refreshRoiOverlayAfterLayout() {
    if (roiOverlay == null || !previewing) return;
    roiOverlay.post(() -> {
      if (!previewing) return;
      previewLayoutReady = isRoiMappingReady();
      if (!previewLayoutReady && roiLayoutRetryCount++ < 20) {
        roiOverlay.postDelayed(this::refreshRoiOverlayAfterLayout, 50L);
        return;
      }
      previewLayoutReady = true;
      enterRoiSelection();
      updateRoiEditButtonVisibility();
    });
  }

  /** Fallback if layout callback never reaches a ready overlay size. */
  private void ensureRoiUiVisible() {
    if (!previewing || roiOverlay == null) return;
    if (roiOverlay.getVisibility() != View.VISIBLE) {
      Log.w(TAG, "ROI UI not shown after preview start â€” forcing enterRoiSelection");
      previewLayoutReady = true;
      enterRoiSelection();
    }
    updateRoiEditButtonVisibility();
  }

  private void updateRoiEditButtonVisibility() {
    if (btnRoiEdit == null) return;
    boolean editing = roiRow != null && roiRow.getVisibility() == View.VISIBLE;
    boolean show = previewing && !editing;
    btnRoiEdit.setVisibility(show ? View.VISIBLE : View.GONE);
  }

  /** Reset surface/overlay to full container so the next start recomputes letterboxing. */
  private void resetPreviewSurfaceLayout() {
    previewLayoutReady = false;
    previewLayoutW = 0;
    previewLayoutH = 0;
    if (surfaceView == null) return;
    FrameLayout.LayoutParams full =
        new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT,
            Gravity.CENTER);
    surfaceView.setLayoutParams(full);
    if (roiOverlay != null) {
      roiOverlay.setLayoutParams(full);
    }
  }

  /** Stops camera hardware without clearing pick-feedback state. */
  private void releaseCameraOnly() {
    if (camera != null) {
      try { camera.setPreviewCallbackWithBuffer(null); } catch (Exception ignored) {}
      try { camera.stopPreview(); } catch (Exception ignored) {}
      try { camera.release(); } catch (Exception ignored) {}
      camera = null;
    }
    previewing = false;
    stopDetectThread();
  }

  private void stopPreview() {
    startRequested = false;
    cancelPendingCameraOpen();
    releaseCamera();
    placeholder.setVisibility(View.VISIBLE);
    btnStart.setEnabled(true);
    btnStop.setEnabled(false);
    statusText.setText(R.string.cam_status_idle);
    highlightActiveIndex();
    if (modeBanner != null) modeBanner.setVisibility(View.GONE);
    updateRoiEditButtonVisibility();
  }

  private void releaseCamera() {
    if (camera != null) {
      try { camera.setPreviewCallbackWithBuffer(null); } catch (Exception ignored) {}
      try { camera.stopPreview(); } catch (Exception ignored) {}
      try { camera.release(); } catch (Exception ignored) {}
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
    resetPreviewSurfaceLayout();
  }

  // ---------- Detection plumbing ----------

  private void startDetectThread() {
    if (detectThread != null) return;
    detectThread = new HandlerThread("TrayCount");
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

  /** Wait until preview + letterbox layout settle â€” NV21 callbacks during resize crash HAL. */
  private void attachDetectionCallbackDeferred() {
    cancelAttachDetectionCallback();
    final int gen = previewGeneration;
    pendingAttachCallbacks =
        () -> {
          pendingAttachCallbacks = null;
          if (gen != previewGeneration || !previewing || camera == null) return;
          syncPreviewDimensionsFromCamera();
          attachDetectionCallback();
        };
    mainHandler.postDelayed(pendingAttachCallbacks, 550L);
  }

  private void attachDetectionCallback() {
    if (camera == null) return;
    try {
      camera.setPreviewCallbackWithBuffer(null);
    } catch (Exception ignored) {}
    int w = previewWidth;
    int h = previewHeight;
    if (w <= 0 || h <= 0) {
      w = 640;
      h = 480;
    }
    int bufSize = w * h * 3 / 2; // NV21 = 1.5 bytes/px
    camera.addCallbackBuffer(new byte[bufSize]);
    camera.addCallbackBuffer(new byte[bufSize]);
    camera.setPreviewCallbackWithBuffer(this::onPreviewFrame);
  }

  private void syncPreviewDimensionsFromCamera() {
    if (camera == null) return;
    Camera.Size sz = safeGetCurrentPreviewSize(camera);
    if (sz != null && sz.width > 0 && sz.height > 0) {
      previewWidth = sz.width;
      previewHeight = sz.height;
    }
  }

  private void onPreviewFrame(byte[] data, Camera cam) {
    if (data == null || cam == null) return;
    if (personFrameSink != null) {
      personFrameSink.onNv21Frame(data, previewWidth, previewHeight);
      try { cam.addCallbackBuffer(data); } catch (Exception ignored) {}
      return;
    }
    if (pausedForPickFeedback || (pickFeedback != null && pickFeedback.isBusy())) {
      try { cam.addCallbackBuffer(data); } catch (Exception ignored) {}
      return;
    }
    long now = SystemClock.uptimeMillis();
    TrayCountingHelper throttleHelper =
        (roiConfirmed && trayHelpers != null && currentCameraId >= 0
            && currentCameraId < TRAY_CAMERA_COUNT)
            ? trayHelpers[currentCameraId]
            : null;
    long minGap =
        (throttleHelper != null && throttleHelper.isCalibrating()) ? 200L : DETECT_INTERVAL_MS;
    if (detectBusy.get() || now - lastDetectAt < minGap) {
      try { cam.addCallbackBuffer(data); } catch (Exception ignored) {}
      return;
    }
    detectBusy.set(true);
    lastDetectAt = now;
    final int w = previewWidth;
    final int h = previewHeight;
    final byte[] frameCopy = new byte[data.length];
    System.arraycopy(data, 0, frameCopy, 0, data.length);
    final Handler h2 = detectHandler;
    if (h2 == null) {
      detectBusy.set(false);
      try { cam.addCallbackBuffer(data); } catch (Exception ignored) {}
      return;
    }
    h2.post(() -> {
      TrayCountingHelper helper = (roiConfirmed
          && currentCameraId >= 0 && currentCameraId < TRAY_CAMERA_COUNT
          && trayHelpers != null)
          ? trayHelpers[currentCameraId] : null;
      TrayCountingHelper.Result r = null;
      if (helper != null) {
        r = helper.process(frameCopy, w, h);
      }
      final TrayCountingHelper.Result result = r;
      mainHandler.post(() -> renderResult(result));
      if (helper != null
          && !helper.isCalibrating()
          && getTrayMode() == TrayOperationMode.CUSTOMER_READY) {
        scheduleHandTrack(frameCopy, w, h);
      }
      if (camera != null) {
        try { camera.addCallbackBuffer(data); } catch (Exception ignored) {}
      }
      detectBusy.set(false);
    });
  }

  /** Update the big number + small status line from a counting result. */
  private void renderResult(@Nullable TrayCountingHelper.Result r) {
    boolean calibrating = r != null && r.calibrating;
    if (wasCalibrating && !calibrating) {
      handDetectAllowedAfterMs = SystemClock.uptimeMillis() + 3000L;
      // Load hand NCNN only when customer mode needs live tracking (avoids early SIGSEGV during stocking).
      if (getTrayMode() == TrayOperationMode.CUSTOMER_READY) {
        ensureHandDetectorLazy();
      }
    }
    wasCalibrating = calibrating;
    if (r == null) {
      if (previewing && !roiConfirmed) {
        bigNumber.setText("â€¦");
        statusLine.setText(R.string.count_roi_needed);
        statusLine.setVisibility(View.VISIBLE);
      } else {
        bigNumber.setText("--");
        statusLine.setVisibility(View.GONE);
      }
      return;
    }
    if (r.calibrating) {
      int pct = Math.max(0, r.calibratePercent);
      bigNumber.setText(pct + "%");
      String line = r.overlay != null ? r.overlay : getString(R.string.count_calibrating_hint);
      statusLine.setText(line);
      statusLine.setVisibility(View.VISIBLE);
      return;
    }
    updateSlotOverlay(r);
    bigNumber.setText(r.count + " / " + r.maxSlots);
    if (r.overlay != null && r.overlay.contains("error")) {
      statusLine.setText(r.overlay);
      statusLine.setVisibility(View.VISIBLE);
    } else if (r.event == TrayCountingHelper.EventType.PICKED_UP) {
      int prev = r.count + r.eventDelta;
      onCustomerTrayPick(prev, r.count, r);
      if (getTrayMode() == TrayOperationMode.CUSTOMER_READY) {
        statusLine.setText(getString(R.string.count_picked_up, prev, r.count));
        statusLine.setVisibility(View.VISIBLE);
        if (pickFeedback != null && !pickFeedback.isBusy()) {
          pausedForPickFeedback = true;
          float handX = lastHandNormX >= 0f ? lastHandNormX : r.pickHandNormX;
          pickFeedback.start(prev, r.count, handX);
        }
      } else {
        statusLine.setText(R.string.count_pick_only_in_customer_mode);
        statusLine.setVisibility(View.VISIBLE);
      }
    } else if (r.event == TrayCountingHelper.EventType.DELIVERED) {
      int prev = Math.max(0, r.count - r.eventDelta);
      statusLine.setText(getString(R.string.count_event_placed, prev, r.count));
      statusLine.setVisibility(View.VISIBLE);
    } else if (r.overlay != null) {
      statusLine.setText(r.overlay);
      statusLine.setVisibility(View.VISIBLE);
    } else {
      statusLine.setVisibility(View.GONE);
    }
  }

  /** Queues hand-model load on {@link #handPipelineHandler} (same thread as detect). */
  private void ensureHandDetectorLazy() {
    if (!isAdded() || handPipelineHandler == null) return;
    handPipelineHandler.post(this::ensureHandDetectorOnPipeline);
  }

  /** Load hand NCNN on HandPipeline thread only; returns true when ready for detect. */
  private boolean ensureHandDetectorOnPipeline() {
    if (handDetector != null && handDetector.isReady()) return true;
    Context c =
        getContext() != null ? getContext().getApplicationContext() : null;
    if (c == null) return false;
    try {
      HandDetectorHelper h = new HandDetectorHelper(c);
      if (h.ensureLoaded()) {
        handDetector = h;
        return true;
      }
    } catch (Throwable t) {
      Log.w(TAG, "Hand model load failed", t);
    }
    return false;
  }

  private void scheduleHandTrack(byte[] nv21, int w, int h) {
    if (handPipelineHandler == null || nv21 == null || w <= 0 || h <= 0) return;
    long now = SystemClock.uptimeMillis();
    if (now < handDetectAllowedAfterMs) return;
    final byte[] copy = Arrays.copyOf(nv21, nv21.length);
    handPipelineHandler.post(() -> runHandTrackOnPipeline(copy, w, h));
  }

  private void runHandTrackOnPipeline(byte[] nv21, int w, int h) {
    try {
      if (!ensureHandDetectorOnPipeline()) return;
      long now = SystemClock.uptimeMillis();
      if (now < handDetectAllowedAfterMs) return;
      if (now - lastHandDetectAt < HAND_TRACK_INTERVAL_MS) return;
      if (w <= 0 || h <= 0 || nv21 == null) return;
      lastHandDetectAt = now;
      byte[] jpeg = nv21ToJpeg(nv21, w, h);
      if (jpeg == null || jpeg.length < 4) return;
      float[] xy = handDetector.detectCenterXY(jpeg, w, h);
      if (xy != null) {
        lastHandNormX = xy[0];
        lastHandNormY = xy[1];
      }
    } catch (Throwable t) {
      Log.w(TAG, "Hand track failed", t);
    }
  }

  private static byte[] nv21ToJpeg(byte[] nv21, int width, int height) {
    try {
      YuvImage yuv = new YuvImage(nv21, ImageFormat.NV21, width, height, null);
      ByteArrayOutputStream out = new ByteArrayOutputStream(width * height / 4);
      yuv.compressToJpeg(new Rect(0, 0, width, height), 82, out);
      return out.toByteArray();
    } catch (Throwable t) {
      return null;
    }
  }

  @Override
  public void onDestroyView() {
    cancelPendingCameraOpen();
    releaseCamera();
    if (pickFeedback != null) {
      pickFeedback.cancel();
      pickFeedback = null;
    }
    pausedForPickFeedback = false;
    personFrameSink = null;
    if (handPipelineHandler != null) {
      handPipelineHandler.post(
          () -> {
            if (handDetector != null) {
              try {
                handDetector.close();
              } catch (Throwable ignored) {
              }
              handDetector = null;
            }
          });
    }
    if (handPipelineThread != null) {
      handPipelineThread.quitSafely();
      handPipelineThread = null;
      handPipelineHandler = null;
    }
    ttsHelper.shutdown();
    if (trayHelpers != null) {
      for (TrayCountingHelper h : trayHelpers) {
        if (h != null) h.close();
      }
      trayHelpers = null;
    }
    super.onDestroyView();
  }

  // ---------- Camera helpers ----------

  /** Prefer â‰¤640Ã—480 â€” large previews + NV21 callbacks crash mediaserver on Keenon trays. */
  private Camera.Size chooseSize(List<Camera.Size> sizes) {
    if (sizes == null || sizes.isEmpty()) return null;
    Camera.Size best = null;
    for (Camera.Size s : sizes) {
      if (s.width <= 640 && s.height <= 480) {
        if (best == null || (s.width * s.height) > (best.width * best.height)) {
          best = s;
        }
      }
    }
    if (best != null) return best;
    best = sizes.get(0);
    for (Camera.Size s : sizes) {
      if (s.width * s.height < best.width * best.height) {
        best = s;
      }
    }
    return best;
  }

  private Camera.Parameters safeGetParameters(Camera camera) {
    try {
      return camera.getParameters();
    } catch (RuntimeException e) {
      Log.w(TAG, "getParameters failed", e);
      return null;
    }
  }

  private void safeSetParameters(Camera camera, Camera.Parameters params) {
    try {
      camera.setParameters(params);
    } catch (RuntimeException e) {
      Log.w(TAG, "setParameters failed, continue with defaults", e);
    }
  }

  private Camera.Size safeGetCurrentPreviewSize(Camera camera) {
    try {
      Camera.Parameters params = camera.getParameters();
      if (params != null) {
        return params.getPreviewSize();
      }
    } catch (RuntimeException e) {
      Log.w(TAG, "Unable to read current preview size", e);
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
    if (camera == null || !previewing || layoutResizing || width <= 0 || height <= 0) {
      return;
    }
    mainHandler.post(() -> restartPreviewOnSurface(holder));
  }

  private void restartPreviewOnSurface(SurfaceHolder holder) {
    if (camera == null || !previewing || layoutResizing) return;
    try {
      camera.setPreviewCallbackWithBuffer(null);
      camera.stopPreview();
      camera.setPreviewDisplay(holder);
      camera.startPreview();
      syncPreviewDimensionsFromCamera();
      attachDetectionCallback();
    } catch (Exception e) {
      Log.e(TAG, "surfaceChanged preview restart failed", e);
    }
  }

  @Override
  public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
    surfaceReady = false;
    releaseCamera();
  }
}

