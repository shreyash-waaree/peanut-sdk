package com.keenon.peanut.supermarket.adapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.keenon.peanut.sample.FormFragment;
import com.keenon.peanut.sample.HomeFragment;
import com.keenon.peanut.sample.VoiceFragment;
import com.keenon.peanut.supermarket.fragment.CameraFragment;
import com.keenon.peanut.supermarket.fragment.HardwareFragment;
import com.keenon.peanut.supermarket.fragment.SensorsFragment;
import com.keenon.peanut.supermarket.fragment.Count2Fragment;
import com.keenon.peanut.supermarket.fragment.Count3Fragment;
import com.keenon.peanut.supermarket.fragment.Count4Fragment;
import com.keenon.peanut.supermarket.fragment.CountFragment;
import com.keenon.peanut.supermarket.fragment.YoloFragment;

/**
 * Developer subtabs:
 * <ul>
 *   <li>Home: SDK demos (legacy SampleApp)</li>
 *   <li>Form: HTTP form demo</li>
 *   <li>Voice: full Vosk / Whisper stack</li>
 *   <li>Hardware: direct motor controls + live sensor read-outs (mirrors RobotController)</li>
 *   <li>Camera: live camera preview for on-robot diagnostics</li>
 *   <li>Sensors: live dashboard for every sensor topic (battery, IMU, LiDAR, sonar, etc.)</li>
 *   <li>Yolo: NCNN YOLO object detection on tray cameras</li>
 *   <li>Count: NCNN canpan tray plate counter (same family as production)</li>
 *   <li>Count2: OpenCV grid / absdiff classical tray counter (no ML)</li>
 *   <li>Count3: SampleApp1 advanced counter (2/4/6/9 slots, stocking/customer, pick feedback)</li>
 *   <li>Count4: Count3 + hand approach angle logs (0–360°) on pickup</li>
 * </ul>
 */
public class DeveloperPagerAdapter extends FragmentStateAdapter {

  public static final int POS_HOME = 0;
  public static final int POS_FORM = 1;
  public static final int POS_VOICE = 2;
  public static final int POS_HARDWARE = 3;
  public static final int POS_CAMERA = 4;
  public static final int POS_SENSORS = 5;
  public static final int POS_YOLO = 6;
  public static final int POS_COUNT = 7;
  public static final int POS_COUNT2 = 8;
  public static final int POS_COUNT3 = 9;
  public static final int POS_COUNT4 = 10;
  public static final int PAGE_COUNT = 11;

  public DeveloperPagerAdapter(@NonNull FragmentActivity activity) {
    super(activity);
  }

  @NonNull
  @Override
  public Fragment createFragment(int position) {
    switch (position) {
      case POS_FORM:
        return new FormFragment();
      case POS_VOICE:
        return new VoiceFragment();
      case POS_HARDWARE:
        return new HardwareFragment();
      case POS_CAMERA:
        return new CameraFragment();
      case POS_SENSORS:
        return new SensorsFragment();
      case POS_YOLO:
        return new YoloFragment();
      case POS_COUNT:
        return new CountFragment();
      case POS_COUNT2:
        return new Count2Fragment();
      case POS_COUNT3:
        return new Count3Fragment();
      case POS_COUNT4:
        return new Count4Fragment();
      case POS_HOME:
      default:
        return new HomeFragment();
    }
  }

  @Override
  public int getItemCount() {
    return PAGE_COUNT;
  }
}
