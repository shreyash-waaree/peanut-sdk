package com.keenon.sdk.sensor.battery;

import com.keenon.common.utils.GsonUtil;
import com.keenon.sdk.constant.TopicName;
import com.keenon.sdk.external.IDataCallback;
import com.keenon.sdk.external.PeanutSDK;
import com.keenon.sdk.hedera.model.ApiCallback;
import com.keenon.sdk.hedera.model.ApiError;
import com.keenon.sdk.scmIot.SCMIoTSender;
import com.keenon.sdk.scmIot.bean.SCMRequest;
import com.keenon.sdk.sensor.common.Sensor;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/sensor/battery/SensorBattery.class */
public class SensorBattery extends Sensor {
    private static SensorBattery sInstance;
    private ApiCallback<Integer> callback = null;
    private final IDataCallback mBottomRwaCallBack = new IDataCallback() { // from class: com.keenon.sdk.sensor.battery.SensorBattery.2
        @Override // com.keenon.sdk.external.IDataCallback
        public void success(String result) {
        }

        @Override // com.keenon.sdk.external.IDataCallback
        public void error(ApiError error) {
        }
    };
    private final IDataCallback mEventCallback = new IDataCallback() { // from class: com.keenon.sdk.sensor.battery.SensorBattery.3
        @Override // com.keenon.sdk.external.IDataCallback
        public void success(String result) {
            BatteryTransportResult lampResult = (BatteryTransportResult) GsonUtil.gson2Bean(result, BatteryTransportResult.class);
            if (null != SensorBattery.this.callback) {
                SensorBattery.this.callback.onSuccess(Integer.valueOf(lampResult.getErrorCode() & 255));
            }
        }

        @Override // com.keenon.sdk.external.IDataCallback
        public void error(ApiError error) {
        }
    };

    public static SensorBattery ins() {
        if (null == sInstance) {
            synchronized (SensorBattery.class) {
                if (null == sInstance) {
                    sInstance = new SensorBattery();
                }
            }
        }
        return sInstance;
    }

    private SensorBattery() {
        SCMIoTSender.addSingeReportDataCallback(this.mEventCallback, 56, 21, 2);
        SCMIoTSender.addSCMThingDataTransfer(21, new BatteryDataTransfer());
        PeanutSDK.getInstance().subscribe(TopicName.BOTTOM_RAW, this.mBottomRwaCallBack);
    }

    @Override // com.keenon.sdk.sensor.common.Sensor
    public String name() {
        return null;
    }

    @Override // com.keenon.sdk.sensor.common.Sensor
    public void mount() {
    }

    @Override // com.keenon.sdk.sensor.common.Sensor
    public void unMount() {
    }

    @Override // com.keenon.sdk.sensor.common.Sensor
    public void release() {
        PeanutSDK.getInstance().unSubscribe(TopicName.BOTTOM_RAW, this.mBottomRwaCallBack);
    }

    public void enterTransportModel(final ApiCallback<Integer> callback) {
        this.callback = callback;
        SCMRequest request = new SCMRequest();
        request.setDev(56);
        request.setTopic(21);
        request.setType(2);
        request.setCmd(6);
        BatteryTransportParam param = new BatteryTransportParam();
        param.setOrder((byte) 1);
        request.setParams(param);
        SCMIoTSender.sendRequest(request, new IDataCallback() { // from class: com.keenon.sdk.sensor.battery.SensorBattery.1
            @Override // com.keenon.sdk.external.IDataCallback
            public void success(String result) {
            }

            @Override // com.keenon.sdk.external.IDataCallback
            public void error(ApiError error) {
                if (null != callback) {
                    callback.onFail(error);
                }
            }
        });
    }
}
