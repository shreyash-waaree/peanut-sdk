package com.keenon.sdk.hedera.model;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/hedera/model/MsgType.class */
public enum MsgType {
    CON(0),
    NON(1);

    public final int value;

    MsgType(int value) {
        this.value = value;
    }

    public static MsgType valueOf(int value) {
        switch (value) {
            case 0:
                return CON;
            case 1:
                return NON;
            default:
                throw new IllegalArgumentException("Unknown CoAP type " + value);
        }
    }
}
