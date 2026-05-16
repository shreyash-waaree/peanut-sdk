package com.keenon.sdk.sensor.callbell;

import com.keenon.sdk.scmIot.protopack.annotation.BeanFieldAno;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/sensor/callbell/DeviceListParam.class */
public class DeviceListParam {

    @BeanFieldAno(order = 1)
    private byte[] sn = {-1, -1, -1, -1, -1, -1};

    public byte[] getSn() {
        return this.sn;
    }

    public void setSn(byte[] sn) {
        this.sn = sn;
    }
}
