package org.eclipse.californium.core.coap;

import org.eclipse.californium.elements.util.Bytes;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-californium-core.jar:org/eclipse/californium/core/coap/Token.class */
public class Token extends Bytes {
    public static final Token EMPTY = new Token(Bytes.EMPTY);

    public Token(byte[] token) {
        this(token, true);
    }

    private Token(byte[] token, boolean copy) {
        super(token, 8, copy);
    }

    @Override // org.eclipse.californium.elements.util.Bytes
    public String toString() {
        return "Token=" + getAsString();
    }

    public static Token fromProvider(byte[] token) {
        return new Token(token, false);
    }
}
