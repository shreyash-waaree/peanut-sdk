package org.eclipse.californium.core;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.config.CoapConfig;
import org.eclipse.californium.core.network.CoapEndpoint;
import org.eclipse.californium.core.network.Endpoint;
import org.eclipse.californium.core.server.MessageDeliverer;
import org.eclipse.californium.core.server.ServerInterface;
import org.eclipse.californium.core.server.ServerMessageDeliverer;
import org.eclipse.californium.core.server.resources.CoapExchange;
import org.eclipse.californium.core.server.resources.DiscoveryResource;
import org.eclipse.californium.core.server.resources.Resource;
import org.eclipse.californium.elements.Connector;
import org.eclipse.californium.elements.PersistentConnector;
import org.eclipse.californium.elements.config.Configuration;
import org.eclipse.californium.elements.util.DataStreamReader;
import org.eclipse.californium.elements.util.DatagramWriter;
import org.eclipse.californium.elements.util.ExecutorsUtil;
import org.eclipse.californium.elements.util.NamedThreadFactory;
import org.eclipse.californium.elements.util.SerializationUtil;
import org.eclipse.californium.elements.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-californium-core.jar:org/eclipse/californium/core/CoapServer.class */
public class CoapServer implements ServerInterface {
    private static final String MARK = "CoAP";
    protected static final Logger LOGGER = LoggerFactory.getLogger(CoapServer.class);
    private final Resource root;
    private final Configuration config;
    private MessageDeliverer deliverer;
    private final List<Endpoint> endpoints;
    private ScheduledExecutorService executor;
    private ScheduledExecutorService secondaryExecutor;
    private boolean detachExecutor;
    private volatile boolean running;
    private volatile String tag;

    public CoapServer() {
        this(Configuration.getStandard(), new int[0]);
    }

    public CoapServer(int... ports) {
        this(Configuration.getStandard(), ports);
    }

    public CoapServer(Configuration config, int... ports) {
        if (config != null) {
            this.config = config;
        } else {
            this.config = Configuration.getStandard();
        }
        setTag(null);
        this.root = createRoot();
        this.deliverer = new ServerMessageDeliverer(this.root);
        CoapResource wellKnown = new CoapResource(".well-known");
        wellKnown.setVisible(false);
        wellKnown.add((CoapResource) new DiscoveryResource(this.root));
        this.root.add(wellKnown);
        this.endpoints = new ArrayList();
        if (ports != null) {
            for (int port : ports) {
                CoapEndpoint.Builder builder = new CoapEndpoint.Builder();
                builder.setPort(port);
                builder.setConfiguration(config);
                addEndpoint(builder.build());
            }
        }
    }

    public synchronized void setExecutors(ScheduledExecutorService mainExecutor, ScheduledExecutorService secondaryExecutor, boolean detach) {
        if (mainExecutor == null || secondaryExecutor == null) {
            throw new NullPointerException("executors must not be null");
        }
        if (this.executor == mainExecutor && this.secondaryExecutor == secondaryExecutor) {
            return;
        }
        if (this.running) {
            throw new IllegalStateException("executor service can not be set on running server");
        }
        if (!this.detachExecutor) {
            if (this.executor != null) {
                this.executor.shutdownNow();
            }
            if (this.secondaryExecutor != null) {
                this.secondaryExecutor.shutdownNow();
            }
        }
        this.executor = mainExecutor;
        this.secondaryExecutor = secondaryExecutor;
        this.detachExecutor = detach;
        for (Endpoint ep : this.endpoints) {
            ep.setExecutors(this.executor, this.secondaryExecutor);
        }
    }

    @Override // org.eclipse.californium.core.server.ServerInterface
    public boolean isRunning() {
        return this.running;
    }

    @Override // org.eclipse.californium.core.server.ServerInterface
    public synchronized void start() {
        if (this.running) {
            return;
        }
        LOGGER.info("{}Starting server", getTag());
        if (this.executor == null) {
            setExecutors(ExecutorsUtil.newScheduledThreadPool(((Integer) this.config.get(CoapConfig.PROTOCOL_STAGE_THREAD_COUNT)).intValue(), new NamedThreadFactory("CoapServer(main)#")), ExecutorsUtil.newDefaultSecondaryScheduler("CoapServer(secondary)#"), false);
        }
        if (this.endpoints.isEmpty()) {
            int port = ((Integer) this.config.get(CoapConfig.COAP_PORT)).intValue();
            LOGGER.info("{}no endpoints have been defined for server, setting up server endpoint on default port {}", getTag(), Integer.valueOf(port));
            CoapEndpoint.Builder builder = new CoapEndpoint.Builder();
            builder.setPort(port);
            builder.setConfiguration(this.config);
            addEndpoint(builder.build());
        }
        int started = 0;
        for (Endpoint ep : this.endpoints) {
            try {
                ep.start();
                started++;
            } catch (IOException e) {
                LOGGER.error("{}cannot start server endpoint [{}]", new Object[]{getTag(), ep.getAddress(), e});
            }
        }
        if (started == 0) {
            throw new IllegalStateException("None of the server endpoints could be started");
        }
        this.running = true;
    }

