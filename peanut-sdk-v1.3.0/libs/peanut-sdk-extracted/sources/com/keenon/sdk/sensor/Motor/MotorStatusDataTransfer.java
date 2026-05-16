package com.keenon.sdk.sensor.Motor;

import com.keenon.sdk.scmIot.transfer.AbsSCMIotDataTransfer;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/sensor/Motor/MotorStatusDataTransfer.class */
public class MotorStatusDataTransfer extends AbsSCMIotDataTransfer<MotorStatus> {
    /* JADX WARN: Can't rename method to resolve collision */
    @Override // com.keenon.sdk.scmIot.transfer.AbsSCMIotDataTransfer
    public MotorStatus newDataInstance() {
        return new MotorStatus();
    }
}
