package org.eclipse.californium.elements;

import java.net.InetSocketAddress;
import java.security.Principal;
import org.eclipse.californium.elements.MapBasedEndpointContext;
import org.eclipse.californium.elements.util.StringUtil;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-element-connector.jar:org/eclipse/californium/elements/TlsEndpointContext.class */
public class TlsEndpointContext extends TcpEndpointContext {
    public static final Definition<String> KEY_SESSION_ID = new Definition<>("TLS_SESSION_ID", String.class, ATTRIBUTE_DEFINITIONS);
    public static final Definition<String> KEY_CIPHER = new Definition<>("TLS_CIPHER", String.class, ATTRIBUTE_DEFINITIONS);

    public TlsEndpointContext(InetSocketAddress peerAddress, Principal peerIdentity, String connectionId, String sessionId, String cipher, long timestamp) {
        super(peerAddress, peerIdentity, new MapBasedEndpointContext.Attributes().add(KEY_CONNECTION_ID, connectionId).add(KEY_CONNECTION_TIMESTAMP, Long.valueOf(timestamp)).add(KEY_SESSION_ID, sessionId).add(KEY_CIPHER, cipher));
    }

    public String getSessionId() {
        return (String) get(KEY_SESSION_ID);
    }

    public String getCipher() {
        return (String) get(KEY_CIPHER);
    }

    @Override // org.eclipse.californium.elements.TcpEndpointContext, org.eclipse.californium.elements.MapBasedEndpointContext, org.eclipse.californium.elements.AddressEndpointContext
    public String toString() {
        return String.format("TLS(%s,%s,%s,%s)", getPeerAddressAsString(), StringUtil.trunc(getConnectionId(), 10), StringUtil.trunc(getSessionId(), 10), getCipher());
    }
}
