package org.eclipse.californium.core.network.stack;

import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import org.eclipse.californium.core.coap.BlockOption;
import org.eclipse.californium.core.coap.EmptyMessage;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.californium.core.network.Exchange;
import org.eclipse.californium.core.network.ExchangeCompleteException;
import org.eclipse.californium.core.network.Outbox;
import org.eclipse.californium.core.network.stack.Layer;
import org.eclipse.californium.core.observe.ObservationStoreException;
import org.eclipse.californium.core.observe.ObserveRelation;
import org.eclipse.californium.core.server.MessageDeliverer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-californium-core.jar:org/eclipse/californium/core/network/stack/BaseCoapStack.class */
public abstract class BaseCoapStack implements CoapStack {
    private static final Logger LOGGER = LoggerFactory.getLogger(BaseCoapStack.class);
    private List<Layer> layers;
    private final Outbox outbox;
    private final StackTopAdapter top = new StackTopAdapter();
    private final StackBottomAdapter bottom = new StackBottomAdapter();
    private MessageDeliverer deliverer;

    protected BaseCoapStack(Outbox outbox) {
        this.outbox = outbox;
    }

    protected final void setLayers(Layer[] specificLayers) {
        Layer.TopDownBuilder builder = new Layer.TopDownBuilder().add(this.top);
        for (Layer layer : specificLayers) {
            builder.add(layer);
        }
        builder.add(this.bottom);
        this.layers = builder.create();
    }

    @Override // org.eclipse.californium.core.network.stack.CoapStack
    public void sendRequest(Exchange exchange, Request request) {
        try {
            this.top.sendRequest(exchange, request);
        } catch (ObservationStoreException ex) {
            LOGGER.debug("error send request {} - {}", request, ex.getMessage());
            request.setSendError(ex);
        } catch (RuntimeException ex2) {
            LOGGER.warn("error send request {}", request, ex2);
            request.setSendError(ex2);
        }
    }

    @Override // org.eclipse.californium.core.network.stack.CoapStack
    public void sendResponse(Exchange exchange, Response response) {
        ObserveRelation relation = exchange.getRelation();
        boolean retransmit = relation != null && relation.isEstablished();
        if (retransmit) {
            try {
                exchange.retransmitResponse();
            } catch (ExchangeCompleteException ex) {
                LOGGER.warn("error send response {}", response, ex);
                response.setSendError(ex);
                return;
            } catch (RuntimeException ex2) {
                LOGGER.warn("error send response {}", response, ex2);
                if (!retransmit) {
                    exchange.sendReject();
                }
                response.setSendError(ex2);
                return;
            }
        }
        this.top.sendResponse(exchange, response);
    }

    @Override // org.eclipse.californium.core.network.stack.CoapStack
    public void sendEmptyMessage(Exchange exchange, EmptyMessage message) {
        try {
            this.top.sendEmptyMessage(exchange, message);
        } catch (RuntimeException ex) {
            LOGGER.warn("error send empty message {}", message, ex);
            message.setSendError(ex);
        }
    }

    @Override // org.eclipse.californium.core.network.stack.CoapStack
    public void receiveRequest(Exchange exchange, Request request) {
        this.bottom.receiveRequest(exchange, request);
    }

    @Override // org.eclipse.californium.core.network.stack.CoapStack
    public void receiveResponse(Exchange exchange, Response response) {
        this.bottom.receiveResponse(exchange, response);
    }

    @Override // org.eclipse.californium.core.network.stack.CoapStack
    public void receiveEmptyMessage(Exchange exchange, EmptyMessage message) {
        this.bottom.receiveEmptyMessage(exchange, message);
    }

    @Override // org.eclipse.californium.core.network.stack.CoapStack
    public final void setExecutors(ScheduledExecutorService mainExecutor, ScheduledExecutorService secondaryExecutor) {
        for (Layer layer : this.layers) {
            layer.setExecutors(mainExecutor, secondaryExecutor);
        }
    }

