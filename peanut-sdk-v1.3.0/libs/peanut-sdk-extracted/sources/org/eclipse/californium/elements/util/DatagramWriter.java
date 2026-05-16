package org.eclipse.californium.elements.util;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-element-connector.jar:org/eclipse/californium/elements/util/DatagramWriter.class */
public final class DatagramWriter {
    private static final int DEFAULT_ARRAY_SIZE = 32;
    private static final int MAX_ARRAY_SIZE = 1073741823;
    public static final AtomicLong COPIES = new AtomicLong();
    public static final AtomicLong TAKES = new AtomicLong();
    private byte[] buffer;
    private int count;
    private byte currentByte;
    private int currentBitIndex;
    private final boolean secureClose;

    public DatagramWriter() {
        this(32, false);
    }

    public DatagramWriter(boolean secureClose) {
        this(32, secureClose);
    }

    public DatagramWriter(int size) {
        this(size, false);
    }

    public DatagramWriter(int size, boolean secureClose) {
        this.secureClose = secureClose;
        this.buffer = new byte[size];
        resetCurrentByte();
    }

    public void writeLong(long data, int numBits) {
        if (numBits < 0 || numBits > 64) {
            throw new IllegalArgumentException(String.format("Number of bits must be 1 to 64, not %d", Integer.valueOf(numBits)));
        }
        if (numBits < 64 && (data >> numBits) != 0) {
            throw new IllegalArgumentException(String.format("Truncating value %d to %d-bit integer", Long.valueOf(data), Integer.valueOf(numBits)));
        }
        int additional = (numBits + 7) / 8;
        if ((numBits & 7) == 0 && !isBytePending()) {
            ensureBufferSize(additional);
            for (int i = numBits - 8; i >= 0; i -= 8) {
                write((byte) (data >> i));
            }
            return;
        }
        ensureBufferSize(additional + 1);
        for (int i2 = numBits - 1; i2 >= 0; i2--) {
            boolean bit = ((data >> i2) & 1) != 0;
            if (bit) {
                this.currentByte = (byte) (this.currentByte | (1 << this.currentBitIndex));
            }
            this.currentBitIndex--;
            if (this.currentBitIndex < 0) {
                writeCurrentByte();
            }
        }
    }

    public void writeLongAt(int position, long data, int numBits) {
        int additional = (numBits + 7) / 8;
        if (position + additional > this.count) {
            ensureBufferSize((position + additional) - this.count);
        }
        int lastCount = this.count;
        int lastBitIndex = this.currentBitIndex;
        byte lastByte = this.currentByte;
        resetCurrentByte();
        this.count = position;
        writeLong(data, numBits);
        if (this.count < lastCount) {
            this.count = lastCount;
            this.currentByte = lastByte;
            this.currentBitIndex = lastBitIndex;
        }
    }

    public void write(int data, int numBits) {
        if (numBits < 0 || numBits > 32) {
            throw new IllegalArgumentException(String.format("Number of bits must be 1 to 32, not %d", Integer.valueOf(numBits)));
        }
        if (numBits < 32 && (data >> numBits) != 0) {
            throw new IllegalArgumentException(String.format("Truncating value %d to %d-bit integer", Integer.valueOf(data), Integer.valueOf(numBits)));
        }
        int additional = (numBits + 7) / 8;
        if ((numBits & 7) == 0 && !isBytePending()) {
            ensureBufferSize(additional);
            for (int i = numBits - 8; i >= 0; i -= 8) {
                write((byte) (data >> i));
            }
            return;
        }
        ensureBufferSize(additional + 1);
        for (int i2 = numBits - 1; i2 >= 0; i2--) {
            boolean bit = ((data >> i2) & 1) != 0;
            if (bit) {
                this.currentByte = (byte) (this.currentByte | (1 << this.currentBitIndex));
            }
            this.currentBitIndex--;
            if (this.currentBitIndex < 0) {
                writeCurrentByte();
            }
        }
    }

    public void writeAt(int position, int data, int numBits) {
        int additional = (numBits + 7) / 8;
        if (position + additional > this.count) {
            ensureBufferSize((position + additional) - this.count);
        }
        int lastCount = this.count;
        int lastBitIndex = this.currentBitIndex;
        byte lastByte = this.currentByte;
        resetCurrentByte();
        this.count = position;
        write(data, numBits);
        if (this.count < lastCount) {
            this.count = lastCount;
            this.currentByte = lastByte;
            this.currentBitIndex = lastBitIndex;
        }
    }

    public void writeBytes(byte[] bytes) {
        if (bytes == null) {
            return;
        }
        writeBytes(bytes, 0, bytes.length);
    }

    public void writeBytes(byte[] bytes, int offset, int length) {
        if (bytes == null || length == 0) {
            return;
        }
        if (isBytePending()) {
            for (int i = 0; i < length; i++) {
                write(bytes[i + offset] & 255, 8);
            }
            return;
        }
        write(bytes, offset, length);
    }

    public void writeByte(byte b) {
        if (isBytePending()) {
            write(b & 255, 8);
        } else {
            ensureBufferSize(1);
            write(b);
        }
    }

