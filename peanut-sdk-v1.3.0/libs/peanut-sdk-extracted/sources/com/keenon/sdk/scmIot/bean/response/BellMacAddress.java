package com.keenon.sdk.scmIot.bean.response;

import com.keenon.sdk.scmIot.protopack.annotation.BeanFieldAno;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/scmIot/bean/response/BellMacAddress.class */
public class BellMacAddress {

    @BeanFieldAno(order = 1)
    private byte[] macAddress;

    public byte[] getMacAddress() {
        return this.macAddress;
    }

    public void setMacAddress(byte[] macAddress) {
        this.macAddress = macAddress;
    }

    public BellMacAddress(byte[] macAddress) {
        this.macAddress = macAddress;
    }

    public BellMacAddress() {
    }

    public String toString() {
        return "BellMacAddress{macAddress='" + this.macAddress + "'}";
    }
}
