package org.eclipse.californium.core.network;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.CoAPMessageFormatException;
import org.eclipse.californium.core.coap.EmptyMessage;
import org.eclipse.californium.core.coap.Message;
import org.eclipse.californium.core.coap.MessageFormatException;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.californium.core.coap.Token;
import org.eclipse.californium.core.config.CoapConfig;
import org.eclipse.californium.core.network.EndpointManager;
import org.eclipse.californium.core.network.Exchange;
import org.eclipse.californium.core.network.interceptors.MessageInterceptor;
import org.eclipse.californium.core.network.serialization.DataParser;
import org.eclipse.californium.core.network.serialization.DataSerializer;
import org.eclipse.californium.core.network.serialization.TcpDataParser;
import org.eclipse.californium.core.network.serialization.TcpDataSerializer;
import org.eclipse.californium.core.network.serialization.UdpDataParser;
import org.eclipse.californium.core.network.serialization.UdpDataSerializer;
import org.eclipse.californium.core.network.stack.CoapStack;
import org.eclipse.californium.core.network.stack.CoapTcpStack;
import org.eclipse.californium.core.network.stack.CoapUdpStack;
import org.eclipse.californium.core.observe.InMemoryObservationStore;
import org.eclipse.californium.core.observe.NotificationListener;
import org.eclipse.californium.core.observe.ObservationStore;
import org.eclipse.californium.core.server.MessageDeliverer;
import org.eclipse.californium.elements.Connector;
import org.eclipse.californium.elements.EndpointContext;
import org.eclipse.californium.elements.EndpointContextMatcher;
import org.eclipse.californium.elements.EndpointIdentityResolver;
import org.eclipse.californium.elements.MessageCallback;
import org.eclipse.californium.elements.RawData;
import org.eclipse.californium.elements.RawDataChannel;
import org.eclipse.californium.elements.UDPConnector;
import org.eclipse.californium.elements.UdpMulticastConnector;
import org.eclipse.californium.elements.config.Configuration;
import org.eclipse.californium.elements.util.ClockUtil;
import org.eclipse.californium.elements.util.DaemonThreadFactory;
import org.eclipse.californium.elements.util.ExecutorsUtil;
import org.eclipse.californium.elements.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-californium-core.jar:org/eclipse/californium/core/network/CoapEndpoint.class */
public class CoapEndpoint implements Endpoint, Executor {
    protected final CoapStack coapstack;
    private final Connector connector;
    private final String scheme;
    private final int multicastBaseMid;
    private final boolean useRequestOffloading;
    private final Configuration config;
    private final EndpointIdentityResolver identityResolver;
    private final Matcher matcher;
    private final DataSerializer serializer;
    private final DataParser parser;
    private final MessageExchangeStore exchangeStore;
    private final ObservationStore observationStore;
    private final String tag;
    private ExecutorService executor;
    private ScheduledExecutorService secondaryExecutor;
    private volatile boolean started;
    private ScheduledFuture<?> statusLogger;
    private static CoapStackFactory defaultCoapStackFactory;
    private static final Logger LOGGER = LoggerFactory.getLogger(CoapEndpoint.class);
    private static final Logger LOGGER_BAN = LoggerFactory.getLogger("org.eclipse.californium.ban");
    private static final AtomicBoolean LOGGER_BAN_STARTED = new AtomicBoolean();
    public static final CoapStackFactory STANDARD_COAP_STACK_FACTORY = new CoapStackFactory() { // from class: org.eclipse.californium.core.network.CoapEndpoint.7
        @Override // org.eclipse.californium.core.network.CoapStackFactory
        public CoapStack createCoapStack(String protocol, String tag, Configuration config, Outbox outbox, Object customStackArgument) {
            if (CoAP.isTcpProtocol(protocol)) {
                return new CoapTcpStack(tag, config, outbox);
            }
            return new CoapUdpStack(tag, config, outbox);
        }
    };
    private List<EndpointObserver> observers = new CopyOnWriteArrayList();
    private List<MessageInterceptor> interceptors = new CopyOnWriteArrayList();
    private List<MessageInterceptor> postProcessInterceptors = new CopyOnWriteArrayList();
    private List<NotificationListener> notificationListeners = new CopyOnWriteArrayList();
    private final EndpointReceiver endpointStackReceiver = new EndpointReceiver() { // from class: org.eclipse.californium.core.network.CoapEndpoint.1
        @Override // org.eclipse.californium.core.network.EndpointReceiver
        public void receiveRequest(Exchange exchange, Request request) {
            exchange.setEndpoint(CoapEndpoint.this);
            CoapEndpoint.this.coapstack.receiveRequest(exchange, request);
            CoapEndpoint.this.notifyReceive((List<MessageInterceptor>) CoapEndpoint.this.postProcessInterceptors, request);
        }

        @Override // org.eclipse.californium.core.network.EndpointReceiver
        public void receiveResponse(Exchange exchange, Response response) {
            if (exchange != null && !response.isCanceled()) {
                exchange.setEndpoint(CoapEndpoint.this);
                if (!exchange.isNotification()) {
                    response.setApplicationRttNanos(exchange.calculateApplicationRtt());
                    response.setTransmissionRttNanos(exchange.calculateTransmissionRtt());
                }
                CoapEndpoint.LOGGER.trace("EndpointReceiver receiveResponse {}: {}", exchange, response);
                CoapEndpoint.this.coapstack.receiveResponse(exchange, response);
            }
            CoapEndpoint.this.notifyReceive((List<MessageInterceptor>) CoapEndpoint.this.postProcessInterceptors, response);
        }

        @Override // org.eclipse.californium.core.network.EndpointReceiver
        public void receiveEmptyMessage(Exchange exchange, EmptyMessage message) {
            Response response;
            if (exchange != null && !message.isCanceled()) {
                exchange.setEndpoint(CoapEndpoint.this);
                if (!exchange.isOfLocalOrigin() && (response = exchange.getCurrentResponse()) != null && response.isConfirmable()) {
                    response.setTransmissionRttNanos(exchange.calculateTransmissionRtt());
                }
                CoapEndpoint.this.coapstack.receiveEmptyMessage(exchange, message);
            }
            CoapEndpoint.this.notifyReceive((List<MessageInterceptor>) CoapEndpoint.this.postProcessInterceptors, message);
        }

        @Override // org.eclipse.californium.core.network.EndpointReceiver
        public void reject(Message message) {
            EmptyMessage rst = EmptyMessage.newRST(message);
            CoapEndpoint.this.coapstack.sendEmptyMessage(null, rst);
        }
    };

