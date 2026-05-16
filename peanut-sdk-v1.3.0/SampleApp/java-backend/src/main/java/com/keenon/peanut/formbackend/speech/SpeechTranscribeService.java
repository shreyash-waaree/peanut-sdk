package com.keenon.peanut.formbackend.speech;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Local Whisper transcription runner.
 *
 * <p>Prefers a long-lived sidecar at {@code speech.whisper.sidecar-url} (the model stays loaded,
 * so short utterances transcribe in ~200-600 ms). Falls back to a per-request Python subprocess
 * if the sidecar is not reachable or not configured — the subprocess mode is slow (5-10 s per
 * request) because the model has to reload every call.
 */
@Service
public class SpeechTranscribeService {

    private static final Logger log = LoggerFactory.getLogger(SpeechTranscribeService.class);

    private final boolean enabled;
    private final String commandCsv;
    private final int timeoutSec;
    private final String sidecarUrl;
    private final int sidecarConnectMs;
    private final int sidecarReadMs;

    public SpeechTranscribeService(
            @Value("${speech.whisper.enabled:true}") boolean enabled,
            @Value("${speech.whisper.command:}") String commandCsv,
            @Value("${speech.whisper.timeout-sec:60}") int timeoutSec,
            @Value("${speech.whisper.sidecar-url:http://127.0.0.1:9090/transcribe}") String sidecarUrl,
            @Value("${speech.whisper.sidecar-connect-ms:800}") int sidecarConnectMs,
            @Value("${speech.whisper.sidecar-read-ms:15000}") int sidecarReadMs) {
        this.enabled = enabled;
        this.commandCsv = commandCsv;
        this.timeoutSec = timeoutSec;
        this.sidecarUrl = sidecarUrl;
        this.sidecarConnectMs = sidecarConnectMs;
        this.sidecarReadMs = sidecarReadMs;
    }

    public boolean isEnabled() {
        return enabled;
    }

    /**
     * @param wavBytes full RIFF WAVE file bytes from client
     * @return transcript text (may be empty if no speech detected)
     */
    public String transcribeWav(byte[] wavBytes) throws IOException {
        if (!enabled) {
            throw new IllegalStateException("Whisper speech disabled (speech.whisper.enabled=false)");
        }

        // Validate WAV shape early to fail fast with readable error.
        WavPcmReader.parse(wavBytes);

        // 1) Fast path: POST to persistent sidecar.
        if (sidecarUrl != null && !sidecarUrl.trim().isEmpty()) {
            long t0 = System.currentTimeMillis();
            try {
                String text = transcribeViaSidecar(wavBytes);
                log.info("Whisper sidecar ok in {} ms ({} bytes)",
                        System.currentTimeMillis() - t0, wavBytes.length);
                return text;
            } catch (IOException e) {
                log.warn("Whisper sidecar unreachable ({}): {} — falling back to subprocess.",
                        sidecarUrl, e.getMessage());
            }
        }

        // 2) Slow path: spawn a fresh Python process. Kept for environments where the
        //    sidecar isn't running. The first request after boot will pay the cold start.
        return transcribeViaSubprocess(wavBytes);
    }

    private String transcribeViaSidecar(byte[] wavBytes) throws IOException {
        URL url = new URL(sidecarUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setDoOutput(true);
        conn.setUseCaches(false);
        conn.setConnectTimeout(sidecarConnectMs);
        conn.setReadTimeout(sidecarReadMs);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/octet-stream");
        conn.setFixedLengthStreamingMode(wavBytes.length);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(wavBytes);
            os.flush();
        }
        int code = conn.getResponseCode();
        InputStream is = (code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream();
        String body = readAll(is);
        if (code < 200 || code >= 300) {
            throw new IOException("sidecar HTTP " + code + ": " + body);
        }
        String text = extractJsonStringField(body, "text");
        return text == null ? "" : text.trim();
    }

    private String transcribeViaSubprocess(byte[] wavBytes) throws IOException {
        if (commandCsv == null || commandCsv.trim().isEmpty()) {
            throw new IOException(
                    "No transcription backend: sidecar unreachable and speech.whisper.command is empty. "
                            + "Start whisper_server.py or set speech.whisper.command.");
        }
        File tmp = File.createTempFile("peanut-whisper-", ".wav");
        try {
            Files.write(tmp.toPath(), wavBytes);
            List<String> cmd = buildCommand(tmp.getAbsolutePath());
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(true);
            Process p = pb.start();
            String output = readAll(p.getInputStream());
            boolean done;
            try {
                done = p.waitFor(timeoutSec, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Whisper command interrupted", e);
            }
            if (!done) {
                p.destroyForcibly();
                throw new IOException("Whisper command timed out after " + timeoutSec + "s");
            }
            int code = p.exitValue();
            if (code != 0) {
                throw new IOException("Whisper command failed (exit " + code + "): " + output);
            }
            return output == null ? "" : output.trim();
        } finally {
            try {
                Files.deleteIfExists(tmp.toPath());
            } catch (IOException ignored) {
            }
        }
    }

    private List<String> buildCommand(String audioPath) {
        String[] parts = commandCsv.split(",");
        List<String> cmd = new ArrayList<>();
        for (String part : parts) {
            String t = part.trim();
            if (!t.isEmpty()) {
                cmd.add(t.replace("{audio}", audioPath));
            }
        }
        return cmd;
    }

    private static String readAll(InputStream in) throws IOException {
        if (in == null) return "";
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) != -1) {
            out.write(buf, 0, n);
        }
        return out.toString(StandardCharsets.UTF_8.name());
    }

    /**
     * Minimal JSON string-field extractor for {@code {"text":"..."}} responses from the sidecar.
     * Avoids adding a JSON dependency to the backend for this one tiny field.
     */
    private static String extractJsonStringField(String json, String field) {
        if (json == null) return null;
        Pattern p = Pattern.compile(
                "\"" + Pattern.quote(field) + "\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"");
        Matcher m = p.matcher(json);
        if (!m.find()) return null;
        String raw = m.group(1);
        // Unescape common JSON string escapes and basic hex-four unicode.
        StringBuilder out = new StringBuilder(raw.length());
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (c == '\\' && i + 1 < raw.length()) {
                char n = raw.charAt(++i);
                switch (n) {
                    case '"': out.append('"'); break;
                    case '\\': out.append('\\'); break;
                    case '/': out.append('/'); break;
                    case 'n': out.append('\n'); break;
                    case 'r': out.append('\r'); break;
                    case 't': out.append('\t'); break;
                    case 'b': out.append('\b'); break;
                    case 'f': out.append('\f'); break;
                    case 'u':
                        if (i + 4 < raw.length()) {
                            String hex = raw.substring(i + 1, i + 5);
                            out.append((char) Integer.parseInt(hex, 16));
                            i += 4;
                        }
                        break;
                    default: out.append(n);
                }
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }
}
