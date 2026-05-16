package org.eclipse.californium.core.network;

import org.eclipse.californium.core.coap.EmptyMessage;
import org.eclipse.californium.core.coap.Message;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.coap.Response;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-californium-core.jar:org/eclipse/californium/core/network/EndpointReceiver.class */
public interface EndpointReceiver {
    void receiveRequest(Exchange exchange, Request request);

    void receiveResponse(Exchange exchange, Response response);

    void receiveEmptyMessage(Exchange exchange, EmptyMessage emptyMessage);

    void reject(Message message);
}
