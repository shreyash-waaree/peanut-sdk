package org.eclipse.californium.core.network;

import java.net.InetSocketAddress;
import org.eclipse.californium.elements.util.StringUtil;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-californium-core.jar:org/eclipse/californium/core/network/KeyMID.class */
public final class KeyMID {
    private final int mid;
    private final Object peer;
    private final int hash;

    public KeyMID(int mid, Object peer) {
        if (mid < 0 || mid > 65535) {
            throw new IllegalArgumentException("MID must be a 16 bit unsigned int: " + mid);
        }
        if (peer == null) {
            throw new NullPointerException("peer must not be null");
        }
        this.mid = mid;
        this.peer = peer;
        this.hash = (31 * mid) + peer.hashCode();
    }

    public int getMID() {
        return this.mid;
    }

    public Object getPeer() {
        return this.peer;
    }

    public int hashCode() {
        return this.hash;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        KeyMID other = (KeyMID) obj;
        if (this.mid != other.mid) {
            return false;
        }
        return this.peer.equals(other.peer);
    }

    public String toString() {
        Object peer = this.peer;
        if (peer instanceof InetSocketAddress) {
            peer = StringUtil.toDisplayString((InetSocketAddress) peer);
        }
        return "KeyMID[" + peer + '-' + this.mid + ']';
    }
}
