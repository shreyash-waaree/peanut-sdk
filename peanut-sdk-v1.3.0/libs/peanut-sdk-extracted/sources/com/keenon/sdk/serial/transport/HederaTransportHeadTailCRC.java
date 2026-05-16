package com.keenon.sdk.serial.transport;

import com.keenon.common.constant.PeanutConstants;
import com.keenon.common.utils.LogUtils;
import com.keenon.sdk.hedera.base.HederaFrameDetector;
import com.keenon.sdk.hedera.base.HederaTransceiver;
import com.keenon.sdk.hedera.base.HederaTransportProtocol;
import com.keenon.sdk.hedera.base.iHedera;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/serial/transport/HederaTransportHeadTailCRC.class */
public class HederaTransportHeadTailCRC implements HederaTransportProtocol {
    private static final String TAG = "[HederaTransportHeadTailCRC]";
    private HederaFrameDetector frameListener;
    private HederaTransceiver transceiver;
    private iHedera.onFrameDataExchangedListener frameDataExchangedListener;
    private Queue<Byte> receive_fifo = new LinkedList();
    private Byte[] headerChars = {(byte) -86, (byte) -86};
    private Byte[] tailChars = {(byte) 85, (byte) 85};
    private Byte ctrlChar = (byte) -91;
    private List<Byte> escapedChars = new ArrayList<Byte>() { // from class: com.keenon.sdk.serial.transport.HederaTransportHeadTailCRC.1
        {
            addAll(Arrays.asList(HederaTransportHeadTailCRC.this.headerChars));
            addAll(Arrays.asList(HederaTransportHeadTailCRC.this.tailChars));
            add(HederaTransportHeadTailCRC.this.ctrlChar);
        }
    };
    private FrameStatesManager frameStatesManager = new FrameStatesManager();

