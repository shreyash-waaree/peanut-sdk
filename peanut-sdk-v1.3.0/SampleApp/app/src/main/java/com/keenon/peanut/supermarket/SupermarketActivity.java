package com.keenon.peanut.supermarket;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.TextView;

import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.keenon.common.constant.PeanutConstants;
import com.keenon.common.external.PeanutConfig;
import com.keenon.common.utils.LogUtils;
import com.keenon.common.utils.VersionInfo;
import com.keenon.peanut.sample.R;
import com.keenon.peanut.sample.SdkDemoHost;
import com.keenon.peanut.sample.util.BaseActivity;
import com.keenon.peanut.supermarket.adapter.SectionsPagerAdapter;
import com.keenon.peanut.supermarket.manager.DualScreenManager;
import com.keenon.peanut.supermarket.manager.IdleTimerManager;
import com.keenon.peanut.supermarket.manager.PatrolNavManager;
import com.keenon.peanut.supermarket.manager.ProductConfig;
import com.keenon.sdk.component.runtime.PeanutRuntime;
import com.keenon.sdk.external.PeanutSDK;

import java.util.ArrayList;

import static com.keenon.sdk.external.PeanutSDK.SDK_INIT_SUCCESS;

/**
 * Primary retail shell: tabs Ad / Voice / Interest / Feedback, patrol + kiosk ad mode.
 * See {@code SupermarketPromoBot_SDD.md}.
 */
public class SupermarketActivity extends BaseActivity implements SdkDemoHost {

  private static final String TAG = "SupermarketActivity";

  public enum AppState {
    PATROL,
    INTERACTION,
    CONFIRMATION
  }

  private TextView tvSdkStatus;
  private TabLayout tabLayout;
  private ViewPager2 viewPager;
  private SectionsPagerAdapter pagerAdapter;

  private ProductConfig productConfig;
  private PatrolNavManager patrolNavManager;
  private final IdleTimerManager idleTimerManager = new IdleTimerManager();
  private DualScreenManager dualScreenMgr;

  private AppState currentState = AppState.PATROL;
  private boolean sdkReady;
  private boolean patrolStarted;
  /** Only first successful SDK init starts patrol / dual-screen shell; reinit from Developer tab skips this. */
  private boolean supermarketShellStarted;

  private final PeanutSDK.ErrorListener errorListener =
      errorCode -> {
        Log.d(TAG, "onInit:" + errorCode);
        runOnUiThread(
            () -> {
              if (tvSdkStatus == null) return;
              if (errorCode == SDK_INIT_SUCCESS) {
                tvSdkStatus.setTextColor(Color.GREEN);
                tvSdkStatus.setText(getString(R.string.str_init_text) + errorCode);
                PeanutRuntime.getInstance()
                    .start(
                        new PeanutRuntime.Listener() {
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
                sdkReady = true;
                if (!supermarketShellStarted) {
                  supermarketShellStarted = true;
                  onSdkReady();
                }
              } else {
                tvSdkStatus.setTextColor(Color.RED);
                tvSdkStatus.setText(getString(R.string.str_init_text) + errorCode);
              }
            });
      };

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_supermarket);

    tvSdkStatus = findViewById(R.id.tv_sdk_status);
    tvSdkStatus.setTextColor(Color.GREEN);
    tvSdkStatus.setText(getString(R.string.str_content) + VersionInfo.versionName);

    tabLayout = findViewById(R.id.tab_layout);
    viewPager = findViewById(R.id.view_pager);

    productConfig = new ProductConfig(this);
    productConfig.load();

    patrolNavManager = new PatrolNavManager(this);
    dualScreenMgr = new DualScreenManager(this);

    pagerAdapter = new SectionsPagerAdapter(this);
    viewPager.setAdapter(pagerAdapter);
    viewPager.setOffscreenPageLimit(4);

    new TabLayoutMediator(
            tabLayout,
            viewPager,
            (tab, position) -> {
              switch (position) {
                case SectionsPagerAdapter.TAB_AD:
                  tab.setText(R.string.tab_ad);
                  break;
                case SectionsPagerAdapter.TAB_VOICE:
                  tab.setText(R.string.tab_voice);
                  break;
                case SectionsPagerAdapter.TAB_INTEREST:
                  tab.setText(R.string.tab_interest);
                  break;
                case SectionsPagerAdapter.TAB_FEEDBACK:
                  tab.setText(R.string.tab_feedback);
                  break;
                case SectionsPagerAdapter.TAB_DEVELOPER:
                  tab.setText(R.string.tab_developer);
                  break;
                default:
                  tab.setText(R.string.tab_ad);
                  break;
              }
            })
        .attach();

    viewPager.registerOnPageChangeCallback(
        new ViewPager2.OnPageChangeCallback() {
          @Override
          public void onPageSelected(int position) {
            if (position != 0 && currentState == AppState.PATROL) {
              enterInteractionMode();
            }
            resetIdleTimer();
          }
        });

    requestAppPermissions();
    initSdk(getType());
  }

