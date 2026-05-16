package com.keenon.peanut.supermarket.speech;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;

import java.util.ArrayList;

/**
 * Minimal speech capture using system {@link SpeechRecognizer} (SDD pipeline entry point).
 */
public class SpeechEngineManager {
  private static final String TAG = "SpeechEngineManager";

  public interface TranscriptCallback {
    void onTranscript(String text);
  }

  public interface ErrorCallback {
    void onError(int code);
  }

  private SpeechRecognizer recognizer;
  private Context appContext;

  public void initialize(Context context) {
    appContext = context.getApplicationContext();
  }

  public boolean isAvailable(Context context) {
    return SpeechRecognizer.isRecognitionAvailable(context);
  }

  public void startListening(
      Context context, TranscriptCallback onTranscript, ErrorCallback onError) {
    startListening(context, false, onTranscript, onError);
  }

  /**
   * Starts the system recognizer. When {@code preferOffline} is true, asks the recognizer to
   * stay on-device via {@code EXTRA_PREFER_OFFLINE} (API 23+). Devices without an on-device
   * recognizer will still return an error that the caller should surface.
   */
  public void startListening(
      Context context,
      boolean preferOffline,
      TranscriptCallback onTranscript,
      ErrorCallback onError) {
    stopListening();
    if (!SpeechRecognizer.isRecognitionAvailable(context)) {
      if (onError != null) onError.onError(-1);
      return;
    }
    recognizer = SpeechRecognizer.createSpeechRecognizer(context);
    recognizer.setRecognitionListener(
        new RecognitionListener() {
          @Override
          public void onReadyForSpeech(Bundle params) {}

          @Override
          public void onBeginningOfSpeech() {}

          @Override
          public void onRmsChanged(float rmsdB) {}

          @Override
          public void onBufferReceived(byte[] buffer) {}

          @Override
          public void onEndOfSpeech() {}

          @Override
          public void onError(int error) {
            Log.w(TAG, "speech error " + error);
            if (onError != null) onError.onError(error);
          }

          @Override
          public void onResults(Bundle results) {
            ArrayList<String> m =
                results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            if (m != null && !m.isEmpty() && onTranscript != null) {
              onTranscript.onTranscript(m.get(0));
            } else if (onTranscript != null) {
              onTranscript.onTranscript("");
            }
          }

          @Override
          public void onPartialResults(Bundle partialResults) {}

          @Override
          public void onEvent(int eventType, Bundle params) {}
        });
    Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
    intent.putExtra(
        RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
    intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
    if (preferOffline && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      intent.putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true);
    }
    recognizer.startListening(intent);
  }

  public void stopListening() {
    if (recognizer != null) {
      try {
        recognizer.stopListening();
        recognizer.destroy();
      } catch (Exception e) {
        Log.w(TAG, "stopListening", e);
      }
      recognizer = null;
    }
  }

  /**
   * Stops audio capture but keeps the recognizer alive so it can deliver {@code onResults}
   * for what was already spoken. Use this for a user-facing "Stop" button. To hard-cancel
   * (and drop results), call {@link #stopListening()} instead.
   */
  public void finishListening() {
    if (recognizer != null) {
      try {
        recognizer.stopListening();
      } catch (Exception e) {
        Log.w(TAG, "finishListening", e);
      }
    }
  }
}
