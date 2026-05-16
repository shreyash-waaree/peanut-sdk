package org.eclipse.californium.core.network.stack;

import com.keenon.sdk.component.navigation.arrival.ArrivalControl;
import java.net.InetSocketAddress;
import java.util.Queue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.EmptyMessage;
import org.eclipse.californium.core.coap.Message;
import org.eclipse.californium.core.coap.MessageObserverAdapter;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.californium.core.config.CoapConfig;
import org.eclipse.californium.core.network.Exchange;
import org.eclipse.californium.core.network.stack.RemoteEndpoint;
import org.eclipse.californium.core.network.stack.congestioncontrol.BasicRto;
import org.eclipse.californium.core.network.stack.congestioncontrol.Cocoa;
import org.eclipse.californium.core.network.stack.congestioncontrol.CongestionStatisticLogger;
import org.eclipse.californium.core.network.stack.congestioncontrol.LinuxRto;
import org.eclipse.californium.core.network.stack.congestioncontrol.PeakhopperRto;
import org.eclipse.californium.core.observe.ObserveRelation;
import org.eclipse.californium.elements.config.Configuration;
import org.eclipse.californium.elements.util.LeastRecentlyUsedCache;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-californium-core.jar:org/eclipse/californium/core/network/stack/CongestionControlLayer.class */
public abstract class CongestionControlLayer extends ReliabilityLayer {
    private static final int EXCHANGELIMIT = 50;
    private static final int MIN_RTO = 500;
    private static final int MAX_RTO = 60000;
    private LeastRecentlyUsedCache<InetSocketAddress, RemoteEndpoint> remoteEndpoints;
    protected final Configuration config;
    protected final String tag;
    private boolean appliesDithering;
    private CongestionStatisticLogger statistic;

    protected abstract RemoteEndpoint createRemoteEndpoint(InetSocketAddress inetSocketAddress);

    public CongestionControlLayer(String tag, Configuration config) {
        super(config);
        this.tag = tag;
        this.config = config;
        this.remoteEndpoints = new LeastRecentlyUsedCache<>(((Integer) config.get(CoapConfig.MAX_ACTIVE_PEERS)).intValue(), config.get(CoapConfig.MAX_PEER_INACTIVITY_PERIOD, TimeUnit.SECONDS).longValue());
        this.remoteEndpoints.setEvictingOnReadAccess(false);
        setDithering(false);
    }

    @Override // org.eclipse.californium.core.network.stack.AbstractLayer, org.eclipse.californium.core.network.stack.Layer
    public void start() {
        this.statistic = new CongestionStatisticLogger(this.tag, ArrivalControl.DEFAULT_BLOCK_DELAY, TimeUnit.MILLISECONDS, this.executor);
        this.statistic.start();
    }

    @Override // org.eclipse.californium.core.network.stack.AbstractLayer, org.eclipse.californium.core.network.stack.Layer
    public void destroy() {
        CongestionStatisticLogger statistic = this.statistic;
        if (statistic != null) {
            if (statistic.stop()) {
                statistic.dump();
            }
            this.statistic = null;
        }
    }

    protected RemoteEndpoint getRemoteEndpoint(Exchange exchange) {
        Message message;
        RemoteEndpoint remoteEndpoint;
        if (exchange.isOfLocalOrigin()) {
            message = exchange.getCurrentRequest();
        } else {
            message = exchange.getCurrentResponse();
        }
        InetSocketAddress remoteSocketAddress = message.getDestinationContext().getPeerAddress();
        synchronized (this.remoteEndpoints) {
            RemoteEndpoint remoteEndpoint2 = this.remoteEndpoints.get(remoteSocketAddress);
            if (remoteEndpoint2 == null) {
                remoteEndpoint2 = createRemoteEndpoint(remoteSocketAddress);
                this.remoteEndpoints.put(remoteSocketAddress, remoteEndpoint2);
            }
            remoteEndpoint = remoteEndpoint2;
        }
        return remoteEndpoint;
    }

