package com.keenon.sdk.sensor.plasma.transfer;

import com.keenon.sdk.scmIot.transfer.AbsSCMIotDataTransfer;
import com.keenon.sdk.sensor.plasma.PlasmaFanStatusInfoResult;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/sensor/plasma/transfer/PlasmaFanStatusDataTransfer.class */
public class PlasmaFanStatusDataTransfer extends AbsSCMIotDataTransfer<PlasmaFanStatusInfoResult> {
    /* JADX WARN: Can't rename method to resolve collision */
    @Override // com.keenon.sdk.scmIot.transfer.AbsSCMIotDataTransfer
    public PlasmaFanStatusInfoResult newDataInstance() {
        return new PlasmaFanStatusInfoResult();
    }
}
