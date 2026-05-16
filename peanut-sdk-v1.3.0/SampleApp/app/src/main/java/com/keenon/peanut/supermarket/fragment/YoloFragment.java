package com.keenon.peanut.supermarket.fragment;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.content.SharedPreferences;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.keenon.peanut.sample.R;
import com.keenon.peanut.sample.receiver.RobotMovementManager;
import com.keenon.peanut.supermarket.SupermarketActivity;
import com.keenon.peanut.supermarket.util.TtsHelper;
import com.keenon.peanut.supermarket.stream.DetectionStreamServer;
import com.keenon.peanut.supermarket.vision.ObjectDetectorHelper;
import com.keenon.peanut.supermarket.yolo.BodyDetectorHelper;
import com.keenon.peanut.supermarket.yolo.HandDetectorHelper;
import com.keenon.peanut.supermarket.yolo.KNCameraHelper;
import com.keenon.peanut.supermarket.yolo.KNCaptureCallback;
import com.keenon.peanut.supermarket.yolo.TrayItemTracker;
import com.keenon.peanut.supermarket.yolo.YoloDetectorHelper;
import com.keenon.sdk.constant.ApiConstants;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Developer → YOLO tab.
 *
 * Two camera modes:
 *   - Android mode: Camera1 API with SurfaceView preview. Works on any device.
 *   - Native mode:  Keenon JNI (/dev/videoN via libcamerasdk.so). For robot tray cameras.
 *
 * Item-pick detection:
 *   When the number of "plate" detections drops (debounced over 3 frames), the robot:
 *     1. Rotates LEFT for ~1.5 s to face the person who just picked the item
 *     2. Says "Please give your feedback" via TTS
 *     3. Shows a full-screen star-rating dialog
 */
public class YoloFragment extends Fragment implements SurfaceHolder.Callback {

    private static final String TAG = "YoloFragment";
    private static final int REQ_CAMERA = 3301;

    private static final long DETECT_INTERVAL_MS       = 300L;
    /** Space out native captures slightly — RK3288 + YUYV→JPEG + NCNN is heavy. */
    private static final long NATIVE_CAPTURE_INTERVAL_MS = 650L;
    /** Native NCNN/OpenCV path: do not run inference every frame (reduces native crashes / OOM). */
    private static final long MIN_NATIVE_DETECT_INTERVAL_MS = 550L;
    /**
     * OEM libyolov5/libncnn is still unstable on native /dev/video path on this robot build
     * (SIGSEGV in ncnn::Mat::from_pixels). Keep preview active but skip native JNI inference.
     */
    private static final boolean ENABLE_NATIVE_YOLO_INFERENCE = false;
    // How long (ms) the robot rotates to do a full 180° turn toward the person behind it
    private static final long ROTATION_DURATION_MS     = 3000L;
    // Camera id used for person-facing interaction trigger.
    private static final int PERSON_TRIGGER_CAMERA_ID = 3;
    private static final int PERSON_CONFIRM_FRAMES = 2;
    private static final long PERSON_COOLDOWN_MS = 12000L;
    // Runnable tag used to cancel pending scan step callbacks
    private Runnable pendingScanStepRunnable;

    // ---- Views ----
    private FrameLayout previewContainer;
    private SurfaceView surfaceView;
    private TextView statusText, fpsOverlay;
    private TextView detectionsOverlay;   // item list lines (green)
    private TextView detCountText;        // "Items on tray: N" header
    private View     detPanel;            // whole detection panel (LinearLayout)
    private View     placeholder;         // idle placeholder LinearLayout
    private View     roiBorder;           // live zone rectangle overlay
    private LinearLayout indexRow;

    // ---- Detection Zone sliders ----
    private static final String PREFS_ZONE = "yolo_zone";
    private SeekBar zoneTop, zoneBottom, zoneLeft, zoneRight;
    private TextView zoneTopVal, zoneBottomVal, zoneLeftVal, zoneRightVal;
    // Current crop fractions (0.0–0.49) for each edge
    private float zoneCropTop = 0f, zoneCropBottom = 0f, zoneCropLeft = 0f, zoneCropRight = 0f;
    private Button btnStart, btnStop;
    private RadioButton modeAndroid, modeNative;
    private final List<Button> indexButtons = new ArrayList<>();

    // ---- State ----
    private boolean useNativeCamera = false;
    private int currentCameraId = 0;
    private int currentVideoNode = 7;
    private int cameraCount;
    private final List<Integer> nativeVideoNodes = new ArrayList<>();

    // ---- Android Camera (mode = Android) ----
    private Camera androidCamera;
    private int previewWidth, previewHeight;
    private boolean previewing, surfaceReady, startRequested;
    /** Monotonic token to reject stale preview callbacks after camera switch/stop. */
    private final AtomicInteger androidCameraSession = new AtomicInteger(0);

    // ---- Keenon native camera (mode = Native) ----
    private long nativeFd = -1;
    private byte[] nativeCaptureBuffer;
    private final AtomicBoolean nativeCapturePending = new AtomicBoolean(false);
    private long lastNativeDetectAtMs;
    private boolean nativeInferenceWarned;

    // ---- Native preview: coalesce draws so the main thread never queues many full Bitmaps ----
    private final Object previewLock = new Object();
    private byte[] previewPendingJpeg;
    private boolean previewFlushPosted;
    private final Runnable previewFlushRunnable = new Runnable() {
        @Override public void run() {
            flushPreviewDrawRunnable();
        }
    };

    // ---- Detection mode ----
    private boolean useCoco = false;  // false = NCNN tray model, true = TFLite COCO

    // ---- TFLite model catalogue (asset path, display name) ----
    // To use Open Images V7: download lite-model_efficientdet_lite2_detection_metadata_1.tflite
    // from https://tfhub.dev/tensorflow/lite-model/efficientdet/lite2/detection/metadata/1
    // rename it to open_images_v7.tflite and place it in assets/models/
    private static final String[] MODEL_PATHS = {
        "models/efficientdet_lite0.tflite",
        "models/efficientdet_lite1.tflite",
        "models/efficientdet_lite2.tflite",
        "models/ssd_mobilenet_v3.tflite",
        "models/open_images_v7.tflite",
    };
    private static final String[] MODEL_NAMES = {
        "EfficientDet-Lite0 (COCO fast)",
        "EfficientDet-Lite1 (COCO better)",
        "EfficientDet-Lite2 (COCO balanced)",
        "SSD MobileNetV3 (COCO fast)",
        "Open Images V7 (600 classes)",
    };
    private int selectedModelIndex = 0;
    private Spinner modelSpinner;

    // ---- YOLO inference (TRAY mode — NCNN) ----
    private YoloDetectorHelper detector;
    // ---- General detection (COCO/Open Images mode — TFLite) ----
    private ObjectDetectorHelper cocoDetector;
    // ---- Fast person detector used exclusively on camera 3 during person scan ----
    private ObjectDetectorHelper personScanDetector;
    private static final String PERSON_SCAN_MODEL = "models/ssd_mobilenet_v3.tflite";
    // ---- Keenon body_detection model — used for person scan (better than COCO "person") ----
    private BodyDetectorHelper bodyDetector;
    // ---- Keenon hand_detect model — used to get accurate pick direction ----
    private HandDetectorHelper handDetector;

    // ---- Hand direction hint (set when item disappears, used to aim scan rotation) ----
    // Normalised X centre of the last disappearing item bbox: <0.5 = left side, >=0.5 = right side.
    // -1 = unknown.
    private float lastItemDisappearX = -1f;

    // ---- Latest JPEG from TRAY camera — used to run body_detection when on camera 3 ----
    private byte[] lastJpegForBodyDetect;
    private int    lastJpegWidth;
    private int    lastJpegHeight;
    private HandlerThread detectThread;
    private Handler detectHandler;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final AtomicBoolean detectBusy = new AtomicBoolean(false);
    private final AtomicBoolean detectInFlight = new AtomicBoolean(false);
    private final AtomicInteger frameSeq = new AtomicInteger(0);
    private int droppedDetectFrames;
    private long lastDetectAt;
    private long lastFrameTime;
    private int frameCount;

    // ---- Item-pick detection ----
    private TrayItemTracker trayTracker;
    // Prevents overlapping rotation+feedback sequences
    private final AtomicBoolean pickHandling = new AtomicBoolean(false);
    private int personSeenFrames;
    private long lastPersonTriggerAtMs;

    // ---- COCO item-count tracking (mirrors TrayItemTracker logic for COCO mode) ----
    private int cocoConfirmedCount  = -1;  // last stable item count in COCO mode
    private int cocoPendingCount    = -1;
    private int cocoPendingFrames   = 0;
    private long cocoLastFiredAt    = 0L;
    // 1 frame = immediate response once the model sees the drop; cooldown reduced so rapid
    // successive picks each trigger the feedback loop.
    private static final int  COCO_CONFIRM_FRAMES = 1;
    private static final long COCO_COOLDOWN_MS    = 2_000L;

