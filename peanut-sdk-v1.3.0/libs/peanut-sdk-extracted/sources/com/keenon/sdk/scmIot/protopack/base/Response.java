package com.keenon.sdk.scmIot.protopack.base;

import com.keenon.sdk.scmIot.protopack.util.Uint;
import com.keenon.sdk.scmIot.protopack.util.Ulong;
import com.keenon.sdk.scmIot.protopack.util.Ushort;
import java.util.List;
import java.util.Map;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/scmIot/protopack/base/Response.class */
public interface Response {
    Uint popUint();

    Boolean popBoolean();

    Integer popInteger();

    Byte popByte();

    byte[] popBytes();

    Short popShort();

    Long popLong();

    String popString();

    String popString32();

    Ushort popUshort();

    Ulong popUlong();

    <T> List<T> popList(Class<T> cls);

    <K, V> Map<K, V> popMap(Class<K> cls, Class<V> cls2);

    <K, V> Map<K, V> popMap(Class<K> cls, Class<V> cls2, boolean z);

    Marshallable popMarshallable(Marshallable marshallable);

    Object popBeanMarshal(Object obj) throws Exception;
}
