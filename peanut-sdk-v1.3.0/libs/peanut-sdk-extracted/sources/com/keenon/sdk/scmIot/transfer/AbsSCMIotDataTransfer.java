package com.keenon.sdk.scmIot.transfer;

import com.keenon.sdk.scmIot.protopack.base.ProtoHeader;
import com.keenon.sdk.scmIot.protopack.base.Unpack;
import com.keenon.sdk.scmIot.protopack.marshal.ObjectMarshal;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/scmIot/transfer/AbsSCMIotDataTransfer.class */
public abstract class AbsSCMIotDataTransfer<T> implements ISCMThingDataTransfer<T> {
    public abstract T newDataInstance();

    @Override // com.keenon.sdk.scmIot.transfer.ISCMThingDataTransfer
    public T dataTransfer(ProtoHeader header, byte[] data) {
        Unpack dataUnpack = new Unpack(data);
        T outputBean = newDataInstance();
        ObjectMarshal outputMarshal = new ObjectMarshal(outputBean);
        outputMarshal.unmarshal(dataUnpack);
        acceptHeader(outputBean, header);
        return outputBean;
    }

    public void acceptHeader(T outputBean, ProtoHeader header) {
    }
}
