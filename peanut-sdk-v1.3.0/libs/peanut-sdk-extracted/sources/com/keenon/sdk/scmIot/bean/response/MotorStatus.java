package com.keenon.sdk.scmIot.bean.response;

import com.keenon.sdk.scmIot.protopack.annotation.BeanFieldAno;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/scmIot/bean/response/MotorStatus.class */
public class MotorStatus {

    @BeanFieldAno(order = 1)
    private byte status;

    public byte getStatus() {
        return this.status;
    }

    public void setStatus(byte status) {
        this.status = status;
    }
}
