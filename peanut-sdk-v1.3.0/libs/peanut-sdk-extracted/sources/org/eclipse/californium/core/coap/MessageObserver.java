package org.eclipse.californium.core.coap;

import org.eclipse.californium.elements.EndpointContext;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-californium-core.jar:org/eclipse/californium/core/coap/MessageObserver.class */
public interface MessageObserver {
    boolean isInternal();

    void onRetransmission();

    void onResponse(Response response);

    void onAcknowledgement();

    void onReject();

    void onTimeout();

    void onCancel();

    void onReadyToSend();

    void onConnecting();

    void onDtlsRetransmission(int i);

    void onSent(boolean z);

    void onSendError(Throwable th);

    void onResponseHandlingError(Throwable th);

    void onContextEstablished(EndpointContext endpointContext);

    void onTransferComplete();
}
