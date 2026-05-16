package com.keenon.peanut.sample;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.os.SystemClock;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.AbstractMap;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.vosk.LibVosk;
import org.vosk.LogLevel;
import org.vosk.Model;
import org.vosk.Recognizer;

import com.keenon.peanut.sample.chassis.ChargerDemo;
import com.keenon.peanut.sample.receiver.RobotMovementManager;
import com.keenon.peanut.sample.BuildConfig;
import com.keenon.sdk.constant.ApiConstants;
import com.keenon.peanut.sample.util.JavaBackendSpeechTranscriber;
import com.keenon.peanut.sample.util.VoiceBackendHelper;
import com.keenon.peanut.sample.util.VoicePreferences;
import com.keenon.peanut.sample.util.VoiceTranscriptUtil;

public class VoiceFragment extends Fragment {

    private static final String TAG = VoiceFragment.class.getSimpleName();
    /** Utterance id for normal chat TTS (no side effects). */
    private static final String TTS_UTTERANCE_VOICE_REPLY = "voice_reply";
    /** Utterance id: after this finishes, open {@link ChargerDemo}. */
    private static final String TTS_UTTERANCE_CHARGING_NAV = "charging_nav";
    private static final int REQUEST_RECORD_AUDIO = 101;
    private static final int VOSK_SAMPLE_RATE = 16000;
    /**
     * Legacy internal folder name used by older builds that copied a model from APK; may still
     * exist on disk and is removed when switching to external storage.
     */
    private static final String VOSK_LEGACY_INTERNAL_CACHE_DIR = "model-en-us-lggraph";
    /**
     * Large (or any) Vosk model without bundling in APK: unzip so {@code am/}, {@code conf/},
     * {@code graph/}, {@code ivector/} sit directly under this folder.
     * Path: {@code Android/data/&lt;package&gt;/files/vosk-model/}
     */
    private static final String VOSK_EXTERNAL_MODEL_FOLDER = "vosk-model";
    /**
     * Unzipped {@code vosk-model-small-en-in-0.4} placed under {@code app/src/main/assets/} so
     * {@code am/}, {@code conf/}, {@code graph/}, {@code ivector/} sit directly under this folder.
     * Copied once to internal storage at runtime (native {@link Model} cannot read from AssetManager).
     */
    private static final String VOSK_ASSET_MODEL_FOLDER = "vosk-model-small-en-in-0.4";
    /** Internal dir for the extracted copy of {@link #VOSK_ASSET_MODEL_FOLDER}. */
    private static final String VOSK_BUNDLED_INTERNAL_DIR = "vosk-bundled-model";
    /** Same chunk size as {@code org.vosk.android.SpeechService} (0.2 s of 16 kHz mono). */
    private static final float VOSK_READ_CHUNK_SEC = 0.2f;
    /** Calibrate ambient / electronic noise for mic meter (ms). */
    private static final long VOSK_NOISE_CALIBRATE_MS = 750;
    /** After calibration, PCM peaks below floor + margin are treated as silence for Vosk. */
    private static final int VOSK_GATE_MARGIN_PCM = 160;
    /** Max folder depth under {@code vosk-model/} when searching for a nested unzip layout. */
    private static final int VOSK_MODEL_SEARCH_MAX_DEPTH = 8;
    /** Cap BFS nodes so we do not walk huge trees (e.g. mistaken copy under {@code graph/}). */
    private static final int VOSK_MODEL_SEARCH_MAX_DIRS = 512;
    /** Debug session NDJSON filename (pull via adb to workspace for analysis). */
    private static final String AGENT_DEBUG_LOG_NAME = "debug-5e44b7.log";
    private static final String AGENT_DEBUG_SESSION = "5e44b7";
    private SpeechRecognizer mSpeechRecognizer;
    private TextToSpeech mTts;
    /** Written on worker thread after load; volatile so UI thread sees it immediately. */
    private volatile Model mVoskModel;
    private Thread mOfflineThread;
    private boolean mTtsReady = false;
    /** Kept in sync with mVoskModel: false whenever model is closed in onDestroyView. */
    private volatile boolean mVoskReady = false;
    /** Absolute path of loaded model (external or internal); for status text. */
    private volatile String mVoskModelPath = "";
    private volatile boolean mVoskFromExternal = false;
    /** True when model path is under {@code getFilesDir()/vosk-model} (e.g. adb run-as push). */
    private volatile boolean mVoskFromInternalVosk = false;
    /** True when load path is under {@link #VOSK_BUNDLED_INTERNAL_DIR} (copy of assets). */
    private volatile boolean mVoskFromBundled = false;
    private volatile boolean mOfflineListening = false;
    private long mLastChargingActionAtMs = 0;
    /** Set when charging reply is queued on TTS; cleared when navigation runs (avoids double start). */
    private final AtomicBoolean mChargingNavFromTtsPending = new AtomicBoolean(false);
    private final AtomicInteger mOfflineInitGeneration = new AtomicInteger(0);
    /** Bumps each time offline listening starts; stale threads must not reset the UI. */
    private final AtomicInteger mOfflineListenSession = new AtomicInteger(0);
    /** API 24+: refresh UI when default network gains/loses connectivity. */
    @Nullable
    private ConnectivityManager.NetworkCallback mNetworkCallback;

    /** Worker that records PCM and POSTs WAV to backend {@code /speech-transcribe}. */
    @Nullable
    private Thread mBackendRecordThread;
    private final AtomicBoolean mCancelBackendRecord = new AtomicBoolean(false);
    /** Set by a user "Stop" tap — makes the loop finalize and upload, not abort. */
    private final AtomicBoolean mStopBackendRecord = new AtomicBoolean(false);

    private ListView lvChat;
    private Button btnMic;
    private Button btnCheckVoiceSupport;
    private TextView tvListening;
    private TextView tvStatusBadge;
    private TextView tvVoiceCapability;
    private TextView tvVoiceDiagnostics;
    private Switch swForceOffline;
    private LinearLayout micLevelRow;
    private ProgressBar pbMicLevel;
    private TextView tvMicLevel;

    private final List<ChatMessage> mMessages = new ArrayList<>();
    private ChatAdapter mAdapter;

    // ---- Model ----

    private static class ChatMessage {
        final String text;
        final boolean isUser; // true = user, false = robot

        ChatMessage(String text, boolean isUser) {
            this.text = text;
            this.isUser = isUser;
        }
    }

    // ---- Lifecycle ----

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_voice, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        lvChat = view.findViewById(R.id.lv_chat);
        btnMic = view.findViewById(R.id.btn_mic);
        btnCheckVoiceSupport = view.findViewById(R.id.btn_check_voice_support);
        tvListening = view.findViewById(R.id.tv_listening);
        tvStatusBadge = view.findViewById(R.id.tv_status_badge);
        tvVoiceCapability = view.findViewById(R.id.tv_voice_capability);
        tvVoiceDiagnostics = view.findViewById(R.id.tv_voice_diagnostics);
        swForceOffline = view.findViewById(R.id.sw_force_offline);
        micLevelRow = view.findViewById(R.id.mic_level_row);
        pbMicLevel = view.findViewById(R.id.pb_mic_level);
        tvMicLevel = view.findViewById(R.id.tv_mic_level);

        if (swForceOffline != null) {
            swForceOffline.setChecked(VoicePreferences.isForceOffline(requireContext()));
            swForceOffline.setOnCheckedChangeListener((buttonView, isChecked) -> {
                VoicePreferences.setForceOffline(requireContext(), isChecked);
                addRobotMessage(isChecked
                        ? "Offline voice forced — Vosk on-device will be used when the model is ready."
                        : "Online voice allowed — will use Java backend or system recognizer if available.");
                refreshVoiceCapabilityStatus(false);
            });
        }

        mAdapter = new ChatAdapter();
        lvChat.setAdapter(mAdapter);

        addRobotMessage("Hi! I am your robot assistant. Say hello to me!");

        initTts();
        initOfflineRecognizerAsync();
        refreshVoiceCapabilityStatus(true);

        btnMic.setOnClickListener(v -> {
            // If a session is already running, the button acts as STOP (finalize + upload).
            if (mBackendRecordThread != null) {
                mStopBackendRecord.set(true);
                btnMic.setEnabled(false);
                btnMic.setText("Processing…");
                tvListening.setText("Finalizing — sending to backend…");
                tvStatusBadge.setText("Processing…");
                return;
            }
            if (mSpeechRecognizer != null) {
                try {
                    mSpeechRecognizer.stopListening();
                } catch (Throwable ignored) {
                }
                btnMic.setEnabled(false);
                btnMic.setText("Processing…");
                tvListening.setText("Finalizing — processing what you said…");
                tvStatusBadge.setText("Processing…");
                return;
            }
            if (mOfflineListening) {
                // Vosk already emits finals mid-stream; just stop the capture loop.
                stopOfflineListening();
                stopListeningUI();
                addRobotMessage("Offline listening stopped.");
                return;
            }
            if (ContextCompat.checkSelfPermission(requireContext(),
                    Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_RECORD_AUDIO);
            } else {
                startListening();
            }
        });

