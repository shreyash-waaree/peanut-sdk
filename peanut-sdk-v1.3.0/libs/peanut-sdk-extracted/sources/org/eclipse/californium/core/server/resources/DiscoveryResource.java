package org.eclipse.californium.core.server.resources;

import java.util.List;
import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.LinkFormat;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-californium-core.jar:org/eclipse/californium/core/server/resources/DiscoveryResource.class */
public class DiscoveryResource extends CoapResource {
    public static final String CORE = "core";
    private final Resource root;

    public DiscoveryResource(Resource root) {
        this(CORE, root);
    }

    public DiscoveryResource(String name, Resource root) {
        super(name);
        this.root = root;
    }

    @Override // org.eclipse.californium.core.CoapResource
    public void handleGET(CoapExchange exchange) {
        List<String> query = exchange.getRequestOptions().getUriQuery();
        if (query.size() <= 1) {
            String tree = discoverTree(this.root, query);
            exchange.respond(CoAP.ResponseCode.CONTENT, tree, 40);
        } else {
            exchange.respond(CoAP.ResponseCode.BAD_OPTION, "only one search query is supported!", 0);
        }
    }

    public String discoverTree(Resource root, List<String> queries) {
        StringBuilder buffer = new StringBuilder();
        for (Resource child : root.getChildren()) {
            LinkFormat.serializeTree(child, queries, buffer);
        }
        if (buffer.length() > 1) {
            buffer.setLength(buffer.length() - 1);
        }
        return buffer.toString();
    }
}
