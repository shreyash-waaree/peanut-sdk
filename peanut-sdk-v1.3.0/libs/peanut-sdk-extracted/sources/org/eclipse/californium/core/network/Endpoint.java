package org.eclipse.californium.core.network;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import org.eclipse.californium.core.coap.EmptyMessage;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.californium.core.coap.Token;
import org.eclipse.californium.core.network.interceptors.MessageInterceptor;
import org.eclipse.californium.core.observe.NotificationListener;
import org.eclipse.californium.core.server.MessageDeliverer;
import org.eclipse.californium.elements.config.Configuration;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-californium-core.jar:org/eclipse/californium/core/network/Endpoint.class */
public interface Endpoint {
    void start() throws IOException;

    void stop();

    void destroy();

    void clear();

    boolean isStarted();

    void setExecutors(ScheduledExecutorService scheduledExecutorService, ScheduledExecutorService scheduledExecutorService2);

    void addObserver(EndpointObserver endpointObserver);

    void removeObserver(EndpointObserver endpointObserver);

    void addNotificationListener(NotificationListener notificationListener);

    void removeNotificationListener(NotificationListener notificationListener);

    void addInterceptor(MessageInterceptor messageInterceptor);

    void removeInterceptor(MessageInterceptor messageInterceptor);

    List<MessageInterceptor> getInterceptors();

    void addPostProcessInterceptor(MessageInterceptor messageInterceptor);

    void removePostProcessInterceptor(MessageInterceptor messageInterceptor);

    List<MessageInterceptor> getPostProcessInterceptors();

    void sendRequest(Request request);

    void sendResponse(Exchange exchange, Response response);

    void sendEmptyMessage(Exchange exchange, EmptyMessage emptyMessage);

    void setMessageDeliverer(MessageDeliverer messageDeliverer);

    InetSocketAddress getAddress();

    URI getUri();

    Configuration getConfig();

    void cancelObservation(Token token);
}
