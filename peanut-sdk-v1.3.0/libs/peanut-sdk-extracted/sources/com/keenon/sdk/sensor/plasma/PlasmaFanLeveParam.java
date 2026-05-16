package com.keenon.sdk.sensor.plasma;

import com.keenon.sdk.scmIot.protopack.annotation.BeanFieldAno;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/sensor/plasma/PlasmaFanLeveParam.class */
public class PlasmaFanLeveParam {

    @BeanFieldAno(order = 1)
    private byte leve;

    public byte getLeve() {
        return this.leve;
    }

    public void setLeve(byte leve) {
        this.leve = leve;
    }
}
