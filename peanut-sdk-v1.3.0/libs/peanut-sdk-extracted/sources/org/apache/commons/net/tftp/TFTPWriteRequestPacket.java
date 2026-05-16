package org.apache.commons.net.tftp;

import java.net.DatagramPacket;
import java.net.InetAddress;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/apache-common-net.jar:org/apache/commons/net/tftp/TFTPWriteRequestPacket.class */
public final class TFTPWriteRequestPacket extends TFTPRequestPacket {
    TFTPWriteRequestPacket(DatagramPacket datagram) throws TFTPPacketException {
        super(2, datagram);
    }

    public TFTPWriteRequestPacket(InetAddress destination, int port, String fileName, int mode) {
        super(destination, port, 2, fileName, mode);
    }

    @Override // org.apache.commons.net.tftp.TFTPPacket
    public String toString() {
        return super.toString() + " WRQ " + getFilename() + " " + TFTP.getModeName(getMode());
    }
}
