package com.keenon.peanut.sample.util;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;

import com.keenon.peanut.sample.BackendConfig;

import java.io.ByteArrayOutputStream;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Records mono PCM16 WAV and POSTs to the Spring Boot {@code /speech-transcribe} endpoint.
 *
 * <p>Stops when the user stops speaking (silence after speech). The VAD uses frame RMS with an
 * adaptive noise floor so sustained motor / crowd noise gets absorbed into the floor and a pause
 * after speech still trips the end-of-utterance timer. Speech-start requires two consecutive
 * above-gate frames to reject single-sample pops.
 */
public final class JavaBackendSpeechTranscriber {

    private static final String TAG = "JavaBackendStt";
    private static final int SAMPLE_RATE = 16000;
    private static final float READ_CHUNK_SEC = 0.1f; // 100 ms frames for snappier VAD
    private static final int CHUNK_MS = Math.round(READ_CHUNK_SEC * 1000f);
    /** After speech ends, this much continuous silence (ms) ends recording and sends audio. */
    private static final int END_SILENCE_MS = 700;
    /** Calibrate microphone noise floor while user stays quiet. */
    private static final long NOISE_CALIBRATE_MS = 800;
    /** Must be this much louder than the adaptive noise floor to count as speech (RMS). */
    private static final int SPEECH_MARGIN_RMS = 300;
    /** Require two consecutive loud frames to flip into "speaking" (reject impulse noise). */
    private static final int SPEECH_START_FRAMES = 2;
    /** If user never speaks after calibration, stop and report (no endless wait). */
    private static final long NO_SPEECH_TIMEOUT_MS = 5000;
    /** Hard cap so a stuck mic never records forever. */
    public static final int RECORD_MAX_MS = 12000;
    /** Minimum audio length to consider a valid utterance (~0.2 s). */
    private static final int MIN_PCM_BYTES = 6400;
    /** Adaptive noise-floor EMA (per-frame) while NOT speaking. Higher = faster absorb motor. */
    private static final float NOISE_FLOOR_ADAPT_UP = 0.12f;
    private static final float NOISE_FLOOR_ADAPT_DOWN = 0.04f;
    /** Never let the adaptive noise floor sit below this (keeps gate meaningful in dead silence). */
    private static final int NOISE_FLOOR_MIN_RMS = 120;
    /** Hard ceiling on noise floor so a loud continuous hum cannot silence user completely. */
    private static final int NOISE_FLOOR_MAX_RMS = 4500;

    public interface Listener {
        void onTranscript(String text);

        /** User-visible error (already logged). */
        void onError(String message);

        /**
         * Input level update: 0..100 percentage relative to the current noise-floor-adjusted
         * dynamic range, plus a coarse state string ("calibrating", "listening", "speaking",
         * "trailing-silence"). Called on the main thread, ~10×/second.
         */
        default void onLevel(int levelPct, String state) {}
    }

    private JavaBackendSpeechTranscriber() {}

    /**
     * Runs recording and upload on a background thread; callbacks are posted to the main looper.
     * Backward-compatible overload — no user-initiated Stop support; call the 4-arg variant if
     * you want a "Stop" button that finalizes and uploads what was captured.
     */
    public static Thread startAsync(
            Context appContext,
            AtomicBoolean cancelFlag,
            Listener listener) {
        return startAsync(appContext, cancelFlag, null, listener);
    }

