package org.eclipse.californium.core.network;

import java.net.InetSocketAddress;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import org.eclipse.californium.core.config.CoapConfig;
import org.eclipse.californium.elements.config.Configuration;
import org.eclipse.californium.elements.util.ClockUtil;
import org.eclipse.californium.elements.util.LeastRecentlyUsedCache;
import org.eclipse.californium.elements.util.NetworkInterfacesUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-californium-core.jar:org/eclipse/californium/core/network/InMemoryMessageIdProvider.class */
public class InMemoryMessageIdProvider implements MessageIdProvider {
    private static final Logger LOG = LoggerFactory.getLogger(InMemoryMessageIdProvider.class);
    private final LeastRecentlyUsedCache<InetSocketAddress, MessageIdTracker> trackers;
    private final MessageIdTracker multicastTracker;
    private final CoapConfig.TrackerMode mode;
    private final Random random;
    private final Configuration config;
    private final int multicastBaseMid;

    public InMemoryMessageIdProvider(Configuration config) {
        if (config == null) {
            throw new NullPointerException("Config must not be null");
        }
        CoapConfig.TrackerMode mode = (CoapConfig.TrackerMode) config.get(CoapConfig.MID_TRACKER);
        this.mode = mode;
        this.config = config;
        if (((Boolean) config.get(CoapConfig.USE_RANDOM_MID_START)).booleanValue()) {
            this.random = new Random(ClockUtil.nanoRealtime());
        } else {
            this.random = null;
        }
        this.trackers = new LeastRecentlyUsedCache<>(((Integer) config.get(CoapConfig.MAX_ACTIVE_PEERS)).intValue(), config.get(CoapConfig.MAX_PEER_INACTIVITY_PERIOD, TimeUnit.SECONDS).longValue());
        this.trackers.setEvictingOnReadAccess(false);
        int multicastBaseMid = ((Integer) config.get(CoapConfig.MULTICAST_BASE_MID)).intValue();
        if (0 < multicastBaseMid) {
            this.multicastBaseMid = multicastBaseMid;
            int mid = null == this.random ? multicastBaseMid : this.random.nextInt(MessageIdTracker.TOTAL_NO_OF_MIDS - multicastBaseMid) + multicastBaseMid;
            switch (mode) {
                case NULL:
                    this.multicastTracker = new NullMessageIdTracker(mid, multicastBaseMid, MessageIdTracker.TOTAL_NO_OF_MIDS);
                    return;
                case MAPBASED:
                    this.multicastTracker = new MapBasedMessageIdTracker(mid, multicastBaseMid, MessageIdTracker.TOTAL_NO_OF_MIDS, config);
                    return;
                case GROUPED:
                default:
                    this.multicastTracker = new GroupedMessageIdTracker(mid, multicastBaseMid, MessageIdTracker.TOTAL_NO_OF_MIDS, config);
                    return;
            }
        }
        this.multicastBaseMid = MessageIdTracker.TOTAL_NO_OF_MIDS;
        this.multicastTracker = null;
    }

    @Override // org.eclipse.californium.core.network.MessageIdProvider
    public int getNextMessageId(InetSocketAddress destination) {
        MessageIdTracker tracker = getTracker(destination);
        if (tracker == null) {
            String time = this.trackers.getExpirationThreshold() + "s";
            throw new IllegalStateException("No MID available, max. peers " + this.trackers.size() + " exhausted! (Timeout " + time + ".)");
        }
        return tracker.getNextMessageId();
    }

    private synchronized MessageIdTracker getTracker(InetSocketAddress destination) {
        MessageIdTracker tracker;
        if (NetworkInterfacesUtil.isMultiAddress(destination.getAddress())) {
            if (this.multicastTracker == null) {
                LOG.warn("Destination address {} is a multicast address, please configure NetworkConfig to support multicast messaging", destination);
            }
            return this.multicastTracker;
        }
        MessageIdTracker tracker2 = this.trackers.get(destination);
        if (tracker2 == null) {
            int mid = null == this.random ? 0 : this.random.nextInt(this.multicastBaseMid);
            switch (this.mode) {
                case NULL:
                    tracker = new NullMessageIdTracker(mid, 0, this.multicastBaseMid);
                    break;
                case MAPBASED:
                    tracker = new MapBasedMessageIdTracker(mid, 0, this.multicastBaseMid, this.config);
                    break;
                case GROUPED:
                default:
                    tracker = new GroupedMessageIdTracker(mid, 0, this.multicastBaseMid, this.config);
                    break;
            }
            if (this.trackers.put(destination, tracker)) {
                return tracker;
            }
            return null;
        }
        return tracker2;
    }
}
