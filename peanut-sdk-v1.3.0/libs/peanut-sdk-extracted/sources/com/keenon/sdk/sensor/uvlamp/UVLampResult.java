package com.keenon.sdk.sensor.uvlamp;

import com.keenon.sdk.scmIot.protopack.annotation.BeanFieldAno;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/sensor/uvlamp/UVLampResult.class */
public class UVLampResult {

    @BeanFieldAno(order = 1)
    private byte result;

    public byte getResult() {
        return this.result;
    }

    public void setResult(byte result) {
        this.result = result;
    }

    public String toString() {
        return "SensorUVLampResult{result=" + ((int) this.result) + '}';
    }
}
