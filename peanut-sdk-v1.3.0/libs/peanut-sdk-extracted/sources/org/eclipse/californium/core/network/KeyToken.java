package org.eclipse.californium.core.network;

import java.net.InetSocketAddress;
import org.eclipse.californium.core.coap.Token;
import org.eclipse.californium.elements.util.StringUtil;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-californium-core.jar:org/eclipse/californium/core/network/KeyToken.class */
public class KeyToken {
    private final int hash;
    private final Token token;
    private final Object peer;

    public KeyToken(Token token, Object peer) {
        this.token = token;
        this.peer = peer;
        int hash = token.hashCode();
        this.hash = peer != null ? hash + (peer.hashCode() * 31) : hash;
    }

    public Token getToken() {
        return this.token;
    }

    public Object getPeer() {
        return this.peer;
    }

    public final int hashCode() {
        return this.hash;
    }

    public final boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        KeyToken other = (KeyToken) obj;
        if (this.hash != other.hash || !this.token.equals(other.token)) {
            return false;
        }
        if (this.peer == other.peer) {
            return true;
        }
        if (this.peer == null) {
            return false;
        }
        return this.peer.equals(other.peer);
    }

    public String toString() {
        StringBuilder builder = new StringBuilder("KeyToken[");
        if (this.peer != null) {
            Object peer = this.peer;
            if (peer instanceof InetSocketAddress) {
                peer = StringUtil.toDisplayString((InetSocketAddress) peer);
            }
            builder.append(peer).append('-');
        }
        builder.append(this.token.getAsString()).append(']');
        return builder.toString();
    }
}
