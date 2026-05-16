package org.eclipse.californium.core.network;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import org.eclipse.californium.core.config.CoapConfig;
import org.eclipse.californium.elements.config.Configuration;
import org.eclipse.californium.elements.util.ClockUtil;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-californium-core.jar:org/eclipse/californium/core/network/GroupedMessageIdTracker.class */
public class GroupedMessageIdTracker implements MessageIdTracker {
    private final int numberOfGroups;
    private final int sizeOfGroups;
    private final int min;
    private final int range;
    private final long exchangeLifetimeNanos;
    private final long[] midLease;
    private int currentMID;

    public GroupedMessageIdTracker(int initialMid, int minMid, int maxMid, Configuration config) {
        if (minMid >= maxMid) {
            throw new IllegalArgumentException("max. MID " + maxMid + " must be larger than min. MID " + minMid + "!");
        }
        if (initialMid < minMid || maxMid <= initialMid) {
            throw new IllegalArgumentException("initial MID " + initialMid + " must be in range [" + minMid + "-" + maxMid + ")!");
        }
        this.exchangeLifetimeNanos = config.get(CoapConfig.EXCHANGE_LIFETIME, TimeUnit.NANOSECONDS).longValue();
        this.currentMID = initialMid - minMid;
        this.min = minMid;
        this.range = maxMid - minMid;
        this.numberOfGroups = ((Integer) config.get(CoapConfig.MID_TRACKER_GROUPS)).intValue();
        this.sizeOfGroups = ((this.range + this.numberOfGroups) - 1) / this.numberOfGroups;
        this.midLease = new long[this.numberOfGroups];
        Arrays.fill(this.midLease, ClockUtil.nanoRealtime() - 1000);
    }

    @Override // org.eclipse.californium.core.network.MessageIdTracker
    public int getNextMessageId() {
        long now = ClockUtil.nanoRealtime();
        synchronized (this) {
            int mid = (this.currentMID & 65535) % this.range;
            int index = mid / this.sizeOfGroups;
            int nextIndex = (index + 1) % this.numberOfGroups;
            if (this.midLease[nextIndex] - now < 0) {
                this.midLease[index] = now + this.exchangeLifetimeNanos;
                this.currentMID = mid + 1;
                return mid + this.min;
            }
            String time = TimeUnit.NANOSECONDS.toSeconds(this.exchangeLifetimeNanos) + "s";
            throw new IllegalStateException("No MID available, all [" + this.min + "-" + (this.min + this.range) + ") MID-groups in use! (MID lifetime " + time + "!)");
        }
    }

    public int getGroupSize() {
        return this.sizeOfGroups;
    }
}
