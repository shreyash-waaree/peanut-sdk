package org.eclipse.californium.core.network;

import org.eclipse.californium.core.coap.Token;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-californium-core.jar:org/eclipse/californium/core/network/TokenGenerator.class */
public interface TokenGenerator {

    /* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-californium-core.jar:org/eclipse/californium/core/network/TokenGenerator$Scope.class */
    public enum Scope {
        LONG_TERM,
        SHORT_TERM,
        SHORT_TERM_CLIENT_LOCAL
    }

    Token createToken(Scope scope);

    Scope getScope(Token token);

    KeyToken getKeyToken(Token token, Object obj);
}
