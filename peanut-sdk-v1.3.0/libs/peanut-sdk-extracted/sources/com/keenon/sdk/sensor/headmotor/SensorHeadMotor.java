package com.keenon.sdk.sensor.headmotor;

import com.keenon.common.utils.GsonUtil;
import com.keenon.common.utils.LogUtils;
import com.keenon.sdk.external.IDataCallback;
import com.keenon.sdk.hedera.model.ApiError;
import com.keenon.sdk.scmIot.SCMIoTSender;
import com.keenon.sdk.scmIot.bean.SCMRequest;
import com.keenon.sdk.scmIot.protopack.util.Ushort;
import com.keenon.sdk.sensor.common.Sensor;
import com.keenon.sdk.sensor.common.SensorEvent;
import com.keenon.sdk.sensor.headmotor.HeadMotorInterface;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/sensor/headmotor/SensorHeadMotor.class */
public final class SensorHeadMotor extends Sensor {
    private static final String TAG = "SensorHeadMotor";
    private static volatile SensorHeadMotor sInstance;
    private final IDataCallback eventCallback = new IDataCallback() { // from class: com.keenon.sdk.sensor.headmotor.SensorHeadMotor.1
        @Override // com.keenon.sdk.external.IDataCallback
        public void success(String result) {
            LogUtils.d(SensorHeadMotor.TAG, "eventCallback :" + result);
            if (result != null) {
                HeadMotorStateResult keyEventResult = (HeadMotorStateResult) GsonUtil.gson2Bean(result, HeadMotorStateResult.class);
                HeadMotorBean bean = new HeadMotorBean();
                bean.setState((keyEventResult.getStatus() & 255) == 0 ? 0 : 1);
                bean.setAngleX(keyEventResult.getAngleX() & 255);
                bean.setAngleY(keyEventResult.getAngleY() & 255);
                SensorHeadMotor.this.notifyEvent(SensorEvent.REPORT_HEAD_MOTOR_STATE_EVENT, bean);
            }
        }

        @Override // com.keenon.sdk.external.IDataCallback
        public void error(ApiError error) {
            LogUtils.e(SensorHeadMotor.TAG, "eventCallback :" + error.toString());
            SensorHeadMotor.this.notifyEvent(SensorEvent.REPORT_HEAD_MOTOR_STATE_EVENT, error);
        }
    };
    private final IDataCallback faultEventCallback = new IDataCallback() { // from class: com.keenon.sdk.sensor.headmotor.SensorHeadMotor.2
        @Override // com.keenon.sdk.external.IDataCallback
        public void success(String result) {
            LogUtils.d(SensorHeadMotor.TAG, "faultEventCallback :" + result);
            SensorHeadMotor.this.notifyEvent(SensorEvent.REPORT_HEAD_MOTOR_FAULT_EVENT, result);
        }

        @Override // com.keenon.sdk.external.IDataCallback
        public void error(ApiError error) {
            LogUtils.e(SensorHeadMotor.TAG, "faultEventCallback :" + error.toString());
            SensorHeadMotor.this.notifyEvent(SensorEvent.REPORT_HEAD_MOTOR_FAULT_EVENT, error);
        }
    };
    private final IDataCallback controlHeadMotorCallBack = new IDataCallback() { // from class: com.keenon.sdk.sensor.headmotor.SensorHeadMotor.3
        @Override // com.keenon.sdk.external.IDataCallback
        public void success(String result) {
            LogUtils.d(SensorHeadMotor.TAG, "controlHeadMotor :" + result);
            if (result != null) {
                SensorHeadMotor.this.notifyEvent(SensorEvent.PLAY_CONTROL_HEAD_MOTOR_ACK, result);
            }
        }

        @Override // com.keenon.sdk.external.IDataCallback
        public void error(ApiError error) {
            LogUtils.e(SensorHeadMotor.TAG, "controlHeadMotor :" + error.toString());
            SensorHeadMotor.this.notifyEvent(SensorEvent.PLAY_CONTROL_HEAD_MOTOR_ACK, error);
        }
    };
    private final IDataCallback getHeadMotorStateCallBack = new IDataCallback() { // from class: com.keenon.sdk.sensor.headmotor.SensorHeadMotor.4
        @Override // com.keenon.sdk.external.IDataCallback
        public void success(String result) {
            LogUtils.d(SensorHeadMotor.TAG, "getHeadMotorState :" + result);
            if (result != null) {
                HeadMotorStateResult keyEventResult = (HeadMotorStateResult) GsonUtil.gson2Bean(result, HeadMotorStateResult.class);
                HeadMotorBean bean = new HeadMotorBean();
                bean.setState((keyEventResult.getStatus() & 255) == 0 ? 0 : 1);
                SensorHeadMotor.this.notifyEvent(SensorEvent.REPLAY_GET_HEAD_MOTOR_STATE_ACK, bean);
            }
        }

        @Override // com.keenon.sdk.external.IDataCallback
        public void error(ApiError error) {
            LogUtils.e(SensorHeadMotor.TAG, "getHeadMotorState :" + error.toString());
            SensorHeadMotor.this.notifyEvent(SensorEvent.REPLAY_GET_HEAD_MOTOR_STATE_ACK, error);
        }
    };

