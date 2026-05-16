package org.eclipse.californium.elements;

import java.net.InetSocketAddress;
import java.security.Principal;
import java.util.Map;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-element-connector.jar:org/eclipse/californium/elements/EndpointContext.class */
public interface EndpointContext {
    <T> T get(Definition<T> definition);

    <T> String getString(Definition<T> definition);

    Map<Definition<?>, Object> entries();

    boolean hasCriticalEntries();

    Principal getPeerIdentity();

    InetSocketAddress getPeerAddress();

    String getVirtualHost();
}
