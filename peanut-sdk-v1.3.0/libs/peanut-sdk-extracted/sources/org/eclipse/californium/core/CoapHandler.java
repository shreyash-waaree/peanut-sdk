package org.eclipse.californium.core;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-californium-core.jar:org/eclipse/californium/core/CoapHandler.class */
public interface CoapHandler {
    void onLoad(CoapResponse coapResponse);

    void onError();
}
