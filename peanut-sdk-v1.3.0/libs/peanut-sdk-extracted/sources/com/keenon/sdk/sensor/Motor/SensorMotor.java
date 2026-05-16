package com.keenon.sdk.sensor.Motor;

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

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/sensor/Motor/SensorMotor.class */
public class SensorMotor extends Sensor {
    private static final String TAG = "SensorMotor";
    private static SensorMotor sInstance;
    private final IDataCallback mMotorStateCallBack = new IDataCallback() { // from class: com.keenon.sdk.sensor.Motor.SensorMotor.1
        @Override // com.keenon.sdk.external.IDataCallback
        public void success(String result) {
            try {
                LogUtils.d(SensorMotor.TAG, "mMotorStateCallBack :" + result);
                MotorStatus antiMotorSwitch = (MotorStatus) GsonUtil.gson2Bean(result, MotorStatus.class);
                SensorMotor.this.notifyEvent(SensorEvent.REPORT_STM32_MOTOR_STATE_ACK, antiMotorSwitch);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override // com.keenon.sdk.external.IDataCallback
        public void error(ApiError error) {
        }
    };
    private final IDataCallback mBottomRwaCallBack = new IDataCallback() { // from class: com.keenon.sdk.sensor.Motor.SensorMotor.2
        @Override // com.keenon.sdk.external.IDataCallback
        public void success(String result) {
        }

        @Override // com.keenon.sdk.external.IDataCallback
        public void error(ApiError error) {
            SensorMotor.this.notifyEvent(SensorEvent.REPORT_STM32_MOTOR_STATE_ERROR_ACK, error);
        }
    };

    public static SensorMotor ins() {
        if (null == sInstance) {
            synchronized (SensorMotor.class) {
                if (null == sInstance) {
                    sInstance = new SensorMotor();
                }
            }
        }
        return sInstance;
    }

    private SensorMotor() {
        PeanutSDK.getInstance().subscribe(TopicName.BOTTOM_RAW, this.mBottomRwaCallBack);
        SCMIoTSender.addSingeReportDataCallback(this.mMotorStateCallBack, 0, 6, 0);
        SCMIoTSender.addSingeReportDataCallback(this.mMotorStateCallBack, 0, 6, 1);
        SCMIoTSender.addSCMThingDataTransferV2(0, 6, 0, new MotorStatusDataTransfer());
        SCMIoTSender.addSCMThingDataTransferV2(0, 6, 1, new MotorStatusDataTransfer());
    }

    public void getMotorState() {
        SCMRequest request = new SCMRequest();
        request.setDev(0);
        request.setTopic(6);
        request.setType(0);
        request.setCmd(0);
        SCMIoTSender.sendRequest(request, this.mBottomRwaCallBack);
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
        PeanutSDK.getInstance().unSubscribe(TopicName.BOTTOM_RAW, this.mBottomRwaCallBack);
        SCMIoTSender.removeSingeReportDataCallback(0, 6, 0);
        SCMIoTSender.removeSingeReportDataCallback(0, 6, 1);
        SCMIoTSender.removeSCMThingDataTransferV2(0, 6, 0);
        SCMIoTSender.removeSCMThingDataTransferV2(0, 6, 1);
        sInstance = null;
    }
}
