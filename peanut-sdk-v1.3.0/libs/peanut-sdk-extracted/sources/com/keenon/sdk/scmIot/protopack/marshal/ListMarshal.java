package com.keenon.sdk.scmIot.protopack.marshal;

import com.keenon.sdk.scmIot.protopack.base.Marshallable;
import com.keenon.sdk.scmIot.protopack.base.Pack;
import com.keenon.sdk.scmIot.protopack.base.Unpack;
import com.keenon.sdk.scmIot.protopack.util.MarshallUtils;
import java.util.List;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/scmIot/protopack/marshal/ListMarshal.class */
public class ListMarshal<T> implements Marshallable {
    public List<T> list;
    public Class<T> elemType;

    public ListMarshal(Class<T> elemType) {
        this.elemType = elemType;
    }

    public ListMarshal(List<T> data, Class<T> elemType) {
        this.list = data;
        this.elemType = elemType;
    }

    @Override // com.keenon.sdk.scmIot.protopack.base.Marshallable
    public void marshal(Pack pack) {
        MarshallUtils.packList(pack, this.list, this.elemType);
    }

    @Override // com.keenon.sdk.scmIot.protopack.base.Marshallable
    public void unmarshal(Unpack unpack) {
        this.list = MarshallUtils.unpackList(unpack, this.elemType);
    }

    public String toString() {
        return this.list.toString();
    }

    @Override // com.keenon.sdk.scmIot.protopack.base.Marshallable
    public Object getData() {
        return this.list;
    }
}
