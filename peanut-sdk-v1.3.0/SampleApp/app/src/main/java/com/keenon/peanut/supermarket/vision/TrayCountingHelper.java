package com.keenon.peanut.supermarket.vision;

import android.content.Context;
import android.os.SystemClock;
import android.util.Log;

import com.keenon.peanut.sample.DemoApplication;

import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Box-based tray item counter using classical OpenCV (no ML).
 *
 * <p>Each tray is divided into a fixed grid of slots (the user puts white tape on the tray
 * to mark the slot boundaries). For every preview frame we ask, per slot: "does this slot
 * differ from the empty-tray baseline by enough pixels to be considered occupied?" The
 * total count is the number of occupied slots, bounded by the grid size (max 4 for 2×2,
 * 9 for 3×3, 6 for 2×3).
 *
 * <p>Pipeline per frame:
 * <pre>
 *   NV21 → grayscale → Gaussian blur
 *        → (if no baseline yet) auto-calibrate when frame is still
 *        → at end of calibration: detect the white-tape grid (or fall back to even grid)
 *        → per slot: absdiff vs baseline, threshold, count changed pixels
 *        → slot occupied if changed-pixel fraction > {@link #OCCUPIED_FRACTION_THRESHOLD}
 *        → total = sum of occupied slots
 *        → rolling smoother, event emission on stable count change
 * </pre>
 *
 * <p>Grid detection: at the end of calibration we threshold the baseline image to find the
 * bright (white-tape) pixels, then sum that mask along rows and columns. The strongest
 * row-projection peaks are horizontal tape lines; the strongest column-projection peaks
 * are vertical tape lines. With (rows-1) horizontal + (cols-1) vertical lines we partition
 * the frame into exactly rows×cols slot rectangles. If we can't find enough peaks (tape
 * obscured / missing), we fall back to an even grid spanning the centre of the frame.
 *
 * <p>The class is API-23 safe (no Java 8 default-method calls like {@code Map.getOrDefault}).
 */
public class TrayCountingHelper {

    private static final String TAG_BASE = "TrayCount";

    private static volatile boolean openCvInitialized = false;

    /**
     * Call from {@link com.keenon.peanut.sample.DemoApplication} after a successful
     * {@code System.load} / {@code System.loadLibrary} of {@code libopencv_java4.so}.
     * Avoids counting init failure when startup preload already mapped the library and a
     * second explicit {@code System.load(path)} would throw "already loaded".
     */
    public static void notifyOpenCvNativeLoaded() {
        openCvInitialized = true;
    }

    private static boolean isAlreadyLoadedLinkError(UnsatisfiedLinkError e) {
        String m = e.getMessage();
        if (m == null) return false;
        String lower = m.toLowerCase(Locale.US);
        return lower.contains("already loaded") || lower.contains("already opened");
    }

    // ---------- Grid layouts ----------

    /** Pre-defined grid layouts selectable by the user (2 / 4 / 6 / 9 slots). */
    public enum BoxLayout {
        GRID_1x2(1, 2),  // 2 slots — one row, two columns
        GRID_2x2(2, 2),  // 4 slots
        GRID_2x3(2, 3),  // 6 slots (2 rows × 3 cols)
        GRID_3x3(3, 3),  // 9 slots
        GRID_4x4(4, 4);  // 16 slots — Count2 tab only

        public final int rows;
        public final int cols;
        BoxLayout(int rows, int cols) { this.rows = rows; this.cols = cols; }
        public int slotCount() { return rows * cols; }

        /** Maps user-facing slot totals (2, 4, 6, 9) to a grid layout. */
        public static BoxLayout fromSlotCount(int slotCount) {
            switch (slotCount) {
                case 2:  return GRID_1x2;
                case 4:  return GRID_2x2;
                case 6:  return GRID_2x3;
                case 9:  return GRID_3x3;
                default: return GRID_2x2;
            }
        }

        public static boolean isSupportedSlotCount(int slotCount) {
            return slotCount == 2 || slotCount == 4 || slotCount == 6 || slotCount == 9;
        }
    }

    // ---------- Tunable constants ----------

    /**
     * Frames averaged into the empty-tray baseline. No motion gate — reliable on robot
     * cameras with auto-exposure (motion-based calib was stuck at 0% on device).
     */
  /** Fewer frames = less native work at completion (robot was crashing at ~80% with 5). */
    public static final int CALIB_ACCUM_FRAMES = 3;

    /** How many frames feed the rolling smoother on the TOTAL count. */
    public static final int STABILITY_WINDOW = 7;

    /** Within the rolling window, how many reads must agree before an event fires. */
    public static final int STABILITY_EMIT_AGREE = 5;

    /** Per-pixel brightness diff (0..255) treated as "this pixel changed". */
    public static final int OCCUPIED_PIXEL_DELTA = 28;

    /** Fraction of changed pixels to mark a slot occupied (turn-on threshold). */
    public static final double OCCUPIED_FRACTION_THRESHOLD = 0.065;

    /** Lower fraction to clear a slot — hysteresis stops flicker at the boundary. */
    public static final double CLEAR_FRACTION_THRESHOLD = 0.038;

    /** Consecutive frames above occupied threshold before a slot flips to "full". */
    public static final int SLOT_OCCUPY_CONFIRM_FRAMES = 3;

    /** Consecutive frames below clear threshold before a slot flips to "empty". */
    public static final int SLOT_CLEAR_CONFIRM_FRAMES = 4;

    /** Gaussian blur kernel — odd. Larger = more noise rejection. */
    public static final int GAUSSIAN_KERNEL = 5;


    /** Per-pixel diff threshold used in the motion-detection step during calibration. */
    public static final int MOTION_PIXEL_DELTA = 25;

    /** Brightness above this (0..255) on the empty-tray baseline = candidate white-tape pixel. */
    public static final int WHITE_TAPE_THRESHOLD = 190;

