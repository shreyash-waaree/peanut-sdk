package com.keenon.sdk.proxy.adapter;

import com.keenon.common.constant.PeanutConstants;
import com.keenon.common.external.PeanutConfig;
import com.keenon.common.external.iPing;
import com.keenon.common.utils.LogUtils;
import com.keenon.sdk.coap.CoAPPing;
import com.keenon.sdk.hedera.DefaultPing;
import com.keenon.sdk.serial.SerialPing;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/proxy/adapter/PingTask.class */
public class PingTask {
    private static volatile PingTask sInstance;
    private iPing ping;

    public static PingTask getInstance() {
        if (sInstance == null) {
            synchronized (PingTask.class) {
                if (sInstance == null) {
                    sInstance = new PingTask();
                }
            }
        }
        return sInstance;
    }

    public void init(long timeout, iPing.Listener listener) {
        if (PeanutConfig.getLinkType() == PeanutConstants.LinkType.COM || PeanutConfig.getLinkType() == PeanutConstants.LinkType.COM_COAP) {
            this.ping = new SerialPing(timeout, listener);
        } else if (PeanutConfig.getLinkType() == PeanutConstants.LinkType.COAP) {
            this.ping = new CoAPPing(timeout, listener);
        } else {
            LogUtils.w(PeanutConstants.TAG_SDK, "protocol ping not support");
            this.ping = new DefaultPing(timeout, listener);
        }
    }

    public void start() {
        this.ping.start();
    }

    public void stop() {
        this.ping.stop();
    }

    public void active() {
        this.ping.active();
    }
}
