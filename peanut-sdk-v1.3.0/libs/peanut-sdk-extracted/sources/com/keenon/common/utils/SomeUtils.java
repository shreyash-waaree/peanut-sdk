package com.keenon.common.utils;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/common/utils/SomeUtils.class */
public class SomeUtils {
    public static void threadSleep(long times) {
        try {
            Thread.sleep(times);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
