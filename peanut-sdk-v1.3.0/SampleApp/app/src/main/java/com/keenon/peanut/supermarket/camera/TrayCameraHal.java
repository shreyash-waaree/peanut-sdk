package com.keenon.peanut.supermarket.camera;

import android.os.Build;

/**
 * Detects Keenon T10 / RK3288 boards whose Camera1 HAL crashes mediaserver when preview
 * frame callbacks are enabled (see {@code camera.rk30board.so setPreviewDataCbRes} SIGSEGV).
 */
public final class TrayCameraHal {

    private TrayCameraHal() {}

    /**
     * @return true on RK3288 RockChip builds where {@code setPreviewCallback*} kills mediaserver
     */
    public static boolean isFragileRockChipHal() {
        String fp = Build.FINGERPRINT != null ? Build.FINGERPRINT.toLowerCase() : "";
        String hw = Build.HARDWARE != null ? Build.HARDWARE.toLowerCase() : "";
        String board = Build.BOARD != null ? Build.BOARD.toLowerCase() : "";
        return fp.contains("rk3288") || fp.contains("rk30board") || hw.contains("rk30")
                || board.contains("rk3288");
    }
}
