package org.eclipse.californium.core.network.stack;

import java.util.concurrent.ScheduledExecutorService;
import org.eclipse.californium.core.coap.EmptyMessage;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.californium.core.network.Exchange;
import org.eclipse.californium.core.server.MessageDeliverer;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-californium-core.jar:org/eclipse/californium/core/network/stack/CoapStack.class */
public interface CoapStack {
    void sendRequest(Exchange exchange, Request request);

    void sendResponse(Exchange exchange, Response response);

    void sendEmptyMessage(Exchange exchange, EmptyMessage emptyMessage);

    void receiveRequest(Exchange exchange, Request request);

    void receiveResponse(Exchange exchange, Response response);

    void receiveEmptyMessage(Exchange exchange, EmptyMessage emptyMessage);

    void setExecutors(ScheduledExecutorService scheduledExecutorService, ScheduledExecutorService scheduledExecutorService2);

    void setDeliverer(MessageDeliverer messageDeliverer);

    void start();

    void destroy();

    boolean hasDeliverer();
}
