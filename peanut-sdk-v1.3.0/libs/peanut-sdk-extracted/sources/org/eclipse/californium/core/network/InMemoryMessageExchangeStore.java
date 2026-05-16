package org.eclipse.californium.core.network;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.eclipse.californium.core.coap.BlockOption;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.Message;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.californium.core.coap.Token;
import org.eclipse.californium.core.network.TokenGenerator;
import org.eclipse.californium.core.network.deduplication.Deduplicator;
import org.eclipse.californium.core.network.deduplication.DeduplicatorFactory;
import org.eclipse.californium.elements.config.Configuration;
import org.eclipse.californium.elements.config.SystemConfig;
import org.eclipse.californium.elements.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-californium-core.jar:org/eclipse/californium/core/network/InMemoryMessageExchangeStore.class */
public class InMemoryMessageExchangeStore implements MessageExchangeStore {
    private static final Logger LOGGER = LoggerFactory.getLogger(InMemoryMessageExchangeStore.class);
    private static final Logger HEALTH_LOGGER = LoggerFactory.getLogger(LOGGER.getName() + ".health");
    private final ConcurrentMap<KeyMID, Exchange> exchangesByMID;
    private final ConcurrentMap<KeyToken, Exchange> exchangesByToken;
    private volatile boolean enableStatus;
    private final Configuration config;
    private final TokenGenerator tokenGenerator;
    private final String tag;
    private volatile boolean running;
    private volatile Deduplicator deduplicator;
    private volatile MessageIdProvider messageIdProvider;
    private ScheduledExecutorService executor;
    private ScheduledFuture<?> statusLogger;

    public InMemoryMessageExchangeStore(Configuration config) {
        this(null, config, new RandomTokenGenerator(config));
    }

    public InMemoryMessageExchangeStore(Configuration config, TokenGenerator tokenProvider) {
        this(null, config, tokenProvider);
    }

    public InMemoryMessageExchangeStore(String tag, Configuration config, TokenGenerator tokenProvider) {
        this.exchangesByMID = new ConcurrentHashMap();
        this.exchangesByToken = new ConcurrentHashMap();
        this.running = false;
        if (config == null) {
            throw new NullPointerException("Configuration must not be null");
        }
        if (tokenProvider == null) {
            throw new NullPointerException("TokenProvider must not be null");
        }
        this.tokenGenerator = tokenProvider;
        this.config = config;
        this.tag = StringUtil.normalizeLoggingTag(tag);
        LOGGER.debug("{}using TokenProvider {}", tag, tokenProvider.getClass().getName());
    }

    private void startStatusLogging() {
        long healthStatusInterval = this.config.get(SystemConfig.HEALTH_STATUS_INTERVAL, TimeUnit.MILLISECONDS).longValue();
        if (healthStatusInterval > 0 && HEALTH_LOGGER.isDebugEnabled() && this.executor != null) {
            this.statusLogger = this.executor.scheduleAtFixedRate(new Runnable() { // from class: org.eclipse.californium.core.network.InMemoryMessageExchangeStore.1
                @Override // java.lang.Runnable
                public void run() {
                    if (InMemoryMessageExchangeStore.this.enableStatus) {
                        InMemoryMessageExchangeStore.this.dump(5);
                    }
                }
            }, healthStatusInterval, healthStatusInterval, TimeUnit.MILLISECONDS);
        }
    }

    private String dumpCurrentLoadLevels() {
        StringBuilder b = new StringBuilder(this.tag);
        b.append("MessageExchangeStore contents: ");
        b.append(this.exchangesByMID.size()).append(" exchanges by MID, ");
        b.append(this.exchangesByToken.size()).append(" exchanges by token, ");
        b.append(this.deduplicator.size()).append(" MIDs.");
        return b.toString();
    }

    public synchronized void setDeduplicator(Deduplicator deduplicator) {
        if (this.running) {
            throw new IllegalStateException("Cannot set Deduplicator when store is already started");
        }
        if (deduplicator == null) {
            throw new NullPointerException("Deduplicator must not be null");
        }
        this.deduplicator = deduplicator;
    }

    public synchronized void setMessageIdProvider(MessageIdProvider provider) {
        if (this.running) {
            throw new IllegalStateException("Cannot set messageIdProvider when store is already started");
        }
        if (provider == null) {
            throw new NullPointerException("Message ID Provider must not be null");
        }
        this.messageIdProvider = provider;
    }

