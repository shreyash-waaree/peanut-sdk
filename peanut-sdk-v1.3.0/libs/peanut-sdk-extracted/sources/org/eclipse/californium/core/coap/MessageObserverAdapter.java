package org.eclipse.californium.core.coap;

import org.eclipse.californium.elements.EndpointContext;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-californium-core.jar:org/eclipse/californium/core/coap/MessageObserverAdapter.class */
public abstract class MessageObserverAdapter implements MessageObserver {
    private final boolean isInternal;

    protected MessageObserverAdapter() {
        this(false);
    }

    protected MessageObserverAdapter(boolean isInternal) {
        this.isInternal = isInternal;
    }

    @Override // org.eclipse.californium.core.coap.MessageObserver
    public boolean isInternal() {
        return this.isInternal;
    }

    @Override // org.eclipse.californium.core.coap.MessageObserver
    public void onRetransmission() {
    }

    @Override // org.eclipse.californium.core.coap.MessageObserver
    public void onResponse(Response response) {
    }

    @Override // org.eclipse.californium.core.coap.MessageObserver
    public void onAcknowledgement() {
    }

    @Override // org.eclipse.californium.core.coap.MessageObserver
    public void onReject() {
        failed();
    }

    @Override // org.eclipse.californium.core.coap.MessageObserver
    public void onCancel() {
    }

    @Override // org.eclipse.californium.core.coap.MessageObserver
    public void onTimeout() {
        failed();
    }

    @Override // org.eclipse.californium.core.coap.MessageObserver
    public void onReadyToSend() {
    }

    @Override // org.eclipse.californium.core.coap.MessageObserver
    public void onConnecting() {
    }

    @Override // org.eclipse.californium.core.coap.MessageObserver
    public void onDtlsRetransmission(int flight) {
    }

    @Override // org.eclipse.californium.core.coap.MessageObserver
    public void onSent(boolean retransmission) {
    }

    @Override // org.eclipse.californium.core.coap.MessageObserver
    public void onSendError(Throwable error) {
        failed();
    }

    @Override // org.eclipse.californium.core.coap.MessageObserver
    public void onResponseHandlingError(Throwable error) {
        failed();
    }

    @Override // org.eclipse.californium.core.coap.MessageObserver
    public void onContextEstablished(EndpointContext endpointContext) {
    }

    @Override // org.eclipse.californium.core.coap.MessageObserver
    public void onTransferComplete() {
    }

    protected void failed() {
    }
}
