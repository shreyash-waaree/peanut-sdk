package com.keenon.sdk.hedera.base;

import com.keenon.sdk.hedera.base.iHedera;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/hedera/base/HederaTransportProtocol.class */
public interface HederaTransportProtocol {
    Byte[] generateFrame(Byte[] bArr);

    void receiveData(byte[] bArr, int i);

    void setFrameDetector(HederaFrameDetector hederaFrameDetector);

    void sendData(Byte[] bArr);

    void setTransceiver(HederaTransceiver hederaTransceiver);

    void setFrameDataExchangedListener(iHedera.onFrameDataExchangedListener onframedataexchangedlistener);
}