    // ---- Person-search state (activated after item count drops) ----
    /** Camera we were on before person search. -1 = not searching. */
    private int preSwitchCameraId = -1;
    /** Pick counts saved when item drop was detected. */
    private int pendingPickPrevCount = -1;
    private int pendingPickNewCount  = -1;
    /** Total time (ms) to rotate + look before giving up. */
    private static final long PERSON_SEARCH_TIMEOUT_MS  = 30000L;
    /**
     * How long to rotate per step. At ~50°/s robot speed, 1800ms ≈ 90° per step.
     * 4 steps × 90° = 360° full sweep. Increase this value if the robot still moves too little.
     */
    private static final long SCAN_ROTATE_STEP_MS       = 1800L;
    /** How long to pause for camera 3 to detect person after each rotation step. */
    private static final long SCAN_LOOK_STEP_MS         = 1200L;
    /** 4 steps × 90° = full 360° sweep. */
    private static final int  SCAN_MAX_STEPS            = 4;
    private long personSearchStartedAtMs = 0L;
    private int  scanStep = 0;
    private boolean scanningForPerson = false;

    // ---- Robot rotation + TTS ----
    private final TtsHelper ttsHelper = new TtsHelper();

    // ---- PC streaming (WebSocket server on port 8765) ----
    private static final int STREAM_PORT = 8765;
    private DetectionStreamServer streamServer;

    // ---- Native capture loop ----
    private final Runnable nativeCaptureLoop = new Runnable() {
        @Override
        public void run() {
            if (!previewing || nativeFd < 0) return;

            if (detector == null || !detector.isReady()) {
                Log.d(TAG, "Model not ready yet — waiting before capture");
                if (detectHandler != null) detectHandler.postDelayed(nativeCaptureLoop, 200);
                return;
            }

            if (!nativeCapturePending.get()) {
                nativeCapturePending.set(true);
                final long captureFd = nativeFd;
                final byte[] captureBuffer = nativeCaptureBuffer;
                KNCameraHelper.captureCamera(captureFd, new KNCaptureCallback() {
                    @Override
                    public void onSuccess(long fd, int status) {
                        if (!previewing || captureBuffer == null || nativeFd != captureFd) {
                            nativeCapturePending.set(false);
                            return;
                        }
                        // Copy immediately — native may reuse the buffer after this callback returns.
                        final byte[] snapshot = Arrays.copyOf(captureBuffer, captureBuffer.length);
                        Handler bg = detectHandler;
                        if (bg == null) {
                            nativeCapturePending.set(false);
                            return;
                        }
                        bg.post(() -> {
                            try {
                                if (!previewing || nativeFd != captureFd) return;
                                byte[] jpeg = nativeBufferToJpeg(snapshot, 640, 480);
                                if (jpeg != null) {
                                    schedulePreviewDraw(jpeg);
                                    long now = SystemClock.uptimeMillis();
                                    if (!ENABLE_NATIVE_YOLO_INFERENCE) {
                                        // Preview-only native path never calls runYoloOnFrame — still push JPEGs
                                        // so the PC dashboard (DetectionStreamServer :8765) receives a stream.
                                        if (streamServer != null && streamServer.hasClients()) {
                                            streamServer.pushFrame(currentCameraId, jpeg, "[]",
                                                    1000f / Math.max(1f, (float) NATIVE_CAPTURE_INTERVAL_MS), 0L);
                                        }
                                        if (!nativeInferenceWarned) {
                                            nativeInferenceWarned = true;
                                            Log.w(TAG, "Native YOLO inference disabled for stability; preview-only mode active.");
                                            mainHandler.post(() -> statusText.setText(
                                                    "Native preview only (YOLO disabled for stability). Use Android mode for inference."));
                                        }
                                    } else if (now - lastNativeDetectAtMs >= MIN_NATIVE_DETECT_INTERVAL_MS) {
                                        lastNativeDetectAtMs = now;
                                        runYoloOnFrame(jpeg, 640, 480);
                                    }
                                } else {
                                    Log.w(TAG, "nativeBufferToJpeg returned null — skipping frame");
                                }
                            } catch (Throwable t) {
                                Log.w(TAG, "Native frame processing failed", t);
                            } finally {
                                nativeCapturePending.set(false);
                            }
                        });
                    }
                    @Override
                    public void onFailure(long fd, int status, int errorCode, String message) {
                        Log.w(TAG, "Native capture failed: " + message + " code=" + errorCode);
                        nativeCapturePending.set(false);
                    }
                });
            }
            if (detectHandler != null) {
                detectHandler.postDelayed(nativeCaptureLoop, NATIVE_CAPTURE_INTERVAL_MS);
            }
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_dev_yolo, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);

        previewContainer  = v.findViewById(R.id.yolo_preview_container);
        surfaceView       = v.findViewById(R.id.yolo_surface);
        statusText        = v.findViewById(R.id.yolo_status);
        placeholder       = v.findViewById(R.id.yolo_placeholder);
        detectionsOverlay = v.findViewById(R.id.yolo_detections);
        detCountText      = v.findViewById(R.id.yolo_det_count);
        detPanel          = v.findViewById(R.id.yolo_detections_panel);
        roiBorder         = v.findViewById(R.id.yolo_roi_border);
        fpsOverlay        = v.findViewById(R.id.yolo_fps);
        indexRow          = v.findViewById(R.id.yolo_index_row);
        btnStart          = v.findViewById(R.id.btn_yolo_start);
        btnStop           = v.findViewById(R.id.btn_yolo_stop);
        modeAndroid       = v.findViewById(R.id.yolo_mode_android);
        modeNative        = v.findViewById(R.id.yolo_mode_native);

        detector            = new YoloDetectorHelper(requireContext());
        cocoDetector        = new ObjectDetectorHelper(requireContext());
        personScanDetector  = new ObjectDetectorHelper(requireContext(), PERSON_SCAN_MODEL);
        bodyDetector        = new BodyDetectorHelper(requireContext());
        handDetector        = new HandDetectorHelper(requireContext());
        trayTracker         = new TrayItemTracker(this::onItemPickedDetected);
        ttsHelper.initialize(requireContext());

        // Start WebSocket stream server so the PC dashboard can connect
        try {
            streamServer = new DetectionStreamServer(STREAM_PORT);
            streamServer.start();
        } catch (Throwable t) {
            Log.e(TAG, "Failed to start stream server", t);
            streamServer = null;
        }

        surfaceView.getHolder().addCallback(this);
        btnStart.setOnClickListener(view -> requestStart());
        btnStop.setOnClickListener(view -> stopAll());

        // Model mode toggle: TRAY (NCNN) vs COCO (TFLite)
        RadioButton modelCoco = v.findViewById(R.id.yolo_model_coco);
        RadioButton modelTray = v.findViewById(R.id.yolo_model_tray);
        modelSpinner = v.findViewById(R.id.yolo_tflite_model_spinner);

        // Populate model spinner with available TFLite models
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(
                requireContext(), R.layout.yolo_spinner_item, MODEL_NAMES);
        spinnerAdapter.setDropDownViewResource(R.layout.yolo_spinner_dropdown_item);
        modelSpinner.setAdapter(spinnerAdapter);
        modelSpinner.setSelection(selectedModelIndex, false);
        modelSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == selectedModelIndex) return;
                selectedModelIndex = position;
                // Reload COCO detector with new model
                if (useCoco) {
                    boolean wasRunning = previewing;
                    if (wasRunning) stopAll();
                    cocoDetector.close();
                    cocoDetector = new ObjectDetectorHelper(requireContext(), MODEL_PATHS[position]);
                    cocoDetector.setRoi(zoneCropLeft, zoneCropTop, zoneCropRight, zoneCropBottom);
                    statusText.setText("Model: " + MODEL_NAMES[position] + " — press Start");
                    if (wasRunning) { startRequested = true; mainHandler.post(YoloFragment.this::startCamera); }
                }
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        if (modelCoco != null) {
            modelCoco.setOnCheckedChangeListener((btn, checked) -> {
                if (checked) {
                    if (modelSpinner != null) modelSpinner.setVisibility(View.VISIBLE);
                    switchDetectorMode(true);
                }
            });
        }
        if (modelTray != null) {
            modelTray.setOnCheckedChangeListener((btn, checked) -> {
                if (checked) {
                    if (modelSpinner != null) modelSpinner.setVisibility(View.GONE);
                    switchDetectorMode(false);
                }
            });
        }

        modeAndroid.setOnCheckedChangeListener((btn, checked) -> {
            if (checked) {
                useNativeCamera = false;
                surfaceView.setVisibility(View.VISIBLE);
                if (previewing) stopAll();
                rebuildIndexButtons();
            }
        });
        modeNative.setOnCheckedChangeListener((btn, checked) -> {
            if (checked) {
                useNativeCamera = true;
                surfaceView.setVisibility(View.VISIBLE);
                if (previewing) stopAll();
                rebuildIndexButtons();
            }
        });

        cameraCount = Math.max(1, Camera.getNumberOfCameras());

        nativeVideoNodes.clear();
        for (int n = 0; n <= 15; n++) {
            if (new File("/dev/video" + n).exists()) nativeVideoNodes.add(n);
        }
        if (!nativeVideoNodes.isEmpty()) currentVideoNode = nativeVideoNodes.get(0);

