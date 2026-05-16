package com.keenon.sdk.scmIot.bean.request;

import com.keenon.sdk.scmIot.protopack.annotation.BeanFieldAno;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/scmIot/bean/request/LEDMultiColorConfig.class */
public class LEDMultiColorConfig {

    @BeanFieldAno(order = 1)
    private byte num;

    @BeanFieldAno(order = 2)
    private byte mode;

    @BeanFieldAno(order = 3)
    private int time;

    public short getNum() {
        return this.num;
    }

    public void setNum(byte num) {
        this.num = num;
    }

    public int getMode() {
        return this.mode;
    }

    public void setMode(byte mode) {
        this.mode = mode;
    }

    public int getTime() {
        return this.time;
    }

    public void setTime(int time) {
        this.time = time;
    }
}
