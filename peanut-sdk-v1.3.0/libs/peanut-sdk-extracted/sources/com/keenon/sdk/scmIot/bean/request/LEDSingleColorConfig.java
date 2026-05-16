package com.keenon.sdk.scmIot.bean.request;

import com.keenon.sdk.scmIot.protopack.annotation.BeanFieldAno;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/scmIot/bean/request/LEDSingleColorConfig.class */
public class LEDSingleColorConfig {

    @BeanFieldAno(order = 1)
    private short onTime;

    @BeanFieldAno(order = 2)
    private short offTime;

    @BeanFieldAno(order = 3)
    private byte repeat;

    public byte getRepeat() {
        return this.repeat;
    }

    public void setRepeat(byte repeat) {
        this.repeat = repeat;
    }

    public short getOnTime() {
        return this.onTime;
    }

    public void setOnTime(short onTime) {
        this.onTime = onTime;
    }

    public int getOffTime() {
        return this.offTime;
    }

    public void setOffTime(short offTime) {
        this.offTime = offTime;
    }
}
