package com.keenon.sdk.sensor.stm32;

import com.keenon.common.utils.GsonUtil;
import com.keenon.common.utils.LogUtils;
import com.keenon.sdk.constant.TopicName;
import com.keenon.sdk.external.IDataCallback;
import com.keenon.sdk.external.PeanutSDK;
import com.keenon.sdk.hedera.model.ApiError;
import com.keenon.sdk.scmIot.SCMIoTSender;
import com.keenon.sdk.scmIot.bean.SCMRequest;
import com.keenon.sdk.sensor.common.Sensor;
import com.keenon.sdk.sensor.common.SensorEvent;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/sensor/stm32/SensorStm32.class */
public class SensorStm32 extends Sensor {
    private static final String TAG = "SensorStm32";
    private static SensorStm32 sInstance;
    private final IDataCallback mSwitchStateCallBack = new IDataCallback() { // from class: com.keenon.sdk.sensor.stm32.SensorStm32.1
        @Override // com.keenon.sdk.external.IDataCallback
        public void success(String result) {
            try {
                LogUtils.d(SensorStm32.TAG, "mSwitchStateCallBack :" + result);
                AntiMotorSwitch antiMotorSwitch = (AntiMotorSwitch) GsonUtil.gson2Bean(result, AntiMotorSwitch.class);
                SensorStm32.this.notifyEvent(SensorEvent.REPORT_STM32_ANTI_MOTOR_SWITCH_ACK, antiMotorSwitch);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override // com.keenon.sdk.external.IDataCallback
        public void error(ApiError error) {
        }
    };
    private final IDataCallback mBottomRwaCallBack = new IDataCallback() { // from class: com.keenon.sdk.sensor.stm32.SensorStm32.2
        @Override // com.keenon.sdk.external.IDataCallback
        public void success(String result) {
        }

        @Override // com.keenon.sdk.external.IDataCallback
        public void error(ApiError error) {
            SensorStm32.this.notifyEvent(SensorEvent.REPORT_STM32_ANTI_MOTOR_SWITCH_ERROR_ACK, error);
        }
    };

    public static SensorStm32 ins() {
        if (null == sInstance) {
            synchronized (SensorStm32.class) {
                if (null == sInstance) {
                    sInstance = new SensorStm32();
                }
            }
        }
        return sInstance;
    }

    private SensorStm32() {
        PeanutSDK.getInstance().subscribe(TopicName.BOTTOM_RAW, this.mBottomRwaCallBack);
        SCMIoTSender.addSingeReportDataCallback(this.mSwitchStateCallBack, 0, 31, 0);
        SCMIoTSender.addSCMThingDataTransferV2(0, 31, 0, new Stm32AntiMotorSwitchDataTransfer());
    }

    public void getAntiMotorSwitch() {
        SCMRequest request = new SCMRequest();
        request.setDev(0);
        request.setTopic(31);
        request.setType(0);
        request.setCmd(0);
        SCMIoTSender.sendRequest(request, this.mBottomRwaCallBack);
    }

    public void modifyAntiMotorSwitch(boolean isOpen) {
        SCMRequest request = new SCMRequest();
        request.setDev(0);
        request.setTopic(31);
        request.setType(0);
        AntiMotorSwitch param = new AntiMotorSwitch();
        param.setSwitchValue((byte) (isOpen ? 0 : 1));
        request.setParams(param);
        request.setCmd(1);
        SCMIoTSender.sendRequest(request, this.mBottomRwaCallBack);
    }

    @Override // com.keenon.sdk.sensor.common.Sensor
    public String name() {
        return TAG;
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
        SCMIoTSender.removeSingeReportDataCallback(0, 31, 0);
        SCMIoTSender.removeSCMThingDataTransferV2(0, 31, 0);
        sInstance = null;
    }
}
