package com.keenon.sdk.scmIot.protopack.base;

import com.keenon.sdk.scmIot.protopack.exception.UnpackException;
import java.io.UnsupportedEncodingException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/scmIot/protopack/base/OTAUnpack.class */
public class OTAUnpack {
    protected ByteBuffer buffer;

    public OTAUnpack(byte[] bytes, int offset, int length) {
        this.buffer = ByteBuffer.wrap(bytes, offset, length);
        this.buffer.order(ByteOrder.LITTLE_ENDIAN);
    }

    public OTAUnpack(byte[] bytes) {
        this(bytes, 0, bytes.length);
    }

    public OTAUnpack(ByteBuffer buf) {
        this(buf.array(), buf.position(), buf.limit() - buf.position());
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

    public String popVarstr(int sz) {
        return popVarstr(sz, "utf-8");
    }

    public String popVarstr(int sz, String encode) {
        try {
            byte[] bytes = popFetch(sz);
            return new String(bytes, encode);
        } catch (UnsupportedEncodingException codeEx) {
            throw new UnpackException("unsupported encoding", codeEx);
        }
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

    public Integer popInt() {
        try {
            return Integer.valueOf(this.buffer.getInt());
        } catch (BufferUnderflowException bEx) {
            throw new UnpackException(bEx);
        }
    }

    public String toString() {
        return this.buffer.toString();
    }
}
