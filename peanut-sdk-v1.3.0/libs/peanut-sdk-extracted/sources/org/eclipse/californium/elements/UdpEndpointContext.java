package org.eclipse.californium.elements;

import java.net.InetSocketAddress;
import org.eclipse.californium.elements.MapBasedEndpointContext;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-element-connector.jar:org/eclipse/californium/elements/UdpEndpointContext.class */
public class UdpEndpointContext extends MapBasedEndpointContext {
    public static final Definition<String> KEY_PLAIN = new Definition<>("PLAIN", String.class, ATTRIBUTE_DEFINITIONS);

    public UdpEndpointContext(InetSocketAddress peerAddress) {
        super(peerAddress, null, new MapBasedEndpointContext.Attributes().add(KEY_PLAIN, ""));
    }

    @Override // org.eclipse.californium.elements.MapBasedEndpointContext, org.eclipse.californium.elements.AddressEndpointContext
    public String toString() {
        return String.format("UDP(%s)", getPeerAddressAsString());
    }
}
