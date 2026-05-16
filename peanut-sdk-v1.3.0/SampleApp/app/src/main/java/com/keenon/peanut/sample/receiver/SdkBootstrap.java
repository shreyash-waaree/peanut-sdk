package com.keenon.peanut.sample.receiver;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.keenon.common.constant.PeanutConstants;
import com.keenon.common.external.PeanutConfig;
import com.keenon.sdk.external.PeanutSDK;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Minimal, process-safe SDK bootstrap so core services can run
 * even when no UI activity is visible.
 *
 * Uses the same config fields as KeenonApiDemoMain.initSDK(...).
 */
public final class SdkBootstrap {
    private static final String LOG_INFO = "INFO";
    private static final String LOG_ERROR = "ERROR";
    private static final AtomicBoolean sInitRequested = new AtomicBoolean(false);

    private SdkBootstrap() {}

    public static void ensureInit(Context context, PeanutSDK.ErrorListener errorListener) {
        if (context == null) {
            return;
        }
        if (!sInitRequested.compareAndSet(false, true)) {
            return;
        }
        try {
            String type = readLinkType(context);
            PeanutConfig.getConfig()
                    .setLinkType(PeanutConstants.REMOTE_LINK_PROXY.equals(type)
                            ? PeanutConstants.LinkType.COAP
                            : PeanutConstants.LinkType.COM_COAP)
                    .setLinkIP(type)
                    .enableLog(true)
                    .setLogLevel(Log.DEBUG)
                    .setAppId("bcb8ebc7f22345bebb378aead035cfb3")
                    .setSecret("nPlQERTP4qJWimTp0+ZXXkM5ND93iEyWpM6eXAGIZ/HQmyEg8zN7x5tGLebwINKLYScXEjg5lhQBvt1QCODovm2gq7dsXAK4pgjBRK2OqQHxl4nvTjq2AX9Or6XrdfFfVgOiHqW0mw+qWGDJc1/EUBg3llLOzMNUiDqwPsXMZYs=")
                    .enableUMLog(false);

            Log.i(LOG_INFO, "PeanutSDK init requested (service)");
            PeanutSDK.getInstance().init(context.getApplicationContext(), errorListener);
        } catch (Throwable t) {
            Log.e(LOG_ERROR, "PeanutSDK init failed in service bootstrap", t);
        }
    }

    private static String readLinkType(Context context) {
        try {
            SharedPreferences sp = context.getSharedPreferences("SP", Context.MODE_PRIVATE);
            return sp.getString("type", PeanutConstants.REMOTE_LINK_PROXY);
        } catch (Exception e) {
            return PeanutConstants.REMOTE_LINK_PROXY;
        }
    }
}

