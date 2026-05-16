package com.keenon.sdk.component.navigation.arrival;

import android.os.Handler;
import android.os.Looper;
import com.keenon.common.constant.PeanutConstants;
import com.keenon.common.utils.LogUtils;
import com.keenon.sdk.component.navigation.arrival.ArrivalControl;
import java.util.Timer;
import java.util.TimerTask;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/component/navigation/arrival/DefaultArrivalControl.class */
public class DefaultArrivalControl implements ArrivalControl {
    private ArrivalControl.Listener listener;
    private boolean enable;
    private final float marginScope;
    private final int blockDelay;
    private final int scopeDelay;
    private boolean blocked;
    private boolean destinationNearby;
    private Timer scopeTimer;
    private Timer blockTimer;
    private TimerTask scopeTimerTask;
    private TimerTask blockTimerTask;

    public DefaultArrivalControl() {
        this(false, 1.0f, ArrivalControl.DEFAULT_BLOCK_DELAY, 10000);
    }

    public DefaultArrivalControl(boolean enable) {
        this(enable, 1.0f, ArrivalControl.DEFAULT_BLOCK_DELAY, 10000);
    }

    public DefaultArrivalControl(boolean enable, float marginScope, int blockDelay, int scopeDelay) {
        this.blocked = false;
        this.destinationNearby = false;
        this.enable = enable;
        this.marginScope = marginScope;
        this.blockDelay = blockDelay;
        this.scopeDelay = scopeDelay;
    }

    @Override // com.keenon.sdk.component.navigation.arrival.ArrivalControl
    public void addListener(ArrivalControl.Listener listener) {
        this.listener = listener;
    }

    @Override // com.keenon.sdk.component.navigation.arrival.ArrivalControl
    public void removeListener(ArrivalControl.Listener listener) {
        this.listener = null;
    }

    @Override // com.keenon.sdk.component.navigation.arrival.ArrivalControl
    public void enable(boolean enable) {
        this.enable = enable;
    }

    @Override // com.keenon.sdk.component.navigation.arrival.ArrivalControl
    public synchronized void notifyDistanceChanged(float distance) {
        LogUtils.d(PeanutConstants.TAG_NAVI, "[DefaultArrivalControl][notifyDistanceChanged][enable->" + this.enable + "][destinationNearby->" + this.destinationNearby + "][blocked->" + this.blocked + "][distance->" + distance + "]");
        if (!this.enable || distance <= 0.0f) {
            return;
        }
        if (this.destinationNearby) {
            if (this.marginScope <= distance) {
                reset();
            }
        } else if (this.marginScope > distance) {
            startScopeTimer();
        }
    }

    @Override // com.keenon.sdk.component.navigation.arrival.ArrivalControl
    public synchronized void notifyBlocked() {
        LogUtils.d(PeanutConstants.TAG_NAVI, "[DefaultArrivalControl][notifyBlocked][enable->" + this.enable + "][destinationNearby->" + this.destinationNearby + "][blocked->" + this.blocked + "]");
        if (this.enable && this.destinationNearby && !this.blocked) {
            startBlockTimer();
        }
    }

    @Override // com.keenon.sdk.component.navigation.arrival.ArrivalControl
    public void release() {
        reset();
    }

    private void startScopeTimer() {
        this.destinationNearby = true;
        this.scopeTimer = new Timer();
        this.scopeTimerTask = new TimerTask() { // from class: com.keenon.sdk.component.navigation.arrival.DefaultArrivalControl.1
            @Override // java.util.TimerTask, java.lang.Runnable
            public void run() {
                if (null != DefaultArrivalControl.this.blockTimerTask) {
                    DefaultArrivalControl.this.blockTimerTask.cancel();
                }
                LogUtils.d(PeanutConstants.TAG_NAVI, "[DefaultArrivalControl][ScopeTimer]");
                DefaultArrivalControl.this.notifyArrived();
            }
        };
        this.scopeTimer.schedule(this.scopeTimerTask, this.scopeDelay);
    }

    private void startBlockTimer() {
        this.blocked = true;
        this.blockTimer = new Timer();
        this.blockTimerTask = new TimerTask() { // from class: com.keenon.sdk.component.navigation.arrival.DefaultArrivalControl.2
            @Override // java.util.TimerTask, java.lang.Runnable
            public void run() {
                DefaultArrivalControl.this.scopeTimerTask.cancel();
                LogUtils.d(PeanutConstants.TAG_NAVI, "[DefaultArrivalControl][BlockTimer]");
                DefaultArrivalControl.this.notifyArrived();
            }
        };
        this.blockTimer.schedule(this.blockTimerTask, this.blockDelay);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void notifyArrived() {
        LogUtils.d(PeanutConstants.TAG_NAVI, "[DefaultArrivalControl][notifyArrived]");
        if (null != this.listener) {
            new Handler(Looper.getMainLooper()).post(new Runnable() { // from class: com.keenon.sdk.component.navigation.arrival.DefaultArrivalControl.3
                @Override // java.lang.Runnable
                public void run() {
                    LogUtils.d(PeanutConstants.TAG_NAVI, "[DefaultArrivalControl][notifyArrived really]");
                    DefaultArrivalControl.this.listener.onArrived();
                }
            });
        }
    }

    private void reset() {
        this.destinationNearby = false;
        this.blocked = false;
        if (null != this.blockTimer) {
            this.blockTimer.cancel();
            this.blockTimer = null;
        }
        if (null != this.blockTimerTask) {
            this.blockTimerTask.cancel();
            this.blockTimerTask = null;
        }
        if (null != this.scopeTimer) {
            this.scopeTimer.cancel();
            this.scopeTimer = null;
        }
        if (null != this.scopeTimerTask) {
            this.scopeTimerTask.cancel();
            this.scopeTimerTask = null;
        }
    }
}
