package org.eclipse.californium.core.server.resources;

import org.eclipse.californium.core.observe.ObserveRelation;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-californium-core.jar:org/eclipse/californium/core/server/resources/ResourceObserverAdapter.class */
public abstract class ResourceObserverAdapter implements ResourceObserver {
    @Override // org.eclipse.californium.core.server.resources.ResourceObserver
    public void changedName(String old) {
    }

    @Override // org.eclipse.californium.core.server.resources.ResourceObserver
    public void changedPath(String old) {
    }

    @Override // org.eclipse.californium.core.server.resources.ResourceObserver
    public void addedChild(Resource child) {
    }

    @Override // org.eclipse.californium.core.server.resources.ResourceObserver
    public void removedChild(Resource child) {
    }

    @Override // org.eclipse.californium.core.server.resources.ResourceObserver
    public void addedObserveRelation(ObserveRelation relation) {
    }

    @Override // org.eclipse.californium.core.server.resources.ResourceObserver
    public void removedObserveRelation(ObserveRelation relation) {
    }
}
