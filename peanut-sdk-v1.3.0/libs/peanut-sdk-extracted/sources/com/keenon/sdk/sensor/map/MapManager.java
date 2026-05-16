package com.keenon.sdk.sensor.map;

import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import androidx.annotation.NonNull;
import com.keenon.common.utils.FileUtil;
import com.keenon.common.utils.GsonUtil;
import com.keenon.common.utils.LogUtils;
import com.keenon.sdk.api.MapDownLoadStatusV2Api;
import com.keenon.sdk.api.SystemStartUpApi;
import com.keenon.sdk.external.IDataCallback;
import com.keenon.sdk.external.PeanutSDK;
import com.keenon.sdk.hedera.model.ApiError;
import com.keenon.sdk.sensor.common.Event;
import com.keenon.sdk.sensor.common.Sensor;
import com.keenon.sdk.sensor.common.SensorObserver;
import java.io.File;
import java.io.IOException;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/sensor/map/MapManager.class */
public class MapManager {
    private static final String TAG = "MapManager";
    private static volatile MapManager sInstance;
    public static final int COPY_TO_ROS_SUCCESS = 1001;
    public static final int COPY_TO_ANDROID_SUCCESS = 1002;
    public static final int COPY_TO_ROS_FAIL = 2001;
    public static final int COPY_TO_ANDROID_FAIL = 2002;
    private static final long DELAY_TIME = 5000;
    private static final long TIMEOUT = 180000;
    private static final int CODE_TIMEOUT = 101;
    private static final int CODE_DELAY = 102;
    private MapListen mapListen;
    private Handler mHandler;
    public static final String SDCARD = Environment.getExternalStorageDirectory() + File.separator;
    public static final String ROS_DS_FILE_NAME = "peanut.db";
    private static final String ROS_DB_PATH = SDCARD + "ros" + File.separator + "config" + File.separator + "database" + File.separator + ROS_DS_FILE_NAME;
    public static final String ROS_DS_FILE_ZIP_NAME = "PeanutDB.zip";
    private static final String DEFAULT_ZIP_PATH = SDCARD + ROS_DS_FILE_ZIP_NAME;
    private static final String DEFAULT_FILE_PATH = SDCARD + ROS_DS_FILE_NAME;
    private SensorObserver sensorObserver = new SensorObserver() { // from class: com.keenon.sdk.sensor.map.MapManager.2
        @Override // com.keenon.sdk.sensor.common.SensorObserver
        public void onUpdate(Event event, Sensor sensor) {
            LogUtils.d(MapManager.TAG, "onUpdate Event " + event.toString());
            switch (event.getName()) {
                case "_sensor.map.download.fault.event":
                    ErrorCodeBean bean = (ErrorCodeBean) GsonUtil.gson2Bean(GsonUtil.bean2String(event.getData()), ErrorCodeBean.class);
                    MapManager.this.handleMapEvent(bean.getError());
                    break;
                case "_sensor.map.download.status":
                    LogUtils.d(MapManager.TAG, "onUpdate Event _sensor.map.download.status");
                    MapDownLoadStatusV2Api.Bean statusBean = (MapDownLoadStatusV2Api.Bean) GsonUtil.gson2Bean(event.getData().toString(), MapDownLoadStatusV2Api.Bean.class);
                    MapManager.this.handleMapStatus(statusBean);
                    break;
            }
        }
    };
    private HandlerThread mHandlerThread = new HandlerThread("#MapManager");

    /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/sensor/map/MapManager$MapListen.class */
    public interface MapListen {
        void onResult(int i);
    }

    public static MapManager getInstance() {
        if (sInstance == null) {
            synchronized (MapManager.class) {
                if (sInstance == null) {
                    sInstance = new MapManager();
                }
            }
        }
        return sInstance;
    }

    private MapManager() {
        this.mHandlerThread.start();
        this.mHandler = new Handler(this.mHandlerThread.getLooper()) { // from class: com.keenon.sdk.sensor.map.MapManager.1
            @Override // android.os.Handler
            public void handleMessage(@NonNull Message msg) {
                int what = msg.what;
                if (101 == what) {
                    MapManager.this.notifyFail();
                } else if (102 == what) {
                    MapManager.this.onLoopRebootState();
                }
            }
        };
    }

    public void addListen(MapListen listen) {
        this.mapListen = listen;
    }

    public void onImportToRos(String filePath) {
        LogUtils.d(TAG, "onImportToRos");
        this.mHandler.sendEmptyMessageDelayed(101, TIMEOUT);
        if (filePath == null || filePath.isEmpty()) {
            LogUtils.e(TAG, "filePath isEmpty", (Exception) new NullPointerException());
            notifyFail();
            return;
        }
        File file = new File(filePath);
        if (!file.exists()) {
            LogUtils.e(TAG, "file isEmpty Path : " + filePath, (Exception) new NullPointerException());
            notifyFail();
            return;
        }
        MapConfig mapConfig = new MapConfig();
        mapConfig.setFileName(ROS_DS_FILE_ZIP_NAME);
        mapConfig.setFileDirectory(filePath.replace(ROS_DS_FILE_ZIP_NAME, ""));
        mapConfig.setType("database");
        mapConfig.setFileMd5(FileUtil.getFileMD5(file));
        mapConfig.setMapUuid(mapConfig.getFileMd5());
        if (!SensorMap.getInstance().contains(this.sensorObserver)) {
            SensorMap.getInstance().addObserver(this.sensorObserver);
        }
        SensorMap.getInstance().download(mapConfig);
    }

