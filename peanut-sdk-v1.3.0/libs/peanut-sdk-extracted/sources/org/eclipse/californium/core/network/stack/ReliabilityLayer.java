package org.eclipse.californium.core.network.stack;

import java.util.Random;
import java.util.concurrent.ScheduledFuture;
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
import org.eclipse.californium.elements.EndpointContext;
import org.eclipse.californium.elements.EndpointContextUtil;
import org.eclipse.californium.elements.config.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-californium-core.jar:org/eclipse/californium/core/network/stack/ReliabilityLayer.class */
public class ReliabilityLayer extends AbstractLayer {
    protected static final Logger LOGGER = LoggerFactory.getLogger(ReliabilityLayer.class);
    protected final ReliabilityLayerParameters defaultReliabilityLayerParameters;
    private final Random rand = new Random();
    private final AtomicInteger counter = new AtomicInteger();
    private final int maxLeisureMillis;

    public ReliabilityLayer(Configuration config) {
        this.defaultReliabilityLayerParameters = ReliabilityLayerParameters.builder().applyConfig(config).build();
        this.maxLeisureMillis = config.getTimeAsInt(CoapConfig.LEISURE, TimeUnit.MILLISECONDS);
        LOGGER.trace("Max. leisure for multicast server={}ms", Integer.valueOf(this.maxLeisureMillis));
        LOGGER.trace("ReliabilityLayer uses ACK_TIMEOUT={}ms, MAX_ACK_TIMEOUT={}ms, ACK_RANDOM_FACTOR={}, and ACK_TIMEOUT_SCALE={} as default", new Object[]{Integer.valueOf(this.defaultReliabilityLayerParameters.getAckTimeout()), Integer.valueOf(this.defaultReliabilityLayerParameters.getMaxAckTimeout()), Float.valueOf(this.defaultReliabilityLayerParameters.getAckRandomFactor()), Float.valueOf(this.defaultReliabilityLayerParameters.getAckTimeoutScale())});
    }

    @Override // org.eclipse.californium.core.network.stack.AbstractLayer, org.eclipse.californium.core.network.stack.Layer
    public void sendRequest(Exchange exchange, Request request) {
        LOGGER.debug("{} send request", exchange);
        prepareRequest(exchange, request);
        lower().sendRequest(exchange, request);
    }

    protected void prepareRequest(Exchange exchange, Request request) {
        if (request.getType() == null) {
            request.setType(CoAP.Type.CON);
        }
        if (request.getType() == CoAP.Type.CON) {
            LOGGER.debug("{} prepare retransmission for {}", exchange, request);
            prepareRetransmission(exchange, new RetransmissionTask(exchange, request) { // from class: org.eclipse.californium.core.network.stack.ReliabilityLayer.1
                @Override // org.eclipse.californium.core.network.stack.ReliabilityLayer.RetransmissionTask
                public void retransmit() {
                    Request request2 = (Request) this.message;
                    if (request2.getEffectiveDestinationContext() != request2.getDestinationContext()) {
                        this.exchange.resetEndpointContext();
                    }
                    ReliabilityLayer.LOGGER.debug("{} send request, failed transmissions: {}", this.exchange, Integer.valueOf(this.exchange.getFailedTransmissionCount()));
                    ReliabilityLayer.this.updateRetransmissionTimeout(this.exchange, getReliabilityLayerParameters());
                    ReliabilityLayer.this.lower().sendRequest(this.exchange, request2);
                }
            });
        }
    }

    @Override // org.eclipse.californium.core.network.stack.AbstractLayer, org.eclipse.californium.core.network.stack.Layer
    public void sendResponse(Exchange exchange, Response response) {
        int leisure;
        LOGGER.debug("{} send response {}", exchange, response);
        prepareResponse(exchange, response);
        if (exchange.getCurrentRequest().isMulticast() && response.getType() == CoAP.Type.NON) {
            synchronized (this.rand) {
                leisure = this.rand.nextInt(this.maxLeisureMillis);
            }
            DelayedResponseTask task = new DelayedResponseTask(exchange, response);
            ScheduledFuture<?> f = this.executor.schedule(task, leisure, TimeUnit.MILLISECONDS);
            exchange.setRetransmissionHandle(f);
            return;
        }
        lower().sendResponse(exchange, response);
    }

