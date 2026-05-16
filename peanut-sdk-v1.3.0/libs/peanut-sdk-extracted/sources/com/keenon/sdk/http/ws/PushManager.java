package com.keenon.sdk.http.ws;

import android.content.Context;
import com.keenon.common.constant.PeanutConstants;
import com.keenon.common.external.PeanutConfig;
import com.keenon.common.utils.GsonUtil;
import com.keenon.common.utils.LogUtils;
import com.keenon.common.utils.StringUtils;
import com.keenon.sdk.http.PeanutHttpClient;
import com.keenon.sdk.http.ws.WsManager;
import okhttp3.OkHttpClient;
import okhttp3.Response;
import okio.ByteString;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/http/ws/PushManager.class */
public class PushManager {
    private static final String TAG = "PushManager";
    private static PushManager sInstance;
    private WsManager wsManager;
    private WsStatusListener wsStatusListener = new WsStatusListener() { // from class: com.keenon.sdk.http.ws.PushManager.1
        @Override // com.keenon.sdk.http.ws.WsStatusListener
        public void onOpen(Response response) {
            PushManager.this.log("onOpen-----服务器连接成功");
        }

        @Override // com.keenon.sdk.http.ws.WsStatusListener
        public void onMessage(String text) {
            PushManager.this.log("onMessage-----收到消息", text);
            PushManager.this.handleMessage(text);
        }

        @Override // com.keenon.sdk.http.ws.WsStatusListener
        public void onMessage(ByteString bytes) {
            PushManager.this.log("onMessage-----收到消息");
        }

        @Override // com.keenon.sdk.http.ws.WsStatusListener
        public void onReconnect() {
            PushManager.this.loge("onReconnect-----服务器重连接中...");
        }

        @Override // com.keenon.sdk.http.ws.WsStatusListener
        public void onClosing(int code, String reason) {
            PushManager.this.loge("onClosing-----服务器连接关闭中...");
        }

        @Override // com.keenon.sdk.http.ws.WsStatusListener
        public void onClosed(int code, String reason) {
            PushManager.this.loge("onClosed-----服务器连接已关闭");
        }

        @Override // com.keenon.sdk.http.ws.WsStatusListener
        public void onFailure(Throwable t, Response response) {
            PushManager.this.loge("onFailure-----服务器连接失败");
        }
    };

    public static PushManager getInstance() {
        if (sInstance == null) {
            synchronized (PushManager.class) {
                if (sInstance == null) {
                    sInstance = new PushManager();
                }
            }
        }
        return sInstance;
    }

    public boolean isConnected() {
        return this.wsManager != null && this.wsManager.isWsConnected();
    }

    private String getUrl() {
        return PeanutConstants.WS_PROTOCOL + PeanutConfig.getLinkIP() + ":" + PeanutConstants.WS_PORT;
    }

    public void init(Context context, OkHttpClient okHttpClient) {
        String url = getUrl();
        LogUtils.i(TAG, "ws server: " + url);
        this.wsManager = new WsManager.Builder(context).client(okHttpClient).needReconnect(true).wsUrl(getUrl()).build();
        this.wsManager.setWsStatusListener(this.wsStatusListener);
        this.wsManager.startConnect();
    }

    public void release() {
        if (this.wsManager != null) {
            this.wsManager.stopConnect();
            this.wsManager = null;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleMessage(String message) {
        WebSocketMessage msg = (WebSocketMessage) GsonUtil.gson2Bean(message, WebSocketMessage.class);
        LogUtils.i(PeanutConstants.TAG_SDK, "PushManager[handleMessage][event : " + msg + "]");
        if (msg == null || StringUtils.isEmpty(msg.getAction())) {
            return;
        }
        switch (msg.getAction()) {
            case "notify":
                PeanutHttpClient.getInstance().handlePushEvent(msg.getData().getApi(), msg.getData().getValue().toString());
                break;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void log(String tag) {
        LogUtils.d(TAG, "[" + tag + "]");
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void log(String tag, String content) {
        LogUtils.d(TAG, "[" + tag + "][" + content + "]");
    }

    private void log(String tag, int content) {
        LogUtils.d(TAG, "[" + tag + "][" + content + "]");
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void loge(String tag) {
        LogUtils.e(TAG, "[" + tag + "]");
    }

    private void loge(String tag, String content) {
        LogUtils.e(TAG, "[" + tag + "][" + content + "]");
    }
}