    public boolean appliesDithering() {
        return this.appliesDithering;
    }

    public void setDithering(boolean mode) {
        this.appliesDithering = mode;
    }

    public RemoteEndpoint.RtoType getExchangeEstimatorState(Exchange exchange) {
        int failed = exchange.getFailedTransmissionCount();
        switch (failed) {
            case 0:
                return RemoteEndpoint.RtoType.STRONG;
            case 1:
            case 2:
                return RemoteEndpoint.RtoType.WEAK;
            default:
                return RemoteEndpoint.RtoType.NONE;
        }
    }

    private boolean processResponse(RemoteEndpoint endpoint, Exchange exchange, Response response) {
        int size;
        CoAP.Type messageType = response.getType();
        if (!response.isNotification()) {
            if (messageType == CoAP.Type.CON) {
                return checkNSTART(endpoint, exchange);
            }
            return true;
        }
        boolean start = false;
        Queue<PostponedExchange> queue = endpoint.getNotifyQueue();
        synchronized (endpoint) {
            PostponedExchange postponedExchange = new PostponedExchange(exchange, response);
            queue.remove(postponedExchange);
            size = queue.size();
            if (size < 50) {
                queue.add(postponedExchange);
                start = endpoint.startProcessingNotifies();
            }
        }
        if (size >= 50) {
            LOGGER.debug("{}drop outgoing notify, queue full {}", this.tag, Integer.valueOf(size));
            return false;
        }
        if (start) {
            this.executor.execute(new BucketTask(endpoint));
            return false;
        }
        return false;
    }

    private boolean checkNSTART(RemoteEndpoint endpoint, Exchange exchange) {
        String messageType;
        CoAP.Type type;
        Queue<Exchange> queue;
        int size;
        Message message;
        boolean send = false;
        boolean queued = false;
        if (exchange.isOfLocalOrigin()) {
            messageType = "req.-";
            type = exchange.getCurrentRequest().getType();
            queue = endpoint.getRequestQueue();
        } else {
            messageType = "resp.-";
            type = exchange.getCurrentResponse().getType();
            queue = endpoint.getResponseQueue();
        }
        synchronized (endpoint) {
            size = queue.size();
            if (endpoint.registerExchange(exchange)) {
                send = true;
            } else if (size < 50) {
                queue.add(exchange);
                queued = true;
            }
        }
        if (send) {
            if (exchange.isOfLocalOrigin()) {
                message = exchange.getCurrentRequest();
            } else {
                message = exchange.getCurrentResponse();
            }
            message.addMessageObserver(new TimeoutTask(endpoint, exchange));
            LOGGER.trace("{}send {}{}", new Object[]{this.tag, messageType, type});
            if (this.statistic != null) {
                this.statistic.sendRequest();
                return true;
            }
            return true;
        }
        if (queued) {
            if (this.statistic != null) {
                this.statistic.queueRequest();
                return false;
            }
            return false;
        }
        LOGGER.debug("{}drop {}{}, queue full {}", new Object[]{this.tag, messageType, type, Integer.valueOf(size)});
        return false;
    }

    private void processRttMeasurement(Exchange exchange) {
        Long rttNanos;
        RemoteEndpoint.RtoType rtoType;
        RemoteEndpoint endpoint = getRemoteEndpoint(exchange);
        Response response = exchange.getCurrentResponse();
        if (response != null && (rttNanos = response.getTransmissionRttNanos()) != null && (rtoType = getExchangeEstimatorState(exchange)) != RemoteEndpoint.RtoType.NONE) {
            long measuredRTT = Math.max(TimeUnit.NANOSECONDS.toMillis(rttNanos.longValue()), 1L);
            endpoint.processRttMeasurement(rtoType, measuredRTT);
        }
        nextQueuedExchange(endpoint, exchange);
    }