    protected void prepareResponse(Exchange exchange, Response response) {
        Request currentRequest = exchange.getCurrentRequest();
        CoAP.Type respType = response.getType();
        if (respType == null) {
            CoAP.Type reqType = currentRequest.getType();
            if (currentRequest.acknowledge()) {
                response.setType(CoAP.Type.ACK);
            } else {
                response.setType(reqType);
            }
            respType = response.getType();
            LOGGER.trace("{} set response type to {} (request was {})", new Object[]{exchange, respType, reqType});
        } else if (respType == CoAP.Type.RST) {
            currentRequest.setRejected(true);
        } else if (respType == CoAP.Type.ACK) {
            currentRequest.acknowledge();
        } else {
            currentRequest.setAcknowledged(true);
        }
        if (respType == CoAP.Type.ACK || respType == CoAP.Type.RST) {
            response.setMID(currentRequest.getMID());
        }
        if (respType == CoAP.Type.CON) {
            LOGGER.debug("{} prepare retransmission for {}", exchange, response);
            prepareRetransmission(exchange, new RetransmissionTask(exchange, response) { // from class: org.eclipse.californium.core.network.stack.ReliabilityLayer.2
                @Override // org.eclipse.californium.core.network.stack.ReliabilityLayer.RetransmissionTask
                public void retransmit() {
                    Response response2 = (Response) this.message;
                    ReliabilityLayer.LOGGER.debug("{} send response {}, failed transmissions: {}", new Object[]{this.exchange, response2, Integer.valueOf(this.exchange.getFailedTransmissionCount())});
                    ReliabilityLayer.this.updateRetransmissionTimeout(this.exchange, getReliabilityLayerParameters());
                    ReliabilityLayer.this.lower().sendResponse(this.exchange, response2);
                }
            });
        }
    }

    private void prepareRetransmission(Exchange exchange, RetransmissionTask task) {
        if (this.executor.isShutdown()) {
            LOGGER.info("Endpoint is being destroyed: skipping retransmission");
            return;
        }
        exchange.setRetransmissionHandle(null);
        updateRetransmissionTimeout(exchange, task.getReliabilityLayerParameters());
        task.message.addMessageObserver(task);
    }

    @Override // org.eclipse.californium.core.network.stack.AbstractLayer, org.eclipse.californium.core.network.stack.Layer
    public void receiveRequest(Exchange exchange, Request request) {
        if (request.isDuplicate()) {
            long send = exchange.getSendNanoTimestamp();
            if (send == 0 || send - request.getNanoTimestamp() > 0) {
                int count = this.counter.incrementAndGet();
                LOGGER.debug("{}: {} duplicate request {}, server sent response delayed, ignore request", new Object[]{Integer.valueOf(count), exchange, request});
                return;
            }
            exchange.retransmitResponse();
            Request previousRequest = exchange.getCurrentRequest();
            Response previousResponse = exchange.getCurrentResponse();
            if (previousResponse == null) {
                if (previousRequest.isAcknowledged()) {
                    LOGGER.debug("{} duplicate request was acknowledged but no response computed yet. Retransmit ACK", exchange);
                    EmptyMessage ack = EmptyMessage.newACK(request);
                    sendEmptyMessage(exchange, ack);
                    return;
                } else {
                    if (previousRequest.isRejected()) {
                        LOGGER.debug("{} duplicate request was rejected. Reject again", exchange);
                        EmptyMessage rst = EmptyMessage.newRST(request);
                        sendEmptyMessage(exchange, rst);
                        return;
                    }
                    LOGGER.debug("{} server has not yet decided what to do with the request. We ignore the duplicate.", exchange);
                    return;
                }
            }
            CoAP.Type type = previousResponse.getType();
            if (type == CoAP.Type.NON || type == CoAP.Type.CON) {
                if (request.acknowledge()) {
                    EmptyMessage ack2 = EmptyMessage.newACK(request);
                    sendEmptyMessage(exchange, ack2);
                }
                if (type == CoAP.Type.CON) {
                    if (previousResponse.isAcknowledged()) {
                        LOGGER.debug("{} request duplicate: ignore, response already acknowledged!", exchange);
                        return;
                    }
                    int failedCount = exchange.incrementFailedTransmissionCount();
                    LOGGER.debug("{} request duplicate: retransmit response, failed: {}, response: {}", new Object[]{exchange, Integer.valueOf(failedCount), previousResponse});
                    previousResponse.retransmitting();
                    sendResponse(exchange, previousResponse);
                    return;
                }
                if (previousResponse.isNotification()) {
                    exchange.incrementFailedTransmissionCount();
                }
            }
            LOGGER.debug("{} respond with the current response to the duplicate request", exchange);
            lower().sendResponse(exchange, previousResponse);
            return;
        }
        exchange.setCurrentRequest(request);
        upper().receiveRequest(exchange, request);
    }

