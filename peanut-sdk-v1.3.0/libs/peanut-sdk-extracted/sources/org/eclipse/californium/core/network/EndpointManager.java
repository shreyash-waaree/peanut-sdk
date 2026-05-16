package org.eclipse.californium.core.network;

import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.californium.core.network.CoapEndpoint;
import org.eclipse.californium.core.server.MessageDeliverer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-californium-core.jar:org/eclipse/californium/core/network/EndpointManager.class */
public class EndpointManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(EndpointManager.class);
    private static final EndpointManager manager = new EndpointManager();
    private final Map<String, Endpoint> endpoints = new ConcurrentHashMap();

    public static EndpointManager getEndpointManager() {
        return manager;
    }

    public synchronized Endpoint getDefaultEndpoint(String uriScheme) {
        if (null == uriScheme) {
            uriScheme = CoAP.COAP_URI_SCHEME;
        }
        if (!CoAP.isSupportedScheme(uriScheme)) {
            throw new IllegalArgumentException("URI scheme " + uriScheme + " not supported!");
        }
        String uriScheme2 = uriScheme.toLowerCase();
        Endpoint endpoint = this.endpoints.get(uriScheme2);
        if (null == endpoint) {
            if (CoAP.COAP_SECURE_URI_SCHEME.equalsIgnoreCase(uriScheme2)) {
                throw new IllegalStateException("URI scheme " + uriScheme2 + " requires a previous set connector!");
            }
            if (CoAP.COAP_TCP_URI_SCHEME.equalsIgnoreCase(uriScheme2)) {
                throw new IllegalStateException("URI scheme " + uriScheme2 + " requires a previous set connector!");
            }
            if (CoAP.COAP_SECURE_TCP_URI_SCHEME.equalsIgnoreCase(uriScheme2)) {
                throw new IllegalStateException("URI scheme " + uriScheme2 + " requires a previous set connector!");
            }
            endpoint = new CoapEndpoint.Builder().build();
            try {
                endpoint.start();
                LOGGER.info("created implicit endpoint {} for {}", endpoint.getUri(), uriScheme2);
            } catch (IOException e) {
                LOGGER.error("could not create {} endpoint", uriScheme2, e);
            }
            this.endpoints.put(uriScheme2, endpoint);
        }
        return endpoint;
    }

    public synchronized void setDefaultEndpoint(Endpoint newEndpoint) {
        if (null == newEndpoint) {
            throw new NullPointerException("endpoint required!");
        }
        URI uri = newEndpoint.getUri();
        if (null == uri) {
            throw new IllegalArgumentException("Endpoint protocol not supported!");
        }
        String uriScheme = uri.getScheme();
        if (!CoAP.isSupportedScheme(uriScheme)) {
            throw new IllegalArgumentException("URI scheme " + uriScheme + " not supported!");
        }
        Endpoint oldEndpoint = this.endpoints.put(uriScheme, newEndpoint);
        if (null != oldEndpoint) {
            oldEndpoint.destroy();
        }
        if (!newEndpoint.isStarted()) {
            try {
                newEndpoint.start();
            } catch (IOException e) {
                LOGGER.error("could not start new {} endpoint", uriScheme, e);
            }
        }
    }

    public Endpoint getDefaultEndpoint() {
        return getDefaultEndpoint(CoAP.COAP_URI_SCHEME);
    }

    public static void clear() {
        EndpointManager it = getEndpointManager();
        for (Endpoint endpoint : it.endpoints.values()) {
            endpoint.clear();
        }
    }

    public static void reset() {
        EndpointManager it = getEndpointManager();
        for (Endpoint endpoint : it.endpoints.values()) {
            endpoint.destroy();
        }
        it.endpoints.clear();
    }

    /* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-californium-core.jar:org/eclipse/californium/core/network/EndpointManager$ClientMessageDeliverer.class */
    public static class ClientMessageDeliverer implements MessageDeliverer {
        @Override // org.eclipse.californium.core.server.MessageDeliverer
        public void deliverRequest(Exchange exchange) {
            EndpointManager.LOGGER.error("Default endpoint without CoapServer has received a request.");
            exchange.sendReject();
        }

        @Override // org.eclipse.californium.core.server.MessageDeliverer
        public void deliverResponse(Exchange exchange, Response response) {
            if (exchange == null) {
                throw new NullPointerException("no CoAP exchange!");
            }
            if (exchange.getRequest() == null) {
                throw new NullPointerException("no CoAP request!");
            }
            if (response == null) {
                throw new NullPointerException("no CoAP response!");
            }
            exchange.getRequest().setResponse(response);
        }
    }
}
