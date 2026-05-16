package com.keenon.sdk.component.runtime;

import com.keenon.common.constant.PeanutConstants;
import com.keenon.common.external.PeanutConfig;
import com.keenon.common.utils.GsonUtil;
import com.keenon.common.utils.LogUtils;
import com.keenon.common.utils.PeanutMainThreadExecutor;
import com.keenon.common.utils.StringUtils;
import com.keenon.sdk.api.BatteryStatusApi;
import com.keenon.sdk.api.ButtonStatusApi;
import com.keenon.sdk.api.DeviceInfoApi;
import com.keenon.sdk.api.MotorStatusApi;
import com.keenon.sdk.api.NavigationAllApi;
import com.keenon.sdk.api.NavigationDestPoseApi;
import com.keenon.sdk.api.PositionRequestApi;
import com.keenon.sdk.api.RuntimeHeartbeatApi;
import com.keenon.sdk.api.RuntimeOdoApi;
import com.keenon.sdk.api.RuntimeRkIPApi;
import com.keenon.sdk.config.PeanutProperties;
import com.keenon.sdk.config.PropertiesKey;
import com.keenon.sdk.constant.PeanutTips;
import com.keenon.sdk.constant.TopicName;
import com.keenon.sdk.external.IDataCallback;
import com.keenon.sdk.external.PeanutEvent;
import com.keenon.sdk.external.PeanutSDK;
import com.keenon.sdk.hedera.model.ApiData;
import com.keenon.sdk.hedera.model.ApiError;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.json.JSONObject;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/component/runtime/PeanutRuntime.class */
public class PeanutRuntime {
    private static final String TAG = "[PeanutRuntime]";
    private static volatile PeanutRuntime sInstance;
    private volatile RuntimeInfo mRuntimeInfo;
    private int mWorkMode;
    private boolean mStarted;
    private final List<Listener> mListeners = new ArrayList();
    private IDataCallback mWorkModeCallback = new IDataCallback() { // from class: com.keenon.sdk.component.runtime.PeanutRuntime.8
        @Override // com.keenon.sdk.external.IDataCallback
        public void success(String response) {
            ApiData data = (ApiData) GsonUtil.gson2Bean(response, ApiData.class);
            if (null != data && data.getStatus() == 0) {
                LogUtils.d(PeanutConstants.TAG_RUNTIME, "[PeanutRuntime][setWorkMode][切换工作模式成功： " + PeanutRuntime.this.mWorkMode + "]");
                if (PeanutRuntime.this.mWorkMode != PeanutRuntime.this.mRuntimeInfo.getWorkMode()) {
                    PeanutRuntime.this.mRuntimeInfo.setWorkMode(PeanutRuntime.this.mWorkMode);
                    PeanutRuntime.this.notifyEvent(PeanutEvent.EVENT_WORK_MODE_CHANGED, Integer.valueOf(PeanutRuntime.this.mWorkMode));
                }
            }
        }

        @Override // com.keenon.sdk.external.IDataCallback
        public void error(ApiError error) {
            LogUtils.e(PeanutConstants.TAG_RUNTIME, "[PeanutRuntime][set work mode][" + error + "]");
            PeanutRuntime.this.notifyEvent(10000, null);
        }
    };
    private IDataCallback mSetEmergencyCallback = new IDataCallback() { // from class: com.keenon.sdk.component.runtime.PeanutRuntime.9
        @Override // com.keenon.sdk.external.IDataCallback
        public void success(String response) {
            ApiData data = (ApiData) GsonUtil.gson2Bean(response, ApiData.class);
            if (null != data && data.getStatus() == 0) {
                LogUtils.d(PeanutConstants.TAG_RUNTIME, "[PeanutRuntime][setEmergency]");
            }
        }

        @Override // com.keenon.sdk.external.IDataCallback
        public void error(ApiError error) {
            LogUtils.e(PeanutConstants.TAG_RUNTIME, "[PeanutRuntime][set emergency][" + error + "]");
            PeanutRuntime.this.notifyEvent(10000, null);
        }
    };
    private IDataCallback mEmergencyInfoCallback = new IDataCallback() { // from class: com.keenon.sdk.component.runtime.PeanutRuntime.10
        @Override // com.keenon.sdk.external.IDataCallback
        public void success(String response) {
            boolean state;
            ButtonStatusApi.Bean data = (ButtonStatusApi.Bean) GsonUtil.gson2Bean(response, ButtonStatusApi.Bean.class);
            if (null != data && null != data.getData() && (state = data.getData().isInfo()) != PeanutRuntime.this.mRuntimeInfo.isEmergencyOpen()) {
                PeanutRuntime.this.mRuntimeInfo.setEmergencyOpen(state);
                PeanutRuntime.this.notifyEvent(PeanutEvent.EVENT_EMERGENCY_STATUS_CHANGED, Boolean.valueOf(data.getData().isInfo()));
            }
        }

        @Override // com.keenon.sdk.external.IDataCallback
        public void error(ApiError error) {
            LogUtils.e(PeanutConstants.TAG_RUNTIME, "[PeanutRuntime][get emergency status][" + error + "]");
            PeanutRuntime.this.notifyEvent(10000, null);
        }
    };
    private IDataCallback mMotorInfoCallback = new IDataCallback() { // from class: com.keenon.sdk.component.runtime.PeanutRuntime.11
        @Override // com.keenon.sdk.external.IDataCallback
        public void success(String response) {
            int motorStatus;
            MotorStatusApi.Bean data = (MotorStatusApi.Bean) GsonUtil.gson2Bean(response, MotorStatusApi.Bean.class);
            if (null != data && null != data.getData() && (motorStatus = data.getData().getStatus()) > -1 && motorStatus != PeanutRuntime.this.mRuntimeInfo.getMotorStatus()) {
                PeanutRuntime.this.notifyEvent(PeanutEvent.EVENT_MOTOR_STATUS_CHANGED, Integer.valueOf(motorStatus));
                PeanutRuntime.this.mRuntimeInfo.setMotorStatus(motorStatus);
            }
        }

        @Override // com.keenon.sdk.external.IDataCallback
        public void error(ApiError error) {
            LogUtils.e(PeanutConstants.TAG_RUNTIME, "[PeanutRuntime][get motor status][" + error + "]");
            PeanutRuntime.this.notifyEvent(10000, null);
        }
    };
    private IDataCallback mRobotIPCallback = new IDataCallback() { // from class: com.keenon.sdk.component.runtime.PeanutRuntime.12
        @Override // com.keenon.sdk.external.IDataCallback
        public void success(String response) {
            RuntimeRkIPApi.Bean data = (RuntimeRkIPApi.Bean) GsonUtil.gson2Bean(response, RuntimeRkIPApi.Bean.class);
            if (null != data && data.getStatus() == 0) {
                String ip = data.getData().getIp();
                if (StringUtils.isNotEmpty(ip) && !ip.equals(PeanutRuntime.this.mRuntimeInfo.getRobotIp())) {
                    PeanutRuntime.this.mRuntimeInfo.setRobotIp(ip);
                    PeanutRuntime.this.notifyEvent(PeanutEvent.EVENT_IP_CHANGED, ip);
                }
            }
        }

        @Override // com.keenon.sdk.external.IDataCallback
        public void error(ApiError error) {
            LogUtils.e(PeanutConstants.TAG_RUNTIME, "[PeanutRuntime][get ip addr][" + error + "]");
            PeanutRuntime.this.notifyEvent(10000, null);
        }
    };
    private IDataCallback mArmInfoCallback = new IDataCallback() { // from class: com.keenon.sdk.component.runtime.PeanutRuntime.13
        @Override // com.keenon.sdk.external.IDataCallback
        public void success(String response) {
            DeviceInfoApi.Bean data = (DeviceInfoApi.Bean) GsonUtil.gson2Bean(response, DeviceInfoApi.Bean.class);
            if (null != data && data.getStatus() == 0) {
                LogUtils.d(PeanutConstants.TAG_RUNTIME, "[PeanutRuntime][获取arm板信息][" + response + "]");
                PeanutRuntime.this.mRuntimeInfo.setRobotArmInfo(response);
            }
        }

        @Override // com.keenon.sdk.external.IDataCallback
        public void error(ApiError error) {
            LogUtils.e(PeanutConstants.TAG_RUNTIME, "[PeanutRuntime][get arm info][" + error + "]");
            PeanutRuntime.this.notifyEvent(10000, null);
        }
    };
    private IDataCallback mStm32InfoCallback = new IDataCallback() { // from class: com.keenon.sdk.component.runtime.PeanutRuntime.14
        @Override // com.keenon.sdk.external.IDataCallback
        public void success(String response) {
            DeviceInfoApi.Bean data = (DeviceInfoApi.Bean) GsonUtil.gson2Bean(response, DeviceInfoApi.Bean.class);
            if (null != data && data.getStatus() == 0) {
                LogUtils.d(PeanutConstants.TAG_RUNTIME, "[PeanutRuntime][获取stm32板信息][" + response + "]");
                PeanutRuntime.this.mRuntimeInfo.setRobotStm32Info(response);
            }
        }

        @Override // com.keenon.sdk.external.IDataCallback
        public void error(ApiError error) {
            LogUtils.e(PeanutConstants.TAG_RUNTIME, "[PeanutRuntime][get stm32 info][" + error + "]");
            PeanutRuntime.this.notifyEvent(10000, null);
        }
    };
    private IDataCallback mPowerCallback = new IDataCallback() { // from class: com.keenon.sdk.component.runtime.PeanutRuntime.15
        @Override // com.keenon.sdk.external.IDataCallback
        public void success(String response) {
            int power;
            BatteryStatusApi.Bean data = (BatteryStatusApi.Bean) GsonUtil.gson2Bean(response, BatteryStatusApi.Bean.class);
            LogUtils.d(PeanutConstants.TAG_RUNTIME, "[PeanutRuntime][电量查询回调][" + response + "]");
            if (null != data && null != data.getData()) {
                int dumpPower = data.getData().getPower();
                if (dumpPower <= 0) {
                    power = 1;
                } else if (dumpPower > 100) {
                    power = 100;
                } else {
                    power = dumpPower;
                }
                if (power != PeanutRuntime.this.mRuntimeInfo.getPower()) {
                    PeanutRuntime.this.mRuntimeInfo.setPower(power);
                    PeanutRuntime.this.notifyEvent(PeanutEvent.EVENT_POWER_CHANGED, Integer.valueOf(power));
                }
            }
        }

        @Override // com.keenon.sdk.external.IDataCallback
        public void error(ApiError error) {
            LogUtils.e(PeanutConstants.TAG_RUNTIME, "[PeanutRuntime][get power status][" + error + "]");
            PeanutRuntime.this.notifyEvent(10000, null);
        }
    };
    private IDataCallback mOdoCallback = new IDataCallback() { // from class: com.keenon.sdk.component.runtime.PeanutRuntime.16
        @Override // com.keenon.sdk.external.IDataCallback
        public void success(String response) {
            LogUtils.d(PeanutConstants.TAG_RUNTIME, "[PeanutRuntime][odo][response = " + response + "]");
            RuntimeOdoApi.Bean data = (RuntimeOdoApi.Bean) GsonUtil.gson2Bean(response, RuntimeOdoApi.Bean.class);
            if (null != data && null != data.getData()) {
                Double odo = data.getData().getOdo();
                LogUtils.d(PeanutConstants.TAG_RUNTIME, "[PeanutRuntime][odo:" + odo + "]");
                if (odo.compareTo(new Double(0.0d)) > 0 && odo.compareTo(PeanutRuntime.this.mRuntimeInfo.getTotalOdo()) > 0) {
                    PeanutRuntime.this.mRuntimeInfo.setTotalOdo(odo);
                    PeanutRuntime.this.notifyEvent(PeanutEvent.EVENT_ODO_CHANGED, Double.valueOf(odo.doubleValue()));
                }
            }
        }

        @Override // com.keenon.sdk.external.IDataCallback
        public void error(ApiError error) {
            LogUtils.e(PeanutConstants.TAG_RUNTIME, "[PeanutRuntime][get odo status][" + error + "]");
            PeanutRuntime.this.notifyEvent(10000, null);
        }
    };
    private IDataCallback mHealthCallback = new IDataCallback() { // from class: com.keenon.sdk.component.runtime.PeanutRuntime.17
        @Override // com.keenon.sdk.external.IDataCallback
        public void success(String response) {
            LogUtils.d(PeanutConstants.TAG_RUNTIME, "[PeanutRuntime][health][response = " + response + "]");
            PeanutRuntime.this.notifyHealth(response);
        }

        @Override // com.keenon.sdk.external.IDataCallback
        public void error(ApiError error) {
            LogUtils.e(PeanutConstants.TAG_RUNTIME, "[PeanutRuntime][get health status][" + error + "]");
            PeanutRuntime.this.notifyEvent(10000, null);
        }
    };
    private IDataCallback mHeartbeatCallback = new IDataCallback() { // from class: com.keenon.sdk.component.runtime.PeanutRuntime.18
        @Override // com.keenon.sdk.external.IDataCallback
        public void success(String response) {
            LogUtils.d(PeanutConstants.TAG_RUNTIME, "[PeanutRuntime][heartbeat][response = " + response + "]");
            RuntimeHeartbeatApi.Bean data = (RuntimeHeartbeatApi.Bean) GsonUtil.gson2Bean(response, RuntimeHeartbeatApi.Bean.class);
            if (null != data && null != data.getData()) {
                int syncStatus = data.getData().getSync() == null ? -1 : data.getData().getSync().getStatus();
                if (PeanutRuntime.this.mRuntimeInfo.getSyncStatus() == -1) {
                    PeanutRuntime.this.mRuntimeInfo.setSyncStatus(syncStatus);
                } else if (syncStatus != -1 && syncStatus != PeanutRuntime.this.mRuntimeInfo.getSyncStatus()) {
                    LogUtils.i(PeanutRuntime.TAG, "robot reboot, current status: " + syncStatus + ", previous status: " + PeanutRuntime.this.mRuntimeInfo.getSyncStatus());
                    if (syncStatus == 0) {
                        PeanutRuntime.this.notifyEvent(PeanutEvent.EVENT_SYNC_PARAMS_REBOOT_STARTUP, PeanutTips.TIP_ROBOT_STARTUP);
                    } else if (syncStatus == 1) {
                        PeanutRuntime.this.notifyEvent(PeanutEvent.EVENT_SYNC_PARAMS_REBOOT_INIT, PeanutTips.TIP_ROBOT_INIT);
                        PeanutRuntime.this.start();
                    } else if (syncStatus == 2) {
                        PeanutRuntime.this.notifyEvent(PeanutEvent.EVENT_SYNC_PARAMS_REBOOT_READY, PeanutTips.TIP_ROBOT_READY);
                    }
                    PeanutRuntime.this.mRuntimeInfo.setSyncStatus(syncStatus);
                }
            }
            PeanutRuntime.this.notifyHeartbeat(response);
        }

        @Override // com.keenon.sdk.external.IDataCallback
        public void error(ApiError error) {
            LogUtils.e(PeanutConstants.TAG_RUNTIME, "[PeanutRuntime][get heartbeat status][" + error + "]");
            PeanutRuntime.this.notifyEvent(10000, null);
        }
    };
    private IDataCallback mSyncParamsCallback = new IDataCallback() { // from class: com.keenon.sdk.component.runtime.PeanutRuntime.19
        @Override // com.keenon.sdk.external.IDataCallback
        public void success(String response) {
            LogUtils.d(PeanutConstants.TAG_RUNTIME, "syncParams2Robot success " + response);
            ApiData data = (ApiData) GsonUtil.gson2Bean(response, ApiData.class);
            if (data.getCode() == -1) {
                PeanutRuntime.this.notifyEvent(PeanutEvent.EVENT_SYNC_PARAMS_FAILED, response);
            } else {
                PeanutRuntime.this.notifyEvent(PeanutEvent.EVENT_SYNC_PARAMS_SUCCESS, null);
            }
        }

        @Override // com.keenon.sdk.external.IDataCallback
        public void error(ApiError error) {
            LogUtils.e(PeanutConstants.TAG_RUNTIME, "[PeanutRuntime][sync params][" + error + "]");
            PeanutRuntime.this.notifyEvent(10000, null);
        }
    };
    private IDataCallback mLocationCallback = new IDataCallback() { // from class: com.keenon.sdk.component.runtime.PeanutRuntime.20
        @Override // com.keenon.sdk.external.IDataCallback
        public void success(String response) {
            LogUtils.d(PeanutConstants.TAG_RUNTIME, "location success " + response);
            PositionRequestApi.Bean data = (PositionRequestApi.Bean) GsonUtil.gson2Bean(response, PositionRequestApi.Bean.class);
            if (data.isSuccess() && data.getData() != null) {
                PeanutRuntime.this.notifyEvent(PeanutEvent.EVENT_LOCATION_SUCCESS, Integer.valueOf(data.getData().getStatus()));
            }
        }

        @Override // com.keenon.sdk.external.IDataCallback
        public void error(ApiError error) {
            LogUtils.e(PeanutConstants.TAG_RUNTIME, "[PeanutRuntime][set location][" + error + "]");
            PeanutRuntime.this.notifyEvent(10000, null);
        }
    };
    private IDataCallback mSetTimeCallback = new IDataCallback() { // from class: com.keenon.sdk.component.runtime.PeanutRuntime.21
        @Override // com.keenon.sdk.external.IDataCallback
        public void success(String response) {
            LogUtils.d(PeanutConstants.TAG_RUNTIME, "set time success " + response);
            PositionRequestApi.Bean data = (PositionRequestApi.Bean) GsonUtil.gson2Bean(response, PositionRequestApi.Bean.class);
            if (data.isSuccess() && data.getData() != null) {
                PeanutRuntime.this.notifyEvent(PeanutEvent.EVENT_SET_TIME_SUCCESS, Integer.valueOf(data.getData().getStatus()));
            }
        }

        @Override // com.keenon.sdk.external.IDataCallback
        public void error(ApiError error) {
            LogUtils.e(PeanutConstants.TAG_RUNTIME, "[PeanutRuntime][set time][" + error + "]");
            PeanutRuntime.this.notifyEvent(10000, null);
        }
    };
    private IDataCallback mGetPathCallback = new IDataCallback() { // from class: com.keenon.sdk.component.runtime.PeanutRuntime.22
        @Override // com.keenon.sdk.external.IDataCallback
        public void success(String response) {
            LogUtils.d(PeanutConstants.TAG_RUNTIME, "get path success " + response);
            PeanutRuntime.this.notifyEvent(PeanutEvent.EVENT_GET_PATH_SUCCESS, response);
        }

        @Override // com.keenon.sdk.external.IDataCallback
        public void error(ApiError error) {
            LogUtils.e(PeanutConstants.TAG_RUNTIME, "[PeanutRuntime][get path][" + error + "]");
            PeanutRuntime.this.notifyEvent(10000, null);
        }
    };
    private IDataCallback mRobotDestListCallback = new IDataCallback() { // from class: com.keenon.sdk.component.runtime.PeanutRuntime.23
        @Override // com.keenon.sdk.external.IDataCallback
        public void success(String response) {
            NavigationAllApi.Bean data = (NavigationAllApi.Bean) GsonUtil.gson2Bean(response, NavigationAllApi.Bean.class);
            if (null != data && data.getStatus() == 0) {
                PeanutRuntime.this.mRuntimeInfo.setDestList(response);
            }
        }

        @Override // com.keenon.sdk.external.IDataCallback
        public void error(ApiError error) {
            LogUtils.e(PeanutConstants.TAG_RUNTIME, "[PeanutRuntime][get dest list][" + error + "]");
            PeanutRuntime.this.notifyEvent(10000, null);
        }
    };
    private IDataCallback mRosDestListCallback = new IDataCallback() { // from class: com.keenon.sdk.component.runtime.PeanutRuntime.24
        @Override // com.keenon.sdk.external.IDataCallback
        public void success(String response) {
            NavigationDestPoseApi.Bean data = (NavigationDestPoseApi.Bean) GsonUtil.gson2Bean(response, NavigationDestPoseApi.Bean.class);
            if (null != data && data.getStatus() == 0) {
                PeanutRuntime.this.mRuntimeInfo.setDestList(response);
            }
        }

        @Override // com.keenon.sdk.external.IDataCallback
        public void error(ApiError error) {
            LogUtils.e(PeanutConstants.TAG_RUNTIME, "[PeanutRuntime][get dest list][" + error + "]");
            PeanutRuntime.this.notifyEvent(10000, null);
        }
    };
    private ScheduledExecutorService mScheduledThreadPool = Executors.newScheduledThreadPool(5);
    private PeanutMainThreadExecutor mainThreadExecutor = PeanutSDK.getInstance().getMainThreadExecutor();

