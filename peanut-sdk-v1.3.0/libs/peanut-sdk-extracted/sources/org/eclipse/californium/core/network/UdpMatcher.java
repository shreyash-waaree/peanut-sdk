package org.eclipse.californium.core.network;

import java.net.InetSocketAddress;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.EmptyMessage;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.californium.core.coap.Token;
import org.eclipse.californium.core.network.Exchange;
import org.eclipse.californium.core.observe.NotificationListener;
import org.eclipse.californium.core.observe.ObservationStore;
import org.eclipse.californium.elements.EndpointContext;
import org.eclipse.californium.elements.EndpointContextMatcher;
import org.eclipse.californium.elements.config.Configuration;
import org.eclipse.californium.elements.util.Bytes;
import org.eclipse.californium.elements.util.NetworkInterfacesUtil;
import org.eclipse.californium.elements.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-californium-core.jar:org/eclipse/californium/core/network/UdpMatcher.class */
public final class UdpMatcher extends BaseMatcher {
    private static final Logger LOGGER = LoggerFactory.getLogger(UdpMatcher.class);
    private final RemoveHandler exchangeRemoveHandler;
    private final EndpointContextMatcher endpointContextMatcher;

    public UdpMatcher(Configuration config, NotificationListener notificationListener, TokenGenerator tokenGenerator, ObservationStore observationStore, MessageExchangeStore exchangeStore, Executor executor, EndpointContextMatcher matchingStrategy) {
        super(config, notificationListener, tokenGenerator, observationStore, exchangeStore, matchingStrategy, executor);
        this.exchangeRemoveHandler = new RemoveHandlerImpl();
        this.endpointContextMatcher = matchingStrategy;
    }

    @Override // org.eclipse.californium.core.network.Matcher
    public void sendRequest(Exchange exchange) {
        Request request = exchange.getCurrentRequest();
        if (request.isObserve() && 0 == exchange.getFailedTransmissionCount()) {
            if (this.exchangeStore.assignMessageId(request) != -1) {
                registerObserve(request);
            } else {
                LOGGER.debug("message IDs exhausted, could not register outbound observe request for tracking");
                request.setSendError(new IllegalStateException("automatic message IDs exhausted"));
                return;
            }
        }
        try {
            if (this.exchangeStore.registerOutboundRequest(exchange)) {
                exchange.setRemoveHandler(this.exchangeRemoveHandler);
                LOGGER.debug("tracking open request [{}, {}]", exchange.getKeyMID(), exchange.getKeyToken());
            } else {
                LOGGER.debug("message IDs exhausted, could not register outbound request for tracking");
                request.setSendError(new IllegalStateException("automatic message IDs exhausted"));
            }
        } catch (IllegalArgumentException ex) {
            request.setSendError(ex);
        }
    }

    @Override // org.eclipse.californium.core.network.Matcher
    public void sendResponse(Exchange exchange) {
        boolean ready = true;
        Response response = exchange.getCurrentResponse();
        response.ensureToken(exchange.getCurrentRequest().getToken());
        if (response.getType() == CoAP.Type.CON) {
            exchange.removeNotifications();
            if (this.exchangeStore.registerOutboundResponse(exchange)) {
                LOGGER.debug("tracking open response [{}]", exchange.getKeyMID());
                ready = false;
            } else {
                response.setSendError(new IllegalStateException("automatic message IDs exhausted"));
            }
        } else if (response.getType() == CoAP.Type.NON) {
            if (response.isNotification()) {
                if (this.exchangeStore.registerOutboundResponse(exchange)) {
                    ready = false;
                } else {
                    response.setSendError(new IllegalStateException("automatic message IDs exhausted"));
                }
            } else if (this.exchangeStore.assignMessageId(response) == -1) {
                response.setSendError(new IllegalStateException("automatic message IDs exhausted"));
            }
        }
        if (ready) {
            exchange.setComplete();
        }
    }

    @Override // org.eclipse.californium.core.network.Matcher
    public void sendEmptyMessage(Exchange exchange, EmptyMessage message) {
        message.setToken(Token.EMPTY);
        if (message.getType() == CoAP.Type.RST && exchange != null) {
            exchange.executeComplete();
        }
    }

