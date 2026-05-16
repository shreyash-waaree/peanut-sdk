package com.keenon.sdk.scmIot.protopack.base;

import com.keenon.sdk.scmIot.protopack.exception.PackException;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/scmIot/protopack/base/ProtoTag.class */
public class ProtoTag {
    public static final Byte U8 = (byte) 1;
    public static final Byte S8 = (byte) 2;
    public static final Byte U16 = (byte) 3;
    public static final Byte S16 = (byte) 4;
    public static final Byte U32 = (byte) 5;
    public static final Byte S32 = (byte) 6;
    public static final Byte U64 = (byte) 7;
    public static final Byte S64 = (byte) 8;
    public static final Byte FLOAT = (byte) 9;
    public static final Byte STR = (byte) 10;
    public static final Map<Byte, Integer> TAG_INFO = new HashMap();

    static {
        TAG_INFO.put(U8, 1);
        TAG_INFO.put(S8, 1);
        TAG_INFO.put(U16, 2);
        TAG_INFO.put(S16, 2);
        TAG_INFO.put(U32, 4);
        TAG_INFO.put(S32, 4);
        TAG_INFO.put(U64, 8);
        TAG_INFO.put(S64, 8);
        TAG_INFO.put(FLOAT, 4);
    }

    public static byte getTag(Byte tag) {
        return tag.byteValue();
    }

    public static byte getLength(Byte tag) {
        return TAG_INFO.get(tag).byteValue();
    }

    public static ByteBuffer addTL(ByteBuffer buffer, Byte tag) {
        try {
            buffer.put(tag.byteValue());
            buffer.put(getLength(tag));
            return buffer;
        } catch (BufferOverflowException bex) {
            throw new PackException("tlv too many bytes", bex);
        }
    }
}
