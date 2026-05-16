package org.eclipse.californium.core.network.stack;

import org.eclipse.californium.core.network.Outbox;
import org.eclipse.californium.elements.config.Configuration;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-californium-core.jar:org/eclipse/californium/core/network/stack/CoapUdpStack.class */
public class CoapUdpStack extends BaseCoapStack {
    public CoapUdpStack(String tag, Configuration config, Outbox outbox) {
        super(outbox);
        Layer[] layers = {createExchangeCleanupLayer(config), createObserveLayer(config), createBlockwiseLayer(tag, config), createReliabilityLayer(tag, config)};
        setLayers(layers);
    }

    protected Layer createExchangeCleanupLayer(Configuration config) {
        return new ExchangeCleanupLayer(config);
    }

    protected Layer createObserveLayer(Configuration config) {
        return new ObserveLayer(config);
    }

    protected Layer createBlockwiseLayer(String tag, Configuration config) {
        return new BlockwiseLayer(tag, false, config);
    }

    protected Layer createReliabilityLayer(String tag, Configuration config) {
        return CongestionControlLayer.newImplementation(tag, config);
    }
}
