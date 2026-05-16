package com.keenon.sdk.sensor.lidar.calibration;

import com.keenon.sdk.scmIot.protopack.annotation.BeanFieldAno;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/sensor/lidar/calibration/PlateLidarParam.class */
public class PlateLidarParam {

    @BeanFieldAno(order = 1)
    private byte code;

    public byte getCode() {
        return this.code;
    }

    public void setCode(byte code) {
        this.code = code;
    }
}
