package com.keenon.sdk.scmIot.protopack.base;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/scmIot/protopack/base/Marshallable.class */
public interface Marshallable {
    void marshal(Pack pack);

    void unmarshal(Unpack unpack);

    Object getData();
}