        btnCheckVoiceSupport.setOnClickListener(v -> {
            if (!mVoskReady) {
                Toast.makeText(requireContext(), "Retrying offline model setup…", Toast.LENGTH_SHORT).show();
                initOfflineRecognizerAsync();
            }
            refreshVoiceCapabilityStatus(true);
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && getContext() != null) {
            ConnectivityManager cm = (ConnectivityManager) requireContext().getSystemService(Context.CONNECTIVITY_SERVICE);
            mNetworkCallback = new ConnectivityManager.NetworkCallback() {
                @Override
                public void onAvailable(@NonNull Network network) {
                    postRefreshCapability();
                }

                @Override
                public void onLost(@NonNull Network network) {
                    postRefreshCapability();
                }
            };
            cm.registerDefaultNetworkCallback(mNetworkCallback);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshVoiceCapabilityStatus(false);
    }

    @Override
    public void onStop() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && mNetworkCallback != null && getContext() != null) {
            ConnectivityManager cm = (ConnectivityManager) requireContext().getSystemService(Context.CONNECTIVITY_SERVICE);
            try {
                cm.unregisterNetworkCallback(mNetworkCallback);
            } catch (RuntimeException ignored) {
            }
            mNetworkCallback = null;
        }
        super.onStop();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == REQUEST_RECORD_AUDIO) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startListening();
            } else {
                Toast.makeText(getActivity(), "Microphone permission is needed for voice chat.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mOfflineInitGeneration.incrementAndGet();
        stopOfflineListening();
        if (mSpeechRecognizer != null) {
            mSpeechRecognizer.destroy();
            mSpeechRecognizer = null;
        }
        if (mVoskModel != null) {
            mVoskModel.close();
            mVoskModel = null;
        }
        mVoskReady = false;
        mVoskModelPath = "";
        mVoskFromExternal = false;
        mVoskFromInternalVosk = false;
        mVoskFromBundled = false;
        if (mTts != null) {
            mTts.stop();
            mTts.shutdown();
            mTts = null;
        }
        mChargingNavFromTtsPending.set(false);
        mCancelBackendRecord.set(true);
        if (mBackendRecordThread != null) {
            try {
                mBackendRecordThread.join(2000);
            } catch (InterruptedException ignored) {
            }
            mBackendRecordThread = null;
        }
    }

    // ---- TTS ----

    private void initTts() {
        Context appCtx = requireActivity().getApplicationContext();
        mTts = new TextToSpeech(appCtx, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = mTts.setLanguage(Locale.US);
                mTtsReady = (result != TextToSpeech.LANG_MISSING_DATA
                        && result != TextToSpeech.LANG_NOT_SUPPORTED);
                if (!mTtsReady) {
                    Log.w(TAG, "TTS language not supported, speech output disabled.");
                } else {
                    applyTtsSpeakerRouting();
                    mTts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                        @Override
                        public void onStart(String utteranceId) {
                        }

                        @Override
                        public void onDone(String utteranceId) {
                            if (TTS_UTTERANCE_CHARGING_NAV.equals(utteranceId)) {
                                postChargingNavigationAfterTts();
                            }
                        }

                        @SuppressWarnings("deprecation")
                        @Override
                        public void onError(String utteranceId) {
                            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP
                                    && TTS_UTTERANCE_CHARGING_NAV.equals(utteranceId)) {
                                postChargingNavigationAfterTts();
                            }
                        }

                        @Override
                        public void onError(String utteranceId, int errorCode) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
                                    && TTS_UTTERANCE_CHARGING_NAV.equals(utteranceId)) {
                                postChargingNavigationAfterTts();
                            }
                        }
                    });
                }
            }
        });
    }

    /**
     * Route TTS to the main loudspeaker: {@link AudioAttributes} on API 21+, legacy stream on 19–20.
     */
    private void applyTtsSpeakerRouting() {
        if (mTts == null) {
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            int usage = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                    ? AudioAttributes.USAGE_ASSISTANT
                    : AudioAttributes.USAGE_MEDIA;
            AudioAttributes attrs = new AudioAttributes.Builder()
                    .setUsage(usage)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build();
            mTts.setAudioAttributes(attrs);
        }
    }

    @Nullable
    private Bundle ttsSpeakExtras() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            Bundle b = new Bundle();
            b.putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_MUSIC);
            return b;
        }
        return null;
    }

    private void speak(String text) {
        if (mTts == null || !mTtsReady) {
            return;
        }
        mTts.speak(text, TextToSpeech.QUEUE_FLUSH, ttsSpeakExtras(), TTS_UTTERANCE_VOICE_REPLY);
    }

    /**
     * Speaks the charging confirmation, then opens {@link ChargerDemo} on the main thread
     * when TTS completes (or errors).
     */
    private void speakChargingThenNavigate(String reply) {
        if (mTts == null || !mTtsReady) {
            triggerChargingAction();
            return;
        }
        mChargingNavFromTtsPending.set(true);
        int code = mTts.speak(reply, TextToSpeech.QUEUE_FLUSH, ttsSpeakExtras(),
                TTS_UTTERANCE_CHARGING_NAV);
        if (code == TextToSpeech.ERROR) {
            mChargingNavFromTtsPending.set(false);
            triggerChargingAction();
        }
    }

    private void postChargingNavigationAfterTts() {
        if (!mChargingNavFromTtsPending.compareAndSet(true, false)) {
            return;
        }
        new Handler(Looper.getMainLooper()).post(() -> {
            if (!isAdded() || getActivity() == null) {
                return;
            }
            triggerChargingAction();
        });
    }

    // ---- Speech Recognition ----

    private boolean isSpeechRecognitionSupported() {
        return SpeechRecognizer.isRecognitionAvailable(requireContext());
    }

    private void postRefreshCapability() {
        if (getActivity() == null) {
            return;
        }
        new Handler(Looper.getMainLooper()).post(() -> {
            if (!isAdded() || getView() == null) {
                return;
            }
            refreshVoiceCapabilityStatus(false);
        });
    }

    /**
     * Spring Boot {@code /speech-transcribe} on the LAN (Whisper). Preferred when the device has
     * network and a configured base URL (same as retail {@code PromoVoiceFragment}).
     */
    private boolean useJavaBackendSpeech() {
        return VoiceBackendHelper.shouldUseJavaBackendStt(requireContext());
    }

    /** Android {@link SpeechRecognizer} when the Java backend is not selected. */
    private boolean useSystemOnlineSpeech() {
        if (VoicePreferences.isForceOffline(requireContext())) {
            return false;
        }
        return VoiceBackendHelper.isNetworkAvailable(requireContext())
                && isSpeechRecognitionSupported()
                && !useJavaBackendSpeech();
    }

    /** Any online STT path (Java backend or system recognizer). */
    private boolean useAnyOnlineSpeech() {
        return useJavaBackendSpeech() || useSystemOnlineSpeech();
    }

    private void refreshVoiceCapabilityStatus(boolean announceInChat) {
        if (getView() == null) {
            return;
        }
        boolean recognizerAvailable = isSpeechRecognitionSupported();
        boolean networkAvailable = VoiceBackendHelper.isNetworkAvailable(requireContext());
        boolean systemOnline = useSystemOnlineSpeech();
        boolean javaBackend = useJavaBackendSpeech();
        updateVoiceDiagnosticsUi(recognizerAvailable, networkAvailable,
                javaBackend ? "Online · Java backend" : (systemOnline ? "Online · system" : "Offline"));
        if (useAnyOnlineSpeech() && mOfflineListening) {
            stopOfflineListening();
            tvListening.setVisibility(View.GONE);
        }

        boolean offlineOk = mVoskReady && mVoskModel != null;

        if (javaBackend) {
            tvVoiceCapability.setText("Voice engine: Online (Java backend → /speech-transcribe)");
            tvStatusBadge.setText("Ready");
            btnMic.setEnabled(true);
            btnMic.setText("Tap to Speak");
            if (announceInChat) {
                addRobotMessage("Voice diagnostic: Online mode — speech goes to the Java backend (full transcript used for replies).");
            }
            return;
        }

        if (systemOnline) {
            tvVoiceCapability.setText("Voice engine: Online (system SpeechRecognizer)");
            tvStatusBadge.setText("Ready");
            btnMic.setEnabled(true);
            btnMic.setText("Hold to Speak");
            if (announceInChat) {
                addRobotMessage("Voice diagnostic: Online mode — system SpeechRecognizer (full transcript used for replies).");
            }
            return;
        }

        if (offlineOk) {
            tvVoiceCapability.setText(mVoskFromBundled
                    ? "Voice engine: Offline Vosk (bundled in APK: assets/" + VOSK_ASSET_MODEL_FOLDER + ")"
                    : (mVoskFromExternal
                    ? "Voice engine: Offline Vosk (Android/data/…/files/" + VOSK_EXTERNAL_MODEL_FOLDER + ")"
                    : (mVoskFromInternalVosk
                    ? "Voice engine: Offline Vosk (internal files/" + VOSK_EXTERNAL_MODEL_FOLDER + ")"
                    : "Voice engine: Offline Vosk ready")));
            tvStatusBadge.setText("Offline Ready");
            btnMic.setEnabled(true);
            btnMic.setText(mOfflineListening ? "Stop Offline Listening" : "Start Offline Listening");
            if (announceInChat) {
                addRobotMessage("Voice diagnostic: Offline mode — using Vosk on-device recognition.");
            }
            return;
        }

        if (isSpeechRecognitionSupported() && !VoiceBackendHelper.isNetworkAvailable(requireContext())) {
            tvVoiceCapability.setText("Voice engine: No internet — waiting for offline Vosk model…");
        } else if (!isSpeechRecognitionSupported()) {
            tvVoiceCapability.setText("Voice engine: No system recognizer — preparing offline model…");
        } else {
            tvVoiceCapability.setText("Voice engine: Preparing offline model…");
        }
        tvStatusBadge.setText("Offline Loading");
        btnMic.setEnabled(false);
        btnMic.setText("Wait — loading model…");
        if (announceInChat) {
            addRobotMessage("Voice diagnostic: Offline model is loading — wait until the mic enables, or tap Check Voice Support to retry.");
        }
    }

    private void startListening() {
        if (useJavaBackendSpeech()) {
            startBackendSpeechRecording();
            return;
        }
        if (useSystemOnlineSpeech()) {
            updateVoiceDiagnosticsUi(true, true, "Online Listening · system");

            if (mSpeechRecognizer != null) {
                mSpeechRecognizer.destroy();
            }
            mSpeechRecognizer = SpeechRecognizer.createSpeechRecognizer(requireContext());
            mSpeechRecognizer.setRecognitionListener(new SimpleRecognitionListener());

            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
            intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
            intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);

            Log.i(TAG, "Voice listening: system SpeechRecognizer");

            mSpeechRecognizer.startListening(intent);

            tvListening.setVisibility(View.VISIBLE);
            tvListening.setText("Listening… tap Stop when done.");
            tvStatusBadge.setText("Listening...");
            btnMic.setEnabled(true);
            btnMic.setText("Stop");
            return;
        }
        toggleOfflineListening();
    }

    /**
     * Records mono PCM at 16 kHz, wraps WAV, POSTs to runtime backend {@code /speech-transcribe}.
     */
    private void startBackendSpeechRecording() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(requireContext(), "Microphone permission required.", Toast.LENGTH_SHORT).show();
            return;
        }
        // Ensure no stale recorder/listener still holds the mic.
        stopOfflineListening();
        if (mSpeechRecognizer != null) {
            try {
                mSpeechRecognizer.destroy();
            } catch (Throwable ignored) {
            }
            mSpeechRecognizer = null;
        }
        mCancelBackendRecord.set(false);
        mStopBackendRecord.set(false);
        tvListening.setVisibility(View.VISIBLE);
        tvListening.setText("Listening… speak, then tap Stop to send");
        tvStatusBadge.setText("Recording...");
        btnMic.setEnabled(true);
        btnMic.setText("Stop");
        showMicLevelUi(true);
        updateMicLevelUi(0);
        updateVoiceDiagnosticsUi(false, true, "Online · Java backend recording");

        mBackendRecordThread =
                JavaBackendSpeechTranscriber.startAsync(
                        requireContext(),
                        mCancelBackendRecord,
                        mStopBackendRecord,
                        new JavaBackendSpeechTranscriber.Listener() {
                            @Override
                            public void onTranscript(String text) {
                                mBackendRecordThread = null;
                                if (!isAdded() || getView() == null) {
                                    return;
                                }
                                processUserSpeech(text);
                                stopListeningUI();
                            }

                            @Override
                            public void onError(String message) {
                                mBackendRecordThread = null;
                                if (!isAdded() || getActivity() == null) {
                                    return;
                                }
                                addRobotMessage("Java backend speech failed: " + message);
                                Toast.makeText(
                                                getActivity(),
                                                "Java backend speech: " + message,
                                                Toast.LENGTH_LONG)
                                        .show();
                                stopListeningUI();
                            }

                            @Override
                            public void onLevel(int levelPct, String state) {
                                if (!isAdded() || getView() == null) return;
                                updateMicLevelUi(levelPct);
                                if ("calibrating".equals(state)) {
                                    tvListening.setText("Calibrating background noise… stay quiet.");
                                } else if ("speaking".equals(state)) {
                                    tvListening.setText("Listening… speaking (" + levelPct + "%)");
                                } else if ("trailing-silence".equals(state)) {
                                    tvListening.setText("Pause detected — finishing up…");
                                } else {
                                    tvListening.setText("Listening… (" + levelPct + "%) speak, then pause.");
                                }
                            }
                        });
    }

    private void showMicLevelUi(boolean visible) {
        if (micLevelRow == null) return;
        micLevelRow.setVisibility(visible ? View.VISIBLE : View.GONE);
        if (!visible && pbMicLevel != null) {
            pbMicLevel.setProgress(0);
            if (tvMicLevel != null) tvMicLevel.setText("0%");
        }
    }

    private void updateMicLevelUi(int pct) {
        if (pbMicLevel != null) pbMicLevel.setProgress(Math.max(0, Math.min(100, pct)));
        if (tvMicLevel != null) tvMicLevel.setText(Math.max(0, Math.min(100, pct)) + "%");
    }

    private void stopListeningUI() {
        if (getView() == null) return;
        tvListening.setVisibility(View.GONE);
        showMicLevelUi(false);
        boolean anyOnline = useAnyOnlineSpeech();
        boolean offlineOk = mVoskReady && mVoskModel != null;
        boolean sys = useSystemOnlineSpeech();
        boolean javaB = useJavaBackendSpeech();
        updateVoiceDiagnosticsUi(
                isSpeechRecognitionSupported(),
                VoiceBackendHelper.isNetworkAvailable(requireContext()),
                javaB ? "Online · Java backend" : (sys ? "Online · system" : "Offline"));
        tvStatusBadge.setText(anyOnline ? "Ready" : (offlineOk ? "Offline Ready" : "Offline Loading"));
        btnMic.setEnabled(anyOnline || offlineOk);
        if (javaB) {
            btnMic.setText("Tap to Speak");
        } else if (sys) {
            btnMic.setText("Hold to Speak");
        } else {
            btnMic.setText(!offlineOk ? "Wait — loading model…"
                    : (mOfflineListening ? "Stop Offline Listening" : "Start Offline Listening"));
        }
    }

    private void updateVoiceDiagnosticsUi(boolean recognizerAvailable, boolean networkAvailable, String mode) {
        if (tvVoiceDiagnostics == null) {
            return;
        }
        String recognizerText = recognizerAvailable ? "Yes" : "No";
        String networkText = networkAvailable ? "Connected" : "Disconnected";
        tvVoiceDiagnostics.setText("Recognizer: " + recognizerText
                + " | Network: " + networkText
                + " | Mode: " + mode);
    }

    private void toggleOfflineListening() {
        if (!mVoskReady || mVoskModel == null) {
            Toast.makeText(getActivity(),
                    "Model still loading. When the mic enables, tap Start Offline Listening. Or tap Check Voice Support to retry.",
                    Toast.LENGTH_LONG).show();
            return;
        }
        if (mOfflineListening) {
            stopOfflineListening();
            stopListeningUI();
            addRobotMessage("Offline listening stopped.");
        } else {
            startOfflineListening();
        }
    }

    private void initOfflineRecognizerAsync() {
        final int generation = mOfflineInitGeneration.incrementAndGet();
        final Context appContext = requireContext().getApplicationContext();
        mVoskReady = false;
        mVoskModel = null;
        mVoskModelPath = "";
        mVoskFromExternal = false;
        mVoskFromInternalVosk = false;
        mVoskFromBundled = false;

        new Thread(() -> {
            File loadDirForError = null;
            long modelLoadStartMs = 0L;
            try {
                try {
                    // #region agent log
                    LibVosk.setLogLevel(BuildConfig.DEBUG ? LogLevel.DEBUG : LogLevel.INFO);
                    // #endregion
                } catch (Throwable ignored) {
                }
                postOfflineUi(generation, () -> {
                    if (!useAnyOnlineSpeech()) {
                        tvVoiceCapability.setText("Preparing offline model…");
                        tvStatusBadge.setText("Loading…");
                        btnMic.setEnabled(false);
                        btnMic.setText("Wait — loading model…");
                    } else {
                        refreshVoiceCapabilityStatus(false);
                    }
                });

                File modelDir = resolveVoskModelDirectory(appContext);
                if (generation != mOfflineInitGeneration.get()) {
                    return;
                }
                if (modelDir == null || !looksLikeVoskModel(modelDir)) {
                    Log.e(TAG, "No valid Vosk model on device");
                    File extRoot = appContext.getExternalFilesDir(null);
                    String extPath = extRoot != null
                            ? new File(extRoot, VOSK_EXTERNAL_MODEL_FOLDER).getAbsolutePath()
                            : "(external files dir unavailable)";
                    String intPath = new File(appContext.getFilesDir(), VOSK_EXTERNAL_MODEL_FOLDER).getAbsolutePath();
                    String assetsHint = "app/src/main/assets/" + VOSK_ASSET_MODEL_FOLDER + "/";
                    toastMain("No Vosk model found. Bundle vosk-model-small-en-in-0.4 under:\n"
                            + assetsHint
                            + "\n(am/, conf/, graph/, ivector/), or deploy under:\n" + extPath + "\nor (adb) " + intPath
                            + "\nSee README_VOICE_MODEL.txt.");
                    mVoskReady = false;
                    mainRefresh(generation, false);
                    return;
                }
                postOfflineUi(generation, () -> {
                    if (!useAnyOnlineSpeech()) {
                        tvVoiceCapability.setText("Loading Vosk engine…");
                        tvStatusBadge.setText("Loading…");
                    } else {
                        refreshVoiceCapabilityStatus(false);
                    }
                });
                File loadDir;
                try {
                    loadDir = modelDir.getCanonicalFile();
                } catch (IOException e) {
                    loadDir = modelDir;
                }
                loadDirForError = loadDir;
                logVoskModelProbe(loadDir);
                Log.i(TAG, "Vosk optional dirs total bytes: rescore="
                        + totalBytesInDirectoryTree(new File(loadDir, "rescore"), 6)
                        + " rnnlm="
                        + totalBytesInDirectoryTree(new File(loadDir, "rnnlm"), 6)
                        + " (late-stage load can OOM on low-RAM devices; see vosk-api#1401)");
                Log.i(TAG, "Loading Vosk from " + loadDir.getAbsolutePath());
                // #region agent log
                try {
                    agentDebugLog(appContext, "H1_H2_H5", "pre_model_ctor",
                            buildVoskLoadDiagnosticsJson(loadDir, appContext));
                } catch (Throwable ignored) {
                }
                modelLoadStartMs = SystemClock.elapsedRealtime();
                // #endregion
                Model model = new Model(loadDir.getAbsolutePath());
                if (generation != mOfflineInitGeneration.get()) {
                    model.close();
                    return;
                }
                mVoskModel = model;
                mVoskModelPath = loadDir.getAbsolutePath();
                File extRoot = appContext.getExternalFilesDir(null);
                File filesDir = appContext.getFilesDir();
                mVoskFromBundled = false;
                mVoskFromExternal = false;
                mVoskFromInternalVosk = false;
                try {
                    String mp = loadDir.getCanonicalPath();
                    String bundledBase = new File(filesDir, VOSK_BUNDLED_INTERNAL_DIR).getCanonicalPath();
                    if (mp.equals(bundledBase) || mp.startsWith(bundledBase + File.separator)) {
                        mVoskFromBundled = true;
                    } else if (extRoot != null) {
                        String extBase = new File(extRoot, VOSK_EXTERNAL_MODEL_FOLDER).getCanonicalPath();
                        mVoskFromExternal = mp.equals(extBase) || mp.startsWith(extBase + File.separator);
                    }
                    if (!mVoskFromBundled) {
                        String intBase = new File(filesDir, VOSK_EXTERNAL_MODEL_FOLDER).getCanonicalPath();
                        mVoskFromInternalVosk = mp.equals(intBase) || mp.startsWith(intBase + File.separator);
                    }
                } catch (IOException e) {
                    String ap = loadDir.getAbsolutePath();
                    String bundledBase = new File(filesDir, VOSK_BUNDLED_INTERNAL_DIR).getAbsolutePath();
                    if (ap.startsWith(bundledBase)) {
                        mVoskFromBundled = true;
                    } else {
                        mVoskFromExternal = extRoot != null
                                && ap.startsWith(new File(extRoot, VOSK_EXTERNAL_MODEL_FOLDER).getAbsolutePath());
                        mVoskFromInternalVosk = ap.startsWith(
                                new File(filesDir, VOSK_EXTERNAL_MODEL_FOLDER).getAbsolutePath());
                    }
                }
                File legacyDir = new File(filesDir, VOSK_LEGACY_INTERNAL_CACHE_DIR);
                boolean fromLegacy;
                try {
                    fromLegacy = loadDir.getCanonicalFile().equals(legacyDir.getCanonicalFile())
                            || loadDir.getCanonicalPath().startsWith(legacyDir.getCanonicalPath() + File.separator);
                } catch (IOException e) {
                    fromLegacy = loadDir.equals(legacyDir);
                }
                if (!fromLegacy && removeInternalPackagedModelCache(appContext)) {
                    toastMain("Removed duplicate voice model from internal storage to free space.");
                }
                mVoskReady = true;
                Log.i(TAG, "Vosk model ready");
                mainRefresh(generation, true);
            } catch (Throwable t) {
                // #region agent log
                try {
                    JSONObject failData = buildVoskLoadDiagnosticsJson(loadDirForError != null
                            ? loadDirForError : new File("."), appContext);
                    failData.put("errorClass", t.getClass().getName());
                    failData.put("errorMsg", t.getMessage() != null ? t.getMessage() : "");
                    failData.put("durationMs", modelLoadStartMs > 0
                            ? SystemClock.elapsedRealtime() - modelLoadStartMs : -1);
                    agentDebugLog(appContext, "H3_H4", "model_ctor_failed", failData);
                } catch (Throwable ignored) {
                }
                // #endregion
                Log.e(TAG, "Failed to initialize offline recognizer", t);
                for (Throwable x = t; x != null; x = x.getCause()) {
                    Log.e(TAG, "init cause: " + x.getClass().getName() + ": " + x.getMessage());
                }
                mVoskReady = false;
                String why = t.getMessage();
                if (why == null || why.trim().isEmpty()) {
                    why = t.getClass().getSimpleName();
                }
                final File errDir = loadDirForError;
                String pathLine = errDir != null
                        ? "\nPath: " + errDir.getAbsolutePath()
                        : "";
                toastMain("Offline model error: " + why
                        + pathLine
                        + "\nTry: official mobile model vosk-model-small-en-in-0.4 (~36 MB) in the same vosk-model folder, or re-copy en-in-0.5 from PC."
                        + "\nDebug APK: logcat *:S VoiceFragment:V Vosk:V — see native Vosk lines."
                        + "\nSee README_VOICE_MODEL.txt.");
                mainRefresh(generation, false);
            }
        }, "vosk-init").start();
    }

    private void mainRefresh(int generation, boolean announce) {
        new Handler(Looper.getMainLooper()).post(() -> {
            if (generation != mOfflineInitGeneration.get() || getView() == null) return;
            refreshVoiceCapabilityStatus(announce);
        });
    }

    private void postOfflineUi(int generation, Runnable r) {
        new Handler(Looper.getMainLooper()).post(() -> {
            if (generation != mOfflineInitGeneration.get() || getView() == null) return;
            r.run();
        });
    }

    /**
     * When using {@link #VOSK_EXTERNAL_MODEL_FOLDER}, delete the old internal copy under
     * {@code files/}{@link #VOSK_LEGACY_INTERNAL_CACHE_DIR} (from a previous APK install) to free space.
     *
     * @return true if a directory was removed
     */
    private static boolean removeInternalPackagedModelCache(Context appContext) {
        File internal = new File(appContext.getFilesDir(), VOSK_LEGACY_INTERNAL_CACHE_DIR);
        if (!internal.isDirectory()) {
            return false;
        }
        Log.i(TAG, "Removing internal packaged model cache (external model in use): "
                + internal.getAbsolutePath());
        deleteRecursiveQuietly(internal);
        return true;
    }

    private static void deleteRecursiveQuietly(File f) {
        if (f == null || !f.exists()) return;
        if (f.isDirectory()) {
            File[] kids = f.listFiles();
            if (kids != null) {
                for (File k : kids) {
                    deleteRecursiveQuietly(k);
                }
            }
        }
        //noinspection ResultOfMethodCallIgnored
        f.delete();
    }

    private void toastMain(String msg) {
        new Handler(Looper.getMainLooper()).post(() -> {
            if (getActivity() != null) {
                Toast.makeText(getActivity(), msg, Toast.LENGTH_LONG).show();
            }
        });
    }

    /**
     * Avoid calling native {@link Model} with only placeholder files — that can crash the process.
     */
    private static boolean looksLikeVoskModel(File modelDir) {
        if (modelDir == null || !modelDir.isDirectory()) return false;
        File conf = new File(modelDir, "conf");
        File am = new File(modelDir, "am");
        if (!conf.isDirectory() || !am.isDirectory()) return false;
        // Require a real acoustic model (.mdl), not a stub (native load fails on tiny files)
        File[] amFiles = am.listFiles();
        if (amFiles == null || amFiles.length == 0) return false;
        long largestMdl = 0;
        for (File f : amFiles) {
            if (f.isFile() && f.getName().endsWith(".mdl")) {
                largestMdl = Math.max(largestMdl, f.length());
            }
        }
        if (largestMdl < 2_000_000L || !new File(conf, "mfcc.conf").isFile()) {
            return false;
        }
        File graph = new File(modelDir, "graph");
        if (!graph.isDirectory()) {
            return false;
        }
        File[] gFiles = graph.listFiles();
        if (gFiles == null || gFiles.length == 0) {
            return false;
        }
        if (!graphLooksPlausible(graph)) {
            return false;
        }
        File ivector = new File(modelDir, "ivector");
        if (!ivector.isDirectory()) {
            return false;
        }
        File[] iv = ivector.listFiles();
        return iv != null && iv.length > 0;
    }

    /**
     * Native Vosk needs a real OpenFST graph (e.g. {@code HCLG.fst}). Empty or stub {@code graph/}
     * folders pass a naive "non-empty" check but still make {@link Model} fail.
     */
    private static boolean graphLooksPlausible(File graphDir) {
        File[] files = graphDir.listFiles();
        if (files == null) {
            return false;
        }
        for (File f : files) {
            if (!f.isFile()) {
                continue;
            }
            String n = f.getName().toLowerCase(Locale.US);
            if (n.endsWith(".fst") && f.length() >= 4096L) {
                return true;
            }
        }
        return false;
    }

    private static void logVoskModelProbe(File modelRoot) {
        if (modelRoot == null || !modelRoot.isDirectory()) {
            return;
        }
        File am = new File(modelRoot, "am");
        File[] amFiles = am.listFiles();
        long largestMdl = 0;
        String largestName = "";
        if (amFiles != null) {
            for (File f : amFiles) {
                if (f.isFile() && f.getName().endsWith(".mdl")) {
                    if (f.length() > largestMdl) {
                        largestMdl = f.length();
                        largestName = f.getName();
                    }
                }
            }
        }
        File graph = new File(modelRoot, "graph");
        int fstCount = 0;
        long largestFst = 0;
        File[] g = graph.listFiles();
        if (g != null) {
            for (File f : g) {
                if (f.isFile() && f.getName().toLowerCase(Locale.US).endsWith(".fst")) {
                    fstCount++;
                    largestFst = Math.max(largestFst, f.length());
                }
            }
        }
        Log.i(TAG, "Vosk probe: am largest .mdl=" + largestName + " (" + largestMdl + " bytes); "
                + "graph .fst files=" + fstCount + " largest=" + largestFst + " bytes");
    }

    // #region agent log
    /**
     * NDJSON debug line for session analysis (hypotheses H1–H5: memory, conf/ivector, JNI/format, corruption, ABI).
     * File: {@link #AGENT_DEBUG_LOG_NAME} under app external files; pull with adb to workspace.
     */
    private static void agentDebugLog(Context ctx, String hypothesisId, String message, JSONObject data) {
        if (ctx == null) {
            return;
        }
        try {
            JSONObject o = new JSONObject();
            o.put("sessionId", AGENT_DEBUG_SESSION);
            o.put("hypothesisId", hypothesisId);
            o.put("timestamp", System.currentTimeMillis());
            o.put("location", "VoiceFragment");
            o.put("message", message);
            o.put("data", data);
            String line = o.toString() + "\n";
            Log.i(TAG, "AGENT_DEBUG " + line.trim());
            File ext = ctx.getExternalFilesDir(null);
            if (ext != null) {
                File f = new File(ext, AGENT_DEBUG_LOG_NAME);
                FileOutputStream fos = new FileOutputStream(f, true);
                fos.write(line.getBytes(StandardCharsets.UTF_8));
                fos.close();
            }
        } catch (Throwable ignored) {
        }
    }

    private static JSONObject buildVoskLoadDiagnosticsJson(File loadDir, Context appContext) {
        JSONObject d = new JSONObject();
        try {
            d.put("voskAndroidAar", "0.3.70");
            d.put("apiLevel", Build.VERSION.SDK_INT);
            if (Build.SUPPORTED_ABIS != null && Build.SUPPORTED_ABIS.length > 0) {
                d.put("abi0", Build.SUPPORTED_ABIS[0]);
                org.json.JSONArray abis = new org.json.JSONArray();
                for (String a : Build.SUPPORTED_ABIS) {
                    abis.put(a);
                }
                d.put("supportedAbis", abis);
            }
            Runtime rt = Runtime.getRuntime();
            d.put("maxMemoryMb", rt.maxMemory() / 1024L / 1024L);
            d.put("totalMemoryMb", rt.totalMemory() / 1024L / 1024L);
            d.put("freeMemoryMb", rt.freeMemory() / 1024L / 1024L);
            if (loadDir == null || !loadDir.isDirectory()) {
                d.put("modelPathOk", false);
                return d;
            }
            d.put("modelPathOk", true);
            d.put("modelPath", loadDir.getAbsolutePath());
            try {
                d.put("canonicalPath", loadDir.getCanonicalPath());
            } catch (IOException e) {
                d.put("canonicalPath", loadDir.getAbsolutePath());
            }
            File mfcc = new File(loadDir, "conf/mfcc.conf");
            d.put("mfccExists", mfcc.isFile());
            d.put("mfccLen", mfcc.isFile() ? mfcc.length() : -1);
            File confDir = new File(loadDir, "conf");
            File[] confKids = confDir.listFiles();
            if (confKids != null) {
                org.json.JSONArray names = new org.json.JSONArray();
                for (File c : confKids) {
                    names.put(c.getName());
                }
                d.put("confChildren", names);
            }
            File ivDir = new File(loadDir, "ivector");
            long ivSum = 0;
            int ivCount = 0;
            if (ivDir.isDirectory()) {
                File[] ivf = ivDir.listFiles();
                if (ivf != null) {
                    for (File f : ivf) {
                        if (f.isFile()) {
                            ivCount++;
                            ivSum += f.length();
                        }
                    }
                }
            }
            d.put("ivectorFileCount", ivCount);
            d.put("ivectorBytesSum", ivSum);
            File am = new File(loadDir, "am");
            long largestMdl = 0;
            String largestMdlName = "";
            File[] amFiles = am.listFiles();
            if (amFiles != null) {
                for (File f : amFiles) {
                    if (f.isFile() && f.getName().endsWith(".mdl")) {
                        if (f.length() > largestMdl) {
                            largestMdl = f.length();
                            largestMdlName = f.getName();
                        }
                    }
                }
            }
            d.put("amLargestMdlBytes", largestMdl);
            d.put("amLargestMdlName", largestMdlName);
            File graph = new File(loadDir, "graph");
            long largestFst = 0;
            int fstCount = 0;
            File[] g = graph.listFiles();
            if (g != null) {
                for (File f : g) {
                    if (f.isFile() && f.getName().toLowerCase(Locale.US).endsWith(".fst")) {
                        fstCount++;
                        largestFst = Math.max(largestFst, f.length());
                    }
                }
            }
            d.put("graphFstCount", fstCount);
            d.put("graphLargestFstBytes", largestFst);
            // Optional dirs: native load can fail late (e.g. rnnlm) on low-RAM devices — see vosk-api#1401
            File rescoreDir = new File(loadDir, "rescore");
            File rnnlmDir = new File(loadDir, "rnnlm");
            d.put("hasRescoreDir", rescoreDir.isDirectory());
            d.put("hasRnnlmDir", rnnlmDir.isDirectory());
            d.put("rescoreBytesRecursive", totalBytesInDirectoryTree(rescoreDir, 6));
            d.put("rnnlmBytesRecursive", totalBytesInDirectoryTree(rnnlmDir, 6));
        } catch (JSONException e) {
            try {
                d.put("diagJsonError", e.getMessage());
            } catch (JSONException ignored) {
            }
        }
        return d;
    }

    /** Sum of file sizes under {@code root} (recursive), depth-limited. */
    private static long totalBytesInDirectoryTree(File root, int maxDepth) {
        if (root == null || !root.isDirectory() || maxDepth < 0) {
            return 0L;
        }
        long sum = 0;
        File[] kids = root.listFiles();
        if (kids == null) {
            return 0L;
        }
        for (File f : kids) {
            if (f.isFile()) {
                sum += f.length();
            } else if (f.isDirectory()) {
                sum += totalBytesInDirectoryTree(f, maxDepth - 1);
            }
        }
        return sum;
    }
    // #endregion

    private static boolean isVoskInteriorDirName(String dirName) {
        if (dirName == null) {
            return false;
        }
        String n = dirName.toLowerCase(Locale.US);
        return "am".equals(n) || "conf".equals(n) || "graph".equals(n) || "ivector".equals(n);
    }

    /**
     * Breadth-first search under {@code root} for the shallowest directory that passes
     * {@link #looksLikeVoskModel(File)}. Handles extra zip wrapper folders (nested subfolders).
     * Does not descend into {@code am/}, {@code conf/}, {@code graph/}, or {@code ivector/} to
     * avoid walking huge trees.
     */
    @Nullable
    private static File findVoskModelRootUnder(File root) {
        if (root == null || !root.isDirectory()) {
            return null;
        }
        ArrayDeque<AbstractMap.SimpleEntry<File, Integer>> q = new ArrayDeque<>();
        q.add(new AbstractMap.SimpleEntry<>(root, 0));
        int visited = 0;
        while (!q.isEmpty() && visited < VOSK_MODEL_SEARCH_MAX_DIRS) {
            AbstractMap.SimpleEntry<File, Integer> e = q.removeFirst();
            File dir = e.getKey();
            int depth = e.getValue();
            visited++;
            if (looksLikeVoskModel(dir)) {
                if (!dir.equals(root)) {
                    Log.i(TAG, "Using nested model folder (depth " + depth + "): "
                            + dir.getAbsolutePath());
                }
                return dir;
            }
            if (depth >= VOSK_MODEL_SEARCH_MAX_DEPTH) {
                continue;
            }
            if (isVoskInteriorDirName(dir.getName())) {
                continue;
            }
            File[] kids = dir.listFiles();
            if (kids == null) {
                continue;
            }
            Arrays.sort(kids, (a, b) -> a.getName().compareToIgnoreCase(b.getName()));
            for (File k : kids) {
                if (k.isDirectory() && !k.getName().startsWith(".")) {
                    q.add(new AbstractMap.SimpleEntry<>(k, depth + 1));
                }
            }
        }
        if (visited >= VOSK_MODEL_SEARCH_MAX_DIRS) {
            Log.w(TAG, "Vosk model search stopped after " + VOSK_MODEL_SEARCH_MAX_DIRS
                    + " dirs under " + root.getAbsolutePath());
        }
        return null;
    }

    /**
     * Opens a recording stream for Vosk. Robot / tablet builds often route
     * {@link MediaRecorder.AudioSource#VOICE_RECOGNITION} to a silent or over-processed path;
     * try {@link MediaRecorder.AudioSource#MIC} and {@link MediaRecorder.AudioSource#VOICE_COMMUNICATION} first.
     */
    @Nullable
    private AudioRecord openAudioRecordForVosk(int recordBufferBytes) {
        int[] sources = new int[]{
                MediaRecorder.AudioSource.MIC,
                MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                MediaRecorder.AudioSource.VOICE_RECOGNITION,
                MediaRecorder.AudioSource.DEFAULT,
                MediaRecorder.AudioSource.CAMCORDER,
        };
        for (int src : sources) {
            AudioRecord r = null;
            try {
                r = new AudioRecord(
                        src,
                        VOSK_SAMPLE_RATE,
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
                Log.i(TAG, "Offline mic: using AudioSource=" + src + " recordBufferBytes=" + recordBufferBytes);
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

    private static int pcm16PeakShorts(short[] buffer, int nShorts) {
        int max = 0;
        for (int i = 0; i < nShorts; i++) {
            int a = Math.abs((int) buffer[i]);
            if (a > max) {
                max = a;
            }
        }
        return max;
    }

    /**
     * Only the latest offline session may clear {@link #mOfflineListening} and the mic UI.
     */
    private void finishOfflineListeningSessionUi(int finishedSessionId) {
        new Handler(Looper.getMainLooper()).post(() -> {
            if (finishedSessionId != mOfflineListenSession.get()) {
                return;
            }
            mOfflineListening = false;
            stopListeningUI();
        });
    }

    private void startOfflineListening() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(requireContext(),
                    "Allow microphone access in Android Settings to use offline listening.",
                    Toast.LENGTH_LONG).show();
            return;
        }
        stopOfflineListening();
        final int sessionId = mOfflineListenSession.incrementAndGet();
        mOfflineListening = true;
        tvListening.setVisibility(View.VISIBLE);
        tvListening.setText("Listening… (speak, then pause briefly)");
        tvStatusBadge.setText("Offline Listening...");
        btnMic.setText("Stop Offline Listening");
        showMicLevelUi(true);
        updateMicLevelUi(0);
        addRobotMessage("Offline listening started. First ~1 s calibrates background noise so the meter stays quiet; then speak and pause.");

        mOfflineThread = new Thread(() -> {
            AudioRecord recorder = null;
            Recognizer recognizer = null;
            final int mySession = sessionId;
            Handler mainHandler = new Handler(Looper.getMainLooper());
            try {
                int minBytes = AudioRecord.getMinBufferSize(
                        VOSK_SAMPLE_RATE,
                        AudioFormat.CHANNEL_IN_MONO,
                        AudioFormat.ENCODING_PCM_16BIT
                );
                if (minBytes <= 0) {
                    postRobotMessage("Offline recorder init failed.");
                    return;
                }
                int chunkSamples = Math.round(VOSK_SAMPLE_RATE * VOSK_READ_CHUNK_SEC);
                int chunkBytes = chunkSamples * 2;
                int recordBufferBytes = Math.max(minBytes, chunkBytes);

                recorder = openAudioRecordForVosk(recordBufferBytes);
                if (recorder == null) {
                    postRobotMessage("Could not open microphone. Check permission and that no other app uses the mic.");
                    return;
                }
                try {
                    recognizer = new Recognizer(mVoskModel, (float) VOSK_SAMPLE_RATE);
                } catch (IOException e) {
                    Log.e(TAG, "Vosk Recognizer failed", e);
                    postRobotMessage("Vosk recognizer could not start.");
                    return;
                }

                short[] buffer = new short[chunkSamples];
                long lastUiPostMs = 0;
                int readErrorStreak = 0;
                boolean loggedInputPeak = false;
                long calibrateUntilMs = SystemClock.uptimeMillis() + VOSK_NOISE_CALIBRATE_MS;
                int noisePeak = 450;
                boolean noiseFloorLogged = false;
                while (mOfflineListening) {
                    int nread = recorder.read(buffer, 0, buffer.length);
                    if (nread < 0) {
                        readErrorStreak++;
                        Log.w(TAG, "AudioRecord.read returned " + nread + " streak=" + readErrorStreak);
                        if (readErrorStreak > 15) {
                            postRobotMessage("Microphone read failed. Try again or reboot the device.");
                            break;
                        }
                        continue;
                    }
                    readErrorStreak = 0;
                    if (nread == 0) {
                        continue;
                    }

                    int peakRaw = pcm16PeakShorts(buffer, nread);
                    long now = SystemClock.uptimeMillis();
                    if (now < calibrateUntilMs) {
                        noisePeak = Math.max(noisePeak, peakRaw);
                        Arrays.fill(buffer, 0, nread, (short) 0);
                    } else {
                        if (!noiseFloorLogged) {
                            noiseFloorLogged = true;
                            Log.i(TAG, "Offline mic: noise floor estimate (PCM peak) ~" + noisePeak);
                        }
                        int floor = Math.max(320, Math.min(5200, (int) (noisePeak * 1.18f + 80)));
                        if (peakRaw < floor + VOSK_GATE_MARGIN_PCM) {
                            Arrays.fill(buffer, 0, nread, (short) 0);
                        }
                    }

                    if (!loggedInputPeak && peakRaw > 2000) {
                        loggedInputPeak = true;
                        Log.i(TAG, "Offline mic: strong speech peak ~" + peakRaw);
                    }

                    boolean utteranceEnd = recognizer.acceptWaveForm(buffer, nread);
                    if (now - lastUiPostMs >= 220) {
                        lastUiPostMs = now;
                        if (now < calibrateUntilMs) {
                            mainHandler.post(() -> {
                                if (!mOfflineListening || getView() == null) {
                                    return;
                                }
                                tvListening.setText("Listening… calibrating background noise (stay quiet)…");
                            });
                        } else {
                            int floor = Math.max(320, Math.min(5200, (int) (noisePeak * 1.18f + 80)));
                            int adj = Math.max(0, peakRaw - floor);
                            int denom = Math.max(1200, 8000 - floor);
                            final int levelPct = Math.min(100, adj * 100 / denom);
                            final String partial = utteranceEnd
                                    ? ""
                                    : parseVoskPartial(recognizer.getPartialResult());
                            mainHandler.post(() -> {
                                if (!mOfflineListening || getView() == null) {
                                    return;
                                }
                                updateMicLevelUi(levelPct);
                                if (partial != null && !partial.isEmpty()) {
                                    tvListening.setText("Listening… " + partial);
                                } else {
                                    tvListening.setText("Listening… (above noise floor) — speak, then pause.");
                                }
                            });
                        }
                    }

                    if (utteranceEnd) {
                        String finalText = parseVoskText(recognizer.getResult());
                        if (finalText != null && !finalText.isEmpty()) {
                            final String text = finalText;
                            mainHandler.post(() -> processUserSpeech(text));
                        }
                    }
                }

                String tail = parseVoskText(recognizer.getFinalResult());
                if (tail != null && !tail.isEmpty()) {
                    final String text = tail;
                    mainHandler.post(() -> processUserSpeech(text));
                }
            } catch (Throwable t) {
                Log.e(TAG, "Offline listening failure", t);
                postRobotMessage("Offline listening error.");
            } finally {
                if (recorder != null) {
                    try {
                        recorder.stop();
                    } catch (Exception ignored) { }
                    recorder.release();
                }
                if (recognizer != null) {
                    recognizer.close();
                }
                finishOfflineListeningSessionUi(mySession);
            }
        }, "vosk-listen");
        mOfflineThread.start();
    }

    private void stopOfflineListening() {
        mOfflineListening = false;
        if (mOfflineThread != null) {
            try {
                mOfflineThread.join(2000);
            } catch (InterruptedException ignored) { }
            mOfflineThread = null;
        }
    }

    private String parseVoskText(String jsonText) {
        if (jsonText == null || jsonText.trim().isEmpty()) return null;
        try {
            JSONObject json = new JSONObject(jsonText);
            String text = json.optString("text");
            if (text == null || text.trim().isEmpty()) {
                text = json.optString("partial");
            }
            return text == null ? null : text.trim();
        } catch (JSONException e) {
            return null;
        }
    }

    /** Live hypothesis while speaking; JSON uses {@code "partial"} — not mixed into finals. */
    private static String parseVoskPartial(String jsonText) {
        if (jsonText == null || jsonText.trim().isEmpty()) {
            return null;
        }
        try {
            JSONObject json = new JSONObject(jsonText);
            String text = json.optString("partial");
            return text == null || text.trim().isEmpty() ? null : text.trim();
        } catch (JSONException e) {
            return null;
        }
    }

    private void postRobotMessage(String msg) {
        new Handler(Looper.getMainLooper()).post(() -> {
            if (!isAdded() || getView() == null) return;
            addRobotMessage(msg);
        });
    }

    /**
     * Resolution order: (1) APK assets {@link #VOSK_ASSET_MODEL_FOLDER} (copied to internal storage),
     * (2) external {@link #VOSK_EXTERNAL_MODEL_FOLDER}, (3) internal {@link #VOSK_EXTERNAL_MODEL_FOLDER},
     * (4) legacy {@link #VOSK_LEGACY_INTERNAL_CACHE_DIR}.
     */
    @Nullable
    private File resolveVoskModelDirectory(Context appContext) {
        File fromAssets = prepareBundledVoskFromAssets(appContext);
        if (fromAssets != null) {
            Log.i(TAG, "Vosk: using APK-bundled model at " + fromAssets.getAbsolutePath());
            return fromAssets;
        }
        File extRoot = appContext.getExternalFilesDir(null);
        if (extRoot != null) {
            File ext = new File(extRoot, VOSK_EXTERNAL_MODEL_FOLDER);
            // Android creates Android/data/<package>/files/ when the app runs; create vosk-model
            // so PC/USB file managers show an empty drop target (user still copies am/conf/graph/ivector).
            if (!ext.exists()) {
                //noinspection ResultOfMethodCallIgnored
                ext.mkdirs();
            }
            File resolved = findVoskModelRootUnder(ext);
            if (resolved != null) {
                Log.i(TAG, "Vosk: using external model at " + resolved.getAbsolutePath());
                return resolved;
            }
        }
        File internalVosk = new File(appContext.getFilesDir(), VOSK_EXTERNAL_MODEL_FOLDER);
        if (!internalVosk.exists()) {
            //noinspection ResultOfMethodCallIgnored
            internalVosk.mkdirs();
        }
        File resolved = findVoskModelRootUnder(internalVosk);
        if (resolved != null) {
            Log.i(TAG, "Vosk: using internal files model at " + resolved.getAbsolutePath());
            return resolved;
        }
        File cached = new File(appContext.getFilesDir(), VOSK_LEGACY_INTERNAL_CACHE_DIR);
        resolved = findVoskModelRootUnder(cached);
        if (resolved != null) {
            Log.i(TAG, "Vosk: using legacy internal cache at " + resolved.getAbsolutePath());
            return resolved;
        }
        if (cached.exists()) {
            Log.w(TAG, "Vosk: wiping invalid legacy internal cache " + cached.getAbsolutePath());
            deleteRecursiveQuietly(cached);
        }
        return null;
    }

    /** True if assets contain the expected top-level model folders (am + conf). */
    private static boolean hasBundledAssetModelTree(Context appContext) {
        try {
            String[] list = appContext.getAssets().list(VOSK_ASSET_MODEL_FOLDER);
            if (list == null || list.length == 0) {
                return false;
            }
            boolean hasAm = false;
            boolean hasConf = false;
            for (String name : list) {
                if ("am".equals(name)) {
                    hasAm = true;
                }
                if ("conf".equals(name)) {
                    hasConf = true;
                }
            }
            return hasAm && hasConf;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Copies {@link #VOSK_ASSET_MODEL_FOLDER} from assets to {@link #VOSK_BUNDLED_INTERNAL_DIR} if needed.
     * Native Vosk needs a real filesystem path; assets are packaged inside the APK.
     */
    @Nullable
    private static File prepareBundledVoskFromAssets(Context appContext) {
        if (!hasBundledAssetModelTree(appContext)) {
            return null;
        }
        File destRoot = new File(appContext.getFilesDir(), VOSK_BUNDLED_INTERNAL_DIR + "/" + VOSK_ASSET_MODEL_FOLDER);
        if (looksLikeVoskModel(destRoot)) {
            return destRoot;
        }
        deleteRecursiveQuietly(destRoot);
        File parent = destRoot.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            Log.e(TAG, "Vosk bundled: could not mkdir " + parent.getAbsolutePath());
            return null;
        }
        try {
            copyAssetPath(appContext.getAssets(), VOSK_ASSET_MODEL_FOLDER, destRoot);
        } catch (IOException e) {
            Log.e(TAG, "Vosk bundled: copy from assets failed", e);
            deleteRecursiveQuietly(destRoot);
            return null;
        }
        if (!looksLikeVoskModel(destRoot)) {
            Log.e(TAG, "Vosk bundled: extracted tree failed validation");
            deleteRecursiveQuietly(destRoot);
            return null;
        }
        return destRoot;
    }

    /**
     * Recursively copies a file or directory from assets. {@code assetPath} uses {@code /} separators.
     */
    private static void copyAssetPath(AssetManager am, String assetPath, File dest) throws IOException {
        String[] items = am.list(assetPath);
        if (items == null) {
            return;
        }
        if (items.length == 0) {
            File parent = dest.getParentFile();
            if (parent != null && !parent.exists() && !parent.mkdirs()) {
                throw new IOException("mkdir " + parent);
            }
            try (InputStream in = am.open(assetPath);
                 FileOutputStream out = new FileOutputStream(dest)) {
                byte[] buf = new byte[8192];
                int n;
                while ((n = in.read(buf)) != -1) {
                    out.write(buf, 0, n);
                }
            }
            return;
        }
        if (!dest.exists() && !dest.mkdirs()) {
            throw new IOException("mkdir " + dest);
        }
        for (String name : items) {
            if (".".equals(name) || "..".equals(name)) {
                continue;
            }
            String sub = assetPath + "/" + name;
            copyAssetPath(am, sub, new File(dest, name));
        }
    }

    // ---- Greeting logic ----

    private void processUserSpeech(String spokenText) {
        if (spokenText == null) return;
        spokenText = VoiceTranscriptUtil.firstWordsForIntent(spokenText);
        if (spokenText.isEmpty()) {
            return;
        }
        addUserMessage(spokenText);

        String lower = normalizeForIntent(spokenText);
        String reply;
        final boolean charging = isChargingIntent(lower);
        final boolean stopCmd = !charging && isStopIntent(lower);
        final int direction = (!charging && !stopCmd) ? parseDirectionIntent(lower) : DIRECTION_NONE;

        // Intent refinement: map common mis-recognitions (e.g. "chargin" -> "charging")
        // into a stable set of robot intents.
        if (charging) {
            reply = "Opening charging.";
        } else if (stopCmd) {
            triggerRobotStop();
            reply = "Stopping.";
        } else if (direction != DIRECTION_NONE) {
            triggerRobotMove(direction);
            reply = replyForDirection(direction);
        } else {
            String mathAnswer = tryComputeMathAnswer(spokenText);
            if (mathAnswer != null) {
                reply = mathAnswer;
            } else if (isHelloIntent(lower) || lower.contains("hi") || lower.contains("hey")) {
                reply = "Hello! How can I help you today?";
            } else if (lower.contains("how are you")) {
                reply = "I am fully charged and ready to serve!";
            } else if (lower.contains("your name") || lower.contains("who are you")) {
                reply = "I am your Keenon robot assistant!";
            } else if (lower.contains("bye") || lower.contains("goodbye")) {
                reply = "Goodbye! Have a great day!";
            } else {
                reply = "I heard: \"" + spokenText + "\". Try saying: charging, home, hello.";
            }
        }

        addRobotMessage(reply);
        if (charging) {
            speakChargingThenNavigate(reply);
        } else {
            speak(reply);
        }
    }

    // More robust "hello" detection for common mis-hears like:
    // "helo", "heelo", "helloo".
    private boolean isHelloIntent(String normalizedLower) {
        if (normalizedLower == null) return false;
        String norm = normalizedLower.trim();
        if (norm.isEmpty()) return false;

        if (norm.contains("hello")) return true;

        String[] tokens = norm.split("\\s+");
        for (String token : tokens) {
            if (token == null) continue;
            token = token.trim();
            if (token.isEmpty()) continue;

            if (token.equals("hello")) return true;
            if (token.startsWith("hell")) return true;

            // Tolerate small spelling mistakes.
            if (levenshteinDistance(token, "hello") <= 2) return true;
        }
        return false;
    }

    // ---- Simple local math (no internet needed) ----
    // Supports queries like: "what is 1+1", "calculate (12*3)+4", "2*3".
    private String tryComputeMathAnswer(String spokenText) {
        if (spokenText == null) return null;
        String t = spokenText.toLowerCase(Locale.US).trim();

        // Extract expression after common prefixes.
        // IMPORTANT: Java regex does not support Python-style named groups `(?P<name>...)`.
        // Using a normal capture group keeps the math parsing stable.
        java.util.regex.Matcher m;
        try {
            m = java.util.regex.Pattern.compile(
                    "(what\\s+is|calculate|compute)\\s*([0-9\\s\\+\\-\\*\\/\\%\\(\\)\\.]*)"
            ).matcher(t);
        } catch (Throwable ignored) {
            // If regex compilation ever fails, just disable math intent instead of crashing the app.
            return null;
        }
        String expr = null;
        if (m.find()) {
            expr = m.group(2); // the captured expression part
        } else {
            // If user said only an expression, try evaluate the whole thing.
            if (t.matches("[0-9\\s\\+\\-\\*\\/\\%\\(\\)\\.]+")) {
                expr = t;
            }
        }
        if (expr == null) return null;

        expr = expr.replaceAll("\\s+", "");
        if (expr.isEmpty()) return null;

        Double val = evaluateArithmeticExpression(expr);
        if (val == null) return null;

        if (Math.abs(val - Math.round(val)) < 1e-9) {
            return String.valueOf(Math.round(val));
        }
        return String.valueOf(val);
    }

    private Double evaluateArithmeticExpression(String expr) {
        try {
            return new ArithmeticParser(expr).parse();
        } catch (Throwable t) {
            return null;
        }
    }

    private static final class ArithmeticParser {
        private final String s;
        private int pos = 0;

        ArithmeticParser(String s) {
            this.s = s;
        }

        Double parse() {
            double v = parseExpression();
            if (pos != s.length()) return null;
            return v;
        }

        private double parseExpression() {
            double v = parseTerm();
            while (pos < s.length()) {
                char c = s.charAt(pos);
                if (c == '+') {
                    pos++;
                    v += parseTerm();
                } else if (c == '-') {
                    pos++;
                    v -= parseTerm();
                } else {
                    break;
                }
            }
            return v;
        }

        private double parseTerm() {
            double v = parseFactor();
            while (pos < s.length()) {
                char c = s.charAt(pos);
                if (c == '*') {
                    pos++;
                    v *= parseFactor();
                } else if (c == '/') {
                    pos++;
                    v /= parseFactor();
                } else if (c == '%') {
                    pos++;
                    v %= parseFactor();
                } else {
                    break;
                }
            }
            return v;
        }

        private double parseFactor() {
            if (pos >= s.length()) return 0;
            char c = s.charAt(pos);

            // unary +/- operators
            if (c == '+') {
                pos++;
                return parseFactor();
            }
            if (c == '-') {
                pos++;
                return -parseFactor();
            }

            if (c == '(') {
                pos++;
                double v = parseExpression();
                if (pos < s.length() && s.charAt(pos) == ')') {
                    pos++;
                    return v;
                }
                throw new IllegalArgumentException("Missing ')'");
            }

            // number
            int start = pos;
            boolean hasDot = false;
            while (pos < s.length()) {
                char ch = s.charAt(pos);
                if (ch >= '0' && ch <= '9') {
                    pos++;
                    continue;
                }
                if (ch == '.') {
                    if (hasDot) break;
                    hasDot = true;
                    pos++;
                    continue;
                }
                break;
            }
            if (start == pos) {
                throw new IllegalArgumentException("Expected number at pos " + pos);
            }
            return Double.parseDouble(s.substring(start, pos));
        }
    }

    private String normalizeForIntent(String spokenText) {
        String lower = spokenText.toLowerCase(Locale.US).trim();
        // Keep only letters/numbers/spaces so phrase matching is robust to punctuation.
        return lower.replaceAll("[^a-z0-9\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private boolean isChargingIntent(String normalizedLower) {
        if (normalizedLower == null || normalizedLower.isEmpty()) return false;

        // Fast substring checks for typical cases.
        if (normalizedLower.contains("charging") || normalizedLower.contains("charger")
                || normalizedLower.contains("charge")) {
            return true;
        }

        // Handle cases where Vosk splits words (e.g., "charg ing").
        String compact = normalizedLower.replace(" ", "");
        if (compact.contains("charging") || compact.contains("charge")) return true;

        // Fuzzy token check for common misspellings (e.g., "chargin").
        String[] tokens = normalizedLower.split("\\s+");
        for (String token : tokens) {
            if (token.startsWith("charg")) return true;
            if (token.length() < 4) continue;

            // Allow small edit distance: charging ~ chargin/chargng.
            if (levenshteinDistance(token, "charging") <= 2) return true;
            if (levenshteinDistance(token, "charge") <= 1) return true;
        }
        return false;
    }

    // ---- Movement voice intents ------------------------------------------------

    /** Sentinel returned by {@link #parseDirectionIntent(String)} when no direction matches. */
    private static final int DIRECTION_NONE = Integer.MIN_VALUE;
    /** How long a single voice-nudge moves the robot before auto-stopping. */
    private static final long VOICE_MOVE_BURST_MS = 650L;
    /** Debounce repeat bursts from Whisper partials / double-taps. */
    private long mLastMoveAtMs = 0L;

    /**
     * Maps common direction words (including joystick-style "up/down") to a
     * {@link ApiConstants.MotorMove} constant. Returns {@link #DIRECTION_NONE} if nothing matches.
     */
    private int parseDirectionIntent(String normalizedLower) {
        if (normalizedLower == null || normalizedLower.isEmpty()) return DIRECTION_NONE;
        String t = normalizedLower;

        // Forward — "forward", "ahead", "straight", "front", "up" (joystick up), "move up"
        if (t.contains("forward") || t.contains("straight")
                || t.contains("ahead") || t.contains("move up")
                || t.contains("go up") || t.equals("up")
                || t.startsWith("up ") || t.endsWith(" up")
                || t.contains(" front") || t.startsWith("front")) {
            return ApiConstants.MotorMove.FRONT;
        }
        // Backward — "backward", "back", "reverse", "down" (joystick)
        if (t.contains("backward") || t.contains("reverse")
                || t.contains("move back") || t.contains("go back")
                || t.contains("back up") || t.equals("back")
                || t.startsWith("back ") || t.endsWith(" back")
                || t.contains("move down") || t.contains("go down")
                || t.equals("down") || t.startsWith("down ") || t.endsWith(" down")) {
            return ApiConstants.MotorMove.BACK;
        }
        if (t.contains("right")) {
            return ApiConstants.MotorMove.RIGHT;
        }
        if (t.contains("left")) {
            return ApiConstants.MotorMove.LEFT;
        }
        return DIRECTION_NONE;
    }

    private boolean isStopIntent(String normalizedLower) {
        if (normalizedLower == null || normalizedLower.isEmpty()) return false;
        String t = normalizedLower;
        return t.equals("stop") || t.startsWith("stop ") || t.endsWith(" stop")
                || t.contains(" stop ")
                || t.contains("halt") || t.contains("pause")
                || t.contains("wait") || t.contains("freeze");
    }

    private String replyForDirection(int direction) {
        if (direction == ApiConstants.MotorMove.FRONT) return "Moving forward.";
        if (direction == ApiConstants.MotorMove.BACK) return "Moving back.";
        if (direction == ApiConstants.MotorMove.LEFT) return "Turning left.";
        if (direction == ApiConstants.MotorMove.RIGHT) return "Turning right.";
        return "Moving.";
    }

    /**
     * Issues a short burst move on a background thread, then schedules a stop.
     * Always re-acquires the motor lock before moving (the firmware / other activities can
     * release it), and surfaces motor errors to the chat if the robot refuses to drive.
     */
    private void triggerRobotMove(final int direction) {
        long now = SystemClock.uptimeMillis();
        if (now - mLastMoveAtMs < 400) return; // debounce double-fires
        mLastMoveAtMs = now;

        new Thread(() -> {
            final java.util.concurrent.atomic.AtomicBoolean motorError =
                    new java.util.concurrent.atomic.AtomicBoolean(false);
            final java.util.concurrent.atomic.AtomicReference<String> errorMsg =
                    new java.util.concurrent.atomic.AtomicReference<>("");
            RobotMovementManager mgr = RobotMovementManager.getInstance();
            RobotMovementManager.Listener prev = null;
            try {
                // Listener: catches errors from motor().manual() heartbeats (the SDK fires
                // an error if the motor is not unlocked / safety is engaged).
                RobotMovementManager.Listener voiceListener = new RobotMovementManager.Listener() {
                    @Override public void onMovementStarted(int d) {}
                    @Override public void onMovementStopped() {}
                    @Override
                    public void onMovementError(String error) {
                        if (motorError.compareAndSet(false, true)) {
                            errorMsg.set(error);
                        }
                    }
                };
                mgr.setListener(voiceListener);

                // Always re-assert the motor lock — prepare() is a one-shot guarded by
                // isPrepared, but the firmware may have released the lock since then.
                mgr.ensureMotorEnabled();
                // Let the enable(UNLOCK) call settle before issuing manual() heartbeats.
                try {
                    Thread.sleep(120);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
                if (motorError.get()) {
                    postRobotMessage(
                            "Motor lock not acquired — robot did not move. "
                                    + "Check motor switch/power. (" + errorMsg.get() + ")");
                    return;
                }

                mgr.executeDirection(direction);

                // Run the burst, bail early if the motor callback reports an error.
                long burstEnd = SystemClock.uptimeMillis() + VOICE_MOVE_BURST_MS;
                while (SystemClock.uptimeMillis() < burstEnd) {
                    if (motorError.get()) break;
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException ignored) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }

                try {
                    mgr.stop();
                } catch (Throwable ignored) {
                }

                if (motorError.get()) {
                    postRobotMessage(
                            "Robot did not move — motor reported an error. "
                                    + "Lock may be released. (" + errorMsg.get() + ")");
                }
            } catch (Throwable t) {
                Log.e(TAG, "Voice move failed", t);
            } finally {
                try {
                    mgr.setListener(prev);
                } catch (Throwable ignored) {
                }
            }
        }, "voice-move").start();
    }

    private void triggerRobotStop() {
        new Thread(() -> {
            try {
                RobotMovementManager.getInstance().stop();
            } catch (Throwable t) {
                Log.w(TAG, "Voice stop failed", t);
            }
        }, "voice-stop").start();
    }

    private void triggerChargingAction() {
        if (!isAdded() || getActivity() == null) {
            return;
        }
        long now = SystemClock.uptimeMillis();
        if (now - mLastChargingActionAtMs < 5000) return; // debounce voice triggers
        mLastChargingActionAtMs = now;

        try {
            Intent intent = new Intent(requireContext(), ChargerDemo.class);
            intent.putExtra(ChargerDemo.EXTRA_START_AUTO_CHARGE, true);
            startActivity(intent);
        } catch (Throwable t) {
            Log.e(TAG, "Failed to open ChargerDemo", t);
        }
    }

    private static int levenshteinDistance(String a, String b) {
        if (a == null) a = "";
        if (b == null) b = "";
        if (a.equals(b)) return 0;
        if (a.isEmpty()) return b.length();
        if (b.isEmpty()) return a.length();
        if (Math.abs(a.length() - b.length()) > 3) return 999;

        int[] prev = new int[b.length() + 1];
        int[] curr = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++) prev[j] = j;

        for (int i = 1; i <= a.length(); i++) {
            curr[0] = i;
            char ca = a.charAt(i - 1);
            for (int j = 1; j <= b.length(); j++) {
                int cost = (ca == b.charAt(j - 1)) ? 0 : 1;
                curr[j] = Math.min(
                        Math.min(curr[j - 1] + 1, prev[j] + 1),
                        prev[j - 1] + cost
                );
            }
            int[] tmp = prev;
            prev = curr;
            curr = tmp;
        }
        return prev[b.length()];
    }

    // ---- Chat helpers ----

    private void addUserMessage(String text) {
        mMessages.add(new ChatMessage(text, true));
        mAdapter.notifyDataSetChanged();
        lvChat.setSelection(mMessages.size() - 1);
    }

    private void addRobotMessage(String text) {
        mMessages.add(new ChatMessage(text, false));
        mAdapter.notifyDataSetChanged();
        lvChat.setSelection(mMessages.size() - 1);
    }

    // ---- RecognitionListener ----

    private class SimpleRecognitionListener implements RecognitionListener {

        @Override
        public void onReadyForSpeech(Bundle params) {
            Log.d(TAG, "onReadyForSpeech");
        }

        @Override
        public void onBeginningOfSpeech() {
            Log.d(TAG, "onBeginningOfSpeech");
        }

        @Override
        public void onRmsChanged(float rmsdB) { }

        @Override
        public void onBufferReceived(byte[] buffer) { }

        @Override
        public void onEndOfSpeech() {
            Log.d(TAG, "onEndOfSpeech");
        }

        @Override
        public void onError(int error) {
            Log.e(TAG, "SpeechRecognizer error: " + error);
            if (mSpeechRecognizer != null) {
                try {
                    mSpeechRecognizer.destroy();
                } catch (Throwable ignored) {
                }
                mSpeechRecognizer = null;
            }
            stopListeningUI();
            String msg = errorMessage(error);
            addRobotMessage("Sorry, I could not hear you. " + msg);
        }

        @Override
        public void onResults(Bundle results) {
            if (mSpeechRecognizer != null) {
                try {
                    mSpeechRecognizer.destroy();
                } catch (Throwable ignored) {
                }
                mSpeechRecognizer = null;
            }
            stopListeningUI();
            ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            if (matches != null && !matches.isEmpty()) {
                // "Whisper-like" refinement: pick the best candidate among top hypotheses.
                String best = matches.get(0);
                int bestScore = Integer.MIN_VALUE;
                for (int i = 0; i < matches.size() && i < 3; i++) {
                    String cand = matches.get(i);
                    String norm = normalizeForIntent(cand);
                    int score = intentScore(norm);
                    if (score > bestScore) {
                        bestScore = score;
                        best = cand;
                    }
                }
                processUserSpeech(best);
            }
        }

        @Override
        public void onPartialResults(Bundle partialResults) { }

        @Override
        public void onEvent(int eventType, Bundle params) { }

        private String errorMessage(int error) {
            switch (error) {
                case SpeechRecognizer.ERROR_AUDIO: return "(Audio error)";
                case SpeechRecognizer.ERROR_NO_MATCH: return "(No speech detected)";
                case SpeechRecognizer.ERROR_NETWORK: return "(Network error)";
                case SpeechRecognizer.ERROR_NETWORK_TIMEOUT: return "(Network timeout)";
                case SpeechRecognizer.ERROR_SPEECH_TIMEOUT: return "(Speech timeout)";
                default: return "";
            }
        }
    }

    private int intentScore(String normalizedLower) {
        if (isChargingIntent(normalizedLower)) return 100;
        if (normalizedLower.contains("hello") || normalizedLower.contains("hi") || normalizedLower.contains("hey")) return 50;
        if (normalizedLower.contains("how are you")) return 40;
        if (normalizedLower.contains("your name") || normalizedLower.contains("who are you")) return 30;
        if (normalizedLower.contains("bye") || normalizedLower.contains("goodbye")) return 20;
        return 1;
    }

    // ---- ChatAdapter ----

    private class ChatAdapter extends BaseAdapter {

        @Override
        public int getCount() { return mMessages.size(); }

        @Override
        public Object getItem(int position) { return mMessages.get(position); }

        @Override
        public long getItemId(int position) { return position; }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(getActivity())
                        .inflate(R.layout.item_chat_bubble, parent, false);
            }

            ChatMessage msg = mMessages.get(position);
            TextView tvBubble = convertView.findViewById(R.id.tv_bubble);
            TextView tvRobotIcon = convertView.findViewById(R.id.tv_robot_icon);
            TextView tvUserIcon = convertView.findViewById(R.id.tv_user_icon);
            ViewGroup row = (ViewGroup) convertView.findViewById(R.id.chat_row);

            tvBubble.setText(msg.text);

            if (msg.isUser) {
                // User bubble: right-aligned, blue background
                tvUserIcon.setVisibility(View.VISIBLE);
                tvRobotIcon.setVisibility(View.GONE);
                tvBubble.setBackgroundColor(0xFFBBDEFB); // light blue
                row.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
            } else {
                // Robot bubble: left-aligned, grey background
                tvRobotIcon.setVisibility(View.VISIBLE);
                tvUserIcon.setVisibility(View.GONE);
                tvBubble.setBackgroundColor(0xFFE0E0E0); // light grey
                row.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
            }

            return convertView;
        }
    }
}
