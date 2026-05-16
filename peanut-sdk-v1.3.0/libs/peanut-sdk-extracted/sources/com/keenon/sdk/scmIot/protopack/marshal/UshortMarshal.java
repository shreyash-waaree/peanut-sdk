package com.keenon.sdk.scmIot.protopack.marshal;

import com.keenon.sdk.scmIot.protopack.base.Marshallable;
import com.keenon.sdk.scmIot.protopack.base.Pack;
import com.keenon.sdk.scmIot.protopack.base.Unpack;
import com.keenon.sdk.scmIot.protopack.util.Ushort;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/scmIot/protopack/marshal/UshortMarshal.class */
public class UshortMarshal implements Marshallable {
    public Ushort data;

    public UshortMarshal() {
    }

    public UshortMarshal(Ushort ulong) {
        this.data = ulong;
    }

    @Override // com.keenon.sdk.scmIot.protopack.base.Marshallable
    public void marshal(Pack pack) {
        pack.putUshort(this.data);
    }

    @Override // com.keenon.sdk.scmIot.protopack.base.Marshallable
    public void unmarshal(Unpack unpack) {
        this.data = unpack.popUshort();
    }

    public String toString() {
        return String.valueOf(this.data);
    }

    @Override // com.keenon.sdk.scmIot.protopack.base.Marshallable
    public Object getData() {
        return this.data;
    }
}
