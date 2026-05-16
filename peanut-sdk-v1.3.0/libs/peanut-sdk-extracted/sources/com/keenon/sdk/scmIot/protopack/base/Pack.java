package com.keenon.sdk.scmIot.protopack.base;

import com.keenon.common.utils.ByteUtils;
import com.keenon.sdk.scmIot.protopack.exception.PackException;
import com.keenon.sdk.scmIot.protopack.util.Uint;
import com.keenon.sdk.scmIot.protopack.util.Ulong;
import com.keenon.sdk.scmIot.protopack.util.Ushort;
import java.io.UnsupportedEncodingException;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/scmIot/protopack/base/Pack.class */
public class Pack {
    protected Object attachment;
    private ByteBuffer buffer;
    private int m_maxCapacity;

    public Pack() {
        this.m_maxCapacity = 2097152;
        this.buffer = ByteBuffer.allocate(3);
        this.buffer.order(ByteOrder.LITTLE_ENDIAN);
    }

    public Pack(int size) {
        this.m_maxCapacity = 2097152;
        this.buffer = ByteBuffer.allocate(size);
        this.buffer.order(ByteOrder.LITTLE_ENDIAN);
    }

    public Object getAttachment() {
        return this.attachment;
    }

    public void setAttachment(Object obj) {
        this.attachment = obj;
    }

    public int size() {
        return this.buffer.position();
    }

    public void clear() {
        this.buffer.clear();
    }

    public void mark() {
        this.buffer.mark();
    }

    public void reset() {
        this.buffer.reset();
    }

    public ByteBuffer getBuffer() {
        ByteBuffer dup = this.buffer.duplicate();
        dup.flip();
        return dup;
    }

    public Pack putFetch(byte[] bytes) {
        try {
            ensureCapacity(bytes.length);
            this.buffer.put(bytes);
            return this;
        } catch (BufferOverflowException bex) {
            throw new PackException("pack too many bytes", bex);
        }
    }

    public Pack putByte(byte bt) {
        try {
            ensureCapacity(3);
            ProtoTag.addTL(this.buffer, ProtoTag.U8);
            this.buffer.put(bt);
            return this;
        } catch (BufferOverflowException bex) {
            throw new PackException("pack too many bytes", bex);
        }
    }

    public Pack putInt(int val) {
        ensureCapacity(6);
        ProtoTag.addTL(this.buffer, ProtoTag.U32);
        this.buffer.putInt(val);
        return this;
    }

    public Pack putUInt(Uint val) {
        return putInt(val.intValue());
    }

    public Pack putUshort(Ushort val) {
        try {
            ensureCapacity(4);
            ProtoTag.addTL(this.buffer, ProtoTag.S16);
            this.buffer.putShort(val.shortValue());
            return this;
        } catch (BufferOverflowException bex) {
            throw new PackException("pack too many bytes", bex);
        }
    }

    public Pack putUlong(Ulong val) {
        return putLong(val.longValue());
    }

    public Pack putBoolean(boolean val) {
        try {
            ensureCapacity(1);
            this.buffer.put((byte) (val ? 1 : 0));
            return this;
        } catch (BufferOverflowException bex) {
            throw new PackException("pack too many bytes", bex);
        }
    }

    public Pack putFloat(float val) {
        try {
            ensureCapacity(4);
            this.buffer.putFloat(val);
            return this;
        } catch (BufferOverflowException bex) {
            throw new PackException("pack too many bytes", bex);
        }
    }

    public Pack putLong(long val) {
        try {
            ensureCapacity(8);
            this.buffer.putLong(val);
            return this;
        } catch (BufferOverflowException bex) {
            throw new PackException("pack too many bytes", bex);
        }
    }

    public Pack putShort(short val) {
        try {
            ensureCapacity(4);
            ProtoTag.addTL(this.buffer, ProtoTag.U16);
            this.buffer.putShort(val);
            return this;
        } catch (BufferOverflowException bex) {
            throw new PackException("pack too many bytes", bex);
        }
    }