    /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/serial/transport/HederaTransportHeadTailCRC$FrameState.class */
    private enum FrameState implements State {
        FRAME_STATE_IDLE { // from class: com.keenon.sdk.serial.transport.HederaTransportHeadTailCRC.FrameState.1
            @Override // com.keenon.sdk.serial.transport.HederaTransportHeadTailCRC.State
            public void process(FrameStatesManager statesManager, byte b) {
                if (b == statesManager.header) {
                    statesManager.addToBuffer(Byte.valueOf(b));
                    statesManager.setFrameState(FRAME_STATE_FIRST_HEADER);
                }
            }
        },
        FRAME_STATE_FIRST_HEADER { // from class: com.keenon.sdk.serial.transport.HederaTransportHeadTailCRC.FrameState.2
            @Override // com.keenon.sdk.serial.transport.HederaTransportHeadTailCRC.State
            public void process(FrameStatesManager statesManager, byte b) {
                if (b == statesManager.header) {
                    statesManager.addToBuffer(Byte.valueOf(b));
                    statesManager.setFrameState(FRAME_STATE_SECOND_HEADER);
                } else {
                    statesManager.clearBuffer();
                    statesManager.setFrameState(FRAME_STATE_IDLE);
                }
            }
        },
        FRAME_STATE_SECOND_HEADER { // from class: com.keenon.sdk.serial.transport.HederaTransportHeadTailCRC.FrameState.3
            @Override // com.keenon.sdk.serial.transport.HederaTransportHeadTailCRC.State
            public void process(FrameStatesManager statesManager, byte b) {
                if (b == statesManager.header) {
                    statesManager.setFrameState(FRAME_STATE_SECOND_HEADER);
                    return;
                }
                if (b == statesManager.tail) {
                    statesManager.addToBuffer(Byte.valueOf(b));
                    statesManager.setFrameState(FRAME_STATE_FIRST_TAIL);
                } else if (b == statesManager.ctrl) {
                    statesManager.addToBuffer(Byte.valueOf(b));
                    statesManager.setFrameState(FRAME_STATE_ESCAPED_CHAR);
                } else {
                    statesManager.addToBuffer(Byte.valueOf(b));
                    statesManager.setFrameState(FRAME_STATE_FRAME_DATA);
                }
            }
        },
        FRAME_STATE_FRAME_DATA { // from class: com.keenon.sdk.serial.transport.HederaTransportHeadTailCRC.FrameState.4
            @Override // com.keenon.sdk.serial.transport.HederaTransportHeadTailCRC.State
            public void process(FrameStatesManager statesManager, byte b) {
                if (b == statesManager.header) {
                    statesManager.addToBuffer(Byte.valueOf(b));
                    statesManager.setFrameState(FRAME_STATE_HEADER_IN_DATA);
                } else if (b == statesManager.tail) {
                    statesManager.addToBuffer(Byte.valueOf(b));
                    statesManager.setFrameState(FRAME_STATE_FIRST_TAIL);
                } else if (b == statesManager.ctrl) {
                    statesManager.addToBuffer(Byte.valueOf(b));
                    statesManager.setFrameState(FRAME_STATE_ESCAPED_CHAR);
                } else {
                    statesManager.addToBuffer(Byte.valueOf(b));
                    statesManager.setFrameState(FRAME_STATE_FRAME_DATA);
                }
            }
        },
        FRAME_STATE_FIRST_TAIL { // from class: com.keenon.sdk.serial.transport.HederaTransportHeadTailCRC.FrameState.5
            @Override // com.keenon.sdk.serial.transport.HederaTransportHeadTailCRC.State
            public void process(FrameStatesManager statesManager, byte b) {
                if (b == statesManager.header) {
                    statesManager.addToBuffer(Byte.valueOf(b));
                    statesManager.setFrameState(FRAME_STATE_HEADER_IN_DATA);
                    return;
                }
                if (b == statesManager.tail) {
                    statesManager.addToBuffer(Byte.valueOf(b));
                    statesManager.outputBuffer();
                    statesManager.setFrameState(FRAME_STATE_IDLE);
                } else if (b == statesManager.ctrl) {
                    statesManager.addToBuffer(Byte.valueOf(b));
                    statesManager.setFrameState(FRAME_STATE_ESCAPED_CHAR);
                } else {
                    statesManager.addToBuffer(Byte.valueOf(b));
                    statesManager.setFrameState(FRAME_STATE_FRAME_DATA);
                }
            }
        },
        FRAME_STATE_HEADER_IN_DATA { // from class: com.keenon.sdk.serial.transport.HederaTransportHeadTailCRC.FrameState.6
            @Override // com.keenon.sdk.serial.transport.HederaTransportHeadTailCRC.State
            public void process(FrameStatesManager statesManager, byte b) {
                if (b == statesManager.header) {
                    statesManager.clearBuffer();
                    statesManager.addToBuffer(Byte.valueOf(statesManager.header));
                    statesManager.addToBuffer(Byte.valueOf(b));
                    statesManager.setFrameState(FRAME_STATE_SECOND_HEADER);
                    return;
                }
                if (b == statesManager.tail) {
                    statesManager.addToBuffer(Byte.valueOf(b));
                    statesManager.setFrameState(FRAME_STATE_FIRST_TAIL);
                } else if (b == statesManager.ctrl) {
                    statesManager.addToBuffer(Byte.valueOf(b));
                    statesManager.setFrameState(FRAME_STATE_ESCAPED_CHAR);
                } else {
                    statesManager.addToBuffer(Byte.valueOf(b));
                    statesManager.setFrameState(FRAME_STATE_FRAME_DATA);
                }
            }
        },
        FRAME_STATE_ESCAPED_CHAR { // from class: com.keenon.sdk.serial.transport.HederaTransportHeadTailCRC.FrameState.7
            @Override // com.keenon.sdk.serial.transport.HederaTransportHeadTailCRC.State
            public void process(FrameStatesManager statesManager, byte b) {
                statesManager.addToBuffer(Byte.valueOf(b));
                statesManager.setFrameState(FRAME_STATE_FRAME_DATA);
            }
        }
    }

    /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/serial/transport/HederaTransportHeadTailCRC$State.class */
    interface State {
        void process(FrameStatesManager frameStatesManager, byte b);
    }

    @Override // com.keenon.sdk.hedera.base.HederaTransportProtocol
    public void setTransceiver(HederaTransceiver transceiver) {
        this.transceiver = transceiver;
    }

    @Override // com.keenon.sdk.hedera.base.HederaTransportProtocol
    public void setFrameDataExchangedListener(iHedera.onFrameDataExchangedListener frameDataExchangedListener) {
        this.frameDataExchangedListener = frameDataExchangedListener;
        if (this.transceiver != null) {
            this.transceiver.setRawDataExchangedListener(frameDataExchangedListener);
        }
    }

    @Override // com.keenon.sdk.hedera.base.HederaTransportProtocol
    public void setFrameDetector(HederaFrameDetector listener) {
        this.frameListener = listener;
    }

    @Override // com.keenon.sdk.hedera.base.HederaTransportProtocol
    public void sendData(Byte[] data) {
        Byte[] frame = generateFrame(data);
        if (this.frameDataExchangedListener != null) {
            this.frameDataExchangedListener.onFrameDataSent(frame, frame.length);
        }
        this.transceiver.send(frame);
    }

