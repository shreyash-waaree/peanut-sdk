package com.keenon.sdk.sensor.door;

import com.keenon.sdk.scmIot.protopack.annotation.BeanFieldAno;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/sensor/door/DoorSwitchParam.class */
public class DoorSwitchParam {

    @BeanFieldAno(order = 1)
    private byte switchFlag;

    public byte getSwitchFlag() {
        return this.switchFlag;
    }

    public void setSwitchFlag(byte switchFlag) {
        this.switchFlag = switchFlag;
    }
}
