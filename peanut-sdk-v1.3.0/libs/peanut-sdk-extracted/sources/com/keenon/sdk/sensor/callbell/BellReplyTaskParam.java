package com.keenon.sdk.sensor.callbell;

import com.keenon.sdk.scmIot.protopack.annotation.BeanFieldAno;
import java.util.Arrays;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/sensor/callbell/BellReplyTaskParam.class */
public class BellReplyTaskParam {

    @BeanFieldAno(order = 1)
    private byte[] sn;

    @BeanFieldAno(order = 2)
    private int taskID;

    @BeanFieldAno(order = 3)
    private int msgId;

    @BeanFieldAno(order = 4)
    private byte state;

    public byte[] getSn() {
        return this.sn;
    }

    public void setSn(byte[] sn) {
        this.sn = sn;
    }

    public int getTaskID() {
        return this.taskID;
    }

    public void setTaskID(int taskID) {
        this.taskID = taskID;
    }

    public int getMsgId() {
        return this.msgId;
    }

    public void setMsgId(int msgId) {
        this.msgId = msgId;
    }

    public byte getState() {
        return this.state;
    }

    public void setState(byte state) {
        this.state = state;
    }

    public String toString() {
        return "BellReplyTaskParam{sn=" + Arrays.toString(this.sn) + ", taskID=" + this.taskID + ", msgId=" + this.msgId + ", state=" + ((int) this.state) + '}';
    }
}