    @Override // org.eclipse.californium.core.server.ServerInterface
    public synchronized void stop() {
        if (this.running) {
            this.running = false;
            LOGGER.info("{}Stopping server ...", getTag());
            for (Endpoint ep : this.endpoints) {
                ep.stop();
            }
            LOGGER.info("{}Stopped server.", getTag());
        }
    }

    @Override // org.eclipse.californium.core.server.ServerInterface
    public synchronized void destroy() {
        LOGGER.info("{}Destroying server", getTag());
        try {
            if (!this.detachExecutor) {
                if (this.running) {
                    ExecutorsUtil.shutdownExecutorGracefully(2000L, this.executor, this.secondaryExecutor);
                } else {
                    if (this.executor != null) {
                        this.executor.shutdownNow();
                    }
                    if (this.secondaryExecutor != null) {
                        this.secondaryExecutor.shutdownNow();
                    }
                }
            }
        } finally {
            for (Endpoint ep : this.endpoints) {
                ep.destroy();
            }
            LOGGER.info("{}CoAP server has been destroyed", getTag());
            this.running = false;
        }
    }

    public void setMessageDeliverer(MessageDeliverer deliverer) {
        this.deliverer = deliverer;
        for (Endpoint endpoint : this.endpoints) {
            endpoint.setMessageDeliverer(deliverer);
        }
    }

    public MessageDeliverer getMessageDeliverer() {
        return this.deliverer;
    }

    @Override // org.eclipse.californium.core.server.ServerInterface
    public void addEndpoint(Endpoint endpoint) {
        endpoint.setMessageDeliverer(this.deliverer);
        if (this.executor != null && this.secondaryExecutor != null) {
            endpoint.setExecutors(this.executor, this.secondaryExecutor);
        }
        this.endpoints.add(endpoint);
    }

    @Override // org.eclipse.californium.core.server.ServerInterface
    public List<Endpoint> getEndpoints() {
        return this.endpoints;
    }

    @Override // org.eclipse.californium.core.server.ServerInterface
    public Endpoint getEndpoint(int port) {
        Endpoint endpoint = null;
        for (Endpoint ep : this.endpoints) {
            if (ep.getAddress().getPort() == port) {
                endpoint = ep;
            }
        }
        return endpoint;
    }

    @Override // org.eclipse.californium.core.server.ServerInterface
    public Endpoint getEndpoint(URI uri) {
        Endpoint endpoint = null;
        Iterator<Endpoint> it = this.endpoints.iterator();
        while (true) {
            if (!it.hasNext()) {
                break;
            }
            Endpoint ep = it.next();
            if (uri.equals(ep.getUri())) {
                endpoint = ep;
                break;
            }
        }
        return endpoint;
    }

    @Override // org.eclipse.californium.core.server.ServerInterface
    public Endpoint getEndpoint(InetSocketAddress address) {
        Endpoint endpoint = null;
        Iterator<Endpoint> it = this.endpoints.iterator();
        while (true) {
            if (!it.hasNext()) {
                break;
            }
            Endpoint ep = it.next();
            if (address.equals(ep.getAddress())) {
                endpoint = ep;
                break;
            }
        }
        return endpoint;
    }

    @Override // org.eclipse.californium.core.server.ServerInterface
    public CoapServer add(Resource... resources) {
        for (Resource r : resources) {
            this.root.add(r);
        }
        return this;
    }

    @Override // org.eclipse.californium.core.server.ServerInterface
    public boolean remove(Resource resource) {
        return this.root.delete(resource);
    }

    public void setTag(String tag) {
        this.tag = StringUtil.normalizeLoggingTag(tag);
    }

    @Override // org.eclipse.californium.core.server.ServerInterface
    public String getTag() {
        return this.tag;
    }

