package com.keenon.peanut.sample.chassis;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ScrollView;
import android.widget.TextView;

import com.keenon.common.utils.LogUtils;
import com.keenon.peanut.sample.R;
import com.keenon.peanut.sample.util.BaseActivity;
import com.keenon.peanut.sample.util.PrintLnLog;
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

public class RosMapDemo extends BaseActivity {
  private static final String TAG = "RosMapDemo";


  private StringBuilder sb = new StringBuilder();


  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_charge);
    ButterKnife.bind(this);
    setButtonBack();
    initData();
  }

  private void initData() {
    //初始化

  }


  @OnClick({R.id.btn_auto_charge, R.id.btn_manual_charge, R.id.btn_stop_charge,R.id.btn_adapter_charge})
  public void onViewClicked(View view) {
    switch (view.getId()) {
      case R.id.btn_manual_charge:
        LogUtils.i(TAG, "Action :" + "btn_manual_charge");

        break;
      default:
        break;
    }
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();

  }
}
