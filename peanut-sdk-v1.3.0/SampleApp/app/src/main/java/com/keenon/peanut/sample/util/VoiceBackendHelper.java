package com.keenon.peanut.sample.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Build;

import com.keenon.peanut.sample.BackendConfig;

import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Shared routing for LAN Java backend speech ({@code /speech-transcribe}) vs other STT paths.
 */
public final class VoiceBackendHelper {

    private VoiceBackendHelper() {}

    public static boolean shouldUseJavaBackendStt(Context context) {
        if (VoicePreferences.isForceOffline(context)) {
            return false;
        }
        return isNetworkAvailable(context) && BackendConfig.hasBaseUrl(context);
    }

    public static boolean isNetworkAvailable(Context context) {
        if (context == null) {
            return false;
        }
        ConnectivityManager cm =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) {
            return false;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Network active = cm.getActiveNetwork();
            if (active == null) {
                return false;
            }
            NetworkCapabilities caps = cm.getNetworkCapabilities(active);
            return caps != null && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
        }
        NetworkInfo ni = cm.getActiveNetworkInfo();
        return ni != null && ni.isConnected();
    }

    /**
     * Fast (~2s) reachability probe. Hits the configured backend base URL with HEAD and treats any
     * HTTP response (even 404) as "server is up". Returns false on connect refused / unreachable
     * within timeout so the caller can fail fast instead of waiting for a long upload timeout.
     *
     * <p>MUST be called off the main thread (blocks on network I/O).
     */
    public static boolean isBackendReachable(Context context) {
        if (context == null || !BackendConfig.hasBaseUrl(context)) {
            return false;
        }
        String base = BackendConfig.getBaseUrl(context);
        if (base == null || base.isEmpty()) {
            return false;
        }
        HttpURLConnection conn = null;
        try {
            URL url = new URL(base + "/health");
            conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(1500);
            conn.setReadTimeout(1500);
            conn.setRequestMethod("HEAD");
            conn.setUseCaches(false);
            int code = conn.getResponseCode();
            // Any response (2xx/3xx/4xx/5xx) means the TCP socket opened -> server is reachable.
            return code > 0;
        } catch (Exception e) {
            return false;
        } finally {
            if (conn != null) {
                try { conn.disconnect(); } catch (Exception ignored) {}
            }
        }
    }
}