    protected CoapEndpoint(Connector connector, Configuration config, TokenGenerator tokenGenerator, ObservationStore store, MessageExchangeStore exchangeStore, EndpointContextMatcher endpointContextMatcher, DataSerializer serializer, DataParser parser, String loggingTag, CoapStackFactory coapStackFactory, Object customStackArgument) {
        if (LOGGER_BAN.isInfoEnabled() && LOGGER_BAN_STARTED.compareAndSet(false, true)) {
            LOGGER_BAN.info("Started.");
        }
        this.config = config;
        this.connector = connector;
        this.connector.setRawDataReceiver(new InboxImpl());
        this.scheme = CoAP.getSchemeForProtocol(connector.getProtocol());
        this.multicastBaseMid = ((Integer) config.get(CoapConfig.MULTICAST_BASE_MID)).intValue();
        this.tag = StringUtil.normalizeLoggingTag(loggingTag);
        tokenGenerator = tokenGenerator == null ? new RandomTokenGenerator(config) : tokenGenerator;
        coapStackFactory = coapStackFactory == null ? getDefaultCoapStackFactory() : coapStackFactory;
        this.exchangeStore = null != exchangeStore ? exchangeStore : new InMemoryMessageExchangeStore(this.tag, config, tokenGenerator);
        this.observationStore = null != store ? store : new InMemoryObservationStore(config);
        endpointContextMatcher = null == endpointContextMatcher ? EndpointContextMatcherFactory.create(connector, config) : endpointContextMatcher;
        this.identityResolver = endpointContextMatcher;
        this.connector.setEndpointContextMatcher(endpointContextMatcher);
        LOGGER.info("{}{} uses {}", new Object[]{this.tag, getClass().getSimpleName(), endpointContextMatcher.getName()});
        this.coapstack = coapStackFactory.createCoapStack(connector.getProtocol(), this.tag, config, new OutboxImpl(), customStackArgument);
        if (CoAP.isTcpProtocol(connector.getProtocol())) {
            this.useRequestOffloading = false;
            this.matcher = new TcpMatcher(config, new NotificationDispatcher(), tokenGenerator, this.observationStore, this.exchangeStore, endpointContextMatcher, this);
            this.serializer = serializer != null ? serializer : new TcpDataSerializer();
            this.parser = parser != null ? parser : new TcpDataParser();
            return;
        }
        this.useRequestOffloading = ((Boolean) config.get(CoapConfig.USE_MESSAGE_OFFLOADING)).booleanValue();
        this.matcher = new UdpMatcher(config, new NotificationDispatcher(), tokenGenerator, this.observationStore, this.exchangeStore, this, endpointContextMatcher);
        this.serializer = serializer != null ? serializer : new UdpDataSerializer();
        this.parser = parser != null ? parser : new UdpDataParser();
    }

    public String getTag() {
        return this.tag;
    }

    @Override // org.eclipse.californium.core.network.Endpoint
    public synchronized void start() throws IOException {
        if (this.started) {
            LOGGER.debug("{}Endpoint at {} is already started", this.tag, getUri());
            return;
        }
        if (!this.coapstack.hasDeliverer()) {
            setMessageDeliverer(new EndpointManager.ClientMessageDeliverer());
        }
        if (this.executor == null) {
            LOGGER.info("{}Endpoint [{}] requires an executor to start, using default single-threaded daemon executor", this.tag, getUri());
            final ScheduledExecutorService executorService = ExecutorsUtil.newSingleThreadScheduledExecutor(new DaemonThreadFactory(":CoapEndpoint-" + this.connector + '#'));
            setExecutors(executorService, executorService);
            addObserver(new EndpointObserver() { // from class: org.eclipse.californium.core.network.CoapEndpoint.2
                @Override // org.eclipse.californium.core.network.EndpointObserver
                public void started(Endpoint endpoint) {
                }

                @Override // org.eclipse.californium.core.network.EndpointObserver
                public void stopped(Endpoint endpoint) {
                }

                @Override // org.eclipse.californium.core.network.EndpointObserver
                public void destroyed(Endpoint endpoint) {
                    ExecutorsUtil.shutdownExecutorGracefully(1000L, executorService);
                }
            });
        }
        try {
            LOGGER.debug("{}Starting endpoint at {}", this.tag, getUri());
            this.matcher.start();
            this.connector.start();
            this.coapstack.start();
            this.started = true;
            for (EndpointObserver obs : this.observers) {
                obs.started(this);
            }
            LOGGER.info("{}Started endpoint at {}", this.tag, getUri());
        } catch (IOException e) {
            stop();
            throw e;
        }
    }

