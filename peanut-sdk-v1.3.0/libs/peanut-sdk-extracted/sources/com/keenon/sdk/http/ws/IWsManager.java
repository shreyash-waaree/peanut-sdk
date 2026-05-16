package com.keenon.sdk.http.ws;

import okhttp3.WebSocket;
import okio.ByteString;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/http/ws/IWsManager.class */
public interface IWsManager {
    WebSocket getWebSocket();

    void startConnect();

    void stopConnect();

    boolean isWsConnected();

    int getCurrentStatus();

    void setCurrentStatus(int i);

    boolean sendMessage(String str);

    boolean sendMessage(ByteString byteString);
}
