package com.keenon.sdk.sensor.lidar.calibration;

import com.keenon.sdk.scmIot.protopack.base.ProtoHeader;
import com.keenon.sdk.scmIot.transfer.AbsSCMIotDataTransfer;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/sensor/lidar/calibration/PlateLidarParamDataTransfer.class */
public class PlateLidarParamDataTransfer extends AbsSCMIotDataTransfer<LidarParamResultBean> {
    /* JADX WARN: Can't rename method to resolve collision */
    @Override // com.keenon.sdk.scmIot.transfer.AbsSCMIotDataTransfer
    public LidarParamResultBean newDataInstance() {
        return new LidarParamResultBean();
    }

    @Override // com.keenon.sdk.scmIot.transfer.AbsSCMIotDataTransfer
    public void acceptHeader(LidarParamResultBean outputBean, ProtoHeader header) {
        outputBean.setLidarId(header.getDev());
    }
}
