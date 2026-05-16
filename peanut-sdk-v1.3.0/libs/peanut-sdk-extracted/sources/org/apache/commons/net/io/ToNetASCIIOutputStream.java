package org.apache.commons.net.io;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/apache-common-net.jar:org/apache/commons/net/io/ToNetASCIIOutputStream.class */
public final class ToNetASCIIOutputStream extends FilterOutputStream {
    private boolean lastWasCR;

    public ToNetASCIIOutputStream(OutputStream output) {
        super(output);
        this.lastWasCR = false;
    }

    @Override // java.io.FilterOutputStream, java.io.OutputStream
    public synchronized void write(byte[] buffer) throws IOException {
        write(buffer, 0, buffer.length);
    }

    @Override // java.io.FilterOutputStream, java.io.OutputStream
    public synchronized void write(byte[] buffer, int offset, int length) throws IOException {
        while (true) {
            int i = length;
            length--;
            if (i > 0) {
                int i2 = offset;
                offset++;
                write(buffer[i2]);
            } else {
                return;
            }
        }
    }

    @Override // java.io.FilterOutputStream, java.io.OutputStream
    public synchronized void write(int ch) throws IOException {
        switch (ch) {
            case 10:
                if (!this.lastWasCR) {
                    this.out.write(13);
                }
                break;
            case 13:
                this.lastWasCR = true;
                this.out.write(13);
                return;
        }
        this.lastWasCR = false;
        this.out.write(ch);
    }
}