    /**
     * @param cancelFlag when set, abort recording and do not upload (onError)
     * @param stopFlag when set, end recording NOW and upload whatever has been captured
     *                 (drives the user-facing "Stop" button). May be null.
     */
    public static Thread startAsync(
            Context appContext,
            AtomicBoolean cancelFlag,
            AtomicBoolean stopFlag,
            Listener listener) {
        Context ctx = appContext.getApplicationContext();
        Handler main = new Handler(Looper.getMainLooper());
        Thread t =
                new Thread(
                        () -> {
                            AudioRecord recorder = null;
                            try {
                                int minBytes =
                                        AudioRecord.getMinBufferSize(
                                                SAMPLE_RATE,
                                                AudioFormat.CHANNEL_IN_MONO,
                                                AudioFormat.ENCODING_PCM_16BIT);
                                if (minBytes <= 0) {
                                    postError(main, listener, "Could not open microphone for backend recording.");
                                    return;
                                }
                                int chunkSamples = Math.round(SAMPLE_RATE * READ_CHUNK_SEC);
                                int chunkBytes = chunkSamples * 2;
                                int recordBufferBytes = Math.max(minBytes, chunkBytes * 4);
                                recorder = openAudioRecord(recordBufferBytes);
                                if (recorder == null) {
                                    try {
                                        Thread.sleep(250);
                                    } catch (InterruptedException ignored) {
                                        Thread.currentThread().interrupt();
                                    }
                                    recorder = openAudioRecord(recordBufferBytes);
                                }
                                if (recorder == null) {
                                    postError(
                                            main,
                                            listener,
                                            "Microphone unavailable. Close other voice apps, wait a few seconds, then try again.");
                                    return;
                                }

                                short[] buffer = new short[chunkSamples];
                                ByteArrayOutputStream pcmOut =
                                        new ByteArrayOutputStream(Math.min(400000, chunkBytes * 64));

                                long sessionStart = SystemClock.uptimeMillis();
                                long calibrateUntil = sessionStart + NOISE_CALIBRATE_MS;
                                long hardCapUntil = sessionStart + RECORD_MAX_MS;

                                // Noise-floor tracker in RMS units (0..32767).
                                float noiseRms = 220f; // conservative starting floor
                                float calibSum = 0f;
                                int calibFrames = 0;

                                boolean speechStarted = false;
                                int consecutiveLoudFrames = 0;
                                int consecutiveSilenceMs = 0;
                                int postSilenceFrames = 0;

                                // Pre-roll: keep a small ring of recent frames so audio at the
                                // very moment the user began speaking is included in the upload.
                                int preRollFrames = 3; // ~300 ms at 100 ms frames
                                short[][] preRoll = new short[preRollFrames][];
                                int preRollHead = 0;

                                while (!cancelFlag.get()
                                        && !Thread.currentThread().isInterrupted()) {
                                    long now = SystemClock.uptimeMillis();
                                    if (now >= hardCapUntil) {
                                        Log.i(TAG, "Backend record: hard cap " + RECORD_MAX_MS + " ms");
                                        break;
                                    }
                                    if (stopFlag != null && stopFlag.get()) {
                                        Log.i(TAG, "Backend record: user pressed Stop");
                                        // If VAD never latched speech, flush pre-roll so the
                                        // user's utterance (likely below our gate due to noise)
                                        // still reaches the backend.
                                        if (!speechStarted) {
                                            for (int i = 0; i < preRollFrames; i++) {
                                                int idx = (preRollHead + i) % preRollFrames;
                                                short[] slice = preRoll[idx];
                                                if (slice != null) {
                                                    appendPcm16Le(pcmOut, slice, slice.length);
                                                }
                                            }
                                        }
                                        break;
                                    }

                                    int nread = recorder.read(buffer, 0, buffer.length);
                                    if (nread <= 0) {
                                        continue;
                                    }
                                    float frameRms = pcm16Rms(buffer, nread);
                                    int peakRaw = pcm16Peak(buffer, nread);

                                    if (now < calibrateUntil) {
                                        calibSum += frameRms;
                                        calibFrames++;
                                        // Level UI during calibration uses the current sample
                                        int pct = levelPercent(frameRms, Math.max(noiseRms, 220f));
                                        postLevel(main, listener, pct, "calibrating");
                                        // Keep pre-roll warm even during calibration
                                        preRoll[preRollHead] = cloneSlice(buffer, nread);
                                        preRollHead = (preRollHead + 1) % preRollFrames;
                                        continue;
                                    }

                                    if (calibFrames > 0) {
                                        // Finalize noise-floor estimate as average of calibration
                                        float avg = calibSum / calibFrames;
                                        noiseRms = Math.max(NOISE_FLOOR_MIN_RMS, avg * 1.2f + 60f);
                                        Log.i(TAG, "Backend mic: noise-floor RMS ~" + (int) noiseRms
                                                + " (calibration avg " + (int) avg + ")");
                                        calibFrames = 0;
                                    }

                                    // Adaptive noise floor: when not speaking, move toward current
                                    // frame RMS. Up-adaption is faster so new motor hum is absorbed
                                    // within ~1 s, which lets end-of-speech silence actually register.
                                    float gate = noiseRms + SPEECH_MARGIN_RMS;
                                    boolean loud = frameRms > gate;

                                    if (loud) {
                                        consecutiveLoudFrames++;
                                    } else {
                                        consecutiveLoudFrames = 0;
                                    }
                                    boolean enteringSpeech =
                                            !speechStarted && consecutiveLoudFrames >= SPEECH_START_FRAMES;

                                    if (enteringSpeech) {
                                        speechStarted = true;
                                        // Flush pre-roll so the start of the utterance is captured
                                        for (int i = 0; i < preRollFrames; i++) {
                                            int idx = (preRollHead + i) % preRollFrames;
                                            short[] slice = preRoll[idx];
                                            if (slice != null) {
                                                appendPcm16Le(pcmOut, slice, slice.length);
                                            }
                                        }
                                        consecutiveSilenceMs = 0;
                                    }

                                    if (speechStarted) {
                                        appendPcm16Le(pcmOut, buffer, nread);
                                        if (loud) {
                                            consecutiveSilenceMs = 0;
                                            postSilenceFrames = 0;
                                        } else {
                                            consecutiveSilenceMs += CHUNK_MS;
                                            postSilenceFrames++;
                                            // Adapt floor downward slightly during trailing silence
                                            // so a brief loud interrupt doesn't permanently raise it.
                                            noiseRms =
                                                    clampFloor(
                                                            noiseRms * (1f - NOISE_FLOOR_ADAPT_DOWN)
                                                                    + frameRms * NOISE_FLOOR_ADAPT_DOWN);
                                            if (consecutiveSilenceMs >= END_SILENCE_MS) {
                                                Log.i(
                                                        TAG,
                                                        "Backend record: end of speech after "
                                                                + END_SILENCE_MS
                                                                + " ms silence (floor RMS="
                                                                + (int) noiseRms + ")");
                                                break;
                                            }
                                        }
                                    } else {
                                        // Not yet speaking — continuously absorb current input into
                                        // the noise floor (this is what makes motor hum stop mattering).
                                        float target = frameRms;
                                        float alpha = target > noiseRms
                                                ? NOISE_FLOOR_ADAPT_UP
                                                : NOISE_FLOOR_ADAPT_DOWN;
                                        noiseRms = clampFloor(noiseRms * (1f - alpha) + target * alpha);
                                        // Fill pre-roll ring
                                        preRoll[preRollHead] = cloneSlice(buffer, nread);
                                        preRollHead = (preRollHead + 1) % preRollFrames;
                                        if (now - sessionStart > NO_SPEECH_TIMEOUT_MS + NOISE_CALIBRATE_MS) {
                                            postError(
                                                    main,
                                                    listener,
                                                    "No speech detected. Tap and speak clearly.");
                                            return;
                                        }
                                    }

                                    String state;
                                    if (speechStarted) {
                                        state = consecutiveSilenceMs > 0 ? "trailing-silence" : "speaking";
                                    } else {
                                        state = "listening";
                                    }
                                    int pct = levelPercent(frameRms, noiseRms);
                                    postLevel(main, listener, pct, state);

                                    // Avoid ignoring the unused peak value in logs during tuning
                                    if (peakRaw > 30000) {
                                        Log.v(TAG, "Clip: peak=" + peakRaw + " rms=" + (int) frameRms);
                                    }
                                }

                                if (pcmOut.size() < MIN_PCM_BYTES) {
                                    postError(main, listener, "Recording too short. Try again.");
                                    return;
                                }

                                byte[] wav = WavPcm16Mono.buildWav(pcmOut.toByteArray(), SAMPLE_RATE);
                                String speechUrl = BackendConfig.getSpeechTranscribeUrl(ctx);
                                Log.i(TAG, "POST WAV bytes=" + wav.length + " to " + speechUrl);
                                String text = SpeechBackendUploader.uploadWav(speechUrl, wav);
                                if (text == null || text.trim().isEmpty()) {
                                    postError(main, listener, "Backend returned an empty transcript.");
                                    return;
                                }
                                final String said = text.trim();
                                main.post(
                                        () -> {
                                            if (listener != null) {
                                                listener.onTranscript(said);
                                            }
                                        });
                            } catch (Throwable ex) {
                                Log.e(TAG, "Backend speech recording/upload failed", ex);
                                String msg =
                                        ex.getMessage() != null ? ex.getMessage() : "speech backend error";
                                postError(main, listener, msg);
                            } finally {
                                if (recorder != null) {
                                    try {
                                        recorder.stop();
                                    } catch (Exception ignored) {
                                    }
                                    recorder.release();
                                }
                            }
                        },
                        "java-backend-stt");
        t.start();
        return t;
    }

