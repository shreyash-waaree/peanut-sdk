package com.keenon.sdk.scmIot.protopack.marshal;

import com.keenon.sdk.scmIot.protopack.base.Marshallable;
import com.keenon.sdk.scmIot.protopack.base.Pack;
import com.keenon.sdk.scmIot.protopack.base.Unpack;
import com.keenon.sdk.scmIot.protopack.util.Uint;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/scmIot/protopack/marshal/UintMarshal.class */
public class UintMarshal implements Marshallable {
    public Uint data;

    public UintMarshal() {
    }

    public UintMarshal(Uint uint) {
        this.data = uint;
    }

    @Override // com.keenon.sdk.scmIot.protopack.base.Marshallable
    public void marshal(Pack pack) {
        pack.putUInt(this.data);
    }

    @Override // com.keenon.sdk.scmIot.protopack.base.Marshallable
    public void unmarshal(Unpack unpack) {
        this.data = unpack.popUInt();
    }

    public String toString() {
        return String.valueOf(this.data);
    }

    @Override // com.keenon.sdk.scmIot.protopack.base.Marshallable
    public Object getData() {
        return this.data;
    }
}
