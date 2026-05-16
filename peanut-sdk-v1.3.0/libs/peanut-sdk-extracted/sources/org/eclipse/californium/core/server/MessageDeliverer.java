package org.eclipse.californium.core.server;

import org.eclipse.californium.core.coap.Response;
import org.eclipse.californium.core.network.Exchange;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-californium-core.jar:org/eclipse/californium/core/server/MessageDeliverer.class */
public interface MessageDeliverer {
    void deliverRequest(Exchange exchange);

    void deliverResponse(Exchange exchange, Response response);
}
