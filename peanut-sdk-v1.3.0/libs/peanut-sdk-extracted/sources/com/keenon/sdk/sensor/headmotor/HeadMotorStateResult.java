package com.keenon.sdk.sensor.headmotor;

import com.keenon.sdk.scmIot.protopack.annotation.BeanFieldAno;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/sensor/headmotor/HeadMotorStateResult.class */
public class HeadMotorStateResult {

    @BeanFieldAno(order = 1)
    private byte status;

    @BeanFieldAno(order = 2)
    private byte angleX;

    @BeanFieldAno(order = 3)
    private byte angleY;

    public byte getStatus() {
        return this.status;
    }

    public void setStatus(byte status) {
        this.status = status;
    }

    public byte getAngleX() {
        return this.angleX;
    }

    public void setAngleX(byte angleX) {
        this.angleX = angleX;
    }

    public byte getAngleY() {
        return this.angleY;
    }

    public void setAngleY(byte angleY) {
        this.angleY = angleY;
    }

    public String toString() {
        return "HeadMotorStateResult{status=" + ((int) this.status) + ", angleX=" + ((int) this.angleX) + ", angleY=" + ((int) this.angleY) + '}';
    }
}
