package org.eclipse.californium.elements;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-element-connector.jar:org/eclipse/californium/elements/EndpointIdentityResolver.class */
public interface EndpointIdentityResolver {
    String getName();

    Object getEndpointIdentity(EndpointContext endpointContext);
}
