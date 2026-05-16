package org.eclipse.californium.core.network.stack;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.eclipse.californium.core.network.Exchange;
import org.eclipse.californium.elements.util.NoPublicAPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-californium-core.jar:org/eclipse/californium/core/network/stack/NoResponseCleanupMessageObserver.class */
@NoPublicAPI
public class NoResponseCleanupMessageObserver extends CleanupMessageObserver {
    static final Logger LOGGER = LoggerFactory.getLogger(NoResponseCleanupMessageObserver.class);
    private final ScheduledExecutorService scheduledExecutor;
    private final long lifetime;
    private volatile ScheduledFuture<?> cleanup;

    public NoResponseCleanupMessageObserver(Exchange exchange, ScheduledExecutorService scheduledExecutor, long lifetime) {
        super(exchange);
        this.scheduledExecutor = scheduledExecutor;
        this.lifetime = lifetime;
        LOGGER.debug("no-response observer");
    }

    @Override // org.eclipse.californium.core.coap.MessageObserverAdapter, org.eclipse.californium.core.coap.MessageObserver
    public void onSent(boolean retransmission) {
        LOGGER.debug("no-response sent");
        if (!retransmission) {
            this.cleanup = this.scheduledExecutor.schedule(new Runnable() { // from class: org.eclipse.californium.core.network.stack.NoResponseCleanupMessageObserver.1
                @Override // java.lang.Runnable
                public void run() {
                    NoResponseCleanupMessageObserver.this.exchange.execute(new Runnable() { // from class: org.eclipse.californium.core.network.stack.NoResponseCleanupMessageObserver.1.1
                        @Override // java.lang.Runnable
                        public void run() {
                            NoResponseCleanupMessageObserver.LOGGER.debug("no-response-timeout");
                            NoResponseCleanupMessageObserver.this.exchange.getRequest().setTimedOut(true);
                        }
                    });
                }
            }, this.lifetime, TimeUnit.MILLISECONDS);
        }
    }

    @Override // org.eclipse.californium.core.network.stack.CleanupMessageObserver
    protected void complete(String action) {
        ScheduledFuture<?> cleanup = this.cleanup;
        if (cleanup != null) {
            cleanup.cancel(false);
        }
        super.complete(action);
    }
}