    public void onImportToRos() {
        onImportToRos(DEFAULT_ZIP_PATH);
    }

    public void onExportToAndroid(String savePath) {
        LogUtils.d(TAG, "onExportToAndroid");
        if (savePath == null || savePath.isEmpty()) {
            LogUtils.e(TAG, "SavePath isEmpty", (Exception) new NullPointerException());
            notifyState(COPY_TO_ANDROID_FAIL);
        } else {
            File parentFolder = new File(savePath).getParentFile();
            if (parentFolder != null) {
                parentFolder.mkdirs();
            }
            new Thread(() -> {
                try {
                    FileUtil.copyFile(ROS_DB_PATH, savePath);
                    String zipPath = savePath.replace(ROS_DS_FILE_NAME, ROS_DS_FILE_ZIP_NAME);
                    FileUtil.compressFile(savePath, zipPath);
                    FileUtil.deleteFile(new File(savePath));
                    notifyState(1002);
                } catch (IOException e) {
                    notifyState(COPY_TO_ANDROID_FAIL);
                    e.printStackTrace();
                    LogUtils.e(TAG, "[onCopyToAndroid]" + e.getMessage());
                }
            }).start();
        }
    }

    public void onExportToAndroid() {
        onExportToAndroid(DEFAULT_FILE_PATH);
    }

    private void onStartReboot() {
        LogUtils.d(TAG, "onStartReboot");
        PeanutSDK.getInstance().device().reboot(new IDataCallback() { // from class: com.keenon.sdk.sensor.map.MapManager.3
            @Override // com.keenon.sdk.external.IDataCallback
            public void success(String result) {
                LogUtils.d(MapManager.TAG, "onStartReboot success");
            }

            @Override // com.keenon.sdk.external.IDataCallback
            public void error(ApiError error) {
                LogUtils.d(MapManager.TAG, "onStartReboot error");
                MapManager.this.notifyFail();
            }
        });
        onLoopRebootState();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onLoopRebootState() {
        LogUtils.d(TAG, "onLoopRebootState");
        PeanutSDK.getInstance().runtime().getSystemStartUpState(new IDataCallback() { // from class: com.keenon.sdk.sensor.map.MapManager.4
            @Override // com.keenon.sdk.external.IDataCallback
            public void success(String result) {
                LogUtils.d(MapManager.TAG, "onLoopRebootState result :" + result);
                SystemStartUpApi.Bean bean = (SystemStartUpApi.Bean) GsonUtil.gson2Bean(result, SystemStartUpApi.Bean.class);
                if (bean.data == null || bean.data.status != 1) {
                    if (MapManager.this.mHandler != null) {
                        MapManager.this.mHandler.sendEmptyMessageDelayed(102, 5000L);
                    }
                } else {
                    MapManager.this.notifyState(1001);
                    if (MapManager.this.mHandler != null) {
                        MapManager.this.mHandler.removeCallbacksAndMessages(null);
                    }
                }
            }

            @Override // com.keenon.sdk.external.IDataCallback
            public void error(ApiError error) {
                if (MapManager.this.mHandler != null) {
                    MapManager.this.mHandler.sendEmptyMessageDelayed(102, 5000L);
                }
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void notifyFail() {
        notifyState(COPY_TO_ROS_FAIL);
        if (this.mHandler != null) {
            this.mHandler.removeCallbacksAndMessages(null);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleMapEvent(int code) {
        LogUtils.d(TAG, "handleMapEvent code :" + code);
        if (1004 == code) {
            onStartReboot();
        } else if (1001 == code) {
            notifyFail();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleMapStatus(MapDownLoadStatusV2Api.Bean bean) {
        LogUtils.d(TAG, "handleMapStatus");
        MapDownLoadStatusV2Api.Bean.DataBean dataBean = bean.getData();
        if (dataBean != null) {
            LogUtils.d(TAG, "handleMapStatus status :" + dataBean.getStatus());
            if (dataBean.getStatus() == 1) {
                onStartReboot();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void notifyState(int code) {
        LogUtils.i(TAG, "[notifyState code :" + code + "]");
        if (this.mapListen != null) {
            this.mapListen.onResult(code);
        }
    }

    public void release() {
        LogUtils.d(TAG, "release");
        SensorMap.getInstance().removeObserver(this.sensorObserver);
        SensorMap.getInstance().release();
        if (this.mHandler != null) {
            this.mHandler.removeCallbacks(null);
        }
        if (this.mHandlerThread != null) {
            this.mHandlerThread.quit();
        }
        this.mHandlerThread = null;
        this.mHandler = null;
        sInstance = null;
    }
}
