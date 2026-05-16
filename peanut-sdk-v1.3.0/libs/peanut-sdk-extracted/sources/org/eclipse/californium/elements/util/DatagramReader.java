package org.eclipse.californium.elements.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import org.eclipse.californium.elements.util.DataStreamReader;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-element-connector.jar:org/eclipse/californium/elements/util/DatagramReader.class */
public final class DatagramReader extends DataStreamReader {
    private byte markByte;
    private int markBitIndex;

    public DatagramReader(byte[] byteArray) {
        this(byteArray, true);
    }

    public DatagramReader(byte[] byteArray, boolean copy) {
        this(copy ? Arrays.copyOf(byteArray, byteArray.length) : byteArray, 0, byteArray.length);
    }

    public DatagramReader(byte[] byteArray, int offset, int length) {
        this(new DataStreamReader.RangeInputStream(byteArray, offset, length));
    }

    public DatagramReader(ByteArrayInputStream byteStream) {
        super(byteStream);
        this.markByte = this.currentByte;
        this.markBitIndex = this.currentBitIndex;
    }

    public void mark() {
        this.markByte = this.currentByte;
        this.markBitIndex = this.currentBitIndex;
        this.byteStream.mark(0);
    }

    public void reset() {
        try {
            this.byteStream.reset();
        } catch (IOException e) {
        }
        this.currentByte = this.markByte;
        this.currentBitIndex = this.markBitIndex;
    }

    @Override // org.eclipse.californium.elements.util.DataStreamReader
    public void close() {
        try {
            this.byteStream.skip(this.byteStream.available());
        } catch (IOException e) {
        }
        super.close();
    }

    @Override // org.eclipse.californium.elements.util.DataStreamReader
    public byte[] readBytes(int count) {
        int available = available();
        int bytesToRead = count;
        if (bytesToRead < 0) {
            bytesToRead = available;
        } else if (bytesToRead > available) {
            throw new IllegalArgumentException("requested " + count + " bytes exceeds available " + available + " bytes.");
        }
        return super.readBytes(bytesToRead);
    }

    public byte[] readBytesLeft() {
        return readBytes(-1);
    }

    public void assertFinished(String message) {
        int left = bitsLeft();
        if (left > 0) {
            throw new IllegalArgumentException(message + " not finished! " + left + " bits left.");
        }
    }

    public boolean bytesAvailable() {
        return available() > 0;
    }

    public boolean bytesAvailable(int expectedBytes) {
        int bytesLeft = available();
        return bytesLeft >= expectedBytes;
    }

    public int bitsLeft() {
        return (available() * 8) + this.currentBitIndex + 1;
    }

    private int available() {
        try {
            return this.byteStream.available();
        } catch (IOException e) {
            return -1;
        }
    }
}