    @Override // org.eclipse.californium.core.network.Endpoint
    public synchronized void stop() {
        URI uri = getUri();
        if (!this.started) {
            LOGGER.debug("{}Endpoint at {} is already stopped", this.tag, uri);
            return;
        }
        LOGGER.debug("{}Stopping endpoint at {}", this.tag, uri);
        this.started = false;
        if (this.statusLogger != null) {
            this.statusLogger.cancel(false);
            this.statusLogger = null;
        }
        this.connector.stop();
        this.matcher.stop();
        for (EndpointObserver obs : this.observers) {
            obs.stopped(this);
        }
        LOGGER.debug("{}Stopped endpoint at {}", this.tag, uri);
    }

    @Override // org.eclipse.californium.core.network.Endpoint
    public synchronized void destroy() {
        LOGGER.info("{}Destroying endpoint at {}", this.tag, getUri());
        if (this.started) {
            stop();
        }
        this.connector.destroy();
        this.coapstack.destroy();
        for (EndpointObserver obs : this.observers) {
            obs.destroyed(this);
        }
    }

    @Override // org.eclipse.californium.core.network.Endpoint
    public void clear() {
        this.matcher.clear();
    }

    @Override // org.eclipse.californium.core.network.Endpoint
    public boolean isStarted() {
        return this.started;
    }

    @Override // org.eclipse.californium.core.network.Endpoint
    public void setExecutors(ScheduledExecutorService mainExecutor, ScheduledExecutorService secondaryExecutor) {
        if (mainExecutor == null || secondaryExecutor == null) {
            throw new IllegalArgumentException("executors must not be null");
        }
        if (this.executor == mainExecutor && this.secondaryExecutor == secondaryExecutor) {
            return;
        }
        if (this.started) {
            throw new IllegalStateException("endpoint already started!");
        }
        this.executor = mainExecutor;
        this.secondaryExecutor = secondaryExecutor;
        this.coapstack.setExecutors(mainExecutor, this.secondaryExecutor);
        this.exchangeStore.setExecutor(this.secondaryExecutor);
        this.observationStore.setExecutor(this.secondaryExecutor);
    }

    @Override // org.eclipse.californium.core.network.Endpoint
    public void addNotificationListener(NotificationListener listener) {
        this.notificationListeners.add(listener);
    }

    @Override // org.eclipse.californium.core.network.Endpoint
    public void removeNotificationListener(NotificationListener listener) {
        this.notificationListeners.remove(listener);
    }

    @Override // org.eclipse.californium.core.network.Endpoint
    public void addObserver(EndpointObserver observer) {
        this.observers.add(observer);
    }

    @Override // org.eclipse.californium.core.network.Endpoint
    public void removeObserver(EndpointObserver observer) {
        this.observers.remove(observer);
    }

    @Override // org.eclipse.californium.core.network.Endpoint
    public void addInterceptor(MessageInterceptor interceptor) {
        this.interceptors.add(interceptor);
    }

    @Override // org.eclipse.californium.core.network.Endpoint
    public void removeInterceptor(MessageInterceptor interceptor) {
        this.interceptors.remove(interceptor);
    }

    @Override // org.eclipse.californium.core.network.Endpoint
    public List<MessageInterceptor> getInterceptors() {
        return Collections.unmodifiableList(this.interceptors);
    }

    @Override // org.eclipse.californium.core.network.Endpoint
    public void addPostProcessInterceptor(MessageInterceptor interceptor) {
        this.postProcessInterceptors.add(interceptor);
    }

    @Override // org.eclipse.californium.core.network.Endpoint
    public void removePostProcessInterceptor(MessageInterceptor interceptor) {
        this.postProcessInterceptors.remove(interceptor);
    }

    @Override // org.eclipse.californium.core.network.Endpoint
    public List<MessageInterceptor> getPostProcessInterceptors() {
        return Collections.unmodifiableList(this.postProcessInterceptors);
    }

