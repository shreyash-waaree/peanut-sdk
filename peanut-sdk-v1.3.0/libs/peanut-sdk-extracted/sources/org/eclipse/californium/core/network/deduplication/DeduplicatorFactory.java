package org.eclipse.californium.core.network.deduplication;

import org.eclipse.californium.core.config.CoapConfig;
import org.eclipse.californium.elements.config.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-californium-core.jar:org/eclipse/californium/core/network/deduplication/DeduplicatorFactory.class */
public class DeduplicatorFactory {
    private static final Logger LOGGER = LoggerFactory.getLogger(DeduplicatorFactory.class);
    private static DeduplicatorFactory factory;

    public static synchronized DeduplicatorFactory getDeduplicatorFactory() {
        if (factory == null) {
            factory = new DeduplicatorFactory();
        }
        return factory;
    }

    public static synchronized void setDeduplicatorFactory(DeduplicatorFactory factory2) {
        factory = factory2;
    }

    public Deduplicator createDeduplicator(Configuration config) {
        String type = (String) config.get(CoapConfig.DEDUPLICATOR);
        switch (type) {
            case "PEERS_MARK_AND_SWEEP":
                return new SweepPerPeerDeduplicator(config);
            case "MARK_AND_SWEEP":
                return new SweepDeduplicator(config);
            case "CROP_ROTATION":
                return new CropRotation(config);
            case "NO_DEDUPLICATOR":
                return new NoDeduplicator();
            default:
                LOGGER.warn("configuration contains unsupported deduplicator type, duplicate detection will be turned off");
                return new NoDeduplicator();
        }
    }
}
