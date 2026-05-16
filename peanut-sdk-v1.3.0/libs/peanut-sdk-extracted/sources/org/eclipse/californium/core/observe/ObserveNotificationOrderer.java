package org.eclipse.californium.core.observe;

import java.util.concurrent.atomic.AtomicInteger;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.californium.elements.util.ClockUtil;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-californium-core.jar:org/eclipse/californium/core/observe/ObserveNotificationOrderer.class */
public class ObserveNotificationOrderer {
    private final AtomicInteger number = new AtomicInteger();
    private long nanoTimestamp;

    public ObserveNotificationOrderer() {
    }

    public ObserveNotificationOrderer(Integer observe) {
        if (observe == null) {
            throw new NullPointerException("observe option must not be null!");
        }
        this.number.set(observe.intValue());
        this.nanoTimestamp = ClockUtil.nanoRealtime();
    }

    public int getNextObserveNumber() {
        int iIncrementAndGet = this.number.incrementAndGet();
        while (true) {
            int next = iIncrementAndGet;
            if (next >= 16777216) {
                this.number.compareAndSet(next, 0);
                iIncrementAndGet = this.number.incrementAndGet();
            } else {
                return next;
            }
        }
    }

    public int getCurrent() {
        return this.number.get();
    }

    public synchronized boolean isNew(Response response) {
        Integer observe = response.getOptions().getObserve();
        if (observe == null) {
            return true;
        }
        long T2 = ClockUtil.nanoRealtime();
        if (NotificationOrder.isNew(this.nanoTimestamp, this.number.get(), T2, observe.intValue())) {
            this.nanoTimestamp = T2;
            this.number.set(observe.intValue());
            return true;
        }
        return false;
    }
}
