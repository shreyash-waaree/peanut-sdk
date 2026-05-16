package org.eclipse.californium.core.network;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-californium-core.jar:org/eclipse/californium/core/network/EndpointObserver.class */
public interface EndpointObserver {
    void started(Endpoint endpoint);

    void stopped(Endpoint endpoint);

    void destroyed(Endpoint endpoint);
}
