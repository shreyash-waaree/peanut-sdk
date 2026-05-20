package com.keenon.peanut.supermarket.vision;

import android.os.SystemClock;
import android.util.Log;

import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Counts physical items on a single robot tray using classical image processing
 * (no ML). One instance owns the state for ONE camera/tray (cams 0/1/2 on the
 * Keenon T10; cam 3 is the front-facing camera and is not handled here).
 *
 * <p>Pipeline per frame:
 * <pre>
 *   NV21 → grayscale → Gaussian blur
 *        → (if no baseline yet) auto-calibrate when frame is still + low-motion
 *        → absdiff vs baseline → Otsu threshold → morph close → findContours
 *        → filter by min area → instant_count
 *        → rolling window of {@link #STABILITY_WINDOW} frames, mode-smoothed
 *        → emit Delivered/Picked-up event when smoothed count differs from
 *          the last emitted count for {@link #STABILITY_EMIT_AGREE} frames
 * </pre>
 *
 * <p>The baseline image is whatever the tray looked like at calibration time —
 * anything present then is baked in and invisible. Events are deltas against
 * that calibration moment, which matches the "detect delivery / pickup" goal.
 *
 * <p>Self-heal: when the smoothed count has been 0 continuously for
 * {@link #RESET_AFTER_EMPTY_MS}, the baseline is re-snapshotted, handling slow
 * lighting drift, crumbs, slight tray shift, etc.
 *
 * <p>Native lib: relies on {@code libopencv_java4.so} which the senior bundled
 * in {@code src/main/jniLibs/armeabi-v7a/} for the NCNN YOLO pipeline; this
 * class only adds the Java bindings (via the QuickBird OpenCV Maven artifact).
 */
public class TrayCountingHelper {

    private static final String TAG_BASE = "TrayCount";

    // ---------- Tunable constants (start here when accuracy is off) ----------

    /** Consecutive still frames required before the baseline is captured. ~3 FPS × 5 = ~5 s wait. */
    public static final int CALIBRATION_FRAMES = 15;

    /** How many frames feed the rolling smoother. */
    public static final int STABILITY_WINDOW = 5;

    /** Within the rolling window, how many reads must agree before an event fires. */
    public static final int STABILITY_EMIT_AGREE = 3;

    /** Min contour area as a fraction of frame area — anything smaller is noise. */
    public static final double MIN_ITEM_AREA_PCT = 0.005;   // 0.5 % of frame

    /** Gaussian blur kernel — odd. Larger = more noise rejection, less detail. */
    public static final int GAUSSIAN_KERNEL = 5;

    /** Morphological-close kernel — odd. Larger = merges nearby blobs (cup rim + base). */
    public static final int MORPH_KERNEL = 15;

    /** During calibration, how many pixels may differ between consecutive frames and still count as "still". */
    public static final int MOTION_THRESHOLD_PX = 1500;

    /** Per-pixel diff threshold (0..255) used in the motion-detection step during calibration. */
    public static final int MOTION_PIXEL_DELTA = 25;

    /** Self-refresh baseline after the tray has been smoothly empty for this long. */
    public static final long RESET_AFTER_EMPTY_MS = 30_000L;

    // ---------- Result type ----------

    public enum EventType { NONE, DELIVERED, PICKED_UP }

    public static final class Result {
        public final boolean calibrating;
        public final int count;          // current smoothed count (0 while calibrating)
        public final EventType event;    // event emitted on this frame (NONE most of the time)
        public final int eventDelta;     // |delta| for the emitted event
        public final String overlay;     // pre-formatted overlay string

        private Result(boolean calibrating, int count, EventType event, int eventDelta, String overlay) {
            this.calibrating = calibrating;
            this.count = count;
            this.event = event;
            this.eventDelta = eventDelta;
            this.overlay = overlay;
        }
    }

    // ---------- Per-instance state ----------

    private final int trayId;
    private final String tag;
    private final boolean openCvReady;

    private Mat baseline;            // grayscale, blurred — the empty/initial-tray reference
    private Mat lastCalibFrame;      // previous frame during the calibration period (for motion check)
    private int calibrationStillFrames;

    private final Deque<Integer> rollingCounts = new ArrayDeque<>(STABILITY_WINDOW);
    private int lastEmittedCount = 0;
    private long emptySinceMs = 0L;
    private long lastEventMs = 0L;
    private EventType lastEventType = EventType.NONE;
    private int lastEventDelta = 0;

    /**
     * Optional ROI (Region of Interest) to restrict processing to the tray area.
     * Values are fractions of the full frame (0.0–1.0). When null, the full frame is used.
     * Setting this via {@link #setRoi} trims background legs/floor before any OpenCV work.
     */
    private float roiLeft = 0f, roiTop = 0f, roiRight = 1f, roiBottom = 1f;
    private boolean roiEnabled = false;

    /** Cumulative process() failures. After 3 we give up on this tray to stop spamming Logcat. */
    private int errorCount = 0;
    private static final int MAX_ERRORS_BEFORE_DISABLE = 3;

    public TrayCountingHelper(int trayId) {
        this.trayId = trayId;
        this.tag = TAG_BASE + "-" + trayId;
        boolean loaded = false;
        try {
            // initDebug() loads the bundled libopencv_java4.so. Safe to call multiple times
            // across multiple helper instances.
            loaded = OpenCVLoader.initDebug();
        } catch (Throwable t) {
            Log.e(tag, "OpenCV init failed", t);
        }
        this.openCvReady = loaded;
        if (!openCvReady) {
            Log.w(tag, "OpenCV not available — tray counting disabled for this instance");
        }
    }

    public boolean isReady() { return openCvReady; }

    /**
     * Restrict processing to a sub-rectangle of the camera frame — the tray region.
     * Pass fractions of the full frame width/height (e.g., left=0.2, top=0.15, right=0.8, bottom=0.85).
     * Anything outside the rectangle is ignored, so background objects (legs, floor, cables)
     * cannot create false detections. Calling this also resets calibration so the new baseline
     * is captured inside the ROI.
     *
     * @param left   fraction from left edge (0.0–1.0)
     * @param top    fraction from top edge  (0.0–1.0)
     * @param right  fraction from left edge (left < right ≤ 1.0)
     * @param bottom fraction from top edge  (top < bottom ≤ 1.0)
     */
    public synchronized void setRoi(float left, float top, float right, float bottom) {
        roiLeft   = Math.max(0f, Math.min(left,   0.99f));
        roiTop    = Math.max(0f, Math.min(top,    0.99f));
        roiRight  = Math.max(roiLeft  + 0.01f, Math.min(right,  1.0f));
        roiBottom = Math.max(roiTop   + 0.01f, Math.min(bottom, 1.0f));
        roiEnabled = true;
        reset();  // old baseline was on the full frame; re-calibrate inside the new ROI
        Log.i(tag, "Tray " + trayId + ": ROI set to ("
                + roiLeft + "," + roiTop + ")-(" + roiRight + "," + roiBottom + ")");
    }

    /** Remove the ROI restriction and process the full frame. Resets calibration. */
    public synchronized void clearRoi() {
        roiEnabled = false;
        reset();
        Log.i(tag, "Tray " + trayId + ": ROI cleared — using full frame");
    }

    /**
     * Process one NV21 frame.
     *
     * @param nv21   raw NV21 buffer from {@code Camera.setPreviewCallbackWithBuffer}
     * @param width  frame width
     * @param height frame height
     * @return a result describing current state; never null
     */
    public synchronized Result process(byte[] nv21, int width, int height) {
        if (!openCvReady || nv21 == null || width <= 0 || height <= 0) {
            return new Result(true, 0, EventType.NONE, 0, formatOverlay(true, 0, EventType.NONE, 0));
        }
        if (errorCount >= MAX_ERRORS_BEFORE_DISABLE) {
            return new Result(false, 0, EventType.NONE, 0,
                    "Tray " + trayId + " · disabled (errors — see Logcat)");
        }
        try {
            return processInner(nv21, width, height);
        } catch (Throwable t) {
            errorCount++;
            Log.e(tag, "Tray " + trayId + " process() failed (#" + errorCount + ") "
                    + "frame=" + width + "x" + height
                    + " baseline=" + (baseline != null
                            ? (baseline.cols() + "x" + baseline.rows() + " type=" + baseline.type())
                            : "null"),
                    t);
            return new Result(false, lastEmittedCount, EventType.NONE, 0,
                    "Tray " + trayId + " · error: " + t.getClass().getSimpleName());
        }
    }

    private Result processInner(byte[] nv21, int width, int height) {
        Mat gray = nv21ToGray(nv21, width, height);

        // Crop to tray ROI so background objects (legs, floor) are never analysed.
        if (roiEnabled) {
            int x  = (int) (roiLeft   * width);
            int y  = (int) (roiTop    * height);
            int w  = (int) ((roiRight  - roiLeft) * width);
            int h  = (int) ((roiBottom - roiTop)  * height);
            // Clamp to actual mat bounds (safety for off-by-one at edges)
            x = Math.max(0, Math.min(x, gray.cols() - 1));
            y = Math.max(0, Math.min(y, gray.rows() - 1));
            w = Math.max(1, Math.min(w, gray.cols() - x));
            h = Math.max(1, Math.min(h, gray.rows() - y));
            Mat cropped = new Mat(gray, new Rect(x, y, w, h)).clone();
            gray.release();
            gray = cropped;
            // Use cropped dimensions for area calculations downstream
            width  = w;
            height = h;
        }

        Mat blurred = new Mat();
        try {
            Imgproc.GaussianBlur(gray, blurred, new Size(GAUSSIAN_KERNEL, GAUSSIAN_KERNEL), 0);
        } finally {
            gray.release();
        }

        // Calibration phase: we don't have a baseline yet.
        if (baseline == null) {
            return stepCalibration(blurred);
        }

        // Sanity: if camera resolution changed under us, baseline is stale. Force re-calibration.
        if (baseline.cols() != blurred.cols() || baseline.rows() != blurred.rows()) {
            Log.w(tag, "Tray " + trayId + ": frame size changed (" + blurred.cols() + "x" + blurred.rows()
                    + " vs baseline " + baseline.cols() + "x" + baseline.rows() + ") — re-calibrating");
            releaseMats();
            blurred.release();
            return new Result(true, 0, EventType.NONE, 0,
                    formatOverlay(true, 0, EventType.NONE, 0));
        }

        // Detection phase.
        int instantCount = countContours(blurred, width, height);
        blurred.release();

        // Rolling smoother.
        rollingCounts.addLast(instantCount);
        while (rollingCounts.size() > STABILITY_WINDOW) rollingCounts.pollFirst();

        int smoothed = mode(rollingCounts);
        int agree = countOf(rollingCounts, smoothed);

        EventType event = EventType.NONE;
        int delta = 0;
        if (smoothed != lastEmittedCount && agree >= STABILITY_EMIT_AGREE) {
            int signed = smoothed - lastEmittedCount;
            delta = Math.abs(signed);
            event = signed > 0 ? EventType.DELIVERED : EventType.PICKED_UP;
            lastEmittedCount = smoothed;
            lastEventMs = SystemClock.uptimeMillis();
            lastEventType = event;
            lastEventDelta = delta;
            Log.i(tag, "Tray " + trayId + " event: " + event + " " + delta + " (count now " + smoothed + ")");
        }

        // Self-heal: refresh baseline after a long stretch of "empty".
        long now = SystemClock.uptimeMillis();
        if (smoothed == 0) {
            if (emptySinceMs == 0L) emptySinceMs = now;
            else if (now - emptySinceMs > RESET_AFTER_EMPTY_MS) {
                Log.i(tag, "Tray " + trayId + ": auto-refreshing baseline after long empty period");
                releaseMats();
                emptySinceMs = 0L;
            }
        } else {
            emptySinceMs = 0L;
        }

        return new Result(false, smoothed, event, delta,
                formatOverlay(false, smoothed,
                        event != EventType.NONE ? event : lastEventType,
                        event != EventType.NONE ? delta : lastEventDelta));
    }

    /** Forces the helper back into calibration mode. Hook for a manual "recalibrate" button. */
    public synchronized void reset() {
        releaseMats();
        rollingCounts.clear();
        lastEmittedCount = 0;
        emptySinceMs = 0L;
        lastEventMs = 0L;
        lastEventType = EventType.NONE;
        lastEventDelta = 0;
        Log.i(tag, "Tray " + trayId + " state reset; will re-calibrate");
    }

    public synchronized void close() {
        releaseMats();
        rollingCounts.clear();
    }

    // ---------- Internals ----------

    private void releaseMats() {
        if (baseline != null) { baseline.release(); baseline = null; }
        if (lastCalibFrame != null) { lastCalibFrame.release(); lastCalibFrame = null; }
        calibrationStillFrames = 0;
    }

    /** Returns true once enough still frames have been seen and the baseline is captured. */
    private Result stepCalibration(Mat currentBlurred) {
        if (currentBlurred == null || currentBlurred.empty()) {
            Log.w(tag, "stepCalibration: empty frame — skipping");
            if (currentBlurred != null) currentBlurred.release();
            return new Result(true, 0, EventType.NONE, 0,
                    formatOverlay(true, 0, EventType.NONE, 0));
        }
        if (lastCalibFrame == null) {
            lastCalibFrame = currentBlurred.clone();
            currentBlurred.release();
            calibrationStillFrames = 1;
            return new Result(true, 0, EventType.NONE, 0,
                    formatOverlay(true, 0, EventType.NONE, 0));
        }
        if (lastCalibFrame.empty()
                || lastCalibFrame.rows() != currentBlurred.rows()
                || lastCalibFrame.cols() != currentBlurred.cols()
                || lastCalibFrame.type() != currentBlurred.type()) {
            Log.w(tag, "stepCalibration: size/type mismatch — resetting motion reference");
            lastCalibFrame.release();
            lastCalibFrame = currentBlurred.clone();
            currentBlurred.release();
            calibrationStillFrames = 1;
            return new Result(true, 0, EventType.NONE, 0,
                    formatOverlay(true, 0, EventType.NONE, 0));
        }

        Mat diff = new Mat();
        Mat mask = new Mat();
        int motionPx;
        try {
            Core.absdiff(currentBlurred, lastCalibFrame, diff);
            Imgproc.threshold(diff, mask, MOTION_PIXEL_DELTA, 255, Imgproc.THRESH_BINARY);
            motionPx = Core.countNonZero(mask);
        } finally {
            diff.release();
            mask.release();
        }

        lastCalibFrame.release();
        lastCalibFrame = currentBlurred.clone();

        if (motionPx < MOTION_THRESHOLD_PX) {
            calibrationStillFrames++;
            if (calibrationStillFrames >= CALIBRATION_FRAMES) {
                baseline = currentBlurred;       // take ownership
                lastCalibFrame.release();
                lastCalibFrame = null;
                calibrationStillFrames = 0;
                lastEmittedCount = 0;
                rollingCounts.clear();
                Log.i(tag, "Tray " + trayId + ": baseline captured");
                return new Result(false, 0, EventType.NONE, 0,
                        "Tray " + trayId + " · ready (0 items)");
            }
            currentBlurred.release();
            return new Result(true, 0, EventType.NONE, 0,
                    formatOverlay(true, calibrationStillFrames, EventType.NONE, 0));
        } else {
            calibrationStillFrames = 0;
            currentBlurred.release();
            return new Result(true, 0, EventType.NONE, 0,
                    formatOverlay(true, 0, EventType.NONE, 0));
        }
    }

    private int countContours(Mat blurred, int width, int height) {
        Mat diff = new Mat();
        Mat mask = new Mat();
        Mat morphed = new Mat();
        Mat kernel = null;
        Mat hierarchy = new Mat();
        List<MatOfPoint> contours = new ArrayList<>();
        try {
            Core.absdiff(blurred, baseline, diff);
            Imgproc.threshold(diff, mask, 0, 255, Imgproc.THRESH_BINARY | Imgproc.THRESH_OTSU);
            kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT,
                    new Size(MORPH_KERNEL, MORPH_KERNEL));
            Imgproc.morphologyEx(mask, morphed, Imgproc.MORPH_CLOSE, kernel);
            Imgproc.findContours(morphed, contours, hierarchy,
                    Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

            double minArea = (double) width * height * MIN_ITEM_AREA_PCT;
            int n = 0;
            for (MatOfPoint c : contours) {
                if (Imgproc.contourArea(c) >= minArea) n++;
            }
            return n;
        } finally {
            for (MatOfPoint c : contours) c.release();
            diff.release();
            mask.release();
            morphed.release();
            hierarchy.release();
            if (kernel != null) kernel.release();
        }
    }

    private Mat nv21ToGray(byte[] nv21, int width, int height) {
        // NV21 layout: Y plane (W*H bytes) followed by interleaved VU plane (W*H/2 bytes).
        // cvtColor COLOR_YUV2GRAY_NV21 extracts only the Y plane, which is exactly the grayscale we want.
        Mat yuv = new Mat(height + height / 2, width, CvType.CV_8UC1);
        yuv.put(0, 0, nv21);
        Mat gray = new Mat();
        Imgproc.cvtColor(yuv, gray, Imgproc.COLOR_YUV2GRAY_NV21);
        yuv.release();
        return gray;
    }

    private String formatOverlay(boolean calibrating, int count, EventType lastEvt, int lastDelta) {
        if (calibrating) {
            int pct = Math.min(100, (int) (100L * calibrationStillFrames / CALIBRATION_FRAMES));
            return String.format(Locale.US, "Tray %d · calibrating %d%%", trayId, pct);
        }
        if (lastEvt == EventType.NONE || lastDelta == 0) {
            return String.format(Locale.US, "Tray %d · %d %s", trayId, count, count == 1 ? "item" : "items");
        }
        String verb = lastEvt == EventType.DELIVERED ? "Delivered" : "Picked up";
        return String.format(Locale.US, "Tray %d · %d %s · last: %s %d",
                trayId, count, count == 1 ? "item" : "items", verb, lastDelta);
    }

    private static int mode(Deque<Integer> window) {
        if (window.isEmpty()) return 0;
        // NOTE: Map.getOrDefault() is API 24+; the Keenon T10 runs Android 6 (API 23),
        // so we use a manual null check instead. Don't reintroduce getOrDefault() here.
        Map<Integer, Integer> freq = new HashMap<>();
        int best = 0;
        int bestN = -1;
        for (Integer v : window) {
            Integer prev = freq.get(v);
            int n = (prev == null ? 0 : prev) + 1;
            freq.put(v, n);
            if (n > bestN) { bestN = n; best = v; }
        }
        return best;
    }

    private static int countOf(Deque<Integer> window, int target) {
        int n = 0;
        for (Integer v : window) if (v == target) n++;
        return n;
    }
}