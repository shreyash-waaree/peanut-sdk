package org.eclipse.californium.core.server;

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.List;
import org.eclipse.californium.core.network.Endpoint;
import org.eclipse.californium.core.server.resources.Resource;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-californium-core.jar:org/eclipse/californium/core/server/ServerInterface.class */
public interface ServerInterface {
    boolean isRunning();

    void start();

    void stop();

    void destroy();

    ServerInterface add(Resource... resourceArr);

    boolean remove(Resource resource);

    void addEndpoint(Endpoint endpoint);

    List<Endpoint> getEndpoints();

    Endpoint getEndpoint(URI uri);

    Endpoint getEndpoint(InetSocketAddress inetSocketAddress);

    Endpoint getEndpoint(int i);

    String getTag();
}
