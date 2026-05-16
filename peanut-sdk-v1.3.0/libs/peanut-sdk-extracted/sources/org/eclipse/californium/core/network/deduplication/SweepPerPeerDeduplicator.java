package org.eclipse.californium.core.network.deduplication;

import java.util.Iterator;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import org.eclipse.californium.core.config.CoapConfig;
import org.eclipse.californium.core.network.KeyMID;
import org.eclipse.californium.core.network.deduplication.SweepDeduplicator;
import org.eclipse.californium.elements.config.Configuration;
import org.eclipse.californium.elements.util.ClockUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-californium-core.jar:org/eclipse/californium/core/network/deduplication/SweepPerPeerDeduplicator.class */
public final class SweepPerPeerDeduplicator extends SweepDeduplicator {
    private static final Logger LOGGER = LoggerFactory.getLogger(SweepPerPeerDeduplicator.class);
    private final ConcurrentMap<Object, Queue<KeyMID>> incomingPerPeerMessages;
    private final int messagePerPeer;

    public SweepPerPeerDeduplicator(Configuration config) {
        super(config);
        this.incomingPerPeerMessages = new ConcurrentHashMap();
        this.algorithm = new SweepAlgorithm();
        this.messagePerPeer = ((Integer) config.get(CoapConfig.PEERS_MARK_AND_SWEEP_MESSAGES)).intValue();
    }

    @Override // org.eclipse.californium.core.network.deduplication.SweepDeduplicator
    protected void onAdd(KeyMID key, boolean replace) {
        Object peer = key.getPeer();
        Queue<KeyMID> peersQueue = this.incomingPerPeerMessages.get(peer);
        if (peersQueue == null) {
            Queue<KeyMID> peersQueue2 = new ArrayBlockingQueue<>(this.messagePerPeer);
            peersQueue2.add(key);
            Queue<KeyMID> temp = this.incomingPerPeerMessages.putIfAbsent(peer, peersQueue2);
            if (temp == null) {
                return;
            } else {
                peersQueue = temp;
            }
        }
        if (replace) {
            peersQueue.remove(key);
        }
        while (!peersQueue.offer(key)) {
            KeyMID oldest = peersQueue.poll();
            this.incomingMessages.remove(oldest);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void removeSame(Queue<KeyMID> peersQueue, KeyMID key) {
        Iterator<KeyMID> iterator = peersQueue.iterator();
        while (iterator.hasNext()) {
            if (iterator.next() == key) {
                iterator.remove();
                return;
            }
        }
    }

    @Override // org.eclipse.californium.core.network.deduplication.SweepDeduplicator, org.eclipse.californium.core.network.deduplication.Deduplicator
    public void clear() {
        super.clear();
        this.incomingPerPeerMessages.clear();
    }

    /* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-californium-core.jar:org/eclipse/californium/core/network/deduplication/SweepPerPeerDeduplicator$SweepAlgorithm.class */
    private class SweepAlgorithm implements Runnable {
        private int lastSizeDiff;

        private SweepAlgorithm() {
        }

        @Override // java.lang.Runnable
        public void run() {
            try {
                SweepPerPeerDeduplicator.LOGGER.trace("Start Mark-And-Sweep with {} entries", Integer.valueOf(SweepPerPeerDeduplicator.this.incomingMessages.size()));
                sweep();
            } catch (Throwable t) {
                SweepPerPeerDeduplicator.LOGGER.warn("Exception in Mark-and-Sweep algorithm", t);
            }
        }

        private void sweep() {
            if (!SweepPerPeerDeduplicator.this.incomingMessages.isEmpty()) {
                long start = ClockUtil.nanoRealtime();
                long oldestAllowed = start - TimeUnit.MILLISECONDS.toNanos(SweepPerPeerDeduplicator.this.exchangeLifetime);
                int size = SweepPerPeerDeduplicator.this.incomingMessages.size();
                int queueSize = 0;
                int missingExchanges = 0;
                for (Map.Entry<?, Queue<KeyMID>> entry : SweepPerPeerDeduplicator.this.incomingPerPeerMessages.entrySet()) {
                    Queue<KeyMID> queue = entry.getValue();
                    if (queue.isEmpty()) {
                        SweepPerPeerDeduplicator.this.incomingPerPeerMessages.remove(entry.getKey());
                    } else {
                        queueSize += queue.size();
                        while (true) {
                            KeyMID key = queue.peek();
                            if (key == null) {
                                break;
                            }
                            SweepDeduplicator.DedupExchange exchange = SweepPerPeerDeduplicator.this.incomingMessages.get(key);
                            long diff = exchange == null ? -1L : exchange.nanoTimestamp - oldestAllowed;
                            if (diff >= 0) {
                                if (SweepPerPeerDeduplicator.LOGGER.isTraceEnabled()) {
                                    SweepPerPeerDeduplicator.LOGGER.trace("Time left {}ms", Long.valueOf(TimeUnit.NANOSECONDS.toMillis(diff)));
                                }
                            } else {
                                if (exchange != null) {
                                    SweepPerPeerDeduplicator.this.incomingMessages.remove(key, exchange);
                                    SweepPerPeerDeduplicator.LOGGER.trace("Mark-And-Sweep removes {}", key);
                                } else {
                                    missingExchanges++;
                                }
                                SweepPerPeerDeduplicator.this.removeSame(queue, key);
                            }
                        }
                    }
                }
                SweepPerPeerDeduplicator.LOGGER.debug("Sweep run took {}ms", Long.valueOf(TimeUnit.NANOSECONDS.toMillis(ClockUtil.nanoRealtime() - start)));
                if (missingExchanges > 0) {
                    SweepPerPeerDeduplicator.LOGGER.warn("{} exchanges missing", Integer.valueOf(missingExchanges));
                }
                int diff2 = size - queueSize;
                if (Math.abs(this.lastSizeDiff) > 1000 && Math.abs(diff2) > 1000) {
                    SweepPerPeerDeduplicator.LOGGER.info("Map size {} differs from queues size {}!", Integer.valueOf(size), Integer.valueOf(queueSize));
                }
                this.lastSizeDiff = diff2;
            }
        }
    }
}
