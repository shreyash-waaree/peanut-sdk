package org.eclipse.californium.core.network;

import org.eclipse.californium.core.network.stack.CoapStack;
import org.eclipse.californium.elements.config.Configuration;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-californium-core.jar:org/eclipse/californium/core/network/CoapStackFactory.class */
public interface CoapStackFactory {
    CoapStack createCoapStack(String str, String str2, Configuration configuration, Outbox outbox, Object obj);
}
