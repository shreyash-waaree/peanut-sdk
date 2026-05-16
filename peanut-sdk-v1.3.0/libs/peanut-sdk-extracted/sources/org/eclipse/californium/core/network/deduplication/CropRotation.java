package org.eclipse.californium.core.network.deduplication;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.eclipse.californium.core.config.CoapConfig;
import org.eclipse.californium.core.network.Exchange;
import org.eclipse.californium.core.network.KeyMID;
import org.eclipse.californium.elements.config.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-californium-core.jar:org/eclipse/californium/core/network/deduplication/CropRotation.class */
public class CropRotation implements Deduplicator {
    private static final Logger LOGGER = LoggerFactory.getLogger(CropRotation.class);
    private volatile ScheduledFuture<?> jobStatus;
    private volatile int first;
    private volatile int second;
    private final long period;
    private final boolean replace;
    private ScheduledExecutorService executor;
    private final Rotation rotation = new Rotation();
    private final ExchangeMap[] maps = new ExchangeMap[3];

    public CropRotation(Configuration config) {
        this.maps[0] = new ExchangeMap();
        this.maps[1] = new ExchangeMap();
        this.maps[2] = new ExchangeMap();
        this.first = 0;
        this.second = 1;
        this.period = config.get(CoapConfig.CROP_ROTATION_PERIOD, TimeUnit.MILLISECONDS).longValue();
        this.replace = ((Boolean) config.get(CoapConfig.DEDUPLICATOR_AUTO_REPLACE)).booleanValue();
    }

    @Override // org.eclipse.californium.core.network.deduplication.Deduplicator
    public synchronized void start() {
        if (this.jobStatus == null) {
            this.jobStatus = this.executor.scheduleAtFixedRate(this.rotation, this.period, this.period, TimeUnit.MILLISECONDS);
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
        int f = this.first;
        int s = this.second;
        Exchange prev = this.maps[f].putIfAbsent(key, exchange);
        if (prev != null || f == s) {
            return prev;
        }
        Exchange prev2 = this.maps[s].putIfAbsent(key, exchange);
        if (this.replace && prev2 != null && prev2.getOrigin() != exchange.getOrigin()) {
            LOGGER.debug("replace exchange for {}", key);
            prev2 = this.maps[s].replace(key, prev2, exchange) ? null : this.maps[s].putIfAbsent(key, exchange);
        }
        return prev2;
    }

    @Override // org.eclipse.californium.core.network.deduplication.Deduplicator
    public boolean replacePrevious(KeyMID key, Exchange previous, Exchange exchange) {
        int s = this.second;
        return this.maps[s].replace(key, previous, exchange) || this.maps[s].putIfAbsent(key, exchange) == null;
    }

    @Override // org.eclipse.californium.core.network.deduplication.Deduplicator
    public Exchange find(KeyMID key) {
        int f = this.first;
        int s = this.second;
        Exchange prev = this.maps[s].get(key);
        if (prev != null || f == s) {
            return prev;
        }
        return this.maps[f].get(key);
    }

    @Override // org.eclipse.californium.core.network.deduplication.Deduplicator
    public void clear() {
        synchronized (this.maps) {
            this.maps[0].clear();
            this.maps[1].clear();
            this.maps[2].clear();
        }
    }

    @Override // org.eclipse.californium.core.network.deduplication.Deduplicator
    public int size() {
        int size;
        synchronized (this.maps) {
            size = this.maps[0].size() + this.maps[1].size() + this.maps[2].size();
        }
        return size;
    }

    @Override // org.eclipse.californium.core.network.deduplication.Deduplicator
    public boolean isEmpty() {
        for (ExchangeMap map : this.maps) {
            if (!map.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    /* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-californium-core.jar:org/eclipse/californium/core/network/deduplication/CropRotation$Rotation.class */
    private class Rotation implements Runnable {
        private Rotation() {
        }

        @Override // java.lang.Runnable
        public void run() {
            try {
                rotation();
            } catch (Throwable t) {
                CropRotation.LOGGER.warn("Exception in Crop-Rotation algorithm", t);
            }
        }

        private void rotation() {
            synchronized (CropRotation.this.maps) {
                int third = CropRotation.this.first;
                CropRotation.this.first = CropRotation.this.second;
                CropRotation.this.second = (CropRotation.this.second + 1) % 3;
                CropRotation.this.maps[third].clear();
            }
        }
    }

    /* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-californium-core.jar:org/eclipse/californium/core/network/deduplication/CropRotation$ExchangeMap.class */
    private static class ExchangeMap extends ConcurrentHashMap<KeyMID, Exchange> {
        private static final long serialVersionUID = 1504940670839294042L;

        private ExchangeMap() {
        }
    }
}
