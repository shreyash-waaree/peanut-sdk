package com.keenon.common.utils;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/common/utils/MathUtils.class */
public class MathUtils {
    public static int onCalculatePercent(long fileSize, int received) {
        if (fileSize == 0) {
            return 100;
        }
        int percentValue = Math.round(((long) (received * 100)) / fileSize);
        return Math.min(percentValue, 100);
    }
}
