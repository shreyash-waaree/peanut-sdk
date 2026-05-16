package com.keenon.sdk.scmIot.protopack.marshal;

import com.keenon.sdk.scmIot.protopack.base.Marshallable;
import com.keenon.sdk.scmIot.protopack.base.Pack;
import com.keenon.sdk.scmIot.protopack.base.Unpack;
import com.keenon.sdk.scmIot.protopack.util.Ulong;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/scmIot/protopack/marshal/UlongMarshal.class */
public class UlongMarshal implements Marshallable {
    public Ulong data;

    public UlongMarshal() {
    }

    public UlongMarshal(Ulong ulong) {
        this.data = ulong;
    }

    @Override // com.keenon.sdk.scmIot.protopack.base.Marshallable
    public void marshal(Pack pack) {
        pack.putUlong(this.data);
    }

    @Override // com.keenon.sdk.scmIot.protopack.base.Marshallable
    public void unmarshal(Unpack unpack) {
        this.data = unpack.popUlong();
    }

    public String toString() {
        return String.valueOf(this.data);
    }

    @Override // com.keenon.sdk.scmIot.protopack.base.Marshallable
    public Object getData() {
        return this.data;
    }
}
