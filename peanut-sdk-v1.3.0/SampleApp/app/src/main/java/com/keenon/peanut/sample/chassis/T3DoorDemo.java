package com.keenon.peanut.sample.chassis;

import android.os.Bundle;
import android.view.View;
import android.widget.ScrollView;
import android.widget.TextView;

import com.keenon.common.utils.LogUtils;
import com.keenon.peanut.sample.R;
import com.keenon.peanut.sample.util.BaseActivity;
import com.keenon.peanut.sample.util.PrintLnLog;
import com.keenon.sdk.constant.TopicName;
import com.keenon.sdk.external.IDataCallback;
import com.keenon.sdk.external.PeanutSDK;
import com.keenon.sdk.hedera.model.ApiError;
import com.keenon.sdk.scmIot.protopack.base.ProtoDev;
import com.keenon.sdk.sensor.common.Event;
import com.keenon.sdk.sensor.common.Sensor;
import com.keenon.sdk.sensor.common.SensorEvent;
import com.keenon.sdk.sensor.common.SensorObserver;
import com.keenon.sdk.sensor.door.SensorDoor;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class T3DoorDemo extends BaseActivity{

    private static final String TAG = T3DoorDemo.class.getSimpleName();
    @BindView(R.id.tv_api_log)
    TextView tvApiLog;
    @BindView(R.id.sv_api_log)
    ScrollView svApiLog;
    private StringBuilder sb = new StringBuilder();
    private boolean door1,door2;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_door_t3);
        ButterKnife.bind(this);
        setButtonBack();
        initData();
    }
    SensorObserver observer = new SensorObserver() {
        @Override
        public void onUpdate(Event event, Sensor sensor) {
            LogUtils.d(TAG,"onUpdate : Event -> " + event.toString());
            switch (event.getName()) {
                case SensorEvent.SET_DOOR_SWITCH_ACK:
                    break;
            }
        }
    };
    /**
     * Door Status
     *
     * -1 open
     * 1  opnIng or closeIng
     * 0 close
     */
    IDataCallback callback = new IDataCallback() {
        @Override
        public void success(String s) {
            PrintLnLog.d(T3DoorDemo.this, tvApiLog, svApiLog, sb, "callback -> " + s);
        }

        @Override
        public void error(ApiError apiError) {

        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        SensorDoor.getInstance().removeObserver(observer);
        SensorDoor.getInstance().release();
        PeanutSDK.getInstance().unSubscribe(TopicName.DOOR_SWITCH_STATUS, callback);
    }

    private void initData() {
        PeanutSDK.getInstance().subscribe(TopicName.DOOR_SWITCH_STATUS, callback);
        SensorDoor.getInstance().addObserver(observer);
        SensorDoor.getInstance().setUSBDirect(true);
    }


    @OnClick({R.id.btn_door1, R.id.btn_door2})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.btn_door1:
                door1 = !door1;
                SensorDoor.getInstance().setDoorSwitch(ProtoDev.SENSOR_DOOR_1,door1);
                break;
            case R.id.btn_door2:
                door2 = !door2;
                SensorDoor.getInstance().setDoorSwitch(ProtoDev.SENSOR_DOOR_2,door2);
                break;
        }
    }

}
