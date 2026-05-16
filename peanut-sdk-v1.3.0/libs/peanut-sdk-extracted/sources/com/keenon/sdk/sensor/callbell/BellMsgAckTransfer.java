package com.keenon.sdk.sensor.callbell;

import com.keenon.sdk.scmIot.transfer.AbsSCMIotDataTransfer;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/sensor/callbell/BellMsgAckTransfer.class */
public class BellMsgAckTransfer extends AbsSCMIotDataTransfer<BellMsgAckResult> {
    /* JADX WARN: Can't rename method to resolve collision */
    @Override // com.keenon.sdk.scmIot.transfer.AbsSCMIotDataTransfer
    public BellMsgAckResult newDataInstance() {
        return new BellMsgAckResult();
    }
}
