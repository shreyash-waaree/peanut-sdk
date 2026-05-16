package org.eclipse.californium.elements;

import java.net.InetSocketAddress;
import java.security.Principal;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-element-connector.jar:org/eclipse/californium/elements/RawData.class */
public final class RawData {
    public final byte[] bytes;
    private final long receiveNanoTimestamp;
    private final boolean multicast;
    private final InetSocketAddress connector;
    private final EndpointContext peerEndpointContext;
    private final MessageCallback callback;

    private RawData(byte[] data, EndpointContext peerEndpointContext, MessageCallback callback, boolean multicast, long nanoTimestamp, InetSocketAddress connector) {
        if (data == null) {
            throw new NullPointerException("Data must not be null");
        }
        if (peerEndpointContext == null) {
            throw new NullPointerException("Peer's EndpointContext must not be null");
        }
        this.bytes = data;
        this.peerEndpointContext = peerEndpointContext;
        this.callback = callback;
        this.multicast = multicast;
        this.receiveNanoTimestamp = nanoTimestamp;
        this.connector = connector;
    }

    public static RawData inbound(byte[] data, EndpointContext peerEndpointContext, boolean isMulticast, long nanoTimestamp, InetSocketAddress connector) {
        if (connector == null) {
            throw new NullPointerException("Connectors's address must not be null");
        }
        return new RawData(data, peerEndpointContext, null, isMulticast, nanoTimestamp, connector);
    }

    public static RawData outbound(byte[] data, EndpointContext peerEndpointContext, MessageCallback callback, boolean useMulticast) {
        return new RawData(data, peerEndpointContext, callback, useMulticast, 0L, null);
    }

    public byte[] getBytes() {
        return this.bytes;
    }

    public int getSize() {
        return this.bytes.length;
    }

    public long getReceiveNanoTimestamp() {
        return this.receiveNanoTimestamp;
    }

    public boolean isMulticast() {
        return this.multicast;
    }

    public InetSocketAddress getConnectorAddress() {
        return this.connector;
    }

    public InetSocketAddress getInetSocketAddress() {
        return this.peerEndpointContext.getPeerAddress();
    }

    public Principal getSenderIdentity() {
        return this.peerEndpointContext.getPeerIdentity();
    }

    public EndpointContext getEndpointContext() {
        return this.peerEndpointContext;
    }

    public void onConnecting() {
        if (null != this.callback) {
            this.callback.onConnecting();
        }
    }

    public void onDtlsRetransmission(int flight) {
        if (null != this.callback) {
            this.callback.onDtlsRetransmission(flight);
        }
    }

    public void onContextEstablished(EndpointContext context) {
        if (null != this.callback) {
            this.callback.onContextEstablished(context);
        }
    }

    public void onSent() {
        if (null != this.callback) {
            this.callback.onSent();
        }
    }

    public void onError(Throwable error) {
        if (null != this.callback) {
            if (null == error) {
                error = new UnknownError();
            }
            this.callback.onError(error);
        }
    }
}