    @Override // com.keenon.sdk.hedera.base.HederaTransportProtocol
    public Byte[] generateFrame(Byte[] data) {
        List<Byte> result = new LinkedList<>();
        int crc = getCRC(data);
        for (Byte ch : data) {
            if (this.escapedChars.contains(ch)) {
                result.add(this.ctrlChar);
            }
            result.add(ch);
        }
        for (byte b : intToBytes(crc)) {
            Byte ch2 = Byte.valueOf(b);
            if (this.escapedChars.contains(ch2)) {
                result.add(this.ctrlChar);
            }
            result.add(ch2);
        }
        result.addAll(0, Arrays.asList(this.headerChars));
        result.addAll(Arrays.asList(this.tailChars));
        return (Byte[]) result.toArray(new Byte[result.size()]);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public Byte[] getDataFromFrame(Byte[] data) {
        List<Byte> frameData = new ArrayList<>(Arrays.asList(data));
        List<Byte> dataAndCRC = new LinkedList<>();
        frameData.remove(0);
        frameData.remove(0);
        frameData.remove(frameData.size() - 1);
        frameData.remove(frameData.size() - 1);
        int i = 0;
        while (i < frameData.size()) {
            if (frameData.get(i).equals(this.ctrlChar)) {
                if (i < frameData.size() - 1 && this.escapedChars.contains(frameData.get(i + 1))) {
                    i++;
                    dataAndCRC.add(frameData.get(i));
                } else {
                    return null;
                }
            } else {
                dataAndCRC.add(frameData.get(i));
            }
            i++;
        }
        if (dataAndCRC.size() < 3) {
            return null;
        }
        Byte[] result = crcCheck(dataAndCRC);
        if (result == null) {
            if (this.frameDataExchangedListener != null) {
                Byte[] dataCopy = (Byte[]) Arrays.copyOf(data, data.length);
                this.frameDataExchangedListener.onFrameDataCrcErrorDetected(dataCopy, dataCopy.length);
            }
        } else if (this.frameDataExchangedListener != null) {
            Byte[] dataCopy2 = (Byte[]) Arrays.copyOf(data, data.length);
            this.frameDataExchangedListener.onFrameDataReceived(dataCopy2, dataCopy2.length);
        }
        return result;
    }

    @Override // com.keenon.sdk.hedera.base.HederaTransportProtocol
    public void receiveData(byte[] data, int size) {
        for (int i = 0; i < size; i++) {
            this.receive_fifo.offer(Byte.valueOf(data[i]));
        }
        while (this.receive_fifo.size() > 0) {
            try {
                this.frameStatesManager.getFrameState().process(this.frameStatesManager, this.receive_fifo.poll().byteValue());
            } catch (Exception e) {
                LogUtils.e(PeanutConstants.TAG_SDK, TAG, e);
                return;
            }
        }
    }

    private Byte[] crcCheck(List<Byte> dataWithCrc) {
        Byte[] crc = {dataWithCrc.remove(dataWithCrc.size() - 1), dataWithCrc.remove(dataWithCrc.size() - 1)};
        Byte[] data = (Byte[]) dataWithCrc.toArray(new Byte[dataWithCrc.size()]);
        int crc_int = bytesToInt(crc);
        int crc_int_calc = getCRC(data);
        if (crc_int != crc_int_calc) {
            return null;
        }
        return data;
    }

    private int getCRC(Byte[] bytes) {
        int i;
        int CRC = 65535;
        for (Byte b : bytes) {
            CRC ^= b.byteValue() & 255;
            for (int j = 0; j < 8; j++) {
                if ((CRC & 1) != 0) {
                    i = (CRC >> 1) ^ 40961;
                } else {
                    i = CRC >> 1;
                }
                CRC = i;
            }
        }
        return CRC;
    }

    private byte[] intToBytes(int value) {
        return new byte[]{(byte) (value & 255), (byte) ((value >> 8) & 255)};
    }

    private int bytesToInt(Byte[] b) {
        return (b[0].byteValue() & 255) | ((b[1].byteValue() & 255) << 8);
    }

    /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/serial/transport/HederaTransportHeadTailCRC$FrameStatesManager.class */
    private class FrameStatesManager {
        byte header;
        byte tail;
        byte ctrl;
        private FrameState frameState;
        private List<Byte> buffer;

        private FrameStatesManager() {
            this.header = HederaTransportHeadTailCRC.this.headerChars[0].byteValue();
            this.tail = HederaTransportHeadTailCRC.this.tailChars[0].byteValue();
            this.ctrl = HederaTransportHeadTailCRC.this.ctrlChar.byteValue();
            this.frameState = FrameState.FRAME_STATE_IDLE;
            this.buffer = new ArrayList();
        }

        FrameState getFrameState() {
            return this.frameState;
        }

        void setFrameState(FrameState frameState) {
            this.frameState = frameState;
        }

        void addToBuffer(Byte b) {
            this.buffer.add(b);
        }

        void clearBuffer() {
            this.buffer.clear();
        }

        void outputBuffer() {
            Byte[] result = HederaTransportHeadTailCRC.this.getDataFromFrame((Byte[]) this.buffer.toArray(new Byte[this.buffer.size()]));
            if (null != result) {
                HederaTransportHeadTailCRC.this.frameListener.onFrameDetected(result, result.length);
            }
            this.buffer.clear();
        }
    }
}
