package com.keenon.common.utils;

import android.content.Context;
import android.content.pm.PackageInfo;
import com.keenon.common.constant.PeanutConstants;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/common/utils/PackageUtils.class */
public class PackageUtils {
    private static final String TAG = "[PackageUtils]";

    public static String getVersionName(Context context) {
        try {
            PackageInfo info = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return info.versionName;
        } catch (Exception e) {
            LogUtils.e(PeanutConstants.TAG_UTIL, TAG, e);
            return "";
        }
    }

    public static int getVersionCode(Context context) {
        try {
            PackageInfo info = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return info.versionCode;
        } catch (Exception e) {
            LogUtils.e(PeanutConstants.TAG_UTIL, TAG, e);
            return -1;
        }
    }

    public static String getPackName(Context context) {
        try {
            return context.getPackageName();
        } catch (Exception e) {
            LogUtils.e(PeanutConstants.TAG_UTIL, TAG, e);
            return "";
        }
    }
}
