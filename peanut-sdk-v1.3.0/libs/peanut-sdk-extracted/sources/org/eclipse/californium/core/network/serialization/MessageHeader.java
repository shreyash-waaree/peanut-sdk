package org.eclipse.californium.core.network.serialization;

import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.Token;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-californium-core.jar:org/eclipse/californium/core/network/serialization/MessageHeader.class */
public class MessageHeader {
    private final int version;
    private final CoAP.Type type;
    private final Token token;
    private final int code;
    private final int mid;
    private final int bodyLength;

    public MessageHeader(int version, CoAP.Type type, Token token, int code, int mid, int bodyLength) {
        this.version = version;
        this.type = type;
        this.token = token;
        this.code = code;
        this.mid = mid;
        this.bodyLength = bodyLength;
    }

    public int getBodyLength() {
        if (this.bodyLength < 0) {
            throw new IllegalStateException("body length not available!");
        }
        return this.bodyLength;
    }

    public int getVersion() {
        return this.version;
    }

    public CoAP.Type getType() {
        return this.type;
    }

    public Token getToken() {
        return this.token;
    }

    public int getCode() {
        return this.code;
    }

    public int getMID() {
        return this.mid;
    }
}
