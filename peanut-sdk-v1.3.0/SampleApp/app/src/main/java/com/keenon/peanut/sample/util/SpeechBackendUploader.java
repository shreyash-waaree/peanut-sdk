package com.keenon.peanut.sample.util;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * POSTs a WAV body to {@code /speech-transcribe} as multipart {@code audio}.
 */
public final class SpeechBackendUploader {

    private SpeechBackendUploader() {}

    public static String uploadWav(String postUrl, byte[] wavBytes) throws IOException {
        if (postUrl == null || postUrl.trim().isEmpty()) {
            throw new IOException("speech URL empty");
        }
        String boundary = "----PeanutSdkBoundary" + System.currentTimeMillis();
        URL url = new URL(postUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setDoOutput(true);
        conn.setUseCaches(false);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
        conn.setConnectTimeout(20000);
        conn.setReadTimeout(120000);

        byte[] partHeader = buildPartHeader(boundary);
        byte[] closing = ("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(partHeader);
            os.write(wavBytes);
            os.write(closing);
        }

        int code = conn.getResponseCode();
        InputStream stream = code >= 200 && code < 300 ? conn.getInputStream() : conn.getErrorStream();
        String body = readFully(stream);
        conn.disconnect();

        if (code < 200 || code >= 300) {
            throw new IOException("HTTP " + code + ": " + body);
        }
        JSONObject o;
        try {
            o = new JSONObject(body);
        } catch (JSONException e) {
            throw new IOException("Invalid JSON from speech backend: " + body, e);
        }
        String status = o.optString("status", "");
        if (!"ok".equals(status)) {
            throw new IOException(o.optString("error", "speech backend error"));
        }
        return o.optString("text", "").trim();
    }

    private static byte[] buildPartHeader(String boundary) {
        String s = "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"audio\"; filename=\"speech.wav\"\r\n"
                + "Content-Type: audio/wav\r\n\r\n";
        return s.getBytes(StandardCharsets.UTF_8);
    }

    private static String readFully(InputStream in) throws IOException {
        if (in == null) {
            return "";
        }
        BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        char[] buf = new char[4096];
        int n;
        while ((n = r.read(buf)) >= 0) {
            sb.append(buf, 0, n);
        }
        return sb.toString();
    }
}
