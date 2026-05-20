package com.keenon.peanut.supermarket.vision;

import androidx.annotation.Nullable;

import java.util.Locale;

/**
 * Polar angle (0–360°) from tray oval center toward the hand at pickup time.
 *
 * <p>0° = right (3 o'clock), 90° = down (6 o'clock, image Y+), 180° = left, 270° = up.</p>
 */
public final class TrayPickApproachAngle {

    public static final String LOG_TAG = "Count4PickAngle";

    public enum HandSource {
        /** NCNN hand_detect model (preferred). */
        DETECTOR,
        /** Cleared slot center from OpenCV grid (fallback). */
        SLOT
    }

    public static final class Reading {
        public final float degrees;
        public final float handNormX;
        public final float handNormY;
        public final float trayNormX;
        public final float trayNormY;
        public final HandSource source;

        Reading(float degrees, float handNormX, float handNormY,
                float trayNormX, float trayNormY, HandSource source) {
            this.degrees = degrees;
            this.handNormX = handNormX;
            this.handNormY = handNormY;
            this.trayNormX = trayNormX;
            this.trayNormY = trayNormY;
            this.source = source;
        }
    }

    private TrayPickApproachAngle() {}

    /**
     * @param trayCx tray center X in camera-frame pixels
     * @param trayCy tray center Y in camera-frame pixels
     * @param frameW camera frame width
     * @param frameH camera frame height
     * @param handNormX hand X in 0..1, or &lt; 0 if unknown
     * @param handNormY hand Y in 0..1, or &lt; 0 if unknown
     * @param slotNormX fallback slot X from pick event
     * @param slotNormY fallback slot Y from pick event
     */
    @Nullable
    public static Reading compute(
            int trayCx, int trayCy, int frameW, int frameH,
            float handNormX, float handNormY,
            float slotNormX, float slotNormY) {
        if (frameW <= 0 || frameH <= 0) return null;
        float tcx = trayCx / (float) frameW;
        float tcy = trayCy / (float) frameH;

        float hx;
        float hy;
        HandSource source;
        if (handNormX >= 0f && handNormY >= 0f) {
            hx = handNormX;
            hy = handNormY;
            source = HandSource.DETECTOR;
        } else if (slotNormX >= 0f && slotNormY >= 0f) {
            hx = slotNormX;
            hy = slotNormY;
            source = HandSource.SLOT;
        } else if (handNormX >= 0f) {
            hx = handNormX;
            hy = tcy;
            source = HandSource.DETECTOR;
        } else if (slotNormX >= 0f) {
            hx = slotNormX;
            hy = slotNormY >= 0f ? slotNormY : tcy;
            source = HandSource.SLOT;
        } else {
            return null;
        }

        float dx = hx - tcx;
        float dy = hy - tcy;
        double rad = Math.atan2(dy, dx);
        float deg = (float) Math.toDegrees(rad);
        if (deg < 0f) deg += 360f;
        return new Reading(deg, hx, hy, tcx, tcy, source);
    }

    public static String formatLogLine(
            int cameraId,
            int prevCount,
            int newCount,
            Reading reading) {
        return String.format(
                Locale.US,
                "PICK cam=%d count=%d->%d approach=%.1f° "
                        + "hand=(%.3f,%.3f) trayCenter=(%.3f,%.3f) source=%s",
                cameraId,
                prevCount,
                newCount,
                reading.degrees,
                reading.handNormX,
                reading.handNormY,
                reading.trayNormX,
                reading.trayNormY,
                reading.source.name());
    }
}
