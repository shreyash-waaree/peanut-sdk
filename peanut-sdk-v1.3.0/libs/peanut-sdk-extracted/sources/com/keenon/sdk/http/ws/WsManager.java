package com.keenon.sdk.http.ws;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.Looper;
import com.keenon.sdk.http.ws.WsStatus;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/http/ws/WsManager.class */
public class WsManager implements IWsManager {
    private static final int RECONNECT_INTERVAL = 5000;
    private static final long RECONNECT_MAX_TIME = 60000;
    private Context mContext;
    private String wsUrl;
    private WebSocket mWebSocket;
    private OkHttpClient mOkHttpClient;
    private Request mRequest;
    private boolean isNeedReconnect;
    private WsStatusListener wsStatusListener;
    private int mCurrentStatus = -1;
    private boolean isManualClose = false;
    private Handler wsMainHandler = new Handler(Looper.getMainLooper());
    private int reconnectCount = 0;
    private Runnable reconnectRunnable = new Runnable() { // from class: com.keenon.sdk.http.ws.WsManager.1
        @Override // java.lang.Runnable
        public void run() {
            if (WsManager.this.wsStatusListener != null) {
                WsManager.this.wsStatusListener.onReconnect();
            }
            WsManager.this.buildConnect();
        }
    };
    private WebSocketListener mWebSocketListener = new WebSocketListener() { // from class: com.keenon.sdk.http.ws.WsManager.2
        public void onOpen(WebSocket webSocket, final Response response) {
            WsManager.this.mWebSocket = webSocket;
            WsManager.this.setCurrentStatus(1);
            WsManager.this.connected();
            if (WsManager.this.wsStatusListener != null) {
                if (Looper.myLooper() != Looper.getMainLooper()) {
                    WsManager.this.wsMainHandler.post(new Runnable() { // from class: com.keenon.sdk.http.ws.WsManager.2.1
                        @Override // java.lang.Runnable
                        public void run() {
                            WsManager.this.wsStatusListener.onOpen(response);
                        }
                    });
                } else {
                    WsManager.this.wsStatusListener.onOpen(response);
                }
            }
        }

        public void onMessage(WebSocket webSocket, final ByteString bytes) {
            if (WsManager.this.wsStatusListener != null) {
                if (Looper.myLooper() != Looper.getMainLooper()) {
                    WsManager.this.wsMainHandler.post(new Runnable() { // from class: com.keenon.sdk.http.ws.WsManager.2.2
                        @Override // java.lang.Runnable
                        public void run() {
                            WsManager.this.wsStatusListener.onMessage(bytes);
                        }
                    });
                } else {
                    WsManager.this.wsStatusListener.onMessage(bytes);
                }
            }
        }

        public void onMessage(WebSocket webSocket, final String text) {
            if (WsManager.this.wsStatusListener != null) {
                if (Looper.myLooper() != Looper.getMainLooper()) {
                    WsManager.this.wsMainHandler.post(new Runnable() { // from class: com.keenon.sdk.http.ws.WsManager.2.3
                        @Override // java.lang.Runnable
                        public void run() {
                            WsManager.this.wsStatusListener.onMessage(text);
                        }
                    });
                } else {
                    WsManager.this.wsStatusListener.onMessage(text);
                }
            }
        }

        public void onClosing(WebSocket webSocket, final int code, final String reason) {
            if (WsManager.this.wsStatusListener != null) {
                if (Looper.myLooper() != Looper.getMainLooper()) {
                    WsManager.this.wsMainHandler.post(new Runnable() { // from class: com.keenon.sdk.http.ws.WsManager.2.4
                        @Override // java.lang.Runnable
                        public void run() {
                            WsManager.this.wsStatusListener.onClosing(code, reason);
                        }
                    });
                } else {
                    WsManager.this.wsStatusListener.onClosing(code, reason);
                }
            }
        }

        public void onClosed(WebSocket webSocket, final int code, final String reason) {
            if (WsManager.this.wsStatusListener != null) {
                if (Looper.myLooper() != Looper.getMainLooper()) {
                    WsManager.this.wsMainHandler.post(new Runnable() { // from class: com.keenon.sdk.http.ws.WsManager.2.5
                        @Override // java.lang.Runnable
                        public void run() {
                            WsManager.this.wsStatusListener.onClosed(code, reason);
                        }
                    });
                } else {
                    WsManager.this.wsStatusListener.onClosed(code, reason);
                }
            }
        }

        public void onFailure(WebSocket webSocket, final Throwable t, final Response response) {
            WsManager.this.tryReconnect();
            if (WsManager.this.wsStatusListener != null) {
                if (Looper.myLooper() != Looper.getMainLooper()) {
                    WsManager.this.wsMainHandler.post(new Runnable() { // from class: com.keenon.sdk.http.ws.WsManager.2.6
                        @Override // java.lang.Runnable
                        public void run() {
                            WsManager.this.wsStatusListener.onFailure(t, response);
                        }
                    });
                } else {
                    WsManager.this.wsStatusListener.onFailure(t, response);
                }
            }
        }
    };
    private Lock mLock = new ReentrantLock();

