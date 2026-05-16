package com.keenon.sdk.scmIot.protopack.base;

import com.keenon.sdk.scmIot.protopack.util.Uint;
import com.keenon.sdk.scmIot.protopack.util.Ulong;
import com.keenon.sdk.scmIot.protopack.util.Ushort;
import java.util.List;
import java.util.Map;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/scmIot/protopack/base/Request.class */
public interface Request {
    void putUint(Uint uint);

    void putInteger(Integer num);

    void putLong(Long l);

    void putBoolean(Boolean bool);

    void putShort(Short sh);

    void putByte(Byte b);

    void putBytes(byte[] bArr);

    void putString(String str);

    void putString32(String str);

    void putUshort(Ushort ushort);

    void putUlong(Ulong ulong);

    <T> void putList(List<T> list, Class<T> cls);

    <K, V> void putMap(Map<K, V> map, Class<K> cls, Class<V> cls2);

    void putMarshall(Marshallable marshallable);

    void putBeanMarshal(Object obj) throws Exception;
}
