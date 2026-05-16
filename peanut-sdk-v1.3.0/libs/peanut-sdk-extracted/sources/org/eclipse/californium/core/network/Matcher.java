package org.eclipse.californium.core.network;

import org.eclipse.californium.core.coap.EmptyMessage;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.californium.core.coap.Token;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-californium-core.jar:org/eclipse/californium/core/network/Matcher.class */
public interface Matcher {
    void start();

    void stop();

    void sendRequest(Exchange exchange);

    void sendResponse(Exchange exchange);

    void sendEmptyMessage(Exchange exchange, EmptyMessage emptyMessage);

    void receiveRequest(Request request, EndpointReceiver endpointReceiver);

    void receiveResponse(Response response, EndpointReceiver endpointReceiver);

    void receiveEmptyMessage(EmptyMessage emptyMessage, EndpointReceiver endpointReceiver);

    void clear();

    void cancelObserve(Token token);
}
