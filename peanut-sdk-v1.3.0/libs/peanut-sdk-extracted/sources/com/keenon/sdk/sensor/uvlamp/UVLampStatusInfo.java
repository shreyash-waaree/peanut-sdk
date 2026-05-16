package com.keenon.sdk.sensor.uvlamp;

import java.util.List;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/sensor/uvlamp/UVLampStatusInfo.class */
public class UVLampStatusInfo {
    private int lamp1 = UVLampStatus.NO_FAULT.code();
    private int lamp2 = UVLampStatus.NO_FAULT.code();
    private int lamp3 = UVLampStatus.NO_FAULT.code();
    private int lamp4 = UVLampStatus.NO_FAULT.code();
    private int lamp5 = UVLampStatus.NO_FAULT.code();
    private List<Integer> devStatus;

    public void setDevStatus(List<Integer> status) {
        this.devStatus = status;
        this.lamp1 = getStatus(0);
        this.lamp2 = getStatus(1);
        this.lamp3 = getStatus(2);
        this.lamp4 = getStatus(3);
        this.lamp5 = getStatus(4);
    }

    public int getLamp1() {
        return this.lamp1;
    }

    public void setLamp1(int lamp1) {
        this.lamp1 = lamp1;
    }

    public int getLamp2() {
        return this.lamp2;
    }

    public void setLamp2(int lamp2) {
        this.lamp2 = lamp2;
    }

    public int getLamp3() {
        return this.lamp3;
    }

    public void setLamp3(int lamp3) {
        this.lamp3 = lamp3;
    }

    public int getLamp4() {
        return this.lamp4;
    }

    public void setLamp4(int lamp4) {
        this.lamp4 = lamp4;
    }

    public int getLamp5() {
        return this.lamp5;
    }

    public void setLamp5(int lamp5) {
        this.lamp5 = lamp5;
    }

    private int getStatus(int position) {
        if (null != this.devStatus && this.devStatus.size() > position) {
            return this.devStatus.get(position).intValue();
        }
        return UVLampStatus.NO_FAULT.code();
    }
}
