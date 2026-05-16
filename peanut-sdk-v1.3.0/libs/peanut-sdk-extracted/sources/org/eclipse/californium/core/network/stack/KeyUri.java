package org.eclipse.californium.core.network.stack;

import java.net.InetSocketAddress;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.network.Exchange;
import org.eclipse.californium.elements.util.StringUtil;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-californium-core.jar:org/eclipse/californium/core/network/stack/KeyUri.class */
public final class KeyUri {
    private final String uri;
    private final Object peersIdentity;
    private final int hash;

    public KeyUri(String requestUri, Object peersIdentity) {
        if (requestUri == null) {
            throw new NullPointerException("URI must not be null");
        }
        if (peersIdentity == null) {
            throw new NullPointerException("peer's identity must not be null");
        }
        this.uri = requestUri;
        this.peersIdentity = peersIdentity;
        this.hash = (requestUri.hashCode() * 31) + peersIdentity.hashCode();
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        KeyUri other = (KeyUri) obj;
        if (!this.peersIdentity.equals(other.peersIdentity) || !this.uri.equals(other.uri)) {
            return false;
        }
        return true;
    }

    public int hashCode() {
        return this.hash;
    }

    public String toString() {
        StringBuilder b = new StringBuilder("KeyUri[");
        b.append(this.uri);
        Object peer = this.peersIdentity;
        if (peer instanceof InetSocketAddress) {
            peer = StringUtil.toDisplayString((InetSocketAddress) peer);
        }
        b.append(", ").append(peer).append("]");
        return b.toString();
    }

    private static String getUri(Request request) {
        if (request == null) {
            throw new NullPointerException("request must not be null");
        }
        return request.getScheme() + ":" + request.getOptions().getUriString();
    }

    public static KeyUri getKey(Exchange exchange) {
        if (exchange == null) {
            throw new NullPointerException("exchange must not be null");
        }
        String uri = getUri(exchange.getRequest());
        return new KeyUri(uri, exchange.getPeersIdentity());
    }
}
