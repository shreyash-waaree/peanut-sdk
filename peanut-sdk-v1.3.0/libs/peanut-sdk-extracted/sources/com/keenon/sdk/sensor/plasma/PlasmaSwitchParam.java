package com.keenon.sdk.sensor.plasma;

import com.keenon.sdk.scmIot.protopack.annotation.BeanFieldAno;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/sensor/plasma/PlasmaSwitchParam.class */
public class PlasmaSwitchParam {

    @BeanFieldAno(order = 1)
    private byte isOpen;

    public byte getIsOpen() {
        return this.isOpen;
    }

    public void setIsOpen(byte isOpen) {
        this.isOpen = isOpen;
    }
}