    /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/component/runtime/PeanutRuntime$Listener.class */
    public interface Listener {
        void onEvent(int i, Object obj);

        void onHealth(Object obj);

        void onHeartbeat(Object obj);
    }

    private PeanutRuntime() {
    }

    public static PeanutRuntime getInstance() {
        if (sInstance == null) {
            synchronized (PeanutRuntime.class) {
                if (sInstance == null) {
                    sInstance = new PeanutRuntime();
                }
            }
        }
        return sInstance;
    }

    public void registerListener(Listener listener) {
        this.mListeners.add(listener);
    }

    public void removeListener(Listener listener) {
        this.mListeners.remove(listener);
    }

    public void start(Listener listener) {
        registerListener(listener);
        start();
    }

    public synchronized void start() {
        if (this.mStarted) {
            LogUtils.d(PeanutConstants.TAG_RUNTIME, "[runtime already start.]");
            return;
        }
        initData();
        initState();
        initScheduledTask();
        initSubscribeTask();
        this.mStarted = true;
    }

    public void destroy() {
        LogUtils.d(PeanutConstants.TAG_RUNTIME, "[destroy]");
        stopSubscribe();
        this.mScheduledThreadPool.shutdown();
        this.mainThreadExecutor.release();
        this.mListeners.clear();
        this.mRuntimeInfo = null;
        this.mStarted = false;
        sInstance = null;
    }

