package com.keenon.sdk.serial.base;

import java.util.Arrays;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/serial/base/SerialData.class */
public class SerialData {
    private Byte[] data;
    private Byte[] status;

    public SerialData() {
    }

    public SerialData(Byte[] bytes) {
        this.data = bytes;
    }

    public SerialData(Byte[] bytes, int size) {
        this.data = (Byte[]) Arrays.copyOfRange(bytes, 0, size);
    }

    public Byte[] getData() {
        return this.data;
    }

    public void setData(Byte[] data) {
        this.data = data;
    }

    public Byte[] getStatus() {
        return this.status;
    }

    public void setStatus(Byte[] status) {
        this.status = status;
    }
}
