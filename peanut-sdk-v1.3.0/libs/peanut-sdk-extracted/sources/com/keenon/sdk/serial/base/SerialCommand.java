package com.keenon.sdk.serial.base;

import com.keenon.common.utils.ArrayUtils;
import com.keenon.common.utils.ByteUtils;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/serial/base/SerialCommand.class */
public class SerialCommand {
    private static final int HEADER_SIZE = 6;
    private static final int MAX_DATA_SIZE = 65535;
    private Byte[] sequence = {(byte) 0};
    private Byte[] commandAction = {(byte) 0};
    private Byte[] commandBody = {(byte) 0};
    private Byte[] status = {(byte) 0};
    private Byte[] dataLength = {(byte) 0, (byte) 0};
    private Byte[] data;
    private String commandId;

    private SerialCommand() {
    }

    public static SerialCommand create(Byte[] action, Byte[] body) {
        SerialCommand serialCommand = new SerialCommand();
        serialCommand.setCommandAction(action);
        serialCommand.setCommandBody(body);
        serialCommand.setCommandId();
        return serialCommand;
    }

    public static SerialCommand create(Byte[] buffer, int size) {
        SerialCommand serialCommand = new SerialCommand();
        if (size < 6 || size > 65541) {
            return null;
        }
        serialCommand.setSequence((Byte[]) Arrays.copyOfRange(buffer, 0, 1));
        serialCommand.setCommandAction((Byte[]) Arrays.copyOfRange(buffer, 1, 2));
        serialCommand.setCommandBody((Byte[]) Arrays.copyOfRange(buffer, 2, 3));
        serialCommand.setStatus((Byte[]) Arrays.copyOfRange(buffer, 3, 4));
        serialCommand.setDataLength((Byte[]) Arrays.copyOfRange(buffer, 4, 6));
        int length = (serialCommand.getDataLength()[0].byteValue() & 255) + ((serialCommand.getDataLength()[1].byteValue() & 255) << 8);
        if (length + 6 > size || length < 0) {
            return null;
        }
        serialCommand.setData((Byte[]) Arrays.copyOfRange(buffer, 6, 6 + length));
        serialCommand.setCommandId();
        return serialCommand;
    }

    private SerialCommand create(String commandId) {
        if (commandId.isEmpty()) {
            return null;
        }
        Byte[] buffer = ByteUtils.hexStrToByte(commandId);
        if (buffer.length < 2) {
            return null;
        }
        SerialCommand serialCommand = new SerialCommand();
        serialCommand.setCommandAction((Byte[]) Arrays.copyOfRange(buffer, 0, 1));
        serialCommand.setCommandBody((Byte[]) Arrays.copyOfRange(buffer, 1, 2));
        return serialCommand;
    }

    public Byte[] toBytes() {
        List<Byte> list = new LinkedList<>();
        list.addAll(Arrays.asList(this.sequence));
        list.addAll(Arrays.asList(this.commandAction));
        list.addAll(Arrays.asList(this.commandBody));
        list.addAll(Arrays.asList(this.status));
        list.addAll(Arrays.asList(getLength()));
        if (null != this.data) {
            list.addAll(Arrays.asList(this.data));
        }
        return (Byte[]) list.toArray(new Byte[list.size()]);
    }

    public Byte[] getSequence() {
        return this.sequence;
    }

    public void setSequence(Byte[] sequence) {
        this.sequence = sequence;
    }

    public Byte[] getStatus() {
        return this.status;
    }

    public void setStatus(Byte[] status) {
        this.status = status;
    }

    public Byte[] getCommandAction() {
        return this.commandAction;
    }

    public void setCommandAction(Byte[] commandAction) {
        this.commandAction = commandAction;
    }

    public Byte[] getCommandBody() {
        return this.commandBody;
    }

    public void setCommandBody(Byte[] commandBody) {
        this.commandBody = commandBody;
    }

    public Byte[] getDataLength() {
        return this.dataLength;
    }

    public void setDataLength(Byte[] dataLength) {
        this.dataLength = dataLength;
    }

    public Byte[] getData() {
        return this.data;
    }

    public void setData(Byte[] data) {
        this.data = data;
    }

    private Byte[] getLength() {
        Byte[] length = {(byte) 0, (byte) 0};
        if (null != this.data) {
            length[0] = Byte.valueOf((byte) (this.data.length & 255));
            length[1] = Byte.valueOf((byte) ((this.data.length >> 8) & 255));
        }
        return length;
    }

    public boolean isReportCommand() {
        return ((byte) (this.commandAction[0].byteValue() & (-128))) == -128;
    }

    public String getCommandId() {
        return this.commandId;
    }

    private void setCommandId() {
        Byte[] commandAction = (Byte[]) Arrays.copyOf(this.commandAction, this.commandAction.length);
        commandAction[0] = Byte.valueOf((byte) (commandAction[0].byteValue() & 127));
        this.commandId = ByteUtils.BytesToHexStr((Byte[]) ArrayUtils.concat(commandAction, this.commandBody));
    }

    public boolean equalsCommand(String commandId) {
        SerialCommand command = create(commandId);
        return Arrays.equals(this.commandAction, command.commandAction) && Arrays.equals(this.commandBody, command.commandBody);
    }

    public boolean equalsCommandIncludeReport(String commandId) {
        SerialCommand command = create(commandId);
        Byte[] commandAction = (Byte[]) Arrays.copyOf(this.commandAction, this.commandAction.length);
        commandAction[0] = Byte.valueOf((byte) (commandAction[0].byteValue() & 127));
        return Arrays.equals(commandAction, command.commandAction) && Arrays.equals(this.commandBody, command.commandBody);
    }

    /* JADX INFO: renamed from: clone, reason: merged with bridge method [inline-methods] */
    public SerialCommand m71clone() {
        SerialCommand serialCommand = new SerialCommand();
        if (null != this.commandAction) {
            serialCommand.commandAction = (Byte[]) this.commandAction.clone();
        }
        if (null != this.commandBody) {
            serialCommand.commandBody = (Byte[]) this.commandBody.clone();
        }
        if (null != this.data) {
            serialCommand.data = (Byte[]) this.data.clone();
        }
        return serialCommand;
    }
}
