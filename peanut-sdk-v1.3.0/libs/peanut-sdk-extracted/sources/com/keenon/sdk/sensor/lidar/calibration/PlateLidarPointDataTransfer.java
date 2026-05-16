package com.keenon.sdk.sensor.lidar.calibration;

import com.keenon.sdk.scmIot.protopack.base.ProtoHeader;
import com.keenon.sdk.scmIot.transfer.AbsSCMIotDataTransfer;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/sensor/lidar/calibration/PlateLidarPointDataTransfer.class */
public class PlateLidarPointDataTransfer extends AbsSCMIotDataTransfer<LidarPointResultBean> {
    /* JADX WARN: Can't rename method to resolve collision */
    @Override // com.keenon.sdk.scmIot.transfer.AbsSCMIotDataTransfer
    public LidarPointResultBean newDataInstance() {
        return new LidarPointResultBean();
    }

    @Override // com.keenon.sdk.scmIot.transfer.AbsSCMIotDataTransfer
    public void acceptHeader(LidarPointResultBean outputBean, ProtoHeader header) {
        outputBean.setLidarId(header.getDev());
    }
}
