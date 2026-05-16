package com.keenon.sdk.scmIot.transfer;

import com.keenon.sdk.scmIot.protopack.base.ProtoHeader;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/scmIot/transfer/ISCMThingDataTransfer.class */
public interface ISCMThingDataTransfer<T> {
    T dataTransfer(ProtoHeader protoHeader, byte[] bArr);
}
