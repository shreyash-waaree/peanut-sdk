package org.eclipse.californium.core.coap;

import org.eclipse.californium.core.coap.CoAP;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-californium-core.jar:org/eclipse/californium/core/coap/Response.class */
public class Response extends Message {
    private final CoAP.ResponseCode code;
    private volatile Long applicationRttNanos;
    private volatile Long transmissionRttNanos;

    public static Response createResponse(Request receivedRequest, CoAP.ResponseCode code) {
        if (receivedRequest == null) {
            throw new NullPointerException("received request must not be null!");
        }
        if (receivedRequest.getSourceContext() == null) {
            throw new IllegalArgumentException("received request must contain a source context.");
        }
        Response response = new Response(code);
        response.setDestinationContext(receivedRequest.getSourceContext());
        return response;
    }

    public Response(CoAP.ResponseCode code) {
        if (code == null) {
            throw new NullPointerException("ResponseCode must not be null!");
        }
        this.code = code;
    }

    public CoAP.ResponseCode getCode() {
        return this.code;
    }

    @Override // org.eclipse.californium.core.coap.Message
    public int getRawCode() {
        return this.code.value;
    }

    @Override // org.eclipse.californium.core.coap.Message
    public void assertPayloadMatchsBlocksize() {
        BlockOption block2 = getOptions().getBlock2();
        if (block2 != null) {
            block2.assertPayloadSize(getPayloadSize());
        }
    }

    public String toString() {
        return toTracingString(this.code.toString());
    }

    public Long getApplicationRttNanos() {
        return this.applicationRttNanos;
    }

    public void setApplicationRttNanos(long rttNanos) {
        this.applicationRttNanos = Long.valueOf(rttNanos);
    }

    public Long getTransmissionRttNanos() {
        return this.transmissionRttNanos;
    }

    public void setTransmissionRttNanos(long rtt) {
        this.transmissionRttNanos = Long.valueOf(rtt);
    }

    public void ensureToken(Token token) {
        Token current = getToken();
        if (current == null) {
            setToken(token);
        } else if (!current.equals(token)) {
            throw new IllegalArgumentException("token mismatch! (" + current + "!=" + token + ")");
        }
    }

    public boolean isNotification() {
        return getOptions().hasObserve();
    }

    public boolean hasBlockOption() {
        return getOptions().hasBlock1() || getOptions().hasBlock2();
    }

    @Override // org.eclipse.californium.core.coap.Message
    public boolean hasBlock(BlockOption block) {
        return hasBlock(block, getOptions().getBlock2());
    }

    public final boolean isSuccess() {
        return this.code.isSuccess();
    }

    public final boolean isError() {
        return isClientError() || isServerError();
    }

    public final boolean isClientError() {
        return this.code.isClientError();
    }

    public final boolean isServerError() {
        return this.code.isServerError();
    }
}
