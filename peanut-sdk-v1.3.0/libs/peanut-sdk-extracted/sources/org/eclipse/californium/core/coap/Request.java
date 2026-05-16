package org.eclipse.californium.core.coap;

import com.keenon.common.constant.PeanutConstants;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.network.Endpoint;
import org.eclipse.californium.core.network.EndpointManager;
import org.eclipse.californium.elements.AddressEndpointContext;
import org.eclipse.californium.elements.EndpointContext;
import org.eclipse.californium.elements.util.ClockUtil;
import org.eclipse.californium.elements.util.NetworkInterfacesUtil;
import org.eclipse.californium.elements.util.StringUtil;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-californium-core.jar:org/eclipse/californium/core/coap/Request.class */
public class Request extends Message {
    private final CoAP.Code code;
    private boolean multicast;
    private Response response;
    private boolean ready;
    private String scheme;
    private boolean uri;
    private boolean proxyUri;
    private boolean proxyScheme;
    private Map<String, String> userContext;
    private volatile Throwable responseHandlingError;

    public Request(CoAP.Code code) {
        this(code, CoAP.Type.CON);
    }

    public Request(CoAP.Code code, CoAP.Type type) {
        super(type);
        this.code = code;
    }

    public CoAP.Code getCode() {
        return this.code;
    }

    @Override // org.eclipse.californium.core.coap.Message
    public int getRawCode() {
        if (this.code == null) {
            return 0;
        }
        return this.code.value;
    }

    public String getScheme() {
        return this.scheme == null ? CoAP.COAP_URI_SCHEME : this.scheme;
    }

    public void setScheme(String scheme) {
        this.scheme = scheme;
    }

    public boolean isMulticast() {
        return this.multicast;
    }

    @Override // org.eclipse.californium.core.coap.Message
    public boolean isIntendedPayload() {
        return (this.code == CoAP.Code.GET || this.code == CoAP.Code.DELETE) ? false : true;
    }

    @Override // org.eclipse.californium.core.coap.Message
    public Request setPayload(String payload) {
        super.setPayload(payload);
        return this;
    }

    @Override // org.eclipse.californium.core.coap.Message
    public Request setPayload(byte[] payload) {
        super.setPayload(payload);
        return this;
    }

    @Override // org.eclipse.californium.core.coap.Message
    public void assertPayloadMatchsBlocksize() {
        BlockOption block1 = getOptions().getBlock1();
        if (block1 != null) {
            block1.assertPayloadSize(getPayloadSize());
        }
    }

    public Request setProxyUri(String proxyUri) {
        if (this.uri) {
            throw new IllegalStateException("CoAP URI is set!");
        }
        if (this.proxyScheme) {
            throw new IllegalStateException("Proxy Scheme is set!");
        }
        getOptions().setProxyUri(proxyUri);
        this.proxyUri = true;
        return this;
    }

    public boolean hasProxyURI() {
        return this.proxyUri;
    }

    public Request setProxyScheme(String proxyScheme) {
        if (this.proxyUri) {
            throw new IllegalStateException("Proxy URI is set!");
        }
        getOptions().setProxyScheme(proxyScheme);
        this.proxyScheme = true;
        return this;
    }

    public Request setURI(String uri) {
        if (uri == null) {
            throw new NullPointerException("URI must not be null");
        }
        try {
            String coapUri = uri;
            if (!uri.contains(CoAP.URI_SCHEME_SEPARATOR)) {
                coapUri = PeanutConstants.COAP_PROTOCOL + uri;
                LOGGER.warn("update your code to supply an RFC 7252 compliant URI including a scheme");
            }
            return setURI(new URI(coapUri));
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("invalid uri: " + uri, e);
        }
    }

    public Request setURI(URI uri) {
        InetSocketAddress destinationAdress;
        checkURI(uri);
        String host = uri.getHost() == null ? "localhost" : uri.getHost();
        String uriScheme = uri.getScheme();
        boolean literalIp = StringUtil.isLiteralIpAddress(host);
        try {
            EndpointContext destinationContext = getDestinationContext();
            if (destinationContext == null) {
                int port = uri.getPort();
                InetAddress destAddress = InetAddress.getByName(host);
                String destHost = literalIp ? null : host;
                int destPort = port <= 0 ? CoAP.getDefaultPort(uriScheme) : port;
                destinationAdress = new InetSocketAddress(destAddress, destPort);
                destinationContext = new AddressEndpointContext(destinationAdress, destHost, null);
            } else {
                destinationAdress = destinationContext.getPeerAddress();
            }
            setOptionsInternal(uri, destinationAdress, literalIp);
            setDestinationContext(destinationContext);
            this.scheme = uriScheme.toLowerCase();
            this.uri = true;
            return this;
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException("cannot resolve host name: " + host);
        }
    }