    @Override // org.eclipse.californium.core.network.Matcher
    public void receiveRequest(final Request request, final EndpointReceiver receiver) {
        EndpointContext previousSourceContext;
        Object peer = this.endpointContextMatcher.getEndpointIdentity(request.getSourceContext());
        KeyMID idByMID = new KeyMID(request.getMID(), peer);
        final Exchange exchange = new Exchange(request, peer, Exchange.Origin.REMOTE, this.executor);
        final Exchange previous = this.exchangeStore.findPrevious(idByMID, exchange);
        boolean duplicate = previous != null;
        if (duplicate) {
            EndpointContext sourceContext = request.getSourceContext();
            Request previousRequest = previous.getCurrentRequest();
            if (previous.isOfLocalOrigin()) {
                previousSourceContext = previousRequest.getDestinationContext();
            } else {
                previousSourceContext = previousRequest.getSourceContext();
            }
            duplicate = this.endpointContextMatcher.isToBeSent(previousSourceContext, sourceContext);
            if (!duplicate) {
                if (this.exchangeStore.replacePrevious(idByMID, previous, exchange)) {
                    LOGGER.debug("replaced request {} by new request {}!", previousRequest, request);
                } else {
                    LOGGER.warn("new request {} could not be registered! Deduplication disabled!", request);
                }
            } else if (previousRequest.isMulticast() || request.isMulticast()) {
                InetSocketAddress group = request.getLocalAddress();
                InetSocketAddress previousGroup = previousRequest.getLocalAddress();
                if (!NetworkInterfacesUtil.equals(group, previousGroup)) {
                    boolean differs = !Bytes.equals(request.getToken(), previousRequest.getToken());
                    long timeDiff = TimeUnit.NANOSECONDS.toMillis(Math.abs(request.getNanoTimestamp() - previousRequest.getNanoTimestamp()));
                    if (differs) {
                        LOGGER.info("received different requests {} with same MID via different multicast groups ({} != {}) within {}ms!", new Object[]{request, StringUtil.toLog(group), StringUtil.toLog(previousGroup), Long.valueOf(timeDiff)});
                    } else {
                        LOGGER.warn("received requests {} via different multicast groups ({} != {}) within {}ms!", new Object[]{request, StringUtil.toLog(group), StringUtil.toLog(previousGroup), Long.valueOf(timeDiff)});
                    }
                }
            }
        }
        if (duplicate && previous != null) {
            LOGGER.trace("duplicate request: {}", request);
            request.setDuplicate(true);
            previous.execute(new Runnable() { // from class: org.eclipse.californium.core.network.UdpMatcher.1
                @Override // java.lang.Runnable
                public void run() {
                    try {
                        receiver.receiveRequest(previous, request);
                    } catch (RuntimeException ex) {
                        UdpMatcher.LOGGER.warn("error receiving again request {}", request, ex);
                        if (!request.isMulticast()) {
                            receiver.reject(request);
                        }
                    }
                }
            });
        } else {
            exchange.setRemoveHandler(this.exchangeRemoveHandler);
            exchange.execute(new Runnable() { // from class: org.eclipse.californium.core.network.UdpMatcher.2
                @Override // java.lang.Runnable
                public void run() {
                    try {
                        receiver.receiveRequest(exchange, request);
                    } catch (RuntimeException ex) {
                        UdpMatcher.LOGGER.warn("error receiving request {}", request, ex);
                        if (!request.isMulticast()) {
                            receiver.reject(request);
                        }
                    }
                }
            });
        }
    }