    public RuntimeInfo getRuntimeInfo() {
        return this.mRuntimeInfo;
    }

    public void setIP(String ip, int port) {
        LogUtils.i(PeanutConstants.TAG_RUNTIME, "[setIP]ip： " + ip + ", port: " + port + "]");
        PeanutConfig.getConfig().setLinkIP(ip);
        PeanutConfig.getConfig().setLinkPort(port);
    }

    private void initData() {
        this.mRuntimeInfo = new RuntimeInfo();
        this.mRuntimeInfo.setPower(1);
        this.mRuntimeInfo.setWorkMode(-1);
        this.mRuntimeInfo.setSyncStatus(-1);
        this.mRuntimeInfo.setMotorStatus(-1);
        this.mRuntimeInfo.setEmergencyOpen(false);
    }

    private void initState() {
        setWorkMode(1);
        queryPower();
        queryIpAddress();
        queryRobotArm();
        queryRobotStm32();
        queryDestList();
        syncHeartBeat();
        syncParams2Robot(false);
        if (PeanutProperties.getValue(PropertiesKey.APP_JERK_ENABLE, 1) != 1) {
            setEmergencyEnable(false);
            return;
        }
        setEmergencyEnable(true);
        queryLockButtonInfo();
        queryMotorStatus();
    }

