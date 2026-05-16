package org.eclipse.californium.core.network;

import java.util.concurrent.Executor;
import org.eclipse.californium.core.coap.EmptyMessage;
import org.eclipse.californium.core.coap.MessageObserverAdapter;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.californium.core.coap.Token;
import org.eclipse.californium.core.network.Exchange;
import org.eclipse.californium.core.observe.NotificationListener;
import org.eclipse.californium.core.observe.ObservationStore;
import org.eclipse.californium.core.observe.ObserveRelation;
import org.eclipse.californium.elements.EndpointContext;
import org.eclipse.californium.elements.EndpointContextMatcher;
import org.eclipse.californium.elements.config.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-californium-core.jar:org/eclipse/californium/core/network/TcpMatcher.class */
public final class TcpMatcher extends BaseMatcher {
    private static final Logger LOGGER = LoggerFactory.getLogger(TcpMatcher.class);
    private final RemoveHandler exchangeRemoveHandler;
    private final EndpointContextMatcher endpointContextMatcher;

    public TcpMatcher(Configuration config, NotificationListener notificationListener, TokenGenerator tokenGenerator, ObservationStore observationStore, MessageExchangeStore exchangeStore, EndpointContextMatcher endpointContextMatcher, Executor executor) {
        super(config, notificationListener, tokenGenerator, observationStore, exchangeStore, endpointContextMatcher, executor);
        this.exchangeRemoveHandler = new RemoveHandlerImpl();
        this.endpointContextMatcher = endpointContextMatcher;
    }

    @Override // org.eclipse.californium.core.network.Matcher
    public void sendRequest(Exchange exchange) {
        Request request = exchange.getCurrentRequest();
        if (request.isObserve()) {
            registerObserve(request);
        }
        exchange.setRemoveHandler(this.exchangeRemoveHandler);
        this.exchangeStore.registerOutboundRequestWithTokenOnly(exchange);
        LOGGER.debug("tracking open request using [{}]", exchange.getKeyToken());
    }

    @Override // org.eclipse.californium.core.network.Matcher
    public void sendResponse(Exchange exchange) {
        Response response = exchange.getCurrentResponse();
        final ObserveRelation observeRelation = exchange.getRelation();
        response.ensureToken(exchange.getCurrentRequest().getToken());
        if (observeRelation != null) {
            response.addMessageObserver(new MessageObserverAdapter() { // from class: org.eclipse.californium.core.network.TcpMatcher.1
                @Override // org.eclipse.californium.core.coap.MessageObserverAdapter, org.eclipse.californium.core.coap.MessageObserver
                public void onSendError(Throwable error) {
                    observeRelation.cleanup();
                }
            });
        }
        exchange.setComplete();
    }

    @Override // org.eclipse.californium.core.network.Matcher
    public void sendEmptyMessage(Exchange exchange, EmptyMessage message) {
        if (message.isConfirmable()) {
            message.setToken(Token.EMPTY);
            return;
        }
        throw new UnsupportedOperationException("sending empty message (ACK/RST) over tcp is not supported!");
    }

    @Override // org.eclipse.californium.core.network.Matcher
    public void receiveRequest(final Request request, final EndpointReceiver receiver) {
        Object peer = this.endpointContextMatcher.getEndpointIdentity(request.getSourceContext());
        final Exchange exchange = new Exchange(request, peer, Exchange.Origin.REMOTE, this.executor);
        exchange.setRemoveHandler(this.exchangeRemoveHandler);
        exchange.execute(new Runnable() { // from class: org.eclipse.californium.core.network.TcpMatcher.2
            @Override // java.lang.Runnable
            public void run() {
                receiver.receiveRequest(exchange, request);
            }
        });
    }

    @Override // org.eclipse.californium.core.network.Matcher
    public void receiveResponse(final Response response, final EndpointReceiver receiver) {
        Object peer = this.endpointContextMatcher.getEndpointIdentity(response.getSourceContext());
        final KeyToken idByToken = this.tokenGenerator.getKeyToken(response.getToken(), peer);
        Exchange tempExchange = this.exchangeStore.get(idByToken);
        if (tempExchange == null) {
            tempExchange = matchNotifyResponse(response);
        }
        if (tempExchange == null) {
            LOGGER.trace("discarding by [{}] unmatchable response from [{}]: {}", new Object[]{idByToken, response.getSourceContext(), response});
            cancel(response, receiver);
        } else {
            final Exchange exchange = tempExchange;
            exchange.execute(new Runnable() { // from class: org.eclipse.californium.core.network.TcpMatcher.3
                @Override // java.lang.Runnable
                public void run() {
                    boolean checkResponseToken = (exchange.isNotification() && exchange.getRequest() == exchange.getCurrentRequest()) ? false : true;
                    if (checkResponseToken && TcpMatcher.this.exchangeStore.get(idByToken) != exchange) {
                        if (TcpMatcher.this.running) {
                            TcpMatcher.LOGGER.debug("ignoring response {}, exchange not longer matching!", response);
                        }
                        TcpMatcher.this.cancel(response, receiver);
                        return;
                    }
                    EndpointContext context = exchange.getEndpointContext();
                    if (context == null) {
                        TcpMatcher.LOGGER.error("ignoring response from [{}]: {}, request pending to sent!", response.getSourceContext(), response);
                        TcpMatcher.this.cancel(response, receiver);
                        return;
                    }
                    try {
                    } catch (Exception ex) {
                        TcpMatcher.LOGGER.error("error receiving response from [{}]: {} for {}", new Object[]{response.getSourceContext(), response, exchange, ex});
                    }
                    if (!TcpMatcher.this.endpointContextMatcher.isResponseRelatedToRequest(context, response.getSourceContext())) {
                        if (TcpMatcher.LOGGER.isDebugEnabled()) {
                            TcpMatcher.LOGGER.debug("ignoring potentially forged response from [{}]: {} for {} with non-matching endpoint context", new Object[]{TcpMatcher.this.endpointContextMatcher.toRelevantState(response.getSourceContext()), response, exchange});
                        }
                        TcpMatcher.this.cancel(response, receiver);
                        return;
                    }
                    Request currentRequest = exchange.getCurrentRequest();
                    if (exchange.isNotification() && response.isNotification() && currentRequest.isObserveCancel()) {
                        TcpMatcher.LOGGER.debug("ignoring notify for pending cancel {}!", response);
                        TcpMatcher.this.cancel(response, receiver);
                    } else {
                        receiver.receiveResponse(exchange, response);
                    }
                }
            });
        }
    }

    @Override // org.eclipse.californium.core.network.Matcher
    public void receiveEmptyMessage(EmptyMessage message, EndpointReceiver receiver) {
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void cancel(Response response, EndpointReceiver receiver) {
        response.setCanceled(true);
        receiver.receiveResponse(null, response);
    }

    /* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-californium-core.jar:org/eclipse/californium/core/network/TcpMatcher$RemoveHandlerImpl.class */
    private class RemoveHandlerImpl implements RemoveHandler {
        private RemoveHandlerImpl() {
        }

        @Override // org.eclipse.californium.core.network.RemoveHandler
        public void remove(Exchange exchange, KeyToken keyToken, KeyMID keyMID) {
            if (keyToken != null) {
                TcpMatcher.this.exchangeStore.remove(keyToken, exchange);
            }
        }
    }
}