    public int saveAllConnectors(OutputStream out, long maxQuietPeriodInSeconds) throws IOException {
        stop();
        int count = 0;
        DatagramWriter writer = new DatagramWriter();
        for (Endpoint endpoint : getEndpoints()) {
            if (endpoint instanceof CoapEndpoint) {
                Connector connector = ((CoapEndpoint) endpoint).getConnector();
                if (connector instanceof PersistentConnector) {
                    SerializationUtil.write(writer, MARK, 8);
                    SerializationUtil.write(writer, getTag(), 8);
                    SerializationUtil.write(writer, endpoint.getUri().toASCIIString(), 8);
                    writer.writeTo(out);
                    int saved = ((PersistentConnector) connector).saveConnections(out, maxQuietPeriodInSeconds);
                    count += saved;
                }
            }
        }
        return count;
    }

    public static ConnectorIdentifier readConnectorIdentifier(InputStream in) throws IOException {
        DataStreamReader reader = new DataStreamReader(in);
        try {
            if (!SerializationUtil.verifyString(reader, MARK, 8)) {
                return null;
            }
            String tag = SerializationUtil.readString(reader, 8);
            if (tag == null) {
                throw new IOException("Missing server's tag!");
            }
            String uri = SerializationUtil.readString(reader, 8);
            try {
                return new ConnectorIdentifier(tag, new URI(uri));
            } catch (URISyntaxException e) {
                LOGGER.warn("{}bad URI {}!", new Object[]{tag, uri, e});
                throw new IOException("Bad URI '" + uri + "'!");
            }
        } catch (IllegalArgumentException ex) {
            LOGGER.warn("loading failed, out of sync!");
            throw new IOException(ex.getMessage() + " " + in.available() + " bytes left.");
        }
    }

    public int loadConnector(ConnectorIdentifier identifier, InputStream in, long delta) throws IOException {
        Endpoint endpoint = getEndpoint(identifier.uri);
        if (endpoint == null) {
            LOGGER.warn("{}connector {} not available!", getTag(), identifier.uri);
            return -1;
        }
        PersistentConnector persistentConnector = null;
        if (endpoint instanceof CoapEndpoint) {
            Connector connector = ((CoapEndpoint) endpoint).getConnector();
            if (connector instanceof PersistentConnector) {
                persistentConnector = (PersistentConnector) connector;
            }
        }
        if (persistentConnector != null) {
            try {
                return persistentConnector.loadConnections(in, delta);
            } catch (IllegalArgumentException e) {
                LOGGER.warn("{}loading failed:", getTag(), e);
                return 0;
            }
        }
        LOGGER.warn("{}connector {} doesn't support persistence!", getTag(), identifier.uri);
        return -1;
    }

    public Resource getRoot() {
        return this.root;
    }

    public Configuration getConfig() {
        return this.config;
    }

    protected Resource createRoot() {
        return new RootResource();
    }

    /* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-californium-core.jar:org/eclipse/californium/core/CoapServer$RootResource.class */
    private class RootResource extends CoapResource {
        private final String msg;

        public RootResource() {
            super("");
            String title = "CoAP RFC 7252";
            if (StringUtil.CALIFORNIUM_VERSION != null) {
                String version = "Cf " + StringUtil.CALIFORNIUM_VERSION;
                title = String.format("%s %50s", title, version);
            }
            StringBuilder builder = new StringBuilder().append("****************************************************************\n").append(title).append("\n").append("****************************************************************\n").append("This server is using the Eclipse Californium (Cf) CoAP framework\n").append("published under EPL+EDL: http://www.eclipse.org/californium/\n\n");
            builder.append("(c) 2014-2021 Institute for Pervasive Computing, ETH Zurich and others\n");
            String master = StringUtil.getConfiguration("COAP_ROOT_RESOURCE_FOOTER");
            if (master != null) {
                builder.append(master).append("\n");
            }
            builder.append("****************************************************************");
            this.msg = builder.toString();
        }

        @Override // org.eclipse.californium.core.CoapResource
        public void handleGET(CoapExchange exchange) {
            exchange.respond(CoAP.ResponseCode.CONTENT, this.msg);
        }
    }

    /* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-californium-core.jar:org/eclipse/californium/core/CoapServer$ConnectorIdentifier.class */
    public static class ConnectorIdentifier {
        public final String tag;
        public final URI uri;

        private ConnectorIdentifier(String tag, URI uri) {
            this.tag = tag;
            this.uri = uri;
        }
    }
}
