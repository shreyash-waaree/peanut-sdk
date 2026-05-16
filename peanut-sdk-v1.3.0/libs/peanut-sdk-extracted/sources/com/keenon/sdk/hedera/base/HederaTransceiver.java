package com.keenon.sdk.hedera.base;

import com.keenon.sdk.hedera.base.iHedera;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/hedera/base/HederaTransceiver.class */
public interface HederaTransceiver {

    /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/hedera/base/HederaTransceiver$OnReceiveListener.class */
    public interface OnReceiveListener {
        void onReceive(byte[] bArr, int i);
    }

    int open();

    int send(Byte[] bArr);

    int setOnReceiveListener(OnReceiveListener onReceiveListener);

    void addReceiveListener(OnReceiveListener onReceiveListener);

    void removeReceiveListener(OnReceiveListener onReceiveListener);

    void close();

    void setRawDataExchangedListener(iHedera.OnRawDataExchangedListener onRawDataExchangedListener);
}
