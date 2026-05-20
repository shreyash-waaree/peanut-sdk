package com.keenon.peanut.supermarket.fragment;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Typeface;
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
import com.keenon.peanut.supermarket.camera.TrayCameraHal;
import com.keenon.peanut.supermarket.vision.ObjectDetectorHelper;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Live camera preview for on-robot diagnostics.
 *
 * <p>Renders explicit 0/1/2/3 camera-index buttons (one per available camera) and keeps the preview
 * SurfaceView sized to the camera's native aspect ratio so the image isn't stretched.
 */
public class CameraFragment extends Fragment implements SurfaceHolder.Callback {

  private static final String TAG = "CameraFragment";
  private static final int REQ_CAMERA = 2201;

  private FrameLayout previewContainer;
  private SurfaceView surfaceView;
  private TextView statusText;
  private TextView placeholder;
  private TextView labelOverlay;
  private LinearLayout indexRow;
  private Button btnStart, btnStop;
  private final java.util.List<Button> indexButtons = new java.util.ArrayList<>();

  private Camera camera;
  private int currentCameraId = 0;
  private int cameraCount;
  private boolean previewing;
  private boolean surfaceReady;
  private boolean startRequested;
  private int previewWidth;
  private int previewHeight;

  // --- Object detection ---
  // Minimum gap between detections (soft cap ~3 FPS). Raises to let UI thread breathe.
  private static final long DETECT_INTERVAL_MS = 300L;

  private ObjectDetectorHelper detector;
  private HandlerThread detectThread;
  private Handler detectHandler;
  private final Handler mainHandler = new Handler(Looper.getMainLooper());
  private final AtomicBoolean detectBusy = new AtomicBoolean(false);
  private long lastDetectAt;

