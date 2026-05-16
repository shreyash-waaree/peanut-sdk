package org.eclipse.californium.core.network;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.eclipse.californium.core.config.CoapConfig;
import org.eclipse.californium.elements.config.Configuration;
import org.eclipse.californium.elements.util.ClockUtil;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-californium-core.jar:org/eclipse/californium/core/network/MapBasedMessageIdTracker.class */
public class MapBasedMessageIdTracker implements MessageIdTracker {
    private final Map<Integer, Long> messageIds;
    private final long exchangeLifetimeNanos;
    private final int min;
    private final int range;
    private int counter;

    public MapBasedMessageIdTracker(int initialMid, int minMid, int maxMid, Configuration config) {
        if (minMid >= maxMid) {
            throw new IllegalArgumentException("max. MID " + maxMid + " must be larger than min. MID " + minMid + "!");
        }
        if (initialMid < minMid || maxMid <= initialMid) {
            throw new IllegalArgumentException("initial MID " + initialMid + " must be in range [" + minMid + "-" + maxMid + ")!");
        }
        this.exchangeLifetimeNanos = config.get(CoapConfig.EXCHANGE_LIFETIME, TimeUnit.NANOSECONDS).longValue();
        this.counter = initialMid - minMid;
        this.min = minMid;
        this.range = maxMid - minMid;
        this.messageIds = new HashMap(this.range);
    }

    @Override // org.eclipse.californium.core.network.MessageIdTracker
    public int getNextMessageId() {
        long now = ClockUtil.nanoRealtime();
        synchronized (this.messageIds) {
            this.counter = (this.counter & 65535) % this.range;
            int end = this.counter + this.range;
            while (this.counter < end) {
                int i = this.counter;
                this.counter = i + 1;
                int idx = i % this.range;
                Long earliestUsage = this.messageIds.get(Integer.valueOf(idx));
                if (earliestUsage == null || earliestUsage.longValue() - now <= 0) {
                    this.messageIds.put(Integer.valueOf(idx), Long.valueOf(now + this.exchangeLifetimeNanos));
                    return idx + this.min;
                }
            }
            String time = TimeUnit.NANOSECONDS.toSeconds(this.exchangeLifetimeNanos) + "s";
            throw new IllegalStateException("No MID available, all [" + this.min + "-" + (this.min + this.range) + ") MIDs in use! (MID lifetime " + time + "!)");
        }
    }
}
