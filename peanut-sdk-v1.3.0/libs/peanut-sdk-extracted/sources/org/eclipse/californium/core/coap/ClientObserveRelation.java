package org.eclipse.californium.core.coap;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.eclipse.californium.core.config.CoapConfig;
import org.eclipse.californium.core.network.Endpoint;
import org.eclipse.californium.core.observe.ObserveNotificationOrderer;
import org.eclipse.californium.elements.EndpointContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-californium-core.jar:org/eclipse/californium/core/coap/ClientObserveRelation.class */
public class ClientObserveRelation {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClientObserveRelation.class);
    private final ScheduledThreadPoolExecutor scheduler;
    protected final Endpoint endpoint;
    private final long reregistrationBackoffMillis;
    private volatile Request request;
    private final AtomicBoolean requestPending = new AtomicBoolean(true);
    private final AtomicReference<ScheduledFuture<?>> reregistrationHandle = new AtomicReference<>();
    private volatile boolean canceled = false;
    private volatile boolean proactiveCancel = false;
    private volatile Response current = null;
    private final Runnable reregister = new Runnable() { // from class: org.eclipse.californium.core.coap.ClientObserveRelation.1
        @Override // java.lang.Runnable
        public void run() {
            ClientObserveRelation.this.reregister();
        }
    };
    private final MessageObserver pendingRequestObserver = new MessageObserverAdapter() { // from class: org.eclipse.californium.core.coap.ClientObserveRelation.2
        @Override // org.eclipse.californium.core.coap.MessageObserverAdapter, org.eclipse.californium.core.coap.MessageObserver
        public void onResponse(Response response) {
            next();
        }

        @Override // org.eclipse.californium.core.coap.MessageObserverAdapter, org.eclipse.californium.core.coap.MessageObserver
        public void onCancel() {
            next();
        }

        @Override // org.eclipse.californium.core.coap.MessageObserverAdapter
        protected void failed() {
            next();
        }

        private void next() {
            if (ClientObserveRelation.this.proactiveCancel) {
                ClientObserveRelation.this.sendCancelObserve();
            } else {
                ClientObserveRelation.this.requestPending.set(false);
            }
        }
    };
    private volatile ObserveNotificationOrderer orderer = new ObserveNotificationOrderer();

    public ClientObserveRelation(Request request, Endpoint endpoint, ScheduledThreadPoolExecutor executor) {
        this.request = request;
        this.endpoint = endpoint;
        this.reregistrationBackoffMillis = endpoint.getConfig().get(CoapConfig.NOTIFICATION_REREGISTRATION_BACKOFF, TimeUnit.MILLISECONDS).longValue();
        this.scheduler = executor;
        this.request.addMessageObserver(this.pendingRequestObserver);
        this.request.setProtectFromOffload();
    }

    public boolean reregister() {
        Request request = this.request;
        if (request.isCanceled()) {
            throw new IllegalStateException("observe request already canceled! token " + request.getTokenString());
        }
        Response response = this.current;
        if (response != null && !response.getOptions().hasObserve()) {
            throw new IllegalStateException("observe not supported by CoAP server!");
        }
        if (isCanceled()) {
            throw new IllegalStateException("observe already canceled!");
        }
        if (this.requestPending.compareAndSet(false, true)) {
            Request refresh = Request.newGet();
            EndpointContext destinationContext = response != null ? response.getSourceContext() : request.getDestinationContext();
            refresh.setDestinationContext(destinationContext);
            refresh.setToken(request.getToken());
            refresh.setOptions(request.getOptions());
            refresh.setMaxResourceBodySize(request.getMaxResourceBodySize());
            if (request.isUnintendedPayload()) {
                refresh.setUnintendedPayload();
                refresh.setPayload(request.getPayload());
            }
            for (MessageObserver observer : request.getMessageObservers()) {
                if (!observer.isInternal()) {
                    request.removeMessageObserver(observer);
                    refresh.addMessageObserver(observer);
                }
            }
            this.request = refresh;
            this.orderer = new ObserveNotificationOrderer();
            this.endpoint.sendRequest(refresh);
            return true;
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void sendCancelObserve() {
        this.proactiveCancel = false;
        Response response = this.current;
        Request request = this.request;
        EndpointContext destinationContext = response != null ? response.getSourceContext() : request.getDestinationContext();
        Request cancel = Request.newGet();
        cancel.setDestinationContext(destinationContext);
        cancel.setToken(request.getToken());
        cancel.setOptions(request.getOptions());
        cancel.setObserveCancel();
        cancel.setMaxResourceBodySize(request.getMaxResourceBodySize());
        if (request.isUnintendedPayload()) {
            cancel.setUnintendedPayload();
            cancel.setPayload(request.getPayload());
        }
        for (MessageObserver observer : request.getMessageObservers()) {
            if (!observer.isInternal()) {
                request.removeMessageObserver(observer);
                cancel.addMessageObserver(observer);
            }
        }
        this.endpoint.sendRequest(cancel);
    }

    private void cancel() {
        this.endpoint.cancelObservation(this.request.getToken());
        setCanceled(true);
    }

    public void proactiveCancel() {
        cancel();
        this.proactiveCancel = true;
        if (this.requestPending.compareAndSet(false, true)) {
            sendCancelObserve();
        }
    }

    public void reactiveCancel() {
        Request request = this.request;
        if (CoAP.isTcpScheme(request.getScheme())) {
            LOGGER.info("change to cancel the observe {} proactive over TCP.", request.getTokenString());
            proactiveCancel();
        } else {
            request.cancel();
            cancel();
        }
    }

    public boolean isCanceled() {
        return this.canceled;
    }

    public Response getCurrentResponse() {
        return this.current;
    }

    public Request getRequest() {
        return this.request;
    }

    protected void setCanceled(boolean canceled) {
        this.canceled = canceled;
        if (this.canceled) {
            setReregistrationHandle(null);
        }
    }

    public boolean onResponse(Response response) {
        boolean isNew = false;
        if (null != response) {
            Integer observe = response.getOptions().getObserve();
            boolean prepareNext = (observe == null || isCanceled()) ? false : true;
            isNew = this.orderer.isNew(response);
            if (isNew) {
                this.current = response;
                LOGGER.debug("Updated with {}", response);
            } else if (prepareNext) {
                prepareNext = this.orderer.getCurrent() == observe.intValue();
            }
            if (prepareNext) {
                prepareReregistration(response);
            } else if (observe == null && !isCanceled()) {
                cancel();
            }
        }
        return isNew;
    }

    public boolean matchRequest(Request request) {
        return this.request.getToken().equals(request.getToken());
    }

    private void setReregistrationHandle(ScheduledFuture<?> reregistrationHandle) {
        ScheduledFuture<?> previousHandle = this.reregistrationHandle.getAndSet(reregistrationHandle);
        if (previousHandle != null) {
            if (previousHandle instanceof Runnable) {
                this.scheduler.remove((Runnable) previousHandle);
            } else {
                previousHandle.cancel(false);
            }
        }
    }

    private void prepareReregistration(Response response) {
        long timeout = TimeUnit.SECONDS.toMillis(response.getOptions().getMaxAge().longValue()) + this.reregistrationBackoffMillis;
        ScheduledFuture<?> f = this.scheduler.schedule(this.reregister, timeout, TimeUnit.MILLISECONDS);
        setReregistrationHandle(f);
        LOGGER.debug("Wait for {}ms fresh notifies.", Long.valueOf(timeout));
    }
}
