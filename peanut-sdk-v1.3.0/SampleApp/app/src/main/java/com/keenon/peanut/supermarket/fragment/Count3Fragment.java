package com.keenon.peanut.supermarket.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.keenon.peanut.sample.R;
import com.keenon.peanut.sample.receiver.SensorDataManager;
import com.keenon.peanut.supermarket.feedback.TrayPickFeedbackCoordinator;

/**
 * Count3 — SampleApp1 "Count 2.0" tray counter: OpenCV grid/absdiff, stocking vs customer modes,
 * slot presets (2/4/6/9), oval ROI, hand-track + robot turn + star feedback on pickup.
 *
 * <p>Logic lives in {@link AdvancedCountFragment}; this tab uses the premium side-panel UI from
 * SampleApp1 {@code fragment_dev_count2} with isolated SharedPreferences.</p>
 */
public class Count3Fragment extends AdvancedCountFragment {

  @Override
  protected String getCountTrayPrefsName() {
    return "count3_tray_prefs";
  }

  @Override
  protected TrayPickFeedbackCoordinator.Timing pickFeedbackTiming() {
    return TrayPickFeedbackCoordinator.Timing.count20();
  }

  @Nullable
  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_dev_count3, container, false);
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    SensorDataManager.getInstance().ensureSafetyMonitoring();
  }
}
