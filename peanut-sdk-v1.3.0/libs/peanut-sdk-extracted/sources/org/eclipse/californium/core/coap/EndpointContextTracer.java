package org.eclipse.californium.core.coap;

import java.util.concurrent.atomic.AtomicReference;
import org.eclipse.californium.core.Utils;
import org.eclipse.californium.elements.EndpointContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-californium-core.jar:org/eclipse/californium/core/coap/EndpointContextTracer.class */
public class EndpointContextTracer extends MessageObserverAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(EndpointContextTracer.class);
    private final AtomicReference<EndpointContext> endpointContext = new AtomicReference<>();

    @Override // org.eclipse.californium.core.coap.MessageObserverAdapter, org.eclipse.californium.core.coap.MessageObserver
    public void onContextEstablished(EndpointContext endpointContext) {
        if (this.endpointContext.compareAndSet(null, endpointContext)) {
            onContextChanged(endpointContext);
        }
    }

    public EndpointContext getCurrentContext() {
        return this.endpointContext.get();
    }

    protected void onContextChanged(EndpointContext endpointContext) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("{}", Utils.prettyPrint(endpointContext));
        }
    }
}
