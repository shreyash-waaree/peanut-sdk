package com.keenon.sdk.sensor.lidar.calibration;

import com.keenon.sdk.scmIot.protopack.annotation.BeanFieldAno;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/sensor/lidar/calibration/CalibrationResultBean.class */
public class CalibrationResultBean {

    @BeanFieldAno(order = 1)
    private byte leftStatus;

    @BeanFieldAno(order = 2)
    private byte rightStatus;
    private int LidarId;

    public byte getLeftStatus() {
        return this.leftStatus;
    }

    public void setLeftStatus(byte leftStatus) {
        this.leftStatus = leftStatus;
    }

    public byte getRightStatus() {
        return this.rightStatus;
    }

    public void setRightStatus(byte rightStatus) {
        this.rightStatus = rightStatus;
    }

    public int getLidarId() {
        return this.LidarId;
    }

    public void setLidarId(int lidarId) {
        this.LidarId = lidarId;
    }
}