    @Override // org.eclipse.californium.core.network.MessageExchangeStore
    public synchronized void setExecutor(ScheduledExecutorService executor) {
        if (this.running) {
            throw new IllegalStateException("Cannot set messageIdProvider when store is already started");
        }
        this.executor = executor;
    }

    @Override // org.eclipse.californium.core.network.MessageExchangeStore
    public boolean isEmpty() {
        return this.exchangesByMID.isEmpty() && this.exchangesByToken.isEmpty() && this.deduplicator.isEmpty();
    }

    public String toString() {
        return dumpCurrentLoadLevels();
    }

    @Override // org.eclipse.californium.core.network.MessageExchangeStore
    public int assignMessageId(Message message) {
        int mid = message.getMID();
        if (-1 == mid) {
            InetSocketAddress dest = message.getDestinationContext().getPeerAddress();
            try {
                mid = this.messageIdProvider.getNextMessageId(dest);
                message.setMID(mid);
            } catch (IllegalStateException ex) {
                String code = CoAP.toCodeString(message.getRawCode());
                LOGGER.debug("{}cannot send message {}-{} to {}, {}", new Object[]{this.tag, message.getType(), code, StringUtil.toLog(dest), ex.getMessage()});
            }
        }
        return mid;
    }

    private KeyMID registerWithMessageId(Exchange exchange, Message message) {
        KeyMID key;
        this.enableStatus = true;
        exchange.assertIncomplete(message);
        int mid = message.getMID();
        if (-1 == mid) {
            int mid2 = assignMessageId(message);
            if (-1 != mid2) {
                key = new KeyMID(mid2, exchange.getPeersIdentity());
                if (this.exchangesByMID.putIfAbsent(key, exchange) != null) {
                    throw new IllegalArgumentException(String.format("generated mid [%d] already in use, cannot register %s", Integer.valueOf(mid2), exchange));
                }
                LOGGER.debug("{}{} added with generated mid {}, {}", new Object[]{this.tag, exchange, key, message});
            } else {
                key = null;
            }
        } else {
            key = new KeyMID(mid, exchange.getPeersIdentity());
            Exchange existingExchange = this.exchangesByMID.putIfAbsent(key, exchange);
            if (existingExchange != null) {
                if (existingExchange != exchange) {
                    throw new IllegalArgumentException(String.format("mid [%d] already in use, cannot register %s", Integer.valueOf(mid), exchange));
                }
                if (exchange.getFailedTransmissionCount() == 0) {
                    throw new IllegalArgumentException(String.format("message with already registered mid [%d] is not a re-transmission, cannot register %s", Integer.valueOf(mid), exchange));
                }
            } else {
                LOGGER.debug("{}{} added with {}, {}", new Object[]{this.tag, exchange, key, message});
            }
        }
        if (key != null) {
            exchange.setKeyMID(key);
        }
        return key;
    }

    private void registerWithToken(Exchange exchange) {
        KeyToken key;
        this.enableStatus = true;
        Request request = exchange.getCurrentRequest();
        exchange.assertIncomplete(request);
        Token token = request.getToken();
        if (token == null) {
            TokenGenerator.Scope scope = request.isMulticast() ? TokenGenerator.Scope.SHORT_TERM : TokenGenerator.Scope.SHORT_TERM_CLIENT_LOCAL;
            do {
                Token token2 = this.tokenGenerator.createToken(scope);
                request.setToken(token2);
                key = this.tokenGenerator.getKeyToken(token2, exchange.getPeersIdentity());
            } while (this.exchangesByToken.putIfAbsent(key, exchange) != null);
            LOGGER.debug("{}{} added with generated token {}, {}", new Object[]{this.tag, exchange, key, request});
        } else {
            if (token.isEmpty() && request.getCode() == null) {
                return;
            }
            key = this.tokenGenerator.getKeyToken(token, exchange.getPeersIdentity());
            Exchange previous = this.exchangesByToken.put(key, exchange);
            if (previous == null) {
                BlockOption block2 = request.getOptions().getBlock2();
                if (block2 != null) {
                    LOGGER.debug("{}block2 {} for block {} add with token {}", new Object[]{this.tag, exchange, Integer.valueOf(block2.getNum()), key});
                } else {
                    LOGGER.debug("{}{} added with token {}, {}", new Object[]{this.tag, exchange, key, request});
                }
            } else if (previous != exchange) {
                if (exchange.getFailedTransmissionCount() == 0 && !request.getOptions().hasBlock1() && !request.getOptions().hasBlock2() && !request.getOptions().hasObserve()) {
                    LOGGER.warn("{}{} with manual token overrides existing {} with open request: {}", new Object[]{this.tag, exchange, previous, key});
                } else {
                    LOGGER.debug("{}{} replaced with token {}, {}", new Object[]{this.tag, exchange, key, request});
                }
            } else {
                LOGGER.debug("{}{} keep for {}, {}", new Object[]{this.tag, exchange, key, request});
            }
        }
        if (key != null) {
            exchange.setKeyToken(key);
        }
    }

