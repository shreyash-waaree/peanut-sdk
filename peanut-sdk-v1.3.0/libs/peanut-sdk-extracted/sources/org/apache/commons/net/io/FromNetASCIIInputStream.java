package org.apache.commons.net.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.nio.charset.StandardCharsets;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/apache-common-net.jar:org/apache/commons/net/io/FromNetASCIIInputStream.class */
public final class FromNetASCIIInputStream extends PushbackInputStream {
    private int length;
    static final String _lineSeparator = System.getProperty("line.separator");
    static final boolean _noConversionRequired = _lineSeparator.equals("\r\n");
    static final byte[] _lineSeparatorBytes = _lineSeparator.getBytes(StandardCharsets.US_ASCII);

    public static boolean isConversionRequired() {
        return !_noConversionRequired;
    }

    public FromNetASCIIInputStream(InputStream input) {
        super(input, _lineSeparatorBytes.length + 1);
    }

    @Override // java.io.PushbackInputStream, java.io.FilterInputStream, java.io.InputStream
    public int available() throws IOException {
        if (this.in == null) {
            throw new IOException("Stream closed");
        }
        return (this.buf.length - this.pos) + this.in.available();
    }

    @Override // java.io.PushbackInputStream, java.io.FilterInputStream, java.io.InputStream
    public int read() throws IOException {
        if (_noConversionRequired) {
            return super.read();
        }
        return readInt();
    }

    @Override // java.io.FilterInputStream, java.io.InputStream
    public int read(byte[] buffer) throws IOException {
        return read(buffer, 0, buffer.length);
    }

    @Override // java.io.PushbackInputStream, java.io.FilterInputStream, java.io.InputStream
    public int read(byte[] buffer, int offset, int length) throws IOException {
        int i;
        if (_noConversionRequired) {
            return super.read(buffer, offset, length);
        }
        if (length < 1) {
            return 0;
        }
        int ch = available();
        this.length = length > ch ? ch : length;
        if (this.length < 1) {
            this.length = 1;
        }
        int i2 = readInt();
        int ch2 = i2;
        if (i2 == -1) {
            return -1;
        }
        do {
            int i3 = offset;
            offset++;
            buffer[i3] = (byte) ch2;
            int i4 = this.length - 1;
            this.length = i4;
            if (i4 <= 0) {
                break;
            }
            i = readInt();
            ch2 = i;
        } while (i != -1);
        return offset - offset;
    }

    private int readInt() throws IOException {
        int ch = super.read();
        if (ch == 13) {
            int ch2 = super.read();
            if (ch2 != 10) {
                if (ch2 != -1) {
                    unread(ch2);
                    return 13;
                }
                return 13;
            }
            unread(_lineSeparatorBytes);
            ch = super.read();
            this.length--;
        }
        return ch;
    }
}
