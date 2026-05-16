package com.keenon.sdk.scmIot.protopack.marshal;

import com.keenon.sdk.scmIot.protopack.base.Marshallable;
import com.keenon.sdk.scmIot.protopack.base.Pack;
import com.keenon.sdk.scmIot.protopack.base.Unpack;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/scmIot/protopack/marshal/LongMarshal.class */
public class LongMarshal implements Marshallable {
    public long data;

    public LongMarshal() {
    }

    public LongMarshal(long data) {
        this.data = data;
    }

    @Override // com.keenon.sdk.scmIot.protopack.base.Marshallable
    public void marshal(Pack pack) {
        pack.putLong(this.data);
    }

    @Override // com.keenon.sdk.scmIot.protopack.base.Marshallable
    public void unmarshal(Unpack unpack) {
        this.data = unpack.popLong().longValue();
    }

    public String toString() {
        return String.valueOf(this.data);
    }

    @Override // com.keenon.sdk.scmIot.protopack.base.Marshallable
    public Object getData() {
        return Long.valueOf(this.data);
    }
}