    /** Self-refresh baseline only after the tray stays stably empty for this long. */
    public static final long RESET_AFTER_EMPTY_MS = 60_000L;

    /** Pixels of inset around each detected slot, so the tape itself isn't included in the ROI. */
    public static final int SLOT_INNER_MARGIN_PX = 6;

    // ---------- Result type ----------

    public enum EventType { NONE, DELIVERED, PICKED_UP }

    public static final class Result {
        public final boolean calibrating;
        /** 0..100 while calibrating; -1 when not calibrating. */
        public final int calibratePercent;
        public final int count;          // current smoothed occupied-slot count
        public final int maxSlots;       // total slot capacity (4 / 6 / 9)
        public final EventType event;    // event emitted on this frame (NONE most frames)
        public final int eventDelta;     // |delta| for the emitted event
        public final String overlay;     // pre-formatted overlay string
        /** Slot rectangles in camera-frame pixels: x,y,w,h per slot; empty when calibrating. */
        public final int[] slotBounds;
        /**
         * Normalized frame X (0..1) of the slot that was just cleared on {@link EventType#PICKED_UP};
         * {@code -1f} if unknown. Used to rotate toward the customer.
         */
        public final float pickHandNormX;
        /** Normalized frame Y of cleared slot on {@link EventType#PICKED_UP}; {@code -1f} if unknown. */
        public final float pickHandNormY;

        private Result(boolean calibrating, int calibratePercent, int count, int maxSlots,
                       EventType event, int eventDelta, String overlay, int[] slotBounds,
                       float pickHandNormX, float pickHandNormY) {
            this.calibrating = calibrating;
            this.calibratePercent = calibratePercent;
            this.count = count;
            this.maxSlots = maxSlots;
            this.event = event;
            this.eventDelta = eventDelta;
            this.overlay = overlay;
            this.slotBounds = slotBounds;
            this.pickHandNormX = pickHandNormX;
            this.pickHandNormY = pickHandNormY;
        }
    }

    // ---------- Per-instance state ----------

    private final int trayId;
    private BoxLayout layout;
    private final String tag;

    private Mat baseline;            // grayscale, blurred — the empty/initial-tray reference
    /** CV_32F running sum for calibration (CV_8U add saturates at 255 and corrupts baseline). */
    private Mat calibSumMat;
    private int calibFrameCount;
    /** Baseline captured; slot grid is built on the next frame (spreads native work). */
    private boolean gridPending;
    /** Slot overlay during calibrating — built once, not every frame. */
    private int[] cachedPreviewSlotBounds = new int[0];

    /**
     * Optional oval ROI mask in CAMERA-FRAME pixel coordinates (CV_8UC1, 255 inside, 0 outside).
     * When set, all calibration motion checks, white-tape grid detection, and slot-occupancy
     * checks are restricted to pixels inside this mask. The mask is independent of the slot
     * grid — slots may overlap the boundary; only the in-mask portion of each slot counts.
     */
    private Mat roiMask;
    /** ROI ellipse params in camera-frame pixels — kept for logging / regeneration after frame size change. */
    private int roiCx = -1, roiCy = -1, roiRx = -1, roiRy = -1;
    private int roiFrameW = -1, roiFrameH = -1;

    /** Detected (or fallback) slot rectangles, populated at end of calibration. */
    private List<Rect> slots = new ArrayList<>();
    /** Marker so the overlay can say whether boxes came from auto-detect or fallback. */
    private boolean gridFromTapeDetection = false;

    private final Deque<Integer> rollingCounts = new ArrayDeque<>(STABILITY_WINDOW);
    /** Per-slot debounced occupied state (length == slots.size() after calibration). */
    private boolean[] slotStableOccupied;
    private int[] slotOccupyRun;
    private int[] slotClearRun;
    private int lastEmittedCount = 0;
    /** Frame-normalized center of the slot that just became empty (for pick-facing / angle). */
    private float lastPickNormX = -1f;
    private float lastPickNormY = -1f;
    private long emptySinceMs = 0L;
    private EventType lastEventType = EventType.NONE;
    private int lastEventDelta = 0;

    /** Cumulative process() failures. After 3 we give up on this tray to stop spamming Logcat. */
    private int errorCount = 0;
    private static final int MAX_ERRORS_BEFORE_DISABLE = 3;

    // ---------- Construction ----------

    public TrayCountingHelper(int trayId, BoxLayout layout) {
        this.trayId = trayId;
        this.layout = layout;
        this.tag = TAG_BASE + "-" + trayId;
        initOpenCv();
        if (!openCvInitialized) {
            Log.w(tag, "OpenCV not available — tray counting disabled for this instance");
        }
    }

    /** Re-check native libs (e.g. after delayed APK extraction); safe to call from UI before ROI confirm. */
    public boolean isReady() {
        return initOpenCv();
    }

    /** True until baseline exists and the slot grid has been built. */
    public synchronized boolean isCalibrating() {
        return hasRoi() && (baseline == null || gridPending);
    }

    private static boolean tryLoadOpenCvExplicit() {
        Context ctx = DemoApplication.getAppContext();
        if (ctx == null) return false;
        try {
            String dir = ctx.getApplicationInfo().nativeLibraryDir;
            if (dir == null) return false;
            File cpp = new File(dir, "libc++_shared.so");
            File cv = new File(dir, "libopencv_java4.so");
            if (!cv.exists()) {
                Log.w(TAG_BASE, "libopencv_java4.so not found under nativeLibraryDir=" + dir);
                return false;
            }
            if (cpp.exists()) {
                try {
                    System.load(cpp.getAbsolutePath());
                } catch (Throwable t) {
                    Log.w(TAG_BASE, "libc++_shared explicit load", t);
                }
            }
            System.load(cv.getAbsolutePath());
            Log.i(TAG_BASE, "OpenCV loaded explicitly: " + cv.getAbsolutePath());
            return true;
        } catch (UnsatisfiedLinkError e) {
            if (isAlreadyLoadedLinkError(e)) {
                Log.i(TAG_BASE, "OpenCV library already loaded (skipping duplicate explicit load)");
                return true;
            }
            Log.w(TAG_BASE, "explicit OpenCV load failed", e);
            return false;
        } catch (Throwable t) {
            Log.w(TAG_BASE, "explicit OpenCV load failed", t);
            return false;
        }
    }

