package org.eclipse.californium.core.observe;

import java.util.concurrent.TimeUnit;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.californium.elements.util.ClockUtil;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-californium-core.jar:org/eclipse/californium/core/observe/NotificationOrder.class */
public class NotificationOrder {
    protected final int number;
    protected final long nanoTimestamp;

    public NotificationOrder(int observe) {
        this(observe, ClockUtil.nanoRealtime());
    }

    public NotificationOrder(int observe, long nanoTime) {
        this.number = observe;
        this.nanoTimestamp = nanoTime;
    }

    public int getObserve() {
        return this.number;
    }

    public boolean isNew(Response response) {
        Integer observe = response.getOptions().getObserve();
        if (observe == null) {
            return true;
        }
        return isNew(this.nanoTimestamp, this.number, ClockUtil.nanoRealtime(), observe.intValue());
    }

    public static boolean isNew(long T1, int V1, long T2, int V2) {
        if (V1 < V2 && V2 - V1 < 8388608) {
            return true;
        }
        if ((V1 > V2 && V1 - V2 > 8388608) || T2 > T1 + TimeUnit.SECONDS.toNanos(128L)) {
            return true;
        }
        return false;
    }
}
