package org.eclipse.californium.core.network.interceptors;

import org.eclipse.californium.core.coap.EmptyMessage;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.coap.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-californium-core.jar:org/eclipse/californium/core/network/interceptors/MessageTracer.class */
public class MessageTracer implements MessageInterceptor {
    private static final Logger LOGGER = LoggerFactory.getLogger(MessageTracer.class);

    @Override // org.eclipse.californium.core.network.interceptors.MessageInterceptor
    public void sendRequest(Request request) {
        LOGGER.info("{} <== req {}", request.getDestinationContext(), request);
    }

    @Override // org.eclipse.californium.core.network.interceptors.MessageInterceptor
    public void sendResponse(Response response) {
        LOGGER.info("{} <== res {}", response.getDestinationContext(), response);
    }

    @Override // org.eclipse.californium.core.network.interceptors.MessageInterceptor
    public void sendEmptyMessage(EmptyMessage message) {
        LOGGER.info("{} <== emp {}", message.getDestinationContext(), message);
    }

    @Override // org.eclipse.californium.core.network.interceptors.MessageInterceptor
    public void receiveRequest(Request request) {
        LOGGER.info("{} ==> req {}", request.getSourceContext(), request);
    }

    @Override // org.eclipse.californium.core.network.interceptors.MessageInterceptor
    public void receiveResponse(Response response) {
        LOGGER.info("{} ==> res {}", response.getSourceContext(), response);
    }

    @Override // org.eclipse.californium.core.network.interceptors.MessageInterceptor
    public void receiveEmptyMessage(EmptyMessage message) {
        LOGGER.info("{} ==> emp {}", message.getSourceContext(), message);
    }
}
