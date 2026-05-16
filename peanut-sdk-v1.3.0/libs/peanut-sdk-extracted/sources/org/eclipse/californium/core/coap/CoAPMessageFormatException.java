package org.eclipse.californium.core.coap;

import org.eclipse.californium.core.coap.CoAP;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-californium-core.jar:org/eclipse/californium/core/coap/CoAPMessageFormatException.class */
public class CoAPMessageFormatException extends MessageFormatException {
    private static final long serialVersionUID = 1;
    private static final int NO_MID = -1;
    private final int mid;
    private final int code;
    private final Token token;
    private final CoAP.ResponseCode errorCode;
    private final boolean confirmable;

    public CoAPMessageFormatException(String description, Token token, int mid, int code, boolean confirmable) {
        this(description, token, mid, code, confirmable, CoAP.ResponseCode.BAD_OPTION);
    }

    public CoAPMessageFormatException(String description, Token token, int mid, int code, boolean confirmable, CoAP.ResponseCode errorCode) {
        super(description);
        this.token = token;
        this.mid = mid;
        this.code = code;
        this.confirmable = confirmable;
        this.errorCode = errorCode;
    }

    public Token getToken() {
        return this.token;
    }

    public final boolean hasMid() {
        return this.mid > -1;
    }

    public final int getMid() {
        return this.mid;
    }

    public final int getCode() {
        return this.code;
    }

    public final CoAP.ResponseCode getErrorCode() {
        return this.errorCode;
    }

    public final boolean isConfirmable() {
        return this.confirmable;
    }
}
