package org.eclipse.californium.core.server.resources;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.OptionSet;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.californium.core.network.Exchange;
import org.eclipse.californium.elements.DtlsEndpointContext;
import org.eclipse.californium.elements.EndpointContext;
import org.eclipse.californium.elements.MapBasedEndpointContext;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-californium-core.jar:org/eclipse/californium/core/server/resources/CoapExchange.class */
public class CoapExchange {
    private final Exchange exchange;
    private final Map<String, String> queryParameters;
    private final CoapResource resource;
    private String locationPath = null;
    private String locationQuery = null;
    private String handshakeMode = null;
    private long maxAge = 60;
    private byte[] eTag = null;

    public CoapExchange(Exchange exchange, CoapResource resource) {
        if (exchange == null) {
            throw new NullPointerException("exchange must not be null");
        }
        if (resource == null) {
            throw new NullPointerException("resource must not be null");
        }
        this.exchange = exchange;
        this.resource = resource;
        if (getRequestOptions().getURIQueryCount() > 0) {
            this.queryParameters = new HashMap();
            for (String param : getRequestOptions().getUriQuery()) {
                addParameter(param);
            }
            return;
        }
        this.queryParameters = null;
    }

    private void addParameter(String param) {
        int idx = param.indexOf("=");
        if (idx > 0) {
            this.queryParameters.put(param.substring(0, idx), param.substring(idx + 1));
        } else {
            this.queryParameters.put(param, Boolean.TRUE.toString());
        }
    }

    public InetSocketAddress getSourceSocketAddress() {
        return this.exchange.getRequest().getSourceContext().getPeerAddress();
    }

    public InetAddress getSourceAddress() {
        return this.exchange.getRequest().getSourceContext().getPeerAddress().getAddress();
    }

    public int getSourcePort() {
        return this.exchange.getRequest().getSourceContext().getPeerAddress().getPort();
    }

    public boolean isMulticastRequest() {
        return this.exchange.getRequest().isMulticast();
    }

    public CoAP.Code getRequestCode() {
        return this.exchange.getRequest().getCode();
    }

    public OptionSet getRequestOptions() {
        return this.exchange.getRequest().getOptions();
    }

    public String getQueryParameter(String name) {
        if (this.queryParameters != null) {
            return this.queryParameters.get(name);
        }
        return null;
    }

    public byte[] getRequestPayload() {
        return this.exchange.getRequest().getPayload();
    }

    public int getRequestPayloadSize() {
        return this.exchange.getRequest().getPayloadSize();
    }

    public String getRequestText() {
        return this.exchange.getRequest().getPayloadString();
    }

    public void accept() {
        this.exchange.sendAccept(applyHandshakeMode());
    }

    public void reject() {
        this.exchange.sendReject(applyHandshakeMode());
    }

    public void setLocationPath(String path) {
        this.locationPath = path;
    }

    public void setLocationQuery(String query) {
        this.locationQuery = query;
    }

    public void setHandshakeMode(String handshakeMode) {
        if (!handshakeMode.equals(DtlsEndpointContext.HANDSHAKE_MODE_AUTO) && !handshakeMode.equals(DtlsEndpointContext.HANDSHAKE_MODE_NONE)) {
            throw new IllegalArgumentException("handshake mode must be either \"auto\" or \"none\"!");
        }
        this.handshakeMode = handshakeMode;
    }

    public void setMaxAge(long age) {
        this.maxAge = age;
    }

    public void setETag(byte[] tag) {
        this.eTag = tag;
    }

    public void respondClientOverload(int seconds) {
        setMaxAge(seconds);
        respond(CoAP.ResponseCode.TOO_MANY_REQUESTS);
    }

    public void respondOverload(int seconds) {
        setMaxAge(seconds);
        respond(CoAP.ResponseCode.SERVICE_UNAVAILABLE);
    }

    public void respond(CoAP.ResponseCode code) {
        respond(new Response(code));
    }

    public void respond(String payload) {
        respond(CoAP.ResponseCode.CONTENT, payload);
    }

    public void respond(CoAP.ResponseCode code, String payload) {
        Response response = new Response(code);
        response.setPayload(payload);
        response.getOptions().setContentFormat(0);
        respond(response);
    }

    public void respond(CoAP.ResponseCode code, byte[] payload) {
        Response response = new Response(code);
        response.setPayload(payload);
        respond(response);
    }

    public void respond(CoAP.ResponseCode code, byte[] payload, int contentFormat) {
        Response response = new Response(code);
        response.setPayload(payload);
        response.getOptions().setContentFormat(contentFormat);
        respond(response);
    }

    public void respond(CoAP.ResponseCode code, String payload, int contentFormat) {
        Response response = new Response(code);
        response.setPayload(payload);
        response.getOptions().setContentFormat(contentFormat);
        respond(response);
    }

    public void respond(Response response) {
        if (response == null) {
            throw new NullPointerException();
        }
        if (this.locationPath != null) {
            response.getOptions().setLocationPath(this.locationPath);
        }
        if (this.locationQuery != null) {
            response.getOptions().setLocationQuery(this.locationQuery);
        }
        if (this.maxAge != 60) {
            response.getOptions().setMaxAge(this.maxAge);
        }
        if (this.eTag != null) {
            response.getOptions().clearETags();
            response.getOptions().addETag(this.eTag);
        }
        this.resource.checkObserveRelation(this.exchange, response);
        if (response.getDestinationContext() == null) {
            response.setDestinationContext(applyHandshakeMode());
        }
        this.exchange.sendResponse(response);
    }

    private EndpointContext applyHandshakeMode() {
        EndpointContext context = this.exchange.getCurrentRequest().getSourceContext();
        if (this.handshakeMode != null && context.get(DtlsEndpointContext.KEY_HANDSHAKE_MODE) == null) {
            MapBasedEndpointContext.Attributes attributes = new MapBasedEndpointContext.Attributes();
            attributes.add(DtlsEndpointContext.KEY_HANDSHAKE_MODE, this.handshakeMode);
            context = MapBasedEndpointContext.addEntries(context, attributes);
        }
        return context;
    }

    public Exchange advanced() {
        return this.exchange;
    }
}
