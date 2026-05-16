package org.eclipse.californium.core.network.stack;

import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.californium.core.network.Exchange;
import org.eclipse.californium.core.observe.ObserveRelation;
import org.eclipse.californium.elements.config.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-californium-core.jar:org/eclipse/californium/core/network/stack/TcpObserveLayer.class */
public class TcpObserveLayer extends AbstractLayer {
    private static final Logger LOGGER = LoggerFactory.getLogger(TcpObserveLayer.class);
    private static final Integer CANCEL = 1;

    public TcpObserveLayer(Configuration config) {
    }

    @Override // org.eclipse.californium.core.network.stack.AbstractLayer, org.eclipse.californium.core.network.stack.Layer
    public void sendRequest(Exchange exchange, Request request) {
        if (CANCEL.equals(request.getOptions().getObserve())) {
        }
        lower().sendRequest(exchange, request);
    }

    @Override // org.eclipse.californium.core.network.stack.AbstractLayer, org.eclipse.californium.core.network.stack.Layer
    public void sendResponse(Exchange exchange, Response response) {
        ObserveRelation relation = exchange.getRelation();
        if (relation != null && relation.isEstablished()) {
            if (response.isSuccess() ^ response.isNotification()) {
                if (response.isNotification()) {
                    LOGGER.warn("Error notification, remove observe-option {}", response);
                    response.getOptions().removeObserve();
                } else {
                    LOGGER.warn("No-notification response with observe-relation {}, drop.", response);
                    response.setSendError(new IllegalArgumentException("Notification must have observe-option!"));
                    return;
                }
            }
            relation.send(response);
        }
        lower().sendResponse(exchange, response);
    }

    @Override // org.eclipse.californium.core.network.stack.AbstractLayer, org.eclipse.californium.core.network.stack.Layer
    public void receiveResponse(Exchange exchange, Response response) {
        if (response.getOptions().hasObserve() && exchange.getRequest().isCanceled()) {
            LOGGER.debug("ignoring notification for canceled TCP Exchange");
        } else {
            upper().receiveResponse(exchange, response);
        }
    }
}
