package com.keenon.sdk.scmIot.protopack.util;

import java.lang.reflect.Type;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/scmIot/protopack/util/TypeVo.class */
public class TypeVo {
    private Type nextType;
    private Class<?> cls;

    public Type getNextType() {
        return this.nextType;
    }

    public void setNextType(Type nextType) {
        this.nextType = nextType;
    }

    public Class<?> getCls() {
        return this.cls;
    }

    public void setCls(Class<?> cls) {
        this.cls = cls;
    }
}
