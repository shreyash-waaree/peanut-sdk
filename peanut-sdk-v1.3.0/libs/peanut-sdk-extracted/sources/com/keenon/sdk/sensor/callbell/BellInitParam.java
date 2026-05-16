package com.keenon.sdk.sensor.callbell;

import com.keenon.sdk.scmIot.protopack.annotation.BeanFieldAno;
import java.util.Arrays;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/sensor/callbell/BellInitParam.class */
public class BellInitParam {

    @BeanFieldAno(order = 1)
    private byte[] sn;

    @BeanFieldAno(order = 2)
    private int msgId = 0;

    @BeanFieldAno(order = 3)
    private byte msgType;

    @BeanFieldAno(order = 4)
    private short stayTime;

    @BeanFieldAno(order = 5)
    private byte keyEvent;

    @BeanFieldAno(order = 6)
    private byte robotState;

    @BeanFieldAno(order = 7)
    private byte enable;

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

    public byte getMsgType() {
        return this.msgType;
    }

    public void setMsgType(byte msgType) {
        this.msgType = msgType;
    }

    public short getStayTime() {
        return this.stayTime;
    }

    public void setStayTime(short stayTime) {
        this.stayTime = stayTime;
    }

    public byte getKeyEvent() {
        return this.keyEvent;
    }

    public void setKeyEvent(byte keyEvent) {
        this.keyEvent = keyEvent;
    }

    public byte getRobotState() {
        return this.robotState;
    }

    public void setRobotState(byte robotState) {
        this.robotState = robotState;
    }

    public byte getEnable() {
        return this.enable;
    }

    public void setEnable(byte enable) {
        this.enable = enable;
    }

    public String toString() {
        return "BellInitParam{sn=" + Arrays.toString(this.sn) + ", msgId=" + this.msgId + ", msgType=" + ((int) this.msgType) + ", stayTime=" + ((int) this.stayTime) + ", keyEvent=" + ((int) this.keyEvent) + ", robotState=" + ((int) this.robotState) + ", enable=" + ((int) this.enable) + '}';
    }
}
