package org.eclipse.californium.core.network;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-californium-core.jar:org/eclipse/californium/core/network/ExchangeCompleteException.class */
public class ExchangeCompleteException extends IllegalStateException {
    private static final long serialVersionUID = 1;

    public ExchangeCompleteException(String message, Throwable caller) {
        super(message, caller);
    }
}
