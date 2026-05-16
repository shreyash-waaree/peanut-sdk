package org.apache.commons.net.io;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/apache-common-net.jar:org/apache/commons/net/io/ToNetASCIIInputStream.class */
public final class ToNetASCIIInputStream extends FilterInputStream {
    private static final int NOTHING_SPECIAL = 0;
    private static final int LAST_WAS_CR = 1;
    private static final int LAST_WAS_NL = 2;
    private int status;

    public ToNetASCIIInputStream(InputStream input) {
        super(input);
        this.status = 0;
    }

    @Override // java.io.FilterInputStream, java.io.InputStream
    public int available() throws IOException {
        int result = this.in.available();
        if (this.status == 2) {
            return result + 1;
        }
        return result;
    }

    @Override // java.io.FilterInputStream, java.io.InputStream
    public boolean markSupported() {
        return false;
    }

    @Override // java.io.FilterInputStream, java.io.InputStream
    public int read() throws IOException {
        if (this.status == 2) {
            this.status = 0;
            return 10;
        }
        int ch = this.in.read();
        switch (ch) {
            case 10:
                if (this.status != 1) {
                    this.status = 2;
                    return 13;
                }
                break;
            case 13:
                this.status = 1;
                return 13;
        }
        this.status = 0;
        return ch;
    }

    @Override // java.io.FilterInputStream, java.io.InputStream
    public int read(byte[] buffer) throws IOException {
        return read(buffer, 0, buffer.length);
    }

    @Override // java.io.FilterInputStream, java.io.InputStream
    public int read(byte[] buffer, int offset, int length) throws IOException {
        int i;
        if (length < 1) {
            return 0;
        }
        int ch = available();
        if (length > ch) {
            length = ch;
        }
        if (length < 1) {
            length = 1;
        }
        int i2 = read();
        int ch2 = i2;
        if (i2 == -1) {
            return -1;
        }
        do {
            int i3 = offset;
            offset++;
            buffer[i3] = (byte) ch2;
            length--;
            if (length <= 0) {
                break;
            }
            i = read();
            ch2 = i;
        } while (i != -1);
        return offset - offset;
    }
}
