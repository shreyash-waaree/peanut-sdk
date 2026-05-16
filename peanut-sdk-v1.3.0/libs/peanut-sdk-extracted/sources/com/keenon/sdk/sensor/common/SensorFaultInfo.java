package com.keenon.sdk.sensor.common;

import java.util.List;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/sensor/common/SensorFaultInfo.class */
public class SensorFaultInfo {
    private List<FaultEntry> infos;

    public List<FaultEntry> getInfos() {
        return this.infos;
    }

    public void setInfos(List<FaultEntry> infos) {
        this.infos = infos;
    }

    /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/sensor/common/SensorFaultInfo$FaultEntry.class */
    public static class FaultEntry {
        private int deviceId;
        private int faultCode;

        public FaultEntry() {
        }

        public FaultEntry(int deviceId, int faultCode) {
            this.deviceId = deviceId;
            this.faultCode = faultCode;
        }

        public int getDeviceId() {
            return this.deviceId;
        }

        public void setDeviceId(int deviceId) {
            this.deviceId = deviceId;
        }

        public int getFaultCode() {
            return this.faultCode;
        }

        public void setFaultCode(int faultCode) {
            this.faultCode = faultCode;
        }
    }
}
