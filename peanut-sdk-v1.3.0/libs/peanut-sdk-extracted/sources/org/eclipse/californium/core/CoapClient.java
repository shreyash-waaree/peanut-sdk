package org.eclipse.californium.core;

import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.eclipse.californium.core.coap.BlockOption;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.LinkFormat;
import org.eclipse.californium.core.coap.MessageObserver;
import org.eclipse.californium.core.coap.MessageObserverAdapter;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.californium.core.coap.Token;
import org.eclipse.californium.core.config.CoapConfig;
import org.eclipse.californium.core.network.Endpoint;
import org.eclipse.californium.core.network.EndpointManager;
import org.eclipse.californium.core.observe.NotificationListener;
import org.eclipse.californium.elements.EndpointContext;
import org.eclipse.californium.elements.exception.ConnectorException;
import org.eclipse.californium.elements.util.ExecutorsUtil;
import org.eclipse.californium.elements.util.NamedThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-californium-core.jar:org/eclipse/californium/core/CoapClient.class */
public class CoapClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(CoapClient.class);
    private Long timeout;
    private String uri;
    private boolean useProxy;
    private String proxyScheme;
    private final AtomicReference<EndpointContext> destinationContext;
    private CoAP.Type type;
    private int blockwise;
    private ExecutorService executor;
    private volatile ScheduledThreadPoolExecutor secondaryExecutor;
    private volatile boolean detachExecutor;
    private Endpoint endpoint;
    private final AtomicInteger receivableNum;
    private final AtomicInteger dropNum;
    private final Map<String, Integer> dropMap;
    private final Map<String, Integer> receivableMap;

    public CoapClient() {
        this("");
    }

    public CoapClient(String uri) {
        this.destinationContext = new AtomicReference<>();
        this.type = CoAP.Type.CON;
        this.blockwise = 0;
        this.receivableNum = new AtomicInteger();
        this.dropNum = new AtomicInteger();
        this.dropMap = new ConcurrentHashMap();
        this.receivableMap = new ConcurrentHashMap();
        this.uri = uri;
    }

    public CoapClient(URI uri) {
        this(uri.toString());
    }

    public CoapClient(String scheme, String host, int port, String... path) {
        this.destinationContext = new AtomicReference<>();
        this.type = CoAP.Type.CON;
        this.blockwise = 0;
        this.receivableNum = new AtomicInteger();
        this.dropNum = new AtomicInteger();
        this.dropMap = new ConcurrentHashMap();
        this.receivableMap = new ConcurrentHashMap();
        StringBuilder builder = new StringBuilder().append(scheme).append(CoAP.URI_SCHEME_SEPARATOR).append(host).append(":").append(port);
        for (String element : path) {
            builder.append("/").append(element);
        }
        this.uri = builder.toString();
    }

    public Long getTimeout() {
        return this.timeout;
    }

    public CoapClient setTimeout(Long timeout) {
        this.timeout = timeout;
        return this;
    }

    public String getURI() {
        return this.uri;
    }

    /* JADX WARN: Removed duplicated region for block: B:16:0x0057  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct code enable 'Show inconsistent code' option in preferences
    */
    public org.eclipse.californium.core.CoapClient setURI(java.lang.String r5) {
        /*
            r4 = this;
            r0 = r4
            boolean r0 = r0.useProxy
            if (r0 != 0) goto L6d
            r0 = r4
            java.lang.String r0 = r0.proxyScheme
            if (r0 != 0) goto L6d
            r0 = r4
            java.lang.String r0 = r0.uri
            r1 = r5
            boolean r0 = java.util.Objects.equals(r0, r1)
            if (r0 != 0) goto L6d
            r0 = 1
            r6 = r0
            r0 = r4
            java.lang.String r0 = r0.uri
            if (r0 == 0) goto L61
            r0 = r5
            if (r0 == 0) goto L61
            java.net.URI r0 = new java.net.URI     // Catch: java.net.URISyntaxException -> L60
            r1 = r0
            r2 = r4
            java.lang.String r2 = r2.uri     // Catch: java.net.URISyntaxException -> L60
            r1.<init>(r2)     // Catch: java.net.URISyntaxException -> L60
            r7 = r0
            java.net.URI r0 = new java.net.URI     // Catch: java.net.URISyntaxException -> L60
            r1 = r0
            r2 = r5
            r1.<init>(r2)     // Catch: java.net.URISyntaxException -> L60
            r8 = r0
            r0 = r7
            int r0 = r0.getPort()     // Catch: java.net.URISyntaxException -> L60
            r1 = r8
            int r1 = r1.getPort()     // Catch: java.net.URISyntaxException -> L60
            if (r0 != r1) goto L57
            r0 = r7
            java.lang.String r0 = r0.getHost()     // Catch: java.net.URISyntaxException -> L60
            r1 = r8
            java.lang.String r1 = r1.getHost()     // Catch: java.net.URISyntaxException -> L60
            boolean r0 = java.util.Objects.equals(r0, r1)     // Catch: java.net.URISyntaxException -> L60
            if (r0 != 0) goto L5b
        L57:
            r0 = 1
            goto L5c
        L5b:
            r0 = 0
        L5c:
            r6 = r0
            goto L61
        L60:
            r7 = move-exception
        L61:
            r0 = r6
            if (r0 == 0) goto L6d
            r0 = r4
            java.util.concurrent.atomic.AtomicReference<org.eclipse.californium.elements.EndpointContext> r0 = r0.destinationContext
            r1 = 0
            r0.set(r1)
        L6d:
            r0 = r4
            r1 = r5
            r0.uri = r1
            r0 = r4
            return r0
        */
        throw new UnsupportedOperationException("Method not decompiled: org.eclipse.californium.core.CoapClient.setURI(java.lang.String):org.eclipse.californium.core.CoapClient");
    }

    public EndpointContext getDestinationContext() {
        return this.destinationContext.get();
    }

    public CoapClient setDestinationContext(EndpointContext peerContext) {
        this.destinationContext.set(peerContext);
        return this;
    }

    public boolean useProxy() {
        return this.useProxy;
    }

    public CoapClient enableProxy(boolean enable) {
        this.useProxy = enable;
        return this;
    }

    public String getProxyScheme() {
        return this.proxyScheme;
    }

    public CoapClient setProxyScheme(String proxyScheme) {
        this.proxyScheme = proxyScheme;
        return this;
    }

    public CoapClient useExecutor() {
        boolean failed = true;
        ExecutorService executor = ExecutorsUtil.newFixedThreadPool(1, new NamedThreadFactory("CoapClient(main)#"));
        ScheduledThreadPoolExecutor secondaryExecutor = new ScheduledThreadPoolExecutor(1, new NamedThreadFactory("CoapClient(secondary)#"));
        synchronized (this) {
            if (this.executor == null && this.secondaryExecutor == null) {
                this.executor = executor;
                this.secondaryExecutor = secondaryExecutor;
                this.detachExecutor = false;
                failed = false;
            }
        }
        if (failed) {
            executor.shutdownNow();
            secondaryExecutor.shutdown();
            throw new IllegalStateException("Executor already set or used!");
        }
        executor.execute(new Runnable() { // from class: org.eclipse.californium.core.CoapClient.1
            @Override // java.lang.Runnable
            public void run() {
                CoapClient.LOGGER.info("using a SingleThreadExecutor for the CoapClient");
            }
        });
        return this;
    }

    public CoapClient setExecutors(ExecutorService executor, ScheduledThreadPoolExecutor secondaryExecutor, boolean detach) {
        if (executor == null || secondaryExecutor == null) {
            throw new NullPointerException("Executors must not be null!");
        }
        boolean failed = true;
        synchronized (this) {
            if (this.executor == null && this.secondaryExecutor == null) {
                this.executor = executor;
                this.secondaryExecutor = secondaryExecutor;
                this.detachExecutor = detach;
                failed = false;
            }
        }
        if (failed) {
            throw new IllegalStateException("Executor already set or used!");
        }
        return this;
    }

    private synchronized ScheduledThreadPoolExecutor getSecondaryExecutor() {
        if (this.secondaryExecutor == null) {
            this.secondaryExecutor = new ScheduledThreadPoolExecutor(1, new NamedThreadFactory("CoapClient(secondary)#"));
        }
        this.detachExecutor = false;
        return this.secondaryExecutor;
    }

    public synchronized Endpoint getEndpoint() {
        return this.endpoint;
    }

    public CoapClient setEndpoint(Endpoint endpoint) {
        synchronized (this) {
            this.endpoint = endpoint;
        }
        if (!endpoint.isStarted()) {
            try {
                endpoint.start();
                LOGGER.info("started set client endpoint {}", endpoint.getAddress());
            } catch (IOException e) {
                LOGGER.error("could not set and start client endpoint", e);
            }
        }
        return this;
    }

    public CoapClient useCONs() {
        this.type = CoAP.Type.CON;
        return this;
    }

    public CoapClient useNONs() {
        this.type = CoAP.Type.NON;
        return this;
    }

    public CoapClient useEarlyNegotiation(int size) {
        this.blockwise = size;
        return this;
    }

    public CoapClient useLateNegotiation() {
        this.blockwise = 0;
        return this;
    }

    public boolean ping() {
        return ping(this.timeout);
    }

    public boolean ping(long timeout) {
        return ping(new Long(timeout));
    }

    private boolean ping(Long timeout) {
        try {
            Request request = Request.newPing();
            request.setToken(Token.EMPTY);
            assignClientUriIfEmpty(request);
            Endpoint outEndpoint = getEffectiveEndpoint(request);
            if (timeout == null) {
                timeout = outEndpoint.getConfig().get(CoapConfig.EXCHANGE_LIFETIME, TimeUnit.MILLISECONDS);
            }
            request.addMessageObserver(new MessageObserverAdapter() { // from class: org.eclipse.californium.core.CoapClient.2
                @Override // org.eclipse.californium.core.coap.MessageObserverAdapter, org.eclipse.californium.core.coap.MessageObserver
                public void onContextEstablished(EndpointContext endpointContext) {
                    CoapClient.this.destinationContext.compareAndSet(null, endpointContext);
                }

                @Override // org.eclipse.californium.core.coap.MessageObserverAdapter, org.eclipse.californium.core.coap.MessageObserver
                public void onSendError(Throwable error) {
                    CoapClient.LOGGER.error("send error: {}", error.getMessage());
                }
            });
            send(request, outEndpoint).waitForResponse(timeout.longValue());
            return request.isRejected();
        } catch (InterruptedException e) {
            return false;
        }
    }

    public Set<WebLink> discover() throws ConnectorException, IOException {
        return discover(null);
    }

    public Set<WebLink> discover(String query) throws ConnectorException, IOException {
        Request discover = newGet();
        assignClientUriIfEmpty(discover);
        discover.getOptions().clearUriPath().clearUriQuery().setUriPath("/.well-known/core");
        if (query != null) {
            discover.getOptions().setUriQuery(query);
        }
        CoapResponse links = synchronous(discover);
        if (links == null) {
            return null;
        }
        setDestinationContextFromResponse(links.advanced());
        if (links.getOptions().getContentFormat() != 40) {
            return Collections.emptySet();
        }
        return LinkFormat.parse(links.getResponseText());
    }

    public CoapResponse get() throws ConnectorException, IOException {
        Request request = newGet();
        assignClientUriIfEmpty(request);
        return synchronous(request);
    }

    public CoapResponse get(int accept) throws ConnectorException, IOException {
        Request request = newGet();
        request.getOptions().setAccept(accept);
        assignClientUriIfEmpty(request);
        return synchronous(request);
    }

    public void get(CoapHandler handler) {
        Request request = newGet();
        assignClientUriIfEmpty(request);
        asynchronous(request, handler);
    }

    public void get(CoapHandler handler, int accept) {
        Request request = newGet();
        request.getOptions().setAccept(accept);
        assignClientUriIfEmpty(request);
        asynchronous(request, handler);
    }

    public CoapResponse post(String payload, int format) throws ConnectorException, IOException {
        Request request = newPost();
        request.setPayload(payload);
        request.getOptions().setContentFormat(format);
        assignClientUriIfEmpty(request);
        return synchronous(request);
    }

    public CoapResponse post(byte[] payload, int format) throws ConnectorException, IOException {
        Request request = newPost();
        request.setPayload(payload);
        request.getOptions().setContentFormat(format);
        assignClientUriIfEmpty(request);
        return synchronous(request);
    }

    public CoapResponse post(String payload, int format, int accept) throws ConnectorException, IOException {
        Request request = newPost();
        request.setPayload(payload);
        request.getOptions().setContentFormat(format);
        request.getOptions().setAccept(accept);
        assignClientUriIfEmpty(request);
        return synchronous(request);
    }

    public CoapResponse post(byte[] payload, int format, int accept) throws ConnectorException, IOException {
        Request request = newPost();
        request.setPayload(payload);
        request.getOptions().setContentFormat(format);
        request.getOptions().setAccept(accept);
        assignClientUriIfEmpty(request);
        return synchronous(request);
    }

    public void post(CoapHandler handler, String payload, int format) {
        Request request = newPost();
        request.setPayload(payload);
        request.getOptions().setContentFormat(format);
        assignClientUriIfEmpty(request);
        asynchronous(request, handler);
    }

    public void post(CoapHandler handler, byte[] payload, int format) {
        Request request = newPost();
        request.setPayload(payload);
        request.getOptions().setContentFormat(format);
        assignClientUriIfEmpty(request);
        asynchronous(request, handler);
    }

    public void post(CoapHandler handler, String payload, int format, int accept) {
        Request request = newPost();
        request.setPayload(payload);
        request.getOptions().setContentFormat(format);
        request.getOptions().setAccept(accept);
        assignClientUriIfEmpty(request);
        asynchronous(request, handler);
    }

    public void post(CoapHandler handler, byte[] payload, int format, int accept) {
        Request request = newPost();
        request.setPayload(payload);
        request.getOptions().setContentFormat(format);
        request.getOptions().setAccept(accept);
        assignClientUriIfEmpty(request);
        asynchronous(request, handler);
    }

    public CoapResponse put(String payload, int format) throws ConnectorException, IOException {
        Request request = newPut();
        request.setPayload(payload);
        request.getOptions().setContentFormat(format);
        assignClientUriIfEmpty(request);
        return synchronous(request);
    }

    public CoapResponse put(byte[] payload, int format) throws ConnectorException, IOException {
        Request request = newPut();
        request.setPayload(payload);
        request.getOptions().setContentFormat(format);
        assignClientUriIfEmpty(request);
        return synchronous(request);
    }

    public CoapResponse putIfMatch(String payload, int format, byte[]... etags) throws ConnectorException, IOException {
        Request request = newPut();
        request.setPayload(payload);
        request.getOptions().setContentFormat(format);
        assignClientUriIfEmpty(request);
        ifMatch(request, etags);
        return synchronous(request);
    }

    public CoapResponse putIfMatch(byte[] payload, int format, byte[]... etags) throws ConnectorException, IOException {
        Request request = newPut();
        request.setPayload(payload);
        request.getOptions().setContentFormat(format);
        assignClientUriIfEmpty(request);
        ifMatch(request, etags);
        return synchronous(request);
    }

    public CoapResponse putIfNoneMatch(String payload, int format) throws ConnectorException, IOException {
        Request request = newPut();
        request.setPayload(payload);
        request.getOptions().setContentFormat(format);
        request.getOptions().setIfNoneMatch(true);
        assignClientUriIfEmpty(request);
        return synchronous(request);
    }

    public CoapResponse putIfNoneMatch(byte[] payload, int format) throws ConnectorException, IOException {
        Request request = newPut();
        request.setPayload(payload);
        request.getOptions().setContentFormat(format);
        request.getOptions().setIfNoneMatch(true);
        assignClientUriIfEmpty(request);
        return synchronous(request);
    }

    public void put(CoapHandler handler, String payload, int format) {
        Request request = newPut();
        request.setPayload(payload);
        request.getOptions().setContentFormat(format);
        assignClientUriIfEmpty(request);
        asynchronous(request, handler);
    }

    public void put(CoapHandler handler, byte[] payload, int format) {
        Request request = newPut();
        request.setPayload(payload);
        request.getOptions().setContentFormat(format);
        assignClientUriIfEmpty(request);
        asynchronous(request, handler);
    }

    public void putIfMatch(CoapHandler handler, String payload, int format, byte[]... etags) {
        Request request = newPut();
        request.setPayload(payload);
        request.getOptions().setContentFormat(format);
        assignClientUriIfEmpty(request);
        ifMatch(request, etags);
        asynchronous(request, handler);
    }

    public void putIfMatch(CoapHandler handler, byte[] payload, int format, byte[]... etags) {
        Request request = newPut();
        request.setPayload(payload);
        request.getOptions().setContentFormat(format);
        assignClientUriIfEmpty(request);
        ifMatch(request, etags);
        asynchronous(request, handler);
    }

    public void putIfNoneMatch(CoapHandler handler, String payload, int format) {
        Request request = newPut();
        request.setPayload(payload);
        request.getOptions().setContentFormat(format);
        request.getOptions().setIfNoneMatch(true);
        assignClientUriIfEmpty(request);
        asynchronous(request, handler);
    }

    public void putIfNoneMatch(CoapHandler handler, byte[] payload, int format) {
        Request request = newPut();
        request.setPayload(payload);
        request.getOptions().setContentFormat(format);
        request.getOptions().setIfNoneMatch(true);
        assignClientUriIfEmpty(request);
        asynchronous(request, handler);
    }

    public CoapResponse delete() throws ConnectorException, IOException {
        Request request = newDelete();
        assignClientUriIfEmpty(request);
        return synchronous(request);
    }

    public void delete(CoapHandler handler) {
        Request request = newDelete();
        assignClientUriIfEmpty(request);
        asynchronous(request, handler);
    }

    public CoapResponse validate(byte[]... etags) throws ConnectorException, IOException {
        Request request = newGet();
        etags(request, etags);
        assignClientUriIfEmpty(request);
        return synchronous(request);
    }

    public void validate(CoapHandler handler, byte[]... etags) {
        Request request = newGet();
        etags(request, etags);
        assignClientUriIfEmpty(request);
        asynchronous(request, handler);
    }

    public CoapResponse advanced(Request request) throws ConnectorException, IOException {
        assignClientUriIfEmpty(request);
        return synchronous(request);
    }

    public void advanced(CoapHandler handler, Request request) {
        assignClientUriIfEmpty(request);
        asynchronous(request, handler);
    }

    public CoapObserveRelation observeAndWait(CoapHandler handler) throws ConnectorException, IOException {
        Request request = newGet();
        request.setObserve();
        return observeAndWait(request, handler);
    }

    public CoapObserveRelation observeAndWait(CoapHandler handler, int accept) throws ConnectorException, IOException {
        Request request = newGet();
        request.setObserve();
        request.getOptions().setAccept(accept);
        return observeAndWait(request, handler);
    }

    public CoapObserveRelation observe(CoapHandler handler) {
        Request request = newGet();
        request.setObserve();
        return observe(request, handler);
    }

    public CoapObserveRelation observe(CoapHandler handler, int accept) {
        Request request = newGet();
        request.setObserve();
        return observe(accept(request, accept), handler);
    }

    public void shutdown() {
        ExecutorService executor;
        ExecutorService secondaryExecutor;
        boolean shutdown;
        synchronized (this) {
            executor = this.executor;
            secondaryExecutor = this.secondaryExecutor;
            shutdown = !this.detachExecutor;
            this.executor = null;
            this.secondaryExecutor = null;
        }
        if (shutdown) {
            if (executor != null) {
                executor.shutdownNow();
            }
            if (secondaryExecutor != null) {
                secondaryExecutor.shutdownNow();
            }
        }
    }

    private void asynchronous(Request request, CoapHandler handler) {
        request.addMessageObserver(new MessageObserverImpl(handler, request.isMulticast()));
        send(request);
    }

    private CoapResponse synchronous(Request request) throws ConnectorException, IOException {
        return synchronous(request, getEffectiveEndpoint(request));
    }

    private CoapResponse synchronous(Request request, Endpoint outEndpoint) throws ConnectorException, IOException {
        try {
            Long timeout = getTimeout();
            if (timeout == null) {
                timeout = outEndpoint.getConfig().get(CoapConfig.EXCHANGE_LIFETIME, TimeUnit.MILLISECONDS);
            }
            Response response = send(request, outEndpoint).waitForResponse(timeout.longValue());
            if (response == null) {
                request.cancel();
                Throwable sendError = request.getSendError();
                if (sendError != null) {
                    if (sendError instanceof ConnectorException) {
                        throw ((ConnectorException) sendError);
                    }
                    throw new IOException(sendError);
                }
                return null;
            }
            if (!request.isMulticast()) {
                setDestinationContextFromResponse(response);
            }
            return new CoapResponse(response);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private static Request accept(Request request, int accept) {
        request.getOptions().setAccept(accept);
        return request;
    }

    private static Request etags(Request request, byte[]... etags) {
        for (byte[] etag : etags) {
            request.getOptions().addETag(etag);
        }
        return request;
    }

    private static Request ifMatch(Request request, byte[]... etags) {
        for (byte[] etag : etags) {
            request.getOptions().addIfMatch(etag);
        }
        return request;
    }

    public CoapObserveRelation observeAndWait(Request request, CoapHandler handler) throws ConnectorException, IOException {
        if (request.getOptions().hasObserve()) {
            assignClientUriIfEmpty(request);
            Endpoint outEndpoint = getEffectiveEndpoint(request);
            CoapObserveRelation relation = new CoapObserveRelation(request, outEndpoint, getSecondaryExecutor());
            ObserveMessageObserverImpl messageObserver = new ObserveMessageObserverImpl(handler, request.isMulticast(), relation);
            request.addMessageObserver(messageObserver);
            NotificationListener notificationListener = new Adapter(messageObserver, relation);
            outEndpoint.addNotificationListener(notificationListener);
            relation.setNotificationListener(notificationListener);
            CoapResponse response = synchronous(request, outEndpoint);
            if (response == null || !response.advanced().getOptions().hasObserve()) {
                relation.setCanceled(true);
            } else {
                relation.waitForResponse(2000L);
            }
            return relation;
        }
        throw new IllegalArgumentException("please make sure that the request has observe option set.");
    }

    public CoapObserveRelation observe(Request request, CoapHandler handler) {
        if (request.getOptions().hasObserve()) {
            assignClientUriIfEmpty(request);
            Endpoint outEndpoint = getEffectiveEndpoint(request);
            CoapObserveRelation relation = new CoapObserveRelation(request, outEndpoint, getSecondaryExecutor());
            ObserveMessageObserverImpl messageObserver = new ObserveMessageObserverImpl(handler, request.isMulticast(), relation);
            request.addMessageObserver(messageObserver);
            NotificationListener notificationListener = new Adapter(messageObserver, relation);
            outEndpoint.addNotificationListener(notificationListener);
            relation.setNotificationListener(notificationListener);
            send(request, outEndpoint);
            return relation;
        }
        throw new IllegalArgumentException("please make sure that the request has observe option set.");
    }

    protected Request send(Request request) {
        return send(request, getEffectiveEndpoint(request));
    }

    protected Request send(Request request, Endpoint outEndpoint) {
        if (this.blockwise != 0) {
            request.getOptions().setBlock2(new BlockOption(BlockOption.size2Szx(this.blockwise), false, 0));
        }
        outEndpoint.sendRequest(request);
        return request;
    }

    protected Endpoint getEffectiveEndpoint(Request request) {
        Endpoint myEndpoint = getEndpoint();
        if (myEndpoint != null) {
            return myEndpoint;
        }
        return EndpointManager.getEndpointManager().getDefaultEndpoint(request.getScheme());
    }

    protected void execute(Runnable job) {
        ExecutorService executor;
        synchronized (this) {
            executor = this.executor;
        }
        if (executor == null) {
            job.run();
            return;
        }
        try {
            executor.execute(job);
        } catch (RejectedExecutionException e) {
            if (!executor.isShutdown()) {
                LOGGER.warn("failed to execute job!");
            }
        }
    }

    private Request newGet() {
        return applyRequestType(Request.newGet());
    }

    private Request newPost() {
        return applyRequestType(Request.newPost());
    }

    private Request newPut() {
        return applyRequestType(Request.newPut());
    }

    private Request newDelete() {
        return applyRequestType(Request.newDelete());
    }

    private Request applyRequestType(Request request) {
        request.setType(this.type);
        return request;
    }

    private Request assignClientUriIfEmpty(Request request) {
        String scheme;
        EndpointContext context = this.destinationContext.get();
        if (context != null && request.getDestinationContext() == null) {
            request.setDestinationContext(context);
            if (this.useProxy && this.proxyScheme == null && (scheme = CoAP.getSchemeFromUri(this.uri)) != null && !CoAP.isSupportedScheme(scheme)) {
                request.setProxyUri(this.uri);
            } else {
                request.setURI(this.uri);
            }
        } else if (!request.hasURI() && !request.hasProxyURI()) {
            request.setURI(this.uri);
        }
        if (this.proxyScheme != null && !request.hasProxyURI()) {
            request.setProxyScheme(this.proxyScheme);
        }
        return request;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setDestinationContextFromResponse(Response response) {
        this.destinationContext.compareAndSet(null, response.getSourceContext());
    }

    /* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-californium-core.jar:org/eclipse/californium/core/CoapClient$Adapter.class */
    private class Adapter implements NotificationListener {
        private final MessageObserver observer;
        private final CoapObserveRelation relation;

        public Adapter(MessageObserver observer, CoapObserveRelation relation) {
            this.observer = observer;
            this.relation = relation;
        }

        @Override // org.eclipse.californium.core.observe.NotificationListener
        public void onNotification(Request request, Response response) {
            if (this.relation.matchRequest(request)) {
                this.observer.onResponse(response);
            }
        }
    }

    /* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-californium-core.jar:org/eclipse/californium/core/CoapClient$MessageObserverImpl.class */
    private class MessageObserverImpl extends MessageObserverAdapter {
        protected final CoapHandler handler;
        private final boolean multicast;

        private MessageObserverImpl(CoapHandler handler, boolean multicast) {
            this.handler = handler;
            this.multicast = multicast;
        }

        @Override // org.eclipse.californium.core.coap.MessageObserverAdapter, org.eclipse.californium.core.coap.MessageObserver
        public void onResponse(Response response) {
            if (!this.multicast) {
                CoapClient.this.setDestinationContextFromResponse(response);
            }
            succeeded(new CoapResponse(response));
        }

        protected void succeeded(final CoapResponse response) {
            CoapClient.this.execute(new Runnable() { // from class: org.eclipse.californium.core.CoapClient.MessageObserverImpl.1
                @Override // java.lang.Runnable
                public void run() {
                    try {
                        MessageObserverImpl.this.deliver(response);
                    } catch (Throwable t) {
                        CoapClient.LOGGER.warn("exception while handling response", t);
                    }
                }
            });
        }

        protected void deliver(CoapResponse response) {
            this.handler.onLoad(response);
        }

        @Override // org.eclipse.californium.core.coap.MessageObserverAdapter
        protected void failed() {
            CoapClient.this.execute(new Runnable() { // from class: org.eclipse.californium.core.CoapClient.MessageObserverImpl.2
                @Override // java.lang.Runnable
                public void run() {
                    try {
                        MessageObserverImpl.this.handler.onError();
                    } catch (Throwable t) {
                        CoapClient.LOGGER.warn("exception while handling failure", t);
                    }
                }
            });
        }
    }

    /* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-californium-core.jar:org/eclipse/californium/core/CoapClient$ObserveMessageObserverImpl.class */
    private class ObserveMessageObserverImpl extends MessageObserverImpl {
        private final CoapObserveRelation relation;

        public ObserveMessageObserverImpl(CoapHandler handler, boolean multicast, CoapObserveRelation relation) {
            super(handler, multicast);
            this.relation = relation;
        }

        @Override // org.eclipse.californium.core.CoapClient.MessageObserverImpl
        protected void deliver(CoapResponse response) {
            synchronized (this.relation) {
                if (this.relation.onResponse(response)) {
                    CoapClient.this.receivableNum.getAndIncrement();
                    CoapClient.LOGGER.debug("deliver receivable Number {}", Integer.valueOf(CoapClient.this.receivableNum.get()));
                    if (this.relation.getRequest() != null && this.relation.getRequest().getOptions() != null && this.relation.getRequest().getOptions().getUriPathString() != null) {
                        String uriPathString = this.relation.getRequest().getOptions().getUriPathString();
                        Integer integer = (Integer) CoapClient.this.receivableMap.get(uriPathString);
                        if (integer == null || integer.intValue() == 0) {
                            CoapClient.this.receivableMap.put(uriPathString, 1);
                            CoapClient.LOGGER.debug("deliver receivable Path : {} Number : {}", uriPathString, 1);
                        } else {
                            CoapClient.this.receivableMap.put(uriPathString, Integer.valueOf(integer.intValue() + 1));
                            CoapClient.LOGGER.debug("deliver receivable Path : {} Number : {}", uriPathString, Integer.valueOf(integer.intValue() + 1));
                        }
                    }
                    this.handler.onLoad(response);
                    return;
                }
                CoapClient.this.dropNum.getAndIncrement();
                CoapClient.LOGGER.debug("deliver drop Number {}", Integer.valueOf(CoapClient.this.dropNum.get()));
                if (this.relation.getRequest() != null && this.relation.getRequest().getOptions() != null && this.relation.getRequest().getOptions().getUriPathString() != null) {
                    String uriPathString2 = this.relation.getRequest().getOptions().getUriPathString();
                    Integer integer2 = (Integer) CoapClient.this.dropMap.get(uriPathString2);
                    if (integer2 == null || integer2.intValue() == 0) {
                        CoapClient.this.dropMap.put(uriPathString2, 1);
                        CoapClient.LOGGER.debug("deliver dropping old Path : {} Number : {}", uriPathString2, 1);
                    } else {
                        CoapClient.this.dropMap.put(uriPathString2, Integer.valueOf(integer2.intValue() + 1));
                        CoapClient.LOGGER.debug("deliver dropping old Path : {} Number : {}", uriPathString2, Integer.valueOf(integer2.intValue() + 1));
                    }
                }
                CoapClient.LOGGER.debug("dropping old notification: {}", response.advanced());
            }
        }

        @Override // org.eclipse.californium.core.CoapClient.MessageObserverImpl, org.eclipse.californium.core.coap.MessageObserverAdapter
        protected void failed() {
            this.relation.setCanceled(true);
            super.failed();
        }
    }

    /* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-californium-core.jar:org/eclipse/californium/core/CoapClient$Builder.class */
    public static class Builder {
        String scheme;
        String host;
        String port;
        String[] path;
        String[] query;

        public Builder(String host, int port) {
            this.host = host;
            this.port = Integer.toString(port);
        }

        public Builder scheme(String scheme) {
            this.scheme = scheme;
            return this;
        }

        public Builder host(String host) {
            this.host = host;
            return this;
        }

        public Builder port(String port) {
            this.port = port;
            return this;
        }

        public Builder port(int port) {
            this.port = Integer.toString(port);
            return this;
        }

        public Builder path(String... path) {
            this.path = path;
            return this;
        }

        public Builder query(String... query) {
            this.query = query;
            return this;
        }

        public CoapClient create() {
            StringBuilder builder = new StringBuilder();
            if (this.scheme != null) {
                builder.append(this.scheme).append(CoAP.URI_SCHEME_SEPARATOR);
            }
            builder.append(this.host).append(":").append(this.port);
            for (String element : this.path) {
                builder.append("/").append(element);
            }
            if (this.query.length > 0) {
                builder.append("?");
            }
            for (int i = 0; i < this.query.length; i++) {
                builder.append(this.query[i]);
                if (i < this.query.length - 1) {
                    builder.append("&");
                }
            }
            return new CoapClient(builder.toString());
        }
    }
}
