package org.eclipse.californium.core.coap;

import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.elements.util.Bytes;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-californium-core.jar:org/eclipse/californium/core/coap/NoResponseOption.class */
public final class NoResponseOption {
    public static final int SUPPRESS_SUCCESS = 2;
    public static final int SUPPRESS_CLIENT_ERROR = 8;
    public static final int SUPPRESS_SERVER_ERROR = 16;
    public static final int SUPPRESS_ALL = 26;
    private final int mask;

    public NoResponseOption(int mask) {
        if (mask < 0 || mask > 255) {
            throw new IllegalArgumentException("No-Response option " + mask + " must be between 0 and 255 inclusive");
        }
        this.mask = mask;
    }

    public byte[] getValue() {
        if (this.mask == 0) {
            return Bytes.EMPTY;
        }
        return new byte[]{(byte) this.mask};
    }

    public int getMask() {
        return this.mask;
    }

    public boolean suppress(int code) {
        int bit = 1 << (CoAP.getCodeClass(code) - 1);
        return (this.mask & bit) != 0;
    }

    public boolean suppress(CoAP.ResponseCode code) {
        int bit = 1 << (code.codeClass - 1);
        return (this.mask & bit) != 0;
    }

    public Option toOption() {
        return new Option(OptionNumberRegistry.NO_RESPONSE, getValue());
    }

    public String toString() {
        if ((this.mask & 26) != 0) {
            StringBuilder text = new StringBuilder("NO ");
            if ((this.mask & 2) != 0) {
                text.append("SUCCESS,");
            }
            if ((this.mask & 8) != 0) {
                text.append("CLIENT_ERROR,");
            }
            if ((this.mask & 16) != 0) {
                text.append("SERVER_ERROR,");
            }
            text.setLength(text.length() - 1);
            return text.toString();
        }
        return "ALL";
    }

    public boolean equals(Object o) {
        if (!(o instanceof NoResponseOption)) {
            return false;
        }
        NoResponseOption option = (NoResponseOption) o;
        return this.mask == option.mask;
    }

    public int hashCode() {
        return this.mask;
    }
}
