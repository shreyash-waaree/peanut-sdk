package com.keenon.sdk.sensor.battery;

import com.keenon.sdk.scmIot.transfer.AbsSCMIotDataTransfer;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/sensor/battery/BatteryDataTransfer.class */
public class BatteryDataTransfer extends AbsSCMIotDataTransfer<BatteryTransportResult> {
    /* JADX WARN: Can't rename method to resolve collision */
    @Override // com.keenon.sdk.scmIot.transfer.AbsSCMIotDataTransfer
    public BatteryTransportResult newDataInstance() {
        return new BatteryTransportResult();
    }
}
