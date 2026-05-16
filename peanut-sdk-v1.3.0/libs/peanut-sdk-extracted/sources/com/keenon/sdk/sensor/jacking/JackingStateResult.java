package com.keenon.sdk.sensor.jacking;

import com.keenon.sdk.scmIot.protopack.annotation.BeanFieldAno;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/sensor/jacking/JackingStateResult.class */
public class JackingStateResult {

    @BeanFieldAno(order = 1)
    private byte status = -2;

    public byte getStatus() {
        return this.status;
    }

    public void setStatus(byte status) {
        this.status = status;
    }

    public String toString() {
        return "JackingParam{status=" + ((int) this.status) + '}';
    }
}
