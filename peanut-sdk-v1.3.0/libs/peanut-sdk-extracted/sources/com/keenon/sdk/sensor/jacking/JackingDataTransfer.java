package com.keenon.sdk.sensor.jacking;

import com.keenon.sdk.scmIot.transfer.AbsSCMIotDataTransfer;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/sensor/jacking/JackingDataTransfer.class */
public class JackingDataTransfer extends AbsSCMIotDataTransfer<JackingStateResult> {
    /* JADX WARN: Can't rename method to resolve collision */
    @Override // com.keenon.sdk.scmIot.transfer.AbsSCMIotDataTransfer
    public JackingStateResult newDataInstance() {
        return new JackingStateResult();
    }
}
