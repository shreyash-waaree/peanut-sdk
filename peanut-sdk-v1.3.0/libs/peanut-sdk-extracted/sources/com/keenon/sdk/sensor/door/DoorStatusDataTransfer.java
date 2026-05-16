package com.keenon.sdk.sensor.door;

import com.keenon.sdk.scmIot.protopack.base.ProtoHeader;
import com.keenon.sdk.scmIot.transfer.AbsSCMIotDataTransfer;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/sensor/door/DoorStatusDataTransfer.class */
public class DoorStatusDataTransfer extends AbsSCMIotDataTransfer<DoorStatusInfo> {
    /* JADX WARN: Can't rename method to resolve collision */
    @Override // com.keenon.sdk.scmIot.transfer.AbsSCMIotDataTransfer
    public DoorStatusInfo newDataInstance() {
        return new DoorStatusInfo();
    }

    @Override // com.keenon.sdk.scmIot.transfer.AbsSCMIotDataTransfer
    public void acceptHeader(DoorStatusInfo outputBean, ProtoHeader header) {
        outputBean.setDoorId(header.getDev());
    }
}
