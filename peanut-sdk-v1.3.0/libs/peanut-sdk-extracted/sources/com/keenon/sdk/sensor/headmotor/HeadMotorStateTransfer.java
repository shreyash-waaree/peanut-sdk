package com.keenon.sdk.sensor.headmotor;

import com.keenon.sdk.scmIot.transfer.AbsSCMIotDataTransfer;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/sensor/headmotor/HeadMotorStateTransfer.class */
public class HeadMotorStateTransfer extends AbsSCMIotDataTransfer<HeadMotorStateResult> {
    /* JADX WARN: Can't rename method to resolve collision */
    @Override // com.keenon.sdk.scmIot.transfer.AbsSCMIotDataTransfer
    public HeadMotorStateResult newDataInstance() {
        return new HeadMotorStateResult();
    }
}
