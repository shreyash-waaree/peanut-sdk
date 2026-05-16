package org.eclipse.californium.core.network.stack;

import org.eclipse.californium.core.coap.MessageObserverAdapter;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.californium.core.network.Exchange;
import org.eclipse.californium.elements.util.NoPublicAPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-californium-core.jar:org/eclipse/californium/core/network/stack/CleanupMessageObserver.class */
@NoPublicAPI
public class CleanupMessageObserver extends MessageObserverAdapter {
    protected static final Logger LOGGER = LoggerFactory.getLogger(CleanupMessageObserver.class);
    protected final Exchange exchange;

    protected CleanupMessageObserver(Exchange exchange) {
        super(true);
        this.exchange = exchange;
    }

    @Override // org.eclipse.californium.core.coap.MessageObserverAdapter, org.eclipse.californium.core.coap.MessageObserver
    public boolean isInternal() {
        return true;
    }

    @Override // org.eclipse.californium.core.coap.MessageObserverAdapter, org.eclipse.californium.core.coap.MessageObserver
    public void onCancel() {
        complete("canceled");
    }

    @Override // org.eclipse.californium.core.coap.MessageObserverAdapter
    public void failed() {
        complete("failed");
    }

    protected void complete(String action) {
        if (this.exchange.executeComplete()) {
            if (this.exchange.isOfLocalOrigin()) {
                Request request = this.exchange.getCurrentRequest();
                LOGGER.debug("{}, {} request [MID={}, {}]", new Object[]{action, this.exchange, Integer.valueOf(request.getMID()), request.getToken()});
            } else {
                Response response = this.exchange.getCurrentResponse();
                LOGGER.debug("{}, {} response [MID={}, {}]", new Object[]{action, this.exchange, Integer.valueOf(response.getMID()), response.getToken()});
            }
        }
    }
}