  @Nullable
  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_dev_camera, container, false);
  }

  @Override
  public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(v, savedInstanceState);

    previewContainer = v.findViewById(R.id.cam_preview_container);
    surfaceView = v.findViewById(R.id.cam_surface);
    statusText = v.findViewById(R.id.cam_status);
    placeholder = v.findViewById(R.id.cam_placeholder);
    labelOverlay = v.findViewById(R.id.cam_labels);
    indexRow = v.findViewById(R.id.cam_index_row);

    detector = new ObjectDetectorHelper(requireContext());

    btnStart = v.findViewById(R.id.btn_cam_start);
    btnStop = v.findViewById(R.id.btn_cam_stop);

    surfaceView.getHolder().addCallback(this);

    btnStart.setOnClickListener(view -> requestStart());
    btnStop.setOnClickListener(view -> stopPreview());

    cameraCount = Math.max(1, Camera.getNumberOfCameras());
    rebuildIndexButtons();
  }

  private void rebuildIndexButtons() {
    indexRow.removeAllViews();
    indexButtons.clear();
    int dpMargin = (int) (6 * getResources().getDisplayMetrics().density);
    for (int i = 0; i < cameraCount; i++) {
      Button b = new Button(getContext());
      b.setText(String.valueOf(i));
      b.setTypeface(b.getTypeface(), Typeface.BOLD);
      LinearLayout.LayoutParams lp =
          new LinearLayout.LayoutParams(
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
    for (int i = 0; i < indexButtons.size(); i++) {
      Button b = indexButtons.get(i);
      if (i == currentCameraId && previewing) {
        b.setBackgroundColor(Color.parseColor("#F59E0B"));
        b.setTextColor(Color.BLACK);
      } else {
        b.setBackground(null);
        // Reset to default button look by re-creating via a dummy transition; in practice we
        // rely on the system default drawable after background=null.
        b.setTextColor(Color.WHITE);
      }
    }
  }

  private void onSelectCamera(int index) {
    if (index == currentCameraId && previewing) return;
    currentCameraId = index;
    if (previewing) {
      stopPreview();
      startRequested = true;
      startPreview();
    } else {
      // Just update highlight; user still needs to tap Start.
      highlightActiveIndex();
      statusText.setText(
          getString(R.string.cam_status_running, index) + " (tap Start to preview)");
    }
  }

  @Override
  public void onPause() {
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
      statusText.setText("Waiting for preview surface…");
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

  private void startPreview() {
    if (previewing) return;
    SurfaceHolder holder = surfaceView.getHolder();
    if (holder.getSurface() == null) {
      return;
    }
    if (currentCameraId < 0 || currentCameraId >= Camera.getNumberOfCameras()) {
      Toast.makeText(requireContext(), "Invalid camera id " + currentCameraId, Toast.LENGTH_SHORT)
          .show();
      return;
    }
    try {
      camera = Camera.open(currentCameraId);

      // Register error callback to catch Camera HAL crashes (e.g. Error 100 = server died).
      camera.setErrorCallback((error, cam) -> {
        Log.e(TAG, "Camera hardware error: " + error + " on camera " + currentCameraId);
        mainHandler.post(() -> {
          String msg = error == Camera.CAMERA_ERROR_SERVER_DIED
              ? "Camera " + currentCameraId + " HAL crashed (try a different camera index)"
              : "Camera hardware error " + error;
          Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show();
          releaseCamera();
          stopPreview();
        });
      });

      Camera.Parameters params = safeGetParameters(camera);
      if (params == null) {
        // Empty parameters means this camera index is not usable (system/restricted camera).
        Log.w(TAG, "Camera " + currentCameraId + " returned empty parameters — skipping");
        Toast.makeText(requireContext(),
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

      camera.startPreview();
      previewing = true;
      placeholder.setVisibility(View.GONE);
      btnStart.setEnabled(false);
      btnStop.setEnabled(true);

      if (TrayCameraHal.isFragileRockChipHal()) {
        statusText.setText(
            getString(R.string.cam_status_running, currentCameraId)
                + " · "
                + previewWidth
                + "×"
                + previewHeight
                + " · preview only");
        Toast.makeText(requireContext(),
            "Object labels disabled on RK3288 (camera driver crashes on frame callbacks).",
            Toast.LENGTH_LONG).show();
      } else {
        startDetectThread();
        attachDetectionCallback();
        statusText.setText(
            getString(R.string.cam_status_running, currentCameraId)
                + " · "
                + previewWidth
                + "×"
                + previewHeight);
      }
      resizeSurfaceToAspect();
      highlightActiveIndex();
    } catch (IOException | RuntimeException e) {
      Log.e(TAG, "startPreview failed", e);
      Toast.makeText(requireContext(), "Camera open failed: " + e.getMessage(), Toast.LENGTH_LONG)
          .show();
      releaseCamera();
      highlightActiveIndex();
    }
  }

  /**
   * Size the SurfaceView to the largest rectangle that (a) fits inside {@link #previewContainer}
   * and (b) has the camera's native aspect ratio. This prevents the "spread / stretched" look.
   */
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

            // Camera gives frames in landscape (width > height). If we render without rotation,
            // match the sensor aspect directly.
            float camAspect = (float) previewWidth / previewHeight;
            int targetW;
            int targetH;
            if (w / (float) h > camAspect) {
              // Container is wider than camera aspect — letterbox left/right.
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

  private void stopPreview() {
    startRequested = false;
    releaseCamera();
    placeholder.setVisibility(View.VISIBLE);
    btnStart.setEnabled(true);
    btnStop.setEnabled(false);
    statusText.setText(R.string.cam_status_idle);
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
    if (labelOverlay != null) {
      labelOverlay.setVisibility(View.GONE);
      labelOverlay.setText("");
    }
  }

  // ---------- Object detection plumbing ----------

  private void startDetectThread() {
    if (detectThread != null) return;
    detectThread = new HandlerThread("ObjDetect");
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
    int bufSize = previewWidth * previewHeight * 3 / 2; // NV21 = 1.5 bytes/px
    // Two buffers — one being processed, one being filled.
    camera.addCallbackBuffer(new byte[bufSize]);
    camera.addCallbackBuffer(new byte[bufSize]);
    camera.setPreviewCallbackWithBuffer(this::onPreviewFrame);
  }

  private void onPreviewFrame(byte[] data, Camera cam) {
    if (data == null || cam == null) return;
    long now = SystemClock.uptimeMillis();
    if (detectBusy.get() || now - lastDetectAt < DETECT_INTERVAL_MS) {
      // Skip: requeue buffer immediately so preview keeps flowing.
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
    h2.post(
        () -> {
          List<ObjectDetectorHelper.Result> detections = detector.detect(data, w, h, 0);
          String summary = formatDetections(detections);
          mainHandler.post(() -> showLabels(summary));
          if (camera != null) {
            try {
              camera.addCallbackBuffer(data);
            } catch (Exception ignored) {
            }
          }
          detectBusy.set(false);
        });
  }

  private static String formatDetections(List<ObjectDetectorHelper.Result> detections) {
    if (detections == null || detections.isEmpty()) return "";
    StringBuilder sb = new StringBuilder();
    for (ObjectDetectorHelper.Result d : detections) {
      if (sb.length() > 0) sb.append("   ");
      sb.append(String.format(Locale.US, "%s %.0f%%", d.label, d.score * 100f));
    }
    return sb.toString();
  }

  private void showLabels(String text) {
    if (labelOverlay == null) return;
    if (text == null || text.isEmpty()) {
      labelOverlay.setVisibility(View.GONE);
      labelOverlay.setText("");
    } else {
      labelOverlay.setText(text);
      labelOverlay.setVisibility(View.VISIBLE);
    }
  }

  @Override
  public void onDestroyView() {
    if (detector != null) {
      detector.close();
    }
    super.onDestroyView();
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
    if (camera != null) {
      try {
        camera.stopPreview();
        camera.setPreviewDisplay(holder);
        camera.startPreview();
      } catch (Exception e) {
        Log.e(TAG, "surfaceChanged preview restart failed", e);
      }
    }
  }

  @Override
  public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
    surfaceReady = false;
    releaseCamera();
  }
}
