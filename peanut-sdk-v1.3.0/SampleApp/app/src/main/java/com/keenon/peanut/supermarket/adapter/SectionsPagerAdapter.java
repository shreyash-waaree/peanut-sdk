package com.keenon.peanut.supermarket.adapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.keenon.peanut.supermarket.fragment.AdFragment;
import com.keenon.peanut.supermarket.fragment.DeveloperFragment;
import com.keenon.peanut.supermarket.fragment.FeedbackFragment;
import com.keenon.peanut.supermarket.fragment.InterestFormFragment;
import com.keenon.peanut.supermarket.fragment.PromoVoiceFragment;

public class SectionsPagerAdapter extends FragmentStateAdapter {

  public static final int TAB_AD = 0;
  public static final int TAB_VOICE = 1;
  public static final int TAB_INTEREST = 2;
  public static final int TAB_FEEDBACK = 3;
  /** Legacy SampleApp: Home + Form + Voice (nested). */
  public static final int TAB_DEVELOPER = 4;
  public static final int NUM_TABS = 5;

  public SectionsPagerAdapter(@NonNull FragmentActivity activity) {
    super(activity);
  }

  @NonNull
  @Override
  public Fragment createFragment(int position) {
    switch (position) {
      case TAB_AD:
        return new AdFragment();
      case TAB_VOICE:
        return new PromoVoiceFragment();
      case TAB_INTEREST:
        return new InterestFormFragment();
      case TAB_FEEDBACK:
        return new FeedbackFragment();
      case TAB_DEVELOPER:
        return new DeveloperFragment();
      default:
        return new AdFragment();
    }
  }

  @Override
  public int getItemCount() {
    return NUM_TABS;
  }
}
