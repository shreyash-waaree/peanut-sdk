package com.keenon.peanut.sample.chassis;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import com.keenon.common.constant.PeanutConstants;
import com.keenon.peanut.sample.R;
import com.keenon.peanut.sample.util.BaseActivity;
import com.keenon.peanut.sample.util.PrintLnLog;
import com.keenon.sdk.external.IDataCallback;
import com.keenon.sdk.external.PeanutSDK;
import com.keenon.sdk.hedera.model.ApiError;
import com.keenon.sdk.sensor.light.LightConfig;
import com.keenon.sdk.sensor.light.SensorLight;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class T8LightDemo extends BaseActivity {

  private static final String TAG = "T8LightDemo";

  @BindView(R.id.tv_api_log)
  TextView tvApiLog;
  @BindView(R.id.sv_api_log)
  ScrollView svApiLog;
  @BindView(R.id.spinner_led_device_multi)
  Spinner spinnerLedDevice;
  @BindView(R.id.spinner_led_effect)
  Spinner spinnerLightEffect;
  @BindView(R.id.tv_led_time)
  EditText tvLedTime;
  private StringBuilder sb = new StringBuilder();

  // LED灯效编号定义
  public static final int EFFECT_0 = 0x00;
  public static final int EFFECT_1 = 0x01;
  public static final int EFFECT_2 = 0x02;
  public static final int EFFECT_3 = 0x03;
  public static final int EFFECT_4 = 0x04;
  public static final int EFFECT_5 = 0x05;
  public static final int EFFECT_6 = 0x06;
  public static final int EFFECT_7 = 0x07;
  public static final int EFFECT_8 = 0x08;
  public static final int EFFECT_9 = 0x09;
  public static final int EFFECT_10 = 0x0a;
  public static final int EFFECT_11 = 0x0b;

  // LED灯效完整定义
  static LightConfig.Effect ledEffect_0 = new LightConfig.Effect(EFFECT_0, 0, "默认效果");//Default effect
  static LightConfig.Effect ledEffect_1 = new LightConfig.Effect(EFFECT_1, 0, "灯光全灭");//All lights go out
  static LightConfig.Effect ledEffect_2 = new LightConfig.Effect(EFFECT_2, 0, "青色闪烁");//Cyan flashing
  static LightConfig.Effect ledEffect_3 = new LightConfig.Effect(EFFECT_3, 0, "红色闪烁");//Red flashing
  static LightConfig.Effect ledEffect_4 = new LightConfig.Effect(EFFECT_4, 0, "渐变色流星灯（方向自高灯珠编号到低灯珠编号）");//Gradient meteor light (direction from high bead number to low bead number)
  static LightConfig.Effect ledEffect_5 = new LightConfig.Effect(EFFECT_5, 0, "渐变色流星灯（方向自低灯珠编号到高灯珠编号）");//Gradient meteor light (direction from low bead number to high bead number
  static LightConfig.Effect ledEffect_6 = new LightConfig.Effect(EFFECT_6, 0, "黄色闪烁");//Yellow flashing
  static LightConfig.Effect ledEffect_7 = new LightConfig.Effect(EFFECT_7, 0, "青色呼吸");//Cyan respiration
  static LightConfig.Effect ledEffect_8 = new LightConfig.Effect(EFFECT_8, 0, "青色常亮");//Cyan always bright
  static LightConfig.Effect ledEffect_9 = new LightConfig.Effect(EFFECT_9, 0, "律动模式");//Rhythmic mode
  static LightConfig.Effect ledEffect_10 = new LightConfig.Effect(EFFECT_10, 0, "绿色旋转（顺时针）");//Green rotation (clockwise)
  static LightConfig.Effect ledEffect_11 = new LightConfig.Effect(EFFECT_11, 0, "绿色旋转（逆时针）");//Green rotation (counterclockwise)

  // T8小机型LED灯带设备定义，包括设备ID、灯珠数目、灯光效果，led色彩类型等
  private static List<LightConfig> ledDeviceListT8 = new ArrayList<>();

  // T5Pro，警示灯灯效
  private static List<LightConfig.Effect> ledEffectListT8 = new ArrayList<>();

  static {
    ledEffectListT8.add(ledEffect_0);
    ledEffectListT8.add(ledEffect_1);
    ledEffectListT8.add(ledEffect_2);
    ledEffectListT8.add(ledEffect_3);
    ledEffectListT8.add(ledEffect_4);
    ledEffectListT8.add(ledEffect_5);
    ledEffectListT8.add(ledEffect_6);
    ledEffectListT8.add(ledEffect_7);
    ledEffectListT8.add(ledEffect_8);
    ledEffectListT8.add(ledEffect_9);
    ledEffectListT8.add(ledEffect_10);
    ledEffectListT8.add(ledEffect_11);
  }

  public static final int LED_DEV_GROUP_1 = 0x03;
  public static final int LED_DEV_GROUP_2 = 0x04;
  /**
   * T8机型，餐盘灯，上下两层
   */
  private static final LightConfig ledDevPlateTop = new LightConfig(LED_DEV_GROUP_1, "上层餐盘彩色灯", 1, 36, ledEffectListT8);
  private static final LightConfig ledDevPlateBottom = new LightConfig(LED_DEV_GROUP_2, "下层餐盘彩色灯", 1, 36, ledEffectListT8);

  static {
    ledDeviceListT8.add(ledDevPlateTop);
    ledDeviceListT8.add(ledDevPlateBottom);
  }
  private int curLed = 0;
  private int curLedEffect = 0;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_t8_light);
    ButterKnife.bind(this);
    setButtonBack();
    initData();
  }

  private void initData() {
    String[] mItems = getResources().getStringArray(R.array.led_device_T8);
    ArrayAdapter<String> adapterLedDevice = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, mItems);
    adapterLedDevice.setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item);
    spinnerLedDevice.setAdapter(adapterLedDevice);
    spinnerLedDevice.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
      @Override
      public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        Log.d(TAG, "device color onItemSelected() called with: parent = [" + parent + "], view = [" + view + "], position = [" + position + "], id = [" + id + "]");
        curLed = position;
        curLedEffect = 0;
        initLedEffectSpinner();
      }

      @Override
      public void onNothingSelected(AdapterView<?> parent) {

      }
    });
  }

  /**
   * 初始化灯光效果下拉选框
   */
  private void initLedEffectSpinner() {
    String[] mItems = getEffectList();
    ArrayAdapter<String> adapterLedEffect = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, mItems);
    adapterLedEffect.setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item);
    spinnerLightEffect.setAdapter(adapterLedEffect);
    spinnerLightEffect.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
      @Override
      public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        Log.d(TAG, "effect onItemSelected() called with: parent = [" + parent + "], view = [" + view + "], position = [" + position + "], id = [" + id + "]");
        curLedEffect = position;
      }

      @Override
      public void onNothingSelected(AdapterView<?> parent) {

      }
    });
  }

  private String[] getEffectList() {
    String[] items = new String[ledEffectListT8.size()];
    for (int i = 0; i < ledEffectListT8.size(); i++) {
      items[i] =ledEffectListT8.get(i).getDesc();
    }
    return items;
  }

  @SuppressLint("NonConstantResourceId")
  @OnClick({
      R.id.start_multi_color,
  })
  public void onViewClicked(View view) {
    switch (view.getId()) {
      case R.id.start_multi_color:
        playMultiColorLightV1();
        break;
      default:
        break;
    }
  }

  private void playMultiColorLightV1() {
    Log.d(TAG, "playMultiColorLightV1");

    if (tvLedTime.getText().length() == 0) {
      showToast(getStr(R.string.str_tips_1));
      return;
    }
    int ledTime = Integer.parseInt(tvLedTime.getText().toString());

    LightConfig lightConfig = ledDeviceListT8.get(curLed);
    lightConfig.setVer(PeanutConstants.SCM_VER_1);
    lightConfig.getEffect().setId(lightConfig.getSupportedEffect().get(curLedEffect).getId());
    lightConfig.getEffect().setTime(ledTime);//0 为持续长亮
    SensorLight.getInstance().play(lightConfig);
  }

  private String getStr(int id) {
    return getResources().getString(id);
  }

}
