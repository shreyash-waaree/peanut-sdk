package org.eclipse.californium.elements;

import java.net.InetSocketAddress;
import org.eclipse.californium.elements.util.StringUtil;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-element-connector.jar:org/eclipse/californium/elements/DefinitionsEndpointContextMatcher.class */
public abstract class DefinitionsEndpointContextMatcher implements EndpointContextMatcher {
    private final String sendTag;
    private final String recvTag;
    private final Definitions<Definition<?>> definitions;
    private final boolean compareHostname;

    public DefinitionsEndpointContextMatcher(Definitions<Definition<?>> definitions) {
        this(definitions, false);
    }

    public DefinitionsEndpointContextMatcher(Definitions<Definition<?>> definitions, boolean compareHostname) {
        if (definitions == null) {
            throw new NullPointerException("Definitions must not be null!");
        }
        this.sendTag = definitions.getName() + " sending";
        this.recvTag = definitions.getName() + " receiving";
        this.definitions = definitions;
        this.compareHostname = compareHostname;
    }

    @Override // org.eclipse.californium.elements.EndpointContextMatcher, org.eclipse.californium.elements.EndpointIdentityResolver
    public String getName() {
        return this.definitions.getName();
    }

    @Override // org.eclipse.californium.elements.EndpointIdentityResolver
    public Object getEndpointIdentity(EndpointContext context) {
        InetSocketAddress address = context.getPeerAddress();
        if (address.isUnresolved()) {
            throw new IllegalArgumentException(StringUtil.toDisplayString(address) + " must be resolved!");
        }
        return address;
    }

    @Override // org.eclipse.californium.elements.EndpointContextMatcher
    public boolean isResponseRelatedToRequest(EndpointContext requestContext, EndpointContext responseContext) {
        boolean result = this.compareHostname ? isSameVirtualHost(requestContext, responseContext) : true;
        return result && internalMatch(this.recvTag, requestContext, responseContext);
    }

    @Override // org.eclipse.californium.elements.EndpointContextMatcher
    public boolean isToBeSent(EndpointContext messageContext, EndpointContext connectionContext) {
        if (null == connectionContext) {
            return !messageContext.hasCriticalEntries();
        }
        boolean result = this.compareHostname ? isSameVirtualHost(messageContext, connectionContext) : true;
        return result && internalMatch(this.sendTag, messageContext, connectionContext);
    }

    private final boolean internalMatch(String tag, EndpointContext requestedContext, EndpointContext availableContext) {
        if (!requestedContext.hasCriticalEntries()) {
            return true;
        }
        return EndpointContextUtil.match(tag, this.definitions, requestedContext, availableContext);
    }

    @Override // org.eclipse.californium.elements.EndpointContextMatcher
    public String toRelevantState(EndpointContext context) {
        if (context == null) {
            return "n.a.";
        }
        return context.toString();
    }

    public static final boolean isSameVirtualHost(EndpointContext firstContext, EndpointContext secondContext) {
        String firstVirtualHost;
        String otherVirtualHost;
        if (firstContext == null) {
            throw new NullPointerException("first context must not be null");
        }
        return secondContext == null || (firstVirtualHost = firstContext.getVirtualHost()) == (otherVirtualHost = secondContext.getVirtualHost()) || (firstVirtualHost != null && firstVirtualHost.equals(otherVirtualHost));
    }
}
