package org.eclipse.californium.core.network.stack;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.eclipse.californium.core.network.Exchange;
import org.eclipse.californium.elements.util.NoPublicAPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-californium-core.jar:org/eclipse/californium/core/network/stack/MulticastCleanupMessageObserver.class */
@NoPublicAPI
public class MulticastCleanupMessageObserver extends CleanupMessageObserver {
    static final Logger LOGGER = LoggerFactory.getLogger(MulticastCleanupMessageObserver.class);
    private final ScheduledExecutorService scheduledExecutor;
    private final long multicastLifetime;
    private volatile ScheduledFuture<?> cleanup;

    public MulticastCleanupMessageObserver(Exchange exchange, ScheduledExecutorService scheduledExecutor, long multicastLifetime) {
        super(exchange);
        this.scheduledExecutor = scheduledExecutor;
        this.multicastLifetime = multicastLifetime;
    }

    @Override // org.eclipse.californium.core.coap.MessageObserverAdapter, org.eclipse.californium.core.coap.MessageObserver
    public void onSent(boolean retransmission) {
        if (!retransmission) {
            this.cleanup = this.scheduledExecutor.schedule(new Runnable() { // from class: org.eclipse.californium.core.network.stack.MulticastCleanupMessageObserver.1
                @Override // java.lang.Runnable
                public void run() {
                    MulticastCleanupMessageObserver.this.exchange.execute(new Runnable() { // from class: org.eclipse.californium.core.network.stack.MulticastCleanupMessageObserver.1.1
                        @Override // java.lang.Runnable
                        public void run() {
                            if (MulticastCleanupMessageObserver.this.exchange.getResponse() == null) {
                                MulticastCleanupMessageObserver.this.exchange.getRequest().setCanceled(true);
                            } else {
                                MulticastCleanupMessageObserver.this.exchange.setComplete();
                            }
                        }
                    });
                }
            }, this.multicastLifetime, TimeUnit.MILLISECONDS);
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
