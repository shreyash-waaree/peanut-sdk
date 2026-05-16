package com.keenon.sdk.scmIot.bean.response;

import com.keenon.sdk.scmIot.protopack.annotation.BeanFieldAno;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/scmIot/bean/response/ReportInfo.class */
public class ReportInfo {
    private int dev;
    private int topic;

    @BeanFieldAno(order = 1)
    private byte info;

    public int getDev() {
        return this.dev;
    }

    public void setDev(int dev) {
        this.dev = dev;
    }

    public int getTopic() {
        return this.topic;
    }

    public void setTopic(int topic) {
        this.topic = topic;
    }

    public int getInfo() {
        return this.info;
    }

    public byte getInfoByte() {
        return this.info;
    }

    public void setInfo(byte result) {
        this.info = result;
    }

    public String toString() {
        return "ReportFault{dev=" + this.dev + ", topic=" + this.topic + ", faultCode=" + ((int) this.info) + '}';
    }
}