    @Override // org.eclipse.californium.core.network.stack.CoapStack
    public final void setDeliverer(MessageDeliverer deliverer) {
        this.deliverer = deliverer;
    }

    @Override // org.eclipse.californium.core.network.stack.CoapStack
    public final boolean hasDeliverer() {
        return this.deliverer != null;
    }

    @Override // org.eclipse.californium.core.network.stack.CoapStack
    public void start() {
        for (Layer layer : this.layers) {
            layer.start();
        }
    }

    @Override // org.eclipse.californium.core.network.stack.CoapStack
    public void destroy() {
        for (Layer layer : this.layers) {
            layer.destroy();
        }
    }

    /* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-californium-core.jar:org/eclipse/californium/core/network/stack/BaseCoapStack$StackTopAdapter.class */
    private class StackTopAdapter extends AbstractLayer {
        private StackTopAdapter() {
        }

        @Override // org.eclipse.californium.core.network.stack.AbstractLayer, org.eclipse.californium.core.network.stack.Layer
        public void sendRequest(Exchange exchange, Request request) {
            exchange.setRequest(request);
            lower().sendRequest(exchange, request);
        }

        @Override // org.eclipse.californium.core.network.stack.AbstractLayer, org.eclipse.californium.core.network.stack.Layer
        public void sendResponse(Exchange exchange, Response response) {
            exchange.setResponse(response);
            lower().sendResponse(exchange, response);
        }

        @Override // org.eclipse.californium.core.network.stack.AbstractLayer, org.eclipse.californium.core.network.stack.Layer
        public void receiveRequest(Exchange exchange, Request request) {
            if (exchange.getRequest() == null) {
                exchange.setRequest(request);
            }
            if (BaseCoapStack.this.hasDeliverer()) {
                BaseCoapStack.this.deliverer.deliverRequest(exchange);
            } else {
                BaseCoapStack.LOGGER.error("Top of CoAP stack has no deliverer to deliver request");
            }
        }

        @Override // org.eclipse.californium.core.network.stack.AbstractLayer, org.eclipse.californium.core.network.stack.Layer
        public void receiveResponse(Exchange exchange, Response response) {
            if (BaseCoapStack.this.hasDeliverer()) {
                BaseCoapStack.this.deliverer.deliverResponse(exchange, response);
            } else {
                BaseCoapStack.LOGGER.error("Top of CoAP stack has no deliverer to deliver response");
            }
        }

        @Override // org.eclipse.californium.core.network.stack.AbstractLayer, org.eclipse.californium.core.network.stack.Layer
        public void receiveEmptyMessage(Exchange exchange, EmptyMessage message) {
        }
    }

    /* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-californium-core.jar:org/eclipse/californium/core/network/stack/BaseCoapStack$StackBottomAdapter.class */
    private class StackBottomAdapter extends AbstractLayer {
        private StackBottomAdapter() {
        }

        @Override // org.eclipse.californium.core.network.stack.AbstractLayer, org.eclipse.californium.core.network.stack.Layer
        public void sendRequest(Exchange exchange, Request request) {
            BaseCoapStack.this.outbox.sendRequest(exchange, request);
        }

        @Override // org.eclipse.californium.core.network.stack.AbstractLayer, org.eclipse.californium.core.network.stack.Layer
        public void sendResponse(Exchange exchange, Response response) {
            BaseCoapStack.this.outbox.sendResponse(exchange, response);
            BlockOption block2 = response.getOptions().getBlock2();
            if (block2 == null || !block2.isM()) {
                response.onTransferComplete();
            }
        }

        @Override // org.eclipse.californium.core.network.stack.AbstractLayer, org.eclipse.californium.core.network.stack.Layer
        public void sendEmptyMessage(Exchange exchange, EmptyMessage message) {
            BaseCoapStack.this.outbox.sendEmptyMessage(exchange, message);
        }
    }
}
