package com.keenon.sdk.scmIot.protopack.base;

import androidx.annotation.NonNull;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/scmIot/protopack/base/ProtoHeader.class */
public class ProtoHeader implements Header {
    public static final int HEADER_SIZE = 8;
    private int seq;
    private int dev;
    private int topic;
    private int type;
    private int cmd;
    private int status;
    private int dataLength;

    public ProtoHeader() {
        setSeq(0);
        setStatus(0);
    }

    public int getSeq() {
        return this.seq;
    }

    public void setSeq(int seq) {
        this.seq = seq;
    }

    public int getDev() {
        return this.dev;
    }

    public void setDev(int dev) {
        this.dev = dev;
    }

    public int getTopic() {
        return this.topic;
    }

    public void setTopic(int topic) {
        this.topic = topic;
    }

    public int getType() {
        return this.type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public int getCmd() {
        return this.cmd;
    }

    public void setCmd(int cmd) {
        this.cmd = cmd;
    }

    public int getStatus() {
        return this.status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public int getDataLength() {
        return this.dataLength;
    }

    public void setDataLength(int dataLength) {
        this.dataLength = dataLength;
    }

    @Override // com.keenon.sdk.scmIot.protopack.base.Marshallable
    public void marshal(Pack pack) {
        ByteBuffer buffer = ByteBuffer.allocate(8);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.put((byte) this.seq);
        buffer.put((byte) this.dev);
        buffer.put((byte) this.topic);
        buffer.put((byte) this.type);
        buffer.put((byte) this.cmd);
        buffer.put((byte) this.status);
        buffer.putShort((short) this.dataLength);
        pack.putBuffer(buffer);
    }

    @Override // com.keenon.sdk.scmIot.protopack.base.Marshallable
    public void unmarshal(Unpack unpack) {
        this.seq = unpack.popRawByte().byteValue();
        this.dev = unpack.popRawByte().byteValue();
        this.topic = unpack.popRawByte().byteValue();
        this.type = unpack.popRawByte().byteValue();
        this.cmd = unpack.popRawByte().byteValue();
        this.status = unpack.popRawByte().byteValue();
        this.dataLength = unpack.popRwaShort().shortValue();
    }

    @Override // com.keenon.sdk.scmIot.protopack.base.Marshallable
    public Object getData() {
        return null;
    }

    @NonNull
    public String toString() {
        return "header: seq = " + this.seq + ", dev = " + this.dev + ", topic = " + this.topic + ", tye = " + this.type + ", cmd = " + this.cmd + ", status = " + this.status + ", length = " + this.dataLength;
    }
}
