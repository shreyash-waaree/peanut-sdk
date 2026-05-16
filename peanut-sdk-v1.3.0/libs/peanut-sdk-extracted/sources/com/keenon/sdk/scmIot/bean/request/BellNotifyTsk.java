package com.keenon.sdk.scmIot.bean.request;

import com.keenon.sdk.scmIot.protopack.annotation.BeanFieldAno;
import java.util.Arrays;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/scmIot/bean/request/BellNotifyTsk.class */
public class BellNotifyTsk {

    @BeanFieldAno(order = 1)
    private byte[] macAddress;

    @BeanFieldAno(order = 2)
    private short taskId;

    public byte[] getMacAddress() {
        return this.macAddress;
    }

    public void setMacAddress(byte[] macAddress) {
        this.macAddress = macAddress;
    }

    public short getTaskId() {
        return this.taskId;
    }

    public void setTaskId(short taskId) {
        this.taskId = taskId;
    }

    public BellNotifyTsk(byte[] macAddress, short taskId) {
        this.macAddress = macAddress;
        this.taskId = taskId;
    }

    public String toString() {
        return "BellNotifyTsk{macAddress=" + Arrays.toString(this.macAddress) + ", taskId=" + ((int) this.taskId) + '}';
    }
}
