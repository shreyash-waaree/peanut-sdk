package com.keenon.peanut.supermarket.fragment;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.keenon.peanut.sample.BackendConfig;
import com.keenon.peanut.sample.R;
import com.keenon.peanut.sample.util.JavaBackendSpeechTranscriber;
import com.keenon.peanut.sample.util.VoiceBackendHelper;
import com.keenon.peanut.sample.util.VoicePreferences;
import com.keenon.peanut.sample.util.VoiceTranscriptUtil;
import com.keenon.peanut.supermarket.adapter.SectionsPagerAdapter;
import com.keenon.peanut.supermarket.SupermarketActivity;
import com.keenon.peanut.supermarket.model.FaqConfig;
import com.keenon.peanut.supermarket.model.FaqTrigger;
import com.keenon.peanut.supermarket.speech.SpeechEngineManager;
import com.keenon.peanut.supermarket.speech.VoiceCommandDispatcher;
import com.keenon.peanut.supermarket.util.TtsHelper;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

public class PromoVoiceFragment extends Fragment {

  private static final int REQUEST_RECORD_AUDIO = 1102;

  private Button btnMic;
  private TextView txtTranscript;
  private TextView txtReply;
  private TextView voiceStatus;
  private View voicePulse;
  private Switch swForceOffline;
  private LinearLayout micLevelRow;
  private ProgressBar pbMicLevel;
  private TextView tvMicLevel;
  @Nullable private Animation pulseAnimation;

  private final SpeechEngineManager speechEngineMgr = new SpeechEngineManager();
  private final TtsHelper ttsHelper = new TtsHelper();
  private final Handler handler = new Handler(Looper.getMainLooper());

  private final AtomicBoolean cancelBackendRecord = new AtomicBoolean(false);
  private final AtomicBoolean stopBackendRecord = new AtomicBoolean(false);
  @Nullable private Thread backendRecordThread;

  private boolean listening;
  private FaqConfig faqConfig;

