package org.eclipse.californium.elements.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-element-connector.jar:org/eclipse/californium/elements/util/DataStreamReader.class */
public class DataStreamReader {
    protected final InputStream byteStream;
    protected byte currentByte;
    protected int currentBitIndex;

    /* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-element-connector.jar:org/eclipse/californium/elements/util/DataStreamReader$RangeInputStream.class */
    protected static class RangeInputStream extends ByteArrayInputStream {
        protected RangeInputStream(byte[] buffer) {
            super(buffer);
        }

        protected RangeInputStream(byte[] buffer, int offset, int length) {
            super(buffer, offset, length);
        }

        /* JADX INFO: Access modifiers changed from: private */
        public RangeInputStream range(int count) {
            int offset = this.pos;
            long available = skip(count);
            if (available < count) {
                throw new IllegalArgumentException("requested " + count + " bytes exceeds available " + available + " bytes.");
            }
            return new RangeInputStream(this.buf, offset, count);
        }
    }

    public DataStreamReader(InputStream byteStream) {
        if (byteStream == null) {
            throw new NullPointerException("byte stream must not be null!");
        }
        this.byteStream = byteStream;
        this.currentByte = (byte) 0;
        this.currentBitIndex = -1;
    }

    public void close() {
        try {
            this.byteStream.close();
        } catch (IOException e) {
        }
        this.currentByte = (byte) 0;
        this.currentBitIndex = -1;
    }

    public long skip(long numBits) {
        int skipped = 0;
        if (this.currentBitIndex >= 0) {
            skipped = this.currentBitIndex + 1;
            numBits -= (long) skipped;
            this.currentBitIndex = -1;
        }
        int left = (int) (numBits & 7);
        long skipBytes = numBits / 8;
        long bytes = skipBytes(skipBytes);
        if (bytes < 0) {
            return skipped;
        }
        if (bytes < skipBytes) {
            left = 0;
        } else {
            try {
                readCurrentByte();
                this.currentBitIndex -= left;
            } catch (IllegalArgumentException e) {
                left = 0;
            }
        }
        return (bytes * 8) + ((long) left) + ((long) skipped);
    }

    public long readLong(int numBits) {
        if (numBits < 0 || numBits > 64) {
            throw new IllegalArgumentException("bits must be in range 0 ... 64!");
        }
        long bits = 0;
        if (this.currentBitIndex < 0 && (numBits & 7) == 0) {
            for (int i = 0; i < numBits; i += 8) {
                bits = (bits << 8) | ((long) readByte());
            }
        } else {
            for (int i2 = numBits - 1; i2 >= 0; i2--) {
                if (this.currentBitIndex < 0) {
                    readCurrentByte();
                }
                boolean bit = ((this.currentByte >> this.currentBitIndex) & 1) != 0;
                if (bit) {
                    bits |= 1 << i2;
                }
                this.currentBitIndex--;
            }
        }
        return bits;
    }

    public int read(int numBits) {
        if (numBits < 0 || numBits > 32) {
            throw new IllegalArgumentException("bits must be in range 0 ... 32!");
        }
        int bits = 0;
        if (this.currentBitIndex < 0 && (numBits & 7) == 0) {
            for (int i = 0; i < numBits; i += 8) {
                bits = (bits << 8) | readByte();
            }
        } else {
            for (int i2 = numBits - 1; i2 >= 0; i2--) {
                if (this.currentBitIndex < 0) {
                    readCurrentByte();
                }
                boolean bit = ((this.currentByte >> this.currentBitIndex) & 1) != 0;
                if (bit) {
                    bits |= 1 << i2;
                }
                this.currentBitIndex--;
            }
        }
        return bits;
    }

    public byte[] readBytes(int count) {
        if (count < 0) {
            throw new IllegalArgumentException("Count " + count + " must not be negative!");
        }
        if (count == 0) {
            return Bytes.EMPTY;
        }
        byte[] bytes = new byte[count];
        if (this.currentBitIndex >= 0) {
            for (int i = 0; i < count; i++) {
                bytes[i] = (byte) read(8);
            }
        } else {
            readBytes(bytes, 0, bytes.length, true);
        }
        return bytes;
    }

    public byte readNextByte() {
        if (this.currentBitIndex >= 0) {
            return (byte) read(8);
        }
        return (byte) readByte();
    }

    public byte[] readVarBytes(int numBits) {
        int varLengthBits = DatagramWriter.getVarLengthBits(numBits);
        int nullLengthValue = DatagramWriter.getNullLengthValue(varLengthBits);
        int len = read(varLengthBits);
        if (len == nullLengthValue) {
            return null;
        }
        return readBytes(len);
    }

    public DatagramReader createRangeReader(int count) {
        return new DatagramReader(createRangeInputStream(count));
    }

    public ByteArrayInputStream createRangeInputStream(int count) {
        if (this.currentBitIndex > 0) {
            throw new IllegalStateException(this.currentBitIndex + " bits unread!");
        }
        if (this.byteStream instanceof RangeInputStream) {
            return ((RangeInputStream) this.byteStream).range(count);
        }
        byte[] range = new byte[count];
        readBytes(range, 0, count, true);
        return new RangeInputStream(range);
    }

    private long skipBytes(long skip) {
        try {
            return this.byteStream.skip(skip);
        } catch (IOException e) {
            return -1L;
        }
    }

    private void readCurrentByte() {
        this.currentByte = (byte) readByte();
        this.currentBitIndex = 7;
    }

    private int readByte() {
        try {
            int val = this.byteStream.read();
            if (val < 0) {
                throw new IllegalArgumentException("requested byte exceeds available bytes!");
            }
            return val;
        } catch (IOException e) {
            throw new IllegalArgumentException("request byte fails!", e);
        }
    }

    private int readBytes(byte[] buffer, int offset, int length, boolean full) {
        int left = length;
        int read = 0;
        while (length > 0) {
            try {
                int available = this.byteStream.read(buffer, offset + read, left);
                if (available <= 0) {
                    break;
                }
                read += available;
                left -= available;
                if (!full) {
                    break;
                }
            } catch (IOException e) {
                throw new IllegalArgumentException("request bytes fails!", e);
            }
        }
        if (read < length) {
            throw new IllegalArgumentException("requested " + length + " bytes exceeds available " + read + " bytes.");
        }
        return read;
    }
}
