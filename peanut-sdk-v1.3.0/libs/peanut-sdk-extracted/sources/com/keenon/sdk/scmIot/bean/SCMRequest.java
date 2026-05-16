package com.keenon.sdk.scmIot.bean;

import com.keenon.common.constant.PeanutConstants;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/scmIot/bean/SCMRequest.class */
public class SCMRequest {
    int dev;
    int topic;
    int type;
    int cmd;
    Object params;
    int channel;
    boolean isCon;
    boolean isSerialDirect;
    String serialProtocolVer;
    boolean isUSBDirect;
    boolean isTrans;

    public SCMRequest() {
        setChannel(0);
        setCon(true);
        setSerialDirect(false);
    }

    public SCMRequest(int dev, int topic, int type, int cmd, Object params, int channel, boolean isCon, boolean isDirect, boolean isUSBDirect, @PeanutConstants.SerialProtocolVer String serialProtocolVer) {
        this.dev = dev;
        this.topic = topic;
        this.type = type;
        this.cmd = cmd;
        this.params = params;
        this.channel = channel;
        this.isCon = isCon;
        this.isSerialDirect = isDirect;
        this.serialProtocolVer = serialProtocolVer;
        this.isUSBDirect = isUSBDirect;
    }

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

    public int getType() {
        return this.type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public int getCmd() {
        return this.cmd;
    }

    public void setCmd(int cmd) {
        this.cmd = cmd;
    }

    public Object getParams() {
        return this.params;
    }

    public void setParams(Object params) {
        this.params = params;
    }

    public int getChannel() {
        return this.channel;
    }

    public void setChannel(int channel) {
        this.channel = channel;
    }

    public boolean isCon() {
        return this.isCon;
    }

    public void setCon(boolean con) {
        this.isCon = con;
    }

    public boolean isSerialDirect() {
        return this.isSerialDirect;
    }

    public void setSerialDirect(boolean serialDirect) {
        setSerialDirect(serialDirect, "v2.0");
    }

    public void setSerialDirect(boolean serialDirect, @PeanutConstants.SerialProtocolVer String protocolVer) {
        this.isSerialDirect = serialDirect;
    }

    public String getSerialProtocolVer() {
        return this.serialProtocolVer;
    }

    public boolean isTrans() {
        return this.isTrans;
    }

    public void setTrans(boolean trans) {
        this.isTrans = trans;
    }

    public boolean isUSBDirect() {
        return this.isUSBDirect;
    }

    public void setUSBDirect(boolean USBDirect) {
        this.isUSBDirect = USBDirect;
    }

    public String toString() {
        return "SCMRequest{dev=" + this.dev + ", topic=" + this.topic + ", type=" + this.type + ", cmd=" + this.cmd + ", params=" + this.params + ", channel=" + this.channel + ", isCon=" + this.isCon + ", isSerialDirect=" + this.isSerialDirect + ", serialProtocolVer=" + this.serialProtocolVer + ", isUsbDirect=" + this.isUSBDirect + '}';
    }
}
