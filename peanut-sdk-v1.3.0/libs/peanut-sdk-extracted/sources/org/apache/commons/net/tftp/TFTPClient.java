package org.apache.commons.net.tftp;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.SocketException;
import org.apache.commons.net.io.FromNetASCIIOutputStream;
import org.apache.commons.net.io.ToNetASCIIInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/apache-common-net.jar:org/apache/commons/net/tftp/TFTPClient.class */
public class TFTPClient extends TFTP {
    private static final Logger LOGGER = LoggerFactory.getLogger(TFTPClient.class);
    public static final int DEFAULT_MAX_TIMEOUTS = 50;
    private int maxTimeouts = 50;
    private long totalBytesReceived;
    private long totalBytesSent;

    public int getMaxTimeouts() {
        return this.maxTimeouts;
    }

    public long getTotalBytesReceived() {
        return this.totalBytesReceived;
    }

    public long getTotalBytesSent() {
        return this.totalBytesSent;
    }

    public int receiveFile(String fileName, int mode, OutputStream output, InetAddress host) throws IOException {
        return receiveFile(fileName, mode, output, host, 69);
    }

    public int receiveFile(String fileName, int mode, OutputStream output, InetAddress host, int port) throws IOException {
        int bytesRead = 0;
        int lastBlock = 0;
        int block = 1;
        int hostPort = 0;
        int dataLength = 0;
        this.totalBytesReceived = 0L;
        if (mode == 0) {
            output = new FromNetASCIIOutputStream(output);
        }
        TFTPPacket sent = new TFTPReadRequestPacket(host, port, fileName, mode);
        TFTPAckPacket ack = new TFTPAckPacket(host, port, 0);
        beginBufferedOps();
        boolean justStarted = true;
        do {
            try {
                bufferedSend(sent);
                boolean wantReply = true;
                int timeouts = 0;
                do {
                    try {
                        TFTPPacket received = bufferedReceive();
                        int recdPort = received.getPort();
                        InetAddress recdAddress = received.getAddress();
                        log("鍦板潃 " + recdAddress.getHostAddress() + "绔\ue21a彛 " + recdPort);
                        if (justStarted) {
                            justStarted = false;
                            if (recdPort == port) {
                                bufferedSend(new TFTPErrorPacket(recdAddress, recdPort, 5, "INCORRECT SOURCE PORT"));
                                throw new IOException("Incorrect source port (" + recdPort + ") in request reply.");
                            }
                            hostPort = recdPort;
                            ack.setPort(hostPort);
                            if (!host.equals(recdAddress)) {
                                host = recdAddress;
                                ack.setAddress(host);
                                sent.setAddress(host);
                            }
                        }
                        if (host.equals(recdAddress) && recdPort == hostPort) {
                            LOGGER.info("receivedType" + received.getType());
                            switch (received.getType()) {
                                case 3:
                                    TFTPDataPacket data = (TFTPDataPacket) received;
                                    dataLength = data.getDataLength();
                                    lastBlock = data.getBlockNumber();
                                    log("block" + block + "lastBlock" + lastBlock + "dataLength" + dataLength);
                                    LOGGER.info("block" + block);
                                    if (lastBlock == block) {
                                        try {
                                            output.write(data.getData(), data.getDataOffset(), dataLength);
                                            block++;
                                            if (block > 65535) {
                                                block = 0;
                                            }
                                            wantReply = false;
                                        } catch (IOException e) {
                                            bufferedSend(new TFTPErrorPacket(host, hostPort, 3, "File write failed."));
                                            throw e;
                                        }
                                    } else {
                                        discardPackets();
                                        if (lastBlock == (block == 0 ? 65535 : block - 1)) {
                                            wantReply = false;
                                        }
                                    }
                                    break;
                                case 5:
                                    TFTPErrorPacket error = (TFTPErrorPacket) received;
                                    throw new IOException("Error code " + error.getError() + " received: " + error.getMessage());
                                default:
                                    throw new IOException("Received unexpected packet type (" + received.getType() + ")");
                            }
                        } else {
                            bufferedSend(new TFTPErrorPacket(recdAddress, recdPort, 5, "Unexpected host or port."));
                        }
                    } catch (InterruptedIOException | SocketException e2) {
                        timeouts++;
                        if (timeouts >= this.maxTimeouts) {
                            throw new IOException("Connection timed out.");
                        }
                    } catch (TFTPPacketException e3) {
                        throw new IOException("Bad packet: " + e3.getMessage());
                    }
                    log("wantReply" + wantReply);
                } while (wantReply);
                ack.setBlockNumber(lastBlock);
                sent = ack;
                bytesRead += dataLength;
                this.totalBytesReceived += (long) dataLength;
                log("dataLength" + dataLength);
            } catch (Throwable th) {
                endBufferedOps();
                throw th;
            }
        } while (dataLength == 32768);
        log("sent" + sent);
        bufferedSend(sent);
        endBufferedOps();
        return bytesRead;
    }