        setupZoneSliders(v);
        rebuildIndexButtons();
        applyStreamButtonIdleState();
    }

    private void setupZoneSliders(View root) {
        TextView zoneToggle = root.findViewById(R.id.yolo_zone_toggle);
        LinearLayout zonePanel = root.findViewById(R.id.yolo_zone_panel);
        zoneTop    = root.findViewById(R.id.yolo_zone_top);
        zoneBottom = root.findViewById(R.id.yolo_zone_bottom);
        zoneLeft   = root.findViewById(R.id.yolo_zone_left);
        zoneRight  = root.findViewById(R.id.yolo_zone_right);
        zoneTopVal    = root.findViewById(R.id.yolo_zone_top_val);
        zoneBottomVal = root.findViewById(R.id.yolo_zone_bottom_val);
        zoneLeftVal   = root.findViewById(R.id.yolo_zone_left_val);
        zoneRightVal  = root.findViewById(R.id.yolo_zone_right_val);
        Button zoneReset = root.findViewById(R.id.yolo_zone_reset);

        // Load zone for the current camera layer
        loadZoneForCurrentCamera();

        // Collapse/expand toggle
        if (zoneToggle != null && zonePanel != null) {
            zoneToggle.setOnClickListener(vv -> {
                boolean visible = zonePanel.getVisibility() == View.VISIBLE;
                zonePanel.setVisibility(visible ? View.GONE : View.VISIBLE);
                zoneToggle.setText(visible ? "▶  DETECTION ZONE" : "▼  DETECTION ZONE");
            });
        }

        SeekBar.OnSeekBarChangeListener listener = new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar sb, int progress, boolean fromUser) {
                if (!fromUser) return;
                int t = zoneTop    != null ? zoneTop.getProgress()    : 0;
                int b = zoneBottom != null ? zoneBottom.getProgress() : 0;
                int l = zoneLeft   != null ? zoneLeft.getProgress()   : 0;
                int r = zoneRight  != null ? zoneRight.getProgress()  : 0;
                applyZoneProgress(t, b, l, r, true);
            }
            @Override public void onStartTrackingTouch(SeekBar sb) {}
            @Override public void onStopTrackingTouch(SeekBar sb) {}
        };
        if (zoneTop    != null) zoneTop.setOnSeekBarChangeListener(listener);
        if (zoneBottom != null) zoneBottom.setOnSeekBarChangeListener(listener);
        if (zoneLeft   != null) zoneLeft.setOnSeekBarChangeListener(listener);
        if (zoneRight  != null) zoneRight.setOnSeekBarChangeListener(listener);

        if (zoneReset != null) {
            zoneReset.setOnClickListener(vv -> {
                if (zoneTop    != null) zoneTop.setProgress(0);
                if (zoneBottom != null) zoneBottom.setProgress(0);
                if (zoneLeft   != null) zoneLeft.setProgress(0);
                if (zoneRight  != null) zoneRight.setProgress(0);
                applyZoneProgress(0, 0, 0, 0, true);
            });
        }
    }

    /** Pref key prefix for a given camera index — e.g. "cam0_top". */
    private String zoneKey(String side) {
        return "cam" + currentCameraId + "_" + side;
    }

    /** Load saved zone sliders for the current camera and push to detector. */
    private void loadZoneForCurrentCamera() {
        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_ZONE, 0);
        int t = prefs.getInt(zoneKey("top"),    0);
        int b = prefs.getInt(zoneKey("bottom"), 0);
        int l = prefs.getInt(zoneKey("left"),   0);
        int r = prefs.getInt(zoneKey("right"),  0);
        // Update sliders without triggering save (fromUser=false)
        if (zoneTop    != null) zoneTop.setProgress(t);
        if (zoneBottom != null) zoneBottom.setProgress(b);
        if (zoneLeft   != null) zoneLeft.setProgress(l);
        if (zoneRight  != null) zoneRight.setProgress(r);
        applyZoneProgress(t, b, l, r, false);
    }

    private void applyZoneProgress(int top, int bottom, int left, int right, boolean save) {
        // SeekBar max=49, so progress/100 gives 0.0–0.49
        zoneCropTop    = top    / 100f;
        zoneCropBottom = bottom / 100f;
        zoneCropLeft   = left   / 100f;
        zoneCropRight  = right  / 100f;

        if (zoneTopVal    != null) zoneTopVal.setText(top    + "%");
        if (zoneBottomVal != null) zoneBottomVal.setText(bottom + "%");
        if (zoneLeftVal   != null) zoneLeftVal.setText(left   + "%");
        if (zoneRightVal  != null) zoneRightVal.setText(right  + "%");

        if (save) {
            requireContext().getSharedPreferences(PREFS_ZONE, 0).edit()
                    .putInt(zoneKey("top"), top).putInt(zoneKey("bottom"), bottom)
                    .putInt(zoneKey("left"), left).putInt(zoneKey("right"), right).apply();
        }

        // Push to detector live
        if (cocoDetector != null) {
            cocoDetector.setRoi(zoneCropLeft, zoneCropTop, zoneCropRight, zoneCropBottom);
        }
        // Update overlay on camera preview
        updateZoneOverlay();
    }

    private void updateZoneOverlay() {
        if (roiBorder == null || previewContainer == null) return;
        boolean anyZone = (zoneCropTop > 0 || zoneCropBottom > 0 || zoneCropLeft > 0 || zoneCropRight > 0);
        if (!anyZone || !useCoco) {
            roiBorder.setVisibility(View.GONE);
            return;
        }
        previewContainer.post(() -> {
            if (previewContainer == null || roiBorder == null) return;
            int pw = previewContainer.getWidth();
            int ph = previewContainer.getHeight();
            if (pw <= 0 || ph <= 0) return;
            int x0 = Math.round(pw * zoneCropLeft);
            int y0 = Math.round(ph * zoneCropTop);
            int x1 = pw - Math.round(pw * zoneCropRight);
            int y1 = ph - Math.round(ph * zoneCropBottom);
            int rw = x1 - x0;
            int rh = y1 - y0;
            if (rw <= 0 || rh <= 0) { roiBorder.setVisibility(View.GONE); return; }
            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(rw, rh);
            lp.leftMargin = x0;
            lp.topMargin  = y0;
            roiBorder.setLayoutParams(lp);
            roiBorder.setVisibility(previewing ? View.VISIBLE : View.GONE);
        });
    }

    /** Start visible, Stop hidden — until preview is running. */
    private void applyStreamButtonIdleState() {
        if (btnStart == null || btnStop == null) return;
        btnStart.setVisibility(View.VISIBLE);
        btnStop.setVisibility(View.GONE);
        btnStart.setEnabled(true);
        btnStop.setEnabled(false);
    }

    /** Stop visible, Start hidden — while camera / stream is active. */
    private void applyStreamButtonRunningState() {
        if (btnStart == null || btnStop == null) return;
        btnStart.setVisibility(View.GONE);
        btnStop.setVisibility(View.VISIBLE);
        btnStart.setEnabled(false);
        btnStop.setEnabled(true);
    }

    // ---- Model mode switch ----

    private void switchDetectorMode(boolean coco) {
        if (useCoco == coco) return;
        boolean wasRunning = previewing;
        if (wasRunning) stopAll();
        useCoco = coco;
        trayTracker.reset();
        statusText.setText("Model: " + (coco ? MODEL_NAMES[selectedModelIndex] + " (TFLite)" : "Tray plate (NCNN)") + " — press Start");
        if (wasRunning) {
            startRequested = true;
            mainHandler.post(this::startCamera);
        }
    }

    // ---- Item pick event — called from detect thread ----

    private void onItemPickedDetected(int previousCount, int newCount) {
        if (!pickHandling.compareAndSet(false, true)) return;

        Log.i(TAG, "Item picked! " + previousCount + " → " + newCount + " — starting person scan");
        pendingPickPrevCount    = previousCount;
        pendingPickNewCount     = newCount;
        personSeenFrames        = 0;
        personSearchStartedAtMs = SystemClock.elapsedRealtime();
        scanStep                = 0;
        scanningForPerson       = true;
        preSwitchCameraId       = currentCameraId;

        // Use hand position hint to pick rotation direction:
        // item disappeared from left side → person is to the left → rotate LEFT, and vice versa.
        if (lastItemDisappearX >= 0) {
            scanDirection = (lastItemDisappearX < 0.5f)
                    ? ApiConstants.MotorMove.LEFT
                    : ApiConstants.MotorMove.RIGHT;
            Log.i(TAG, "Hand was at x=" + lastItemDisappearX + " → scan " +
                    (scanDirection == ApiConstants.MotorMove.LEFT ? "LEFT" : "RIGHT"));
        } else {
            scanDirection = ApiConstants.MotorMove.RIGHT; // default
        }

        mainHandler.post(() -> {
            // Clear stale tray detections before switching to camera 3
            showDetections("");
            if (personScanDetector != null) personScanDetector.resetSmoothing();
            // Switch to camera 3 once — stays there for the entire scan
            if (!useNativeCamera && currentCameraId != PERSON_TRIGGER_CAMERA_ID) {
                currentCameraId = PERSON_TRIGGER_CAMERA_ID;
                personSeenFrames = 0;
                if (previewing) {
                    stopCameraOnly();
                    startRequested = true;
                    startCamera();
                }
            }
            mainHandler.postDelayed(this::doScanStep, 800); // small delay for cam 3 to open
        });
    }

    /**
     * Active person-scan loop. Camera stays fixed on PERSON_TRIGGER_CAMERA_ID (cam 3).
     * Only the ROBOT rotates to sweep the room. Each step:
     *   1. Rotate in the scan direction for SCAN_ROTATE_STEP_MS
     *   2. Stop — camera 3 now sees a new angle
     *   3. Wait SCAN_LOOK_STEP_MS for detection frames to confirm person
     *   4. If not found, repeat up to SCAN_MAX_STEPS (~360°), then show dialog.
     */
    private void doScanStep() {
        if (!pickHandling.get() || !scanningForPerson) return;

        if (scanStep == 0 && !isPeanutSdkReadyForMotors()) {
            Log.w(TAG, "Person scan: chassis SDK not ready — rotation steps are skipped until "
                    + "the main screen SDK status is GREEN (motors stay idle). Camera 3 person "
                    + "detection still runs.");
        }

        if (scanStep >= SCAN_MAX_STEPS
                || SystemClock.elapsedRealtime() - personSearchStartedAtMs > PERSON_SEARCH_TIMEOUT_MS) {
            Log.w(TAG, "Person scan done — no person after " + scanStep + " steps, showing dialog");
            scanningForPerson = false;
            stopMotorIfRunning();
            onPersonSearchComplete();
            return;
        }

        Log.d(TAG, "Scan step " + scanStep + "/" + SCAN_MAX_STEPS
                + " dir=" + (scanDirection == ApiConstants.MotorMove.RIGHT ? "RIGHT" : "LEFT"));

        if (isPeanutSdkReadyForMotors()) {
            try {
                RobotMovementManager mgr = RobotMovementManager.getInstance();
                // Re-assert motor lock every step — firmware can release it between steps
                mgr.ensureMotorEnabled();
                mgr.executeDirection(scanDirection);
                Runnable stopAndNext = () -> {
                    if (!scanningForPerson) return;
                    mgr.stop();
                    pendingScanStepRunnable = () -> { scanStep++; doScanStep(); };
                    mainHandler.postDelayed(pendingScanStepRunnable, SCAN_LOOK_STEP_MS);
                };
                pendingScanStepRunnable = stopAndNext;
                mainHandler.postDelayed(stopAndNext, SCAN_ROTATE_STEP_MS);
            } catch (Throwable t) {
                Log.w(TAG, "Motor unavailable during scan: " + t.getMessage());
                pendingScanStepRunnable = () -> { scanStep++; doScanStep(); };
                mainHandler.postDelayed(pendingScanStepRunnable, SCAN_LOOK_STEP_MS);
            }
        } else {
            // No motor — just wait and check camera
            pendingScanStepRunnable = () -> { scanStep++; doScanStep(); };
            mainHandler.postDelayed(pendingScanStepRunnable, SCAN_LOOK_STEP_MS);
        }
    }

    /** Direction the robot rotates during person scan — set from hand position hint. */
    private int scanDirection = ApiConstants.MotorMove.RIGHT;

    private void stopMotorIfRunning() {
        // Cancel any pending rotate/look callbacks before stopping motor
        if (pendingScanStepRunnable != null) {
            mainHandler.removeCallbacks(pendingScanStepRunnable);
            pendingScanStepRunnable = null;
        }
        if (!isPeanutSdkReadyForMotors()) return;
        try {
            RobotMovementManager mgr = RobotMovementManager.getInstance();
            mgr.stop();
        } catch (Throwable ignored) {}
    }

    /** Called by checkPersonOnCamera3 when a person is confirmed during active scan. */
    private void onPersonDetectedFromScan() {
        if (!pickHandling.get() || !scanningForPerson) return;
        scanningForPerson = false;
        stopMotorIfRunning();
        Log.i(TAG, "Person found on camera " + currentCameraId + " at scan step " + scanStep);
        lastPersonTriggerAtMs = SystemClock.elapsedRealtime();
        onPersonSearchComplete();
    }

    /** Called when person is found (or scan timed out). Faces person then shows dialog. */
    private void onPersonSearchComplete() {
        int prev = pendingPickPrevCount;
        int next = pendingPickNewCount;
        pendingPickPrevCount = -1;
        pendingPickNewCount  = -1;
        triggerFeedbackFlow(prev, next);
    }

    private void triggerFeedbackFlow(int previousCount, int newCount) {
        // Robot already faces the person from the scan — just speak and show dialog.
        mainHandler.post(() -> {
            try {
                // Release motor lock so robot stays still during dialog
                if (isPeanutSdkReadyForMotors()) {
                    RobotMovementManager.getInstance().release();
                }
            } catch (Throwable ignored) {}
            String prompt = isAdded()
                    ? getString(R.string.yolo_tts_feedback_prompt)
                    : "Please give us your feedback.";
            ttsHelper.speak(prompt, () ->
                    mainHandler.post(() -> showFeedbackDialog(previousCount, newCount)));
        });
    }

    /** Switch back to the camera we were on before the person-search switch. */
    private void restoreOriginalCamera() {
        if (preSwitchCameraId < 0 || preSwitchCameraId == currentCameraId) {
            preSwitchCameraId = -1;
            return;
        }
        int restore = preSwitchCameraId;
        preSwitchCameraId = -1;
        mainHandler.post(() -> {
            if (!isAdded() || getActivity() == null) return;
            currentCameraId = restore;
            trayTracker.reset();
            personSeenFrames = 0;
            cocoConfirmedCount = -1;
            cocoPendingCount   = -1;
            cocoPendingFrames  = 0;
            lastItemDisappearX = -1f;
            if (previewing) {
                stopCameraOnly();
                startRequested = true;
                startCamera();
            }
        });
    }

    /**
     * Called after feedback dialog closes (any path: submit, skip, close).
     * Switches back to the tray camera and restarts detection from a clean state.
     */
    private void restartTrayCamera() {
        int restore = (preSwitchCameraId >= 0) ? preSwitchCameraId : 0;
        preSwitchCameraId = -1;
        scanningForPerson = false;
        personSeenFrames  = 0;
        cocoConfirmedCount = -1;
        cocoPendingCount   = -1;
        cocoPendingFrames  = 0;
        lastItemDisappearX = -1f;
        pendingPickPrevCount = -1;
        pendingPickNewCount  = -1;
        pickHandling.set(false);

        mainHandler.post(() -> {
            if (!isAdded() || getActivity() == null) return;
            currentCameraId = restore;
            trayTracker.reset();
            // Reset smoothing so camera 1 re-learns item count from scratch
            if (cocoDetector != null)       cocoDetector.resetSmoothing();
            if (personScanDetector != null) personScanDetector.resetSmoothing();
            // Full hardware stop — sets previewing=false cleanly before restart
            androidCameraSession.incrementAndGet();
            releaseAndroidCamera();
            releaseNativeCamera();
            stopDetectThread();
            previewing = false;
            mainHandler.removeCallbacks(previewFlushRunnable);
            synchronized (previewLock) { previewPendingJpeg = null; previewFlushPosted = false; }
            // Clear stale detection overlay from camera 3
            showDetections("");
            highlightActiveIndex();
            // Small pause so hardware fully releases before reopening
            mainHandler.postDelayed(() -> {
                if (!isAdded() || getActivity() == null) return;
                startRequested = true;
                startCamera();
            }, 300);
        });
    }

    /**
     * Stops the camera hardware only — does NOT reset pickHandling or trayTracker state.
     * Used when switching cameras mid-flow (item pick → camera 3 → original camera).
     */
    private void stopCameraOnly() {
        androidCameraSession.incrementAndGet();
        releaseAndroidCamera();
        releaseNativeCamera();
        stopDetectThread();
        previewing = false;
        mainHandler.removeCallbacks(previewFlushRunnable);
        synchronized (previewLock) {
            previewPendingJpeg = null;
            previewFlushPosted = false;
        }
    }

    /** Motor/TTS use PeanutSDK; init completes asynchronously — never drive motors before ready. */
    private boolean isPeanutSdkReadyForMotors() {
        android.app.Activity a = getActivity();
        return a instanceof SupermarketActivity && ((SupermarketActivity) a).isSdkReady();
    }

    private void showFeedbackDialog(int previousCount, int newCount) {
        if (getActivity() == null || !isAdded()) {
            pickHandling.set(false);
            return;
        }
        if (isStateSaved()) {
            Log.w(TAG, "showFeedbackDialog skipped — fragment state already saved");
            pickHandling.set(false);
            return;
        }
        // Dismiss any already-open instance
        androidx.fragment.app.FragmentManager fm = getParentFragmentManager();
        androidx.fragment.app.Fragment existing = fm.findFragmentByTag(TrayFeedbackDialogFragment.TAG);
        if (existing != null) {
            ((TrayFeedbackDialogFragment) existing).dismissAllowingStateLoss();
        }

        TrayFeedbackDialogFragment dialog =
                TrayFeedbackDialogFragment.newInstance(previousCount, newCount);
        dialog.setCallback(new TrayFeedbackDialogFragment.Callback() {
            @Override
            public void onFeedbackSubmitted(int stars) {
                Log.i(TAG, "Feedback submitted: " + stars + " stars");
                ttsHelper.speak("Thank you for your feedback!");
                restartTrayCamera();
            }
            @Override
            public void onFeedbackDismissed() {
                restartTrayCamera();
            }
        });
        dialog.show(fm, TrayFeedbackDialogFragment.TAG);
    }

    // ---- Camera index buttons ----

    private void rebuildIndexButtons() {
        indexRow.removeAllViews();
        indexButtons.clear();
        int dpMargin = (int) (6 * getResources().getDisplayMetrics().density);

        if (useNativeCamera) {
            if (nativeVideoNodes.isEmpty()) {
                statusText.setText("No /dev/videoN nodes found on this device");
                return;
            }
            for (int n : nativeVideoNodes) {
                Button b = new Button(getContext());
                b.setText("video" + n);
                b.setTypeface(b.getTypeface(), Typeface.BOLD);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
                lp.setMargins(indexButtons.isEmpty() ? 0 : dpMargin, 0, 0, 0);
                b.setLayoutParams(lp);
                final int node = n;
                b.setOnClickListener(view -> onSelectVideoNode(node));
                indexRow.addView(b);
                indexButtons.add(b);
            }
        } else {
            for (int i = 0; i < cameraCount; i++) {
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
        }
        highlightActiveIndex();
    }

    private void highlightActiveIndex() {
        if (useNativeCamera) {
            for (int i = 0; i < indexButtons.size(); i++) {
                Button b = indexButtons.get(i);
                boolean active = (nativeVideoNodes.get(i) == currentVideoNode && previewing);
                b.setBackgroundColor(active ? Color.parseColor("#F59E0B") : Color.TRANSPARENT);
                b.setTextColor(active ? Color.BLACK : Color.WHITE);
            }
        } else {
            for (int i = 0; i < indexButtons.size(); i++) {
                Button b = indexButtons.get(i);
                boolean active = (i == currentCameraId && previewing);
                b.setBackgroundColor(active ? Color.parseColor("#F59E0B") : Color.TRANSPARENT);
                b.setTextColor(active ? Color.BLACK : Color.WHITE);
            }
        }
    }

    private void onSelectVideoNode(int videoNode) {
        currentVideoNode = videoNode;
        trayTracker.reset();
        personSeenFrames = 0;
        if (previewing) {
            stopAll();
            startRequested = true;
            mainHandler.post(this::startCamera);
        } else {
            highlightActiveIndex();
        }
    }

    private void onSelectCamera(int index) {
        currentCameraId = index;
        trayTracker.reset();
        personSeenFrames = 0;
        // Load detection zone saved for this camera layer
        loadZoneForCurrentCamera();
        if (previewing) {
            stopAll();
            startRequested = true;
            mainHandler.post(this::startCamera);
        } else {
            highlightActiveIndex();
        }
    }

    // ---- Start / stop ----

    private void requestStart() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, REQ_CAMERA);
            return;
        }
        startRequested = true;
        startCamera();
    }

    private void startCamera() {
        startDetectThread();
        statusText.setText("Loading model…");
        final boolean isCoco = useCoco;
        detectHandler.post(() -> {
            boolean modelOk;
            if (isCoco) {
                modelOk = cocoDetector != null && cocoDetector.ensureLoaded();
                // Push current zone crop values after model is loaded
                if (modelOk && cocoDetector != null) {
                    cocoDetector.setRoi(zoneCropLeft, zoneCropTop, zoneCropRight, zoneCropBottom);
                }
            } else {
                modelOk = detector != null && detector.ensureLoaded();
            }
            // Load body, hand, and person-scan models in background — failures are non-fatal.
            if (bodyDetector != null && !bodyDetector.isReady()) {
                boolean bodyOk = bodyDetector.ensureLoaded();
                Log.i(TAG, "body_detection load: " + (bodyOk ? "OK" : "FAILED — will use fallback"));
            }
            if (handDetector != null && !handDetector.isReady()) {
                boolean handOk = handDetector.ensureLoaded();
                Log.i(TAG, "hand_detect load: " + (handOk ? "OK" : "FAILED — will use centroid fallback"));
            }
            if (personScanDetector != null) {
                boolean scanOk = personScanDetector.ensureLoaded();
                Log.i(TAG, "person scan detector load: " + (scanOk ? "OK" : "FAILED"));
            }
            mainHandler.post(() -> {
                if (!startRequested) return;
                if (!modelOk) {
                    statusText.setText(R.string.yolo_status_model_failed);
                    Toast.makeText(requireContext(),
                            R.string.yolo_toast_model_failed, Toast.LENGTH_LONG).show();
                    applyStreamButtonIdleState();
                    return;
                }
                if (!previewing && startRequested) {
                    if (useNativeCamera) {
                        startNativeCamera();
                    } else {
                        if (surfaceReady) startAndroidCamera();
                        else statusText.setText("Waiting for surface…");
                    }
                }
            });
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == REQ_CAMERA && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            requestStart();
        } else {
            Toast.makeText(requireContext(), R.string.cam_no_permission, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onPause() {
        stopAll();
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        mainHandler.removeCallbacks(previewFlushRunnable);
        synchronized (previewLock) {
            previewPendingJpeg = null;
            previewFlushPosted = false;
        }
        if (detector != null)           detector.close();
        if (cocoDetector != null)       cocoDetector.close();
        if (personScanDetector != null) personScanDetector.close();
        if (bodyDetector != null)       bodyDetector.close();
        if (handDetector != null)       handDetector.close();
        ttsHelper.shutdown();
        if (streamServer != null) {
            try { streamServer.stop(500); } catch (Throwable ignored) {}
            streamServer = null;
        }
        super.onDestroyView();
    }

    // ---- Android Camera (Camera1 API) ----

    private void startAndroidCamera() {
        if (previewing) return;
        if (androidCamera != null) return;
        try {
            final int session = androidCameraSession.incrementAndGet();
            androidCamera = Camera.open(currentCameraId);
            androidCamera.setErrorCallback((error, cam) -> mainHandler.post(() -> {
                Toast.makeText(requireContext(),
                        "Camera " + currentCameraId + " error " + error, Toast.LENGTH_LONG).show();
                stopAll();
            }));

            Camera.Parameters params = androidCamera.getParameters();
            if (params != null) {
                Camera.Size sz = chooseBestSize(params.getSupportedPreviewSizes());
                if (sz != null) {
                    params.setPreviewSize(sz.width, sz.height);
                    previewWidth  = sz.width;
                    previewHeight = sz.height;
                }
                List<String> modes = params.getSupportedFocusModes();
                if (modes != null && modes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
                    params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
                }
                params.setPreviewFormat(ImageFormat.NV21);
                try { androidCamera.setParameters(params); } catch (RuntimeException ignored) {}
            }

            if (previewWidth  <= 0) previewWidth  = 640;
            if (previewHeight <= 0) previewHeight = 480;

            Camera.CameraInfo info = new Camera.CameraInfo();
            Camera.getCameraInfo(currentCameraId, info);
            androidCamera.setDisplayOrientation(info.orientation);
            androidCamera.setPreviewDisplay(surfaceView.getHolder());

            startDetectThread();
            attachAndroidDetectionCallback(session);

            androidCamera.startPreview();
            previewing = true;
            applyRunningUiState();
            resizeSurface();
        } catch (Exception e) {
            Log.e(TAG, "Android camera open failed", e);
            Toast.makeText(requireContext(), "Camera open failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
            releaseAndroidCamera();
        }
    }

    private void attachAndroidDetectionCallback(final int session) {
        if (androidCamera == null) return;
        final Camera activeCamera = androidCamera;
        final int bufSize = previewWidth * previewHeight * 3 / 2;
        activeCamera.addCallbackBuffer(new byte[bufSize]);
        activeCamera.addCallbackBuffer(new byte[bufSize]);
        activeCamera.setPreviewCallbackWithBuffer((data, cam) -> {
            if (data == null) return;
            if (session != androidCameraSession.get() || cam != androidCamera || useNativeCamera || !previewing) {
                try { cam.addCallbackBuffer(data); } catch (Exception ignored) {}
                return;
            }
            long now = SystemClock.uptimeMillis();
            if (detectBusy.get() || now - lastDetectAt < DETECT_INTERVAL_MS) {
                try { cam.addCallbackBuffer(data); } catch (Exception ignored) {}
                return;
            }
            detectBusy.set(true);
            lastDetectAt = now;
            Handler h = detectHandler;
            if (h == null) {
                detectBusy.set(false);
                try { cam.addCallbackBuffer(data); } catch (Exception ignored) {}
                return;
            }
            final int w = previewWidth, ht = previewHeight;
            final boolean isCoco = useCoco;
            h.post(() -> {
                if (session != androidCameraSession.get() || cam != androidCamera || useNativeCamera || !previewing) {
                    try { cam.addCallbackBuffer(data); } catch (Exception ignored) {}
                    detectBusy.set(false);
                    return;
                }
                if (isCoco) {
                    // COCO mode: TFLite takes NV21 directly — no JPEG conversion needed
                    runCocoOnFrame(data, w, ht);
                } else {
                    byte[] jpeg = nv21ToJpeg(data, w, ht);
                    if (jpeg != null) runYoloOnFrame(jpeg, w, ht);
                }
                if (session == androidCameraSession.get() && cam == androidCamera) {
                    try { androidCamera.addCallbackBuffer(data); } catch (Exception ignored) {}
                } else {
                    try { cam.addCallbackBuffer(data); } catch (Exception ignored) {}
                }
                detectBusy.set(false);
            });
        });
    }

    private void releaseAndroidCamera() {
        if (androidCamera != null) {
            try { androidCamera.setPreviewCallbackWithBuffer(null); } catch (Exception ignored) {}
            try { androidCamera.stopPreview(); } catch (Exception ignored) {}
            try { androidCamera.release(); } catch (Exception ignored) {}
            androidCamera = null;
        }
    }

    // ---- Keenon native camera ----

    private void startNativeCamera() {
        if (previewing) return;
        if (!KNCameraHelper.canOpenCamera(currentVideoNode)) {
            Toast.makeText(requireContext(),
                    "/dev/video" + currentVideoNode + " not writable — try another node or Android mode",
                    Toast.LENGTH_LONG).show();
            return;
        }
        startDetectThread();
        KNCameraHelper.openCamera(
                currentVideoNode, 640, 480, KNCameraHelper.FORMAT_JPEG,
                new KNCameraHelper.KNCameraCallback() {
                    @Override
                    public void onSuccess(int idx, long fd, int bufferLength) {
                        nativeFd = fd;
                        nativeCaptureBuffer = new byte[bufferLength];
                        KNCameraHelper.initCaptureData(fd, nativeCaptureBuffer);
                        lastNativeDetectAtMs = 0L;
                        nativeInferenceWarned = false;
                        previewing = true;
                        mainHandler.post(() -> applyRunningUiState());
                        if (detectHandler != null) detectHandler.post(nativeCaptureLoop);
                    }
                    @Override
                    public void onFailure(int errorCode, String message) {
                        Log.e(TAG, "Native openCamera failed: " + message);
                        mainHandler.post(() -> Toast.makeText(requireContext(),
                                "Native camera failed: " + message, Toast.LENGTH_LONG).show());
                    }
                });
    }

    private void releaseNativeCamera() {
        if (nativeFd >= 0) {
            try {
                if (nativeCaptureBuffer != null) {
                    KNCameraHelper.releaseCamera(nativeFd, nativeCaptureBuffer);
                } else {
                    KNCameraHelper.closeCamera(nativeFd);
                }
            } catch (Exception ignored) {}
            nativeFd = -1;
            nativeCaptureBuffer = null;
        }
    }

    // ---- Stop everything ----

    private void stopAll() {
        startRequested = false;
        androidCameraSession.incrementAndGet();
        releaseAndroidCamera();
        releaseNativeCamera();
        stopDetectThread();
        previewing = false;
        lastNativeDetectAtMs = 0L;
        nativeInferenceWarned = false;
        mainHandler.removeCallbacks(previewFlushRunnable);
        synchronized (previewLock) {
            previewPendingJpeg = null;
            previewFlushPosted = false;
        }
        trayTracker.reset();
        personSeenFrames = 0;
        lastPersonTriggerAtMs = 0L;
        preSwitchCameraId = -1;
        pendingPickPrevCount = -1;
        pendingPickNewCount  = -1;
        personSearchStartedAtMs = 0L;
        scanStep = 0;
        scanningForPerson = false;
        cocoConfirmedCount    = -1;
        cocoPendingCount      = -1;
        cocoPendingFrames     = 0;
        lastJpegForBodyDetect = null;
        mainHandler.post(() -> {
            placeholder.setVisibility(View.VISIBLE);
            if (detPanel != null) detPanel.setVisibility(View.GONE);
            detectionsOverlay.setVisibility(View.GONE);
            if (roiBorder != null) roiBorder.setVisibility(View.GONE);
            fpsOverlay.setVisibility(View.GONE);
            applyStreamButtonIdleState();
            statusText.setText(R.string.yolo_status_idle);
            highlightActiveIndex();
        });
    }

    // ---- YOLO inference (common path) ----

    private void runYoloOnFrame(byte[] frame, int srcWidth, int srcHeight) {
        if (frame == null || frame.length == 0 || detector == null) return;
        if (!detectInFlight.compareAndSet(false, true)) {
            droppedDetectFrames++;
            if (droppedDetectFrames % 20 == 0) {
                Log.w(TAG, "Dropping frame while detect in-flight. dropped=" + droppedDetectFrames);
            }
            // Still ship video to the browser; only inference was skipped for this tick.
            pushStreamJpegIfWatching(currentCameraId, frame, "[]", 0f, 0L);
            return;
        }
        // PC dashboard: show the latest frame immediately; do not wait on NCNN.
        pushStreamJpegIfWatching(currentCameraId, frame, "[]", 0f, 0L);
        final int seq = frameSeq.incrementAndGet();
        try {
            Log.d(TAG, "Detect frame#" + seq + " src=" + srcWidth + "x" + srcHeight
                    + " bytes=" + frame.length + " mode=" + (useNativeCamera ? "native" : "android"));
            long t0 = SystemClock.elapsedRealtime();
            // Save frame for body_detection use in maybeTriggerPersonInteraction.
            lastJpegForBodyDetect = frame;
            lastJpegWidth  = srcWidth;
            lastJpegHeight = srcHeight;

            List<YoloDetectorHelper.Result> results = detector.detect(frame, srcWidth, srcHeight);
            long ms = SystemClock.elapsedRealtime() - t0;

            // Update hand direction from the real hand model on every tray frame.
            // This gives accurate pick-direction before the feedback scan starts.
            if (handDetector != null && handDetector.isReady()) {
                float hx = handDetector.detectCenterX(frame, srcWidth, srcHeight);
                if (hx >= 0) lastItemDisappearX = hx;
            }

            trayTracker.onFrame(results);
            maybeTriggerPersonInteraction(results);

            frameCount++;
            long now = SystemClock.elapsedRealtime();
            float fps = frameCount / Math.max(1f, (now - lastFrameTime) / 1000f);
            if (now - lastFrameTime > 2000) { frameCount = 0; lastFrameTime = now; }

            int plateCount = trayTracker.getConfirmedCount();
            String detText = formatDetections(results, plateCount);
            String fpsText = String.format(Locale.US, "%.1f fps  %d ms", fps, ms);
            mainHandler.post(() -> {
                showDetections(detText);
                showFps(fpsText);
            });

            // Push detections overlay (video was already sent before detect to avoid dead air).
            pushStreamJpegIfWatching(currentCameraId, frame,
                    yoloDetectionsToJson(results), fps, ms);
            Log.d(TAG, "Detect frame#" + seq + " done in " + ms + " ms results="
                    + (results != null ? results.size() : 0));
        } catch (Throwable t) {
            Log.w(TAG, "runYoloOnFrame failed for " + srcWidth + "x" + srcHeight
                    + " frameLen=" + frame.length, t);
        } finally {
            detectInFlight.set(false);
        }
    }

    // ---- COCO inference (TFLite path) ----

    private void runCocoOnFrame(byte[] nv21, int srcWidth, int srcHeight) {
        if (nv21 == null || nv21.length == 0 || cocoDetector == null) return;
        byte[] streamJpeg = null;
        if (streamServer != null && streamServer.hasClients()) {
            streamJpeg = nv21ToJpeg(nv21, srcWidth, srcHeight);
            pushStreamJpegIfWatching(currentCameraId, streamJpeg, "[]", 0f, 0L);
        }
        if (!detectInFlight.compareAndSet(false, true)) {
            droppedDetectFrames++;
            return;
        }
        try {
            long t0 = SystemClock.elapsedRealtime();
            final boolean onPersonCam = scanningForPerson
                    && currentCameraId == PERSON_TRIGGER_CAMERA_ID;
            // On camera 3 use the dedicated fast person-scan detector (SSD MobileNetV3).
            // On tray camera use the user-selected cocoDetector with ROI crop.
            List<ObjectDetectorHelper.Result> results = onPersonCam
                    ? (personScanDetector != null ? personScanDetector.detect(nv21, srcWidth, srcHeight, 0, false)
                                                  : cocoDetector.detect(nv21, srcWidth, srcHeight, 0, false))
                    : cocoDetector.detect(nv21, srcWidth, srcHeight, 0, true);
            long ms = SystemClock.elapsedRealtime() - t0;

            frameCount++;
            long now = SystemClock.elapsedRealtime();
            float fps = frameCount / Math.max(1f, (now - lastFrameTime) / 1000f);
            if (now - lastFrameTime > 2000) { frameCount = 0; lastFrameTime = now; }

            int cocoItemCount = 0;
            boolean personSeen = false;
            float sumCx = 0;
            int cxCount = 0;
            float sumPromoCx = 0;
            int promoCxCount = 0;
            if (results != null) {
                for (ObjectDetectorHelper.Result r : results) {
                    if (r == null) continue;
                    if ("person".equalsIgnoreCase(r.label)) { personSeen = true; continue; }
                    cocoItemCount++;
                    if (r.centerX >= 0) {
                        sumCx += r.centerX;
                        cxCount++;
                        if (isTrayPromoCocoLabel(r.label)) {
                            sumPromoCx += r.centerX;
                            promoCxCount++;
                        }
                    }
                }
            }
            // Prefer real hand position from Keenon hand_detect model.
            // Fall back to COCO item centroid if hand model not ready.
            if (handDetector != null && handDetector.isReady()) {
                byte[] frameJpeg = nv21ToJpeg(nv21, srcWidth, srcHeight);
                if (frameJpeg != null) {
                    float hx = handDetector.detectCenterX(frameJpeg, srcWidth, srcHeight);
                    if (hx >= 0) lastItemDisappearX = hx;
                }
            } else if (promoCxCount > 0) {
                lastItemDisappearX = sumPromoCx / promoCxCount;
            } else if (cxCount > 0) {
                lastItemDisappearX = sumCx / cxCount;
            }

            // Person confirmation: prefer Keenon body_detection model over COCO "person" class.
            boolean bodyPersonSeen = false;
            if (bodyDetector != null && bodyDetector.isReady()
                    && scanningForPerson && currentCameraId == PERSON_TRIGGER_CAMERA_ID) {
                byte[] frameJpeg = nv21ToJpeg(nv21, srcWidth, srcHeight);
                if (frameJpeg != null) {
                    bodyPersonSeen = bodyDetector.isPersonVisible(frameJpeg, srcWidth, srcHeight);
                }
            }
            checkPersonOnCamera3(bodyPersonSeen || personSeen);
            trackCocoItemCount(cocoItemCount);

            String detText = formatCocoDetections(results);
            String fpsText = String.format(Locale.US, "%.1f fps  %d ms", fps, ms);
            mainHandler.post(() -> {
                showDetections(detText);
                showFps(fpsText);
            });

            if (streamJpeg == null && streamServer != null && streamServer.hasClients()) {
                streamJpeg = nv21ToJpeg(nv21, srcWidth, srcHeight);
            }
            pushStreamJpegIfWatching(currentCameraId, streamJpeg,
                    cocoDetectionsToJson(results), fps, ms);
        } catch (Throwable t) {
            Log.w(TAG, "runCocoOnFrame failed", t);
        } finally {
            detectInFlight.set(false);
        }
    }

    /** COCO has no "hand" — use food/drink centroids as a proxy for which side the shopper used. */
    private static boolean isTrayPromoCocoLabel(String label) {
        if (label == null) return false;
        switch (label.toLowerCase(Locale.US)) {
            case "bottle":
            case "wine glass":
            case "cup":
            case "bowl":
            case "fork":
            case "knife":
            case "spoon":
            case "banana":
            case "apple":
            case "orange":
            case "sandwich":
            case "broccoli":
            case "carrot":
            case "hot dog":
            case "pizza":
            case "donut":
            case "cake":
            case "cell phone":
                return true;
            default:
                return false;
        }
    }

    private static String formatCocoDetections(List<ObjectDetectorHelper.Result> results) {
        if (results == null || results.isEmpty()) {
            // Don't say "No items detected" — the smoothing means we're still waiting for
            // confirmation. Show nothing so the overlay stays blank until confirmed.
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Items on tray: ").append(results.size()).append("\n");
        for (ObjectDetectorHelper.Result r : results) {
            sb.append(String.format(Locale.US, "  %-18s %.0f%%\n", r.label, r.score * 100f));
        }
        if (sb.charAt(sb.length() - 1) == '\n') sb.deleteCharAt(sb.length() - 1);
        return sb.toString();
    }

    /**
     * Called during TRAY (NCNN) mode to check if a person is visible on camera 3.
     * Uses body_detection model if available; falls back to COCO "person" class from YOLO results.
     * The lastJpegForBodyDetect field holds the most recent JPEG from the tray camera.
     */
    private void maybeTriggerPersonInteraction(List<YoloDetectorHelper.Result> results) {
        // body_detection model is more accurate than COCO "person" at close range.
        if (bodyDetector != null && bodyDetector.isReady()
                && scanningForPerson && currentCameraId == PERSON_TRIGGER_CAMERA_ID
                && lastJpegForBodyDetect != null) {
            boolean bodyFound = bodyDetector.isPersonVisible(
                    lastJpegForBodyDetect, lastJpegWidth, lastJpegHeight);
            if (bodyFound) { checkPersonOnCamera3(true); return; }
        }
        // Fallback: check COCO YOLO results for "person" label.
        checkPersonOnCamera3(results != null
                && results.stream()
                    .anyMatch(r -> r != null && "person".equalsIgnoreCase(r.label) && r.score >= 0.45f));
    }

    /**
     * Tracks item count per frame in COCO mode and fires onItemPickedDetected when a drop
     * is confirmed over COCO_CONFIRM_FRAMES consecutive frames. Mirrors TrayItemTracker logic.
     * Must be called from the detect thread.
     */
    private void trackCocoItemCount(int count) {
        // Don't track while we're already handling a pick or searching for a person
        if (pickHandling.get()) return;
        // Don't count while we're on camera 3 (person-search view, not tray view)
        if (currentCameraId == PERSON_TRIGGER_CAMERA_ID) return;

        if (cocoConfirmedCount < 0) {
            cocoConfirmedCount = count;
            cocoPendingCount   = -1;
            cocoPendingFrames  = 0;
            return;
        }
        if (count >= cocoConfirmedCount) {
            cocoConfirmedCount = count;
            cocoPendingCount   = -1;
            cocoPendingFrames  = 0;
            return;
        }
        // Count dropped — debounce
        if (count == cocoPendingCount) {
            cocoPendingFrames++;
        } else {
            cocoPendingCount  = count;
            cocoPendingFrames = 1;
        }
        if (cocoPendingFrames >= COCO_CONFIRM_FRAMES) {
            long now = System.currentTimeMillis();
            if (now - cocoLastFiredAt >= COCO_COOLDOWN_MS) {
                int previous = cocoConfirmedCount;
                cocoConfirmedCount = cocoPendingCount;
                cocoPendingCount   = -1;
                cocoPendingFrames  = 0;
                cocoLastFiredAt    = now;
                Log.i(TAG, "COCO item count dropped: " + previous + " → " + cocoConfirmedCount);
                onItemPickedDetected(previous, cocoConfirmedCount);
            } else {
                cocoConfirmedCount = cocoPendingCount;
                cocoPendingCount   = -1;
                cocoPendingFrames  = 0;
            }
        }
    }

    /**
     * Called from both TRAY (NCNN) and COCO (TFLite) detect paths when on camera 3.
     * Counts consecutive person-seen frames and fires onPersonDetectedFromCamera3 when confirmed.
     */
    private void checkPersonOnCamera3(boolean personSeen) {
        // Only fire if we are actively scanning for a person (triggered by item drop)
        if (!scanningForPerson) return;
        if (useNativeCamera) return;
        personSeenFrames = personSeen ? (personSeenFrames + 1) : 0;
        if (personSeenFrames >= PERSON_CONFIRM_FRAMES) {
            personSeenFrames = 0;
            onPersonDetectedFromScan();
        }
    }

    private static String formatDetections(List<YoloDetectorHelper.Result> results, int confirmedCount) {
        StringBuilder sb = new StringBuilder();

        // Count raw detections per class (what the model actually sees this frame)
        int rawPlate = 0, rawEmpty = 0;
        if (results != null) {
            for (YoloDetectorHelper.Result r : results) {
                if ("plate".equals(r.label)) rawPlate++;
                else if ("ros_empty".equals(r.label)) rawEmpty++;
            }
        }
        // Show both the raw per-frame count and the debounced stable count
        sb.append("Detected this frame: ").append(rawPlate).append(" plate");
        if (rawEmpty > 0) sb.append(" / ").append(rawEmpty).append(" empty");
        sb.append("\n");
        if (confirmedCount >= 0) {
            sb.append("Stable count (debounced): ").append(confirmedCount).append("\n");
        }

        // Show best confidence per class (not every box, to avoid noise)
        if (results != null && !results.isEmpty()) {
            java.util.Map<String, Float> bestScore = new java.util.LinkedHashMap<>();
            for (YoloDetectorHelper.Result r : results) {
                Float prev = bestScore.get(r.label);
                if (prev == null || r.score > prev) bestScore.put(r.label, r.score);
            }
            for (java.util.Map.Entry<String, Float> e : bestScore.entrySet()) {
                sb.append(String.format(Locale.US, "%-12s best=%.0f%%\n", e.getKey(), e.getValue() * 100f));
            }
        }

        if (sb.length() > 0 && sb.charAt(sb.length() - 1) == '\n') {
            sb.deleteCharAt(sb.length() - 1);
        }
        return sb.toString();
    }

    private void showDetections(String text) {
        if (detectionsOverlay == null) return;
        if (text == null || text.isEmpty()) {
            if (detPanel != null) detPanel.setVisibility(View.GONE);
            detectionsOverlay.setVisibility(View.GONE);
            return;
        }
        // Split "Items on tray: N\n  item  XX%\n  item  XX%" into header + body
        if (detPanel != null) {
            String[] lines = text.split("\n", 2);
            if (detCountText != null) detCountText.setText(lines[0]);
            if (lines.length > 1 && !lines[1].trim().isEmpty()) {
                detectionsOverlay.setText(lines[1].trim());
                detectionsOverlay.setVisibility(View.VISIBLE);
            } else {
                detectionsOverlay.setVisibility(View.GONE);
            }
            detPanel.setVisibility(View.VISIBLE);
        } else {
            detectionsOverlay.setText(text);
            detectionsOverlay.setVisibility(View.VISIBLE);
        }
    }

    private void showFps(String text) {
        if (fpsOverlay == null) return;
        fpsOverlay.setText(text);
        fpsOverlay.setVisibility(previewing ? View.VISIBLE : View.GONE);
    }

    // ---- Detect thread lifecycle ----

    private void startDetectThread() {
        if (detectThread != null) return;
        detectThread = new HandlerThread("YoloDetect");
        detectThread.start();
        detectHandler = new Handler(detectThread.getLooper());
        lastDetectAt = 0;
        lastFrameTime = SystemClock.elapsedRealtime();
        frameCount = 0;
        frameSeq.set(0);
        droppedDetectFrames = 0;
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
        detectInFlight.set(false);
        nativeCapturePending.set(false);
    }

    // ---- SurfaceHolder.Callback ----

    @Override
    public void surfaceCreated(@NonNull SurfaceHolder holder) {
        surfaceReady = true;
        if (startRequested && !previewing && !useNativeCamera) startAndroidCamera();
    }

    @Override
    public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int w, int h) {
        if (androidCamera != null) {
            try {
                androidCamera.stopPreview();
                androidCamera.setPreviewDisplay(holder);
                androidCamera.startPreview();
            } catch (Exception e) { Log.e(TAG, "surfaceChanged restart failed", e); }
        }
    }

    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
        surfaceReady = false;
        releaseAndroidCamera();
    }

    // ---- UI helpers ----

    private void applyRunningUiState() {
        placeholder.setVisibility(View.GONE);
        applyStreamButtonRunningState();
        String camLabel = useNativeCamera
                ? "Native /dev/video" + currentVideoNode
                : "Android cam " + currentCameraId;
        String modelLabel = useCoco ? "COCO·TFLite" : "NCNN·YOLOv5";
        statusText.setText(camLabel + " · " + modelLabel);
        // Show ROI border only in COCO mode (it reflects the 70% crop zone)
        if (roiBorder != null) {
            roiBorder.setVisibility(useCoco ? View.VISIBLE : View.GONE);
            positionRoiBorder();
        }
        highlightActiveIndex();
    }

    /** Refresh the zone overlay border (called on start/stop/mode change). */
    private void positionRoiBorder() {
        updateZoneOverlay();
    }

    private void resizeSurface() {
        if (previewWidth <= 0 || previewHeight <= 0 || previewContainer == null) return;
        ViewTreeObserver vto = previewContainer.getViewTreeObserver();
        vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override public void onGlobalLayout() {
                int cw = previewContainer.getWidth();
                int ch = previewContainer.getHeight();
                if (cw <= 0 || ch <= 0) return;
                previewContainer.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                float camAspect = (float) previewWidth / previewHeight;
                int tw, th;
                if (cw / (float) ch > camAspect) { th = ch; tw = Math.round(ch * camAspect); }
                else { tw = cw; th = Math.round(cw / camAspect); }
                ViewGroup.LayoutParams lp = surfaceView.getLayoutParams();
                lp.width = tw; lp.height = th;
                surfaceView.setLayoutParams(lp);
            }
        });
        previewContainer.requestLayout();
    }

    // ---- Streaming helpers ----

    /**
     * Send a JPEG to the PC dashboard without waiting for inference.
     * Inference can block or be skipped ({@code detectInFlight}); preview must still stream.
     */
    private void pushStreamJpegIfWatching(int camId, byte[] jpeg, String detectionsJson, float fps, long inferenceMs) {
        if (streamServer == null || !streamServer.hasClients() || jpeg == null || jpeg.length == 0) return;
        try {
            streamServer.pushFrame(camId, jpeg, detectionsJson, fps, inferenceMs);
        } catch (Throwable ignored) {}
    }

    /** Build a JSON array of COCO detections for the PC dashboard. */
    private static String cocoDetectionsToJson(List<ObjectDetectorHelper.Result> results) {
        if (results == null || results.isEmpty()) return "[]";
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < results.size(); i++) {
            ObjectDetectorHelper.Result r = results.get(i);
            if (i > 0) sb.append(",");
            sb.append("{\"label\":\"").append(r.label)
              .append("\",\"score\":").append(String.format(Locale.US, "%.2f", r.score))
              .append(",\"cx\":").append(String.format(Locale.US, "%.3f", r.centerX))
              .append("}");
        }
        sb.append("]");
        return sb.toString();
    }

    /** Build a JSON array of YOLO (tray) detections for the PC dashboard. */
    private static String yoloDetectionsToJson(List<YoloDetectorHelper.Result> results) {
        if (results == null || results.isEmpty()) return "[]";
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < results.size(); i++) {
            YoloDetectorHelper.Result r = results.get(i);
            if (i > 0) sb.append(",");
            sb.append("{\"label\":\"").append(r.label)
              .append("\",\"score\":").append(String.format(Locale.US, "%.2f", r.score))
              .append(",\"x1\":").append(String.format(Locale.US, "%.3f", r.x1))
              .append(",\"y1\":").append(String.format(Locale.US, "%.3f", r.y1))
              .append(",\"x2\":").append(String.format(Locale.US, "%.3f", r.x2))
              .append(",\"y2\":").append(String.format(Locale.US, "%.3f", r.y2))
              .append("}");
        }
        sb.append("]");
        return sb.toString();
    }

    // ---- Utilities ----

    private static Camera.Size chooseBestSize(List<Camera.Size> sizes) {
        if (sizes == null || sizes.isEmpty()) return null;
        Camera.Size best = null;
        for (Camera.Size s : sizes) {
            if (s.width <= 1280 && s.height <= 960) {
                if (best == null || s.width * s.height > best.width * best.height) best = s;
            }
        }
        return best != null ? best : sizes.get(0);
    }

    private static byte[] nv21ToJpeg(byte[] nv21, int width, int height) {
        try {
            YuvImage yuv = new YuvImage(nv21, ImageFormat.NV21, width, height, null);
            ByteArrayOutputStream out = new ByteArrayOutputStream(nv21.length / 4);
            yuv.compressToJpeg(new Rect(0, 0, width, height), 85, out);
            return out.toByteArray();
        } catch (Exception e) {
            Log.w(TAG, "nv21ToJpeg failed", e);
            return null;
        }
    }

    /**
     * Convert a native camera buffer to JPEG for preview + NCNN.
     * Tray firmware usually fills YUYV; some builds may already embed JPEG (starts with FF D8).
     */
    private static byte[] nativeBufferToJpeg(byte[] raw, int width, int height) {
        if (raw == null || raw.length < 4) return null;
        if (raw[0] == (byte) 0xFF && raw[1] == (byte) 0xD8) {
            int end = raw.length;
            for (int i = 2; i < raw.length - 1; i++) {
                if (raw[i] == (byte) 0xFF && raw[i + 1] == (byte) 0xD9) {
                    end = i + 2;
                    break;
                }
            }
            // No EOI found — use the full buffer as-is; most decoders handle truncated JPEGs.
            return Arrays.copyOfRange(raw, 0, end);
        }
        return yuyvToJpeg(raw, width, height);
    }

    /**
     * Convert raw YUYV (YUV422 packed) bytes from the Keenon native camera to JPEG.
     * Buffer layout: [Y0 U0 Y1 V0 Y2 U1 Y3 V1 ...], size = width * height * 2.
     */
    private static byte[] yuyvToJpeg(byte[] yuyv, int width, int height) {
        try {
            int frameSize = width * height;
            byte[] nv21 = new byte[frameSize * 3 / 2];

            for (int i = 0; i < frameSize; i++) {
                nv21[i] = yuyv[i * 2];
            }

            int uvOffset = frameSize;
            for (int row = 0; row < height; row += 2) {
                for (int col = 0; col < width; col += 2) {
                    int yuyvIdx = row * width * 2 + col * 2;
                    byte u = yuyv[yuyvIdx + 1];
                    byte v = yuyv[yuyvIdx + 3];
                    nv21[uvOffset++] = v;
                    nv21[uvOffset++] = u;
                }
            }

            YuvImage yuv = new YuvImage(nv21, ImageFormat.NV21, width, height, null);
            ByteArrayOutputStream out = new ByteArrayOutputStream(frameSize / 4);
            yuv.compressToJpeg(new Rect(0, 0, width, height), 85, out);
            return out.toByteArray();
        } catch (Exception e) {
            Log.w(TAG, "yuyvToJpeg failed", e);
            return null;
        }
    }

    /**
     * Queue one preview frame; drops older frames if the main thread is still decoding/drawing.
     * Prevents OOM / surface faults from piling up ARGB Bitmaps on low-RAM robots.
     */
    private void schedulePreviewDraw(byte[] jpeg) {
        if (jpeg == null || surfaceView == null) return;
        synchronized (previewLock) {
            previewPendingJpeg = jpeg;
            if (previewFlushPosted) return;
            previewFlushPosted = true;
        }
        mainHandler.post(previewFlushRunnable);
    }

    /** Runs on main thread; drains at most one pending frame per posted runnable, loops if more arrived. */
    private void flushPreviewDrawRunnable() {
        while (previewing && surfaceView != null) {
            byte[] jpeg;
            synchronized (previewLock) {
                jpeg = previewPendingJpeg;
                previewPendingJpeg = null;
                if (jpeg == null) {
                    previewFlushPosted = false;
                    return;
                }
            }
            try {
                drawJpegToSurface(jpeg);
            } catch (Throwable t) {
                Log.w(TAG, "flushPreviewDrawRunnable", t);
            }
            synchronized (previewLock) {
                if (previewPendingJpeg == null) {
                    previewFlushPosted = false;
                    return;
                }
            }
        }
        synchronized (previewLock) {
            previewFlushPosted = false;
            previewPendingJpeg = null;
        }
    }

    /** Decode a JPEG and blit it to the SurfaceView for live native-mode preview. */
    private void drawJpegToSurface(byte[] jpeg) {
        if (surfaceView == null || jpeg == null || jpeg.length < 4) return;
        SurfaceHolder holder = surfaceView.getHolder();
        Surface surface = holder.getSurface();
        if (surface == null || !surface.isValid()) return;

        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inPreferredConfig = Bitmap.Config.RGB_565;

        Bitmap bmp = null;
        android.graphics.Canvas canvas = null;
        try {
            bmp = BitmapFactory.decodeByteArray(jpeg, 0, jpeg.length, opts);
            if (bmp == null) return;
            canvas = holder.lockCanvas();
            if (canvas != null) {
                int cw = canvas.getWidth();
                int ch = canvas.getHeight();
                if (cw > 0 && ch > 0) {
                    canvas.drawBitmap(bmp, null, new Rect(0, 0, cw, ch), null);
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "drawJpegToSurface failed", e);
        } finally {
            if (bmp != null && !bmp.isRecycled()) {
                bmp.recycle();
            }
            if (canvas != null) {
                try { holder.unlockCanvasAndPost(canvas); } catch (Exception ignored) {}
            }
        }
    }
}
