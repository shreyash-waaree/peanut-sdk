package com.keenon.sdk.sensor.battery;

import com.keenon.sdk.scmIot.protopack.annotation.BeanFieldAno;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/sensor/battery/BatteryTransportParam.class */
public class BatteryTransportParam {

    @BeanFieldAno(order = 1)
    private byte order;

    public byte getOrder() {
        return this.order;
    }

    public void setOrder(byte order) {
        this.order = order;
    }

    public String toString() {
        return "BatteryTransportParam{order=" + ((int) this.order) + '}';
    }
}
