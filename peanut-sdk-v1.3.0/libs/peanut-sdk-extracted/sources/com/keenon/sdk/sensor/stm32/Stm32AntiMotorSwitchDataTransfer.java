package com.keenon.sdk.sensor.stm32;

import com.keenon.sdk.scmIot.transfer.AbsSCMIotDataTransfer;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/sensor/stm32/Stm32AntiMotorSwitchDataTransfer.class */
public class Stm32AntiMotorSwitchDataTransfer extends AbsSCMIotDataTransfer<AntiMotorSwitch> {
    /* JADX WARN: Can't rename method to resolve collision */
    @Override // com.keenon.sdk.scmIot.transfer.AbsSCMIotDataTransfer
    public AntiMotorSwitch newDataInstance() {
        return new AntiMotorSwitch();
    }
}