  private void onSdkReady() {
    patrolNavManager.init(productConfig.getPatrolConfig());
    dualScreenMgr.init();
    // Safety: opening app must not auto-drive/auto-charge.
    // Keep robot idle until patrol is explicitly started by user action.
    patrolStarted = false;
    enterInteractionMode();
  }

  @Override
  protected void onResume() {
    super.onResume();
    if (sdkReady && currentState == AppState.PATROL) {
      patrolNavManager.resumePatrol();
    }
  }

  @Override
  protected void onPause() {
    patrolNavManager.pausePatrol();
    super.onPause();
  }

  @Override
  protected void onDestroy() {
    idleTimerManager.cancelAll();
    dualScreenMgr.release();
    patrolNavManager.release();
    PeanutSDK.getInstance().release();
    super.onDestroy();
  }

  @Override
  public boolean dispatchTouchEvent(MotionEvent ev) {
    if (ev.getAction() == MotionEvent.ACTION_DOWN) {
      resetIdleTimer();
      if (currentState == AppState.PATROL) {
        enterInteractionMode();
      }
    }
    return super.dispatchTouchEvent(ev);
  }

  public void switchToTab(int index) {
    viewPager.setCurrentItem(index, true);
    if (index != SectionsPagerAdapter.TAB_AD) {
      enterInteractionMode();
    }
  }

  public void enterPatrolMode() {
    currentState = AppState.PATROL;
    tabLayout.setVisibility(android.view.View.GONE);
    viewPager.setCurrentItem(SectionsPagerAdapter.TAB_AD, false);
    viewPager.setUserInputEnabled(false);
    idleTimerManager.cancelAll();
    patrolNavManager.resumePatrol();
  }

  public void enterInteractionMode() {
    if (currentState == AppState.INTERACTION) {
      idleTimerManager.restartGlobalIdle(90_000);
      patrolNavManager.pausePatrol();
      return;
    }
    currentState = AppState.INTERACTION;
    tabLayout.setVisibility(android.view.View.VISIBLE);
    viewPager.setUserInputEnabled(true);
    patrolNavManager.pausePatrol();
    idleTimerManager.cancelAll();
    idleTimerManager.startGlobalIdle(90_000, this::enterPatrolMode);
  }

  public void enterConfirmationMode() {
    currentState = AppState.CONFIRMATION;
    idleTimerManager.cancelAll();
    idleTimerManager.startConfirmIdle(
        10_000,
        () -> {
          switchToTab(SectionsPagerAdapter.TAB_AD);
          enterPatrolMode();
        });
  }

  public void resetIdleTimer() {
    if (currentState == AppState.INTERACTION) {
      idleTimerManager.restartGlobalIdle(90_000);
    }
  }

  public ProductConfig getProductConfig() {
    return productConfig;
  }

  public PatrolNavManager getPatrolNavManager() {
    return patrolNavManager;
  }

  public DualScreenManager getDualScreenManager() {
    return dualScreenMgr;
  }

  public boolean isSdkReady() {
    return sdkReady;
  }

  private void initSdk(String ip) {
    PeanutConfig.getConfig()
        .setLinkType(
            PeanutConstants.REMOTE_LINK_PROXY.equals(ip)
                ? PeanutConstants.LinkType.COAP
                : PeanutConstants.LinkType.COM_COAP)
        .setLinkIP(ip)
        .enableLog(true)
        .setLogLevel(Log.DEBUG)
        .setAppId("bcb8ebc7f22345bebb378aead035cfb3")
        .setSecret(
            "nPlQERTP4qJWimTp0+ZXXkM5ND93iEyWpM6eXAGIZ/HQmyEg8zN7x5tGLebwINKLYScXEjg5lhQBvt1QCODovm2gq7dsXAK4pgjBRK2OqQHxl4nvTjq2AX9Or6XrdfFfVgOiHqW0mw+qWGDJc1/EUBg3llLOzMNUiDqwPsXMZYs=")
        .enableUMLog(false);
    PeanutSDK.getInstance().init(getApplicationContext(), errorListener);
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
    return getSharedPreferences("SP", Context.MODE_PRIVATE)
        .getString("type", PeanutConstants.REMOTE_LINK_PROXY);
  }

  @Override
  public void saveToSP(String type) {
    SharedPreferences sp = getSharedPreferences("SP", Context.MODE_PRIVATE);
    sp.edit().putString("type", type).apply();
  }

  @Override
  public void reinitSDK(String ip) {
    PeanutSDK.getInstance().release();
    initSdk(ip);
    if (tvSdkStatus != null) {
      tvSdkStatus.setTextColor(Color.YELLOW);
      tvSdkStatus.setText("Reconnecting...");
    }
  }

  @Override
  public void pausePatrolIfActive() {
    try {
      if (patrolNavManager != null) {
        patrolNavManager.pausePatrol();
      }
    } catch (Throwable t) {
      Log.w(TAG, "pausePatrolIfActive failed", t);
    }
  }
}
