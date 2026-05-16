package org.eclipse.californium.core.network.stack;

import java.util.concurrent.TimeUnit;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.californium.core.config.CoapConfig;
import org.eclipse.californium.core.network.Exchange;
import org.eclipse.californium.elements.config.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-californium-core.jar:org/eclipse/californium/core/network/stack/ExchangeCleanupLayer.class */
public class ExchangeCleanupLayer extends AbstractLayer {
    static final Logger LOGGER = LoggerFactory.getLogger(ExchangeCleanupLayer.class);
    private final int lifetime;

    public ExchangeCleanupLayer(Configuration config) {
        this.lifetime = config.getTimeAsInt(CoapConfig.NON_LIFETIME, TimeUnit.MILLISECONDS) + config.getTimeAsInt(CoapConfig.MAX_LATENCY, TimeUnit.MILLISECONDS) + config.getTimeAsInt(CoapConfig.MAX_SERVER_RESPONSE_DELAY, TimeUnit.MILLISECONDS);
    }

    @Override // org.eclipse.californium.core.network.stack.AbstractLayer, org.eclipse.californium.core.network.stack.Layer
    public void sendRequest(Exchange exchange, Request request) {
        if (request.isMulticast()) {
            request.addMessageObserver(new MulticastCleanupMessageObserver(exchange, this.executor, this.lifetime));
        } else if (request.getOptions().hasNoResponse()) {
            request.addMessageObserver(new NoResponseCleanupMessageObserver(exchange, this.executor, this.lifetime));
        } else {
            request.addMessageObserver(new CleanupMessageObserver(exchange));
        }
        super.sendRequest(exchange, request);
    }

    @Override // org.eclipse.californium.core.network.stack.AbstractLayer, org.eclipse.californium.core.network.stack.Layer
    public void sendResponse(Exchange exchange, Response response) {
        CoAP.Type type;
        if (!response.isNotification() && ((type = response.getType()) == null || type == CoAP.Type.CON)) {
            response.addMessageObserver(new CleanupMessageObserver(exchange));
        }
        super.sendResponse(exchange, response);
    }

    @Override // org.eclipse.californium.core.network.stack.AbstractLayer, org.eclipse.californium.core.network.stack.Layer
    public void receiveResponse(Exchange exchange, Response response) {
        if (!exchange.getRequest().isMulticast()) {
            exchange.setComplete();
            exchange.getRequest().onTransferComplete();
        }
        super.receiveResponse(exchange, response);
    }
}
