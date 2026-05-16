package org.apache.commons.net.tftp;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/apache-common-net.jar:org/apache/commons/net/tftp/TFTPOptionAckPacket.class */
public class TFTPOptionAckPacket extends TFTPPacket {
    public static final String OPTION_BLOCK_SIZE = "blksize";
    public static final String OPTION_TIMEOUT = "timeout";
    public static final String OPTION_TRANSFER_SIZE = "tsize";
    private Integer blockSize;
    private Integer timeout;
    private Long transferSize;

    TFTPOptionAckPacket(InetAddress address, int port, Integer blockSize, Integer timeout, Long transferSize) {
        super(6, address, port);
        this.blockSize = blockSize;
        this.timeout = timeout;
        this.transferSize = transferSize;
    }

    public Integer getBlockSize() {
        return this.blockSize;
    }

    public Integer getTimeout() {
        return this.timeout;
    }

    public Long getTransferSize() {
        return this.transferSize;
    }

    @Override // org.apache.commons.net.tftp.TFTPPacket
    public DatagramPacket newDatagram() {
        int totalLength = 2;
        int blkSizeLength = 0;
        int timeoutLength = 0;
        int tSizeLength = 0;
        byte[] blkSizeData = new byte[0];
        byte[] timeoutData = new byte[0];
        byte[] tSizeData = new byte[0];
        int optionBlkSizeLength = OPTION_BLOCK_SIZE.length();
        int optionTimeoutLength = OPTION_TIMEOUT.length();
        int optionTSizeLength = OPTION_TRANSFER_SIZE.length();
        if (this.blockSize != null) {
            blkSizeData = String.valueOf(this.blockSize).getBytes(StandardCharsets.US_ASCII);
            blkSizeLength = blkSizeData.length;
            totalLength = 2 + optionBlkSizeLength + blkSizeLength + 2;
        }
        if (this.timeout != null) {
            timeoutData = String.valueOf(this.timeout).getBytes(StandardCharsets.US_ASCII);
            timeoutLength = timeoutData.length;
            totalLength += optionTimeoutLength + timeoutLength + 2;
        }
        if (this.transferSize != null) {
            tSizeData = String.valueOf(this.transferSize).getBytes(StandardCharsets.US_ASCII);
            tSizeLength = tSizeData.length;
            totalLength += optionTSizeLength + tSizeLength + 2;
        }
        byte[] data = new byte[totalLength];
        data[0] = 0;
        data[1] = (byte) this.type;
        int pos = 2;
        if (this.blockSize != null) {
            System.arraycopy(OPTION_BLOCK_SIZE.getBytes(), 0, data, 2, optionBlkSizeLength);
            data[optionBlkSizeLength + 2] = 0;
            int pos2 = 2 + optionBlkSizeLength + 1;
            System.arraycopy(blkSizeData, 0, data, pos2, blkSizeLength);
            data[blkSizeLength + pos2] = 0;
            pos = pos2 + blkSizeLength + 1;
        }
        if (this.timeout != null) {
            System.arraycopy(OPTION_TIMEOUT.getBytes(), 0, data, pos, optionTimeoutLength);
            data[optionTimeoutLength + pos] = 0;
            int pos3 = pos + optionTimeoutLength + 1;
            System.arraycopy(timeoutData, 0, data, pos3, timeoutLength);
            data[timeoutLength + pos3] = 0;
            pos = pos3 + timeoutLength + 1;
        }
        if (this.transferSize != null) {
            System.arraycopy(OPTION_TRANSFER_SIZE.getBytes(), 0, data, pos, optionTSizeLength);
            data[optionTSizeLength + pos] = 0;
            int pos4 = pos + optionTSizeLength + 1;
            System.arraycopy(tSizeData, 0, data, pos4, tSizeLength);
            data[tSizeLength + pos4] = 0;
            int i = pos4 + tSizeLength + 1;
        }
        return new DatagramPacket(data, data.length, this.address, this.port);
    }

    @Override // org.apache.commons.net.tftp.TFTPPacket
    DatagramPacket newDatagram(DatagramPacket datagram, byte[] data) {
        int blkSizeLength = 0;
        int timeoutLength = 0;
        int tSizeLength = 0;
        byte[] blkSizeData = new byte[0];
        byte[] timeoutData = new byte[0];
        byte[] tSizeData = new byte[0];
        int optionBlkSizeLength = OPTION_BLOCK_SIZE.length();
        int optionTimeoutLength = OPTION_TIMEOUT.length();
        int optionTSizeLength = OPTION_TRANSFER_SIZE.length();
        if (this.blockSize != null) {
            blkSizeData = String.valueOf(this.blockSize).getBytes(StandardCharsets.US_ASCII);
            blkSizeLength = blkSizeData.length;
        }
        if (this.timeout != null) {
            timeoutData = String.valueOf(this.timeout).getBytes(StandardCharsets.US_ASCII);
            timeoutLength = timeoutData.length;
        }
        if (this.transferSize != null) {
            tSizeData = String.valueOf(this.transferSize).getBytes(StandardCharsets.US_ASCII);
            tSizeLength = tSizeData.length;
        }
        data[0] = 0;
        data[1] = (byte) this.type;
        int pos = 2;
        if (this.blockSize != null) {
            System.arraycopy(OPTION_BLOCK_SIZE.getBytes(), 0, data, 2, optionBlkSizeLength);
            data[optionBlkSizeLength + 2] = 0;
            int pos2 = 2 + optionBlkSizeLength + 1;
            System.arraycopy(blkSizeData, 0, data, pos2, blkSizeLength);
            data[blkSizeLength + pos2] = 0;
            pos = pos2 + blkSizeLength + 1;
        }
        if (this.timeout != null) {
            System.arraycopy(OPTION_TIMEOUT.getBytes(), 0, data, pos, optionTimeoutLength);
            data[optionTimeoutLength + pos] = 0;
            int pos3 = pos + optionTimeoutLength + 1;
            System.arraycopy(timeoutData, 0, data, pos3, timeoutLength);
            data[timeoutLength + pos3] = 0;
            pos = pos3 + timeoutLength + 1;
        }
        if (this.transferSize != null) {
            System.arraycopy(OPTION_TRANSFER_SIZE.getBytes(), 0, data, pos, optionTSizeLength);
            data[optionTSizeLength + pos] = 0;
            int pos4 = pos + optionTSizeLength + 1;
            System.arraycopy(tSizeData, 0, data, pos4, tSizeLength);
            data[tSizeLength + pos4] = 0;
            pos = pos4 + tSizeLength + 1;
        }
        datagram.setAddress(this.address);
        datagram.setPort(this.port);
        datagram.setData(data);
        datagram.setLength(pos);
        return datagram;
    }
}
