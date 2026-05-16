package org.eclipse.californium.elements;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-element-connector.jar:org/eclipse/californium/elements/StrictDtlsEndpointContextMatcher.class */
public class StrictDtlsEndpointContextMatcher extends DefinitionsEndpointContextMatcher {
    private static final Definitions<Definition<?>> DEFINITIONS = new Definitions("strict context").add(DtlsEndpointContext.KEY_SESSION_ID).add(DtlsEndpointContext.KEY_CIPHER).add(DtlsEndpointContext.KEY_EPOCH);

    public StrictDtlsEndpointContextMatcher() {
        super(DEFINITIONS, true);
    }
}
