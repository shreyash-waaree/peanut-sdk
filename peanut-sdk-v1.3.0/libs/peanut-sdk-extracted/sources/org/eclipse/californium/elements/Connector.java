package org.eclipse.californium.elements;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetSocketAddress;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-element-connector.jar:org/eclipse/californium/elements/Connector.class */
public interface Connector {
    void start() throws IOException;

    void stop();

    void destroy();

    void send(RawData rawData);

    void setRawDataReceiver(RawDataChannel rawDataChannel);

    void setEndpointContextMatcher(EndpointContextMatcher endpointContextMatcher);

    InetSocketAddress getAddress();

    String getProtocol();

    boolean isRunning();

    void processDatagram(DatagramPacket datagramPacket);
}
