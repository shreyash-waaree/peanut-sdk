package org.apache.commons.net.tftp;

import java.net.DatagramPacket;
import java.net.InetAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/apache-common-net.jar:org/apache/commons/net/tftp/TFTPRequestPacket.class */
public abstract class TFTPRequestPacket extends TFTPPacket {
    private static final Logger LOGGER = LoggerFactory.getLogger(TFTPRequestPacket.class);
    static final String[] modeStrings = {"netascii", "octet"};
    private static final byte[][] modeBytes = {new byte[]{110, 101, 116, 97, 115, 99, 105, 105, 0}, new byte[]{111, 99, 116, 101, 116, 0}};
    private final int mode;
    private final String fileName;
    protected Integer blockSize;
    protected Integer timeout;
    protected Long transferSize;

    TFTPRequestPacket(InetAddress destination, int port, int type, String fileName, int mode) {
        super(type, destination, port);
        this.blockSize = null;
        this.timeout = null;
        this.transferSize = null;
        this.fileName = fileName;
        this.mode = mode;
    }

    TFTPRequestPacket(int type, DatagramPacket datagram) throws TFTPPacketException {
        super(type, datagram.getAddress(), datagram.getPort());
        this.blockSize = null;
        this.timeout = null;
        this.transferSize = null;
        byte[] data = datagram.getData();
        if (getType() != data[1]) {
            throw new TFTPPacketException("TFTP operator code does not match type.");
        }
        LOGGER.info("{}", Integer.valueOf(datagram.getLength()));
        String str = new String(data, 2, datagram.getLength());
        String[] strArray = str.split("\u0000");
        this.fileName = strArray[0];
        String modeString = strArray[1];
        int modeLength = modeStrings.length;
        int mode = 0;
        int i = 0;
        while (true) {
            if (i >= modeLength) {
                break;
            }
            if (!modeString.equals(modeStrings[i])) {
                i++;
            } else {
                mode = i;
                break;
            }
        }
        this.mode = mode;
        for (int i2 = 2; i2 < strArray.length; i2++) {
            switch (strArray[i2]) {
                case "blksize":
                    this.blockSize = Integer.valueOf(Integer.parseInt(strArray[i2 + 1]));
                    break;
                case "timeout":
                    this.timeout = Integer.valueOf(Integer.parseInt(strArray[i2 + 1]));
                    break;
                case "tsize":
                    this.transferSize = Long.valueOf(Long.parseLong(strArray[i2 + 1]));
                    break;
            }
        }
        LOGGER.info("璇昏\ue1ec姹�, 鏂囦欢鍚嶏細{} , 妯″紡锛歿}, 鍧楀ぇ灏忥細{}B, 瓒呮椂{}, 鎬诲ぇ灏弡}B.", new Object[]{this.fileName, modeStrings[this.mode], this.blockSize, this.timeout, this.transferSize});
    }

    public boolean isNegotiate() {
        return (this.blockSize == null && this.timeout == null && this.transferSize == null) ? false : true;
    }

    public final String getFilename() {
        return this.fileName;
    }

    public final int getMode() {
        return this.mode;
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
    public final DatagramPacket newDatagram() {
        int fileLength = this.fileName.length();
        int modeLength = modeBytes[this.mode].length;
        byte[] data = new byte[fileLength + modeLength + 4];
        data[0] = 0;
        data[1] = (byte) this.type;
        System.arraycopy(this.fileName.getBytes(), 0, data, 2, fileLength);
        data[fileLength + 2] = 0;
        System.arraycopy(modeBytes[this.mode], 0, data, fileLength + 3, modeLength);
        return new DatagramPacket(data, data.length, this.address, this.port);
    }

    @Override // org.apache.commons.net.tftp.TFTPPacket
    final DatagramPacket newDatagram(DatagramPacket datagram, byte[] data) {
        int fileLength = this.fileName.length();
        int modeLength = modeBytes[this.mode].length;
        data[0] = 0;
        data[1] = (byte) this.type;
        System.arraycopy(this.fileName.getBytes(), 0, data, 2, fileLength);
        data[fileLength + 2] = 0;
        System.arraycopy(modeBytes[this.mode], 0, data, fileLength + 3, modeLength);
        datagram.setAddress(this.address);
        datagram.setPort(this.port);
        datagram.setData(data);
        datagram.setLength(fileLength + modeLength + 3);
        return datagram;
    }
}
