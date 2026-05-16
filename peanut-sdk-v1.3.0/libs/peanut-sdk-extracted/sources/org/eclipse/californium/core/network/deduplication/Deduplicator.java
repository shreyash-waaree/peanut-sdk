package org.eclipse.californium.core.network.deduplication;

import java.util.concurrent.ScheduledExecutorService;
import org.eclipse.californium.core.network.Exchange;
import org.eclipse.californium.core.network.KeyMID;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-californium-core.jar:org/eclipse/californium/core/network/deduplication/Deduplicator.class */
public interface Deduplicator {
    void start();

    void stop();

    void setExecutor(ScheduledExecutorService scheduledExecutorService);

    Exchange findPrevious(KeyMID keyMID, Exchange exchange);

    boolean replacePrevious(KeyMID keyMID, Exchange exchange, Exchange exchange2);

    Exchange find(KeyMID keyMID);

    boolean isEmpty();

    int size();

    void clear();
}
