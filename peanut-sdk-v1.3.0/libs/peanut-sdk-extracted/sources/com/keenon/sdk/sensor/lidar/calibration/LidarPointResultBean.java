package com.keenon.sdk.sensor.lidar.calibration;

import com.keenon.sdk.scmIot.protopack.annotation.BeanFieldAno;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/sensor/lidar/calibration/LidarPointResultBean.class */
public class LidarPointResultBean {

    @BeanFieldAno(order = 1)
    private byte type;

    @BeanFieldAno(order = 2)
    private float[] angles;

    @BeanFieldAno(order = 3)
    private short[] distances;
    private int LidarId;

    public byte getType() {
        return this.type;
    }

    public void setType(byte type) {
        this.type = type;
    }

    public float[] getAngles() {
        return this.angles;
    }

    public void setAngles(float[] angles) {
        this.angles = angles;
    }

    public short[] getDistances() {
        return this.distances;
    }

    public void setDistances(short[] distances) {
        this.distances = distances;
    }

    public int getLidarId() {
        return this.LidarId;
    }

    public void setLidarId(int lidarId) {
        this.LidarId = lidarId;
    }
}
