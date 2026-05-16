package com.keenon.sdk.serial;

import com.keenon.common.constant.PeanutConstants;
import com.keenon.common.error.PeanutError;
import com.keenon.common.external.iPing;
import com.keenon.common.utils.LogUtils;
import com.keenon.common.utils.PeanutThreadFactory;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/serial/SerialPing.class */
public class SerialPing extends iPing {
    private static final String TAG = "[SerialPing]";
    private ScheduledExecutorService scheduledThreadPool;
    private volatile long aliveTimeStamp;
    private volatile boolean isStart;
    private volatile boolean isOnline;
    private volatile boolean hasSent;

    public SerialPing(long timeout, iPing.Listener listener) {
        super(timeout, listener);
        this.isStart = false;
        this.isOnline = false;
        this.hasSent = false;
    }

    @Override // com.keenon.common.external.iPing
    public void active() {
        this.aliveTimeStamp = System.currentTimeMillis();
        this.isOnline = true;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public long getOfflineTime() {
        return System.currentTimeMillis() - this.aliveTimeStamp;
    }

    @Override // com.keenon.common.external.iPing
    public boolean isTimeout() {
        return getOfflineTime() >= this.connectionTimeout;
    }

    @Override // com.keenon.common.external.iPing
    public void start() {
        if (this.isStart) {
            return;
        }
        this.isStart = true;
        this.scheduledThreadPool = new ScheduledThreadPoolExecutor(1, new PeanutThreadFactory("ComPing#"));
        active();
        this.scheduledThreadPool.scheduleWithFixedDelay(new Runnable() { // from class: com.keenon.sdk.serial.SerialPing.1
            @Override // java.lang.Runnable
            public void run() {
                LogUtils.i(PeanutConstants.TAG_SDK, "[SerialPing][startPing][offLineTime: " + SerialPing.this.getOfflineTime() + ", timeout : " + SerialPing.this.connectionTimeout + ", isTimeout : " + SerialPing.this.isTimeout() + ", isOnline : " + SerialPing.this.isOnline + "]");
                if (SerialPing.this.isTimeout()) {
                    if (SerialPing.this.hasSent) {
                        return;
                    }
                    SerialPing.this.notifyState(PeanutError.CONNECTION_TIMEOUT);
                    SerialPing.this.isOnline = false;
                    SerialPing.this.hasSent = true;
                    return;
                }
                if (SerialPing.this.hasSent) {
                    SerialPing.this.notifyState(PeanutError.CONNECTION_RESTORE);
                    SerialPing.this.hasSent = false;
                }
            }
        }, 0L, 1L, TimeUnit.SECONDS);
    }

    @Override // com.keenon.common.external.iPing
    public void stop() {
        if (this.scheduledThreadPool != null) {
            this.scheduledThreadPool.shutdown();
        }
        this.isStart = false;
    }
}