    @Override // org.eclipse.californium.core.network.stack.AbstractLayer, org.eclipse.californium.core.network.stack.Layer
    public void receiveResponse(Exchange exchange, Response response) {
        if (processResponse(exchange, response)) {
            upper().receiveResponse(exchange, response);
        }
    }

    protected boolean processResponse(Exchange exchange, Response response) {
        EmptyMessage empty;
        exchange.setRetransmissionHandle(null);
        if (response.getType() == CoAP.Type.CON) {
            boolean ack = true;
            if (response.isDuplicate()) {
                long send = exchange.getSendNanoTimestamp();
                if (send == 0 || send - response.getNanoTimestamp() > 0) {
                    int count = this.counter.incrementAndGet();
                    LOGGER.debug("{}: {} duplicate response {}, server sent ACK delayed, ignore response", new Object[]{Integer.valueOf(count), exchange, response});
                    return false;
                }
                if (response.isRejected()) {
                    ack = false;
                    LOGGER.debug("{} reject duplicate CON response, request canceled.", exchange);
                } else {
                    LOGGER.debug("{} acknowledging duplicate CON response", exchange);
                }
            } else if (exchange.getRequest().isCanceled()) {
                ack = false;
                LOGGER.debug("{} reject CON response, request canceled.", exchange);
            } else {
                LOGGER.debug("{} acknowledging CON response", exchange);
            }
            if (ack) {
                empty = EmptyMessage.newACK(response);
                response.setAcknowledged(true);
            } else {
                empty = EmptyMessage.newRST(response);
                response.setRejected(true);
            }
            sendEmptyMessage(exchange, empty);
        }
        if (response.isDuplicate()) {
            if (response.getType() != CoAP.Type.CON) {
                LOGGER.debug("{} ignoring duplicate response", exchange);
                return false;
            }
            return false;
        }
        exchange.getCurrentRequest().setAcknowledged(true);
        exchange.setCurrentResponse(response);
        return true;
    }

    @Override // org.eclipse.californium.core.network.stack.AbstractLayer, org.eclipse.californium.core.network.stack.Layer
    public void receiveEmptyMessage(Exchange exchange, EmptyMessage message) {
        if (processEmptyMessage(exchange, message)) {
            upper().receiveEmptyMessage(exchange, message);
        }
    }

