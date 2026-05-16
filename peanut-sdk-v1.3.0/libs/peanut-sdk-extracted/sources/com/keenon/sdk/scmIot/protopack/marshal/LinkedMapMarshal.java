package com.keenon.sdk.scmIot.protopack.marshal;

import com.keenon.sdk.scmIot.protopack.base.Unpack;
import com.keenon.sdk.scmIot.protopack.util.MarshallUtils;
import java.util.Map;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/scmIot/protopack/marshal/LinkedMapMarshal.class */
public class LinkedMapMarshal<T, K> extends MapMarshal<T, K> {
    public LinkedMapMarshal(Class<T> keyType, Class<K> valueType) {
        super(keyType, valueType);
    }

    public LinkedMapMarshal(Map<T, K> map, Class<T> keyType, Class<K> valueType) {
        super(map, keyType, valueType);
    }

    @Override // com.keenon.sdk.scmIot.protopack.marshal.MapMarshal, com.keenon.sdk.scmIot.protopack.base.Marshallable
    public void unmarshal(Unpack unpack) {
        this.data = MarshallUtils.unpackMap(unpack, this.keyType, this.valueType, true);
    }

    @Override // com.keenon.sdk.scmIot.protopack.marshal.MapMarshal
    public String toString() {
        return this.data.toString();
    }
}