    private void initScheduledTask() {
        syncRobotIP();
        syncRobotOdo();
        syncRobotHeartBeat();
        syncPower();
    }

    private void syncRobotIP() {
        this.mScheduledThreadPool.scheduleAtFixedRate(new Runnable() { // from class: com.keenon.sdk.component.runtime.PeanutRuntime.1
            @Override // java.lang.Runnable
            public void run() {
                PeanutRuntime.this.queryIpAddress();
            }
        }, 0L, 60L, TimeUnit.SECONDS);
    }

    private void syncRobotOdo() {
        this.mScheduledThreadPool.scheduleAtFixedRate(new Runnable() { // from class: com.keenon.sdk.component.runtime.PeanutRuntime.2
            @Override // java.lang.Runnable
            public void run() {
                PeanutRuntime.this.queryOdo();
            }
        }, 0L, 3L, TimeUnit.SECONDS);
    }

    private void syncRobotHeartBeat() {
        this.mScheduledThreadPool.scheduleAtFixedRate(new Runnable() { // from class: com.keenon.sdk.component.runtime.PeanutRuntime.3
            @Override // java.lang.Runnable
            public void run() {
                PeanutRuntime.this.queryHearBeat();
            }
        }, 0L, 5L, TimeUnit.SECONDS);
    }