  @Nullable
  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_promo_voice, container, false);
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    btnMic = view.findViewById(R.id.btn_mic);
    txtTranscript = view.findViewById(R.id.txt_transcript);
    txtReply = view.findViewById(R.id.txt_reply);
    voiceStatus = view.findViewById(R.id.voice_status);
    voicePulse = view.findViewById(R.id.voice_pulse);
    swForceOffline = view.findViewById(R.id.sw_force_offline);
    micLevelRow = view.findViewById(R.id.mic_level_row);
    pbMicLevel = view.findViewById(R.id.pb_mic_level);
    tvMicLevel = view.findViewById(R.id.tv_mic_level);
    btnMic.setOnClickListener(v -> onMicClick());

    if (swForceOffline != null) {
      swForceOffline.setChecked(VoicePreferences.isForceOffline(requireContext()));
      swForceOffline.setOnCheckedChangeListener((buttonView, isChecked) -> {
        VoicePreferences.setForceOffline(requireContext(), isChecked);
        refreshVoiceAvailabilityHint();
        Toast.makeText(
                requireContext(),
                isChecked
                    ? "Offline voice forced — uses on-device recognizer."
                    : "Online voice allowed — will use Java backend when reachable.",
                Toast.LENGTH_SHORT)
            .show();
      });
    }

    EditText etBackend = view.findViewById(R.id.et_voice_backend_url);
    TextView tvState = view.findViewById(R.id.tv_voice_backend_state);
    if (etBackend != null) {
      etBackend.setText(BackendConfig.getBaseUrl(requireContext()));
    }
    View btnSave = view.findViewById(R.id.btn_voice_backend_save);
    if (btnSave != null) {
      btnSave.setOnClickListener(
          v -> {
            String input = etBackend != null ? etBackend.getText().toString() : "";
            boolean ok = BackendConfig.setBaseUrl(requireContext(), input);
            if (ok) {
              if (tvState != null) tvState.setText(R.string.voice_backend_saved);
              Toast.makeText(requireContext(), R.string.voice_backend_saved, Toast.LENGTH_SHORT)
                  .show();
              refreshVoiceAvailabilityHint();
            } else {
              if (tvState != null) tvState.setText(R.string.voice_backend_saved_invalid);
              Toast.makeText(
                      requireContext(),
                      R.string.voice_backend_saved_invalid,
                      Toast.LENGTH_SHORT)
                  .show();
            }
          });
    }
    View btnTest = view.findViewById(R.id.btn_voice_backend_test);
    if (btnTest != null) {
      btnTest.setOnClickListener(
          v -> {
            if (tvState != null) tvState.setText("Testing…");
            new Thread(
                    () -> {
                      boolean reachable = VoiceBackendHelper.isBackendReachable(requireContext());
                      handler.post(
                          () -> {
                            if (tvState != null) {
                              tvState.setText(
                                  reachable
                                      ? R.string.voice_backend_reachable
                                      : R.string.voice_backend_unreachable);
                            }
                          });
                    },
                    "voice-backend-test")
                .start();
          });
    }
  }

  private void startPulse() {
    if (voicePulse == null) return;
    if (pulseAnimation == null) {
      pulseAnimation = AnimationUtils.loadAnimation(requireContext(), R.anim.voice_pulse);
    }
    voicePulse.startAnimation(pulseAnimation);
  }

  private void stopPulse() {
    if (voicePulse != null) {
      voicePulse.clearAnimation();
    }
  }

  private void showMicLevelUi(boolean visible) {
    if (micLevelRow == null) return;
    micLevelRow.setVisibility(visible ? View.VISIBLE : View.GONE);
    if (!visible) {
      if (pbMicLevel != null) pbMicLevel.setProgress(0);
      if (tvMicLevel != null) tvMicLevel.setText("0%");
    }
  }

  private void updateMicLevelUi(int pct) {
    int clamped = Math.max(0, Math.min(100, pct));
    if (pbMicLevel != null) pbMicLevel.setProgress(clamped);
    if (tvMicLevel != null) tvMicLevel.setText(clamped + "%");
  }

  @Override
  public void onResume() {
    super.onResume();
    if (getActivity() instanceof SupermarketActivity) {
      faqConfig = ((SupermarketActivity) getActivity()).getProductConfig().getFaqConfig();
    }
    speechEngineMgr.initialize(requireContext());
    ttsHelper.initialize(requireContext());
    refreshVoiceAvailabilityHint();
  }

  @Override
  public void onPause() {
    super.onPause();
    speechEngineMgr.stopListening();
    ttsHelper.stop();
    cancelBackendRecord.set(true);
  }

  @Override
  public void onDestroyView() {
    cancelBackendRecord.set(true);
    if (backendRecordThread != null) {
      try {
        backendRecordThread.join(2000);
      } catch (InterruptedException ignored) {
      }
      backendRecordThread = null;
    }
    ttsHelper.shutdown();
    super.onDestroyView();
  }

  @Override
  public void onRequestPermissionsResult(
      int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    if (requestCode == REQUEST_RECORD_AUDIO) {
      if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
        onMicClick();
      } else {
        Toast.makeText(requireContext(), "Microphone permission is required for voice.", Toast.LENGTH_LONG)
            .show();
      }
    }
  }

  private void refreshVoiceAvailabilityHint() {
    boolean forceOffline = VoicePreferences.isForceOffline(requireContext());
    if (!forceOffline && VoiceBackendHelper.shouldUseJavaBackendStt(requireContext())) {
      voiceStatus.setText("Voice: Java backend — tap mic");
      btnMic.setEnabled(true);
      return;
    }
    if (!speechEngineMgr.isAvailable(requireContext())) {
      voiceStatus.setText("Voice: unavailable (install Google app / enable recognition)");
      btnMic.setEnabled(false);
      Toast.makeText(requireContext(), "Voice is currently unavailable", Toast.LENGTH_LONG).show();
      return;
    }
    voiceStatus.setText(
        forceOffline
            ? "Voice: offline mode (on-device recognizer) — tap mic"
            : getString(R.string.voice_status_idle));
    btnMic.setEnabled(true);
  }

  private void onMicClick() {
    if (listening) {
      // User tapped "Stop": finalize recording and let the backend / recognizer process it.
      if (backendRecordThread != null) {
        stopBackendRecord.set(true);
      } else {
        speechEngineMgr.finishListening();
      }
      btnMic.setEnabled(false);
      btnMic.setText("Processing…");
      voiceStatus.setText("Finalizing — sending to backend…");
      return;
    }
    boolean forceOffline = VoicePreferences.isForceOffline(requireContext());
    if (!forceOffline && VoiceBackendHelper.shouldUseJavaBackendStt(requireContext())) {
      if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO)
          != PackageManager.PERMISSION_GRANTED) {
        requestPermissions(new String[] {Manifest.permission.RECORD_AUDIO}, REQUEST_RECORD_AUDIO);
        return;
      }
      startJavaBackendListening();
      return;
    }
    if (!speechEngineMgr.isAvailable(requireContext())) {
      Toast.makeText(requireContext(), "Speech recognition not available", Toast.LENGTH_SHORT).show();
      return;
    }
    listening = true;
    btnMic.setText("Stop");
    voiceStatus.setText(
        forceOffline ? "Listening (offline) — tap Stop when done…" : "Listening — tap Stop when done…");
    startPulse();
    txtTranscript.setText("");
    txtReply.setText("");
    speechEngineMgr.startListening(
        requireContext(),
        forceOffline,
        text -> handler.post(() -> {
          listening = false;
          stopPulse();
          btnMic.setEnabled(true);
          btnMic.setText(R.string.tap_to_speak);
          handleVoiceTranscript(text != null ? text : "");
        }),
        code ->
            handler.post(
                () -> {
                  listening = false;
                  stopPulse();
                  btnMic.setEnabled(true);
                  btnMic.setText(R.string.tap_to_speak);
                  voiceStatus.setText(R.string.voice_status_idle);
                  txtReply.setText(
                      forceOffline
                          ? "Offline recognizer failed (code " + code
                              + "). This device may not ship an on-device speech engine."
                          : "Sorry, I didn't catch that. Try again.");
                }));
  }

  private void startJavaBackendListening() {
    listening = true;
    cancelBackendRecord.set(false);
    stopBackendRecord.set(false);
    voiceStatus.setText("Listening… speak, then tap Stop to send");
    btnMic.setText("Stop");
    btnMic.setEnabled(true);
    txtTranscript.setText("");
    txtReply.setText("");
    startPulse();
    showMicLevelUi(true);
    updateMicLevelUi(0);

    backendRecordThread =
        JavaBackendSpeechTranscriber.startAsync(
            requireContext(),
            cancelBackendRecord,
            stopBackendRecord,
            new JavaBackendSpeechTranscriber.Listener() {
              @Override
              public void onTranscript(String text) {
                handler.post(
                    () -> {
                      listening = false;
                      backendRecordThread = null;
                      stopPulse();
                      showMicLevelUi(false);
                      btnMic.setEnabled(true);
                      btnMic.setText(R.string.tap_to_speak);
                      voiceStatus.setText("Processing reply…");
                      handleVoiceTranscript(text != null ? text : "");
                    });
              }

              @Override
              public void onError(String message) {
                handler.post(
                    () -> {
                      listening = false;
                      backendRecordThread = null;
                      stopPulse();
                      showMicLevelUi(false);
                      btnMic.setEnabled(true);
                      btnMic.setText(R.string.tap_to_speak);
                      voiceStatus.setText(R.string.voice_status_idle);
                      txtReply.setText("Voice error: " + message);
                      Toast.makeText(
                              requireContext(),
                              "Voice: " + message,
                              Toast.LENGTH_LONG)
                          .show();
                    });
              }

              @Override
              public void onLevel(int levelPct, String state) {
                if (getView() == null) return;
                updateMicLevelUi(levelPct);
                if ("calibrating".equals(state)) {
                  voiceStatus.setText("Calibrating background noise… stay quiet.");
                } else if ("speaking".equals(state)) {
                  voiceStatus.setText("Listening… speaking (" + levelPct + "%)");
                } else if ("trailing-silence".equals(state)) {
                  voiceStatus.setText("Pause detected — finishing up…");
                } else {
                  voiceStatus.setText("Listening… (" + levelPct + "%) speak, then pause.");
                }
              }
            });
  }

  private void handleVoiceTranscript(String text) {
    listening = false;
    voiceStatus.setText(R.string.voice_status_idle);
    String intentText = VoiceTranscriptUtil.firstWordsForIntent(text);
    txtTranscript.setText(intentText);
    if (TextUtils.isEmpty(intentText.trim())) {
      return;
    }
    processUserSpeech(intentText.trim());
  }

  private void processUserSpeech(String transcript) {
    // Check robot commands (stop/charging/directions) first — highest priority for safety.
    if (getActivity() instanceof SupermarketActivity) {
      VoiceCommandDispatcher.Result cmd =
          VoiceCommandDispatcher.dispatch((SupermarketActivity) getActivity(), transcript);
      if (cmd.handled) {
        replyToShopper(cmd.reply);
        return;
      }
    }

    if (faqConfig == null) {
      ttsHelper.speak("Configuration not loaded.");
      return;
    }
    String normalized = transcript.toLowerCase(Locale.US).trim();
    for (FaqTrigger t : faqConfig.getTriggers()) {
      List<String> kws = t.getKeywords();
      if (kws == null) continue;
      for (String kw : kws) {
        if (kw != null && normalized.contains(kw.toLowerCase(Locale.US))) {
          replyToShopper(t.getReply());
          if (t.isNavigateToForm()) {
            handler.postDelayed(
                () -> {
                  if (getActivity() instanceof SupermarketActivity) {
                    ((SupermarketActivity) getActivity())
                        .switchToTab(SectionsPagerAdapter.TAB_INTEREST);
                  }
                },
                2000);
          }
          return;
        }
      }
    }
    replyToShopper(faqConfig.getDefaultReply());
  }

  private void replyToShopper(String message) {
    txtReply.setText(message);
    ttsHelper.speak(message);
  }
}
