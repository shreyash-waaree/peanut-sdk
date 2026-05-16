package com.keenon.sdk.scmIot.bean.request;

import com.keenon.sdk.scmIot.protopack.annotation.BeanFieldAno;
import java.util.Arrays;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/scmIot/bean/request/LEDMultiColorConfigV3.class */
public class LEDMultiColorConfigV3 {

    @BeanFieldAno(order = 1)
    private short head;

    @BeanFieldAno(order = 2)
    private short num;

    @BeanFieldAno(order = 3)
    private byte[] rgbs;

    public byte[] getRgbs() {
        return this.rgbs;
    }

    public void setRgbs(byte[] rgbs) {
        this.rgbs = rgbs;
    }

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

    public String toString() {
        return "LEDMultiColorConfigV3{head=" + ((int) this.head) + ", num=" + ((int) this.num) + ", rgbs=" + Arrays.toString(this.rgbs) + '}';
    }
}
