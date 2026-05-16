package org.eclipse.californium.core.server;

import org.eclipse.californium.core.coap.CoAP;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-californium-core.jar:org/eclipse/californium/core/server/DelivererException.class */
public class DelivererException extends Exception {
    private static final long serialVersionUID = 123;
    private final CoAP.ResponseCode response;

    public DelivererException(CoAP.ResponseCode response, String message) {
        super(message);
        if (response.isClientError() || response.isServerError()) {
            this.response = response;
            return;
        }
        throw new IllegalArgumentException("response code " + response + " must be an error-code!");
    }

    public CoAP.ResponseCode getErrorResponseCode() {
        return this.response;
    }
}
