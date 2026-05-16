package com.keenon.sdk.sensor.lidar.calibration;

import java.util.Arrays;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/sensor/lidar/calibration/LidarPointBean.class */
public class LidarPointBean {
    private int LidarId;
    private int type;
    private float[] pointX;
    private float[] pointY;

    public int getLidarId() {
        return this.LidarId;
    }

    public void setLidarId(int lidarId) {
        this.LidarId = lidarId;
    }

    public int getType() {
        return this.type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public float[] getPointX() {
        return this.pointX;
    }

    public void setPointX(float[] pointX) {
        this.pointX = pointX;
    }

    public float[] getPointY() {
        return this.pointY;
    }

    public void setPointY(float[] pointY) {
        this.pointY = pointY;
    }

    public String toString() {
        return "LidarPointBean{LidarId=" + this.LidarId + ", type=" + this.type + ", pointX=" + Arrays.toString(this.pointX) + ", pointY=" + Arrays.toString(this.pointY) + '}';
    }
}