    public WsManager(Builder builder) {
        this.mContext = builder.mContext;
        this.wsUrl = builder.wsUrl;
        this.isNeedReconnect = builder.needReconnect;
        this.mOkHttpClient = builder.mOkHttpClient;
    }

    private void initWebSocket() {
        if (this.mOkHttpClient == null) {
            this.mOkHttpClient = new OkHttpClient.Builder().retryOnConnectionFailure(true).build();
        }
        if (this.mRequest == null) {
            this.mRequest = new Request.Builder().url(this.wsUrl).build();
        }
        this.mOkHttpClient.dispatcher().cancelAll();
        try {
            this.mLock.lockInterruptibly();
            try {
                this.mOkHttpClient.newWebSocket(this.mRequest, this.mWebSocketListener);
                this.mLock.unlock();
            } catch (Throwable th) {
                this.mLock.unlock();
                throw th;
            }
        } catch (InterruptedException e) {
        }
    }

    @Override // com.keenon.sdk.http.ws.IWsManager
    public WebSocket getWebSocket() {
        return this.mWebSocket;
    }

    public void setWsStatusListener(WsStatusListener wsStatusListener) {
        this.wsStatusListener = wsStatusListener;
    }

    @Override // com.keenon.sdk.http.ws.IWsManager
    public synchronized boolean isWsConnected() {
        return this.mCurrentStatus == 1;
    }

    @Override // com.keenon.sdk.http.ws.IWsManager
    public synchronized int getCurrentStatus() {
        return this.mCurrentStatus;
    }

    @Override // com.keenon.sdk.http.ws.IWsManager
    public synchronized void setCurrentStatus(int currentStatus) {
        this.mCurrentStatus = currentStatus;
    }

    @Override // com.keenon.sdk.http.ws.IWsManager
    public void startConnect() {
        this.isManualClose = false;
        buildConnect();
    }

    @Override // com.keenon.sdk.http.ws.IWsManager
    public void stopConnect() {
        this.isManualClose = true;
        disconnect();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void tryReconnect() {
        if ((!this.isNeedReconnect) | this.isManualClose) {
            return;
        }
        setCurrentStatus(2);
        long delay = this.reconnectCount * 5000;
        this.wsMainHandler.postDelayed(this.reconnectRunnable, Math.min(delay, 60000L));
        this.reconnectCount++;
    }

    private void cancelReconnect() {
        this.wsMainHandler.removeCallbacks(this.reconnectRunnable);
        this.reconnectCount = 0;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void connected() {
        cancelReconnect();
    }

    private void disconnect() {
        if (this.mCurrentStatus == -1) {
            return;
        }
        cancelReconnect();
        if (this.mOkHttpClient != null) {
            this.mOkHttpClient.dispatcher().cancelAll();
        }
        if (this.mWebSocket != null) {
            boolean isClosed = this.mWebSocket.close(WsStatus.CODE.NORMAL_CLOSE, WsStatus.TIP.NORMAL_CLOSE);
            if (!isClosed && this.wsStatusListener != null) {
                this.wsStatusListener.onClosed(1001, WsStatus.TIP.ABNORMAL_CLOSE);
            }
        }
        setCurrentStatus(-1);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public synchronized void buildConnect() {
        switch (getCurrentStatus()) {
            case 0:
            case 1:
                break;
            default:
                setCurrentStatus(0);
                initWebSocket();
                break;
        }
    }

    @Override // com.keenon.sdk.http.ws.IWsManager
    public boolean sendMessage(String msg) {
        return send(msg);
    }

    @Override // com.keenon.sdk.http.ws.IWsManager
    public boolean sendMessage(ByteString byteString) {
        return send(byteString);
    }

    private boolean send(Object msg) {
        boolean isSend = false;
        if (this.mWebSocket != null && this.mCurrentStatus == 1) {
            if (msg instanceof String) {
                isSend = this.mWebSocket.send((String) msg);
            } else if (msg instanceof ByteString) {
                isSend = this.mWebSocket.send((ByteString) msg);
            }
            if (!isSend) {
                tryReconnect();
            }
        }
        return isSend;
    }

    private boolean isNetworkConnected(Context context) {
        if (context != null) {
            ConnectivityManager mConnectivityManager = (ConnectivityManager) context.getSystemService("connectivity");
            NetworkInfo mNetworkInfo = mConnectivityManager.getActiveNetworkInfo();
            if (mNetworkInfo != null) {
                return mNetworkInfo.isAvailable();
            }
            return false;
        }
        return false;
    }

    /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/http/ws/WsManager$Builder.class */
    public static final class Builder {
        private Context mContext;
        private String wsUrl;
        private boolean needReconnect = true;
        private OkHttpClient mOkHttpClient;

        public Builder(Context val) {
            this.mContext = val;
        }

        public Builder wsUrl(String val) {
            this.wsUrl = val;
            return this;
        }

        public Builder client(OkHttpClient val) {
            this.mOkHttpClient = val;
            return this;
        }

        public Builder needReconnect(boolean val) {
            this.needReconnect = val;
            return this;
        }

        public WsManager build() {
            return new WsManager(this);
        }
    }
}
