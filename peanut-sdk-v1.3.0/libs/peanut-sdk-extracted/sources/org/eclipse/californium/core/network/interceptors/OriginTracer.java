package org.eclipse.californium.core.network.interceptors;

import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.EmptyMessage;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.elements.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-californium-core.jar:org/eclipse/californium/core/network/interceptors/OriginTracer.class */
public final class OriginTracer extends MessageInterceptorAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(OriginTracer.class);

    @Override // org.eclipse.californium.core.network.interceptors.MessageInterceptorAdapter, org.eclipse.californium.core.network.interceptors.MessageInterceptor
    public void receiveRequest(Request request) {
        LOGGER.trace("{}", StringUtil.toLog(request.getSourceContext().getPeerAddress()));
    }

    @Override // org.eclipse.californium.core.network.interceptors.MessageInterceptorAdapter, org.eclipse.californium.core.network.interceptors.MessageInterceptor
    public void receiveEmptyMessage(EmptyMessage message) {
        if (message.getType() == CoAP.Type.CON) {
            LOGGER.trace("{}", StringUtil.toLog(message.getSourceContext().getPeerAddress()));
        }
    }
}