    public Request setOptions(URI uri) {
        checkURI(uri);
        EndpointContext destinationContext = getDestinationContext();
        if (destinationContext == null) {
            throw new IllegalStateException("destination must be set ahead!");
        }
        setOptionsInternal(uri, destinationContext.getPeerAddress(), StringUtil.isLiteralIpAddress(uri.getHost()));
        this.uri = true;
        return this;
    }

    private void checkURI(URI uri) {
        if (this.proxyUri) {
            throw new IllegalStateException("Proxy URI is set!");
        }
        if (uri == null) {
            throw new NullPointerException("URI must not be null");
        }
        if (!CoAP.isSupportedScheme(uri.getScheme())) {
            throw new IllegalArgumentException("URI scheme '" + uri.getScheme() + "' is not supported!");
        }
        if (uri.getFragment() != null) {
            throw new IllegalArgumentException("URI must not contain a fragment '" + uri.getFragment() + "'!");
        }
        if (uri.getSchemeSpecificPart() != null && uri.getHost() == null) {
            throw new IllegalArgumentException("URI expected host '" + uri.getSchemeSpecificPart() + "' is invalid!");
        }
    }

    private void setOptionsInternal(URI uri, InetSocketAddress destination, boolean literalIp) {
        if (destination == null) {
            throw new NullPointerException("destination address must not be null!");
        }
        OptionSet options = getOptions();
        boolean explicitUriOption = options.hasExplicitUriOptions();
        String host = uri.getHost();
        if (host != null) {
            if (literalIp) {
                try {
                    InetAddress hostAddress = InetAddress.getByName(host);
                    InetAddress destinationAddress = destination.getAddress();
                    if (hostAddress.equals(destinationAddress)) {
                        host = null;
                    }
                } catch (UnknownHostException e) {
                    LOGGER.warn("could not parse IP address of URI despite successful IP address pattern matching");
                }
            } else if (!StringUtil.isValidHostName(host)) {
                throw new IllegalArgumentException("URI's hostname '" + host + "' is invalid!'");
            }
            if (host != null) {
                options.setUriHost(host.toLowerCase());
            }
        }
        if (host == null) {
            options.removeUriHost();
        }
        int port = uri.getPort();
        if (port <= 0) {
            port = CoAP.getDefaultPort(uri.getScheme());
        }
        if (port == destination.getPort()) {
            port = -1;
        }
        if (0 < port) {
            options.setUriPort(port);
        } else {
            options.removeUriPort();
        }
        String path = uri.getPath();
        if (path != null && path.length() > 1) {
            options.setUriPath(path);
        } else if (!explicitUriOption) {
            options.clearUriPath();
        }
        String query = uri.getQuery();
        if (query != null) {
            options.setUriQuery(query);
        } else if (!explicitUriOption) {
            options.clearUriQuery();
        }
        if (!explicitUriOption) {
            options.resetExplicitUriOptions();
        }
    }

    public void setUriIsApplied() {
        this.uri = true;
    }

    public boolean hasURI() {
        return this.uri;
    }

    public String getURI() {
        OptionSet options = getOptions();
        String host = options.getUriHost();
        Integer port = options.getUriPort();
        if (host == null) {
            if (getDestinationContext() != null) {
                host = getDestinationContext().getPeerAddress().getAddress().getHostAddress();
            } else {
                host = "localhost";
            }
        }
        if (port == null) {
            if (getDestinationContext() != null) {
                port = Integer.valueOf(getDestinationContext().getPeerAddress().getPort());
            } else {
                port = -1;
            }
        }
        if (port.intValue() > 0) {
            if (CoAP.isSupportedScheme(getScheme()) && CoAP.getDefaultPort(getScheme()) == port.intValue()) {
                port = -1;
            }
        } else {
            port = -1;
        }
        String path = "/" + options.getUriPathString();
        String query = options.getURIQueryCount() > 0 ? options.getUriQueryString() : null;
        try {
            URI uri = new URI(getScheme(), null, host, port.intValue(), path, query, null);
            return uri.toASCIIString();
        } catch (URISyntaxException e) {
            throw new IllegalStateException("cannot create URI from request", e);
        }
    }

    @Override // org.eclipse.californium.core.coap.Message
    public boolean hasBlock(BlockOption block) {
        return hasBlock(block, getOptions().getBlock1());
    }

    @Override // org.eclipse.californium.core.coap.Message
    public Request setDestinationContext(EndpointContext peerContext) {
        super.setRequestDestinationContext(peerContext);
        this.multicast = (peerContext == null || peerContext.getPeerAddress().isUnresolved() || !NetworkInterfacesUtil.isMultiAddress(peerContext.getPeerAddress().getAddress())) ? false : true;
        return this;
    }

    public void setLocalAddress(InetSocketAddress local, boolean multicast) {
        super.setLocalAddress(local);
        this.multicast = multicast;
    }

    public Request send() {
        send(EndpointManager.getEndpointManager().getDefaultEndpoint(getScheme()));
        return this;
    }

    public Request send(Endpoint endpoint) {
        validateBeforeSending();
        endpoint.sendRequest(this);
        return this;
    }

