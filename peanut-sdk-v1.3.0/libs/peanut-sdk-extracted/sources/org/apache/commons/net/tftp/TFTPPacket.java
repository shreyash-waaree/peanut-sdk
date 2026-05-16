package org.apache.commons.net.tftp;

import java.net.DatagramPacket;
import java.net.InetAddress;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/apache-common-net.jar:org/apache/commons/net/tftp/TFTPPacket.class */
public abstract class TFTPPacket {
    static final int MIN_PACKET_SIZE = 4;
    public static final int READ_REQUEST = 1;
    public static final int WRITE_REQUEST = 2;
    public static final int DATA = 3;
    public static final int ACKNOWLEDGEMENT = 4;
    public static final int ERROR = 5;
    public static final int OPTION_ACK = 6;
    public static final int SEGMENT_SIZE = 32768;
    int type;
    int port;
    InetAddress address;

    public abstract DatagramPacket newDatagram();

    abstract DatagramPacket newDatagram(DatagramPacket datagramPacket, byte[] bArr);

    public static final TFTPPacket newTFTPPacket(DatagramPacket datagram) throws TFTPPacketException {
        TFTPPacket packet;
        if (datagram.getLength() < 4) {
            throw new TFTPPacketException("Bad packet. Datagram data length is too short.");
        }
        byte[] data = datagram.getData();
        switch (data[1]) {
            case 1:
                packet = new TFTPReadRequestPacket(datagram);
                break;
            case 2:
                packet = new TFTPWriteRequestPacket(datagram);
                break;
            case 3:
                packet = new TFTPDataPacket(datagram);
                break;
            case 4:
                packet = new TFTPAckPacket(datagram);
                break;
            case 5:
                packet = new TFTPErrorPacket(datagram);
                break;
            default:
                throw new TFTPPacketException("Bad packet.  Invalid TFTP operator code.");
        }
        return packet;
    }

    TFTPPacket(int type, InetAddress address, int port) {
        this.type = type;
        this.address = address;
        this.port = port;
    }

    public final InetAddress getAddress() {
        return this.address;
    }

    public final int getPort() {
        return this.port;
    }

    public final int getType() {
        return this.type;
    }

    public final void setAddress(InetAddress address) {
        this.address = address;
    }

    public final void setPort(int port) {
        this.port = port;
    }

    public String toString() {
        return this.address + " " + this.port + " " + this.type;
    }
}
