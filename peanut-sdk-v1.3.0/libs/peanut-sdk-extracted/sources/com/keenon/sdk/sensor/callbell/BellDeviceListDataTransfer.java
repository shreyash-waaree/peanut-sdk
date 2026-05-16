package com.keenon.sdk.sensor.callbell;

import com.keenon.sdk.scmIot.transfer.AbsSCMIotDataTransfer;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/sensor/callbell/BellDeviceListDataTransfer.class */
public class BellDeviceListDataTransfer extends AbsSCMIotDataTransfer<BellDeviceListResult> {
    /* JADX WARN: Can't rename method to resolve collision */
    @Override // com.keenon.sdk.scmIot.transfer.AbsSCMIotDataTransfer
    public BellDeviceListResult newDataInstance() {
        return new BellDeviceListResult();
    }
}
