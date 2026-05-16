package org.eclipse.californium.core.observe;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.eclipse.californium.core.coap.Token;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-californium-core.jar:org/eclipse/californium/core/observe/ObservingEndpoint.class */
public class ObservingEndpoint {
    private final InetSocketAddress address;
    private final List<ObserveRelation> relations = new CopyOnWriteArrayList();

    public ObservingEndpoint(InetSocketAddress address) {
        this.address = address;
    }

    public void addObserveRelation(ObserveRelation relation) {
        this.relations.add(relation);
    }

    public void removeObserveRelation(ObserveRelation relation) {
        this.relations.remove(relation);
    }

    public void cancelAll() {
        for (ObserveRelation relation : this.relations) {
            relation.cancel();
        }
    }

    public InetSocketAddress getAddress() {
        return this.address;
    }

    public ObserveRelation getObserveRelation(Token token) {
        if (token != null) {
            for (ObserveRelation relation : this.relations) {
                if (token.equals(relation.getExchange().getRequest().getToken())) {
                    return relation;
                }
            }
            return null;
        }
        return null;
    }
}
