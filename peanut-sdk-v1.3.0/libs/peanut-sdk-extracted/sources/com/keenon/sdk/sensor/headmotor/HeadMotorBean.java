package com.keenon.sdk.sensor.headmotor;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/sensor/headmotor/HeadMotorBean.class */
public class HeadMotorBean {
    private int state;
    private int angleX;
    private int angleY;

    public int getState() {
        return this.state;
    }

    public void setState(int state) {
        this.state = state;
    }

    public int getAngleX() {
        return this.angleX;
    }

    public void setAngleX(int angleX) {
        this.angleX = angleX;
    }

    public int getAngleY() {
        return this.angleY;
    }

    public void setAngleY(int angleY) {
        this.angleY = angleY;
    }

    public String toString() {
        return "HeadMotorBean{state=" + this.state + ", angleX=" + this.angleX + ", angleY=" + this.angleY + '}';
    }
}
