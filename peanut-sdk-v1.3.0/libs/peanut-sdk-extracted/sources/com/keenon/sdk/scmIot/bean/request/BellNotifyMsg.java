package com.keenon.sdk.scmIot.bean.request;

import com.keenon.sdk.scmIot.protopack.annotation.BeanFieldAno;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/scmIot/bean/request/BellNotifyMsg.class */
public class BellNotifyMsg {

    @BeanFieldAno(order = 1)
    private byte[] macAddress;

    @BeanFieldAno(order = 2)
    private byte robotStatus;

    public BellNotifyMsg(byte[] macAddress, byte robotStatus) {
        this.macAddress = macAddress;
        this.robotStatus = robotStatus;
    }

    public BellNotifyMsg(byte[] macAddress) {
        this.macAddress = macAddress;
    }

    public BellNotifyMsg() {
    }

    public byte[] getMacAddress() {
        return this.macAddress;
    }

    public void setMacAddress(byte[] macAddress) {
        this.macAddress = macAddress;
    }

    public byte getRobotStatus() {
        return this.robotStatus;
    }

    public void setRobotStatus(byte robotStatus) {
        this.robotStatus = robotStatus;
    }

    public String toString() {
        return "BellMacAddress{macAddress='" + this.macAddress + "'}";
    }
}
