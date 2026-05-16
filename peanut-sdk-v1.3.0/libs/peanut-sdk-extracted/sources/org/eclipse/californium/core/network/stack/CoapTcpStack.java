package org.eclipse.californium.core.network.stack;

import org.eclipse.californium.core.network.Outbox;
import org.eclipse.californium.elements.config.Configuration;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-californium-core.jar:org/eclipse/californium/core/network/stack/CoapTcpStack.class */
public class CoapTcpStack extends BaseCoapStack {
    public CoapTcpStack(String tag, Configuration config, Outbox outbox) {
        super(outbox);
        Layer[] layers = {new TcpExchangeCleanupLayer(), new TcpObserveLayer(config), new BlockwiseLayer(tag, true, config), new TcpAdaptionLayer()};
        setLayers(layers);
    }
}