    private static boolean initOpenCv() {
        if (openCvInitialized) return true;
        synchronized (TrayCountingHelper.class) {
            if (openCvInitialized) return true;
            if (tryLoadOpenCvExplicit()) {
                openCvInitialized = true;
                return true;
            }
            // On-device / release APK: OpenCVLoader.initDebug() is usually false (no OpenCV Manager).
            try {
                System.loadLibrary("c++_shared");
            } catch (Throwable ignored) {
            }
            try {
                System.loadLibrary("opencv_java4");
                openCvInitialized = true;
                Log.i(TAG_BASE, "OpenCV native OK (opencv_java4)");
                return true;
            } catch (Throwable t) {
                Log.w(TAG_BASE, "System.loadLibrary(opencv_java4) failed — trying OpenCVLoader.initDebug",
                        t);
            }
            try {
                if (OpenCVLoader.initDebug()) {
                    openCvInitialized = true;
                    Log.i(TAG_BASE, "OpenCV OK (OpenCVLoader.initDebug)");
                    return true;
                }
            } catch (Throwable t) {
                Log.e(TAG_BASE, "OpenCVLoader.initDebug failed", t);
            }
            Log.e(TAG_BASE, "OpenCV unavailable — counting disabled");
            return false;
        }
    }

    /** Clears baseline and restarts the still-frame calibration sequence. */
    public synchronized void beginCalibration() {
        releaseMats();
        gridPending = false;
        lastEmittedCount = 0;
        rollingCounts.clear();
        emptySinceMs = 0L;
        refreshCachedPreviewSlotBounds();
        Log.i(tag, "Tray " + trayId + ": calibration started");
    }

    private void refreshCachedPreviewSlotBounds() {
        if (!hasRoi() || roiFrameW <= 0 || roiFrameH <= 0) {
            cachedPreviewSlotBounds = new int[0];
            return;
        }
        cachedPreviewSlotBounds =
                rectsToBoundsArray(buildEvenGrid(roiFrameW, roiFrameH, layout.rows, layout.cols));
    }

    /**
     * Slot grid inside the ROI — used to draw boxes on the overlay while calibrating.
     */
    public synchronized int[] getPreviewSlotBounds() {
        return cachedPreviewSlotBounds != null ? cachedPreviewSlotBounds : new int[0];
    }

    /**
     * Re-apply ROI when camera preview size differs from the size stored at confirm time.
     * Call from the UI thread right before {@link #beginCalibration()}.
     */
    public synchronized void syncRoiToFrameSize(int frameW, int frameH) {
        if (!hasRoi() || frameW <= 0 || frameH <= 0) return;
        if (roiFrameW == frameW && roiFrameH == frameH) return;
        float sx = (float) frameW / roiFrameW;
        float sy = (float) frameH / roiFrameH;
        setRoiEllipse(
                frameW,
                frameH,
                Math.round(roiCx * sx),
                Math.round(roiCy * sy),
                Math.max(1, Math.round(roiRx * sx)),
                Math.max(1, Math.round(roiRy * sy)));
        refreshCachedPreviewSlotBounds();
    }

    public synchronized int getSlotCount() { return layout.slotCount(); }

    /**
     * Change how many slots this tray is divided into. Forces re-calibration on next frames.
     */
    public synchronized void setBoxLayout(BoxLayout newLayout) {
        if (newLayout == null || newLayout == layout) return;
        layout = newLayout;
        reset();
        Log.i(tag, "Tray " + trayId + ": layout set to " + layout.rows + "x" + layout.cols
                + " (" + layout.slotCount() + " slots)");
    }

    /**
     * Set the oval region-of-interest for this tray. Must be called BEFORE the first
     * {@link #process(byte[], int, int)} call (or after a {@link #reset()}) to take effect
     * before calibration. Ellipse is in camera-frame pixel coordinates.
     *
     * <p>Calling this with the same dimensions multiple times is cheap and idempotent.
     * Passing {@code frameW <= 0} or any radius {@code <= 0} clears the ROI.
     */
    public synchronized void setRoiEllipse(int frameW, int frameH, int cx, int cy, int rx, int ry) {
        if (frameW <= 0 || frameH <= 0 || rx <= 0 || ry <= 0) {
            clearRoiMask();
            return;
        }
        clearRoiMask();
        Mat mask = Mat.zeros(frameH, frameW, CvType.CV_8UC1);
        Imgproc.ellipse(mask,
                new Point(cx, cy),
                new Size(rx, ry),
                0.0,   // angle
                0.0,   // startAngle
                360.0, // endAngle
                new Scalar(255),
                -1);   // filled
        roiMask = mask;
        roiFrameW = frameW;
        roiFrameH = frameH;
        roiCx = cx; roiCy = cy; roiRx = rx; roiRy = ry;
        // Any in-progress calibration / baseline is now stale — force a clean restart.
        if (baseline != null) { baseline.release(); baseline = null; }
        gridPending = false;
        clearCalibAccum();
        slots.clear();
        rollingCounts.clear();
        lastEmittedCount = 0;
        refreshCachedPreviewSlotBounds();
        Log.i(tag, "Tray " + trayId + ": ROI set ellipse cx=" + cx + " cy=" + cy
                + " rx=" + rx + " ry=" + ry + " in frame " + frameW + "x" + frameH);
    }

    /** Discards the ROI mask. Subsequent processing uses the whole frame. */
    public synchronized void clearRoi() {
        clearRoiMask();
        Log.i(tag, "Tray " + trayId + ": ROI cleared");
    }