    public void writeVarBytes(byte[] bytes, int numBits) {
        int varLengthBits = getVarLengthBits(numBits);
        int nullLengthValue = getNullLengthValue(varLengthBits);
        if (bytes == null) {
            write(nullLengthValue, varLengthBits);
            return;
        }
        if (nullLengthValue == bytes.length) {
            throw new IllegalArgumentException(bytes.length + " bytes is too large for " + numBits + "!");
        }
        if (numBits < varLengthBits && (bytes.length >> numBits) != 0) {
            throw new IllegalArgumentException(String.format("Truncating value %d to %d-bit integer", Integer.valueOf(bytes.length), Integer.valueOf(numBits)));
        }
        write(bytes.length, varLengthBits);
        writeBytes(bytes);
    }

    public void writeVarBytes(Bytes bytes, int numBits) {
        writeVarBytes(bytes == null ? null : bytes.getBytes(), numBits);
    }

    public int space(int numBits) {
        if (numBits % 8 != 0) {
            throw new IllegalArgumentException("Number of bits must be multiple of 8, not " + numBits + "!");
        }
        if (isBytePending()) {
            throw new IllegalStateException("bits are pending!");
        }
        int position = this.count;
        int bytes = numBits / 8;
        ensureBufferSize(bytes);
        this.count += bytes;
        return position;
    }

    public byte[] toByteArray() {
        byte[] byteArray;
        writeCurrentByte();
        if (this.buffer.length == this.count) {
            byteArray = this.buffer;
            this.buffer = Bytes.EMPTY;
            TAKES.incrementAndGet();
        } else {
            byteArray = Arrays.copyOf(this.buffer, this.count);
            if (this.secureClose) {
                Arrays.fill(this.buffer, 0, this.count, (byte) 0);
            }
            COPIES.incrementAndGet();
        }
        this.count = 0;
        return byteArray;
    }

    public void write(DatagramWriter data) {
        data.writeCurrentByte();
        write(data.buffer, 0, data.count);
    }

    public void writeTo(OutputStream out) throws IOException {
        writeCurrentByte();
        out.write(this.buffer, 0, this.count);
        this.count = 0;
    }

    public void writeSize(int position, int numBits) {
        if (numBits % 8 != 0) {
            throw new IllegalArgumentException("Number of bits must be multiple of 8, not " + numBits + "!");
        }
        int size = (this.count - position) - (numBits / 8);
        writeAt(position, size, numBits);
    }

    public int size() {
        return this.count;
    }

    public void reset() {
        if (this.secureClose) {
            Arrays.fill(this.buffer, 0, this.count, (byte) 0);
        }
        this.count = 0;
    }

    public void close() {
        reset();
        this.buffer = Bytes.EMPTY;
    }

    public void writeCurrentByte() {
        if (isBytePending()) {
            ensureBufferSize(1);
            write(this.currentByte);
            resetCurrentByte();
        }
    }

    public final boolean isBytePending() {
        return this.currentBitIndex < 7;
    }

    private final void resetCurrentByte() {
        this.currentByte = (byte) 0;
        this.currentBitIndex = 7;
    }

    private final void write(byte[] b, int offset, int length) {
        if (b != null && length > 0) {
            ensureBufferSize(length);
            System.arraycopy(b, offset, this.buffer, this.count, length);
            this.count += length;
        }
    }

    private final void write(byte b) {
        byte[] bArr = this.buffer;
        int i = this.count;
        this.count = i + 1;
        bArr[i] = b;
    }

    private final void ensureBufferSize(int add) {
        int size = this.count + add;
        if (size > this.buffer.length) {
            int newSize = calculateBufferSize(size);
            setBufferSize(newSize);
        }
    }

    private final void setBufferSize(int size) {
        byte[] newBuffer = new byte[size];
        System.arraycopy(this.buffer, 0, newBuffer, 0, this.count);
        if (this.secureClose) {
            Arrays.fill(this.buffer, 0, this.count, (byte) 0);
        }
        this.buffer = newBuffer;
    }

    private final int calculateBufferSize(int size) {
        int newSize = this.buffer.length;
        if (newSize == 0) {
            newSize = 32;
        }
        if (newSize < size) {
            newSize = size;
        } else if (newSize > MAX_ARRAY_SIZE) {
            newSize = MAX_ARRAY_SIZE;
        }
        return newSize;
    }

    public static int getVarLengthBits(int numBits) {
        if (numBits % 8 != 0) {
            numBits = (numBits & (-8)) + 8;
        }
        return numBits;
    }

    public static int getNullLengthValue(int varLengthBits) {
        switch (varLengthBits) {
            case 8:
                return 255;
            case 16:
                return 65535;
            case 24:
                return 16777215;
            case 32:
                return -1;
            default:
                throw new IllegalArgumentException("Var length Bits must be a multiple of 8, not " + varLengthBits + "!");
        }
    }

    public String toString() {
        byte[] byteArray = Arrays.copyOf(this.buffer, this.count);
        if (byteArray != null && byteArray.length != 0) {
            StringBuilder builder = new StringBuilder(byteArray.length * 3);
            for (int i = 0; i < byteArray.length; i++) {
                builder.append(String.format("%02X", Integer.valueOf(255 & byteArray[i])));
                if (i < byteArray.length - 1) {
                    builder.append(' ');
                }
            }
            return builder.toString();
        }
        return "--";
    }
}
