package com.keenon.peanut.sample;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.keenon.common.constant.PeanutConstants;
import com.keenon.common.external.PeanutConfig;
import com.keenon.common.utils.LogUtils;
import com.keenon.common.utils.VersionInfo;
import com.keenon.peanut.sample.util.BaseActivity;
import com.keenon.sdk.component.runtime.PeanutRuntime;
import com.keenon.sdk.external.PeanutSDK;

import java.util.ArrayList;

import static com.keenon.sdk.external.PeanutSDK.SDK_INIT_SUCCESS;

public class KeenonApiDemoMain extends BaseActivity implements SdkDemoHost {

    private static final String TAG = KeenonApiDemoMain.class.getSimpleName();
    private static final String[] TAB_TITLES = {"Home", "Form", "Voice"};

    private TextView tvSdkStatus;

    private PeanutSDK.ErrorListener mErrorListener = errorCode -> {
        Log.d(TAG, "onInit:" + errorCode);
        runOnUiThread(() -> {
            if (tvSdkStatus == null) return;
            if (errorCode == SDK_INIT_SUCCESS) {
                tvSdkStatus.setTextColor(Color.GREEN);
                tvSdkStatus.setText(getString(R.string.str_init_text) + errorCode);
                PeanutRuntime.getInstance().start(new PeanutRuntime.Listener() {
                    @Override
                    public void onEvent(int event, Object obj) {
                        LogUtils.d(TAG, "onEvent:" + event + ", content: " + obj);
                    }

                    @Override
                    public void onHealth(Object content) {
                        LogUtils.d(TAG, "onHealth:" + content);
                    }

                    @Override
                    public void onHeartbeat(Object content) {
                        LogUtils.d(TAG, "onHeartbeat:" + content);
                    }
                });
            } else {
                tvSdkStatus.setTextColor(Color.RED);
                tvSdkStatus.setText(getString(R.string.str_init_text) + errorCode);
            }
        });
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        tvSdkStatus = findViewById(R.id.tv_sdk_status);
        tvSdkStatus.setTextColor(Color.GREEN);
        tvSdkStatus.setText(getString(R.string.str_content) + VersionInfo.versionName);

        ViewPager2 viewPager = findViewById(R.id.view_pager);
        TabLayout tabLayout = findViewById(R.id.tab_layout);

        viewPager.setAdapter(new TabAdapter(this));
        // Keep at 1 so VoiceFragment (and Vosk) are not created at cold start — avoids native crash
        // when the offline model is missing or incomplete.
        viewPager.setOffscreenPageLimit(1);

        new TabLayoutMediator(tabLayout, viewPager,
                (tab, position) -> tab.setText(TAB_TITLES[position])
        ).attach();

        requestAppPermissions();

        initSDK(getType());
    }

    @Override
    protected void onDestroy() {
        PeanutSDK.getInstance().release();
        super.onDestroy();
    }

    private void initSDK(String ip) {
        PeanutConfig.getConfig()
                .setLinkType(PeanutConstants.REMOTE_LINK_PROXY.equals(ip)
                        ? PeanutConstants.LinkType.COAP
                        : PeanutConstants.LinkType.COM_COAP)
                .setLinkIP(ip)
                .enableLog(true)
                .setLogLevel(Log.DEBUG)
                .setAppId("bcb8ebc7f22345bebb378aead035cfb3")
                .setSecret("nPlQERTP4qJWimTp0+ZXXkM5ND93iEyWpM6eXAGIZ/HQmyEg8zN7x5tGLebwINKLYScXEjg5lhQBvt1QCODovm2gq7dsXAK4pgjBRK2OqQHxl4nvTjq2AX9Or6XrdfFfVgOiHqW0mw+qWGDJc1/EUBg3llLOzMNUiDqwPsXMZYs=")
                .enableUMLog(false);
        PeanutSDK.getInstance().init(this.getApplicationContext(), mErrorListener);
    }

    @Override
    public void reinitSDK(String ip) {
        PeanutSDK.getInstance().release();
        initSDK(ip);
        if (tvSdkStatus != null) {
            tvSdkStatus.setTextColor(Color.YELLOW);
            tvSdkStatus.setText("Reconnecting...");
        }
    }

    private void requestAppPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            String[] permissions = {
                    Manifest.permission.ACCESS_NETWORK_STATE,
                    Manifest.permission.INTERNET,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.ACCESS_WIFI_STATE,
                    Manifest.permission.READ_PHONE_STATE,
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.CAMERA,
            };

            ArrayList<String> needed = new ArrayList<>();
            for (String perm : permissions) {
                if (checkSelfPermission(perm) != PackageManager.PERMISSION_GRANTED) {
                    needed.add(perm);
                }
            }
            if (!needed.isEmpty()) {
                requestPermissions(needed.toArray(new String[0]), 0);
            }
        }
    }

    @Override
    public String getType() {
        SharedPreferences sp = getSharedPreferences("SP", Context.MODE_PRIVATE);
        return sp.getString("type", PeanutConstants.REMOTE_LINK_PROXY);
    }

    @Override
    public void saveToSP(String type) {
        SharedPreferences sp = getSharedPreferences("SP", Context.MODE_PRIVATE);
        sp.edit().putString("type", type).apply();
    }

    private static class TabAdapter extends FragmentStateAdapter {

        TabAdapter(@NonNull FragmentActivity activity) {
            super(activity);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            switch (position) {
                case 1: return new FormFragment();
                case 2: return new VoiceFragment();
                default: return new HomeFragment();
            }
        }

        @Override
        public int getItemCount() {
            return TAB_TITLES.length;
        }
    }
}