    protected boolean processEmptyMessage(Exchange exchange, EmptyMessage message) {
        String type;
        Message currentMessage;
        exchange.setRetransmissionHandle(null);
        if (exchange.isOfLocalOrigin()) {
            type = "request";
            currentMessage = exchange.getCurrentRequest();
        } else {
            type = "response";
            currentMessage = exchange.getCurrentResponse();
        }
        int observer = currentMessage.getMessageObservers().size();
        if (message.getType() == CoAP.Type.ACK) {
            LOGGER.debug("{} acknowledge {} for {} {} ({} msg observer)", new Object[]{exchange, message, type, currentMessage, Integer.valueOf(observer)});
            currentMessage.acknowledge();
            return true;
        }
        if (message.getType() == CoAP.Type.RST) {
            LOGGER.debug("{} reject {} for {} {} ({} msg observer)", new Object[]{exchange, message, type, currentMessage, Integer.valueOf(observer)});
            currentMessage.setRejected(true);
            return true;
        }
        LOGGER.warn("{} received empty message that is neither ACK nor RST: {}", exchange, message);
        return false;
    }

    protected void updateRetransmissionTimeout(Exchange exchange, ReliabilityLayerParameters reliabilityLayerParameters) {
        int timeout;
        if (exchange.getFailedTransmissionCount() == 0) {
            exchange.setTimeoutScale(reliabilityLayerParameters.getAckTimeoutScale());
            timeout = getRandomTimeout(reliabilityLayerParameters.getAckTimeout(), reliabilityLayerParameters.getAckRandomFactor());
        } else {
            timeout = (int) (exchange.getTimeoutScale() * exchange.getCurrentTimeout());
        }
        exchange.setCurrentTimeout(Math.min(reliabilityLayerParameters.getMaxAckTimeout(), timeout));
    }

    protected int getRandomTimeout(int ackTimeout, float randomFactor) {
        int iNextInt;
        if (randomFactor <= 1.0d) {
            return ackTimeout;
        }
        int delta = (((int) (ackTimeout * randomFactor)) - ackTimeout) + 1;
        synchronized (this.rand) {
            iNextInt = ackTimeout + this.rand.nextInt(delta);
        }
        return iNextInt;
    }

    /* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-californium-core.jar:org/eclipse/californium/core/network/stack/ReliabilityLayer$DelayedResponseTask.class */
    private class DelayedResponseTask implements Runnable {
        protected final Exchange exchange;
        protected final Response response;

        private DelayedResponseTask(Exchange exchange, Response response) {
            this.exchange = exchange;
            this.response = response;
        }

        @Override // java.lang.Runnable
        public void run() {
            this.exchange.execute(new Runnable() { // from class: org.eclipse.californium.core.network.stack.ReliabilityLayer.DelayedResponseTask.1
                @Override // java.lang.Runnable
                public void run() {
                    ReliabilityLayer.this.lower().sendResponse(DelayedResponseTask.this.exchange, DelayedResponseTask.this.response);
                }
            });
        }
    }

    /* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-californium-core.jar:org/eclipse/californium/core/network/stack/ReliabilityLayer$RetransmissionTask.class */
    protected abstract class RetransmissionTask extends MessageObserverAdapter implements Runnable {
        protected final Exchange exchange;
        protected final Message message;

        public abstract void retransmit();

        private RetransmissionTask(Exchange exchange, Message message) {
            super(true);
            this.exchange = exchange;
            this.message = message;
        }

        public ReliabilityLayerParameters getReliabilityLayerParameters() {
            ReliabilityLayerParameters parameters = this.message.getReliabilityLayerParameters();
            if (parameters == null) {
                parameters = ReliabilityLayer.this.defaultReliabilityLayerParameters;
            }
            return parameters;
        }

        private boolean isInTransit() {
            Message current;
            if (this.message.isAcknowledged() || this.exchange.isComplete()) {
                return false;
            }
            if (this.exchange.isOfLocalOrigin()) {
                current = this.exchange.getCurrentRequest();
            } else {
                current = this.exchange.getCurrentResponse();
            }
            return this.message == current;
        }