    private void syncPower() {
        this.mScheduledThreadPool.scheduleAtFixedRate(new Runnable() { // from class: com.keenon.sdk.component.runtime.PeanutRuntime.4
            @Override // java.lang.Runnable
            public void run() {
                PeanutRuntime.this.queryPower();
            }
        }, 0L, 60L, TimeUnit.SECONDS);
    }

    private void initSubscribeTask() {
        stopSubscribe();
        startSubscribe();
    }

    private void startSubscribe() {
        PeanutSDK.getInstance().subscribe(TopicName.BUTTON_STATUS, this.mEmergencyInfoCallback);
        PeanutSDK.getInstance().subscribe(TopicName.MOTOR_STATUS, this.mMotorInfoCallback);
        PeanutSDK.getInstance().subscribe(TopicName.RUNTIME_HEALTH, this.mHealthCallback);
    }

    private void stopSubscribe() {
        PeanutSDK.getInstance().unSubscribe(TopicName.BUTTON_STATUS, this.mEmergencyInfoCallback);
        PeanutSDK.getInstance().unSubscribe(TopicName.MOTOR_STATUS, this.mMotorInfoCallback);
        PeanutSDK.getInstance().unSubscribe(TopicName.RUNTIME_HEALTH, this.mHealthCallback);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void queryHearBeat() {
        LogUtils.d(PeanutConstants.TAG_RUNTIME, "[PeanutRuntime][queryHearBeat]");
        PeanutSDK.getInstance().runtime().getHeartBeat(this.mHeartbeatCallback);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void queryPower() {
        LogUtils.d(PeanutConstants.TAG_RUNTIME, "[PeanutRuntime][queryPower]");
        PeanutSDK.getInstance().battery().getStatus(this.mPowerCallback);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void queryOdo() {
        LogUtils.d(PeanutConstants.TAG_RUNTIME, "[PeanutRuntime][queryOdo]");
        PeanutSDK.getInstance().runtime().getOdo(this.mOdoCallback);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void queryIpAddress() {
        LogUtils.d(PeanutConstants.TAG_RUNTIME, "[PeanutRuntime][queryIpAddress]");
        PeanutSDK.getInstance().runtime().getIPAddress(this.mRobotIPCallback);
    }

    private void queryRobotArm() {
        PeanutSDK.getInstance().device().getBoardInfo(this.mArmInfoCallback, "{\"id\": 1,\"name\": \"robot-arm\"}");
    }

    private void queryRobotStm32() {
        PeanutSDK.getInstance().device().getBoardInfo(this.mStm32InfoCallback, "{\"id\": 2,\"name\": \"robot-stm32\"}");
    }

    public void setEmergencyEnable(boolean enable) {
        PeanutSDK.getInstance().device().setButtonEnable(this.mSetEmergencyCallback, enable);
    }

    private void queryLockButtonInfo() {
        PeanutSDK.getInstance().device().getScramButtonStatus(this.mEmergencyInfoCallback);
    }

    private void queryMotorStatus() {
        PeanutSDK.getInstance().motor().getStatus(this.mMotorInfoCallback);
    }

    private void queryDestList() {
        PeanutSDK.getInstance().navigation().getAllTargets(this.mRobotDestListCallback);
        PeanutSDK.getInstance().navigation().getAllDestPose(this.mRosDestListCallback);
    }

    public void setWorkMode(int mode) {
        this.mWorkMode = mode;
        PeanutSDK.getInstance().runtime().setMode(this.mWorkModeCallback, this.mWorkMode);
    }

    public void setTime(long timestamp) {
        PeanutSDK.getInstance().runtime().setTime(this.mSetTimeCallback, timestamp);
    }

    public void getPath() {
        PeanutSDK.getInstance().runtime().getPath(this.mGetPathCallback);
    }

    public void syncParams2Robot(boolean needReboot) {
        HashMap<String, String> params = PeanutProperties.loadRobotParams();
        params.put("AppRestart", needReboot ? "0" : "1");
        PeanutSDK.getInstance().device().updateConfig(this.mSyncParamsCallback, new JSONObject(params).toString());
    }

    public void syncParams2RobotV2(boolean needReboot) {
        HashMap<String, String> params = PeanutProperties.loadRobotParams();
        params.put("AppRestart", needReboot ? "0" : "1");
        PeanutSDK.getInstance().device().updateConfigCompat(this.mSyncParamsCallback, new JSONObject(params).toString());
    }

    public void location() {
        PeanutSDK.getInstance().runtime().location(this.mLocationCallback);
    }

    private void syncHeartBeat() {
        PeanutSDK.getInstance().runtime().sync(null, System.currentTimeMillis());
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void notifyEvent(final int event, final Object object) {
        for (final Listener listener : this.mListeners) {
            LogUtils.d(PeanutConstants.TAG_RUNTIME, "[PeanutRuntime][notifyEvent][event： " + event + "]");
            this.mainThreadExecutor.execute(new Runnable() { // from class: com.keenon.sdk.component.runtime.PeanutRuntime.5
                @Override // java.lang.Runnable
                public void run() {
                    listener.onEvent(event, object);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void notifyHealth(final Object object) {
        for (final Listener listener : this.mListeners) {
            this.mainThreadExecutor.execute(new Runnable() { // from class: com.keenon.sdk.component.runtime.PeanutRuntime.6
                @Override // java.lang.Runnable
                public void run() {
                    listener.onHealth(object);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void notifyHeartbeat(final Object object) {
        for (final Listener listener : this.mListeners) {
            this.mainThreadExecutor.execute(new Runnable() { // from class: com.keenon.sdk.component.runtime.PeanutRuntime.7
                @Override // java.lang.Runnable
                public void run() {
                    listener.onHeartbeat(object);
                }
            });
        }
    }
}
