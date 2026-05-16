package com.keenon.sdk.scmIot.protopack.marshal;

import com.keenon.sdk.scmIot.protopack.base.Marshallable;
import com.keenon.sdk.scmIot.protopack.base.Pack;
import com.keenon.sdk.scmIot.protopack.base.Unpack;
import com.keenon.sdk.scmIot.protopack.util.MarshallUtils;
import java.util.Map;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/scmIot/protopack/marshal/MapMarshal.class */
public class MapMarshal<T, K> implements Marshallable {
    public Class<T> keyType;
    public Class<K> valueType;
    public Map<T, K> data;

    public MapMarshal(Class<T> keyType, Class<K> valueType) {
        this.keyType = keyType;
        this.valueType = valueType;
    }

    public MapMarshal(Map<T, K> map, Class<T> keyType, Class<K> valueType) {
        this.keyType = keyType;
        this.valueType = valueType;
        this.data = map;
    }

    @Override // com.keenon.sdk.scmIot.protopack.base.Marshallable
    public void marshal(Pack pack) {
        MarshallUtils.packMap(pack, this.data, this.keyType, this.valueType);
    }

    @Override // com.keenon.sdk.scmIot.protopack.base.Marshallable
    public void unmarshal(Unpack unpack) {
        this.data = MarshallUtils.unpackMap(unpack, this.keyType, this.valueType, false);
    }

    public String toString() {
        return this.data.toString();
    }

    @Override // com.keenon.sdk.scmIot.protopack.base.Marshallable
    public Object getData() {
        return this.data;
    }
}