    @Override // org.eclipse.californium.core.network.Matcher
    public void receiveResponse(final Response response, final EndpointReceiver receiver) {
        final Object peer = this.endpointContextMatcher.getEndpointIdentity(response.getSourceContext());
        final KeyToken idByToken = this.tokenGenerator.getKeyToken(response.getToken(), peer);
        LOGGER.trace("received response {} from {}", response, response.getSourceContext());
        Exchange tempExchange = this.exchangeStore.get(idByToken);
        if (tempExchange == null) {
            if (response.getType() != CoAP.Type.ACK) {
                KeyMID idByMID = new KeyMID(response.getMID(), peer);
                final Exchange prev = this.exchangeStore.find(idByMID);
                if (prev != null) {
                    prev.execute(new Runnable() { // from class: org.eclipse.californium.core.network.UdpMatcher.3
                        @Override // java.lang.Runnable
                        public void run() {
                            if (prev.getCurrentRequest().isMulticast()) {
                                UdpMatcher.LOGGER.debug("Ignore delayed response {} to multicast request {}", response, StringUtil.toLog(prev.getCurrentRequest().getDestinationContext().getPeerAddress()));
                                UdpMatcher.this.cancel(response, receiver);
                                return;
                            }
                            try {
                            } catch (RuntimeException ex) {
                                UdpMatcher.LOGGER.warn("error receiving response {} for {}", new Object[]{response, prev, ex});
                            }
                            if (UdpMatcher.this.endpointContextMatcher.isResponseRelatedToRequest(prev.getEndpointContext(), response.getSourceContext())) {
                                UdpMatcher.LOGGER.trace("received response {} for already completed {}", response, prev);
                                response.setDuplicate(true);
                                Response prevResponse = prev.getCurrentResponse();
                                if (prevResponse != null) {
                                    response.setRejected(prevResponse.isRejected());
                                }
                                receiver.receiveResponse(prev, response);
                                return;
                            }
                            UdpMatcher.LOGGER.debug("ignoring potentially forged response {} for already completed {}", response, prev);
                            UdpMatcher.this.reject(response, receiver);
                        }
                    });
                    return;
                }
            }
            tempExchange = matchNotifyResponse(response);
            if (tempExchange == null) {
                if (response.getType() == CoAP.Type.ACK) {
                    LOGGER.trace("discarding by [{}] unmatchable piggy-backed response from [{}]: {}", new Object[]{idByToken, response.getSourceContext(), response});
                    cancel(response, receiver);
                    return;
                } else {
                    LOGGER.trace("discarding by [{}] unmatchable response from [{}]: {}", new Object[]{idByToken, response.getSourceContext(), response});
                    reject(response, receiver);
                    return;
                }
            }
        }
        final Exchange exchange = tempExchange;
        exchange.execute(new Runnable() { // from class: org.eclipse.californium.core.network.UdpMatcher.4
            @Override // java.lang.Runnable
            public void run() {
                boolean checkResponseToken = (exchange.isNotification() && exchange.getRequest() == exchange.getCurrentRequest()) ? false : true;
                UdpMatcher.LOGGER.debug("!exchange.isNotification() is {}, (exchange.getRequest() != exchange.getCurrentRequest()) is {}", Boolean.valueOf(!exchange.isNotification()), Boolean.valueOf(exchange.getRequest() != exchange.getCurrentRequest()));
                UdpMatcher.LOGGER.debug("checkResponseToken is {}, (exchangeStore.get(idByToken) != exchange) is {}", Boolean.valueOf(checkResponseToken), Boolean.valueOf(UdpMatcher.this.exchangeStore.get(idByToken) != exchange));
                if (checkResponseToken && UdpMatcher.this.exchangeStore.get(idByToken) != exchange) {
                    if (UdpMatcher.this.running) {
                        UdpMatcher.LOGGER.debug("ignoring response {}, exchange not longer matching!", response);
                    }
                    UdpMatcher.this.cancel(response, receiver);
                    return;
                }
                EndpointContext context = exchange.getEndpointContext();
                if (context == null) {
                    UdpMatcher.LOGGER.debug("ignoring response {}, request pending to sent!", response);
                    UdpMatcher.this.cancel(response, receiver);
                    return;
                }
                try {
                } catch (RuntimeException ex) {
                    UdpMatcher.LOGGER.warn("error receiving response {} for {}", new Object[]{response, exchange, ex});
                }
                if (!UdpMatcher.this.endpointContextMatcher.isResponseRelatedToRequest(context, response.getSourceContext())) {
                    UdpMatcher.LOGGER.debug("ignoring potentially forged response for token {} with non-matching endpoint context", idByToken);
                    UdpMatcher.this.reject(response, receiver);
                    return;
                }
                CoAP.Type type = response.getType();
                Request currentRequest = exchange.getCurrentRequest();
                int requestMid = currentRequest.getMID();
                if (currentRequest.isMulticast()) {
                    if (type != CoAP.Type.NON) {
                        UdpMatcher.LOGGER.debug("ignoring response of type {} for multicast request with token [{}], from {}", new Object[]{response.getType(), response.getTokenString(), StringUtil.toLog(response.getSourceContext().getPeerAddress())});
                        UdpMatcher.this.cancel(response, receiver);
                        return;
                    }
                } else if (type == CoAP.Type.ACK && requestMid != response.getMID()) {
                    UdpMatcher.LOGGER.debug("ignoring ACK, possible MID reuse before lifetime end for token {}, expected MID {} but received {}", new Object[]{response.getTokenString(), Integer.valueOf(requestMid), Integer.valueOf(response.getMID())});
                    UdpMatcher.this.cancel(response, receiver);
                    return;
                }
                if (type != CoAP.Type.ACK && !exchange.isNotification() && response.isNotification() && currentRequest.isObserveCancel()) {
                    UdpMatcher.LOGGER.debug("ignoring notify for pending cancel {}!", response);
                    UdpMatcher.this.cancel(response, receiver);
                    return;
                }
                if (type == CoAP.Type.CON || type == CoAP.Type.NON) {
                    KeyMID idByMID2 = new KeyMID(response.getMID(), peer);
                    Exchange prev2 = UdpMatcher.this.exchangeStore.findPrevious(idByMID2, exchange);
                    if (prev2 != null) {
                        UdpMatcher.LOGGER.trace("received duplicate response for open {}: {}", exchange, response);
                        response.setDuplicate(true);
                        Response prevResponse = prev2.getCurrentResponse();
                        if (prevResponse != null) {
                            response.setRejected(prevResponse.isRejected());
                        }
                    }
                }
                receiver.receiveResponse(exchange, response);
            }
        });
    }

