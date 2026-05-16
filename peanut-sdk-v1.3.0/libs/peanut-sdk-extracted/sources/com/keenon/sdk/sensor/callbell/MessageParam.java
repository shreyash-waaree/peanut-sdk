package com.keenon.sdk.sensor.callbell;

import java.util.Arrays;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/sensor/callbell/MessageParam.class */
public class MessageParam {
    private byte[] sn;
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

    public String toString() {
        return "MessageParam{sn=" + Arrays.toString(this.sn) + ", msgId=" + this.msgId + '}';
    }
}
