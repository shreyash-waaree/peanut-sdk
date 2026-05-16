package com.keenon.sdk.scmIot.bean.request;

import com.keenon.sdk.scmIot.protopack.annotation.BeanFieldAno;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/scmIot/bean/request/LEDMultiColorConfigV2.class */
public class LEDMultiColorConfigV2 {

    @BeanFieldAno(order = 1)
    private short head;

    @BeanFieldAno(order = 2)
    private short num;

    @BeanFieldAno(order = 3)
    private short mode;

    @BeanFieldAno(order = 4)
    private short time;

    public short getHead() {
        return this.head;
    }

    public void setHead(short head) {
        this.head = head;
    }

    public short getNum() {
        return this.num;
    }

    public void setNum(short num) {
        this.num = num;
    }

    public short getMode() {
        return this.mode;
    }

    public void setMode(short mode) {
        this.mode = mode;
    }

    public short getTime() {
        return this.time;
    }

    public void setTime(short time) {
        this.time = time;
    }

    public String toString() {
        return "LEDMultiColorConfigV2{head=" + ((int) this.head) + ", num=" + ((int) this.num) + ", mode=" + ((int) this.mode) + ", time=" + ((int) this.time) + '}';
    }
}
