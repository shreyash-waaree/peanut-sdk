package com.keenon.peanut.supermarket.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.keenon.peanut.sample.R;
import com.keenon.peanut.supermarket.adapter.DeveloperPagerAdapter;

/**
 * Embeds the legacy SampleApp experience: Home (SDK list), Form, Voice (Java {@code /speech-transcribe} or system / Vosk).
 */
public class DeveloperFragment extends Fragment {

  @Nullable
  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_developer, container, false);
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    TabLayout tabLayout = view.findViewById(R.id.dev_tab_layout);
    ViewPager2 pager = view.findViewById(R.id.dev_view_pager);
    if (getActivity() == null) return;

    DeveloperPagerAdapter adapter = new DeveloperPagerAdapter(requireActivity());
    pager.setAdapter(adapter);
    // Defer Voice/Hardware/Camera fragment creation until user opens them (cold start, mic, camera).
    pager.setOffscreenPageLimit(1);

    String[] titles = {
      getString(R.string.dev_subtab_home),
      getString(R.string.dev_subtab_form),
      getString(R.string.dev_subtab_voice),
      getString(R.string.dev_subtab_hardware),
      getString(R.string.dev_subtab_camera),
      getString(R.string.dev_subtab_sensors),
      getString(R.string.dev_subtab_yolo),
      getString(R.string.dev_subtab_count),
      getString(R.string.dev_subtab_count2),
      getString(R.string.dev_subtab_count3),
      getString(R.string.dev_subtab_count4)
    };
    new TabLayoutMediator(tabLayout, pager, (tab, position) -> tab.setText(titles[position]))
        .attach();
    tabLayout.setTabMode(com.google.android.material.tabs.TabLayout.MODE_SCROLLABLE);
  }
}
