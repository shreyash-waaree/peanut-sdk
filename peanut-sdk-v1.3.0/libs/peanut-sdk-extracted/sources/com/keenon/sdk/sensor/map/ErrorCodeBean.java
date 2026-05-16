package com.keenon.sdk.sensor.map;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/sensor/map/ErrorCodeBean.class */
public class ErrorCodeBean {
    private int error;

    public ErrorCodeBean(int error) {
        this.error = error;
    }

    public int getError() {
        return this.error;
    }

    public void setError(int error) {
        this.error = error;
    }

    public String toString() {
        return "ErrorCodeBean{error=" + this.error + '}';
    }
}
