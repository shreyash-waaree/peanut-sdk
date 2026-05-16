package org.apache.commons.net.tftp;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.DatagramPacket;
import java.net.SocketException;
import org.apache.commons.net.DatagramSocketClient;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/apache-common-net.jar:org/apache/commons/net/tftp/TFTP.class */
public class TFTP extends DatagramSocketClient {
    public static final int ASCII_MODE = 0;
    public static final int NETASCII_MODE = 0;
    public static final int BINARY_MODE = 1;
    public static final int IMAGE_MODE = 1;
    public static final int OCTET_MODE = 1;
    public static final int DEFAULT_TIMEOUT = 300;
    public static final int DEFAULT_PORT = 69;
    static final int PACKET_SIZE = 32772;
    private byte[] receiveBuffer;
    private DatagramPacket receiveDatagram;
    private DatagramPacket sendDatagram;
    byte[] sendBuffer;

    public static final String getModeName(int mode) {
        return TFTPRequestPacket.modeStrings[mode];
    }

    public TFTP() {
        setDefaultTimeout(300);
        this.receiveBuffer = null;
        this.receiveDatagram = null;
    }

    public final void beginBufferedOps() {
        this.receiveBuffer = new byte[PACKET_SIZE];
        this.receiveDatagram = new DatagramPacket(this.receiveBuffer, this.receiveBuffer.length);
        this.sendBuffer = new byte[PACKET_SIZE];
        this.sendDatagram = new DatagramPacket(this.sendBuffer, this.sendBuffer.length);
    }

    public final TFTPPacket bufferedReceive() throws TFTPPacketException, IOException {
        this.receiveDatagram.setData(this.receiveBuffer);
        this.receiveDatagram.setLength(this.receiveBuffer.length);
        this._socket_.receive(this.receiveDatagram);
        TFTPPacket newTFTPPacket = TFTPPacket.newTFTPPacket(this.receiveDatagram);
        trace("<", newTFTPPacket);
        return newTFTPPacket;
    }

    public final void bufferedSend(TFTPPacket packet) throws IOException {
        trace(">", packet);
        this._socket_.send(packet.newDatagram(this.sendDatagram, this.sendBuffer));
    }

    public final void discardPackets() throws IOException {
        DatagramPacket datagram = new DatagramPacket(new byte[PACKET_SIZE], PACKET_SIZE);
        int to = getSoTimeout();
        setSoTimeout(1);
        while (true) {
            try {
                this._socket_.receive(datagram);
            } catch (InterruptedIOException | SocketException e) {
                setSoTimeout(to);
                return;
            }
        }
    }

    public final void endBufferedOps() {
        this.receiveBuffer = null;
        this.receiveDatagram = null;
        this.sendBuffer = null;
        this.sendDatagram = null;
    }

    public final TFTPPacket receive() throws TFTPPacketException, IOException {
        DatagramPacket packet = new DatagramPacket(new byte[PACKET_SIZE], PACKET_SIZE);
        this._socket_.receive(packet);
        TFTPPacket newTFTPPacket = TFTPPacket.newTFTPPacket(packet);
        trace("<", newTFTPPacket);
        return newTFTPPacket;
    }

    public final void send(TFTPPacket packet) throws IOException {
        trace(">", packet);
        this._socket_.send(packet.newDatagram());
    }

    protected void trace(String direction, TFTPPacket packet) {
    }
}
