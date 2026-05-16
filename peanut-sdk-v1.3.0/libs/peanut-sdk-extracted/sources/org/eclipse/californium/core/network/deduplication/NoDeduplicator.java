package org.eclipse.californium.core.network.deduplication;

import java.util.concurrent.ScheduledExecutorService;
import org.eclipse.californium.core.network.Exchange;
import org.eclipse.californium.core.network.KeyMID;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-californium-core.jar:org/eclipse/californium/core/network/deduplication/NoDeduplicator.class */
public class NoDeduplicator implements Deduplicator {
    @Override // org.eclipse.californium.core.network.deduplication.Deduplicator
    public void start() {
    }

    @Override // org.eclipse.californium.core.network.deduplication.Deduplicator
    public void stop() {
    }

    @Override // org.eclipse.californium.core.network.deduplication.Deduplicator
    public void setExecutor(ScheduledExecutorService executor) {
    }

    @Override // org.eclipse.californium.core.network.deduplication.Deduplicator
    public Exchange findPrevious(KeyMID key, Exchange exchange) {
        return null;
    }

    @Override // org.eclipse.californium.core.network.deduplication.Deduplicator
    public boolean replacePrevious(KeyMID key, Exchange exchange, Exchange previous) {
        return true;
    }

    @Override // org.eclipse.californium.core.network.deduplication.Deduplicator
    public Exchange find(KeyMID key) {
        return null;
    }

    @Override // org.eclipse.californium.core.network.deduplication.Deduplicator
    public void clear() {
    }

    @Override // org.eclipse.californium.core.network.deduplication.Deduplicator
    public boolean isEmpty() {
        return true;
    }

    @Override // org.eclipse.californium.core.network.deduplication.Deduplicator
    public int size() {
        return 0;
    }
}
