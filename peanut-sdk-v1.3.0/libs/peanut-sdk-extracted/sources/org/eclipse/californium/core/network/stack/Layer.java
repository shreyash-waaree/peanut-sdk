package org.eclipse.californium.core.network.stack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import org.eclipse.californium.core.coap.EmptyMessage;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.californium.core.network.Exchange;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-californium-core.jar:org/eclipse/californium/core/network/stack/Layer.class */
public interface Layer {
    void sendRequest(Exchange exchange, Request request);

    void sendResponse(Exchange exchange, Response response);

    void sendEmptyMessage(Exchange exchange, EmptyMessage emptyMessage);

    void receiveRequest(Exchange exchange, Request request);

    void receiveResponse(Exchange exchange, Response response);

    void receiveEmptyMessage(Exchange exchange, EmptyMessage emptyMessage);

    void setLowerLayer(Layer layer);

    void setUpperLayer(Layer layer);

    void setExecutors(ScheduledExecutorService scheduledExecutorService, ScheduledExecutorService scheduledExecutorService2);

    void start();

    void destroy();

    /* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-californium-core.jar:org/eclipse/californium/core/network/stack/Layer$TopDownBuilder.class */
    public static final class TopDownBuilder {
        private final List<Layer> stack = new ArrayList();
        private Layer bottom;

        public TopDownBuilder add(Layer layer) {
            if (this.bottom != null) {
                this.bottom.setLowerLayer(layer);
            }
            this.stack.add(layer);
            this.bottom = layer;
            return this;
        }

        public List<Layer> create() {
            return Collections.unmodifiableList(new ArrayList(this.stack));
        }
    }
}