    public int receiveFile(String fileName, int mode, OutputStream output, String hostname) throws IOException {
        return receiveFile(fileName, mode, output, InetAddress.getByName(hostname), 69);
    }

    public int receiveFile(String fileName, int mode, OutputStream output, String hostname, int port) throws IOException {
        return receiveFile(fileName, mode, output, InetAddress.getByName(hostname), port);
    }

    public void sendFile(String fileName, int mode, InputStream input, InetAddress host) throws IOException {
        sendFile(fileName, mode, input, host, 69);
    }

    public void sendFile(String fileName, int mode, InputStream input, InetAddress host, int port) throws IOException {
        int bytesRead;
        int block = 0;
        int hostPort = 0;
        boolean justStarted = true;
        boolean lastAckWait = false;
        this.totalBytesSent = 0L;
        if (mode == 0) {
            input = new ToNetASCIIInputStream(input);
        }
        TFTPPacket sent = new TFTPWriteRequestPacket(host, port, fileName, mode);
        TFTPDataPacket data = new TFTPDataPacket(host, port, 0, this.sendBuffer, 4, 0);
        beginBufferedOps();
        while (true) {
            try {
                bufferedSend(sent);
                boolean wantReply = true;
                int timeouts = 0;
                do {
                    try {
                        try {
                            TFTPPacket received = bufferedReceive();
                            InetAddress recdAddress = received.getAddress();
                            int recdPort = received.getPort();
                            if (justStarted) {
                                justStarted = false;
                                if (recdPort == port) {
                                    bufferedSend(new TFTPErrorPacket(recdAddress, recdPort, 5, "INCORRECT SOURCE PORT"));
                                    throw new IOException("Incorrect source port (" + recdPort + ") in request reply.");
                                }
                                hostPort = recdPort;
                                data.setPort(hostPort);
                                if (!host.equals(recdAddress)) {
                                    host = recdAddress;
                                    data.setAddress(host);
                                    sent.setAddress(host);
                                }
                            }
                            if (host.equals(recdAddress) && recdPort == hostPort) {
                                switch (received.getType()) {
                                    case 4:
                                        int lastBlock = ((TFTPAckPacket) received).getBlockNumber();
                                        if (lastBlock == block) {
                                            block++;
                                            if (block > 65535) {
                                                block = 0;
                                            }
                                            wantReply = false;
                                        } else {
                                            discardPackets();
                                        }
                                        break;
                                    case 5:
                                        TFTPErrorPacket error = (TFTPErrorPacket) received;
                                        throw new IOException("Error code " + error.getError() + " received: " + error.getMessage());
                                    default:
                                        throw new IOException("Received unexpected packet type.");
                                }
                            } else {
                                bufferedSend(new TFTPErrorPacket(recdAddress, recdPort, 5, "Unexpected host or port."));
                            }
                        } catch (InterruptedIOException | SocketException e) {
                            timeouts++;
                            if (timeouts >= this.maxTimeouts) {
                                throw new IOException("Connection timed out.");
                            }
                        }
                    } catch (TFTPPacketException e2) {
                        throw new IOException("Bad packet: " + e2.getMessage());
                    }
                } while (wantReply);
                if (!lastAckWait) {
                    int dataLength = 32768;
                    int offset = 4;
                    int totalThisPacket = 0;
                    while (dataLength > 0 && (bytesRead = input.read(this.sendBuffer, offset, dataLength)) > 0) {
                        offset += bytesRead;
                        dataLength -= bytesRead;
                        totalThisPacket += bytesRead;
                    }
                    if (totalThisPacket < 32768) {
                        lastAckWait = true;
                    }
                    data.setBlockNumber(block);
                    data.setData(this.sendBuffer, 4, totalThisPacket);
                    sent = data;
                    this.totalBytesSent += (long) totalThisPacket;
                } else {
                    return;
                }
            } finally {
                endBufferedOps();
            }
        }
    }

    public void sendFile(String fileName, int mode, InputStream input, String hostname) throws IOException {
        sendFile(fileName, mode, input, InetAddress.getByName(hostname), 69);
    }

    public void sendFile(String fileName, int mode, InputStream input, String hostname, int port) throws IOException {
        sendFile(fileName, mode, input, InetAddress.getByName(hostname), port);
    }

    public void setMaxTimeouts(int numTimeouts) {
        if (numTimeouts < 1) {
            this.maxTimeouts = 1;
        } else {
            this.maxTimeouts = numTimeouts;
        }
    }

    private void log(String msg) {
    }
}