    private void validateBeforeSending() {
        if (getDestinationContext() == null) {
            throw new IllegalStateException("Destination is null");
        }
        if (getDestinationContext().getPeerAddress().getAddress() == null) {
            throw new IllegalStateException("Destination address is null");
        }
        if (getDestinationContext().getPeerAddress().getPort() == 0) {
            throw new IllegalStateException("Destination port is 0");
        }
    }

    public final Request setObserve() {
        if (!CoAP.isObservable(this.code)) {
            throw new IllegalStateException("observe option can only be set on a GET or FETCH request");
        }
        getOptions().setObserve(0);
        return this;
    }

    public final boolean isObserve() {
        return isObserveOption(0);
    }

    public final Request setObserveCancel() {
        if (!CoAP.isObservable(this.code)) {
            throw new IllegalStateException("observe option can only be set on a GET or FETCH request");
        }
        getOptions().setObserve(1);
        return this;
    }

    public final boolean isObserveCancel() {
        return isObserveOption(1);
    }

    private final boolean isObserveOption(int observe) {
        Integer optionObserver = getOptions().getObserve();
        return optionObserver != null && optionObserver.intValue() == observe;
    }

    public synchronized Response getResponse() {
        return this.response;
    }

    public void setResponse(Response response) {
        if (response == null) {
            throw new NullPointerException("no CoAP response!");
        }
        synchronized (this) {
            this.response = response;
            notifyAll();
        }
        for (MessageObserver handler : getMessageObservers()) {
            handler.onResponse(response);
        }
    }

    public Response waitForResponse() throws InterruptedException {
        return waitForResponse(0L);
    }

    public Response waitForResponse(long timeout) throws InterruptedException {
        Response r;
        long expiresNano = ClockUtil.nanoRealtime() + TimeUnit.MILLISECONDS.toNanos(timeout);
        long leftTimeout = timeout;
        synchronized (this) {
            while (!this.ready && this.response == null) {
                wait(leftTimeout);
                if (timeout > 0) {
                    long leftNanos = expiresNano - ClockUtil.nanoRealtime();
                    if (leftNanos <= 0) {
                        break;
                    }
                    leftTimeout = TimeUnit.NANOSECONDS.toMillis(leftNanos) + 1;
                }
            }
            r = this.response;
            this.response = null;
        }
        return r;
    }

    @Override // org.eclipse.californium.core.coap.Message
    public void setTimedOut(boolean timedOut) {
        super.setTimedOut(timedOut);
        if (timedOut) {
            synchronized (this) {
                this.ready = true;
                notifyAll();
            }
        }
    }

    @Override // org.eclipse.californium.core.coap.Message
    public void setCanceled(boolean canceled) {
        super.setCanceled(canceled);
        if (canceled) {
            synchronized (this) {
                this.ready = true;
                notifyAll();
            }
        }
    }

    @Override // org.eclipse.californium.core.coap.Message
    public void setRejected(boolean rejected) {
        super.setRejected(rejected);
        if (rejected) {
            synchronized (this) {
                this.ready = true;
                notifyAll();
            }
        }
    }

    @Override // org.eclipse.californium.core.coap.Message
    public void setSendError(Throwable sendError) {
        super.setSendError(sendError);
        if (sendError != null) {
            synchronized (this) {
                this.ready = true;
                notifyAll();
            }
        }
    }

    public Throwable getOnResponseError() {
        return this.responseHandlingError;
    }

    public void setOnResponseError(Throwable cause) {
        this.responseHandlingError = cause;
        if (this.responseHandlingError != null) {
            for (MessageObserver handler : getMessageObservers()) {
                handler.onResponseHandlingError(this.responseHandlingError);
            }
            synchronized (this) {
                this.ready = true;
                notifyAll();
            }
        }
    }

    public Map<String, String> getUserContext() {
        return this.userContext;
    }

    public Request setUserContext(Map<String, String> userContext) {
        if (userContext == null || userContext.isEmpty()) {
            this.userContext = Collections.emptyMap();
        } else {
            this.userContext = Collections.unmodifiableMap(new HashMap(userContext));
        }
        return this;
    }

    public String toString() {
        CoAP.Code code = getCode();
        return toTracingString(code == null ? "PING" : code.toString());
    }

    public static Request newGet() {
        return new Request(CoAP.Code.GET);
    }

    public static Request newFetch() {
        return new Request(CoAP.Code.FETCH);
    }

    public static Request newPost() {
        return new Request(CoAP.Code.POST);
    }

    public static Request newPut() {
        return new Request(CoAP.Code.PUT);
    }

    public static Request newPatch() {
        return new Request(CoAP.Code.PATCH);
    }

    public static Request newIPatch() {
        return new Request(CoAP.Code.IPATCH);
    }

    public static Request newDelete() {
        return new Request(CoAP.Code.DELETE);
    }

    public static Request newPing() {
        return new Request(null);
    }
}
