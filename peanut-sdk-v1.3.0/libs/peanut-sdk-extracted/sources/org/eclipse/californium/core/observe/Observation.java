package org.eclipse.californium.core.observe;

import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.elements.EndpointContext;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-californium-core.jar:org/eclipse/californium/core/observe/Observation.class */
public final class Observation {
    private final Request request;
    private final EndpointContext context;

    public Observation(Request request, EndpointContext context) {
        if (request == null) {
            throw new NullPointerException("request must not be null");
        }
        if (!request.isObserve()) {
            throw new IllegalArgumentException("request has no observe=0 option");
        }
        this.request = request;
        this.context = context;
    }

    public Request getRequest() {
        return this.request;
    }

    public EndpointContext getContext() {
        return this.context;
    }

    public String toString() {
        return this.request.toString();
    }
}
