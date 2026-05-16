package com.keenon.sdk.scmIot.protopack.base;

import com.keenon.sdk.scmIot.protopack.exception.UnpackException;
import com.keenon.sdk.scmIot.protopack.util.Uint;
import com.keenon.sdk.scmIot.protopack.util.Ulong;
import com.keenon.sdk.scmIot.protopack.util.Ushort;
import java.io.UnsupportedEncodingException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/scmIot/protopack/base/Unpack.class */
public class Unpack {
    protected ByteBuffer buffer;
    protected Object attachment;
    protected short[] shorts;
    protected float[] floats;
    protected int[] ints;

    public Unpack(byte[] bytes, int offset, int length) {
        this.shorts = new short[0];
        this.floats = new float[0];
        this.ints = new int[0];
        this.buffer = ByteBuffer.wrap(bytes, offset, length);
        this.buffer.order(ByteOrder.LITTLE_ENDIAN);
    }

    public Unpack(byte[] bytes) {
        this(bytes, 0, bytes.length);
    }

    public Unpack(ByteBuffer buf) {
        this(buf.array(), buf.position(), buf.limit() - buf.position());
    }

    public Unpack() {
        this.shorts = new short[0];
        this.floats = new float[0];
        this.ints = new int[0];
        this.buffer = null;
    }

    public Object getAttachment() {
        return this.attachment;
    }

    public void setAttachment(Object obj) {
        this.attachment = obj;
    }

    public int size() {
        return this.buffer.limit() - this.buffer.position();
    }

    public void reset(byte[] bytes) {
        this.buffer.clear();
        this.buffer.put(bytes);
        this.buffer.flip();
    }

    public ByteBuffer getBuffer() {
        return this.buffer.duplicate();
    }

    public byte[] popFetch(int sz) {
        try {
            byte[] fetch = new byte[sz];
            this.buffer.get(fetch);
            return fetch;
        } catch (BufferUnderflowException bEx) {
            throw new UnpackException(bEx);
        }
    }

    public Object popObject(Object obj) throws Exception {
        if (obj instanceof Marshallable) {
            return popMarshallable((Marshallable) obj);
        }
        if (obj instanceof String) {
            return popVarstr();
        }
        if (obj instanceof Unpack) {
            ((Unpack) obj).buffer = ByteBuffer.allocate(this.buffer.remaining());
            ((Unpack) obj).buffer.order(ByteOrder.LITTLE_ENDIAN);
            ((Unpack) obj).buffer.put(this.buffer);
            ((Unpack) obj).buffer.rewind();
            return obj;
        }
        throw new UnpackException("unknow object type");
    }

    public Byte popRawByte() {
        try {
            return Byte.valueOf(this.buffer.get());
        } catch (BufferUnderflowException bEx) {
            throw new UnpackException(bEx);
        }
    }

    public Byte popByte() {
        byte result;
        try {
            if (this.buffer.get(this.buffer.position() + 1) > ProtoTag.getLength(ProtoTag.U8)) {
                result = this.buffer.get(this.buffer.position() + 2);
                this.buffer.position(this.buffer.position() + 1);
            } else {
                result = this.buffer.get(this.buffer.position() + 2);
                this.buffer.position(this.buffer.position() + 3);
            }
            return Byte.valueOf(result);
        } catch (BufferUnderflowException bEx) {
            throw new UnpackException(bEx);
        }
    }

    public byte[] popVarbin8() {
        if (this.buffer.position() >= this.buffer.limit()) {
            return new byte[0];
        }
        int length = this.buffer.get(this.buffer.position() + 1) & 255;
        this.buffer.getShort();
        return popFetch(length);
    }

    public short[] popVarbinShorts() {
        if (this.buffer.position() >= this.buffer.limit()) {
            return this.shorts;
        }
        int len = (this.buffer.getShort(this.buffer.position() + 1) & 255) / 2;
        this.shorts = new short[len];
        this.buffer.getShort();
        for (int i = 0; i < len; i++) {
            this.shorts[i] = this.buffer.getShort();
        }
        return this.shorts;
    }

