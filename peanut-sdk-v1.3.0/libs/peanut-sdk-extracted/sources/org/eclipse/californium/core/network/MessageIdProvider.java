package org.eclipse.californium.core.network;

import java.net.InetSocketAddress;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-californium-core.jar:org/eclipse/californium/core/network/MessageIdProvider.class */
public interface MessageIdProvider {
    int getNextMessageId(InetSocketAddress inetSocketAddress);
}
