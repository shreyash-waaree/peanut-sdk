package org.eclipse.californium.core.network.stack;

import java.util.concurrent.ScheduledExecutorService;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.EmptyMessage;
import org.eclipse.californium.core.coap.Message;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.californium.core.network.Exchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-californium-core.jar:org/eclipse/californium/core/network/stack/AbstractLayer.class */
public abstract class AbstractLayer implements Layer {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractLayer.class);
    private Layer upperLayer = LogOnlyLayer.getInstance();
    private Layer lowerLayer = LogOnlyLayer.getInstance();
    protected ScheduledExecutorService executor;
    protected ScheduledExecutorService secondaryExecutor;

    @Override // org.eclipse.californium.core.network.stack.Layer
    public void sendRequest(Exchange exchange, Request request) {
        this.lowerLayer.sendRequest(exchange, request);
    }

    @Override // org.eclipse.californium.core.network.stack.Layer
    public void sendResponse(Exchange exchange, Response response) {
        this.lowerLayer.sendResponse(exchange, response);
    }

    @Override // org.eclipse.californium.core.network.stack.Layer
    public void sendEmptyMessage(Exchange exchange, EmptyMessage message) {
        this.lowerLayer.sendEmptyMessage(exchange, message);
    }

    @Override // org.eclipse.californium.core.network.stack.Layer
    public void receiveRequest(Exchange exchange, Request request) {
        this.upperLayer.receiveRequest(exchange, request);
    }

    @Override // org.eclipse.californium.core.network.stack.Layer
    public void receiveResponse(Exchange exchange, Response response) {
        this.upperLayer.receiveResponse(exchange, response);
    }

    @Override // org.eclipse.californium.core.network.stack.Layer
    public void receiveEmptyMessage(Exchange exchange, EmptyMessage message) {
        this.upperLayer.receiveEmptyMessage(exchange, message);
    }

    @Override // org.eclipse.californium.core.network.stack.Layer
    public final void setLowerLayer(Layer layer) {
        if (this.lowerLayer != layer) {
            if (this.lowerLayer != null) {
                this.lowerLayer.setUpperLayer(null);
            }
            this.lowerLayer = layer;
            this.lowerLayer.setUpperLayer(this);
        }
    }

    final Layer lower() {
        return this.lowerLayer;
    }

    @Override // org.eclipse.californium.core.network.stack.Layer
    public final void setUpperLayer(Layer layer) {
        if (this.upperLayer != layer) {
            if (this.upperLayer != null) {
                this.upperLayer.setLowerLayer(null);
            }
            this.upperLayer = layer;
            this.upperLayer.setLowerLayer(this);
        }
    }

    final Layer upper() {
        return this.upperLayer;
    }

    @Override // org.eclipse.californium.core.network.stack.Layer
    public final void setExecutors(ScheduledExecutorService mainExecutor, ScheduledExecutorService secondaryExecutor) {
        this.executor = mainExecutor;
        this.secondaryExecutor = secondaryExecutor;
    }

    public final void reject(Exchange exchange, Message message) {
        if (message.getType() == CoAP.Type.ACK || message.getType() == CoAP.Type.RST) {
            throw new IllegalArgumentException("Can only reject CON/NON messages");
        }
        lower().sendEmptyMessage(exchange, EmptyMessage.newRST(message));
    }

    @Override // org.eclipse.californium.core.network.stack.Layer
    public void start() {
    }

    @Override // org.eclipse.californium.core.network.stack.Layer
    public void destroy() {
    }

    /* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-californium-core.jar:org/eclipse/californium/core/network/stack/AbstractLayer$LogOnlyLayer.class */
    public static final class LogOnlyLayer implements Layer {
        private static final LogOnlyLayer INSTANCE = new LogOnlyLayer();

        public static LogOnlyLayer getInstance() {
            return INSTANCE;
        }

        @Override // org.eclipse.californium.core.network.stack.Layer
        public void sendRequest(Exchange exchange, Request request) {
            AbstractLayer.LOGGER.error("No lower layer set for sending request [{}]", request);
        }

        @Override // org.eclipse.californium.core.network.stack.Layer
        public void sendResponse(Exchange exchange, Response response) {
            AbstractLayer.LOGGER.error("No lower layer set for sending response [{}]", response);
        }

        @Override // org.eclipse.californium.core.network.stack.Layer
        public void sendEmptyMessage(Exchange exchange, EmptyMessage emptyMessage) {
            AbstractLayer.LOGGER.error("No lower layer set for sending empty message [{}]", emptyMessage);
        }

        @Override // org.eclipse.californium.core.network.stack.Layer
        public void receiveRequest(Exchange exchange, Request request) {
            AbstractLayer.LOGGER.error("No upper layer set for receiving request [{}]", request);
        }

        @Override // org.eclipse.californium.core.network.stack.Layer
        public void receiveResponse(Exchange exchange, Response response) {
            AbstractLayer.LOGGER.error("No lower layer set for receiving response [{}]", response);
        }

        @Override // org.eclipse.californium.core.network.stack.Layer
        public void receiveEmptyMessage(Exchange exchange, EmptyMessage emptyMessage) {
            AbstractLayer.LOGGER.error("No lower layer set for receiving empty message [{}]", emptyMessage);
        }

        @Override // org.eclipse.californium.core.network.stack.Layer
        public void setLowerLayer(Layer layer) {
        }

        @Override // org.eclipse.californium.core.network.stack.Layer
        public void setUpperLayer(Layer layer) {
        }

        @Override // org.eclipse.californium.core.network.stack.Layer
        public void setExecutors(ScheduledExecutorService mainExecutor, ScheduledExecutorService secondaryExecutor) {
        }

        @Override // org.eclipse.californium.core.network.stack.Layer
        public void start() {
        }

        @Override // org.eclipse.californium.core.network.stack.Layer
        public void destroy() {
        }
    }
}
