package org.apache.commons.net.io;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/apache-common-net.jar:org/apache/commons/net/io/FromNetASCIIOutputStream.class */
public final class FromNetASCIIOutputStream extends FilterOutputStream {
    private boolean lastWasCR;

    public FromNetASCIIOutputStream(OutputStream output) {
        super(output);
        this.lastWasCR = false;
    }

    @Override // java.io.FilterOutputStream, java.io.OutputStream, java.io.Closeable, java.lang.AutoCloseable
    public synchronized void close() throws IOException {
        if (FromNetASCIIInputStream._noConversionRequired) {
            super.close();
            return;
        }
        if (this.lastWasCR) {
            this.out.write(13);
        }
        super.close();
    }

    @Override // java.io.FilterOutputStream, java.io.OutputStream
    public synchronized void write(byte[] buffer) throws IOException {
        write(buffer, 0, buffer.length);
    }

    @Override // java.io.FilterOutputStream, java.io.OutputStream
    public synchronized void write(byte[] buffer, int offset, int length) throws IOException {
        if (FromNetASCIIInputStream._noConversionRequired) {
            this.out.write(buffer, offset, length);
            return;
        }
        while (true) {
            int i = length;
            length--;
            if (i > 0) {
                int i2 = offset;
                offset++;
                writeInt(buffer[i2]);
            } else {
                return;
            }
        }
    }

    @Override // java.io.FilterOutputStream, java.io.OutputStream
    public synchronized void write(int ch) throws IOException {
        if (FromNetASCIIInputStream._noConversionRequired) {
            this.out.write(ch);
        } else {
            writeInt(ch);
        }
    }

    private void writeInt(int ch) throws IOException {
        switch (ch) {
            case 10:
                if (this.lastWasCR) {
                    this.out.write(FromNetASCIIInputStream._lineSeparatorBytes);
                    this.lastWasCR = false;
                } else {
                    this.lastWasCR = false;
                    this.out.write(10);
                }
                break;
            case 13:
                this.lastWasCR = true;
                break;
            default:
                if (this.lastWasCR) {
                    this.out.write(13);
                    this.lastWasCR = false;
                }
                this.out.write(ch);
                break;
        }
    }
}