    public synchronized boolean hasRoi() { return roiMask != null; }

    private void clearRoiMask() {
        if (roiMask != null) { roiMask.release(); roiMask = null; }
        roiCx = roiCy = roiRx = roiRy = -1;
        roiFrameW = roiFrameH = -1;
    }

    // ---------- Public API ----------

    /**
     * Process one NV21 frame. Never throws; failures are caught, logged, and exposed via the
     * overlay text. After {@link #MAX_ERRORS_BEFORE_DISABLE} failures the helper disables itself.
     */
    public synchronized Result process(byte[] nv21, int width, int height) {
        if (!initOpenCv()) {
            return new Result(false, -1, 0, layout.slotCount(), EventType.NONE, 0,
                    "Tray " + trayId + " · OpenCV not loaded", null, -1f, -1f);
        }
        if (nv21 == null || width <= 0 || height <= 0) {
            return calibratingResult(0);
        }
        int nv21Need = nv21StrideBytes(width, height);
        if (nv21.length < nv21Need) {
            Log.w(tag, "Tray " + trayId + ": NV21 too short len=" + nv21.length
                    + " need>=" + nv21Need + " for " + width + "x" + height);
            return calibratingResult(calibFramePct());
        }
        byte[] usable = nv21;
        if (nv21.length > nv21Need) {
            usable = Arrays.copyOf(nv21, nv21Need);
        }
        if (!hasRoi()) {
            return new Result(true, 0, 0, layout.slotCount(), EventType.NONE, 0,
                    "Tray " + trayId + " · confirm tray shape first", null, -1f, -1f);
        }
        if (errorCount >= MAX_ERRORS_BEFORE_DISABLE) {
            return new Result(false, -1, 0, layout.slotCount(), EventType.NONE, 0,
                    "Tray " + trayId + " · disabled (errors — see Logcat)", null, -1f, -1f);
        }
        try {
            return processInner(usable, width, height);
        } catch (Throwable t) {
            errorCount++;
            Log.e(tag, "Tray " + trayId + " process() failed (#" + errorCount + ") "
                    + "frame=" + width + "x" + height
                    + " baseline=" + (baseline != null
                            ? (baseline.cols() + "x" + baseline.rows() + " type=" + baseline.type())
                            : "null")
                    + " slots=" + slots.size(),
                    t);
            return new Result(false, -1, lastEmittedCount, layout.slotCount(), EventType.NONE, 0,
                    "Tray " + trayId + " · error: " + t.getClass().getSimpleName(), null, -1f, -1f);
        }
    }

    /** Keeps baseline and slot grid; use after customer feedback so counting resumes at {@code count}. */
    public synchronized void acknowledgePickCount(int count) {
        lastEmittedCount = Math.max(0, Math.min(count, layout.slotCount()));
        rollingCounts.clear();
        for (int i = 0; i < STABILITY_WINDOW; i++) {
            rollingCounts.addLast(lastEmittedCount);
        }
        emptySinceMs = 0L;
        lastPickNormX = -1f;
        lastPickNormY = -1f;
        Log.i(tag, "Tray " + trayId + ": pick acknowledged — continuing at " + lastEmittedCount
                + "/" + layout.slotCount());
    }

    /** Forces the helper back into calibration mode. Hook for a manual "recalibrate" button. */
    public synchronized void reset() {
        releaseMats();
        slots.clear();
        clearSlotState();
        gridFromTapeDetection = false;
        rollingCounts.clear();
        lastEmittedCount = 0;
        emptySinceMs = 0L;
        lastEventType = EventType.NONE;
        lastEventDelta = 0;
        Log.i(tag, "Tray " + trayId + " state reset; will re-calibrate");
    }

    private void clearSlotState() {
        slotStableOccupied = null;
        slotOccupyRun = null;
        slotClearRun = null;
    }

    private void initSlotState(int slotCount) {
        slotStableOccupied = new boolean[slotCount];
        slotOccupyRun = new int[slotCount];
        slotClearRun = new int[slotCount];
    }

    private boolean allRollingZero() {
        for (Integer v : rollingCounts) {
            if (v != null && v != 0) return false;
        }
        return true;
    }

    public synchronized void close() {
        releaseMats();
        clearRoiMask();
        slots.clear();
        rollingCounts.clear();
    }

    // ---------- Internals ----------

