package com.keenon.sdk.sensor.lidar.calibration;

import com.keenon.sdk.scmIot.protopack.annotation.BeanFieldAno;
import java.util.Arrays;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/sensor/lidar/calibration/LidarParamResultBean.class */
public class LidarParamResultBean {

    @BeanFieldAno(order = 1)
    private int[] paramOne;

    @BeanFieldAno(order = 2)
    private byte[] paramTow;

    @BeanFieldAno(order = 3)
    private short[] paramThree;
    private int LidarId;

    public int getLidarId() {
        return this.LidarId;
    }

    public void setLidarId(int lidarId) {
        this.LidarId = lidarId;
    }

    public int[] getParamOne() {
        return this.paramOne;
    }

    public void setParamOne(int[] paramOne) {
        this.paramOne = paramOne;
    }

    public byte[] getParamTow() {
        return this.paramTow;
    }

    public void setParamTow(byte[] paramTow) {
        this.paramTow = paramTow;
    }

    public short[] getParamThree() {
        return this.paramThree;
    }

    public void setParamThree(short[] paramThree) {
        this.paramThree = paramThree;
    }

    public String toString() {
        return "LidarParamResultBean{paramOne=" + Arrays.toString(this.paramOne) + ", paramTow=" + Arrays.toString(this.paramTow) + ", paramThree=" + Arrays.toString(this.paramThree) + ", LidarId=" + this.LidarId + '}';
    }
}
