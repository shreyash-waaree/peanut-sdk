package org.eclipse.californium.core.observe;

import java.net.InetSocketAddress;
import java.util.concurrent.ConcurrentHashMap;
import org.eclipse.californium.core.coap.Token;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-californium-core.jar:org/eclipse/californium/core/observe/ObserveManager.class */
public class ObserveManager {
    private final ConcurrentHashMap<InetSocketAddress, ObservingEndpoint> endpoints = new ConcurrentHashMap<>();

    public ObservingEndpoint findObservingEndpoint(InetSocketAddress address) {
        ObservingEndpoint ep = this.endpoints.get(address);
        if (ep == null) {
            ep = createObservingEndpoint(address);
        }
        return ep;
    }

    public ObservingEndpoint getObservingEndpoint(InetSocketAddress address) {
        return this.endpoints.get(address);
    }

    private ObservingEndpoint createObservingEndpoint(InetSocketAddress address) {
        ObservingEndpoint ep = new ObservingEndpoint(address);
        ObservingEndpoint previous = this.endpoints.putIfAbsent(address, ep);
        if (previous != null) {
            return previous;
        }
        return ep;
    }

    public ObserveRelation getRelation(InetSocketAddress source, Token token) {
        ObservingEndpoint remote = getObservingEndpoint(source);
        if (remote != null) {
            return remote.getObserveRelation(token);
        }
        return null;
    }
}
