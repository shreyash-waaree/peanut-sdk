package com.keenon.sdk.sensor.door;

import com.keenon.common.utils.GsonUtil;
import com.keenon.common.utils.LogUtils;
import com.keenon.common.utils.SomeUtils;
import com.keenon.sdk.constant.TopicName;
import com.keenon.sdk.external.IDataCallback;
import com.keenon.sdk.external.IDataCallback2;
import com.keenon.sdk.external.PeanutSDK;
import com.keenon.sdk.hedera.model.ApiError;
import com.keenon.sdk.scmIot.SCMIoTSender;
import com.keenon.sdk.scmIot.bean.SCMRequest;
import com.keenon.sdk.scmIot.bean.response.ReportInfo;
import com.keenon.sdk.sensor.common.Sensor;
import com.keenon.sdk.sensor.common.SensorEvent;
import com.keenon.sdk.sensor.common.SensorFaultInfo;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/sensor/door/SensorDoor.class */
public class SensorDoor extends Sensor {
    private static final String TAG = "SensorDoor";
    private static volatile SensorDoor sInstance;
    private int mCmdExeInterval = 30;
    private int[] mSupportDoors = {22, 23, 24, 25};
    private Map<Integer, DoorStatusInfo> mCacheDoorStatus = new HashMap();
    private Map<Integer, IDataCallback2<DoorStatusInfo>> mGetDoorStatusCallbacks = new HashMap();
    private ExecutorService mExecutor = Executors.newCachedThreadPool();
    private IDataCallback mDoorSwitchCallback = new IDataCallback() { // from class: com.keenon.sdk.sensor.door.SensorDoor.3
        @Override // com.keenon.sdk.external.IDataCallback
        public void success(String result) {
            SensorDoor.this.notifyEvent(SensorEvent.SET_DOOR_SWITCH_ACK, result);
        }

        @Override // com.keenon.sdk.external.IDataCallback
        public void error(ApiError error) {
            SensorDoor.this.notifyEvent(SensorEvent.SET_DOOR_SWITCH_ACK, error);
        }
    };
    private IDataCallback mReportCallback = new IDataCallback() { // from class: com.keenon.sdk.sensor.door.SensorDoor.4
        @Override // com.keenon.sdk.external.IDataCallback
        public void success(String result) {
            LogUtils.d(SensorDoor.TAG, "mReportCallback" + result);
            ReportInfo info = (ReportInfo) GsonUtil.gson2Bean(result, ReportInfo.class);
            SensorDoor.this.handleSwitchFaultReport(info);
        }

        @Override // com.keenon.sdk.external.IDataCallback
        public void error(ApiError error) {
        }
    };
    private IDataCallback mDoorStatusCallback = new IDataCallback() { // from class: com.keenon.sdk.sensor.door.SensorDoor.5
        @Override // com.keenon.sdk.external.IDataCallback
        public void success(String result) {
            LogUtils.d(SensorDoor.TAG, "mDoorStatusCallback" + result);
            DoorStatusInfo info = (DoorStatusInfo) GsonUtil.gson2Bean(result, DoorStatusInfo.class);
            SensorDoor.this.handleSwitchStatusReport(info);
        }

        @Override // com.keenon.sdk.external.IDataCallback
        public void error(ApiError error) {
        }
    };
    private final IDataCallback mGetDoorStatus = new IDataCallback() { // from class: com.keenon.sdk.sensor.door.SensorDoor.6
        @Override // com.keenon.sdk.external.IDataCallback
        public void success(String result) {
            LogUtils.d(SensorDoor.TAG, "getDoorStatus: " + result);
            DoorStatusInfo status = (DoorStatusInfo) GsonUtil.gson2Bean(result, DoorStatusInfo.class);
            if (null != SensorDoor.this.mGetDoorStatusCallbacks) {
                IDataCallback2<DoorStatusInfo> callback = (IDataCallback2) SensorDoor.this.mGetDoorStatusCallbacks.get(Integer.valueOf(status.getDoorId()));
                if (null != callback) {
                    callback.success(status);
                }
                SensorDoor.this.mGetDoorStatusCallbacks.remove(Integer.valueOf(status.getDoorId()));
            }
            SensorDoor.this.handleSwitchStatusReport(status);
        }

        @Override // com.keenon.sdk.external.IDataCallback
        public void error(ApiError error) {
        }
    };
    private final IDataCallback mBottomRwaCallBack = new IDataCallback() { // from class: com.keenon.sdk.sensor.door.SensorDoor.7
        @Override // com.keenon.sdk.external.IDataCallback
        public void success(String result) {
        }

        @Override // com.keenon.sdk.external.IDataCallback
        public void error(ApiError error) {
        }
    };

    public static SensorDoor getInstance() {
        if (sInstance == null) {
            synchronized (SensorDoor.class) {
                if (sInstance == null) {
                    sInstance = new SensorDoor();
                }
            }
        }
        return sInstance;
    }

    private SensorDoor() {
        SCMIoTSender.addSCMThingDataTransfer(16, new DoorStatusDataTransfer());
        PeanutSDK.getInstance().subscribe(TopicName.BOTTOM_RAW, this.mBottomRwaCallBack);
        init();
    }

    private void init() {
        this.mCacheDoorStatus.clear();
        SCMIoTSender.removeReportDataCallback(this.mSupportDoors);
        SCMIoTSender.addReportDataCallback(this.mReportCallback, this.mSupportDoors);
        for (int doorId : this.mSupportDoors) {
            this.mCacheDoorStatus.put(Integer.valueOf(doorId), new DoorStatusInfo(doorId, (byte) 17));
            SCMIoTSender.addSingeReportDataCallback(this.mGetDoorStatus, doorId, 16, 0);
            SCMIoTSender.addSingeReportDataCallback(this.mDoorStatusCallback, doorId, 16, 1);
        }
    }

