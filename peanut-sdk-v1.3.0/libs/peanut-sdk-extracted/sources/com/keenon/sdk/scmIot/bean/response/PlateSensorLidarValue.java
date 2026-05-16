package com.keenon.sdk.scmIot.bean.response;

import com.keenon.sdk.scmIot.protopack.annotation.BeanFieldAno;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/scmIot/bean/response/PlateSensorLidarValue.class */
public class PlateSensorLidarValue {

    @BeanFieldAno(order = 1)
    private byte plate_1;

    @BeanFieldAno(order = 2)
    private byte plate_2;

    @BeanFieldAno(order = 3)
    private byte plate_3;

    @BeanFieldAno(order = 4)
    private byte plate_4;

    public int getPlate_1() {
        return this.plate_1;
    }

    public void setPlate_1(byte plate_1) {
        this.plate_1 = plate_1;
    }

    public byte getPlate_2() {
        return this.plate_2;
    }

    public void setPlate_2(byte plate_2) {
        this.plate_2 = plate_2;
    }

    public byte getPlate_3() {
        return this.plate_3;
    }

    public void setPlate_3(byte plate_3) {
        this.plate_3 = plate_3;
    }

    public byte getPlate_4() {
        return this.plate_4;
    }

    public void setPlate_4(byte plate_4) {
        this.plate_4 = plate_4;
    }

    public String toString() {
        return "PlateSensorLidarValue{plate_1=" + ((int) this.plate_1) + ", plate_2=" + ((int) this.plate_2) + ", plate_3=" + ((int) this.plate_3) + ", plate_4=" + ((int) this.plate_4) + '}';
    }
}
