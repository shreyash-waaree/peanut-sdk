package org.eclipse.californium.elements;

import com.keenon.sdk.constant.ApiConstants;
import java.security.Principal;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-element-connector.jar:org/eclipse/californium/elements/PrincipalEndpointContextMatcher.class */
public class PrincipalEndpointContextMatcher implements EndpointContextMatcher {
    private final boolean usePrincipalAsIdentity;

    public PrincipalEndpointContextMatcher() {
        this(false);
    }

    public PrincipalEndpointContextMatcher(boolean usePrincipalAsIdentity) {
        this.usePrincipalAsIdentity = usePrincipalAsIdentity;
    }

    @Override // org.eclipse.californium.elements.EndpointContextMatcher, org.eclipse.californium.elements.EndpointIdentityResolver
    public String getName() {
        return "principal correlation";
    }

    @Override // org.eclipse.californium.elements.EndpointIdentityResolver
    public Object getEndpointIdentity(EndpointContext context) {
        if (this.usePrincipalAsIdentity) {
            Principal identity = context.getPeerIdentity();
            if (identity == null) {
                throw new IllegalArgumentException("Principal identity missing in provided endpoint context!");
            }
            return identity;
        }
        return context.getPeerAddress();
    }

    @Override // org.eclipse.californium.elements.EndpointContextMatcher
    public boolean isResponseRelatedToRequest(EndpointContext requestContext, EndpointContext responseContext) {
        return internalMatch(requestContext, responseContext);
    }

    @Override // org.eclipse.californium.elements.EndpointContextMatcher
    public boolean isToBeSent(EndpointContext messageContext, EndpointContext connectorContext) {
        if (null == connectorContext) {
            return true;
        }
        return internalMatch(messageContext, connectorContext);
    }

    private final boolean internalMatch(EndpointContext requestedContext, EndpointContext availableContext) {
        if (requestedContext.getPeerIdentity() != null && (availableContext.getPeerIdentity() == null || !matchPrincipals(requestedContext.getPeerIdentity(), availableContext.getPeerIdentity()))) {
            return false;
        }
        String cipher = requestedContext.getString(DtlsEndpointContext.KEY_CIPHER);
        if (cipher != null && !cipher.equals(availableContext.getString(DtlsEndpointContext.KEY_CIPHER))) {
            return false;
        }
        return true;
    }

    @Override // org.eclipse.californium.elements.EndpointContextMatcher
    public String toRelevantState(EndpointContext context) {
        if (context == null) {
            return "n.a.";
        }
        StringBuilder builder = new StringBuilder();
        builder.append("[");
        builder.append(context.getPeerIdentity());
        String cipher = context.getString(DtlsEndpointContext.KEY_CIPHER);
        if (cipher != null) {
            builder.append(ApiConstants.DELIMITER_COMMA).append(cipher);
        }
        builder.append("]");
        return builder.toString();
    }

    protected boolean matchPrincipals(Principal requestedPrincipal, Principal availablePrincipal) {
        return requestedPrincipal.equals(availablePrincipal);
    }
}
