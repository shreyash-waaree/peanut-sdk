package org.eclipse.californium.core;

import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.OptionSet;
import org.eclipse.californium.core.coap.Response;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-californium-core.jar:org/eclipse/californium/core/CoapResponse.class */
public class CoapResponse {
    private Response response;

    protected CoapResponse(Response response) {
        if (response == null) {
            throw new NullPointerException("Response must not be null!");
        }
        this.response = response;
    }

    public CoAP.ResponseCode getCode() {
        return this.response.getCode();
    }

    public boolean isSuccess() {
        return this.response.isSuccess();
    }

    public String getResponseText() {
        return this.response.getPayloadString().replace("\n", "");
    }

    public byte[] getPayload() {
        return this.response.getPayload();
    }

    public int getPayloadSize() {
        return this.response.getPayloadSize();
    }

    public OptionSet getOptions() {
        return this.response.getOptions();
    }

    public Response advanced() {
        return this.response;
    }

    public String toString() {
        return this.response.toString();
    }
}
