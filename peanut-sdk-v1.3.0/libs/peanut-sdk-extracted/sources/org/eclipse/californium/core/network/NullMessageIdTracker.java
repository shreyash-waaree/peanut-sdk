package org.eclipse.californium.core.network;

import java.util.concurrent.atomic.AtomicInteger;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-californium-core.jar:org/eclipse/californium/core/network/NullMessageIdTracker.class */
public class NullMessageIdTracker implements MessageIdTracker {
    private final AtomicInteger currentMID = new AtomicInteger();
    private final int min;
    private final int range;

    public NullMessageIdTracker(int initialMid, int minMid, int maxMid) {
        if (minMid >= maxMid) {
            throw new IllegalArgumentException("max. MID " + maxMid + " must be larger than min. MID " + minMid + "!");
        }
        if (initialMid < minMid || maxMid <= initialMid) {
            throw new IllegalArgumentException("initial MID " + initialMid + " must be in range [" + minMid + "-" + maxMid + ")!");
        }
        this.currentMID.set(initialMid - minMid);
        this.min = minMid;
        this.range = maxMid - minMid;
    }

    @Override // org.eclipse.californium.core.network.MessageIdTracker
    public int getNextMessageId() {
        int mid = this.currentMID.getAndIncrement();
        int result = mid % this.range;
        if (result == this.range - 1) {
            this.currentMID.addAndGet(-this.range);
        }
        return this.min + mid;
    }
}
