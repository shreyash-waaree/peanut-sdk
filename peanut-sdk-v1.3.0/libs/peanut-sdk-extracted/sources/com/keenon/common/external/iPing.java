package com.keenon.common.external;

import com.keenon.common.utils.LogUtils;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/common/external/iPing.class */
public abstract class iPing {
    private static final String TAG = "iPing";
    public long connectionTimeout;
    public Listener stateListener;

    /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/common/external/iPing$Listener.class */
    public interface Listener {
        void onUpdate(int i);
    }

    public abstract void start();

    public abstract void stop();

    public abstract void active();

    public abstract boolean isTimeout();

    public iPing(long timeout, Listener listener) {
        this.connectionTimeout = timeout;
        this.stateListener = listener;
    }

    public void notifyState(int state) {
        if (this.stateListener != null) {
            this.stateListener.onUpdate(state);
        }
    }

    public void sendPingRequest(String ipAddress) {
        try {
            InetAddress geek = InetAddress.getByName(ipAddress);
            LogUtils.i(TAG, "Sending Ping Request to " + ipAddress);
            if (geek.isReachable(2000)) {
                LogUtils.i(TAG, "Host is reachable " + ipAddress);
            } else {
                LogUtils.i(TAG, "can't reach to this host " + ipAddress);
            }
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e2) {
            e2.printStackTrace();
        }
    }
}
