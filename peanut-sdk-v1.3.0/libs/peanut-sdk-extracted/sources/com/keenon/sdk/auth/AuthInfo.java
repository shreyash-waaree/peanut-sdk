package com.keenon.sdk.auth;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/auth/AuthInfo.class */
public class AuthInfo {
    private String appName;
    private String appId;
    private String appSecret;
    private String robotSN;
    private long usedTime;
    private long expireTime;
    private String activatedTime;
    private LicenseInfo license;

    public String getAppName() {
        return this.appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getAppId() {
        return this.appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public String getAppSecret() {
        return this.appSecret;
    }

    public void setAppSecret(String appSecret) {
        this.appSecret = appSecret;
    }

    public String getRobotSN() {
        return this.robotSN;
    }

    public void setRobotSN(String robotSN) {
        this.robotSN = robotSN;
    }

    public long getUsedTime() {
        return this.usedTime;
    }

    public void setUsedTime(long usedTime) {
        this.usedTime = usedTime;
    }

    public long getExpireTime() {
        return this.expireTime;
    }

    public void setExpireTime(long expireTime) {
        this.expireTime = expireTime;
    }

    public String getActivatedTime() {
        return this.activatedTime;
    }

    public void setActivatedTime(String activatedTime) {
        this.activatedTime = activatedTime;
    }

    public LicenseInfo getLicense() {
        return this.license;
    }

    public void setLicense(LicenseInfo license) {
        this.license = license;
    }

    public String toString() {
        return "AuthInfo{appName='" + this.appName + "', appId='" + this.appId + "', appSecret='" + this.appSecret + "', robotSN='" + this.robotSN + "', usedTime=" + this.usedTime + ", expireTime=" + this.expireTime + ", activatedTime='" + this.activatedTime + "', license=" + this.license + '}';
    }
}
