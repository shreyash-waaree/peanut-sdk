package com.keenon.sdk.sensor.stm32;

import com.keenon.sdk.scmIot.protopack.annotation.BeanFieldAno;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/sensor/stm32/AntiMotorSwitch.class */
public class AntiMotorSwitch {

    @BeanFieldAno(order = 1)
    private byte switchValue;

    public byte getSwitchValue() {
        return this.switchValue;
    }

    public void setSwitchValue(byte order) {
        this.switchValue = order;
    }

    public String toString() {
        return "AntiMotorSwitch{switchValue=" + ((int) this.switchValue) + '}';
    }
}
