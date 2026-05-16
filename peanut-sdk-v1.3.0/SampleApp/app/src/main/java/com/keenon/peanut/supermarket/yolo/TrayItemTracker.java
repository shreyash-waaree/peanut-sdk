package com.keenon.peanut.supermarket.yolo;

import java.util.List;

/**
 * Tracks per-frame plate counts and fires {@link Listener#onItemPicked(int, int)} when it
 * confirms that someone has taken an item from the tray.
 *
 * Detection is debounced: the count must drop and stay lower for
 * {@link #CONFIRM_FRAMES} consecutive frames before the event fires. This avoids spurious
 * triggers from a single missed detection.
 *
 * After firing, the tracker enters a cooldown period ({@link #COOLDOWN_MS}) so repeated
 * detections for the same pick event do not trigger multiple feedback windows.
 */
public class TrayItemTracker {

    public interface Listener {
        /**
         * Called on the detect thread when an item pick is confirmed.
         *
         * @param previousCount plate count before the pick
         * @param newCount      plate count after the pick (may be 0)
         */
        void onItemPicked(int previousCount, int newCount);
    }

    // 1 frame = immediate response once the model sees the drop; cooldown reduced so rapid
    // successive picks each trigger the feedback loop.
    private static final int CONFIRM_FRAMES = 1;
    private static final long COOLDOWN_MS = 2_000;

    private final Listener listener;

    private int confirmedCount = -1;       // last stable plate count (-1 = not yet established)
    private int pendingCount   = -1;       // candidate new (lower) count being debounced
    private int pendingFrames  = 0;        // how many consecutive frames showed pendingCount
    private long lastFiredAt   = 0;        // System.currentTimeMillis() of last onItemPicked call

    public TrayItemTracker(Listener listener) {
        this.listener = listener;
    }

    /**
     * Feed the latest detection results for one frame.
     * Count only "plate" detections (class label == "plate"); ignore "ros_empty".
     * Must be called from the same thread for every frame (the detect thread).
     */
    public void onFrame(List<YoloDetectorHelper.Result> results) {
        int plateCount = 0;
        if (results != null) {
            for (YoloDetectorHelper.Result r : results) {
                if ("plate".equals(r.label)) plateCount++;
            }
        }

        if (confirmedCount < 0) {
            // Not yet established a baseline — just record the first stable reading
            confirmedCount = plateCount;
            pendingCount   = -1;
            pendingFrames  = 0;
            return;
        }

        if (plateCount >= confirmedCount) {
            // Count stayed same or went up (e.g., item returned, or detection noise upward)
            // Update baseline upward and reset any in-progress debounce
            confirmedCount = plateCount;
            pendingCount   = -1;
            pendingFrames  = 0;
            return;
        }

        // Count dropped — start or continue debounce
        if (plateCount == pendingCount) {
            pendingFrames++;
        } else {
            // New candidate lower count
            pendingCount  = plateCount;
            pendingFrames = 1;
        }

        if (pendingFrames >= CONFIRM_FRAMES) {
            long now = System.currentTimeMillis();
            if (now - lastFiredAt >= COOLDOWN_MS) {
                int previous = confirmedCount;
                confirmedCount = pendingCount;
                pendingCount   = -1;
                pendingFrames  = 0;
                lastFiredAt    = now;
                if (listener != null) listener.onItemPicked(previous, confirmedCount);
            } else {
                // Still in cooldown — accept the new count silently
                confirmedCount = pendingCount;
                pendingCount   = -1;
                pendingFrames  = 0;
            }
        }
    }

    /** Reset all state (call when camera is stopped/switched). */
    public void reset() {
        confirmedCount = -1;
        pendingCount   = -1;
        pendingFrames  = 0;
    }

    public int getConfirmedCount() {
        return confirmedCount;
    }
}
