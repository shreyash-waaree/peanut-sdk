package org.eclipse.californium.elements;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-element-connector.jar:org/eclipse/californium/elements/TcpEndpointContextMatcher.class */
public class TcpEndpointContextMatcher extends DefinitionsEndpointContextMatcher {
    private static final Definitions<Definition<?>> DEFINITIONS = new Definitions("tcp context").add(TcpEndpointContext.KEY_CONNECTION_ID);

    public TcpEndpointContextMatcher() {
        super(DEFINITIONS);
    }
}
