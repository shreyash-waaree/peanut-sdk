package org.eclipse.californium.core.network.interceptors;

import org.eclipse.californium.core.coap.EmptyMessage;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.coap.Response;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-californium-core.jar:org/eclipse/californium/core/network/interceptors/MessageInterceptorAdapter.class */
public abstract class MessageInterceptorAdapter implements MessageInterceptor {
    @Override // org.eclipse.californium.core.network.interceptors.MessageInterceptor
    public void sendRequest(Request request) {
    }

    @Override // org.eclipse.californium.core.network.interceptors.MessageInterceptor
    public void sendResponse(Response response) {
    }

    @Override // org.eclipse.californium.core.network.interceptors.MessageInterceptor
    public void sendEmptyMessage(EmptyMessage message) {
    }

    @Override // org.eclipse.californium.core.network.interceptors.MessageInterceptor
    public void receiveRequest(Request request) {
    }

    @Override // org.eclipse.californium.core.network.interceptors.MessageInterceptor
    public void receiveResponse(Response response) {
    }

    @Override // org.eclipse.californium.core.network.interceptors.MessageInterceptor
    public void receiveEmptyMessage(EmptyMessage message) {
    }
}