    private Result processInner(byte[] nv21, int width, int height) {
        Mat gray = nv21ToGray(nv21, width, height);
        Mat blurred = new Mat();
        try {
            Imgproc.GaussianBlur(gray, blurred, new Size(GAUSSIAN_KERNEL, GAUSSIAN_KERNEL), 0);
        } finally {
            gray.release();
        }

        // Calibration phase: build baseline, then slot grid on the following frame.
        if (baseline == null) {
            return stepCalibration(blurred);
        }
        if (gridPending) {
            try {
                return finishGridBuild();
            } finally {
                blurred.release();
            }
        }

        // Sanity: if camera resolution changed under us, baseline is stale. Force re-calibration.
        if (baseline.cols() != blurred.cols() || baseline.rows() != blurred.rows()) {
            Log.w(tag, "Tray " + trayId + ": frame size changed (" + blurred.cols() + "x" + blurred.rows()
                    + " vs baseline " + baseline.cols() + "x" + baseline.rows() + ") — re-calibrating");
            releaseMats();
            slots.clear();
            blurred.release();
            return calibratingResult(0);
        }

        // Detection phase.
        int instantCount = countOccupiedSlots(blurred);
        blurred.release();

        // Rolling smoother.
        rollingCounts.addLast(instantCount);
        while (rollingCounts.size() > STABILITY_WINDOW) rollingCounts.pollFirst();

        int smoothed = mode(rollingCounts);
        int agree = countOf(rollingCounts, smoothed);

        EventType event = EventType.NONE;
        int delta = 0;
        float pickX = -1f;
        float pickY = -1f;
        if (smoothed != lastEmittedCount && agree >= STABILITY_EMIT_AGREE) {
            int signed = smoothed - lastEmittedCount;
            delta = Math.abs(signed);
            event = signed > 0 ? EventType.DELIVERED : EventType.PICKED_UP;
            if (event == EventType.PICKED_UP && lastPickNormX >= 0f) {
                pickX = lastPickNormX;
                pickY = lastPickNormY;
                lastPickNormX = -1f;
                lastPickNormY = -1f;
            }
            lastEmittedCount = smoothed;
            lastEventType = event;
            lastEventDelta = delta;
            Log.i(tag, "Tray " + trayId + " event: " + event + " " + delta
                    + " (occupied " + smoothed + "/" + layout.slotCount() + ")");
        }

        // Self-heal: only refresh when the whole rolling window has been 0 (no brief flicker).
        long now = SystemClock.uptimeMillis();
        if (smoothed == 0 && rollingCounts.size() >= STABILITY_WINDOW && allRollingZero()) {
            if (emptySinceMs == 0L) emptySinceMs = now;
            else if (now - emptySinceMs > RESET_AFTER_EMPTY_MS) {
                Log.i(tag, "Tray " + trayId + ": auto-refreshing baseline after long stable empty");
                releaseMats();
                slots.clear();
                clearSlotState();
                emptySinceMs = 0L;
            }
        } else {
            emptySinceMs = 0L;
        }

        return new Result(false, -1, smoothed, layout.slotCount(),
                event, delta,
                formatOverlay(false, smoothed,
                        event != EventType.NONE ? event : lastEventType,
                        event != EventType.NONE ? delta : lastEventDelta),
                snapshotSlotBounds(), pickX, pickY);
    }

    private Result calibratingResult(int percentComplete) {
        int pct = Math.min(100, Math.max(0, percentComplete));
        return new Result(true, pct, 0, layout.slotCount(), EventType.NONE, 0,
                String.format(Locale.US,
                        "Tray %d · calibrating %d%% — keep tray empty", trayId, pct),
                cachedPreviewSlotBounds, -1f, -1f);
    }

    private int[] snapshotSlotBounds() {
        return rectsToBoundsArray(slots);
    }

    private static int[] rectsToBoundsArray(List<Rect> rects) {
        if (rects == null || rects.isEmpty()) return new int[0];
        int[] out = new int[rects.size() * 4];
        for (int i = 0; i < rects.size(); i++) {
            Rect r = rects.get(i);
            int o = i * 4;
            out[o] = r.x;
            out[o + 1] = r.y;
            out[o + 2] = r.width;
            out[o + 3] = r.height;
        }
        return out;
    }

    private void clearCalibAccum() {
        if (calibSumMat != null) {
            calibSumMat.release();
            calibSumMat = null;
        }
        calibFrameCount = 0;
    }

    private void releaseMats() {
        if (baseline != null) { baseline.release(); baseline = null; }
        gridPending = false;
        clearCalibAccum();
        clearSlotState();
    }

    /** NV21/YV12 planar size for given dimensions (stride = width, no pad). */
    private static int nv21StrideBytes(int width, int height) {
        return width * height * 3 / 2;
    }

    /** Last known calib % for degraded returns when frame is skipped. */
    private int calibFramePct() {
        if (calibFrameCount <= 0) return 0;
        return Math.min(99, (100 * calibFrameCount) / CALIB_ACCUM_FRAMES);
    }

    private void accumulateCalibFrame(Mat blurred8u) {
        Mat frame32 = new Mat();
        try {
            blurred8u.convertTo(frame32, CvType.CV_32F);
            int rows = frame32.rows();
            int cols = frame32.cols();
            if (calibSumMat == null) {
                calibSumMat = frame32.clone();
            } else if (calibSumMat.rows() != rows || calibSumMat.cols() != cols) {
                Log.w(tag, "Tray " + trayId + ": calibration frame size changed — restarting");
                calibSumMat.release();
                calibSumMat = frame32.clone();
                calibFrameCount = 0;
            } else {
                // Never use Core.add(dst, src, dst) — crashes native OpenCV on some robot builds.
                Mat summed = new Mat();
                try {
                    Core.add(calibSumMat, frame32, summed);
                } finally {
                    calibSumMat.release();
                    calibSumMat = summed;
                }
            }
        } finally {
            frame32.release();
        }
    }

    /**
     * Average {@link #CALIB_ACCUM_FRAMES} frames into baseline; grid build runs on the next frame.
     */
    private Result stepCalibration(Mat currentBlurred) {
        try {
            if (currentBlurred.rows() <= 0 || currentBlurred.cols() <= 0) {
                return calibratingResult(0);
            }
            accumulateCalibFrame(currentBlurred);
        } finally {
            currentBlurred.release();
        }

        calibFrameCount++;
        int pct = Math.min(99, (100 * calibFrameCount) / CALIB_ACCUM_FRAMES);
        if (calibFrameCount < CALIB_ACCUM_FRAMES) {
            return calibratingResult(pct);
        }

        int n = calibFrameCount;
        if (calibSumMat == null || calibSumMat.empty()) {
            Log.e(tag, "Tray " + trayId + ": calibSumMat empty at finish — restart calib");
            clearCalibAccum();
            return calibratingResult(0);
        }
        // Core.divide(Mat, Scalar, Mat) SIGSEGV on rk3288 OpenCV 4.4 — use convertTo scale instead.
        baseline = new Mat();
        calibSumMat.convertTo(baseline, CvType.CV_8U, 1.0 / n, 0);
        clearCalibAccum();

        lastEmittedCount = 0;
        rollingCounts.clear();
        gridPending = true;
        Log.i(tag, "Tray " + trayId + ": baseline from " + n + " frames — building grid next");
        return calibratingResult(99);
    }

