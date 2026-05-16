package com.keenon.peanut.supermarket.backend;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.keenon.peanut.supermarket.model.FeedbackPayload;
import com.keenon.peanut.supermarket.model.InterestPayload;

import org.json.JSONException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PromoSubmitter {
  private static final String TAG = "PromoSubmitter";
  private static final int CONNECT_TIMEOUT = 5000;
  private static final int READ_TIMEOUT = 5000;
  private static final long RETRY_INTERVAL_MS = 300_000;

  public interface Callback {
    void call();
  }

  public interface ErrorCallback {
    void call(String message);
  }

  private final OfflineCache offlineCache;
  private final ExecutorService executor = Executors.newSingleThreadExecutor();
  private final Handler mainHandler = new Handler(Looper.getMainLooper());
  private final Handler retryHandler = new Handler(Looper.getMainLooper());
  private final Context appContext;

  public PromoSubmitter(Context context) {
    appContext = context.getApplicationContext();
    offlineCache = new OfflineCache(appContext);
    scheduleRetry();
  }

  public void submitInterest(
      InterestPayload p, Callback onSuccess, ErrorCallback onError) {
    executor.execute(
        () -> {
          try {
            httpPost(PromoBackendConfig.getInterestUrl(appContext), p.toJson().toString());
            mainHandler.post(() -> onSuccess.call());
          } catch (IOException | JSONException e) {
            Log.w(TAG, "interest submit", e);
            try {
              offlineCache.store("interest", p.toJson());
            } catch (IOException | JSONException io) {
              Log.e(TAG, "offline cache", io);
            }
            mainHandler.post(() -> onError.call(e.getMessage()));
          }
        });
  }

  public void submitFeedback(
      FeedbackPayload p, Callback onSuccess, ErrorCallback onError) {
    executor.execute(
        () -> {
          try {
            httpPost(PromoBackendConfig.getFeedbackUrl(appContext), p.toJson().toString());
            mainHandler.post(() -> onSuccess.call());
          } catch (IOException | JSONException e) {
            Log.w(TAG, "feedback submit", e);
            try {
              offlineCache.store("feedback", p.toJson());
            } catch (IOException | JSONException io) {
              Log.e(TAG, "offline cache", io);
            }
            mainHandler.post(() -> onError.call(e.getMessage()));
          }
        });
  }

  private String httpPost(String urlStr, String jsonBody) throws IOException {
    URL u = new URL(urlStr);
    HttpURLConnection conn = (HttpURLConnection) u.openConnection();
    conn.setRequestMethod("POST");
    conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
    conn.setConnectTimeout(CONNECT_TIMEOUT);
    conn.setReadTimeout(READ_TIMEOUT);
    conn.setDoOutput(true);
    byte[] body = jsonBody.getBytes(StandardCharsets.UTF_8);
    OutputStream os = conn.getOutputStream();
    os.write(body);
    os.flush();
    os.close();
    int code = conn.getResponseCode();
    InputStream stream = code >= 400 ? conn.getErrorStream() : conn.getInputStream();
    String resp = stream != null ? readStream(stream) : "";
    if (code < 200 || code >= 300) {
      throw new IOException("HTTP " + code + " " + resp);
    }
    return resp;
  }

  private static String readStream(InputStream is) throws IOException {
    BufferedReader br =
        new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
    StringBuilder sb = new StringBuilder();
    String line;
    while ((line = br.readLine()) != null) sb.append(line);
    br.close();
    return sb.toString();
  }

  private void scheduleRetry() {
    retryHandler.postDelayed(this::retryRunnable, RETRY_INTERVAL_MS);
  }

  private void retryRunnable() {
    executor.execute(
        () -> {
          try {
            List<OfflineCache.Entry> entries = offlineCache.getAll();
            for (OfflineCache.Entry e : entries) {
              try {
                String url =
                    "interest".equals(e.type)
                        ? PromoBackendConfig.getInterestUrl(appContext)
                        : PromoBackendConfig.getFeedbackUrl(appContext);
                httpPost(url, e.json);
                offlineCache.remove(e.id);
              } catch (IOException ex) {
                Log.w(TAG, "retry failed for " + e.id, ex);
                break;
              }
            }
          } catch (IOException io) {
            Log.w(TAG, "retry list", io);
          } finally {
            scheduleRetry();
          }
        });
  }
}
