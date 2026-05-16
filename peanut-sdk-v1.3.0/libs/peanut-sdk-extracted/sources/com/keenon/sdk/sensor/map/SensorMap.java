package com.keenon.sdk.sensor.map;

import com.keenon.common.utils.GsonUtil;
import com.keenon.sdk.api.MapDownloadV2Api;
import com.keenon.sdk.constant.TopicName;
import com.keenon.sdk.external.IDataCallback;
import com.keenon.sdk.external.PeanutSDK;
import com.keenon.sdk.hedera.model.ApiError;
import com.keenon.sdk.sensor.common.Sensor;
import com.keenon.sdk.sensor.common.SensorEvent;
import com.keenon.sdk.tftp.TFTPServerManager;
import java.io.File;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/sensor/map/SensorMap.class */
public final class SensorMap extends Sensor {
    private static final String TAG = "SensorMap";
    private static volatile SensorMap sInstance;
    private MapConfig mMapConfig;
    private static final int RESET = 1;
    public static final int ERROR_CODE_TFTP_FAIL = 1001;
    public static final int ERROR_CODE_DOWNLOAD_ING = 1002;
    public static final int ERROR_CODE_DOWNLOAD_UUID_REPEAT = 1003;
    public static final int ERROR_CODE_DOWNLOAD_RESTART_ROS = 1004;
    private final IDataCallback sendInfoCallBack = new IDataCallback() { // from class: com.keenon.sdk.sensor.map.SensorMap.2
        @Override // com.keenon.sdk.external.IDataCallback
        public void success(String result) {
            MapDownloadV2Api.Bean dataBean = (MapDownloadV2Api.Bean) GsonUtil.gson2Bean(result, MapDownloadV2Api.Bean.class);
            if (dataBean != null && dataBean.getData() != null && dataBean.getData().getStatus() != 1) {
                if (dataBean.getData().getStatus() == 2) {
                    SensorMap.this.notifyEvent(SensorEvent.REPORT_MAP_DOWNLOAD_FAULT_EVENT, new ErrorCodeBean(SensorMap.ERROR_CODE_DOWNLOAD_UUID_REPEAT));
                } else if (dataBean.getData().getStatus() == 3) {
                    SensorMap.this.notifyEvent(SensorEvent.REPORT_MAP_DOWNLOAD_FAULT_EVENT, new ErrorCodeBean(SensorMap.ERROR_CODE_DOWNLOAD_RESTART_ROS));
                }
            }
        }

        @Override // com.keenon.sdk.external.IDataCallback
        public void error(ApiError error) {
            SensorMap.this.notifyEvent(SensorEvent.REPORT_MAP_DOWNLOAD_FAULT_EVENT, new ErrorCodeBean(error.code));
        }
    };
    private final IDataCallback mCallBack = new IDataCallback() { // from class: com.keenon.sdk.sensor.map.SensorMap.3
        @Override // com.keenon.sdk.external.IDataCallback
        public void success(String result) {
            SensorMap.this.notifyEvent(SensorEvent.REPORT_MAP_DOWNLOAD_STATUS, result);
        }

        @Override // com.keenon.sdk.external.IDataCallback
        public void error(ApiError error) {
            SensorMap.this.notifyEvent(SensorEvent.REPORT_MAP_DOWNLOAD_FAULT_EVENT, new ErrorCodeBean(error.code));
        }
    };

    public static SensorMap getInstance() {
        if (sInstance == null) {
            synchronized (SensorMap.class) {
                if (sInstance == null) {
                    sInstance = new SensorMap();
                }
            }
        }
        return sInstance;
    }

    private SensorMap() {
        PeanutSDK.getInstance().subscribe(TopicName.MAP_DOWNLOAD_STATUS_V2API, this.mCallBack);
    }

    @Override // com.keenon.sdk.sensor.common.Sensor
    public String name() {
        return getClass().getSimpleName();
    }

    @Override // com.keenon.sdk.sensor.common.Sensor
    public void mount() {
    }

    @Override // com.keenon.sdk.sensor.common.Sensor
    public void unMount() {
    }

    @Override // com.keenon.sdk.sensor.common.Sensor
    public void release() {
        TFTPServerManager.getInstance().stop();
        PeanutSDK.getInstance().map().sendMapDownLoadAction(new IDataCallback() { // from class: com.keenon.sdk.sensor.map.SensorMap.1
            @Override // com.keenon.sdk.external.IDataCallback
            public void success(String result) {
            }

            @Override // com.keenon.sdk.external.IDataCallback
            public void error(ApiError error) {
            }
        }, 1);
        PeanutSDK.getInstance().unSubscribe(TopicName.MAP_DOWNLOAD_STATUS_V2API, this.mCallBack);
        sInstance = null;
    }

    public void download(MapConfig config) {
        this.mMapConfig = config;
        if (this.mMapConfig == null) {
            log("startTftp", "mMapConfig is NULL");
        } else {
            log("startTftp", "mMapConfig is " + this.mMapConfig.toString());
            startTftp();
        }
    }

    private void startTftp() {
        if (!TFTPServerManager.getInstance().isRunning() && !TFTPServerManager.getInstance().init(this.mMapConfig.getFileDirectory())) {
            notifyEvent(SensorEvent.REPORT_MAP_DOWNLOAD_FAULT_EVENT, new ErrorCodeBean(1001));
        }
        String uri = TFTPServerManager.getInstance().getUri() + File.separator + this.mMapConfig.getFileName();
        PeanutSDK.getInstance().map().sendDownloadInfo(this.sendInfoCallBack, this.mMapConfig.getFileMd5(), uri, this.mMapConfig.getType(), this.mMapConfig.getMapUuid());
    }
}
