package org.eclipse.californium.core.observe;

import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-californium-core.jar:org/eclipse/californium/core/observe/ObserveRelationContainer.class */
public class ObserveRelationContainer implements Iterable<ObserveRelation> {
    private final ConcurrentHashMap<String, ObserveRelation> observeRelations = new ConcurrentHashMap<>();

    public boolean add(ObserveRelation relation) {
        return addAndGetPrevious(relation) != null;
    }

    public ObserveRelation addAndGetPrevious(ObserveRelation relation) {
        if (relation == null) {
            throw new NullPointerException();
        }
        ObserveRelation previous = this.observeRelations.put(relation.getKey(), relation);
        if (null != previous) {
            previous.cancel();
        }
        return previous;
    }

    public boolean remove(ObserveRelation relation) {
        if (relation == null) {
            throw new NullPointerException();
        }
        return this.observeRelations.remove(relation.getKey(), relation);
    }

    public int getSize() {
        return this.observeRelations.size();
    }

    @Override // java.lang.Iterable
    public Iterator<ObserveRelation> iterator() {
        return this.observeRelations.values().iterator();
    }
}
