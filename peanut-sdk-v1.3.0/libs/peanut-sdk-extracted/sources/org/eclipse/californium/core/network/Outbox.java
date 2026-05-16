package org.eclipse.californium.core.network;

import org.eclipse.californium.core.coap.EmptyMessage;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.coap.Response;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-californium-core.jar:org/eclipse/californium/core/network/Outbox.class */
public interface Outbox {
    void sendRequest(Exchange exchange, Request request);

    void sendResponse(Exchange exchange, Response response);

    void sendEmptyMessage(Exchange exchange, EmptyMessage emptyMessage);
}
