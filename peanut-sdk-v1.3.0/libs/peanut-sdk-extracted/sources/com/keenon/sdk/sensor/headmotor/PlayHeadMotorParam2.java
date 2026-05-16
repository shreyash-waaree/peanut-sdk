package com.keenon.sdk.sensor.headmotor;

import com.keenon.sdk.scmIot.protopack.annotation.BeanFieldAno;
import com.keenon.sdk.scmIot.protopack.util.Ushort;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/sensor/headmotor/PlayHeadMotorParam2.class */
public class PlayHeadMotorParam2 {

    @BeanFieldAno(order = 1)
    private byte action;

    @BeanFieldAno(order = 2)
    private Ushort angleX;

    @BeanFieldAno(order = 3)
    private Ushort angleY;

    public byte getAction() {
        return this.action;
    }

    public void setAction(byte action) {
        this.action = action;
    }

    public Ushort getAngleX() {
        return this.angleX;
    }

    public void setAngleX(Ushort angleX) {
        this.angleX = angleX;
    }

    public Ushort getAngleY() {
        return this.angleY;
    }

    public void setAngleY(Ushort angleY) {
        this.angleY = angleY;
    }

    public String toString() {
        return "PlayHeadMotorParam2{action=" + ((int) this.action) + ", angleX=" + this.angleX + ", angleY=" + this.angleY + '}';
    }
}
