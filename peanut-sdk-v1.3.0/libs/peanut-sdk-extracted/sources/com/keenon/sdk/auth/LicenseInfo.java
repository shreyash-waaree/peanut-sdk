package com.keenon.sdk.auth;

import com.google.gson.annotations.SerializedName;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/auth/LicenseInfo.class */
public class LicenseInfo {

    @SerializedName("app")
    private String appId;

    @SerializedName("name")
    private String packageName;

    @SerializedName("sn")
    private String robotSN;

    @SerializedName("expire")
    private long expireTime;

    public LicenseInfo(String appId, String packageName, String robotSN, long expireTime) {
        this.appId = appId;
        this.packageName = packageName;
        this.robotSN = robotSN;
        this.expireTime = expireTime;
    }

    public String getAppId() {
        return this.appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public String getPackageName() {
        return this.packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getRobotSN() {
        return this.robotSN;
    }

    public void setRobotSN(String robotSN) {
        this.robotSN = robotSN;
    }

    public long getExpireTime() {
        return this.expireTime;
    }

    public void setExpireTime(long expireTime) {
        this.expireTime = expireTime;
    }
}