    @Override // org.eclipse.californium.core.network.Endpoint
    public void sendRequest(final Request request) {
        Object identity;
        if (!this.started) {
            request.cancel();
            return;
        }
        InetSocketAddress destinationAddress = request.getDestinationContext().getPeerAddress();
        int mid = request.getMID();
        if (request.isMulticast()) {
            if (0 >= this.multicastBaseMid) {
                LOGGER.warn("{}multicast messaging to destination {} is not enabled! Please enable it configuring \"" + CoapConfig.MULTICAST_BASE_MID.getKey() + "\" greater than 0", this.tag, StringUtil.toLog(destinationAddress));
                request.setSendError(new IllegalArgumentException("multicast is not enabled!"));
                return;
            } else if (request.getType() == CoAP.Type.CON) {
                LOGGER.warn("{}CON request to multicast destination {} is not allowed, as per RFC 7252, 8.1, a client MUST use NON message type for multicast requests", this.tag, StringUtil.toLog(destinationAddress));
                request.setSendError(new IllegalArgumentException("multicast is not supported for CON!"));
                return;
            } else if (request.hasMID() && mid < this.multicastBaseMid) {
                LOGGER.warn("{}multicast request to group {} has mid {} which is not in the MULTICAST_MID range [{}-65535]", new Object[]{this.tag, StringUtil.toLog(destinationAddress), Integer.valueOf(mid), Integer.valueOf(this.multicastBaseMid)});
                request.setSendError(new IllegalArgumentException("multicast mid is not in range [" + this.multicastBaseMid + "-65535]"));
                return;
            }
        } else if (isMulticastMid(mid)) {
            LOGGER.warn("{}request to {} has mid {}, which is in the MULTICAST_MID range [{}-65535]", new Object[]{this.tag, StringUtil.toLog(destinationAddress), Integer.valueOf(mid), Integer.valueOf(this.multicastBaseMid)});
            request.setSendError(new IllegalArgumentException("unicast mid is in multicast range [" + this.multicastBaseMid + "-65535]"));
            return;
        }
        if (destinationAddress.isUnresolved()) {
            String addr = StringUtil.toDisplayString(destinationAddress);
            LOGGER.warn("{}request has unresolved destination address {}", this.tag, addr);
            request.setSendError(new IllegalArgumentException(addr + " is a unresolved address!"));
            return;
        }
        try {
            identity = this.identityResolver.getEndpointIdentity(request.getDestinationContext());
        } catch (IllegalArgumentException ex) {
            if (request.getRawCode() == 0) {
                identity = request.getDestinationContext().getPeerAddress();
            } else {
                throw ex;
            }
        }
        final Exchange exchange = new Exchange(request, identity, Exchange.Origin.LOCAL, this.executor);
        exchange.execute(new Runnable() { // from class: org.eclipse.californium.core.network.CoapEndpoint.3
            @Override // java.lang.Runnable
            public void run() {
                CoapEndpoint.this.coapstack.sendRequest(exchange, request);
            }
        });
    }

    @Override // org.eclipse.californium.core.network.Endpoint
    public void sendResponse(final Exchange exchange, final Response response) {
        if (!this.started) {
            response.cancel();
        } else if (exchange.checkOwner()) {
            this.coapstack.sendResponse(exchange, response);
        } else {
            exchange.execute(new Runnable() { // from class: org.eclipse.californium.core.network.CoapEndpoint.4
                @Override // java.lang.Runnable
                public void run() {
                    CoapEndpoint.this.coapstack.sendResponse(exchange, response);
                }
            });
        }
    }

    @Override // org.eclipse.californium.core.network.Endpoint
    public void sendEmptyMessage(final Exchange exchange, final EmptyMessage message) {
        if (!this.started) {
            message.cancel();
        } else if (exchange.checkOwner()) {
            this.coapstack.sendEmptyMessage(exchange, message);
        } else {
            exchange.execute(new Runnable() { // from class: org.eclipse.californium.core.network.CoapEndpoint.5
                @Override // java.lang.Runnable
                public void run() {
                    CoapEndpoint.this.coapstack.sendEmptyMessage(exchange, message);
                }
            });
        }
    }

    @Override // org.eclipse.californium.core.network.Endpoint
    public void setMessageDeliverer(MessageDeliverer deliverer) {
        this.coapstack.setDeliverer(deliverer);
    }

    @Override // org.eclipse.californium.core.network.Endpoint
    public InetSocketAddress getAddress() {
        return this.connector.getAddress();
    }

    @Override // org.eclipse.californium.core.network.Endpoint
    public URI getUri() {
        try {
            InetSocketAddress address = getAddress();
            String hostname = StringUtil.getUriHostname(address.getAddress());
            return new URI(this.scheme, null, hostname, address.getPort(), null, null, null);
        } catch (URISyntaxException e) {
            LOGGER.warn("{}URI", this.tag, e);
            return null;
        }
    }

    @Override // org.eclipse.californium.core.network.Endpoint
    public Configuration getConfig() {
        return this.config;
    }

    public Connector getConnector() {
        return this.connector;
    }

    /* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-californium-core.jar:org/eclipse/californium/core/network/CoapEndpoint$NotificationDispatcher.class */
    private class NotificationDispatcher implements NotificationListener {
        private NotificationDispatcher() {
        }

