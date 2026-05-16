package org.eclipse.californium.core.network;

import java.security.SecureRandom;
import org.eclipse.californium.core.coap.Token;
import org.eclipse.californium.core.config.CoapConfig;
import org.eclipse.californium.core.network.TokenGenerator;
import org.eclipse.californium.elements.config.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-californium-core.jar:org/eclipse/californium/core/network/RandomTokenGenerator.class */
public class RandomTokenGenerator implements TokenGenerator {
    private static final Logger LOGGER = LoggerFactory.getLogger(RandomTokenGenerator.class);
    private final int tokenSize;
    private final SecureRandom rng;

    public RandomTokenGenerator(Configuration config) {
        if (config == null) {
            throw new NullPointerException("NetworkConfig must not be null");
        }
        this.rng = new SecureRandom();
        this.rng.nextInt(10);
        this.tokenSize = ((Integer) config.get(CoapConfig.TOKEN_SIZE_LIMIT)).intValue();
        LOGGER.info("using tokens of {} bytes in length", Integer.valueOf(this.tokenSize));
    }

    @Override // org.eclipse.californium.core.network.TokenGenerator
    public Token createToken(TokenGenerator.Scope scope) {
        byte[] token = new byte[this.tokenSize];
        this.rng.nextBytes(token);
        switch (scope) {
            case LONG_TERM:
                token[0] = (byte) (token[0] | 1);
                break;
            case SHORT_TERM:
                token[0] = (byte) (token[0] & 252);
                token[0] = (byte) (token[0] | 2);
                break;
            case SHORT_TERM_CLIENT_LOCAL:
                token[0] = (byte) (token[0] & 252);
                break;
        }
        return Token.fromProvider(token);
    }

    @Override // org.eclipse.californium.core.network.TokenGenerator
    public TokenGenerator.Scope getScope(Token token) {
        if (token.length() != this.tokenSize) {
            return TokenGenerator.Scope.SHORT_TERM_CLIENT_LOCAL;
        }
        int scope = token.getBytes()[0] & 3;
        switch (scope) {
            case 0:
                break;
            case 2:
                break;
        }
        return TokenGenerator.Scope.SHORT_TERM_CLIENT_LOCAL;
    }

    @Override // org.eclipse.californium.core.network.TokenGenerator
    public KeyToken getKeyToken(Token token, Object peer) {
        if (getScope(token) == TokenGenerator.Scope.SHORT_TERM_CLIENT_LOCAL) {
            if (peer == null) {
                throw new IllegalArgumentException("client-local token requires peer!");
            }
            return new KeyToken(token, peer);
        }
        return new KeyToken(token, null);
    }
}
