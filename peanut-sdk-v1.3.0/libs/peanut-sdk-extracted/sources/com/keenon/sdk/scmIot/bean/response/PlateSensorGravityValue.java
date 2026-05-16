package com.keenon.sdk.scmIot.bean.response;

import com.keenon.sdk.scmIot.protopack.annotation.BeanFieldAno;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/scmIot/bean/response/PlateSensorGravityValue.class */
public class PlateSensorGravityValue {

    @BeanFieldAno(order = 1)
    private int gravity_1;

    @BeanFieldAno(order = 2)
    private int gravity_2;

    @BeanFieldAno(order = 3)
    private int gravity_3;

    @BeanFieldAno(order = 4)
    private int gravity_4;

    public int getGravity_1() {
        return this.gravity_1;
    }

    public void setGravity_1(int gravity_1) {
        this.gravity_1 = gravity_1;
    }

    public int getGravity_2() {
        return this.gravity_2;
    }

    public void setGravity_2(int gravity_2) {
        this.gravity_2 = gravity_2;
    }

    public int getGravity_3() {
        return this.gravity_3;
    }

    public void setGravity_3(int gravity_3) {
        this.gravity_3 = gravity_3;
    }

    public int getGravity_4() {
        return this.gravity_4;
    }

    public void setGravity_4(int gravity_4) {
        this.gravity_4 = gravity_4;
    }

    public String toString() {
        return "PlateSensorGravityValue{gravity_1=" + this.gravity_1 + ", gravity_2=" + this.gravity_2 + ", gravity_3=" + this.gravity_3 + ", gravity_4=" + this.gravity_4 + '}';
    }
}