    @Override // org.eclipse.californium.core.network.Matcher
    public void receiveEmptyMessage(final EmptyMessage message, final EndpointReceiver receiver) {
        EndpointContext context = message.getSourceContext();
        Object identity = this.endpointContextMatcher.getEndpointIdentity(context);
        KeyMID byMID = new KeyMID(message.getMID(), identity);
        Exchange tempExchange = this.exchangeStore.get(byMID);
        if (tempExchange == null && identity != context.getPeerAddress()) {
            KeyMID pongByMID = new KeyMID(message.getMID(), context.getPeerAddress());
            tempExchange = this.exchangeStore.get(pongByMID);
            if (tempExchange != null) {
                byMID = pongByMID;
            }
        }
        if (tempExchange == null) {
            LOGGER.debug("ignoring {} message unmatchable by {}", message.getType(), byMID);
            cancel(message, receiver);
        } else {
            final KeyMID idByMID = byMID;
            final Exchange exchange = tempExchange;
            exchange.execute(new Runnable() { // from class: org.eclipse.californium.core.network.UdpMatcher.5
                @Override // java.lang.Runnable
                public void run() {
                    if (exchange.getCurrentRequest().isMulticast()) {
                        UdpMatcher.LOGGER.debug("ignoring {} message for multicast request {}", message.getType(), idByMID);
                        UdpMatcher.this.cancel(message, receiver);
                        return;
                    }
                    if (UdpMatcher.this.exchangeStore.get(idByMID) != exchange) {
                        if (UdpMatcher.this.running) {
                            UdpMatcher.LOGGER.debug("ignoring {} message not longer matching by {}", message.getType(), idByMID);
                        }
                        UdpMatcher.this.cancel(message, receiver);
                        return;
                    }
                    try {
                    } catch (RuntimeException ex) {
                        UdpMatcher.LOGGER.warn("error receiving {} message for {}", new Object[]{message.getType(), exchange, ex});
                    }
                    if (UdpMatcher.this.endpointContextMatcher.isResponseRelatedToRequest(exchange.getEndpointContext(), message.getSourceContext())) {
                        UdpMatcher.this.exchangeStore.remove(idByMID, exchange);
                        UdpMatcher.LOGGER.debug("received expected {} reply for {}", message.getType(), idByMID);
                        receiver.receiveEmptyMessage(exchange, message);
                    } else {
                        UdpMatcher.LOGGER.debug("ignoring potentially forged {} reply for {} with non-matching endpoint context", message.getType(), idByMID);
                        UdpMatcher.this.cancel(message, receiver);
                    }
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void reject(Response response, EndpointReceiver receiver) {
        if (response.getType() != CoAP.Type.ACK && response.hasMID()) {
            receiver.reject(response);
        }
        cancel(response, receiver);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void cancel(Response response, EndpointReceiver receiver) {
        response.setCanceled(true);
        receiver.receiveResponse(null, response);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void cancel(EmptyMessage message, EndpointReceiver receiver) {
        message.setCanceled(true);
        receiver.receiveEmptyMessage(null, message);
    }

    /* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-californium-core.jar:org/eclipse/californium/core/network/UdpMatcher$RemoveHandlerImpl.class */
    private class RemoveHandlerImpl implements RemoveHandler {
        private RemoveHandlerImpl() {
        }

        @Override // org.eclipse.californium.core.network.RemoveHandler
        public void remove(Exchange exchange, KeyToken keyToken, KeyMID keyMID) {
            if (keyToken != null) {
                UdpMatcher.this.exchangeStore.remove(keyToken, exchange);
            }
            if (keyMID != null) {
                UdpMatcher.this.exchangeStore.remove(keyMID, exchange);
            }
        }
    }
}
