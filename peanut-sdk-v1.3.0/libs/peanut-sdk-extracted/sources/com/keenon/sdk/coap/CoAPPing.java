package com.keenon.sdk.coap;

import com.keenon.common.constant.PeanutConstants;
import com.keenon.common.error.PeanutError;
import com.keenon.common.external.PeanutConfig;
import com.keenon.common.external.iPing;
import com.keenon.common.utils.LogUtils;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.eclipse.californium.elements.util.NamedThreadFactory;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/coap/CoAPPing.class */
public class CoAPPing extends iPing {
    private static final String TAG = "[CoAPPing]";
    private ScheduledExecutorService scheduledThreadPool;
    private String mUrl;
    private long mStartTime;
    private volatile boolean isStart;
    private volatile boolean isOnline;
    private volatile boolean hasSent;

    public CoAPPing(long timeout, iPing.Listener listener) {
        super(timeout, listener);
        this.isStart = false;
        this.isOnline = false;
        this.hasSent = false;
    }

    @Override // com.keenon.common.external.iPing
    public boolean isTimeout() {
        return getOfflineTime() > this.connectionTimeout;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public long getOfflineTime() {
        return System.currentTimeMillis() - this.mStartTime;
    }

    @Override // com.keenon.common.external.iPing
    public void active() {
        this.mStartTime = System.currentTimeMillis();
        this.isOnline = true;
    }

    @Override // com.keenon.common.external.iPing
    public void start() {
        if (this.isStart) {
            return;
        }
        this.isStart = true;
        this.mUrl = PeanutConfig.getLinkUrl();
        active();
        this.scheduledThreadPool = new ScheduledThreadPoolExecutor(1, new NamedThreadFactory("CoapPing#"));
        this.scheduledThreadPool.scheduleWithFixedDelay(new Runnable() { // from class: com.keenon.sdk.coap.CoAPPing.1
            @Override // java.lang.Runnable
            public void run() {
                LogUtils.i(PeanutConstants.TAG_SDK, "[CoAPPing][startPing][offLineTime: " + CoAPPing.this.getOfflineTime() + ", timeout : " + CoAPPing.this.connectionTimeout + ", isTimeout : " + CoAPPing.this.isTimeout() + ", isOnline : " + CoAPPing.this.isOnline + "]");
                if (PeanutCoapClient.getInstance().ping(CoAPPing.this.mUrl)) {
                    CoAPPing.this.active();
                }
                if (!CoAPPing.this.isTimeout()) {
                    if (CoAPPing.this.hasSent) {
                        CoAPPing.this.sendPingRequest(PeanutConstants.REMOTE_LINK_PROXY);
                        CoAPPing.this.notifyState(PeanutError.CONNECTION_RESTORE);
                        CoAPPing.this.hasSent = false;
                        return;
                    }
                    return;
                }
                CoAPPing.this.sendPingRequest(PeanutConstants.REMOTE_LINK_PROXY);
                if (CoAPPing.this.hasSent) {
                    return;
                }
                CoAPPing.this.notifyState(PeanutError.CONNECTION_TIMEOUT);
                CoAPPing.this.isOnline = false;
                CoAPPing.this.hasSent = true;
            }
        }, 0L, 2L, TimeUnit.SECONDS);
    }

    @Override // com.keenon.common.external.iPing
    public void stop() {
        if (this.scheduledThreadPool != null) {
            this.scheduledThreadPool.shutdown();
        }
        if (null != this.stateListener) {
            this.stateListener = null;
        }
        this.isStart = false;
    }
}
