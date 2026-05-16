package com.keenon.sdk.sensor.callbell;

import com.keenon.sdk.scmIot.protopack.annotation.BeanFieldAno;
import java.util.Arrays;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/sensor/callbell/HeartBeatParam.class */
public class HeartBeatParam {

    @BeanFieldAno(order = 1)
    private byte[] sn;

    public byte[] getSn() {
        return this.sn;
    }

    public void setSn(byte[] sn) {
        this.sn = sn;
    }

    public String toString() {
        return "HeartBeatParam{sn=" + Arrays.toString(this.sn) + '}';
    }
}