    public float[] popVarbinFloats() {
        if (this.buffer.position() >= this.buffer.limit()) {
            return this.floats;
        }
        int len = (this.buffer.getShort(this.buffer.position() + 1) & 255) / 4;
        this.floats = new float[len];
        this.buffer.getShort();
        for (int i = 0; i < len; i++) {
            this.floats[i] = this.buffer.getFloat();
        }
        return this.floats;
    }

    public int[] popVarbinInt() {
        if (this.buffer.position() >= this.buffer.limit()) {
            return this.ints;
        }
        int len = (this.buffer.getShort(this.buffer.position() + 1) & 255) / 4;
        this.ints = new int[len];
        this.buffer.getShort();
        for (int i = 0; i < len; i++) {
            this.ints[i] = this.buffer.getInt();
        }
        return this.ints;
    }

    public byte[] popVarbin() {
        int length = this.buffer.get(1) & 255;
        this.buffer.getShort();
        return popFetch(length);
    }

    public byte[] popVarbin32() {
        return popFetch(this.buffer.getInt());
    }

    public String popVarbin(String encode) {
        try {
            byte[] bytes = popVarbin();
            return new String(bytes, encode);
        } catch (UnsupportedEncodingException codeEx) {
            throw new UnpackException("unsupported encoding", codeEx);
        }
    }

    public String popVarbin32(String encode) {
        try {
            byte[] bytes = popVarbin32();
            return new String(bytes, encode);
        } catch (UnsupportedEncodingException codeEx) {
            throw new UnpackException(codeEx);
        }
    }

    public String popVarstr() {
        return popVarbin("utf-8");
    }

    public String popVarstr32() {
        return popVarbin32("utf-8");
    }

    public String popVarstr(String encode) {
        return popVarbin(encode);
    }

    public byte[] popBytes() {
        return popVarbin8();
    }

    public float[] popFloats() {
        return popVarbinFloats();
    }

    public short[] popShorts() {
        return popVarbinShorts();
    }

    public int[] popInts() {
        return popVarbinInt();
    }

    public Integer popInt() {
        try {
            Integer result = Integer.valueOf(this.buffer.getInt(this.buffer.position() + 2));
            this.buffer.position(this.buffer.position() + 6);
            return result;
        } catch (BufferUnderflowException bEx) {
            throw new UnpackException(bEx);
        }
    }

    public float popFloat() {
        try {
            this.buffer.position(this.buffer.position() + 2);
            return this.buffer.getFloat();
        } catch (BufferUnderflowException bEx) {
            throw new UnpackException(bEx);
        }
    }

    public float popRwaFloat() {
        try {
            return this.buffer.getFloat();
        } catch (BufferUnderflowException bEx) {
            throw new UnpackException(bEx);
        }
    }

    public Uint popUInt() {
        return new Uint(popInt().intValue());
    }

    public Ushort popUshort() {
        return new Ushort(popShort().shortValue());
    }

    public Ulong popUlong() {
        return new Ulong(popLong().longValue());
    }

    public Long popLong() {
        try {
            return Long.valueOf(this.buffer.getLong());
        } catch (BufferUnderflowException bEx) {
            throw new UnpackException(bEx);
        }
    }

    public Short popShort() {
        try {
            Short result = Short.valueOf(this.buffer.getShort(this.buffer.position() + 2));
            this.buffer.position(this.buffer.position() + 4);
            return result;
        } catch (BufferUnderflowException bEx) {
            throw new UnpackException(bEx);
        }
    }

    public Short popRwaShort() {
        try {
            return Short.valueOf(this.buffer.getShort());
        } catch (BufferUnderflowException bEx) {
            throw new UnpackException(bEx);
        }
    }

    public Marshallable popMarshallable(Marshallable mar) {
        mar.unmarshal(this);
        return mar;
    }

    public Boolean popBoolean() {
        if (popByte().byteValue() > 0) {
            return true;
        }
        return false;
    }

    public String toString() {
        return this.buffer.toString();
    }
}
