package com.keenon.peanut.sample;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Locale;

public final class BackendConfig {
    private BackendConfig() {}

    /**
     * Set to your backend PC LAN IP (same network as robot/tablet).
     * Example: http://192.168.1.50:8080
     */
    public static final String DEFAULT_BASE_URL = "http://172.20.10.3:8080";
    private static final String PREFS_NAME = "backend_config";
    private static final String KEY_BASE_URL = "base_url";

    /** Returns persisted runtime backend URL, or {@link #DEFAULT_BASE_URL} if not set. */
    public static String getBaseUrl(Context context) {
        SharedPreferences sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String saved = sp.getString(KEY_BASE_URL, "");
        String normalized = normalizeBaseUrl(saved);
        return normalized != null ? normalized : DEFAULT_BASE_URL;
    }

    /** Saves runtime backend URL entered by user. */
    public static boolean setBaseUrl(Context context, String backendInput) {
        String normalized = normalizeBaseUrl(backendInput);
        if (normalized == null) {
            return false;
        }
        SharedPreferences sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        sp.edit().putString(KEY_BASE_URL, normalized).apply();
        return true;
    }

    public static String getFormSubmitUrl(Context context) {
        return getBaseUrl(context) + "/form-submit";
    }

    public static String getSpeechTranscribeUrl(Context context) {
        return getBaseUrl(context) + "/speech-transcribe";
    }

    public static boolean hasBaseUrl(Context context) {
        return getBaseUrl(context) != null && !getBaseUrl(context).trim().isEmpty();
    }

    public static String normalizeBaseUrl(String backendInput) {
        String base = backendInput == null ? "" : backendInput.trim();
        if (base.isEmpty()) {
            return null;
        }
        String lower = base.toLowerCase(Locale.US);
        if (!lower.startsWith("http://") && !lower.startsWith("https://")) {
            base = "http://" + base;
        }
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        if (base.endsWith("/form-submit")) {
            base = base.substring(0, base.length() - "/form-submit".length());
        } else if (base.endsWith("/speech-transcribe")) {
            base = base.substring(0, base.length() - "/speech-transcribe".length());
        } else if (base.endsWith("/health")) {
            base = base.substring(0, base.length() - "/health".length());
        }
        return base;
    }
}
