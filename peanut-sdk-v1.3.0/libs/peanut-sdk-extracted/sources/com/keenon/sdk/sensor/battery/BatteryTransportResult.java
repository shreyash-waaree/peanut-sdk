package com.keenon.sdk.sensor.battery;

import com.keenon.sdk.scmIot.protopack.annotation.BeanFieldAno;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/sensor/battery/BatteryTransportResult.class */
public class BatteryTransportResult {

    @BeanFieldAno(order = 1)
    private byte order;

    @BeanFieldAno(order = 2)
    private byte errorCode;

    public byte getOrder() {
        return this.order;
    }

    public void setOrder(byte order) {
        this.order = order;
    }

    public byte getErrorCode() {
        return this.errorCode;
    }

    public void setErrorCode(byte errorCode) {
        this.errorCode = errorCode;
    }

    public String toString() {
        return "BatteryTransportResult{order=" + ((int) this.order) + ", errorCode=" + ((int) this.errorCode) + '}';
    }
}
