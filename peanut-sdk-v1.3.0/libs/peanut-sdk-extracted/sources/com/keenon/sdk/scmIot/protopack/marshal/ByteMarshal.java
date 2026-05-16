package com.keenon.sdk.scmIot.protopack.marshal;

import com.keenon.sdk.scmIot.protopack.base.Marshallable;
import com.keenon.sdk.scmIot.protopack.base.Pack;
import com.keenon.sdk.scmIot.protopack.base.Unpack;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/scmIot/protopack/marshal/ByteMarshal.class */
public class ByteMarshal implements Marshallable {
    public byte data;

    public ByteMarshal() {
    }

    public ByteMarshal(byte data) {
        this.data = data;
    }

    @Override // com.keenon.sdk.scmIot.protopack.base.Marshallable
    public void marshal(Pack pack) {
        pack.putByte(this.data);
    }

    @Override // com.keenon.sdk.scmIot.protopack.base.Marshallable
    public void unmarshal(Unpack unpack) {
        this.data = unpack.popByte().byteValue();
    }

    public String toString() {
        return String.valueOf((int) this.data);
    }

    @Override // com.keenon.sdk.scmIot.protopack.base.Marshallable
    public Object getData() {
        return Byte.valueOf(this.data);
    }
}
