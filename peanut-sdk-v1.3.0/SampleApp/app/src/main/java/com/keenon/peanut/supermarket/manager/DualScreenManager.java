package com.keenon.peanut.supermarket.manager;

import android.app.Presentation;
import android.content.Context;
import android.hardware.display.DisplayManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.widget.ImageView;
import android.widget.VideoView;

import androidx.appcompat.view.ContextThemeWrapper;

import com.keenon.peanut.sample.R;

public class DualScreenManager {
  private static final String TAG = "DualScreenManager";

  private final Context appContext;
  private DisplayManager displayManager;
  private AdPresentation adPresentation;
  private Display secondaryDisplay;

  public DualScreenManager(Context context) {
    appContext = context.getApplicationContext();
  }

  public void init() {
    displayManager = (DisplayManager) appContext.getSystemService(Context.DISPLAY_SERVICE);
    if (displayManager == null) return;
    Display[] displays =
        displayManager.getDisplays(DisplayManager.DISPLAY_CATEGORY_PRESENTATION);
    if (displays != null && displays.length > 0) {
      secondaryDisplay = displays[0];
      try {
        Context themed =
            new ContextThemeWrapper(appContext, R.style.AppTheme);
        adPresentation = new AdPresentation(themed, secondaryDisplay);
        adPresentation.show();
      } catch (Exception e) {
        Log.w(TAG, "Secondary display unavailable", e);
        adPresentation = null;
        secondaryDisplay = null;
      }
    }
  }

  public void updateMedia(Uri mediaUri, boolean isVideo) {
    if (adPresentation == null) return;
    adPresentation.loadMedia(mediaUri, isVideo);
  }

  public void release() {
    if (adPresentation != null) {
      try {
        adPresentation.dismiss();
      } catch (Exception ignored) {
      }
      adPresentation = null;
    }
    secondaryDisplay = null;
  }

  public boolean isAvailable() {
    return secondaryDisplay != null && adPresentation != null;
  }

  private static final class AdPresentation extends Presentation {
    private VideoView videoView;
    private ImageView imageView;

    AdPresentation(Context outerContext, Display display) {
      super(outerContext, display);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.presentation_ad);
      videoView = findViewById(R.id.presentation_video);
      imageView = findViewById(R.id.presentation_image);
    }

    void loadMedia(Uri uri, boolean isVideo) {
      if (videoView == null || imageView == null) return;
      if (isVideo) {
        imageView.setVisibility(android.view.View.GONE);
        videoView.setVisibility(android.view.View.VISIBLE);
        videoView.setVideoURI(uri);
        videoView.setOnPreparedListener(mp -> videoView.start());
        videoView.setOnCompletionListener(mp -> videoView.start());
      } else {
        videoView.stopPlayback();
        videoView.setVisibility(android.view.View.GONE);
        imageView.setVisibility(android.view.View.VISIBLE);
        imageView.setImageURI(uri);
      }
    }
  }
}
