package com.keenon.peanut.sample.util;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * User-selectable voice preferences. The "force offline" flag tells {@link VoiceBackendHelper}
 * to skip the LAN Java backend and prefer on-device speech (Vosk in {@code VoiceFragment}, or
 * {@code EXTRA_PREFER_OFFLINE} in {@code PromoVoiceFragment}).
 */
public final class VoicePreferences {

    private static final String PREFS_NAME = "voice_prefs";
    private static final String KEY_FORCE_OFFLINE = "force_offline";

    private VoicePreferences() {}

    public static boolean isForceOffline(Context context) {
        if (context == null) return false;
        SharedPreferences sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return sp.getBoolean(KEY_FORCE_OFFLINE, false);
    }

    public static void setForceOffline(Context context, boolean forceOffline) {
        if (context == null) return;
        SharedPreferences sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        sp.edit().putBoolean(KEY_FORCE_OFFLINE, forceOffline).apply();
    }
}
