package com.keenon.sdk.scmIot.protopack.marshal;

import com.keenon.sdk.scmIot.protopack.base.Pack;
import com.keenon.sdk.scmIot.protopack.base.Unpack;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/scmIot/protopack/marshal/ObjectMarshal.class */
public class ObjectMarshal extends BeanMarshal {
    private Object obj;

    public ObjectMarshal(Object obj) {
        this.obj = obj;
    }

    public Object getObj() {
        return this.obj;
    }

    public void setObj(Object obj) {
        this.obj = obj;
    }

    @Override // com.keenon.sdk.scmIot.protopack.marshal.BeanMarshal, com.keenon.sdk.scmIot.protopack.base.Marshallable
    public void marshal(Pack pack) {
        marshalObject(pack, this.obj);
    }

    @Override // com.keenon.sdk.scmIot.protopack.marshal.BeanMarshal, com.keenon.sdk.scmIot.protopack.base.Marshallable
    public void unmarshal(Unpack unpack) {
        unmarshalObject(unpack, this.obj);
    }

    @Override // com.keenon.sdk.scmIot.protopack.base.Marshallable
    public Object getData() {
        return this.obj;
    }
}
