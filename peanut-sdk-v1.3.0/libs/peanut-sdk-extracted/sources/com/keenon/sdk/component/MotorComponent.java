package com.keenon.sdk.component;

import com.keenon.common.constant.PeanutConstants;
import com.keenon.common.utils.LogUtils;
import com.keenon.sdk.api.DevicesMoveControlApi;
import com.keenon.sdk.api.MotorEnableApi;
import com.keenon.sdk.api.MotorEncoderApi;
import com.keenon.sdk.api.MotorGMaxApi;
import com.keenon.sdk.api.MotorHealthApi;
import com.keenon.sdk.api.MotorHrcApi;
import com.keenon.sdk.api.MotorManualApi;
import com.keenon.sdk.api.MotorSMaxApi;
import com.keenon.sdk.api.MotorSpeedApi;
import com.keenon.sdk.api.MotorStatusApi;
import com.keenon.sdk.component.Component;
import com.keenon.sdk.external.IDataCallback;
import com.keenon.sdk.scmIot.SCMIoTSender;
import com.keenon.sdk.scmIot.bean.SCMRequest;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/component/MotorComponent.class */
public class MotorComponent extends BaseComponent implements Component {
    public static final int FORWARD = 1;
    public static final int BACKWARD = 2;
    public static final int LEFT = 3;
    public static final int RIGHT = 4;

    @Override // com.keenon.sdk.component.BaseComponent, com.keenon.sdk.component.Component
    public String getComponentName() {
        return Component.RobotComponentName.COMPONENT_MOTOR.getComponentName();
    }

    public void getStatus(IDataCallback callBack) {
        new MotorStatusApi().send(callBack);
    }

    public void getEncoder(IDataCallback callBack) {
        new MotorEncoderApi().send(callBack);
    }

    public void getHealth(IDataCallback callBack) {
        new MotorHealthApi().send(callBack);
    }

    public void getSpeed(IDataCallback callBack) {
        new MotorSpeedApi().send(callBack);
    }

    public void enable(IDataCallback callBack, int enable) {
        new MotorEnableApi().send(callBack, enable);
    }

    public void hrc(IDataCallback callBack, boolean enable) {
        new MotorHrcApi().send(callBack, enable);
    }

    public void manual(IDataCallback callBack, int move) {
        new MotorManualApi().send(callBack, move);
    }

    public void forward(IDataCallback callBack) {
        new MotorManualApi().send(callBack, 1);
    }

    public void backward(IDataCallback callBack) {
        new MotorManualApi().send(callBack, 2);
    }

    public void turnLeft(IDataCallback callBack) {
        new MotorManualApi().send(callBack, 3);
    }

    public void turnRight(IDataCallback callBack) {
        new MotorManualApi().send(callBack, 4);
    }

    public void getMaxSpeed(IDataCallback callBack, int speed) {
        new MotorGMaxApi().send(callBack, speed);
    }

    public void setMaxSpeed(IDataCallback callBack, int speed) {
        new MotorSMaxApi().send(callBack, speed);
    }

    public void getState(IDataCallback callBack) {
        LogUtils.d(PeanutConstants.TAG_API, "getState");
        SCMRequest request = new SCMRequest();
        request.setDev(0);
        request.setTopic(6);
        request.setType(0);
        request.setCmd(0);
        SCMIoTSender.sendRequest(request, callBack);
    }

    public void moveControl(IDataCallback callBack, DevicesMoveControlApi.ParamBean paramBean) {
        new DevicesMoveControlApi().send(callBack, paramBean);
    }
}
