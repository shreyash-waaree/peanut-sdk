package com.keenon.sdk.sensor.plasma.transfer;

import com.keenon.sdk.scmIot.transfer.AbsSCMIotDataTransfer;
import com.keenon.sdk.sensor.plasma.PlasmaEnvironmentInfoResult;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/sensor/plasma/transfer/PlasmaStatusDataTransfer.class */
public class PlasmaStatusDataTransfer extends AbsSCMIotDataTransfer<PlasmaEnvironmentInfoResult> {
    /* JADX WARN: Can't rename method to resolve collision */
    @Override // com.keenon.sdk.scmIot.transfer.AbsSCMIotDataTransfer
    public PlasmaEnvironmentInfoResult newDataInstance() {
        return new PlasmaEnvironmentInfoResult();
    }
}