    private static short[] cloneSlice(short[] src, int n) {
        short[] out = new short[n];
        System.arraycopy(src, 0, out, 0, n);
        return out;
    }

    private static float clampFloor(float v) {
        if (v < NOISE_FLOOR_MIN_RMS) return NOISE_FLOOR_MIN_RMS;
        if (v > NOISE_FLOOR_MAX_RMS) return NOISE_FLOOR_MAX_RMS;
        return v;
    }

    /**
     * Maps the frame's loudness against a noise-floor-adjusted 0..100 bar. 0 = at/under floor,
     * 100 = loud speech. Uses a fixed dynamic range above the floor so the bar behaves like a
     * speaker VU meter.
     */
    private static int levelPercent(float frameRms, float noiseRms) {
        float floor = Math.max(noiseRms, 120f);
        float above = Math.max(0f, frameRms - floor);
        // ~4500 units above floor is "loud normal speech"; cap at 100.
        float pct = above / 4500f * 100f;
        if (pct < 0f) pct = 0f;
        if (pct > 100f) pct = 100f;
        return (int) pct;
    }

    private static void appendPcm16Le(ByteArrayOutputStream out, short[] buffer, int nShorts) {
        for (int i = 0; i < nShorts; i++) {
            short s = buffer[i];
            out.write(s & 0xff);
            out.write((s >> 8) & 0xff);
        }
    }

