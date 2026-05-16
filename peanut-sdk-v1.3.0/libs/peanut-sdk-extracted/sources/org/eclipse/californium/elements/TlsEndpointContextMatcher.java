package org.eclipse.californium.elements;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-element-connector.jar:org/eclipse/californium/elements/TlsEndpointContextMatcher.class */
public class TlsEndpointContextMatcher extends DefinitionsEndpointContextMatcher {
    private static final Definitions<Definition<?>> DEFINITIONS = new Definitions("tls context").add(TcpEndpointContext.KEY_CONNECTION_ID).add(TlsEndpointContext.KEY_SESSION_ID).add(TlsEndpointContext.KEY_CIPHER);

    public TlsEndpointContextMatcher() {
        super(DEFINITIONS);
    }
}
