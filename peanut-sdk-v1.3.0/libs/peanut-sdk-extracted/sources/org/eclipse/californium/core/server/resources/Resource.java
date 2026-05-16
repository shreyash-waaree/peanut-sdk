package org.eclipse.californium.core.server.resources;

import java.util.Collection;
import java.util.concurrent.Executor;
import org.eclipse.californium.core.network.Exchange;
import org.eclipse.californium.core.observe.ObserveRelation;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-californium-core.jar:org/eclipse/californium/core/server/resources/Resource.class */
public interface Resource {
    void handleRequest(Exchange exchange);

    String getName();

    void setName(String str);

    String getPath();

    void setPath(String str);

    String getURI();

    boolean isVisible();

    boolean isCachable();

    boolean isObservable();

    ResourceAttributes getAttributes();

    void add(Resource resource);

    boolean delete(Resource resource);

    Collection<Resource> getChildren();

    Resource getChild(String str);

    Resource getParent();

    void setParent(Resource resource);

    void addObserver(ResourceObserver resourceObserver);

    void removeObserver(ResourceObserver resourceObserver);

    void addObserveRelation(ObserveRelation observeRelation);

    void removeObserveRelation(ObserveRelation observeRelation);

    Executor getExecutor();
}
