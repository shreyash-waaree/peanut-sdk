package com.keenon.common.utils;

import com.keenon.common.constant.PeanutConstants;
import java.util.Collection;
import java.util.Map;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/common/utils/CheckUtils.class */
public abstract class CheckUtils {
    private static final String TAG = "[CheckUtils]";
    private static final int MIN_CLICK_DELAY_TIME = 500;
    private static long lastClickTime;

    CheckUtils() {
    }

    public static final boolean isNull(Object object) {
        return object == null;
    }

    public static final void assertNotNull(Object object, String message) {
        if (isNull(object)) {
            throw new CheckException(message);
        }
    }

    public static <T> T checkNotNull(T object, String message) {
        if (isNull(object)) {
            LogUtils.e(PeanutConstants.TAG_UTIL, "[CheckUtils][checkNotNull][" + message + "]");
        }
        return object;
    }

    public static final void assertTrue(boolean flag, String message) {
        assertTrue(flag, message, (Long) null);
    }

    public static final void assertTrue(boolean flag, String message, Long code) {
        if (!flag) {
            throw new CheckException(message);
        }
    }

    public static final boolean isEmpty(Collection collection) {
        return collection == null || collection.size() <= 0;
    }

    public static final boolean isEmpty(Map map) {
        return map == null || map.size() <= 0;
    }

    public static final boolean isNotEmpty(Collection collection) {
        return !isEmpty(collection);
    }

    public static final void assertNotEmpty(Collection collection, String message) {
        if (isEmpty(collection)) {
            throw new CheckException(message);
        }
    }

    public static final void assertEmpty(Collection collection, String message) {
        if (!isEmpty(collection)) {
            throw new CheckException(message);
        }
    }

    public static boolean isFastSpark() {
        boolean flag = false;
        long curClickTime = System.currentTimeMillis();
        if (curClickTime - lastClickTime >= 500) {
            flag = true;
        }
        lastClickTime = curClickTime;
        return flag;
    }
}
