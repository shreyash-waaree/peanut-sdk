package com.keenon.peanut.sample.chassis;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ScrollView;
import android.widget.TextView;

import com.keenon.peanut.sample.R;
import com.keenon.peanut.sample.util.BaseActivity;
import com.keenon.peanut.sample.util.PrintLnLog;
import com.keenon.common.utils.LogUtils;
import com.keenon.sdk.component.charger.PeanutCharger;
import com.keenon.sdk.component.charger.common.Charger;
import com.keenon.sdk.component.charger.common.ChargerInfo;
import com.keenon.sdk.constant.TopicName;
import com.keenon.sdk.external.IDataCallback;
import com.keenon.sdk.external.PeanutSDK;
import com.keenon.sdk.hedera.model.ApiError;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnCheckedChanged;
import butterknife.OnClick;

public class ChargerDemo extends BaseActivity {
  private static final String TAG = "ChargerDemo";

  /**
   * When true, {@link PeanutCharger#CHARGE_ACTION_AUTO} is sent shortly after init (voice command flow).
   */
  public static final String EXTRA_START_AUTO_CHARGE =
          "com.keenon.peanut.sample.chassis.ChargerDemo.EXTRA_START_AUTO_CHARGE";

  @BindView(R.id.tv_api_log)
  TextView tvApiLog;
  @BindView(R.id.sv_api_log)
  ScrollView svApiLog;
  PeanutCharger mPeanutCharger;
  private StringBuilder sb = new StringBuilder();

  //充电回调
  Charger.Listener listener=new Charger.Listener() {
    @Override
    public void onChargerInfoChanged(int event, ChargerInfo chargerInfo) {
      Log.d(TAG, "event = " + event +
              " Power = " + chargerInfo.getPower() + " ChargeEvent = " + chargerInfo.getEvent());
    }

    @Override
    public void onChargerStatusChanged(int status) {
      PrintLnLog.d(ChargerDemo.this, tvApiLog, svApiLog, sb, "status = " + status);
    }

    @Override
    public void onError(int errorCode) {
      PrintLnLog.d(ChargerDemo.this, tvApiLog, svApiLog, sb, "errorCode = " + errorCode);
    }
  };

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_charge);
    ButterKnife.bind(this);
    setButtonBack();
    initData();
    maybeStartAutoChargeFromVoice();
  }

  /**
   * Same as tapping "Auto charge" on this screen, when opened from Voice tab charging intent.
   */
  private void maybeStartAutoChargeFromVoice() {
    if (!getIntent().getBooleanExtra(EXTRA_START_AUTO_CHARGE, false)) {
      return;
    }
    new Handler(Looper.getMainLooper()).postDelayed(() -> {
      if (isFinishing() || mPeanutCharger == null) {
        return;
      }
      LogUtils.i(TAG, "Voice: starting auto charge (CHARGE_ACTION_AUTO)");
      mPeanutCharger.performAction(PeanutCharger.CHARGE_ACTION_AUTO);
    }, 400L);
  }

  private void initData() {
    try {
      mPeanutCharger = new PeanutCharger.Builder()
          .setListener(listener)
          .build();
      mPeanutCharger.execute();
    } catch (Throwable t) {
      Log.e(TAG, "ChargerDemo init failed", t);
      PrintLnLog.d(ChargerDemo.this, tvApiLog, svApiLog, sb,
          "Charger init failed: " + t.getMessage());
    }
  }
  IDataCallback callback= new IDataCallback() {
    @Override
    public void success(String response) {
      PrintLnLog.d(ChargerDemo.this, tvApiLog, svApiLog, sb, "charge match success = " + response);
    }

    @Override
    public void error(ApiError error) {
      PrintLnLog.d(ChargerDemo.this, tvApiLog, svApiLog, sb, "charge match error = " + error.toString());
    }
  };
  @OnCheckedChanged({
      R.id.cb_auto_charge_match
  })
  public void onViewChecked(CompoundButton view, boolean isChecked) {
    switch (view.getId()) {
      case R.id.cb_auto_charge_match:
        if (isChecked) {
          PeanutSDK.getInstance().subscribe(TopicName.CHARGE_MATCH_TIMES,callback );
        } else {
          PeanutSDK.getInstance().unSubscribe(TopicName.CHARGE_MATCH_TIMES,callback);
        }
        break;
      default:
        break;
    }
  }

  @OnClick({R.id.btn_auto_charge, R.id.btn_manual_charge, R.id.btn_stop_charge,R.id.btn_adapter_charge})
  public void onViewClicked(View view) {
    if (mPeanutCharger == null) {
      PrintLnLog.d(ChargerDemo.this, tvApiLog, svApiLog, sb,
          "Charger not initialised — cannot perform action");
      return;
    }
    if (!isSdkBatteryReady()) {
      PrintLnLog.d(ChargerDemo.this, tvApiLog, svApiLog, sb,
          "SDK not connected to robot (battery component null). Check the SDK status line on the main screen — it must be GREEN/SUCCESS before charging.");
      return;
    }
    try {
      switch (view.getId()) {
        case R.id.btn_manual_charge:
          LogUtils.i(TAG, "Action :" + "btn_manual_charge");
          mPeanutCharger.performAction(PeanutCharger.CHARGE_ACTION_MANUAL);
          break;
        case R.id.btn_auto_charge:
          mPeanutCharger.performAction(PeanutCharger.CHARGE_ACTION_AUTO);
          break;
        case R.id.btn_stop_charge:
          mPeanutCharger.performAction(PeanutCharger.CHARGE_ACTION_STOP);
          break;
        case R.id.btn_adapter_charge:
          mPeanutCharger.performAction(PeanutCharger.CHARGE_ACTION_ADAPTER);
          break;
        default:
          break;
      }
    } catch (Throwable t) {
      Log.e(TAG, "performAction failed", t);
      PrintLnLog.d(ChargerDemo.this, tvApiLog, svApiLog, sb,
          "performAction failed: " + t.getMessage());
    }
  }

  /**
   * Probe whether {@link PeanutSDK#battery()} is usable right now. The SDK throws a NullComponent
   * exception if {@code release()} was called — or silently returns null if auth failed during
   * init. We swallow both so we never crash, just refuse the action.
   */
  private static boolean isSdkBatteryReady() {
    try {
      return PeanutSDK.getInstance().battery() != null;
    } catch (Throwable t) {
      return false;
    }
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();

    if (mPeanutCharger != null) {
      mPeanutCharger.release();
    }
  }
}
