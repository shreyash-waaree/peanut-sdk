package org.eclipse.californium.elements;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.security.Principal;
import java.util.Collections;
import java.util.Map;
import org.eclipse.californium.elements.util.Bytes;
import org.eclipse.californium.elements.util.StringUtil;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-element-connector.jar:org/eclipse/californium/elements/AddressEndpointContext.class */
public class AddressEndpointContext implements EndpointContext {
    protected static final int ID_TRUNC_LENGTH = 10;
    private final InetSocketAddress peerAddress;
    private final Principal peerIdentity;
    private final String virtualHost;

    public AddressEndpointContext(String address, int port) {
        if (address == null) {
            throw new NullPointerException("missing peer inet address!");
        }
        this.peerAddress = new InetSocketAddress(address, port);
        this.peerIdentity = null;
        this.virtualHost = null;
    }

    public AddressEndpointContext(InetAddress address, int port) {
        if (address == null) {
            throw new NullPointerException("missing peer inet address!");
        }
        this.peerAddress = new InetSocketAddress(address, port);
        this.peerIdentity = null;
        this.virtualHost = null;
    }

    public AddressEndpointContext(InetSocketAddress peerAddress) {
        this(peerAddress, null, null);
    }

    public AddressEndpointContext(InetSocketAddress peerAddress, Principal peerIdentity) {
        this(peerAddress, null, peerIdentity);
    }

    public AddressEndpointContext(InetSocketAddress peerAddress, String virtualHost, Principal peerIdentity) {
        if (peerAddress == null) {
            throw new NullPointerException("missing peer socket address, must not be null!");
        }
        this.peerAddress = peerAddress;
        this.virtualHost = virtualHost == null ? null : virtualHost.toLowerCase();
        this.peerIdentity = peerIdentity;
    }

    @Override // org.eclipse.californium.elements.EndpointContext
    public <T> T get(Definition<T> definition) {
        return null;
    }

    @Override // org.eclipse.californium.elements.EndpointContext
    public <T> String getString(Definition<T> definition) {
        Object obj = get(definition);
        if (obj != null) {
            if (obj instanceof Bytes) {
                return ((Bytes) obj).getAsString();
            }
            return obj.toString();
        }
        return null;
    }

    @Override // org.eclipse.californium.elements.EndpointContext
    public Map<Definition<?>, Object> entries() {
        return Collections.emptyMap();
    }

    @Override // org.eclipse.californium.elements.EndpointContext
    public boolean hasCriticalEntries() {
        return false;
    }

    @Override // org.eclipse.californium.elements.EndpointContext
    public final Principal getPeerIdentity() {
        return this.peerIdentity;
    }

    @Override // org.eclipse.californium.elements.EndpointContext
    public final InetSocketAddress getPeerAddress() {
        return this.peerAddress;
    }

    @Override // org.eclipse.californium.elements.EndpointContext
    public final String getVirtualHost() {
        return this.virtualHost;
    }

    public String toString() {
        return String.format("IP(%s)", getPeerAddressAsString());
    }

    protected final String getPeerAddressAsString() {
        return StringUtil.toDisplayString(this.peerAddress);
    }
}
