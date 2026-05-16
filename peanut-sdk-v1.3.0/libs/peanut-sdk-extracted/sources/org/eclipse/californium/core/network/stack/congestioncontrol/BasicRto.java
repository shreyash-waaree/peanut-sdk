package org.eclipse.californium.core.network.stack.congestioncontrol;

import java.net.InetSocketAddress;
import org.eclipse.californium.core.network.stack.CongestionControlLayer;
import org.eclipse.californium.core.network.stack.RemoteEndpoint;
import org.eclipse.californium.elements.config.Configuration;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-californium-core.jar:org/eclipse/californium/core/network/stack/congestioncontrol/BasicRto.class */
public class BasicRto extends CongestionControlLayer {
    public BasicRto(String tag, Configuration config) {
        super(tag, config);
    }

    @Override // org.eclipse.californium.core.network.stack.CongestionControlLayer
    protected RemoteEndpoint createRemoteEndpoint(InetSocketAddress remoteSocketAddress) {
        return new RemoteEndpoint(remoteSocketAddress, this.defaultReliabilityLayerParameters.getAckTimeout(), this.defaultReliabilityLayerParameters.getNstart(), false) { // from class: org.eclipse.californium.core.network.stack.congestioncontrol.BasicRto.1
            @Override // org.eclipse.californium.core.network.stack.RemoteEndpoint
            public void processRttMeasurement(RemoteEndpoint.RtoType rtoType, long measuredRTT) {
                updateRTO(measuredRTT + (measuredRTT / 2));
            }
        };
    }
}
