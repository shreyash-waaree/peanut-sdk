package com.keenon.sdk.sensor.lidar.calibration;

import com.keenon.sdk.scmIot.protopack.base.ProtoHeader;
import com.keenon.sdk.scmIot.transfer.AbsSCMIotDataTransfer;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/sensor/lidar/calibration/PlateCalibrationStatusDataTransfer.class */
public class PlateCalibrationStatusDataTransfer extends AbsSCMIotDataTransfer<CalibrationResultBean> {
    /* JADX WARN: Can't rename method to resolve collision */
    @Override // com.keenon.sdk.scmIot.transfer.AbsSCMIotDataTransfer
    public CalibrationResultBean newDataInstance() {
        return new CalibrationResultBean();
    }

    @Override // com.keenon.sdk.scmIot.transfer.AbsSCMIotDataTransfer
    public void acceptHeader(CalibrationResultBean outputBean, ProtoHeader header) {
        outputBean.setLidarId(header.getDev());
    }
}