    protected float calculateVBF(long rto, float scale) {
        return scale;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void nextQueuedExchange(final RemoteEndpoint endpoint, Exchange removeExchange) {
        String messageType;
        CoAP.Type type;
        int size;
        Exchange nextExchange = null;
        synchronized (endpoint) {
            if (endpoint.removeExchange(removeExchange)) {
                nextExchange = endpoint.getResponseQueue().poll();
                if (nextExchange == null) {
                    nextExchange = endpoint.getRequestQueue().poll();
                }
                if (nextExchange != null) {
                    endpoint.registerExchange(nextExchange);
                }
            }
        }
        if (nextExchange != null) {
            this.statistic.dequeueRequest();
            final Exchange exchange = nextExchange;
            if (exchange.isOfLocalOrigin()) {
                messageType = "req.-";
                type = exchange.getCurrentRequest().getType();
                size = endpoint.getRequestQueue().size();
            } else {
                messageType = "resp.-";
                type = exchange.getCurrentResponse().getType();
                size = endpoint.getResponseQueue().size();
            }
            LOGGER.trace("{}send from queue {}{}, queue left {}", new Object[]{this.tag, messageType, type, Integer.valueOf(size)});
            exchange.execute(new Runnable() { // from class: org.eclipse.californium.core.network.stack.CongestionControlLayer.1
                @Override // java.lang.Runnable
                public void run() {
                    if (exchange.isComplete()) {
                        CongestionControlLayer.this.nextQueuedExchange(endpoint, exchange);
                    } else if (exchange.isOfLocalOrigin()) {
                        CongestionControlLayer.this.sendRequest(exchange, exchange.getCurrentRequest());
                    } else {
                        CongestionControlLayer.this.sendResponse(exchange, exchange.getCurrentResponse());
                    }
                }
            });
        }
    }

    @Override // org.eclipse.californium.core.network.stack.ReliabilityLayer, org.eclipse.californium.core.network.stack.AbstractLayer, org.eclipse.californium.core.network.stack.Layer
    public void sendRequest(Exchange exchange, Request request) {
        if (exchange.getFailedTransmissionCount() > 0) {
            LOGGER.warn("{}retransmission in sendRequest", this.tag, new Throwable("retransmission"));
            return;
        }
        prepareRequest(exchange, request);
        RemoteEndpoint endpoint = getRemoteEndpoint(exchange);
        if (checkNSTART(endpoint, exchange)) {
            endpoint.checkAging();
            LOGGER.debug("{}send request", this.tag);
            if (!endpoint.inFlightExchange(exchange)) {
                LOGGER.warn("{}unregistered request", this.tag, new Throwable("unregistered request"));
            }
            lower().sendRequest(exchange, request);
        }
    }

    @Override // org.eclipse.californium.core.network.stack.ReliabilityLayer, org.eclipse.californium.core.network.stack.AbstractLayer, org.eclipse.californium.core.network.stack.Layer
    public void sendResponse(Exchange exchange, Response response) {
        RemoteEndpoint endpoint = getRemoteEndpoint(exchange);
        prepareResponse(exchange, response);
        if (exchange.getFailedTransmissionCount() > 0) {
            if (response.isNotification()) {
                lower().sendResponse(exchange, response);
                return;
            } else {
                LOGGER.warn("{}retransmission in sendResponse", this.tag, new Throwable("retransmission"));
                return;
            }
        }
        if (processResponse(endpoint, exchange, response)) {
            endpoint.checkAging();
            lower().sendResponse(exchange, response);
        }
    }

    @Override // org.eclipse.californium.core.network.stack.ReliabilityLayer
    protected void updateRetransmissionTimeout(Exchange exchange, ReliabilityLayerParameters reliabilityLayerParameters) {
        int timeout;
        int timeout2;
        int maxTimeout = Math.min(reliabilityLayerParameters.getMaxAckTimeout(), MAX_RTO);
        RemoteEndpoint remoteEndpoint = getRemoteEndpoint(exchange);
        if (exchange.getFailedTransmissionCount() == 0) {
            if (this.defaultReliabilityLayerParameters == reliabilityLayerParameters) {
                timeout2 = (int) remoteEndpoint.getRTO();
            } else {
                timeout2 = reliabilityLayerParameters.getAckTimeout();
            }
            if (appliesDithering()) {
                timeout2 = getRandomTimeout(timeout2, reliabilityLayerParameters.getAckRandomFactor());
            }
            timeout = Math.min(maxTimeout, Math.max(MIN_RTO, timeout2));
            float scale = calculateVBF(timeout, reliabilityLayerParameters.getAckTimeoutScale());
            exchange.setTimeoutScale(scale);
        } else {
            int timeout3 = (int) (exchange.getTimeoutScale() * exchange.getCurrentTimeout());
            timeout = Math.min(maxTimeout, timeout3);
        }
        exchange.setCurrentTimeout(timeout);
    }

    @Override // org.eclipse.californium.core.network.stack.ReliabilityLayer, org.eclipse.californium.core.network.stack.AbstractLayer, org.eclipse.californium.core.network.stack.Layer
    public void receiveResponse(Exchange exchange, Response response) {
        LOGGER.debug("{}receive response", this.tag);
        if (processResponse(exchange, response)) {
            processRttMeasurement(exchange);
            if (this.statistic != null) {
                this.statistic.receiveResponse(response);
            }
            upper().receiveResponse(exchange, response);
        }
    }

    @Override // org.eclipse.californium.core.network.stack.ReliabilityLayer, org.eclipse.californium.core.network.stack.AbstractLayer, org.eclipse.californium.core.network.stack.Layer
    public void receiveEmptyMessage(Exchange exchange, EmptyMessage message) {
        if (processEmptyMessage(exchange, message)) {
            processRttMeasurement(exchange);
            upper().receiveEmptyMessage(exchange, message);
        }
    }

    /* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-californium-core.jar:org/eclipse/californium/core/network/stack/CongestionControlLayer$BucketTask.class */
    private class BucketTask implements Runnable {
        final AtomicInteger count = new AtomicInteger();
        final RemoteEndpoint endpoint;

        public BucketTask(RemoteEndpoint queue) {
            this.endpoint = queue;
        }

        @Override // java.lang.Runnable
        public void run() {
            final PostponedExchange exchange;
            int size = 0;
            synchronized (this.endpoint) {
                exchange = this.endpoint.getNotifyQueue().peek();
                if (exchange == null) {
                    this.endpoint.stopProcessingNotifies();
                } else {
                    this.count.incrementAndGet();
                    size = this.endpoint.getNotifyQueue().size();
                }
            }
            if (exchange != null) {
                final long rto = this.endpoint.getRTO();
                ReliabilityLayer.LOGGER.trace("{}send notify from queue, left {}, next {} ms", new Object[]{CongestionControlLayer.this.tag, Integer.valueOf(size), Long.valueOf(rto)});
                exchange.exchange.execute(new Runnable() { // from class: org.eclipse.californium.core.network.stack.CongestionControlLayer.BucketTask.1
                    @Override // java.lang.Runnable
                    public void run() {
                        long time = 0;
                        try {
                            synchronized (BucketTask.this.endpoint) {
                                if (BucketTask.this.endpoint.getNotifyQueue().peek() != exchange) {
                                    if (0 > 0) {
                                        CongestionControlLayer.this.executor.schedule(BucketTask.this, 0L, TimeUnit.MILLISECONDS);
                                        return;
                                    } else {
                                        CongestionControlLayer.this.executor.execute(BucketTask.this);
                                        return;
                                    }
                                }
                                BucketTask.this.endpoint.getNotifyQueue().remove();
                                ObserveRelation relation = exchange.exchange.getRelation();
                                if (relation != null && !relation.isCanceled()) {
                                    Response response = exchange.exchange.getCurrentResponse();
                                    if (exchange.message != response) {
                                        if (response.isNotification()) {
                                            ReliabilityLayer.LOGGER.warn("{} notify changed!", CongestionControlLayer.this.tag);
                                        } else {
                                            ReliabilityLayer.LOGGER.warn("{} notification finished!", CongestionControlLayer.this.tag);
                                        }
                                        if (0 > 0) {
                                            CongestionControlLayer.this.executor.schedule(BucketTask.this, 0L, TimeUnit.MILLISECONDS);
                                            return;
                                        } else {
                                            CongestionControlLayer.this.executor.execute(BucketTask.this);
                                            return;
                                        }
                                    }
                                    if (!exchange.exchange.isComplete() && !response.isCanceled()) {
                                        CongestionControlLayer.super.sendResponse(exchange.exchange, response);
                                        time = rto;
                                    }
                                }
                                if (time > 0) {
                                    CongestionControlLayer.this.executor.schedule(BucketTask.this, time, TimeUnit.MILLISECONDS);
                                } else {
                                    CongestionControlLayer.this.executor.execute(BucketTask.this);
                                }
                            }
                        } catch (Throwable th) {
                            if (0 > 0) {
                                CongestionControlLayer.this.executor.schedule(BucketTask.this, 0L, TimeUnit.MILLISECONDS);
                            } else {
                                CongestionControlLayer.this.executor.execute(BucketTask.this);
                            }
                            throw th;
                        }
                    }
                });
            } else {
                int jobs = this.count.getAndSet(0);
                ReliabilityLayer.LOGGER.debug("{}queue for outgoing notify stopped after {} jobs!", CongestionControlLayer.this.tag, Integer.valueOf(jobs));
            }
        }
    }

    /* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-californium-core.jar:org/eclipse/californium/core/network/stack/CongestionControlLayer$TimeoutTask.class */
    private class TimeoutTask extends MessageObserverAdapter {
        final RemoteEndpoint endpoint;
        final Exchange exchange;

        public TimeoutTask(RemoteEndpoint endpoint, Exchange exchange) {
            this.endpoint = endpoint;
            this.exchange = exchange;
        }

        @Override // org.eclipse.californium.core.coap.MessageObserverAdapter, org.eclipse.californium.core.coap.MessageObserver
        public void onTimeout() {
            CongestionControlLayer.this.nextQueuedExchange(this.endpoint, this.exchange);
        }
    }

    /* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-californium-core.jar:org/eclipse/californium/core/network/stack/CongestionControlLayer$PostponedExchange.class */
    public static class PostponedExchange {
        private final Exchange exchange;
        private final Message message;

        PostponedExchange(Exchange exchange, Message message) {
            this.exchange = exchange;
            this.message = message;
        }

        public int hashCode() {
            return this.exchange.hashCode();
        }

        public boolean equals(Object obj) {
            if (obj instanceof PostponedExchange) {
                return this.exchange.equals(((PostponedExchange) obj).exchange);
            }
            return false;
        }
    }

    public static ReliabilityLayer newImplementation(String tag, Configuration config) {
        ReliabilityLayer layer = null;
        CoapConfig.CongestionControlMode mode = (CoapConfig.CongestionControlMode) config.get(CoapConfig.CONGESTION_CONTROL_ALGORITHM);
        switch (mode) {
            case COCOA:
                layer = new Cocoa(tag, config, false);
                break;
            case COCOA_STRONG:
                layer = new Cocoa(tag, config, true);
                break;
            case BASIC_RTO:
                layer = new BasicRto(tag, config);
                break;
            case LINUX_RTO:
                layer = new LinuxRto(tag, config);
                break;
            case PEAKHOPPER_RTO:
                layer = new PeakhopperRto(tag, config);
                break;
            case NULL:
                layer = new ReliabilityLayer(config);
                break;
        }
        if (layer != null) {
            if (mode != CoapConfig.CongestionControlMode.NULL) {
                LOGGER.info("Enabling congestion control: {}", layer.getClass().getSimpleName());
            }
            return layer;
        }
        throw new IllegalArgumentException("Unsupported " + CoapConfig.CONGESTION_CONTROL_ALGORITHM.getKey());
    }
}
