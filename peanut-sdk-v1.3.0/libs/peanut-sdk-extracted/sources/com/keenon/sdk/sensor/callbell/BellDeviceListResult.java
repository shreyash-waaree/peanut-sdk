package com.keenon.sdk.sensor.callbell;

import com.keenon.sdk.scmIot.protopack.annotation.BeanFieldAno;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/sensor/callbell/BellDeviceListResult.class */
public class BellDeviceListResult {

    @BeanFieldAno(order = 1)
    private byte[] dataValue;

    public byte[] getDataValue() {
        return this.dataValue;
    }

    public void setDataValue(byte[] dataValue) {
        this.dataValue = dataValue;
    }
}
