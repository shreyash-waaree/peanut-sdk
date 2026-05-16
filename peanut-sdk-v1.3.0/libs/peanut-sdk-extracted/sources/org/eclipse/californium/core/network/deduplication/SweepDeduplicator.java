package org.eclipse.californium.core.network.deduplication;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.eclipse.californium.core.config.CoapConfig;
import org.eclipse.californium.core.network.Exchange;
import org.eclipse.californium.core.network.KeyMID;
import org.eclipse.californium.elements.config.Configuration;
import org.eclipse.californium.elements.util.ClockUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-californium-core.jar:org/eclipse/californium/core/network/deduplication/SweepDeduplicator.class */
public class SweepDeduplicator implements Deduplicator {
    private static final Logger LOGGER = LoggerFactory.getLogger(SweepDeduplicator.class);
    final ConcurrentMap<KeyMID, DedupExchange> incomingMessages = new ConcurrentHashMap();
    final long exchangeLifetime;
    final boolean replace;
    Runnable algorithm;
    private final long sweepInterval;
    private volatile ScheduledFuture<?> jobStatus;
    private ScheduledExecutorService executor;

    /* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-californium-core.jar:org/eclipse/californium/core/network/deduplication/SweepDeduplicator$DedupExchange.class */
    static class DedupExchange {
        public final long nanoTimestamp = ClockUtil.nanoRealtime();
        public final Exchange exchange;

        public DedupExchange(Exchange exchange) {
            this.exchange = exchange;
        }

        public int hashCode() {
            return this.exchange.hashCode();
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            DedupExchange other = (DedupExchange) obj;
            return this.exchange.equals(other.exchange);
        }
    }

    public SweepDeduplicator(Configuration config) {
        this.sweepInterval = config.get(CoapConfig.MARK_AND_SWEEP_INTERVAL, TimeUnit.MILLISECONDS).longValue();
        this.exchangeLifetime = config.get(CoapConfig.EXCHANGE_LIFETIME, TimeUnit.MILLISECONDS).longValue();
        this.replace = ((Boolean) config.get(CoapConfig.DEDUPLICATOR_AUTO_REPLACE)).booleanValue();
    }

    @Override // org.eclipse.californium.core.network.deduplication.Deduplicator
    public synchronized void start() {
        if (this.algorithm == null) {
            this.algorithm = new SweepAlgorithm();
        }
        if (this.jobStatus == null) {
            this.jobStatus = this.executor.scheduleAtFixedRate(this.algorithm, this.sweepInterval, this.sweepInterval, TimeUnit.MILLISECONDS);
        }
    }

    @Override // org.eclipse.californium.core.network.deduplication.Deduplicator
    public synchronized void stop() {
        if (this.jobStatus != null) {
            this.jobStatus.cancel(false);
            this.jobStatus = null;
            clear();
        }
    }

    @Override // org.eclipse.californium.core.network.deduplication.Deduplicator
    public synchronized void setExecutor(ScheduledExecutorService executor) {
        if (this.jobStatus != null) {
            throw new IllegalStateException("executor service can not be set on running Deduplicator");
        }
        this.executor = executor;
    }

    @Override // org.eclipse.californium.core.network.deduplication.Deduplicator
    public Exchange findPrevious(KeyMID key, Exchange exchange) {
        DedupExchange current = new DedupExchange(exchange);
        DedupExchange previous = this.incomingMessages.putIfAbsent(key, current);
        boolean replaced = false;
        if (this.replace && previous != null && previous.exchange.getOrigin() != exchange.getOrigin()) {
            if (this.incomingMessages.replace(key, previous, current)) {
                LOGGER.debug("replace exchange for {}", key);
                previous = null;
                replaced = true;
            } else {
                previous = this.incomingMessages.putIfAbsent(key, current);
            }
        }
        if (previous == null) {
            LOGGER.debug("add exchange for {}", key);
            onAdd(key, replaced);
            return null;
        }
        LOGGER.debug("found exchange for {}", key);
        return previous.exchange;
    }

    @Override // org.eclipse.californium.core.network.deduplication.Deduplicator
    public boolean replacePrevious(KeyMID key, Exchange previous, Exchange exchange) {
        boolean replaced = true;
        boolean result = true;
        DedupExchange prev = new DedupExchange(previous);
        DedupExchange current = new DedupExchange(exchange);
        if (!this.incomingMessages.replace(key, prev, current)) {
            replaced = false;
            result = this.incomingMessages.putIfAbsent(key, current) == null;
        }
        if (result) {
            onAdd(key, replaced);
        }
        return result;
    }

    @Override // org.eclipse.californium.core.network.deduplication.Deduplicator
    public Exchange find(KeyMID key) {
        DedupExchange previous = this.incomingMessages.get(key);
        if (null == previous) {
            return null;
        }
        return previous.exchange;
    }

    @Override // org.eclipse.californium.core.network.deduplication.Deduplicator
    public void clear() {
        this.incomingMessages.clear();
    }

    @Override // org.eclipse.californium.core.network.deduplication.Deduplicator
    public boolean isEmpty() {
        return this.incomingMessages.isEmpty();
    }

    @Override // org.eclipse.californium.core.network.deduplication.Deduplicator
    public int size() {
        return this.incomingMessages.size();
    }

    protected void onAdd(KeyMID key, boolean replace) {
    }

    /* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-californium-core.jar:org/eclipse/californium/core/network/deduplication/SweepDeduplicator$SweepAlgorithm.class */
    private class SweepAlgorithm implements Runnable {
        private SweepAlgorithm() {
        }

        @Override // java.lang.Runnable
        public void run() {
            try {
                SweepDeduplicator.LOGGER.trace("Start Mark-And-Sweep with {} entries", Integer.valueOf(SweepDeduplicator.this.incomingMessages.size()));
                sweep();
            } catch (Throwable t) {
                SweepDeduplicator.LOGGER.warn("Exception in Mark-and-Sweep algorithm", t);
            }
        }

        private void sweep() {
            if (!SweepDeduplicator.this.incomingMessages.isEmpty()) {
                long start = ClockUtil.nanoRealtime();
                long oldestAllowed = start - TimeUnit.MILLISECONDS.toNanos(SweepDeduplicator.this.exchangeLifetime);
                for (Map.Entry<?, DedupExchange> entry : SweepDeduplicator.this.incomingMessages.entrySet()) {
                    DedupExchange exchange = entry.getValue();
                    if (exchange.nanoTimestamp - oldestAllowed < 0) {
                        SweepDeduplicator.LOGGER.trace("Mark-And-Sweep removes {}", entry.getKey());
                        SweepDeduplicator.this.incomingMessages.remove(entry.getKey());
                    }
                }
                SweepDeduplicator.LOGGER.debug("Sweep run took {}ms", Long.valueOf(TimeUnit.NANOSECONDS.toMillis(ClockUtil.nanoRealtime() - start)));
            }
        }
    }
}