    @Override // org.eclipse.californium.core.network.MessageExchangeStore
    public boolean registerOutboundRequest(Exchange exchange) {
        if (exchange == null) {
            throw new NullPointerException("exchange must not be null");
        }
        if (exchange.getCurrentRequest() == null) {
            throw new IllegalArgumentException("exchange does not contain a request");
        }
        Request currentRequest = exchange.getCurrentRequest();
        KeyMID key = registerWithMessageId(exchange, currentRequest);
        if (key != null) {
            registerWithToken(exchange);
            if (exchange.getCurrentRequest() != currentRequest) {
                throw new ConcurrentModificationException("Current request modified!");
            }
            return true;
        }
        return false;
    }

    @Override // org.eclipse.californium.core.network.MessageExchangeStore
    public boolean registerOutboundRequestWithTokenOnly(Exchange exchange) {
        if (exchange == null) {
            throw new NullPointerException("exchange must not be null");
        }
        if (exchange.getCurrentRequest() == null) {
            throw new IllegalArgumentException("exchange does not contain a request");
        }
        Request currentRequest = exchange.getCurrentRequest();
        registerWithToken(exchange);
        if (exchange.getCurrentRequest() != currentRequest) {
            throw new ConcurrentModificationException("Current request modified!");
        }
        return true;
    }

    @Override // org.eclipse.californium.core.network.MessageExchangeStore
    public void remove(KeyToken token, Exchange exchange) {
        boolean removed = this.exchangesByToken.remove(token, exchange);
        if (removed) {
            LOGGER.debug("{}removing {} for token {}", new Object[]{this.tag, exchange, token});
        }
    }

    @Override // org.eclipse.californium.core.network.MessageExchangeStore
    public Exchange remove(KeyMID messageId, Exchange exchange) {
        Exchange removedExchange;
        if (null == exchange) {
            removedExchange = this.exchangesByMID.remove(messageId);
        } else if (this.exchangesByMID.remove(messageId, exchange)) {
            removedExchange = exchange;
        } else {
            removedExchange = null;
        }
        if (null != removedExchange) {
            LOGGER.debug("{}removing {} for MID {}", new Object[]{this.tag, removedExchange, messageId});
        }
        return removedExchange;
    }

    @Override // org.eclipse.californium.core.network.MessageExchangeStore
    public Exchange get(KeyToken keyToken) {
        if (keyToken == null) {
            return null;
        }
        return this.exchangesByToken.get(keyToken);
    }

    @Override // org.eclipse.californium.core.network.MessageExchangeStore
    public Exchange get(KeyMID messageId) {
        if (messageId == null) {
            return null;
        }
        return this.exchangesByMID.get(messageId);
    }

    @Override // org.eclipse.californium.core.network.MessageExchangeStore
    public boolean registerOutboundResponse(Exchange exchange) {
        if (exchange == null) {
            throw new NullPointerException("exchange must not be null");
        }
        if (exchange.getCurrentResponse() == null) {
            throw new IllegalArgumentException("exchange does not contain a response");
        }
        Response currentResponse = exchange.getCurrentResponse();
        if (registerWithMessageId(exchange, currentResponse) != null) {
            if (exchange.getCurrentResponse() != currentResponse) {
                throw new ConcurrentModificationException("Current response modified!");
            }
            return true;
        }
        return false;
    }