    /** Second calibration step — even grid only (tape detect deferred; was crashing native HAL). */
    private Result finishGridBuild() {
        gridPending = false;
        if (baseline == null) {
            return calibratingResult(0);
        }
        int w = baseline.cols();
        int h = baseline.rows();
        syncRoiMaskToBaselineSize(w, h);
        try {
            slots = buildEvenGrid(w, h, layout.rows, layout.cols);
            gridFromTapeDetection = false;
        } catch (Throwable t) {
            Log.e(tag, "Tray " + trayId + ": finishGridBuild failed", t);
            slots = new ArrayList<>();
        }
        if (slots.isEmpty()) {
            slots = buildEvenGrid(w, h, layout.rows, layout.cols);
        }
        initSlotState(slots.size());
        cachedPreviewSlotBounds = snapshotSlotBounds();
        Log.i(tag, "Tray " + trayId + ": ready " + slots.size() + "/" + layout.slotCount() + " slots");
        return new Result(false, -1, 0, layout.slotCount(), EventType.NONE, 0,
                "Tray " + trayId + " · ready 0/" + layout.slotCount(),
                snapshotSlotBounds(), -1f, -1f);
    }

    /** Rescale ROI mask to match baseline dimensions without clearing baseline. */
    private void syncRoiMaskToBaselineSize(int frameW, int frameH) {
        if (roiMask == null || roiFrameW <= 0 || roiFrameH <= 0) return;
        if (roiMask.cols() == frameW && roiMask.rows() == frameH) return;
        float sx = (float) frameW / roiFrameW;
        float sy = (float) frameH / roiFrameH;
        int cx = Math.round(roiCx * sx);
        int cy = Math.round(roiCy * sy);
        int rx = Math.max(1, Math.round(roiRx * sx));
        int ry = Math.max(1, Math.round(roiRy * sy));
        roiMask.release();
        Mat mask = Mat.zeros(frameH, frameW, CvType.CV_8UC1);
        Imgproc.ellipse(mask, new Point(cx, cy), new Size(rx, ry),
                0.0, 0.0, 360.0, new Scalar(255), -1);
        roiMask = mask;
        roiFrameW = frameW;
        roiFrameH = frameH;
        roiCx = cx;
        roiCy = cy;
        roiRx = rx;
        roiRy = ry;
        refreshCachedPreviewSlotBounds();
        Log.i(tag, "Tray " + trayId + ": ROI synced to baseline " + frameW + "x" + frameH);
    }

