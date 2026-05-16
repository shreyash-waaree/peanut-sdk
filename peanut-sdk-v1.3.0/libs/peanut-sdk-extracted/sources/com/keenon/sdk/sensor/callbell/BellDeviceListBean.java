package com.keenon.sdk.sensor.callbell;

import java.util.List;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/sensor/callbell/BellDeviceListBean.class */
public class BellDeviceListBean {
    private List<String> gatewaySnList;
    private List<String> relaySnList;
    private List<String> scheduleSnList;

    public List<String> getGatewaySnList() {
        return this.gatewaySnList;
    }

    public void setGatewaySnList(List<String> gatewaySnList) {
        this.gatewaySnList = gatewaySnList;
    }

    public List<String> getRelaySnList() {
        return this.relaySnList;
    }

    public void setRelaySnList(List<String> relaySnList) {
        this.relaySnList = relaySnList;
    }

    public List<String> getScheduleSnList() {
        return this.scheduleSnList;
    }

    public void setScheduleSnList(List<String> scheduleSnList) {
        this.scheduleSnList = scheduleSnList;
    }

    public String toString() {
        return "BellDeviceListBean{gatewaySnList=" + this.gatewaySnList + ", relaySnList=" + this.relaySnList + ", ScheduleSnList=" + this.scheduleSnList + '}';
    }
}