    @Override // org.eclipse.californium.core.network.MessageExchangeStore
    public synchronized void start() {
        if (!this.running) {
            startStatusLogging();
            if (this.deduplicator == null) {
                DeduplicatorFactory factory = DeduplicatorFactory.getDeduplicatorFactory();
                this.deduplicator = factory.createDeduplicator(this.config);
            }
            this.deduplicator.setExecutor(this.executor);
            this.deduplicator.start();
            if (this.messageIdProvider == null) {
                LOGGER.debug("{}no MessageIdProvider set, using default {}", this.tag, InMemoryMessageIdProvider.class.getName());
                this.messageIdProvider = new InMemoryMessageIdProvider(this.config);
            }
            this.running = true;
        }
    }

    @Override // org.eclipse.californium.core.network.MessageExchangeStore
    public synchronized void stop() {
        if (this.running) {
            this.running = false;
            for (Exchange exchange : this.exchangesByMID.values()) {
                exchange.getRequest().setCanceled(true);
            }
            if (this.statusLogger != null) {
                this.statusLogger.cancel(false);
                this.statusLogger = null;
            }
            this.deduplicator.stop();
            this.exchangesByMID.clear();
            this.exchangesByToken.clear();
        }
    }

    public void dump(int logMaxExchanges) {
        if (HEALTH_LOGGER.isDebugEnabled()) {
            HEALTH_LOGGER.debug(dumpCurrentLoadLevels());
            if (0 < logMaxExchanges) {
                if (!this.exchangesByMID.isEmpty()) {
                    dumpExchanges(logMaxExchanges, this.exchangesByMID.entrySet());
                }
                if (!this.exchangesByToken.isEmpty()) {
                    dumpExchanges(logMaxExchanges, this.exchangesByToken.entrySet());
                }
            }
        }
    }

    private <K> void dumpExchanges(int logMaxExchanges, Set<Map.Entry<K, Exchange>> exchangeEntries) {
        for (Map.Entry<K, Exchange> exchangeEntry : exchangeEntries) {
            Exchange exchange = exchangeEntry.getValue();
            Request origin = exchange.getRequest();
            Request current = exchange.getCurrentRequest();
            String pending = exchange.isTransmissionPending() ? "/pending" : "";
            if (origin != null && origin != current && !origin.getToken().equals(current.getToken())) {
                HEALTH_LOGGER.debug("  {}, {}, retransmission {}{}, org {}, {}, {}", new Object[]{exchangeEntry.getKey(), exchange, Integer.valueOf(exchange.getFailedTransmissionCount()), pending, origin.getToken(), current, exchange.getCurrentResponse()});
            } else {
                String mark = origin == null ? "(missing origin request) " : "";
                HEALTH_LOGGER.debug("  {}, {}, retransmission {}{}, {}{}, {}", new Object[]{exchangeEntry.getKey(), exchange, Integer.valueOf(exchange.getFailedTransmissionCount()), pending, mark, current, exchange.getCurrentResponse()});
            }
            Throwable caller = exchange.getCaller();
            if (caller != null) {
                HEALTH_LOGGER.trace("  ", caller);
            }
            logMaxExchanges--;
            if (0 >= logMaxExchanges) {
                return;
            }
        }
    }

    @Override // org.eclipse.californium.core.network.MessageExchangeStore
    public Exchange findPrevious(KeyMID messageId, Exchange exchange) {
        return this.deduplicator.findPrevious(messageId, exchange);
    }

    @Override // org.eclipse.californium.core.network.MessageExchangeStore
    public boolean replacePrevious(KeyMID key, Exchange previous, Exchange exchange) {
        return this.deduplicator.replacePrevious(key, previous, exchange);
    }

    @Override // org.eclipse.californium.core.network.MessageExchangeStore
    public Exchange find(KeyMID messageId) {
        return this.deduplicator.find(messageId);
    }

    @Override // org.eclipse.californium.core.network.MessageExchangeStore
    public List<Exchange> findByToken(Token token) {
        Request request;
        List<Exchange> result = new ArrayList<>();
        if (token != null) {
            if (this.tokenGenerator.getScope(token) == TokenGenerator.Scope.SHORT_TERM_CLIENT_LOCAL) {
                throw new IllegalArgumentException("token must not have client-local scope!");
            }
            for (Map.Entry<KeyToken, Exchange> entry : this.exchangesByToken.entrySet()) {
                if (entry.getValue().isOfLocalOrigin() && (request = entry.getValue().getRequest()) != null && token.equals(request.getToken())) {
                    result.add(entry.getValue());
                }
            }
        }
        return result;
    }
}