    public Pack putBytes(byte[] bytes) {
        try {
            ensureCapacity(2 + bytes.length);
            this.buffer.put(ProtoTag.U8.byteValue());
            this.buffer.put((byte) bytes.length);
            this.buffer.put(bytes);
            return this;
        } catch (BufferOverflowException bex) {
            throw new PackException("required too many bytes :" + bytes.length, bex);
        }
    }

    public Pack putVarbin(byte[] bytes) {
        try {
            ensureCapacity(2 + bytes.length);
            this.buffer.putShort((short) bytes.length);
            this.buffer.put(bytes);
            return this;
        } catch (BufferOverflowException bex) {
            throw new PackException("required too many bytes :" + bytes.length, bex);
        }
    }

    public Pack putVarbin32(byte[] bytes) {
        try {
            ensureCapacity(4 + bytes.length);
            this.buffer.putInt(bytes.length);
            this.buffer.put(bytes);
            return this;
        } catch (BufferOverflowException bex) {
            throw new PackException("pack too many bytes", bex);
        }
    }

    public Pack putVarstr(String str) {
        if (str == null) {
            str = "";
        }
        try {
            return putVarbin(str.getBytes("utf-8"));
        } catch (UnsupportedEncodingException codeEx) {
            throw new PackException(codeEx);
        }
    }

    public Pack putVarstr32(String str) {
        if (str == null) {
            str = "";
        }
        try {
            return putVarbin32(str.getBytes("utf-8"));
        } catch (UnsupportedEncodingException codeEx) {
            throw new PackException(codeEx);
        }
    }

    public Pack putMarshallable(Marshallable mar) {
        mar.marshal(this);
        return this;
    }

    public Pack putBuffer(ByteBuffer buf) {
        try {
            ensureCapacity(buf.capacity());
            this.buffer.put(buf.array(), 0, buf.limit());
            return this;
        } catch (BufferOverflowException bex) {
            throw new PackException(bex);
        }
    }

    public void replaceShort(int off, short val) {
        try {
            int pos = this.buffer.position();
            this.buffer.position(off);
            this.buffer.putShort(val).position(pos);
        } catch (IllegalArgumentException iex) {
            throw new PackException(iex);
        } catch (BufferOverflowException bex) {
            throw new PackException("pack too many bytes", bex);
        }
    }

    public void replaceInt(int off, int val) {
        try {
            int pos = this.buffer.position();
            this.buffer.position(off);
            this.buffer.putInt(val).position(pos);
        } catch (IllegalArgumentException iex) {
            throw new PackException(iex);
        } catch (BufferOverflowException bex) {
            throw new PackException("pack too many bytes", bex);
        }
    }

    public void ensureCapacity(int increament) throws BufferOverflowException {
        if (this.buffer.remaining() >= increament) {
            return;
        }
        int requiredCapacity = (this.buffer.capacity() + increament) - this.buffer.remaining();
        if (requiredCapacity > this.m_maxCapacity) {
            throw new BufferOverflowException();
        }
        int tmp = Math.max(requiredCapacity, this.buffer.capacity());
        int newCapacity = Math.min(tmp, this.m_maxCapacity);
        ByteBuffer newBuffer = ByteBuffer.allocate(newCapacity);
        newBuffer.order(this.buffer.order());
        this.buffer.flip();
        newBuffer.put(this.buffer);
        this.buffer = newBuffer;
    }

    public String toString() {
        byte[] byteArray = this.buffer.array();
        StringBuilder sb = new StringBuilder(size() * 8);
        for (int i = 0; i < size(); i++) {
            sb.append(ByteUtils.byteToBitString(byteArray[i]));
            sb.append("  ");
            int j = i + 1;
            if (j % 4 == 0) {
                sb.append('\n');
            }
        }
        return this.buffer.toString() + " Size " + size() + " and binary bits : \n" + sb.toString();
    }

    public byte[] getBytes() {
        return this.buffer.array();
    }
}
