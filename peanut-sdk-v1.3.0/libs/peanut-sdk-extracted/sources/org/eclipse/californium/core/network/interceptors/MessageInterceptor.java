package org.eclipse.californium.core.network.interceptors;

import org.eclipse.californium.core.coap.EmptyMessage;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.coap.Response;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-californium-core.jar:org/eclipse/californium/core/network/interceptors/MessageInterceptor.class */
public interface MessageInterceptor {
    void sendRequest(Request request);

    void sendResponse(Response response);

    void sendEmptyMessage(EmptyMessage emptyMessage);

    void receiveRequest(Request request);

    void receiveResponse(Response response);

    void receiveEmptyMessage(EmptyMessage emptyMessage);
}
