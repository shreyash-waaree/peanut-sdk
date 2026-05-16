package com.keenon.peanut.sample.chassis;

import android.os.Bundle;
import android.view.View;
import android.widget.ScrollView;
import android.widget.TextView;

import com.keenon.peanut.sample.R;
import com.keenon.peanut.sample.util.BaseActivity;
import com.keenon.peanut.sample.util.PrintLnLog;
import com.keenon.sdk.sensor.map.MapManager;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MapDemo extends BaseActivity {

    private static final String TAG = MapDemo.class.getSimpleName();
    @BindView(R.id.tv_api_log)
    TextView tvApiLog;
    @BindView(R.id.sv_api_log)
    ScrollView svApiLog;
    private StringBuilder sb = new StringBuilder();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);
        ButterKnife.bind(this);
        setButtonBack();
        initData();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        MapManager.getInstance().release();
    }

    private void initData() {
        MapManager.getInstance().addListen(new MapManager.MapListen() {
            @Override
            public void onResult(int code) {
                PrintLnLog.d(MapDemo.this, tvApiLog, svApiLog, sb, "onResult Code :" + code);
            }
        });
    }

    @OnClick({R.id.btn_import, R.id.btn_export})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.btn_import:
                //指定目录
//                MapManager.getInstance().onImportToRos(MapManager.SDCARD + "map" + File.separator + MapManager.ROS_DS_FILE_ZIP_NAME);
                MapManager.getInstance().onImportToRos();//默认Sdcard
                break;
            case R.id.btn_export:
                //指定目录
//                MapManager.getInstance().onExportToAndroid(MapManager.SDCARD + "map" + File.separator + MapManager.ROS_DS_FILE_NAME);
                MapManager.getInstance().onExportToAndroid();//默认Sdcard
                break;
        }
    }

}
