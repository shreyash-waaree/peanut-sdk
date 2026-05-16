package com.keenon.sdk.http.ws;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/http/ws/WsStatus.class */
public class WsStatus {
    public static final int CONNECTED = 1;
    public static final int CONNECTING = 0;
    public static final int RECONNECT = 2;
    public static final int DISCONNECTED = -1;

    /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/http/ws/WsStatus$CODE.class */
    class CODE {
        public static final int NORMAL_CLOSE = 1000;
        public static final int ABNORMAL_CLOSE = 1001;

        CODE() {
        }
    }

    /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/http/ws/WsStatus$TIP.class */
    class TIP {
        public static final String NORMAL_CLOSE = "normal close";
        public static final String ABNORMAL_CLOSE = "abnormal close";

        TIP() {
        }
    }

    /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/http/ws/WsStatus$ACTION.class */
    class ACTION {
        public static final String ACTION_NOTIFY = "notify";
        public static final String ACTION_NOTIFY_GET = "notifyGet";

        ACTION() {
        }
    }
}
