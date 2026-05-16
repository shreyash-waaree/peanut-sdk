package com.keenon.peanut.supermarket.fragment;

import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.keenon.peanut.sample.R;
import com.keenon.peanut.supermarket.SupermarketActivity;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class AdFragment extends Fragment {

  private static final long SLIDE_INTERVAL_MS = 4000;

  private VideoView videoView;
  private ImageView imageSlideshow;
  private ImageView fallback;
  private View defaultBrand;
  private TextView overlayText;

  private final List<File> mediaFiles = new ArrayList<>();
  private boolean videoMode;
  private int loopCount;
  private final Handler slideshowHandler = new Handler(Looper.getMainLooper());
  private int currentSlide;
  private final Runnable slideRunnable =
      new Runnable() {
        @Override
        public void run() {
          if (mediaFiles.isEmpty() || videoMode) return;
          showSlide(currentSlide);
          currentSlide = (currentSlide + 1) % mediaFiles.size();
          slideshowHandler.postDelayed(this, SLIDE_INTERVAL_MS);
        }
      };

  @Nullable
  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_ad, container, false);
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    videoView = view.findViewById(R.id.ad_video);
    imageSlideshow = view.findViewById(R.id.ad_slideshow);
    fallback = view.findViewById(R.id.ad_fallback);
    defaultBrand = view.findViewById(R.id.ad_default_brand);
    overlayText = view.findViewById(R.id.ad_overlay);
    overlayText.setOnClickListener(
        v -> {
          overlayText.setVisibility(View.GONE);
          if (getActivity() instanceof SupermarketActivity) {
            ((SupermarketActivity) getActivity()).enterInteractionMode();
          }
        });
  }

  @Override
  public void onResume() {
    super.onResume();
    loadMedia();
    startPlayback();
  }

  @Override
  public void onPause() {
    super.onPause();
    stopPlayback();
  }

  private void loadMedia() {
    mediaFiles.clear();
    if (getContext() == null) return;
    File dir = new File(getContext().getExternalFilesDir(null), "promo");
    if (!dir.isDirectory()) {
      // eslint-disable - dir may be created by store manager later
      //noinspection ResultOfMethodCallIgnored
      dir.mkdirs();
    }
    File[] files = dir.listFiles();
    if (files != null) {
      List<File> vids = new ArrayList<>();
      List<File> imgs = new ArrayList<>();
      for (File f : files) {
        if (!f.isFile()) continue;
        String n = f.getName().toLowerCase();
        if (n.endsWith(".mp4")) vids.add(f);
        else if (n.endsWith(".jpg") || n.endsWith(".jpeg") || n.endsWith(".png")) imgs.add(f);
      }
      if (!vids.isEmpty()) {
        videoMode = true;
        mediaFiles.addAll(vids);
        sortByName(mediaFiles);
      } else if (!imgs.isEmpty()) {
        videoMode = false;
        mediaFiles.addAll(imgs);
        sortByName(mediaFiles);
      }
    }
    videoMode = !mediaFiles.isEmpty() && mediaFiles.get(0).getName().toLowerCase().endsWith(".mp4");
  }

  private static void sortByName(List<File> files) {
    files.sort(Comparator.comparing(f -> f.getName().toLowerCase()));
  }

  private void startPlayback() {
    if (getContext() == null) return;
    if (mediaFiles.isEmpty()) {
      fallback.setVisibility(View.VISIBLE);
      defaultBrand.setVisibility(View.VISIBLE);
      videoView.setVisibility(View.GONE);
      imageSlideshow.setVisibility(View.GONE);
      return;
    }
    fallback.setVisibility(View.GONE);
    defaultBrand.setVisibility(View.GONE);
    if (videoMode) {
      imageSlideshow.setVisibility(View.GONE);
      videoView.setVisibility(View.VISIBLE);
      File f = mediaFiles.get(0);
      videoView.setVideoPath(f.getAbsolutePath());
      videoView.setOnPreparedListener(mp -> videoView.start());
      videoView.setOnCompletionListener(
          mp -> {
            loopCount++;
            if (loopCount == 1) {
              showOverlay();
            }
            videoView.start();
          });
      videoView.setOnErrorListener(
          (mp, what, extra) -> {
            videoMode = false;
            mediaFiles.clear();
            loadMedia();
            startPlayback();
            return true;
          });
      notifySecondary(f, true);
    } else {
      videoView.setVisibility(View.GONE);
      imageSlideshow.setVisibility(View.VISIBLE);
      currentSlide = 0;
      showSlide(0);
      slideshowHandler.postDelayed(slideRunnable, SLIDE_INTERVAL_MS);
      if (!mediaFiles.isEmpty()) {
        notifySecondary(mediaFiles.get(0), false);
      }
    }
  }

  private void notifySecondary(File file, boolean isVideo) {
    if (!(getActivity() instanceof SupermarketActivity)) return;
    ((SupermarketActivity) getActivity())
        .getDualScreenManager()
        .updateMedia(Uri.fromFile(file), isVideo);
  }

  private void showOverlay() {
    overlayText.setAlpha(0f);
    overlayText.setVisibility(View.VISIBLE);
    overlayText.animate().alpha(1f).setDuration(500).start();
  }

  private void showSlide(int index) {
    if (index < 0 || index >= mediaFiles.size()) return;
    imageSlideshow.setImageURI(Uri.fromFile(mediaFiles.get(index)));
  }

  private void stopPlayback() {
    slideshowHandler.removeCallbacksAndMessages(null);
    if (videoView != null) {
      videoView.stopPlayback();
    }
  }
}
