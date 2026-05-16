package com.keenon.sdk.sensor.callbell;

import com.keenon.sdk.scmIot.protopack.annotation.BeanFieldAno;
import java.util.Arrays;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/sensor/callbell/BellReceivedTaskResult.class */
public class BellReceivedTaskResult {

    @BeanFieldAno(order = 1)
    private byte[] sn;

    @BeanFieldAno(order = 2)
    private int taskId;

    @BeanFieldAno(order = 3)
    private int msgId;

    @BeanFieldAno(order = 4)
    private byte msgType;

    @BeanFieldAno(order = 5)
    private int bellId;

    @BeanFieldAno(order = 6)
    private int waitTime;

    @BeanFieldAno(order = 7)
    private int repeatTime;

    @BeanFieldAno(order = 8)
    private byte keyEvent;

    public byte[] getSn() {
        return this.sn;
    }

    public void setSn(byte[] sn) {
        this.sn = sn;
    }

    public int getTaskId() {
        return this.taskId;
    }

    public void setTaskId(int taskId) {
        this.taskId = taskId;
    }

    public int getMsgId() {
        return this.msgId;
    }

    public void setMsgId(int msgId) {
        this.msgId = msgId;
    }

    public byte getMsgType() {
        return this.msgType;
    }

    public void setMsgType(byte msgType) {
        this.msgType = msgType;
    }

    public int getBellId() {
        return this.bellId;
    }

    public void setBellId(int bellId) {
        this.bellId = bellId;
    }

    public int getWaitTime() {
        return this.waitTime;
    }

    public void setWaitTime(int waitTime) {
        this.waitTime = waitTime;
    }

    public int getRepeatTime() {
        return this.repeatTime;
    }

    public void setRepeatTime(int repeatTime) {
        this.repeatTime = repeatTime;
    }

    public byte getKeyEvent() {
        return this.keyEvent;
    }

    public void setKeyEvent(byte keyEvent) {
        this.keyEvent = keyEvent;
    }

    public String toString() {
        return "BellReceivedTaskResult{sn=" + Arrays.toString(this.sn) + ", taskId=" + this.taskId + ", msgId=" + this.msgId + ", msgType=" + ((int) this.msgType) + ", bellId=" + this.bellId + ", waitTime=" + this.waitTime + ", repeatTime=" + this.repeatTime + ", keyEvent=" + ((int) this.keyEvent) + '}';
    }
}
