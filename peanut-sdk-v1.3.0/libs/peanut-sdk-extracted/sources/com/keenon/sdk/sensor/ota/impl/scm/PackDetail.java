package com.keenon.sdk.sensor.ota.impl.scm;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/sensor/ota/impl/scm/PackDetail.class */
public class PackDetail {
    private int fwSize;
    private String fwMd5;
    private String fwVersion;
    private String fwName;
    private byte[] appData;
    private byte[] packDetail;

    public byte[] getPackDetail() {
        return this.packDetail;
    }

    public void setPackDetail(byte[] packDetail) {
        this.packDetail = packDetail;
    }

    public byte[] getAppData() {
        return this.appData;
    }

    public void setAppData(byte[] appData) {
        this.appData = appData;
    }

    public int getFwSize() {
        return this.fwSize;
    }

    public void setFwSize(int fwSize) {
        this.fwSize = fwSize;
    }

    public String getFwMd5() {
        return this.fwMd5;
    }

    public void setFwMd5(String fwMd5) {
        this.fwMd5 = fwMd5;
    }

    public String getFwVersion() {
        return this.fwVersion;
    }

    public void setFwVersion(String fwVersion) {
        this.fwVersion = fwVersion;
    }

    public String getFwName() {
        return this.fwName;
    }

    public void setFwName(String fwName) {
        this.fwName = fwName;
    }

    public String toString() {
        return "PackInfo{fwSize=" + this.fwSize + ", fwMd5='" + this.fwMd5 + "', fwVersion='" + this.fwVersion + "', fwName='" + this.fwName + "'}";
    }
}