        @Override // org.eclipse.californium.core.coap.MessageObserverAdapter, org.eclipse.californium.core.coap.MessageObserver
        public void onSent(boolean retransmission) {
            if (isInTransit()) {
                this.exchange.execute(new Runnable() { // from class: org.eclipse.californium.core.network.stack.ReliabilityLayer.RetransmissionTask.1
                    @Override // java.lang.Runnable
                    public void run() {
                        RetransmissionTask.this.startTimer();
                    }
                });
            }
        }

        public void startTimer() {
            if (isInTransit()) {
                int timeout = this.exchange.getCurrentTimeout();
                ScheduledFuture<?> f = ReliabilityLayer.this.executor.schedule(this, timeout, TimeUnit.MILLISECONDS);
                this.exchange.setRetransmissionHandle(f);
            }
        }

        @Override // java.lang.Runnable
        public void run() {
            this.exchange.execute(new Runnable() { // from class: org.eclipse.californium.core.network.stack.ReliabilityLayer.RetransmissionTask.2
                @Override // java.lang.Runnable
                public void run() {
                    RetransmissionTask.this.retry();
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void retry() {
            Message current;
            try {
                this.exchange.setRetransmissionHandle(null);
                if (this.exchange.isComplete()) {
                    ReliabilityLayer.LOGGER.debug("Timeout: for {}, {}", this.exchange, this.message);
                    return;
                }
                if (this.exchange.isOfLocalOrigin()) {
                    current = this.exchange.getCurrentRequest();
                } else {
                    current = this.exchange.getCurrentResponse();
                }
                if (this.message != current) {
                    ReliabilityLayer.LOGGER.debug("Timeout: for {}, message has changed!", this.exchange);
                    return;
                }
                if (this.message.isAcknowledged()) {
                    ReliabilityLayer.LOGGER.trace("Timeout: for {} message already acknowledged, cancel retransmission of {}", this.exchange, this.message);
                    return;
                }
                if (this.message.isRejected()) {
                    ReliabilityLayer.LOGGER.trace("Timeout: for {} message already rejected, cancel retransmission of {}", this.exchange, this.message);
                    return;
                }
                if (this.message.isCanceled()) {
                    ReliabilityLayer.LOGGER.trace("Timeout: for {}, {} is canceled, do not retransmit", this.exchange, this.message);
                    return;
                }
                int failedCount = this.exchange.incrementFailedTransmissionCount();
                if (failedCount == 1) {
                    EndpointContext context = EndpointContextUtil.getFollowUpEndpointContext(this.message.getDestinationContext(), this.exchange.getEndpointContext());
                    this.message.setEffectiveDestinationContext(context);
                }
                ReliabilityLayer.LOGGER.debug("Timeout: for {} retry {} of {}", new Object[]{this.exchange, Integer.valueOf(failedCount), this.message});
                int max = getReliabilityLayerParameters().getMaxRetransmit();
                if (failedCount <= max) {
                    ReliabilityLayer.LOGGER.debug("Timeout: for {} retransmit message, failed-count: {}, message: {}", new Object[]{this.exchange, Integer.valueOf(failedCount), this.message});
                    this.message.retransmitting();
                    if (this.message.isCanceled()) {
                        ReliabilityLayer.LOGGER.trace("Timeout: for {}, {} got canceled, do not retransmit", this.exchange, this.message);
                    } else {
                        if (this.exchange.isComplete()) {
                            ReliabilityLayer.LOGGER.debug("Timeout: for {}, {} got completed, do not retransmit", this.exchange, this.message);
                            return;
                        }
                        retransmit();
                    }
                } else {
                    ReliabilityLayer.LOGGER.debug("Timeout: for {} retransmission limit {} reached, exchange failed with timeout {} ms, message: {}", new Object[]{this.exchange, Integer.valueOf(max), Integer.valueOf(this.exchange.getCurrentTimeout()), this.message});
                    this.exchange.setTimedOut(this.message);
                }
            } catch (Exception e) {
                ReliabilityLayer.LOGGER.error("Exception for {} in MessageObserver: {}", new Object[]{this.exchange, e.getMessage(), e});
            }
        }
    }
}