        @Override // org.eclipse.californium.core.observe.NotificationListener
        public void onNotification(Request request, Response response) {
            for (NotificationListener notificationListener : CoapEndpoint.this.notificationListeners) {
                notificationListener.onNotification(request, response);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void notifySend(List<MessageInterceptor> list, Request request) {
        for (MessageInterceptor interceptor : list) {
            interceptor.sendRequest(request);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void notifySend(List<MessageInterceptor> list, Response response) {
        for (MessageInterceptor interceptor : list) {
            interceptor.sendResponse(response);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void notifySend(List<MessageInterceptor> list, EmptyMessage emptyMessage) {
        for (MessageInterceptor interceptor : list) {
            interceptor.sendEmptyMessage(emptyMessage);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void notifyReceive(List<MessageInterceptor> list, Request request) {
        for (MessageInterceptor interceptor : list) {
            interceptor.receiveRequest(request);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void notifyReceive(List<MessageInterceptor> list, Response response) {
        for (MessageInterceptor interceptor : list) {
            interceptor.receiveResponse(response);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void notifyReceive(List<MessageInterceptor> list, EmptyMessage emptyMessage) {
        for (MessageInterceptor interceptor : list) {
            interceptor.receiveEmptyMessage(emptyMessage);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean isMulticastMid(int mid) {
        return 0 < this.multicastBaseMid && this.multicastBaseMid <= mid && mid <= 65535;
    }

    /* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-californium-core.jar:org/eclipse/californium/core/network/CoapEndpoint$OutboxImpl.class */
    public class OutboxImpl implements Outbox {
        public OutboxImpl() {
        }

        @Override // org.eclipse.californium.core.network.Outbox
        public void sendRequest(Exchange exchange, Request request) {
            assertMessageHasDestinationAddress(request);
            exchange.setCurrentRequest(request);
            CoapEndpoint.this.matcher.sendRequest(exchange);
            CoapEndpoint.this.notifySend((List<MessageInterceptor>) CoapEndpoint.this.interceptors, request);
            request.setReadyToSend();
            if (!CoapEndpoint.this.started) {
                request.cancel();
            }
            if (request.isCanceled() || request.getSendError() != null) {
                exchange.executeComplete();
                return;
            }
            if (exchange.getFailedTransmissionCount() == 0) {
                exchange.startTransmissionRtt();
            }
            RawData message = CoapEndpoint.this.serializer.serializeRequest(request, new ExchangeCallback<Request>(exchange, request) { // from class: org.eclipse.californium.core.network.CoapEndpoint.OutboxImpl.1
                /* JADX INFO: Access modifiers changed from: protected */
                @Override // org.eclipse.californium.core.network.CoapEndpoint.SendingCallback
                public void notifyPostProcess(Request request2) {
                    CoapEndpoint.this.notifySend((List<MessageInterceptor>) CoapEndpoint.this.postProcessInterceptors, request2);
                }
            });
            CoapEndpoint.this.connector.send(message);
        }

        @Override // org.eclipse.californium.core.network.Outbox
        public void sendResponse(Exchange exchange, Response response) {
            assertMessageHasDestinationAddress(response);
            exchange.setCurrentResponse(response);
            CoapEndpoint.this.matcher.sendResponse(exchange);
            CoapEndpoint.this.notifySend((List<MessageInterceptor>) CoapEndpoint.this.interceptors, response);
            response.setReadyToSend();
            if (!CoapEndpoint.this.started) {
                response.cancel();
            }
            if (!response.isCanceled() && response.getSendError() == null) {
                RawData data = CoapEndpoint.this.serializer.serializeResponse(response, new ExchangeCallback<Response>(exchange, response) { // from class: org.eclipse.californium.core.network.CoapEndpoint.OutboxImpl.2
                    /* JADX INFO: Access modifiers changed from: protected */
                    @Override // org.eclipse.californium.core.network.CoapEndpoint.SendingCallback
                    public void notifyPostProcess(Response response2) {
                        CoapEndpoint.this.notifySend((List<MessageInterceptor>) CoapEndpoint.this.postProcessInterceptors, response2);
                        if (CoapEndpoint.this.useRequestOffloading) {
                            this.exchange.getCurrentRequest().offload(Message.OffloadMode.FULL);
                            response2.offload(Message.OffloadMode.PAYLOAD);
                        }
                    }
                });
                if (response.isConfirmable() && exchange.getFailedTransmissionCount() == 0) {
                    exchange.startTransmissionRtt();
                }
                CoapEndpoint.this.connector.send(data);
                return;
            }
            exchange.executeComplete();
        }

        @Override // org.eclipse.californium.core.network.Outbox
        public void sendEmptyMessage(Exchange exchange, EmptyMessage message) {
            assertMessageHasDestinationAddress(message);
            CoapEndpoint.this.matcher.sendEmptyMessage(exchange, message);
            CoapEndpoint.this.notifySend((List<MessageInterceptor>) CoapEndpoint.this.interceptors, message);
            message.setReadyToSend();
            if (!CoapEndpoint.this.started) {
                message.cancel();
            }
            if (message.isCanceled() || message.getSendError() != null) {
                if (null != exchange) {
                    exchange.executeComplete();
                }
            } else if (exchange != null) {
                CoapEndpoint.this.connector.send(CoapEndpoint.this.serializer.serializeEmptyMessage(message, new ExchangeCallback<EmptyMessage>(exchange, message) { // from class: org.eclipse.californium.core.network.CoapEndpoint.OutboxImpl.3
                    /* JADX INFO: Access modifiers changed from: protected */
                    @Override // org.eclipse.californium.core.network.CoapEndpoint.SendingCallback
                    public void notifyPostProcess(EmptyMessage message2) {
                        CoapEndpoint.this.notifySend((List<MessageInterceptor>) CoapEndpoint.this.postProcessInterceptors, message2);
                    }
                }));
            } else {
                CoapEndpoint.this.connector.send(CoapEndpoint.this.serializer.serializeEmptyMessage(message, new SendingCallback<EmptyMessage>(message) { // from class: org.eclipse.californium.core.network.CoapEndpoint.OutboxImpl.4
                    /* JADX INFO: Access modifiers changed from: protected */
                    @Override // org.eclipse.californium.core.network.CoapEndpoint.SendingCallback
                    public void notifyPostProcess(EmptyMessage message2) {
                        CoapEndpoint.this.notifySend((List<MessageInterceptor>) CoapEndpoint.this.postProcessInterceptors, message2);
                    }
                }));
            }
        }

        private void assertMessageHasDestinationAddress(Message message) {
            if (message.getDestinationContext() == null) {
                throw new IllegalArgumentException("Message has no endpoint context");
            }
        }
    }

    /* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-californium-core.jar:org/eclipse/californium/core/network/CoapEndpoint$InboxImpl.class */
    private class InboxImpl implements RawDataChannel {
        private InboxImpl() {
        }

        @Override // org.eclipse.californium.elements.RawDataChannel
        public void receiveData(final RawData raw) {
            if (raw.getEndpointContext() == null) {
                throw new IllegalArgumentException("received message that does not have a endpoint context");
            }
            if (raw.getEndpointContext().getPeerAddress() == null) {
                throw new IllegalArgumentException("received message that does not have a source address");
            }
            if (raw.getEndpointContext().getPeerAddress().getPort() == 0) {
                throw new IllegalArgumentException("received message that does not have a source port");
            }
            CoapEndpoint.this.execute(new Runnable() { // from class: org.eclipse.californium.core.network.CoapEndpoint.InboxImpl.1
                @Override // java.lang.Runnable
                public void run() {
                    InboxImpl.this.receiveMessage(raw);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void receiveMessage(RawData raw) {
            char last;
            Message msg;
            EndpointContext context = raw.getEndpointContext();
            Exception ex = null;
            try {
                msg = CoapEndpoint.this.parser.parseMessage(raw);
            } catch (CoAPMessageFormatException e) {
                ex = e;
                if (!e.isConfirmable() || !e.hasMid() || raw.isMulticast()) {
                    CoapEndpoint.LOGGER.debug("{}discarding malformed message from [{}]: {}", new Object[]{CoapEndpoint.this.tag, context, e.getMessage()});
                } else if (CoAP.isRequest(e.getCode()) && e.getToken() != null) {
                    responseBadOption(raw, e);
                    CoapEndpoint.LOGGER.debug("{}respond malformed request from [{}], reason: {}", new Object[]{CoapEndpoint.this.tag, context, e.getMessage()});
                } else {
                    reject(raw, e);
                    CoapEndpoint.LOGGER.debug("{}rejected malformed message from [{}], reason: {}", new Object[]{CoapEndpoint.this.tag, context, e.getMessage()});
                }
            } catch (MessageFormatException e2) {
                ex = e2;
                CoapEndpoint.LOGGER.debug("{}discarding malformed message from [{}]: {}", new Object[]{CoapEndpoint.this.tag, context, e2.getMessage()});
            }
            if (CoAP.isRequest(msg.getRawCode())) {
                receiveRequest((Request) msg);
                return;
            }
            if (CoAP.isResponse(msg.getRawCode())) {
                if (raw.isMulticast()) {
                    CoapEndpoint.LOGGER.debug("{}multicast-receiver silently ignoring responses from {}", CoapEndpoint.this.tag, raw.getEndpointContext());
                    return;
                } else {
                    receiveResponse((Response) msg);
                    return;
                }
            }
            if (CoAP.isEmptyMessage(msg.getRawCode())) {
                if (raw.isMulticast()) {
                    CoapEndpoint.LOGGER.debug("{}multicast-receiver silently ignoring empty messages from {}", CoapEndpoint.this.tag, raw.getEndpointContext());
                    return;
                } else {
                    receiveEmptyMessage((EmptyMessage) msg);
                    return;
                }
            }
            if (!raw.isMulticast()) {
                CoapEndpoint.LOGGER.debug("{}silently ignoring non-CoAP message from {}", CoapEndpoint.this.tag, context);
            } else {
                CoapEndpoint.LOGGER.debug("{}multicast-receiver silently ignoring non-CoAP message from {}", CoapEndpoint.this.tag, raw.getEndpointContext());
            }
            if (CoapEndpoint.LOGGER_BAN.isInfoEnabled()) {
                String address = context.getPeerAddress().getAddress().getHostAddress();
                String protocol = CoapEndpoint.this.connector.getProtocol();
                StringBuilder message = new StringBuilder();
                if (ex != null) {
                    message.append(ex.getMessage().trim());
                    int len = message.length();
                    if (len > 0 && (last = message.charAt(len - 1)) != '.' && last != '!' && last != ';' && last != '#') {
                        if (last == ':') {
                            message.setLength(len - 1);
                        }
                        message.append(";");
                    }
                    message.append(" ");
                }
                message.append(StringUtil.byteArray2HexString(raw.getBytes(), (char) 0, 8));
                CoapEndpoint.LOGGER_BAN.info("{}{} {} Ban: {}", new Object[]{CoapEndpoint.this.tag, message, protocol, address});
            }
        }

        private void responseBadOption(RawData raw, CoAPMessageFormatException cause) {
            Response response = new Response(cause.getErrorCode());
            response.setDestinationContext(raw.getEndpointContext());
            response.setToken(cause.getToken());
            response.setMID(cause.getMid());
            response.setType(CoAP.Type.ACK);
            response.setPayload(cause.getMessage());
            CoapEndpoint.this.notifySend((List<MessageInterceptor>) CoapEndpoint.this.interceptors, response);
            response.setReadyToSend();
            if (!CoapEndpoint.this.started) {
                response.cancel();
            }
            RawData data = CoapEndpoint.this.serializer.serializeResponse(response, new SendingCallback<Response>(response) { // from class: org.eclipse.californium.core.network.CoapEndpoint.InboxImpl.2
                /* JADX INFO: Access modifiers changed from: protected */
                @Override // org.eclipse.californium.core.network.CoapEndpoint.SendingCallback
                public void notifyPostProcess(Response response2) {
                    CoapEndpoint.this.notifySend((List<MessageInterceptor>) CoapEndpoint.this.postProcessInterceptors, response2);
                }
            });
            CoapEndpoint.this.connector.send(data);
        }

        private void reject(RawData raw, CoAPMessageFormatException cause) {
            EmptyMessage rst = new EmptyMessage(CoAP.Type.RST);
            rst.setMID(cause.getMid());
            rst.setDestinationContext(raw.getEndpointContext());
            CoapEndpoint.this.coapstack.sendEmptyMessage(null, rst);
        }

        private void receiveRequest(Request request) {
            request.setScheme(CoapEndpoint.this.scheme);
            if (!CoapEndpoint.this.started) {
                CoapEndpoint.LOGGER.debug("{}not running, drop request {}", CoapEndpoint.this.tag, request);
                return;
            }
            CoapEndpoint.this.notifyReceive((List<MessageInterceptor>) CoapEndpoint.this.interceptors, request);
            if (!request.isCanceled()) {
                CoapEndpoint.this.matcher.receiveRequest(request, CoapEndpoint.this.endpointStackReceiver);
            }
        }

        private void receiveResponse(Response response) {
            CoapEndpoint.this.notifyReceive((List<MessageInterceptor>) CoapEndpoint.this.interceptors, response);
            if (!response.isCanceled()) {
                CoapEndpoint.this.matcher.receiveResponse(response, CoapEndpoint.this.endpointStackReceiver);
            }
        }

        private void receiveEmptyMessage(EmptyMessage message) {
            CoapEndpoint.this.notifyReceive((List<MessageInterceptor>) CoapEndpoint.this.interceptors, message);
            if (!message.isCanceled()) {
                if ((message.getType() == CoAP.Type.CON || message.getType() == CoAP.Type.NON) && message.hasMID()) {
                    CoapEndpoint.LOGGER.debug("{}responding to ping from {}", CoapEndpoint.this.tag, message.getSourceContext());
                    CoapEndpoint.this.endpointStackReceiver.reject(message);
                } else {
                    if (!CoapEndpoint.this.isMulticastMid(message.getMID())) {
                        CoapEndpoint.this.matcher.receiveEmptyMessage(message, CoapEndpoint.this.endpointStackReceiver);
                        return;
                    }
                    CoapEndpoint.LOGGER.debug("{} silently ignoring empty messages for multicast request {}", CoapEndpoint.this.tag, message.getSourceContext());
                    message.setCanceled(true);
                    CoapEndpoint.this.endpointStackReceiver.receiveEmptyMessage(null, message);
                }
            }
        }
    }

    /* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-californium-core.jar:org/eclipse/californium/core/network/CoapEndpoint$SendingCallback.class */
    private static abstract class SendingCallback<T extends Message> implements MessageCallback {
        private final T message;

        protected abstract void notifyPostProcess(T t);

        public SendingCallback(T message) {
            if (null == message) {
                throw new NullPointerException("message must not be null");
            }
            this.message = message;
        }

        @Override // org.eclipse.californium.elements.MessageCallback
        public void onConnecting() {
            this.message.onConnecting();
        }

        @Override // org.eclipse.californium.elements.MessageCallback
        public void onDtlsRetransmission(int flight) {
            this.message.onDtlsRetransmission(flight);
        }

        @Override // org.eclipse.californium.elements.MessageCallback
        public final void onContextEstablished(EndpointContext context) {
            long now = ClockUtil.nanoRealtime();
            this.message.setNanoTimestamp(now);
            onContextEstablished(context, now);
        }

        @Override // org.eclipse.californium.elements.MessageCallback
        public void onSent() {
            if (this.message.isSent()) {
                this.message.setDuplicate(true);
            }
            this.message.setSent(true);
            notifyPostProcess(this.message);
        }

        @Override // org.eclipse.californium.elements.MessageCallback
        public void onError(Throwable error) {
            this.message.setSendError(error);
            notifyPostProcess(this.message);
        }

        protected void onContextEstablished(EndpointContext context, long nanoTimestamp) {
        }
    }

    /* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-californium-core.jar:org/eclipse/californium/core/network/CoapEndpoint$ExchangeCallback.class */
    private static abstract class ExchangeCallback<T extends Message> extends SendingCallback<T> {
        protected final Exchange exchange;

        public ExchangeCallback(Exchange exchange, T message) {
            super(message);
            if (null == exchange) {
                throw new NullPointerException("exchange must not be null");
            }
            this.exchange = exchange;
        }

        @Override // org.eclipse.californium.core.network.CoapEndpoint.SendingCallback
        protected void onContextEstablished(EndpointContext context, long nanoTimestamp) {
            this.exchange.setSendNanoTimestamp(nanoTimestamp == 0 ? -1L : nanoTimestamp);
            this.exchange.setEndpointContext(context);
        }
    }

    @Override // org.eclipse.californium.core.network.Endpoint
    public void cancelObservation(Token token) {
        this.matcher.cancelObserve(token);
    }

    @Override // java.util.concurrent.Executor
    public void execute(final Runnable task) {
        Executor exchangeExecutor = this.executor;
        if (exchangeExecutor == null) {
            LOGGER.error("{}Executor not ready!", this.tag, new Throwable("execution failed!"));
            return;
        }
        try {
            exchangeExecutor.execute(new Runnable() { // from class: org.eclipse.californium.core.network.CoapEndpoint.6
                @Override // java.lang.Runnable
                public void run() {
                    try {
                        task.run();
                    } catch (Throwable t) {
                        CoapEndpoint.LOGGER.error("{}exception in protocol stage thread: {}", new Object[]{CoapEndpoint.this.tag, t.getMessage(), t});
                    }
                }
            });
        } catch (RejectedExecutionException e) {
            LOGGER.debug("{} execute:", this.tag, e);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    /* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-californium-core.jar:org/eclipse/californium/core/network/CoapEndpoint$Builder.class */
    public static class Builder {
        private Configuration config = null;
        private InetSocketAddress bindAddress = null;
        private Connector connector = null;
        private ObservationStore observationStore = null;
        private MessageExchangeStore exchangeStore = null;
        private EndpointContextMatcher endpointContextMatcher = null;
        private TokenGenerator tokenGenerator;
        private CoapStackFactory coapStackFactory;
        private DataSerializer serializer;
        private DataParser parser;
        private String tag;
        private Object customStackArgument;

        public Builder setConfiguration(Configuration config) {
            this.config = config;
            return this;
        }

        public Builder setPort(int port) {
            if (this.bindAddress != null || this.connector != null) {
                throw new IllegalStateException("bind address already defined!");
            }
            this.bindAddress = new InetSocketAddress(port);
            return this;
        }

        public Builder setInetSocketAddress(InetSocketAddress address) {
            if (this.bindAddress != null || this.connector != null) {
                throw new IllegalStateException("bind address already defined!");
            }
            this.bindAddress = address;
            return this;
        }

        public Builder setConnector(Connector connector) {
            if (this.bindAddress != null || this.connector != null) {
                throw new IllegalStateException("bind address already defined!");
            }
            if ((connector instanceof UdpMulticastConnector) && ((UdpMulticastConnector) connector).isMutlicastReceiver()) {
                throw new IllegalStateException("connector must not be a multicast receiver!");
            }
            this.connector = connector;
            return this;
        }

        public Builder setObservationStore(ObservationStore store) {
            this.observationStore = store;
            return this;
        }

        public Builder setMessageExchangeStore(MessageExchangeStore exchangeStore) {
            this.exchangeStore = exchangeStore;
            return this;
        }

        public Builder setEndpointContextMatcher(EndpointContextMatcher endpointContextMatcher) {
            this.endpointContextMatcher = endpointContextMatcher;
            return this;
        }

        public Builder setTokenGenerator(TokenGenerator tokenGenerator) {
            this.tokenGenerator = tokenGenerator;
            return this;
        }

        public Builder setCoapStackFactory(CoapStackFactory coapStackFactory) {
            this.coapStackFactory = coapStackFactory;
            return this;
        }

        public Builder setDataSerializerAndParser(DataSerializer serializer, DataParser parser) {
            this.serializer = serializer;
            this.parser = parser;
            return this;
        }

        public Builder setLoggingTag(String tag) {
            this.tag = tag;
            return this;
        }

        public Builder setCustomCoapStackArgument(Object customStackArgument) {
            this.customStackArgument = customStackArgument;
            return this;
        }

        public CoapEndpoint build() {
            if (this.config == null) {
                this.config = Configuration.getStandard();
            }
            if (this.connector == null) {
                if (this.bindAddress == null) {
                    this.bindAddress = new InetSocketAddress(0);
                }
                this.connector = new UDPConnector(this.bindAddress, this.config);
            }
            if (this.tokenGenerator == null) {
                this.tokenGenerator = new RandomTokenGenerator(this.config);
            }
            if (this.observationStore == null) {
                this.observationStore = new InMemoryObservationStore(this.config);
            }
            if (this.endpointContextMatcher == null) {
                this.endpointContextMatcher = EndpointContextMatcherFactory.create(this.connector, this.config);
            }
            if (this.tag == null) {
                this.tag = CoAP.getSchemeForProtocol(this.connector.getProtocol());
            }
            this.tag = StringUtil.normalizeLoggingTag(this.tag);
            if (this.exchangeStore == null) {
                this.exchangeStore = new InMemoryMessageExchangeStore(this.tag, this.config, this.tokenGenerator);
            }
            if (this.coapStackFactory == null) {
                this.coapStackFactory = CoapEndpoint.getDefaultCoapStackFactory();
            }
            return new CoapEndpoint(this.connector, this.config, this.tokenGenerator, this.observationStore, this.exchangeStore, this.endpointContextMatcher, this.serializer, this.parser, this.tag, this.coapStackFactory, this.customStackArgument);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static synchronized CoapStackFactory getDefaultCoapStackFactory() {
        if (defaultCoapStackFactory == null) {
            defaultCoapStackFactory = STANDARD_COAP_STACK_FACTORY;
        }
        return defaultCoapStackFactory;
    }

    public static synchronized void setDefaultCoapStackFactory(CoapStackFactory newFactory) {
        if (defaultCoapStackFactory != null) {
            throw new IllegalStateException("Default coap-stack-factory already set!");
        }
        if (newFactory == null) {
            throw new NullPointerException("new coap-stack-factory must not be null!");
        }
        defaultCoapStackFactory = newFactory;
    }
}
