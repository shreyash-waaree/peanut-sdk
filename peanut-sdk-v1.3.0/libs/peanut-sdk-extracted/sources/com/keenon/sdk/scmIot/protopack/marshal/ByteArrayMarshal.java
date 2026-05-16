package com.keenon.sdk.scmIot.protopack.marshal;

import com.keenon.sdk.scmIot.protopack.base.Marshallable;
import com.keenon.sdk.scmIot.protopack.base.Pack;
import com.keenon.sdk.scmIot.protopack.base.Unpack;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/scmIot/protopack/marshal/ByteArrayMarshal.class */
public class ByteArrayMarshal implements Marshallable {
    public byte[] data;

    public ByteArrayMarshal() {
    }

    public ByteArrayMarshal(byte[] data) {
        this.data = data;
    }

    @Override // com.keenon.sdk.scmIot.protopack.base.Marshallable
    public void marshal(Pack pack) {
        if (this.data != null) {
            pack.putInt(this.data.length);
            for (byte b : this.data) {
                pack.putByte(b);
            }
            return;
        }
        pack.putInt(0);
    }

    @Override // com.keenon.sdk.scmIot.protopack.base.Marshallable
    public void unmarshal(Unpack unpack) {
        this.data = unpack.getBuffer().array();
        int size = this.data.length;
        for (int i = 0; i < size; i++) {
            this.data[i] = unpack.popByte().byteValue();
        }
    }

    @Override // com.keenon.sdk.scmIot.protopack.base.Marshallable
    public Object getData() {
        return this.data;
    }
}
