package com.keenon.peanut.supermarket.util;

import android.content.Context;
import android.media.AudioAttributes;
import android.os.Build;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;

import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

public class TtsHelper implements TextToSpeech.OnInitListener {
  private TextToSpeech tts;
  private final AtomicBoolean ready = new AtomicBoolean(false);
  private Context appContext;

  public void initialize(Context context) {
    if (tts != null) return;
    appContext = context.getApplicationContext();
    tts = new TextToSpeech(appContext, this);
  }

  @Override
  public void onInit(int status) {
    if (status != TextToSpeech.SUCCESS || tts == null) {
      ready.set(false);
      return;
    }
    tts.setLanguage(Locale.US);
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      tts.setAudioAttributes(
          new AudioAttributes.Builder()
              .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
              .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
              .build());
    }
    ready.set(true);
  }

  public void speak(String message) {
    if (tts == null || !ready.get() || message == null) return;
    String id = "promo_" + System.nanoTime();
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      tts.speak(message, TextToSpeech.QUEUE_FLUSH, null, id);
    } else {
      tts.speak(message, TextToSpeech.QUEUE_FLUSH, null);
    }
  }

  public void speak(String message, Runnable onDone) {
    if (tts == null || !ready.get() || message == null) {
      if (onDone != null) onDone.run();
      return;
    }
    String id = "promo_done_" + System.nanoTime();
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      tts.setOnUtteranceProgressListener(
          new UtteranceProgressListener() {
            @Override
            public void onStart(String utteranceId) {}

            @Override
            public void onDone(String utteranceId) {
              if (onDone != null) onDone.run();
            }

            @Override
            public void onError(String utteranceId) {
              if (onDone != null) onDone.run();
            }
          });
      tts.speak(message, TextToSpeech.QUEUE_FLUSH, null, id);
    } else {
      tts.speak(message, TextToSpeech.QUEUE_FLUSH, null);
      if (onDone != null) onDone.run();
    }
  }

  public void stop() {
    if (tts != null) {
      tts.stop();
    }
  }

  public void shutdown() {
    if (tts != null) {
      tts.stop();
      tts.shutdown();
      tts = null;
    }
    ready.set(false);
  }
}
