package com.keenon.peanut.sample.chassis;

import android.os.Bundle;
import android.view.View;
import android.widget.ScrollView;
import android.widget.TextView;

import com.keenon.peanut.sample.R;
import com.keenon.peanut.sample.util.BaseActivity;
import com.keenon.peanut.sample.util.PrintLnLog;
import com.keenon.sdk.component.gating.callback.DoorListener;
import com.keenon.sdk.component.gating.data.Faults;
import com.keenon.sdk.component.gating.data.GatingType;
import com.keenon.sdk.component.gating.manager.PeanutDoor;
import com.keenon.sdk.component.gating.state.GatingState;
import com.keenon.sdk.external.IDataCallback;
import com.keenon.sdk.external.PeanutSDK;
import com.keenon.sdk.hedera.model.ApiError;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class DoorDemo extends BaseActivity{

    private static final String TAG = DoorDemo.class.getSimpleName();
    @BindView(R.id.tv_api_log)
    TextView tvApiLog;
    @BindView(R.id.sv_api_log)
    ScrollView svApiLog;
    private StringBuilder sb = new StringBuilder();
    private boolean door1,door2,door3,door4;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_door);
        ButterKnife.bind(this);
        setButtonBack();
        initData();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private void initData() {

    }

    @OnClick({R.id.btn_door1, R.id.btn_door2,
            R.id.btn_door3, R.id.btn_door4})
    public void onViewClicked(View view) {
        switch (view.getId()) {

            case R.id.btn_door1:
                door1 = !door1;
                controlDoor(0,door1);
                break;
            case R.id.btn_door2:
                door2 = !door2;
                controlDoor(1,door2);
                break;
            case R.id.btn_door3:
                door3 = !door3;
                controlDoor(2,door3);
                break;
            case R.id.btn_door4:
                door4 = !door4;
                controlDoor(3,door4);
                break;
        }
    }

    private void controlDoor(int doorId,boolean isOpen) {
        if (isOpen) {
            PeanutSDK.getInstance().door().open(new IDataCallback() {
                @Override
                public void success(String s) {
                    PrintLnLog.d(DoorDemo.this, tvApiLog, svApiLog, sb, "open success :" + doorId);
                }

                @Override
                public void error(ApiError apiError) {
                    PrintLnLog.d(DoorDemo.this, tvApiLog, svApiLog, sb, "open error :" + doorId);
                }
            },doorId);
        } else{
            PeanutSDK.getInstance().door().close(new IDataCallback() {
                @Override
                public void success(String s) {
                    PrintLnLog.d(DoorDemo.this, tvApiLog, svApiLog, sb, "close success :" + doorId);
                }

                @Override
                public void error(ApiError apiError) {
                    PrintLnLog.d(DoorDemo.this, tvApiLog, svApiLog, sb, "close error :" + doorId);
                }
            },doorId);
        }

    }


}
