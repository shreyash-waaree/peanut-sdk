package org.eclipse.californium.elements;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-element-connector.jar:org/eclipse/californium/elements/RelaxedDtlsEndpointContextMatcher.class */
public class RelaxedDtlsEndpointContextMatcher extends DefinitionsEndpointContextMatcher {
    private static final Definitions<Definition<?>> DEFINITIONS = new Definitions("relaxed context").add(DtlsEndpointContext.KEY_SESSION_ID).add(DtlsEndpointContext.KEY_CIPHER);

    public RelaxedDtlsEndpointContextMatcher() {
        super(DEFINITIONS, true);
    }
}
