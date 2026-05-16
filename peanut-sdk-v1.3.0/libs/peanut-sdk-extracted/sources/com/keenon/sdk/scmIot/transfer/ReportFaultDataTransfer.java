package com.keenon.sdk.scmIot.transfer;

import com.keenon.sdk.scmIot.bean.response.ReportInfo;
import com.keenon.sdk.scmIot.protopack.base.ProtoHeader;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/scmIot/transfer/ReportFaultDataTransfer.class */
public class ReportFaultDataTransfer extends AbsSCMIotDataTransfer<ReportInfo> {
    @Override // com.keenon.sdk.scmIot.transfer.AbsSCMIotDataTransfer, com.keenon.sdk.scmIot.transfer.ISCMThingDataTransfer
    public ReportInfo dataTransfer(ProtoHeader header, byte[] data) {
        ReportInfo reportFault = (ReportInfo) super.dataTransfer(header, data);
        reportFault.setDev(header.getDev());
        reportFault.setTopic(header.getTopic());
        return reportFault;
    }

    /* JADX WARN: Can't rename method to resolve collision */
    @Override // com.keenon.sdk.scmIot.transfer.AbsSCMIotDataTransfer
    public ReportInfo newDataInstance() {
        return new ReportInfo();
    }
}
