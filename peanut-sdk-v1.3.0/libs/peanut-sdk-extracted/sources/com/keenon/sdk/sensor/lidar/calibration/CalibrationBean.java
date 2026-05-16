package com.keenon.sdk.sensor.lidar.calibration;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/sensor/lidar/calibration/CalibrationBean.class */
public class CalibrationBean {
    private int leftStatus;
    private int rightStatus;
    private int LidarId;

    public int getLeftStatus() {
        return this.leftStatus;
    }

    public void setLeftStatus(int leftStatus) {
        this.leftStatus = leftStatus;
    }

    public int getRightStatus() {
        return this.rightStatus;
    }

    public void setRightStatus(int rightStatus) {
        this.rightStatus = rightStatus;
    }

    public int getLidarId() {
        return this.LidarId;
    }

    public void setLidarId(int lidarId) {
        this.LidarId = lidarId;
    }
}
