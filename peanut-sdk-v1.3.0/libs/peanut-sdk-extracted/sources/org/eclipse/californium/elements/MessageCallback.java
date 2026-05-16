package org.eclipse.californium.elements;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-element-connector.jar:org/eclipse/californium/elements/MessageCallback.class */
public interface MessageCallback {
    void onConnecting();

    void onDtlsRetransmission(int i);

    void onContextEstablished(EndpointContext endpointContext);

    void onSent();

    void onError(Throwable th);
}
