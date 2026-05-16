package com.keenon.peanut.sample.util;

import android.content.Context;
import android.os.Build;
import android.util.Log;
import com.keenon.sdk.external.PeanutSDK;
import com.keenon.common.constant.PeanutConstants;
import com.keenon.common.external.PeanutConfig;

/**
 * Proxy class to extend SDK support for different device types and architectures.
 */
public class DeviceProxy {
    private static final String TAG = "DeviceProxy";
    private static boolean isMockMode = false;

    public static void init(Context context, PeanutSDK.ErrorListener listener) {
        // Check if we are running on a non-robot device (e.g. x86 emulator or generic phone)
        if (isGenericDevice()) {
            Log.w(TAG, "Running on generic device. Enabling Mock Mode for compatibility.");
            isMockMode = true;
            listener.onInit(PeanutSDK.SDK_INIT_SUCCESS);
            return;
        }

        try {
            // Standard SDK Initialization
            PeanutSDK.getInstance().init(context, listener);
        } catch (UnsatisfiedLinkError e) {
            Log.e(TAG, "Native library error: Likely 32-bit SDK on 64-bit/x86 device. Switching to Mock.");
            isMockMode = true;
            listener.onInit(PeanutSDK.SDK_INIT_SUCCESS);
        }
    }

    private static boolean isGenericDevice() {
        String abi = Build.CPU_ABI;
        // If it's x86 or doesn't have robot-specific hardware markers
        return abi.contains("x86") || Build.MODEL.contains("Emulator") || Build.BRAND.contains("generic");
    }

    public static boolean isMockMode() {
        return isMockMode;
    }
}
