package com.keenon.common.utils;

import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/common/utils/MessageIdUtil.class */
public class MessageIdUtil {
    private static final int MAX = 1073741824;
    private static AtomicInteger mAtomicInteger = new AtomicInteger();

    public static int getMessAgeId() {
        mAtomicInteger.getAndIncrement();
        if (mAtomicInteger.get() >= MAX) {
            mAtomicInteger = new AtomicInteger(1);
        }
        return mAtomicInteger.get();
    }

    public static void init() {
        Random random = new Random();
        int i = random.nextInt(536870912);
        mAtomicInteger.set(i);
    }
}
