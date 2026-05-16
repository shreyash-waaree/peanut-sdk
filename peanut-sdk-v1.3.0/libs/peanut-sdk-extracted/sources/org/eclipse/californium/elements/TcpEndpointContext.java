package org.eclipse.californium.elements;

import java.net.InetSocketAddress;
import java.security.Principal;
import org.eclipse.californium.elements.MapBasedEndpointContext;
import org.eclipse.californium.elements.util.StringUtil;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-element-connector.jar:org/eclipse/californium/elements/TcpEndpointContext.class */
public class TcpEndpointContext extends MapBasedEndpointContext {
    public static final Definition<String> KEY_CONNECTION_ID = new Definition<>("CONNECTION_ID", String.class, ATTRIBUTE_DEFINITIONS);
    public static final Definition<Long> KEY_CONNECTION_TIMESTAMP = new Definition<>("CONNECTION_TIMESTAMP", Long.class, ATTRIBUTE_DEFINITIONS);

    public TcpEndpointContext(InetSocketAddress peerAddress, String connectionId, long timestamp) {
        this(peerAddress, (Principal) null, new MapBasedEndpointContext.Attributes().add(KEY_CONNECTION_ID, connectionId).add(KEY_CONNECTION_TIMESTAMP, Long.valueOf(timestamp)));
    }

    protected TcpEndpointContext(InetSocketAddress peerAddress, Principal peerIdentity, MapBasedEndpointContext.Attributes attributes) {
        super(peerAddress, peerIdentity, attributes);
        if (null == getConnectionId()) {
            throw new IllegalArgumentException("Missing " + KEY_CONNECTION_ID + " attribute!");
        }
    }

    public String getConnectionId() {
        return (String) get(KEY_CONNECTION_ID);
    }

    public final Number getConnectionTimestamp() {
        return (Number) get(KEY_CONNECTION_TIMESTAMP);
    }

    @Override // org.eclipse.californium.elements.MapBasedEndpointContext, org.eclipse.californium.elements.AddressEndpointContext
    public String toString() {
        return String.format("TCP(%s,ID:%s)", getPeerAddressAsString(), StringUtil.trunc(getConnectionId(), 10));
    }
}
