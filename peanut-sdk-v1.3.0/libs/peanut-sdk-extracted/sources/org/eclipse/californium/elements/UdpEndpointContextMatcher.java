package org.eclipse.californium.elements;

import java.net.InetSocketAddress;
import org.eclipse.californium.elements.util.NetworkInterfacesUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-element-connector.jar:org/eclipse/californium/elements/UdpEndpointContextMatcher.class */
public class UdpEndpointContextMatcher extends DefinitionsEndpointContextMatcher {
    private static final Logger LOGGER = LoggerFactory.getLogger(UdpEndpointContextMatcher.class);
    private static final Definitions<Definition<?>> DEFINITIONS = new Definitions("udp context").add(UdpEndpointContext.KEY_PLAIN);
    public static final String MULTICAST_IDENTITY = "MULTICAST";
    private final boolean checkAddress;

    public UdpEndpointContextMatcher() {
        this(true);
    }

    public UdpEndpointContextMatcher(boolean checkAddress) {
        super(DEFINITIONS);
        this.checkAddress = checkAddress;
    }

    @Override // org.eclipse.californium.elements.DefinitionsEndpointContextMatcher, org.eclipse.californium.elements.EndpointIdentityResolver
    public Object getEndpointIdentity(EndpointContext context) {
        Object identity = super.getEndpointIdentity(context);
        if (identity instanceof InetSocketAddress) {
            InetSocketAddress address = (InetSocketAddress) identity;
            if (NetworkInterfacesUtil.isMultiAddress(address.getAddress())) {
                return MULTICAST_IDENTITY;
            }
        }
        return identity;
    }

    @Override // org.eclipse.californium.elements.DefinitionsEndpointContextMatcher, org.eclipse.californium.elements.EndpointContextMatcher
    public boolean isResponseRelatedToRequest(EndpointContext requestContext, EndpointContext responseContext) {
        if (this.checkAddress) {
            InetSocketAddress peerAddress1 = requestContext.getPeerAddress();
            InetSocketAddress peerAddress2 = responseContext.getPeerAddress();
            if (!peerAddress1.equals(peerAddress2) && !NetworkInterfacesUtil.isMultiAddress(peerAddress1.getAddress())) {
                LOGGER.info("request {}:{} doesn't match {}:{}!", new Object[]{peerAddress1.getAddress().getHostAddress(), Integer.valueOf(peerAddress1.getPort()), peerAddress2.getAddress().getHostAddress(), Integer.valueOf(peerAddress2.getPort())});
                return false;
            }
        }
        return super.isResponseRelatedToRequest(requestContext, responseContext);
    }
}
