package com.keenon.sdk.scmIot.protopack.marshal;

import com.keenon.sdk.scmIot.protopack.base.Marshallable;
import com.keenon.sdk.scmIot.protopack.base.Pack;
import com.keenon.sdk.scmIot.protopack.base.Unpack;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/scmIot/protopack/marshal/IntegerMarshal.class */
public class IntegerMarshal implements Marshallable {
    public Integer data;

    public IntegerMarshal() {
    }

    public IntegerMarshal(Integer value) {
        this.data = value;
    }

    @Override // com.keenon.sdk.scmIot.protopack.base.Marshallable
    public void marshal(Pack pack) {
        pack.putInt(this.data.intValue());
    }

    @Override // com.keenon.sdk.scmIot.protopack.base.Marshallable
    public void unmarshal(Unpack unpack) {
        this.data = unpack.popInt();
    }

    public String toString() {
        return String.valueOf(this.data);
    }

    @Override // com.keenon.sdk.scmIot.protopack.base.Marshallable
    public Object getData() {
        return this.data;
    }
}
