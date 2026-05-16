package org.eclipse.californium.core.server.resources;

import org.eclipse.californium.core.observe.ObserveRelation;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-californium-core.jar:org/eclipse/californium/core/server/resources/ResourceObserver.class */
public interface ResourceObserver {
    void changedName(String str);

    void changedPath(String str);

    void addedChild(Resource resource);

    void removedChild(Resource resource);

    void addedObserveRelation(ObserveRelation observeRelation);

    void removedObserveRelation(ObserveRelation observeRelation);
}
