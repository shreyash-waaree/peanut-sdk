package com.keenon.sdk.sensor.callbell;

import com.keenon.sdk.sensor.callbell.BellInterface;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/sensor/callbell/BellConfig.class */
public class BellConfig {
    private String SN;
    private int stayTime;
    private BellInterface.KeyEvent keyEvent;
    private boolean enable;
    private BellInterface.RobotState robotState;

    public String getSN() {
        return this.SN;
    }

    public void setSN(String SN) {
        this.SN = SN;
    }

    public int getStayTime() {
        return this.stayTime;
    }

    public void setStayTime(int stayTime) {
        this.stayTime = stayTime;
    }

    public BellInterface.KeyEvent getKeyEvent() {
        return this.keyEvent;
    }

    public void setKeyEvent(BellInterface.KeyEvent keyEvent) {
        this.keyEvent = keyEvent;
    }

    public boolean isEnable() {
        return this.enable;
    }

    public void setEnable(boolean enable) {
        this.enable = enable;
    }

    public BellInterface.RobotState getRobotState() {
        return this.robotState;
    }

    public void setRobotState(BellInterface.RobotState robotState) {
        this.robotState = robotState;
    }

    public String toString() {
        return "BellConfig{SN='" + this.SN + "', stayTime=" + this.stayTime + ", keyEvent=" + this.keyEvent + ", enable=" + this.enable + ", robotState=" + this.robotState + '}';
    }
}
