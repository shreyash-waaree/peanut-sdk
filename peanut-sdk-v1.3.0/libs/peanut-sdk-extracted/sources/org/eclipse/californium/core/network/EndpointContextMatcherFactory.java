package org.eclipse.californium.core.network;

import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.config.CoapConfig;
import org.eclipse.californium.elements.Connector;
import org.eclipse.californium.elements.EndpointContextMatcher;
import org.eclipse.californium.elements.PrincipalEndpointContextMatcher;
import org.eclipse.californium.elements.RelaxedDtlsEndpointContextMatcher;
import org.eclipse.californium.elements.StrictDtlsEndpointContextMatcher;
import org.eclipse.californium.elements.TcpEndpointContextMatcher;
import org.eclipse.californium.elements.TlsEndpointContextMatcher;
import org.eclipse.californium.elements.UdpEndpointContextMatcher;
import org.eclipse.californium.elements.config.Configuration;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-californium-core.jar:org/eclipse/californium/core/network/EndpointContextMatcherFactory.class */
public class EndpointContextMatcherFactory {
    public static EndpointContextMatcher create(Connector connector, Configuration config) {
        String protocol = null;
        if (null != connector) {
            protocol = connector.getProtocol();
            if (CoAP.PROTOCOL_TCP.equalsIgnoreCase(protocol)) {
                return new TcpEndpointContextMatcher();
            }
            if (CoAP.PROTOCOL_TLS.equalsIgnoreCase(protocol)) {
                return new TlsEndpointContextMatcher();
            }
        }
        CoapConfig.MatcherMode mode = (CoapConfig.MatcherMode) config.get(CoapConfig.RESPONSE_MATCHING);
        switch (mode) {
            case RELAXED:
                if (CoAP.PROTOCOL_UDP.equalsIgnoreCase(protocol)) {
                    return new UdpEndpointContextMatcher(false);
                }
                return new RelaxedDtlsEndpointContextMatcher();
            case PRINCIPAL:
                if (CoAP.PROTOCOL_UDP.equalsIgnoreCase(protocol)) {
                    return new UdpEndpointContextMatcher(true);
                }
                return new PrincipalEndpointContextMatcher();
            case PRINCIPAL_IDENTITY:
                if (CoAP.PROTOCOL_UDP.equalsIgnoreCase(protocol)) {
                    return new UdpEndpointContextMatcher(true);
                }
                return new PrincipalEndpointContextMatcher(true);
            case STRICT:
            default:
                if (CoAP.PROTOCOL_UDP.equalsIgnoreCase(protocol)) {
                    return new UdpEndpointContextMatcher(true);
                }
                return new StrictDtlsEndpointContextMatcher();
        }
    }
}
