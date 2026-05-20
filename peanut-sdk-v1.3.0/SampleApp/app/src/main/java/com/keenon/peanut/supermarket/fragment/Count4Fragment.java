package com.keenon.peanut.supermarket.fragment;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.keenon.peanut.sample.R;
import com.keenon.peanut.supermarket.vision.TrayCountingHelper;
import com.keenon.peanut.supermarket.vision.TrayPickApproachAngle;

import java.util.Locale;

/**
 * Count4 — Count3 + approach-angle logging on customer pickup (0–360° from tray oval center).
 * Logcat filter: {@link TrayPickApproachAngle#LOG_TAG}.
 */
public class Count4Fragment extends Count3Fragment {

  private static final int MAX_LOG_LINES = 8;
  private final StringBuilder angleLogBuffer = new StringBuilder();
  @Nullable private TextView angleLogView;

  @Override
  protected String getCountTrayPrefsName() {
    return "count4_tray_prefs";
  }

  @Nullable
  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_dev_count4, container, false);
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    angleLogView = view.findViewById(R.id.count4_angle_log);
    appendAngleLogLine(getString(R.string.count4_angle_log_hint));
  }

  @Override
  protected void onCustomerTrayPick(
      int prevCount, int newCount, @NonNull TrayCountingHelper.Result r) {
    FrameRoi roi = getActiveTrayRoi();
    if (roi == null) {
      Log.w(TrayPickApproachAngle.LOG_TAG, "PICK skipped — tray ROI not set");
      appendAngleLogLine(getString(R.string.count4_angle_no_roi));
      return;
    }

    float handX = getLastHandNormX() >= 0f ? getLastHandNormX() : -1f;
    float handY = getLastHandNormY() >= 0f ? getLastHandNormY() : -1f;
    TrayPickApproachAngle.Reading reading =
        TrayPickApproachAngle.compute(
            roi.cx,
            roi.cy,
            roi.frameW,
            roi.frameH,
            handX,
            handY,
            r.pickHandNormX,
            r.pickHandNormY);

    if (reading == null) {
      Log.w(
          TrayPickApproachAngle.LOG_TAG,
          "PICK cam=" + getTrayCameraId() + " count=" + prevCount + "->" + newCount
              + " — angle unknown");
      appendAngleLogLine(getString(R.string.count4_angle_unknown));
      return;
    }

    String line =
        TrayPickApproachAngle.formatLogLine(getTrayCameraId(), prevCount, newCount, reading);
    Log.i(TrayPickApproachAngle.LOG_TAG, line);

    String ui =
        String.format(
            Locale.US,
            "%.0f° (%s)  %d→%d  cam%d",
            reading.degrees,
            reading.source.name(),
            prevCount,
            newCount,
            getTrayCameraId());
    appendAngleLogLine(ui);
  }

  private void appendAngleLogLine(String line) {
    if (angleLogBuffer.length() > 0) {
      angleLogBuffer.append('\n');
    }
    angleLogBuffer.append(line);
    int lines = 0;
    for (int i = 0; i < angleLogBuffer.length(); i++) {
      if (angleLogBuffer.charAt(i) == '\n') lines++;
    }
    while (lines >= MAX_LOG_LINES) {
      int cut = angleLogBuffer.indexOf("\n");
      if (cut < 0) {
        angleLogBuffer.setLength(0);
        break;
      }
      angleLogBuffer.delete(0, cut + 1);
      lines--;
    }
    if (angleLogView != null) {
      angleLogView.setText(angleLogBuffer.toString());
    }
  }
}
