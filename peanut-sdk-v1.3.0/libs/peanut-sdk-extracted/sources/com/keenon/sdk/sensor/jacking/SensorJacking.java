package com.keenon.sdk.sensor.jacking;

import com.keenon.common.utils.LogUtils;
import com.keenon.sdk.constant.TopicName;
import com.keenon.sdk.external.IDataCallback;
import com.keenon.sdk.external.PeanutSDK;
import com.keenon.sdk.hedera.model.ApiError;
import com.keenon.sdk.scmIot.SCMIoTSender;
import com.keenon.sdk.scmIot.bean.SCMRequest;
import com.keenon.sdk.sensor.common.Sensor;
import com.keenon.sdk.sensor.common.SensorEvent;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/sensor/jacking/SensorJacking.class */
public final class SensorJacking extends Sensor {
    private static final String TAG = "SensorJacking";
    private static volatile SensorJacking sInstance;
    private final IDataCallback mEventCallback = new IDataCallback() { // from class: com.keenon.sdk.sensor.jacking.SensorJacking.2
        @Override // com.keenon.sdk.external.IDataCallback
        public void success(String result) {
            SensorJacking.this.notifyEvent(SensorEvent.REPORT_JACKING_STATE, result);
        }

        @Override // com.keenon.sdk.external.IDataCallback
        public void error(ApiError error) {
            SensorJacking.this.notifyEvent(SensorEvent.REPORT_JACKING_STATE, error);
        }
    };
    private final IDataCallback mBottomRwaCallBack = new IDataCallback() { // from class: com.keenon.sdk.sensor.jacking.SensorJacking.3
        @Override // com.keenon.sdk.external.IDataCallback
        public void success(String result) {
        }

        @Override // com.keenon.sdk.external.IDataCallback
        public void error(ApiError error) {
        }
    };
    private final IDataCallback controlJackingCallBack = new IDataCallback() { // from class: com.keenon.sdk.sensor.jacking.SensorJacking.4
        @Override // com.keenon.sdk.external.IDataCallback
        public void success(String result) {
            LogUtils.d(SensorJacking.TAG, "controlJacking :" + result);
            SensorJacking.this.notifyEvent(SensorEvent.CONTROL_JACKING_ACK, result);
        }

        @Override // com.keenon.sdk.external.IDataCallback
        public void error(ApiError error) {
            LogUtils.e(SensorJacking.TAG, "controlJacking :" + error.toString());
            SensorJacking.this.notifyEvent(SensorEvent.CONTROL_JACKING_ACK, error);
        }
    };

    public static SensorJacking getInstance() {
        if (sInstance == null) {
            synchronized (SensorJacking.class) {
                if (sInstance == null) {
                    sInstance = new SensorJacking();
                }
            }
        }
        return sInstance;
    }

    private SensorJacking() {
        SCMIoTSender.addSingeReportDataCallback(this.mEventCallback, 57, 25, 1);
        PeanutSDK.getInstance().subscribe(TopicName.BOTTOM_RAW, this.mBottomRwaCallBack);
        SCMIoTSender.addSCMThingDataTransfer(25, new JackingDataTransfer());
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
        SCMIoTSender.removeSingeReportDataCallback(57, 25, 1);
        PeanutSDK.getInstance().unSubscribe(TopicName.BOTTOM_RAW, this.mBottomRwaCallBack);
        SCMIoTSender.removeSCMThingDataTransfer(25);
        sInstance = null;
    }

    public void getJackingState() {
        SCMRequest request = new SCMRequest();
        request.setDev(57);
        request.setTopic(25);
        request.setType(0);
        request.setCmd(0);
        request.setSerialDirect(isSerialDirect());
        request.setUSBDirect(isUsbDirect());
        request.setTrans(isTrans());
        SCMIoTSender.sendRequest(request, new IDataCallback() { // from class: com.keenon.sdk.sensor.jacking.SensorJacking.1
            @Override // com.keenon.sdk.external.IDataCallback
            public void success(String result) {
                LogUtils.d(SensorJacking.TAG, "getJackingState :" + result);
                SensorJacking.this.notifyEvent(SensorEvent.GET_JACKING_STATE_ACK, result);
            }

            @Override // com.keenon.sdk.external.IDataCallback
            public void error(ApiError error) {
                LogUtils.d(SensorJacking.TAG, "getJackingState :" + error.toString());
                SensorJacking.this.notifyEvent(SensorEvent.GET_JACKING_STATE_ACK, error);
            }
        });
    }

    public void onControlUpRise() {
        SCMRequest request = new SCMRequest();
        request.setDev(57);
        request.setTopic(25);
        request.setType(2);
        request.setCmd(6);
        JackingParam prams = new JackingParam();
        prams.setStatus((byte) -1);
        request.setParams(prams);
        request.setSerialDirect(isSerialDirect());
        request.setUSBDirect(isUsbDirect());
        request.setTrans(isTrans());
        SCMIoTSender.sendRequest(request, this.controlJackingCallBack);
    }

    public void onControlDrop() {
        SCMRequest request = new SCMRequest();
        request.setDev(57);
        request.setTopic(25);
        request.setType(2);
        request.setCmd(6);
        JackingParam prams = new JackingParam();
        prams.setStatus((byte) 0);
        request.setParams(prams);
        request.setSerialDirect(isSerialDirect());
        request.setUSBDirect(isUsbDirect());
        request.setTrans(isTrans());
        SCMIoTSender.sendRequest(request, this.controlJackingCallBack);
    }
}
