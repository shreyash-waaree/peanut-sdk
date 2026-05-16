package com.keenon.sdk.sensor.Motor;

import com.keenon.sdk.scmIot.protopack.annotation.BeanFieldAno;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/sensor/Motor/MotorStatus.class */
public class MotorStatus {

    @BeanFieldAno(order = 1)
    private byte state;

    public byte getState() {
        return this.state;
    }

    public void setState(byte state) {
        this.state = state;
    }
}