    private static int pcm16Peak(short[] buffer, int nShorts) {
        int max = 0;
        for (int i = 0; i < nShorts; i++) {
            int a = Math.abs((int) buffer[i]);
            if (a > max) {
                max = a;
            }
        }
        return max;
    }

    private static float pcm16Rms(short[] buffer, int nShorts) {
        if (nShorts <= 0) return 0f;
        double sumSq = 0.0;
        for (int i = 0; i < nShorts; i++) {
            double v = buffer[i];
            sumSq += v * v;
        }
        return (float) Math.sqrt(sumSq / nShorts);
    }

    private static void postLevel(Handler main, Listener listener, int levelPct, String state) {
        if (listener == null) return;
        main.post(() -> listener.onLevel(levelPct, state));
    }

    private static void postError(Handler main, Listener listener, String message) {
        main.post(
                () -> {
                    if (listener != null) {
                        listener.onError(message);
                    }
                });
    }

    /** Same source order as offline Vosk path in {@code VoiceFragment}. */
    private static AudioRecord openAudioRecord(int recordBufferBytes) {
        int[] sources =
                new int[] {
                    MediaRecorder.AudioSource.MIC,
                    MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                    MediaRecorder.AudioSource.VOICE_RECOGNITION,
                    MediaRecorder.AudioSource.DEFAULT,
                    MediaRecorder.AudioSource.CAMCORDER,
                };
        for (int src : sources) {
            AudioRecord r = null;
            try {
                r =
                        new AudioRecord(
                                src,
                                SAMPLE_RATE,
                                AudioFormat.CHANNEL_IN_MONO,
                                AudioFormat.ENCODING_PCM_16BIT,
                                recordBufferBytes);
                if (r.getState() != AudioRecord.STATE_INITIALIZED) {
                    r.release();
                    continue;
                }
                r.startRecording();
                if (r.getRecordingState() != AudioRecord.RECORDSTATE_RECORDING) {
                    try {
                        r.stop();
                    } catch (Exception ignored) {
                    }
                    r.release();
                    continue;
                }
                Log.i(TAG, "Backend mic: AudioSource=" + src);
                return r;
            } catch (Throwable t) {
                Log.w(TAG, "AudioRecord AudioSource=" + src + " failed", t);
                if (r != null) {
                    try {
                        r.release();
                    } catch (Exception ignored) {
                    }
                }
            }
        }
        return null;
    }
}
