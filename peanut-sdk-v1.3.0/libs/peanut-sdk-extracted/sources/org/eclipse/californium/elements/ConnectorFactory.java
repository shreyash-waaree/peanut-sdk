package org.eclipse.californium.elements;

import java.net.InetSocketAddress;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-element-connector.jar:org/eclipse/californium/elements/ConnectorFactory.class */
public interface ConnectorFactory {
    Connector newConnector(InetSocketAddress inetSocketAddress);
}
