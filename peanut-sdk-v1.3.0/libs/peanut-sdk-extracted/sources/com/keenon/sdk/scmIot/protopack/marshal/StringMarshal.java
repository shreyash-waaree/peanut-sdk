package com.keenon.sdk.scmIot.protopack.marshal;

import com.keenon.sdk.scmIot.protopack.base.Marshallable;
import com.keenon.sdk.scmIot.protopack.base.Pack;
import com.keenon.sdk.scmIot.protopack.base.Unpack;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/scmIot/protopack/marshal/StringMarshal.class */
public class StringMarshal implements Marshallable {
    public String data;

    public StringMarshal() {
    }

    public StringMarshal(String data) {
        this.data = data;
    }

    @Override // com.keenon.sdk.scmIot.protopack.base.Marshallable
    public void marshal(Pack pack) {
        pack.putVarstr(this.data);
    }

    @Override // com.keenon.sdk.scmIot.protopack.base.Marshallable
    public void unmarshal(Unpack unpack) {
        this.data = unpack.popVarstr();
    }

    public String toString() {
        return this.data;
    }

    public int hashCode() {
        int result = (31 * 1) + (this.data == null ? 0 : this.data.hashCode());
        return result;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || !(obj instanceof StringMarshal)) {
            return false;
        }
        StringMarshal other = (StringMarshal) obj;
        if (this.data == null) {
            if (other.data != null) {
                return false;
            }
            return true;
        }
        if (!this.data.equals(other.data)) {
            return false;
        }
        return true;
    }

    @Override // com.keenon.sdk.scmIot.protopack.base.Marshallable
    public Object getData() {
        return this.data;
    }
}
