package com.keenon.sdk.sensor.uvlamp;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/sensor/uvlamp/UVLampStatus.class */
public enum UVLampStatus {
    NO_FAULT(0),
    FAULT_CURRENT_TOO_LOW(1),
    FAULT_CURRENT_TOO_BIG(2);

    int faultCode;

    UVLampStatus(int code) {
        this.faultCode = code;
    }

    public int code() {
        return this.faultCode;
    }
}