    /**
     * For each slot rectangle: compute changed-pixel fraction inside the slot vs baseline,
     * restricted to the ROI mask if one is set. Slot is occupied when the fraction exceeds
     * {@link #OCCUPIED_FRACTION_THRESHOLD}. With an oval ROI, a slot near the tray edge has
     * a smaller effective area (only the in-mask portion counts) so its threshold is
     * automatically scaled.
     */
    private int countOccupiedSlots(Mat blurred) {
        if (slots.isEmpty()) return 0;
        if (slotStableOccupied == null || slotStableOccupied.length != slots.size()) {
            initSlotState(slots.size());
        }
        Mat diff = new Mat();
        Mat mask = new Mat();
        Mat morphKernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(3, 3));
        try {
            Core.absdiff(blurred, baseline, diff);
            Imgproc.threshold(diff, mask, OCCUPIED_PIXEL_DELTA, 255, Imgproc.THRESH_BINARY);
            if (roiMask != null && roiMask.cols() == mask.cols() && roiMask.rows() == mask.rows()) {
                Core.bitwise_and(mask, roiMask, mask);
            }
            // Remove single-pixel speckle before measuring each slot.
            Imgproc.morphologyEx(mask, mask, Imgproc.MORPH_OPEN, morphKernel);

            int W = blurred.cols();
            int H = blurred.rows();
            boolean[] wasOccupied = java.util.Arrays.copyOf(slotStableOccupied, slotStableOccupied.length);
            int occupied = 0;
            for (int i = 0; i < slots.size(); i++) {
                Rect slot = slots.get(i);
                Mat slotMask = mask.submat(slot);
                int changed = Core.countNonZero(slotMask);
                double area;
                if (roiMask != null) {
                    Mat slotRoi = roiMask.submat(slot);
                    area = Core.countNonZero(slotRoi);
                } else {
                    area = (double) slot.width * (double) slot.height;
                }
                if (area <= 0) continue;

                double frac = changed / area;
                if (slotStableOccupied[i]) {
                    if (frac < CLEAR_FRACTION_THRESHOLD) {
                        slotClearRun[i]++;
                        slotOccupyRun[i] = 0;
                        if (slotClearRun[i] >= SLOT_CLEAR_CONFIRM_FRAMES) {
                            slotStableOccupied[i] = false;
                            slotClearRun[i] = 0;
                            if (wasOccupied[i]) {
                                lastPickNormX = (slot.x + slot.width * 0.5f) / W;
                                lastPickNormY = (slot.y + slot.height * 0.5f) / H;
                            }
                        }
                    } else {
                        slotClearRun[i] = 0;
                    }
                } else {
                    if (frac >= OCCUPIED_FRACTION_THRESHOLD) {
                        slotOccupyRun[i]++;
                        slotClearRun[i] = 0;
                        if (slotOccupyRun[i] >= SLOT_OCCUPY_CONFIRM_FRAMES) {
                            slotStableOccupied[i] = true;
                            slotOccupyRun[i] = 0;
                        }
                    } else {
                        slotOccupyRun[i] = 0;
                    }
                }
                if (slotStableOccupied[i]) occupied++;
            }
            return occupied;
        } finally {
            diff.release();
            mask.release();
            morphKernel.release();
        }
    }

    // ---------- Grid detection ----------

    /**
     * Build the list of slot rectangles. Attempts to find the white tape lines via
     * row/column brightness projections; if it can't find enough peaks, falls back to
     * an evenly-spaced grid spanning the central portion of the frame.
     */
    private List<Rect> buildSlotGrid(Mat baseline, BoxLayout layout) {
        int W = baseline.cols();
        int H = baseline.rows();
        int rows = layout.rows;
        int cols = layout.cols;

        // 2- and 4-slot trays only need one divider line each way; the tray texture
        // (zig-zag, etc.) produces false tape peaks that land off-centre (thin side boxes).
        // Always use a symmetric even grid inside the user-drawn ROI for these layouts.
        if (!shouldUseTapeGrid(layout)) {
            gridFromTapeDetection = false;
            Log.i(tag, "Tray " + trayId + ": " + layout.slotCount()
                    + " slots — using even grid inside ROI (tape detect skipped)");
            return buildEvenGrid(W, H, rows, cols);
        }

        // Step 1: white-pixel mask, restricted to the oval ROI when set.
        Mat whiteMask = new Mat();
        Imgproc.threshold(baseline, whiteMask, WHITE_TAPE_THRESHOLD, 255, Imgproc.THRESH_BINARY);
        if (roiMask != null && roiMask.cols() == whiteMask.cols() && roiMask.rows() == whiteMask.rows()) {
            Core.bitwise_and(whiteMask, roiMask, whiteMask);
        }

        // Step 2: row + column projections
        int[] rowProj = projectionSum(whiteMask, /*alongRows=*/ true);
        int[] colProj = projectionSum(whiteMask, /*alongRows=*/ false);
        whiteMask.release();

        // Step 3: find top (rows-1) horizontal lines and (cols-1) vertical lines.
        int minSeparationH = Math.max(8, H / (rows + 2));
        int minSeparationV = Math.max(8, W / (cols + 2));
        int hPeakMin = Math.max(1, W / 4);
        int vPeakMin = Math.max(1, H / 4);

        int[] hLines = findPeaks(rowProj, rows - 1, minSeparationH, hPeakMin);
        int[] vLines = findPeaks(colProj, cols - 1, minSeparationV, vPeakMin);

        boolean hOk = hLines.length == rows - 1;
        boolean vOk = vLines.length == cols - 1;
        gridFromTapeDetection = hOk && vOk;

        if (!gridFromTapeDetection) {
            Log.i(tag, "Tray " + trayId + ": tape detection incomplete (h=" + hLines.length
                    + "/" + (rows - 1) + ", v=" + vLines.length + "/" + (cols - 1)
                    + ") — using fallback even grid");
            return buildEvenGrid(W, H, rows, cols);
        }

        Arrays.sort(hLines);
        Arrays.sort(vLines);

        if (!validateTapeLines(hLines, vLines, W, H)) {
            gridFromTapeDetection = false;
            Log.i(tag, "Tray " + trayId + ": tape lines failed ROI sanity check — even grid");
            return buildEvenGrid(W, H, rows, cols);
        }

        // Step 4: boundaries — use ROI bbox when available, not full-frame margins.
        int[] ys = new int[rows + 1];
        int[] xs = new int[cols + 1];
        gridOuterBounds(W, H, xs, ys, rows, cols);
        for (int i = 0; i < hLines.length; i++) ys[i + 1] = hLines[i];
        for (int i = 0; i < vLines.length; i++) xs[i + 1] = vLines[i];

        return rectsFromBoundaries(xs, ys, W, H);
    }

    /**
     * Tape peak picking is unreliable when there are only one or two divider lines (2 or 4 slots).
     * 6+ slots have enough real tape lines that projection peaks work on physical trays.
     */
    private static boolean shouldUseTapeGrid(BoxLayout layout) {
        return layout.slotCount() > 4;
    }

    /** Outer grid edges: ROI axis-aligned bbox when set, else 4 % frame inset. */
    private void gridOuterBounds(int W, int H, int[] xs, int[] ys, int rows, int cols) {
        if (roiCx >= 0 && roiRx > 0 && roiRy > 0) {
            int x0 = Math.max(0, roiCx - roiRx);
            int y0 = Math.max(0, roiCy - roiRy);
            int x1 = Math.min(W, roiCx + roiRx);
            int y1 = Math.min(H, roiCy + roiRy);
            xs[0] = x0;
            xs[cols] = x1;
            ys[0] = y0;
            ys[rows] = y1;
        } else {
            int outerMargin = Math.min(W, H) / 25;
            xs[0] = outerMargin;
            xs[cols] = W - outerMargin;
            ys[0] = outerMargin;
            ys[rows] = H - outerMargin;
        }
    }

    /**
     * Reject tape peaks that sit far from the ROI centre (common with textured trays).
     */
    private boolean validateTapeLines(int[] hLines, int[] vLines, int W, int H) {
        if (roiCx < 0 || roiRx <= 0 || roiRy <= 0) return true;
        int xLo = roiCx - roiRx / 2;
        int xHi = roiCx + roiRx / 2;
        int yLo = roiCy - roiRy / 2;
        int yHi = roiCy + roiRy / 2;
        for (int x : vLines) {
            if (x < xLo || x > xHi) return false;
        }
        for (int y : hLines) {
            if (y < yLo || y > yHi) return false;
        }
        return true;
    }

    /**
     * Even N×M grid with a 4 % outer margin. Used as a fallback. If a ROI ellipse is set,
     * the grid spans the ROI's bounding box instead of the whole frame, so slots actually
     * fall inside the oval.
     */
    private List<Rect> buildEvenGrid(int W, int H, int rows, int cols) {
        int x0, y0, gridW, gridH;
        if (roiCx >= 0 && roiRx > 0 && roiRy > 0) {
            // Inscribed-rectangle of the ellipse: width = rx*sqrt(2), height = ry*sqrt(2).
            // We use the simpler axis-aligned bounding box (2*rx by 2*ry) for the fallback —
            // slot rectangles at the corners may extend slightly past the oval, but the per-slot
            // ROI mask in countOccupiedSlots() handles that correctly.
            x0 = Math.max(0, roiCx - roiRx);
            y0 = Math.max(0, roiCy - roiRy);
            gridW = Math.min(W - x0, 2 * roiRx);
            gridH = Math.min(H - y0, 2 * roiRy);
        } else {
            int outerMargin = Math.min(W, H) / 25;
            x0 = outerMargin; y0 = outerMargin;
            gridW = W - 2 * outerMargin;
            gridH = H - 2 * outerMargin;
        }
        int[] xs = new int[cols + 1];
        int[] ys = new int[rows + 1];
        for (int i = 0; i <= cols; i++) xs[i] = x0 + Math.round((float) i * gridW / cols);
        for (int i = 0; i <= rows; i++) ys[i] = y0 + Math.round((float) i * gridH / rows);
        return rectsFromBoundaries(xs, ys, W, H);
    }

    /** Build rows×cols rectangles from a pair of boundary arrays. Applies SLOT_INNER_MARGIN_PX. */
    private static List<Rect> rectsFromBoundaries(int[] xs, int[] ys, int W, int H) {
        List<Rect> out = new ArrayList<>();
        int m = SLOT_INNER_MARGIN_PX;
        for (int r = 0; r < ys.length - 1; r++) {
            for (int c = 0; c < xs.length - 1; c++) {
                int x = Math.max(0, xs[c] + m);
                int y = Math.max(0, ys[r] + m);
                int w = Math.min(W - x, xs[c + 1] - xs[c] - 2 * m);
                int h = Math.min(H - y, ys[r + 1] - ys[r] - 2 * m);
                if (w > 0 && h > 0) out.add(new Rect(x, y, w, h));
            }
        }
        return out;
    }

    /**
     * Sum a uint8 mask along rows or columns into an int[] projection.
     * @param alongRows true → returns int[H] (sum across cols per row); false → returns int[W] (sum across rows per col).
     */
    private static int[] projectionSum(Mat mask, boolean alongRows) {
        Mat dst = new Mat();
        try {
            int dim = alongRows ? 1 : 0;   // REDUCE: dim=1 means reduce across cols, output is column-vector of H rows
            Core.reduce(mask, dst, dim, Core.REDUCE_SUM, CvType.CV_32S);
            int n = alongRows ? mask.rows() : mask.cols();
            int[] out = new int[n];
            // dst is 1×N or N×1 of CV_32S; flatten safely
            int[] tmp = new int[n];
            if (alongRows) {
                // dst shape: rows × 1
                int[] cell = new int[1];
                for (int r = 0; r < n; r++) {
                    dst.get(r, 0, cell);
                    tmp[r] = cell[0];
                }
            } else {
                // dst shape: 1 × cols
                int[] cell = new int[1];
                for (int c = 0; c < n; c++) {
                    dst.get(0, c, cell);
                    tmp[c] = cell[0];
                }
            }
            // Light smoothing (3-tap box) to merge wide tape lines into a single peak.
            for (int i = 0; i < n; i++) {
                int sum = tmp[i];
                int cnt = 1;
                if (i > 0) { sum += tmp[i - 1]; cnt++; }
                if (i < n - 1) { sum += tmp[i + 1]; cnt++; }
                out[i] = sum / cnt;
            }
            return out;
        } finally {
            dst.release();
        }
    }

    /**
     * Find up to {@code wantedCount} highest peaks separated by at least {@code minSeparation}.
     * Each peak must be ≥ {@code minPeakValue}. Returns the peak indices (not necessarily sorted).
     */
    private static int[] findPeaks(int[] arr, int wantedCount, int minSeparation, int minPeakValue) {
        if (wantedCount <= 0 || arr == null || arr.length == 0) return new int[0];
        List<Integer> picked = new ArrayList<>();
        // We work on a copy so we can suppress neighbours of each picked peak.
        int[] work = Arrays.copyOf(arr, arr.length);
        for (int k = 0; k < wantedCount; k++) {
            int bestIdx = -1;
            int bestVal = -1;
            for (int i = 0; i < work.length; i++) {
                if (work[i] > bestVal) { bestVal = work[i]; bestIdx = i; }
            }
            if (bestIdx < 0 || bestVal < minPeakValue) break;
            picked.add(bestIdx);
            int lo = Math.max(0, bestIdx - minSeparation);
            int hi = Math.min(work.length - 1, bestIdx + minSeparation);
            for (int i = lo; i <= hi; i++) work[i] = Integer.MIN_VALUE;
        }
        int[] out = new int[picked.size()];
        for (int i = 0; i < picked.size(); i++) out[i] = picked.get(i);
        return out;
    }

    // ---------- Frame conversion + formatting ----------

    private Mat nv21ToGray(byte[] nv21, int width, int height) {
        Mat yuv = new Mat(height + height / 2, width, CvType.CV_8UC1);
        yuv.put(0, 0, nv21);
        Mat gray = new Mat();
        Imgproc.cvtColor(yuv, gray, Imgproc.COLOR_YUV2GRAY_NV21);
        yuv.release();
        return gray;
    }

    private String formatOverlay(boolean calibrating, int count, EventType lastEvt, int lastDelta) {
        if (calibrating) {
            return String.format(Locale.US,
                    "Tray %d · calibrating — keep tray empty", trayId);
        }
        int max = layout.slotCount();
        if (lastEvt == EventType.NONE || lastDelta == 0) {
            return String.format(Locale.US, "Tray %d · %d/%d occupied", trayId, count, max);
        }
        String verb = lastEvt == EventType.DELIVERED ? "Delivered" : "Picked up";
        return String.format(Locale.US, "Tray %d · %d/%d · last: %s %d",
                trayId, count, max, verb, lastDelta);
    }

    // ---------- Tiny utilities (API 23 safe — no Map.getOrDefault) ----------

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
