package com.keenon.sdk.serial.transport;

import com.keenon.common.utils.ByteUtils;
import com.keenon.sdk.hedera.base.HederaFrameDetector;
import com.keenon.sdk.hedera.base.HederaTransceiver;
import com.keenon.sdk.hedera.base.HederaTransportProtocol;
import com.keenon.sdk.hedera.base.iHedera;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/serial/transport/HederaTransportTransparent.class */
public class HederaTransportTransparent implements HederaTransportProtocol {
    private HederaFrameDetector detector;
    private HederaTransceiver transceiver;
    private iHedera.onFrameDataExchangedListener frameDataExchangedListener;

    @Override // com.keenon.sdk.hedera.base.HederaTransportProtocol
    public Byte[] generateFrame(Byte[] data) {
        return data;
    }

    @Override // com.keenon.sdk.hedera.base.HederaTransportProtocol
    public void receiveData(byte[] data, int size) {
        this.detector.onFrameDetected(ByteUtils.ByteToObject(data), size);
    }

    @Override // com.keenon.sdk.hedera.base.HederaTransportProtocol
    public void setFrameDetector(HederaFrameDetector detector) {
        this.detector = detector;
    }

    @Override // com.keenon.sdk.hedera.base.HederaTransportProtocol
    public void sendData(Byte[] data) {
        this.transceiver.send(generateFrame(data));
    }

    @Override // com.keenon.sdk.hedera.base.HederaTransportProtocol
    public void setTransceiver(HederaTransceiver transceiver) {
        this.transceiver = transceiver;
    }

    @Override // com.keenon.sdk.hedera.base.HederaTransportProtocol
    public void setFrameDataExchangedListener(iHedera.onFrameDataExchangedListener frameDataExchangedListener) {
        this.frameDataExchangedListener = frameDataExchangedListener;
    }
}
