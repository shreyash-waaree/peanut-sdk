package com.keenon.sdk.http.ws;

import okhttp3.Response;
import okio.ByteString;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/http/ws/WsStatusListener.class */
public abstract class WsStatusListener {
    public void onOpen(Response response) {
    }

    public void onMessage(String text) {
    }

    public void onMessage(ByteString bytes) {
    }

    public void onReconnect() {
    }

    public void onClosing(int code, String reason) {
    }

    public void onClosed(int code, String reason) {
    }

    public void onFailure(Throwable t, Response response) {
    }
}
