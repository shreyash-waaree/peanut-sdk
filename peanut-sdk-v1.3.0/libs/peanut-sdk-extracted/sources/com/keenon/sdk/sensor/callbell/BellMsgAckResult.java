package com.keenon.sdk.sensor.callbell;

import com.keenon.sdk.scmIot.protopack.annotation.BeanFieldAno;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/sensor/callbell/BellMsgAckResult.class */
public class BellMsgAckResult {

    @BeanFieldAno(order = 1)
    private byte[] sn;

    @BeanFieldAno(order = 2)
    private int msgId;

    public byte[] getSn() {
        return this.sn;
    }

    public void setSn(byte[] sn) {
        this.sn = sn;
    }

    public int getMsgId() {
        return this.msgId;
    }

    public void setMsgId(int msgId) {
        this.msgId = msgId;
    }
}
