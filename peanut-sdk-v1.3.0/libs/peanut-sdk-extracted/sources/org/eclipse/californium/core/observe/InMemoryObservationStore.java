package org.eclipse.californium.core.observe;

import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.eclipse.californium.core.coap.Token;
import org.eclipse.californium.elements.EndpointContext;
import org.eclipse.californium.elements.config.Configuration;
import org.eclipse.californium.elements.config.SystemConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-californium-core.jar:org/eclipse/californium/core/observe/InMemoryObservationStore.class */
public final class InMemoryObservationStore implements ObservationStore {
    private static final Logger LOGGER = LoggerFactory.getLogger(InMemoryObservationStore.class);
    private static final Logger HEALTH_LOGGER = LoggerFactory.getLogger(LOGGER.getName() + ".health");
    private final ConcurrentMap<Token, Observation> map = new ConcurrentHashMap();
    private volatile boolean enableStatus;
    private final Configuration config;
    private ScheduledFuture<?> statusLogger;
    private ScheduledExecutorService executor;

    public InMemoryObservationStore(Configuration config) {
        this.config = config;
    }

    @Override // org.eclipse.californium.core.observe.ObservationStore
    public void setExecutor(ScheduledExecutorService executor) {
        this.executor = executor;
    }

    @Override // org.eclipse.californium.core.observe.ObservationStore
    public Observation putIfAbsent(Token key, Observation obs) {
        if (key == null) {
            throw new NullPointerException("token must not be null");
        }
        if (obs == null) {
            throw new NullPointerException("observation must not be null");
        }
        this.enableStatus = true;
        Observation result = this.map.putIfAbsent(key, obs);
        if (result == null) {
            LOGGER.debug("added observation for {}", key);
        } else {
            LOGGER.debug("kept observation {} for {}", result, key);
        }
        return result;
    }

    @Override // org.eclipse.californium.core.observe.ObservationStore
    public Observation put(Token key, Observation obs) {
        if (key == null) {
            throw new NullPointerException("token must not be null");
        }
        if (obs == null) {
            throw new NullPointerException("observation must not be null");
        }
        this.enableStatus = true;
        Observation result = this.map.put(key, obs);
        if (result == null) {
            LOGGER.debug("added observation for {}", key);
        } else {
            LOGGER.debug("replaced observation {} for {}", result, key);
        }
        return result;
    }

    @Override // org.eclipse.californium.core.observe.ObservationStore
    public Observation get(Token token) {
        if (token == null) {
            return null;
        }
        Observation obs = this.map.get(token);
        LOGGER.debug("looking up observation for token {}: {}", token, obs);
        return ObservationUtil.shallowClone(obs);
    }

    @Override // org.eclipse.californium.core.observe.ObservationStore
    public void remove(Token token) {
        if (token != null) {
            if (this.map.remove(token) != null) {
                LOGGER.debug("removed observation for token {}", token);
            } else {
                LOGGER.debug("Already removed observation for token {}", token);
            }
        }
    }

    public boolean isEmpty() {
        return this.map.isEmpty();
    }

    public int getSize() {
        return this.map.size();
    }

    public void clear() {
        this.map.clear();
    }

    @Override // org.eclipse.californium.core.observe.ObservationStore
    public void setContext(Token token, EndpointContext ctx) {
        Observation obs;
        if (token != null && ctx != null && (obs = this.map.get(token)) != null) {
            this.map.replace(token, obs, new Observation(obs.getRequest(), ctx));
        }
    }

    @Override // org.eclipse.californium.core.observe.ObservationStore
    public synchronized void start() {
        long healthStatusInterval = this.config.get(SystemConfig.HEALTH_STATUS_INTERVAL, TimeUnit.MILLISECONDS).longValue();
        if (healthStatusInterval > 0 && HEALTH_LOGGER.isDebugEnabled() && this.executor != null) {
            this.statusLogger = this.executor.scheduleAtFixedRate(new Runnable() { // from class: org.eclipse.californium.core.observe.InMemoryObservationStore.1
                @Override // java.lang.Runnable
                public void run() {
                    if (InMemoryObservationStore.this.enableStatus) {
                        InMemoryObservationStore.HEALTH_LOGGER.debug("{} observes", Integer.valueOf(InMemoryObservationStore.this.map.size()));
                        Iterator<Token> iterator = InMemoryObservationStore.this.map.keySet().iterator();
                        int max = 5;
                        while (iterator.hasNext()) {
                            InMemoryObservationStore.HEALTH_LOGGER.debug("   observe {}", iterator.next());
                            max--;
                            if (max == 0) {
                                return;
                            }
                        }
                    }
                }
            }, healthStatusInterval, healthStatusInterval, TimeUnit.MILLISECONDS);
        }
    }

    @Override // org.eclipse.californium.core.observe.ObservationStore
    public synchronized void stop() {
        if (this.statusLogger != null) {
            this.statusLogger.cancel(false);
            this.statusLogger = null;
        }
    }
}
