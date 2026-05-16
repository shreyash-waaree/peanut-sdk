package com.keenon.sdk.sensor.uvlamp;

import com.keenon.sdk.scmIot.transfer.AbsSCMIotDataTransfer;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/sensor/uvlamp/UVLampDataTransfer.class */
public class UVLampDataTransfer extends AbsSCMIotDataTransfer<UVLampResult> {
    /* JADX WARN: Can't rename method to resolve collision */
    @Override // com.keenon.sdk.scmIot.transfer.AbsSCMIotDataTransfer
    public UVLampResult newDataInstance() {
        return new UVLampResult();
    }
}
