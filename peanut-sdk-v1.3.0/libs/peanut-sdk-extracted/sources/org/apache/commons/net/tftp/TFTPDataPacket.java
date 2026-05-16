package org.apache.commons.net.tftp;

import java.net.DatagramPacket;
import java.net.InetAddress;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/apache-common-net.jar:org/apache/commons/net/tftp/TFTPDataPacket.class */
public final class TFTPDataPacket extends TFTPPacket {
    public static final int MAX_DATA_LENGTH = 32768;
    public static final int MIN_DATA_LENGTH = 512;
    int blockNumber;
    private int length;
    private int offset;
    private byte[] data;

    TFTPDataPacket(DatagramPacket datagram) throws TFTPPacketException {
        super(3, datagram.getAddress(), datagram.getPort());
        this.data = datagram.getData();
        this.offset = 4;
        if (getType() != this.data[1]) {
            throw new TFTPPacketException("TFTP operator code does not match type.");
        }
        this.blockNumber = ((this.data[2] & 255) << 8) | (this.data[3] & 255);
        this.length = datagram.getLength() - 4;
        if (this.length > 32768) {
            this.length = 32768;
        }
    }

    public TFTPDataPacket(InetAddress destination, int port, int blockNumber, byte[] data) {
        this(destination, port, blockNumber, data, 0, data.length);
    }

    public TFTPDataPacket(InetAddress destination, int port, int blockNumber, byte[] data, int offset, int length) {
        super(3, destination, port);
        this.blockNumber = blockNumber;
        this.data = data;
        this.offset = offset;
        if (length > 32768) {
            this.length = 32768;
        } else {
            this.length = length;
        }
    }

    public int getBlockNumber() {
        return this.blockNumber;
    }

    public byte[] getData() {
        return this.data;
    }

    public int getDataLength() {
        return this.length;
    }

    public int getDataOffset() {
        return this.offset;
    }

    @Override // org.apache.commons.net.tftp.TFTPPacket
    public DatagramPacket newDatagram() {
        byte[] data = new byte[this.length + 4];
        data[0] = 0;
        data[1] = (byte) this.type;
        data[2] = (byte) ((this.blockNumber & 65535) >> 8);
        data[3] = (byte) (this.blockNumber & 255);
        System.arraycopy(this.data, this.offset, data, 4, this.length);
        return new DatagramPacket(data, this.length + 4, this.address, this.port);
    }

    @Override // org.apache.commons.net.tftp.TFTPPacket
    DatagramPacket newDatagram(DatagramPacket datagram, byte[] data) {
        data[0] = 0;
        data[1] = (byte) this.type;
        data[2] = (byte) ((this.blockNumber & 65535) >> 8);
        data[3] = (byte) (this.blockNumber & 255);
        if (data != this.data) {
            System.arraycopy(this.data, this.offset, data, 4, this.length);
        }
        datagram.setAddress(this.address);
        datagram.setPort(this.port);
        datagram.setData(data);
        datagram.setLength(this.length + 4);
        return datagram;
    }

    public void setBlockNumber(int blockNumber) {
        this.blockNumber = blockNumber;
    }

    public void setData(byte[] data, int offset, int length) {
        this.data = data;
        this.offset = offset;
        this.length = length;
        if (length > 32768) {
            this.length = 32768;
        } else {
            this.length = length;
        }
    }

    @Override // org.apache.commons.net.tftp.TFTPPacket
    public String toString() {
        return super.toString() + " DATA " + this.blockNumber + " " + this.length;
    }
}