    public static SensorHeadMotor getInstance() {
        if (sInstance == null) {
            synchronized (SensorHeadMotor.class) {
                if (sInstance == null) {
                    sInstance = new SensorHeadMotor();
                }
            }
        }
        return sInstance;
    }

    private SensorHeadMotor() {
        SCMIoTSender.addSingeReportDataCallback(this.getHeadMotorStateCallBack, 66, 33, 0);
        SCMIoTSender.addSCMThingDataTransferV2(66, 33, 0, new HeadMotorStateTransfer());
        SCMIoTSender.addSCMThingDataTransferV2(66, 33, 1, new HeadMotorStateTransfer());
        SCMIoTSender.addSingeReportDataCallback(this.controlHeadMotorCallBack, 66, 33, 2);
        SCMIoTSender.addSingeReportDataCallback(this.eventCallback, 66, 33, 1);
        SCMIoTSender.addSingeReportDataCallback(this.faultEventCallback, 66, 1, 1);
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
        SCMIoTSender.removeSingeReportDataCallback(66, 33, 0);
        SCMIoTSender.removeSingeReportDataCallback(66, 33, 1);
        SCMIoTSender.removeSingeReportDataCallback(66, 1, 1);
        SCMIoTSender.removeSCMThingDataTransferV2(66, 33, 0);
        sInstance = null;
    }

    public void getHeadMotorState() {
        SCMRequest request = new SCMRequest();
        request.setDev(66);
        request.setTopic(33);
        request.setType(0);
        request.setCmd(0);
        request.setSerialDirect(isSerialDirect());
        request.setUSBDirect(isUsbDirect());
        request.setTrans(isTrans());
        SCMIoTSender.sendRequest(request, this.getHeadMotorStateCallBack);
    }

    public void onControlHeadMotorPlay(HeadMotorInterface.MotorAction motorAction) {
        onControlHeadMotorPlay(motorAction, (short) 0);
    }

    public void onControlHeadMotorPlay(HeadMotorInterface.MotorAction motorAction, short angle) {
        SCMRequest request = new SCMRequest();
        request.setDev(66);
        request.setTopic(33);
        request.setType(2);
        request.setCmd(6);
        PlayHeadMotorParam prams = new PlayHeadMotorParam();
        prams.setAction(motorAction.getCode());
        prams.setAngle(new Ushort(angle));
        request.setParams(prams);
        request.setSerialDirect(isSerialDirect());
        request.setUSBDirect(isUsbDirect());
        request.setTrans(isTrans());
        SCMIoTSender.sendRequest(request, this.controlHeadMotorCallBack);
    }

    public void onControlHeadMotorPlay(HeadMotorInterface.MotorAction motorAction, short angleX, short angleY) {
        SCMRequest request = new SCMRequest();
        request.setDev(66);
        request.setTopic(33);
        request.setType(2);
        request.setCmd(6);
        PlayHeadMotorParam2 prams = new PlayHeadMotorParam2();
        prams.setAction(motorAction.getCode());
        prams.setAngleX(new Ushort(angleX));
        prams.setAngleY(new Ushort(angleY));
        request.setParams(prams);
        request.setSerialDirect(isSerialDirect());
        request.setUSBDirect(isUsbDirect());
        request.setTrans(isTrans());
        SCMIoTSender.sendRequest(request, this.controlHeadMotorCallBack);
    }
}
