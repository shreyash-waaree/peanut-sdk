package org.eclipse.californium.core.server.resources;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.network.Exchange;
import org.eclipse.californium.elements.util.ExecutorsUtil;
import org.eclipse.californium.elements.util.NamedThreadFactory;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-californium-core.jar:org/eclipse/californium/core/server/resources/ConcurrentCoapResource.class */
public class ConcurrentCoapResource extends CoapResource {
    public static int SINGLE_THREADED = 1;
    private int threads;
    private ExecutorService executor;

    public ConcurrentCoapResource(String name) {
        this(name, getAvailableProcessors());
    }

    public ConcurrentCoapResource(String name, int threads) {
        super(name);
        this.threads = threads;
        setExecutor(ExecutorsUtil.newFixedThreadPool(threads, new NamedThreadFactory("ConcurrentCoapResource-" + name + '#')));
    }

    public void setExecutor(ExecutorService executor) {
        this.executor = executor;
    }

    @Override // org.eclipse.californium.core.CoapResource, org.eclipse.californium.core.server.resources.Resource
    public Executor getExecutor() {
        return this.executor != null ? this.executor : super.getExecutor();
    }

    protected static int getAvailableProcessors() {
        return Runtime.getRuntime().availableProcessors();
    }

    public int getThreadCount() {
        return this.threads;
    }

    public static ConcurrentCoapResource createConcurrentCoapResource(int threads, final Resource impl) {
        return new ConcurrentCoapResource(impl.getName(), threads) { // from class: org.eclipse.californium.core.server.resources.ConcurrentCoapResource.1
            @Override // org.eclipse.californium.core.CoapResource, org.eclipse.californium.core.server.resources.Resource
            public void handleRequest(Exchange exchange) {
                impl.handleRequest(exchange);
            }
        };
    }
}
