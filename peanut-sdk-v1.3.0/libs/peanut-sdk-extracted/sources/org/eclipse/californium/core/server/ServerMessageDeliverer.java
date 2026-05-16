package org.eclipse.californium.core.server;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.Executor;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.californium.core.network.Exchange;
import org.eclipse.californium.core.observe.ObserveManager;
import org.eclipse.californium.core.observe.ObserveRelation;
import org.eclipse.californium.core.observe.ObservingEndpoint;
import org.eclipse.californium.core.server.resources.Resource;
import org.eclipse.californium.elements.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-californium-core.jar:org/eclipse/californium/core/server/ServerMessageDeliverer.class */
public class ServerMessageDeliverer implements MessageDeliverer {
    private static final Logger LOGGER = LoggerFactory.getLogger(ServerMessageDeliverer.class);
    private final Resource root;
    private final ObserveManager observeManager = new ObserveManager();

    public ServerMessageDeliverer(Resource root) {
        this.root = root;
    }

    @Override // org.eclipse.californium.core.server.MessageDeliverer
    public final void deliverRequest(final Exchange exchange) {
        if (exchange == null) {
            throw new NullPointerException("exchange must not be null");
        }
        boolean processed = preDeliverRequest(exchange);
        if (!processed) {
            try {
                final Resource resource = findResource(exchange);
                if (resource != null) {
                    checkForObserveOption(exchange, resource);
                    Executor executor = resource.getExecutor();
                    if (executor != null) {
                        executor.execute(new Runnable() { // from class: org.eclipse.californium.core.server.ServerMessageDeliverer.1
                            @Override // java.lang.Runnable
                            public void run() {
                                resource.handleRequest(exchange);
                            }
                        });
                    } else {
                        resource.handleRequest(exchange);
                    }
                } else {
                    if (LOGGER.isInfoEnabled()) {
                        Request request = exchange.getRequest();
                        LOGGER.info("did not find resource /{} requested by {}", request.getOptions().getUriPathString(), StringUtil.toLog(request.getSourceContext().getPeerAddress()));
                    }
                    exchange.sendResponse(new Response(CoAP.ResponseCode.NOT_FOUND));
                }
            } catch (DelivererException ex) {
                Response response = new Response(ex.getErrorResponseCode());
                response.setPayload(ex.getMessage());
                exchange.sendResponse(response);
            }
        }
    }

    protected boolean preDeliverRequest(Exchange exchange) {
        return false;
    }

    protected final void checkForObserveOption(Exchange exchange, Resource resource) {
        ObserveRelation relation;
        Request request = exchange.getRequest();
        if (CoAP.isObservable(request.getCode()) && request.getOptions().hasObserve() && resource.isObservable()) {
            InetSocketAddress source = request.getSourceContext().getPeerAddress();
            if (request.isObserve()) {
                LOGGER.debug("initiating an observe relation between {} and resource {}, {}", new Object[]{StringUtil.toLog(source), resource.getURI(), exchange});
                ObservingEndpoint remote = this.observeManager.findObservingEndpoint(source);
                ObserveRelation relation2 = new ObserveRelation(remote, resource, exchange);
                remote.addObserveRelation(relation2);
                exchange.setRelation(relation2);
                request.setProtectFromOffload();
                return;
            }
            if (request.isObserveCancel() && (relation = this.observeManager.getRelation(source, request.getToken())) != null) {
                relation.cancel();
            }
        }
    }

    protected Resource getRootResource() {
        return this.root;
    }

    protected Resource findResource(Exchange exchange) throws DelivererException {
        return findResource(exchange.getRequest().getOptions().getUriPath());
    }

    protected Resource findResource(List<String> path) throws DelivererException {
        Resource current = getRootResource();
        for (String name : path) {
            current = current.getChild(name);
            if (current == null) {
                break;
            }
        }
        return current;
    }

    @Override // org.eclipse.californium.core.server.MessageDeliverer
    public final void deliverResponse(Exchange exchange, Response response) {
        if (response == null) {
            throw new NullPointerException("Response must not be null");
        }
        if (exchange == null) {
            throw new NullPointerException("Exchange must not be null");
        }
        if (exchange.getRequest() == null) {
            throw new IllegalArgumentException("Exchange does not contain request");
        }
        boolean processed = preDeliverResponse(exchange, response);
        if (!processed) {
            exchange.getRequest().setResponse(response);
        }
    }

    protected boolean preDeliverResponse(Exchange exchange, Response response) {
        return false;
    }
}
