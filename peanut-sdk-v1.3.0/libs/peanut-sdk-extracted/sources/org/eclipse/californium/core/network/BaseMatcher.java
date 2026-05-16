package org.eclipse.californium.core.network;

import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import org.eclipse.californium.core.coap.MessageObserverAdapter;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.californium.core.coap.Token;
import org.eclipse.californium.core.network.Exchange;
import org.eclipse.californium.core.network.TokenGenerator;
import org.eclipse.californium.core.observe.NotificationListener;
import org.eclipse.californium.core.observe.Observation;
import org.eclipse.californium.core.observe.ObservationStore;
import org.eclipse.californium.elements.EndpointContext;
import org.eclipse.californium.elements.EndpointIdentityResolver;
import org.eclipse.californium.elements.config.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-californium-core.jar:org/eclipse/californium/core/network/BaseMatcher.class */
public abstract class BaseMatcher implements Matcher {
    private static final Logger LOG = LoggerFactory.getLogger(BaseMatcher.class);
    protected final Configuration config;
    protected final ObservationStore observationStore;
    protected final MessageExchangeStore exchangeStore;
    protected final TokenGenerator tokenGenerator;
    protected final Executor executor;
    protected boolean running = false;
    private final NotificationListener notificationListener;
    private final EndpointIdentityResolver identityResolver;

    public BaseMatcher(Configuration config, NotificationListener notificationListener, TokenGenerator tokenGenerator, ObservationStore observationStore, MessageExchangeStore exchangeStore, EndpointIdentityResolver identityResolver, Executor executor) {
        if (config == null) {
            throw new NullPointerException("Config must not be null");
        }
        if (notificationListener == null) {
            throw new NullPointerException("NotificationListener must not be null");
        }
        if (tokenGenerator == null) {
            throw new NullPointerException("TokenGenerator must not be null");
        }
        if (exchangeStore == null) {
            throw new NullPointerException("MessageExchangeStore must not be null");
        }
        if (observationStore == null) {
            throw new NullPointerException("ObservationStore must not be null");
        }
        if (identityResolver == null) {
            throw new NullPointerException("EndpointIdentityResolver must not be null");
        }
        if (executor == null) {
            throw new NullPointerException("Executor must not be null");
        }
        this.config = config;
        this.notificationListener = notificationListener;
        this.exchangeStore = exchangeStore;
        this.observationStore = observationStore;
        this.tokenGenerator = tokenGenerator;
        this.identityResolver = identityResolver;
        this.executor = executor;
    }

    @Override // org.eclipse.californium.core.network.Matcher
    public synchronized void start() {
        if (!this.running) {
            this.exchangeStore.start();
            this.observationStore.start();
            this.running = true;
        }
    }

    @Override // org.eclipse.californium.core.network.Matcher
    public synchronized void stop() {
        if (this.running) {
            this.exchangeStore.stop();
            this.observationStore.stop();
            clear();
            this.running = false;
        }
    }

    @Override // org.eclipse.californium.core.network.Matcher
    public void clear() {
    }

    protected final void registerObserve(Request request) {
        if (!request.getOptions().hasBlock2() || request.getOptions().getBlock2().getNum() == 0) {
            LOG.debug("registering observe request {}", request);
            Token token = request.getToken();
            if (token == null) {
                do {
                    token = this.tokenGenerator.createToken(TokenGenerator.Scope.LONG_TERM);
                    request.setToken(token);
                } while (this.observationStore.putIfAbsent(token, new Observation(request, null)) != null);
            } else {
                this.observationStore.put(token, new Observation(request, null));
            }
            request.addMessageObserver(new ObservationObserverAdapter(token) { // from class: org.eclipse.californium.core.network.BaseMatcher.1
                @Override // org.eclipse.californium.core.coap.MessageObserverAdapter, org.eclipse.californium.core.coap.MessageObserver
                public void onCancel() {
                    remove();
                }

                @Override // org.eclipse.californium.core.coap.MessageObserverAdapter
                protected void failed() {
                    remove();
                }

                @Override // org.eclipse.californium.core.coap.MessageObserverAdapter, org.eclipse.californium.core.coap.MessageObserver
                public void onContextEstablished(EndpointContext endpointContext) {
                    BaseMatcher.this.observationStore.setContext(this.token, endpointContext);
                }
            });
        }
    }

    protected final Exchange matchNotifyResponse(Response response) {
        Token token;
        Observation obs;
        Exchange exchange = null;
        if ((!response.isSuccess() || response.getOptions().hasObserve()) && (obs = this.observationStore.get((token = response.getToken()))) != null) {
            final Request request = obs.getRequest();
            Object identity = this.identityResolver.getEndpointIdentity(response.getSourceContext());
            exchange = new Exchange(request, identity, Exchange.Origin.LOCAL, this.executor, obs.getContext(), true);
            LOG.debug("re-created exchange from original observe request: {}", request);
            request.addMessageObserver(new ObservationObserverAdapter(token) { // from class: org.eclipse.californium.core.network.BaseMatcher.2
                @Override // org.eclipse.californium.core.network.BaseMatcher.ObservationObserverAdapter, org.eclipse.californium.core.coap.MessageObserverAdapter, org.eclipse.californium.core.coap.MessageObserver
                public void onResponse(Response response2) {
                    try {
                        BaseMatcher.this.notificationListener.onNotification(request, response2);
                    } finally {
                        if (!response2.isNotification()) {
                            BaseMatcher.LOG.debug("observation with token {} removed, removing from observation store", this.token);
                            remove();
                        }
                    }
                }
            });
        }
        return exchange;
    }

    @Override // org.eclipse.californium.core.network.Matcher
    public void cancelObserve(Token token) {
        boolean found = false;
        for (Exchange exchange : this.exchangeStore.findByToken(token)) {
            Request request = exchange.getRequest();
            if (request.isObserve()) {
                request.cancel();
                if (!exchange.isNotification()) {
                    found = true;
                }
                exchange.executeComplete();
            }
        }
        if (!found) {
            this.observationStore.remove(token);
        }
    }

    /* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-californium-core.jar:org/eclipse/californium/core/network/BaseMatcher$ObservationObserverAdapter.class */
    private class ObservationObserverAdapter extends MessageObserverAdapter {
        protected final AtomicBoolean removed;
        protected final Token token;

        public ObservationObserverAdapter(Token token) {
            super(true);
            this.removed = new AtomicBoolean();
            this.token = token;
        }

        @Override // org.eclipse.californium.core.coap.MessageObserverAdapter, org.eclipse.californium.core.coap.MessageObserver
        public void onResponse(Response response) {
            Observation observation = BaseMatcher.this.observationStore.get(this.token);
            if (observation != null) {
                if (response.isError() || !response.isNotification()) {
                    BaseMatcher.LOG.debug("observation with token {} not established, removing from observation store", this.token);
                    remove();
                }
            }
        }

        protected void remove() {
            if (this.removed.compareAndSet(false, true)) {
                BaseMatcher.this.observationStore.remove(this.token);
            }
        }
    }
}
