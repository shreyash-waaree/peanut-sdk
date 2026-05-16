package com.keenon.sdk.sensor.headmotor;

import com.keenon.sdk.scmIot.protopack.annotation.BeanFieldAno;
import com.keenon.sdk.scmIot.protopack.util.Ushort;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/sensor/headmotor/PlayHeadMotorParam.class */
public class PlayHeadMotorParam {

    @BeanFieldAno(order = 1)
    private byte action;

    @BeanFieldAno(order = 2)
    private Ushort angle;

    public byte getAction() {
        return this.action;
    }

    public void setAction(byte action) {
        this.action = action;
    }

    public Ushort getAngle() {
        return this.angle;
    }

    public void setAngle(Ushort angle) {
        this.angle = angle;
    }

    public String toString() {
        return "PlayHeadMotorParam{action=" + ((int) this.action) + ", angle=" + this.angle + '}';
    }
}
