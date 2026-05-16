package com.keenon.sdk.sensor.plasma;

import com.keenon.sdk.scmIot.protopack.annotation.BeanFieldAno;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/sensor/plasma/PlasmaFanStatusInfoResult.class */
public class PlasmaFanStatusInfoResult {

    @BeanFieldAno(order = 1)
    private short speed;

    public short getSpeed() {
        return this.speed;
    }

    public void setSpeed(short speed) {
        this.speed = speed;
    }
}
