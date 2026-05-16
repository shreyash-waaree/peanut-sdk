package com.keenon.sdk.hedera.base;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/hedera/base/iHedera.class */
public interface iHedera {

    /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/hedera/base/iHedera$OnDataExchangedListener.class */
    public interface OnDataExchangedListener extends onFrameDataExchangedListener {
        void onCmdDataSent(Byte[] bArr, int i);

        void onCmdDataReceived(Byte[] bArr, int i);
    }

    /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/hedera/base/iHedera$OnRawDataExchangedListener.class */
    public interface OnRawDataExchangedListener {
        void onRawDataSent(Byte[] bArr, int i);

        void onRawDataReceived(Byte[] bArr, int i);
    }

    /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/hedera/base/iHedera$onFrameDataExchangedListener.class */
    public interface onFrameDataExchangedListener extends OnRawDataExchangedListener {
        void onFrameDataCrcErrorDetected(Byte[] bArr, int i);

        void onFrameDataSent(Byte[] bArr, int i);

        void onFrameDataReceived(Byte[] bArr, int i);
    }

    void open();

    void close();
}