    public void changeSupportDevices(int[] doorDevices) {
        if (null == doorDevices) {
            return;
        }
        this.mSupportDoors = doorDevices;
        init();
    }

    public void changeCmsExeInterval(int interval) {
        this.mCmdExeInterval = interval;
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
        SCMIoTSender.removeSCMThingDataTransfer(16);
        PeanutSDK.getInstance().unSubscribe(TopicName.BOTTOM_RAW, this.mBottomRwaCallBack);
        SCMIoTSender.removeReportDataCallback(this.mSupportDoors);
        for (int doorId : this.mSupportDoors) {
            SCMIoTSender.removeSingeReportDataCallback(doorId, 16, 0);
            SCMIoTSender.removeSingeReportDataCallback(doorId, 16, 1);
        }
        sInstance = null;
    }

    public void getDoorStatus(int doorId, IDataCallback2<DoorStatusInfo> callback) {
        LogUtils.d(TAG, "getDoorStatus: " + doorId);
        this.mGetDoorStatusCallbacks.put(Integer.valueOf(doorId & 255), callback);
        SCMRequest request = new SCMRequest();
        request.setDev(doorId);
        request.setTopic(16);
        request.setType(0);
        request.setCmd(0);
        request.setSerialDirect(isSerialDirect());
        request.setUSBDirect(isUsbDirect());
        request.setTrans(isTrans());
        SCMIoTSender.sendRequest(request, this.mGetDoorStatus);
    }

    public void getAllDoorStatusUseCache() {
        LogUtils.d(TAG, "getAllDoorStatusUseCache : " + GsonUtil.bean2String(this.mCacheDoorStatus.values()));
        boolean hasDefault = false;
        for (DoorStatusInfo info : this.mCacheDoorStatus.values()) {
            if (info.getStatus() == 17) {
                hasDefault = true;
                getDoorStatus(info.getDoorId(), (IDataCallback2<DoorStatusInfo>) null);
            }
        }
        if (!hasDefault) {
            notifySubscribeSuccess(TopicName.DOOR_SWITCH_STATUS, this.mCacheDoorStatus.values());
        }
    }

    public void getDoorStatus(final IDataCallback2<DoorStatusInfo> callback, final int... doorIds) {
        LogUtils.d(TAG, "getDoorStatus: " + GsonUtil.bean2String(doorIds));
        if (null != doorIds) {
            if (doorIds.length == 1) {
                getDoorStatus(doorIds[0], callback);
            } else {
                this.mExecutor.execute(new Runnable() { // from class: com.keenon.sdk.sensor.door.SensorDoor.1
                    @Override // java.lang.Runnable
                    public void run() {
                        int doorSize = doorIds.length;
                        for (int i = 0; i < doorSize; i++) {
                            SensorDoor.this.getDoorStatus(doorIds[i], callback);
                            SomeUtils.threadSleep(SensorDoor.this.mCmdExeInterval);
                        }
                    }
                });
            }
        }
    }

    public void setDoorSwitch(int doorId, boolean isOpen) {
        LogUtils.d(TAG, "setDoorSwitch: " + doorId + " => " + isOpen);
        SCMRequest request = new SCMRequest();
        request.setDev(doorId);
        request.setTopic(16);
        request.setType(2);
        request.setCmd(6);
        DoorSwitchParam prams = new DoorSwitchParam();
        prams.setSwitchFlag((byte) (isOpen ? 255 : 0));
        request.setParams(prams);
        request.setSerialDirect(isSerialDirect());
        request.setUSBDirect(isUsbDirect());
        request.setTrans(isTrans());
        SCMIoTSender.sendRequest(request, this.mDoorSwitchCallback);
        if (null != this.mCacheDoorStatus.get(Integer.valueOf(doorId)) && this.mCacheDoorStatus.get(Integer.valueOf(doorId)).getStatus() == 17) {
            DoorStatusInfo info = new DoorStatusInfo();
            info.setDoorId(doorId);
            info.setStatus(prams.getSwitchFlag());
            handleSwitchStatusReport(info);
        }
    }

    public void setDoorSwitch(final boolean isOpen, final int... doorIds) {
        LogUtils.d(TAG, "setDoorSwitch: " + GsonUtil.bean2String(doorIds) + " => " + isOpen);
        if (null != doorIds) {
            this.mExecutor.execute(new Runnable() { // from class: com.keenon.sdk.sensor.door.SensorDoor.2
                @Override // java.lang.Runnable
                public void run() {
                    int doorSize = doorIds.length;
                    for (int i = 0; i < doorSize; i++) {
                        SensorDoor.this.setDoorSwitch(doorIds[i], isOpen);
                        SomeUtils.threadSleep(SensorDoor.this.mCmdExeInterval);
                    }
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleSwitchStatusReport(DoorStatusInfo info) {
        LogUtils.d(TAG, "handleSwitchStatusReport: " + info);
        if (null == info || info.equals(this.mCacheDoorStatus.get(Integer.valueOf(info.getDoorId())))) {
            return;
        }
        this.mCacheDoorStatus.put(Integer.valueOf(info.getDoorId()), info);
        notifySubscribeSuccess(TopicName.DOOR_SWITCH_STATUS, this.mCacheDoorStatus.values());
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleSwitchFaultReport(ReportInfo info) {
        LogUtils.d(TAG, "handleSwitchStatusReport: " + info.toString());
        SensorFaultInfo.FaultEntry entry = new SensorFaultInfo.FaultEntry();
        entry.setDeviceId(info.getDev());
        entry.setFaultCode(info.getInfo());
        notifySubscribeSuccess(TopicName.DOOR_SWITCH_FAULT, entry);
    }
}
