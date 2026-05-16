package org.eclipse.californium.core.network;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-californium-core.jar:org/eclipse/californium/core/network/MessageIdTracker.class */
public interface MessageIdTracker {
    public static final int TOTAL_NO_OF_MIDS = 65536;

    int getNextMessageId();
}
