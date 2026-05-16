package org.eclipse.californium.core.coap;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-californium-core.jar:org/eclipse/californium/core/coap/ResponseTimeout.class */
public class ResponseTimeout extends MessageObserverAdapter implements Runnable {
    private static final Logger LOGGER = LoggerFactory.getLogger(ResponseTimeout.class);
    private final AtomicReference<ScheduledFuture<?>> responseTimeout = new AtomicReference<>();
    private final Request request;
    private final ScheduledExecutorService executor;
    private final long timeout;

    public ResponseTimeout(Request request, long timeout, ScheduledExecutorService executor) {
        this.request = request;
        this.executor = executor;
        this.timeout = timeout;
    }

    private void scheduleTimeout() {
        ScheduledFuture<?> schedule = this.executor.schedule(this, this.timeout, TimeUnit.MILLISECONDS);
        ScheduledFuture<?> previous = this.responseTimeout.getAndSet(schedule);
        if (previous != null) {
            previous.cancel(false);
        }
    }

    private void cancelTimeout() {
        ScheduledFuture<?> schedule = this.responseTimeout.getAndSet(null);
        if (schedule != null) {
            schedule.cancel(false);
        }
    }

    @Override // org.eclipse.californium.core.coap.MessageObserverAdapter, org.eclipse.californium.core.coap.MessageObserver
    public void onSent(boolean retransmission) {
        if (!retransmission) {
            if ((!this.request.isConfirmable() || !this.request.hasMID()) && this.request.getResponse() == null) {
                LOGGER.trace("start non-response timeout {}", Long.valueOf(this.timeout));
                scheduleTimeout();
            }
        }
    }

    @Override // org.eclipse.californium.core.coap.MessageObserverAdapter, org.eclipse.californium.core.coap.MessageObserver
    public void onAcknowledgement() {
        if (this.request.isConfirmable() && this.request.getResponse() == null) {
            LOGGER.trace("start con-response timeout {}", Long.valueOf(this.timeout));
            scheduleTimeout();
        }
    }

    @Override // org.eclipse.californium.core.coap.MessageObserverAdapter, org.eclipse.californium.core.coap.MessageObserver
    public void onResponse(Response response) {
        cancelTimeout();
    }

    @Override // org.eclipse.californium.core.coap.MessageObserverAdapter
    protected void failed() {
        cancelTimeout();
    }

    @Override // java.lang.Runnable
    public void run() {
        if (this.request.getResponse() == null) {
            LOGGER.trace("response timeout!");
            this.request.setTimedOut(true);
        }
    }
}
