package org.eclipse.californium.core;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.ReentrantLock;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.californium.core.network.Exchange;
import org.eclipse.californium.core.observe.ObserveNotificationOrderer;
import org.eclipse.californium.core.observe.ObserveRelation;
import org.eclipse.californium.core.observe.ObserveRelationContainer;
import org.eclipse.californium.core.observe.ObserveRelationFilter;
import org.eclipse.californium.core.server.resources.CoapExchange;
import org.eclipse.californium.core.server.resources.Resource;
import org.eclipse.californium.core.server.resources.ResourceAttributes;
import org.eclipse.californium.core.server.resources.ResourceObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-californium-core.jar:org/eclipse/californium/core/CoapResource.class */
public class CoapResource implements Resource {
    protected static final Logger LOGGER = LoggerFactory.getLogger(CoapResource.class);
    private final ResourceAttributes attributes;
    private final ReentrantLock recursionProtection;
    private String name;
    private String path;
    private boolean visible;
    private boolean observable;
    private ConcurrentHashMap<String, Resource> children;
    private Resource parent;
    private CoAP.Type observeType;
    private List<ResourceObserver> observers;
    private ObserveRelationContainer observeRelations;
    private ObserveNotificationOrderer notificationOrderer;

    public CoapResource(String name) {
        this(name, true);
    }

    public CoapResource(String name, boolean visible) {
        this.recursionProtection = new ReentrantLock();
        this.observeType = null;
        this.name = name;
        this.path = "";
        this.visible = visible;
        this.attributes = new ResourceAttributes();
        this.children = new ConcurrentHashMap<>();
        this.observers = new CopyOnWriteArrayList();
        this.observeRelations = new ObserveRelationContainer();
        this.notificationOrderer = new ObserveNotificationOrderer();
    }

    @Override // org.eclipse.californium.core.server.resources.Resource
    public void handleRequest(Exchange exchange) {
        CoAP.Code code = exchange.getRequest().getCode();
        switch (code) {
            case GET:
                handleGET(new CoapExchange(exchange, this));
                break;
            case POST:
                handlePOST(new CoapExchange(exchange, this));
                break;
            case PUT:
                handlePUT(new CoapExchange(exchange, this));
                break;
            case DELETE:
                handleDELETE(new CoapExchange(exchange, this));
                break;
            case FETCH:
                handleFETCH(new CoapExchange(exchange, this));
                break;
            case PATCH:
                handlePATCH(new CoapExchange(exchange, this));
                break;
            case IPATCH:
                handleIPATCH(new CoapExchange(exchange, this));
                break;
            default:
                exchange.sendResponse(new Response(CoAP.ResponseCode.METHOD_NOT_ALLOWED));
                break;
        }
    }

    public void handleGET(CoapExchange exchange) {
        exchange.respond(CoAP.ResponseCode.METHOD_NOT_ALLOWED);
    }

    public void handlePOST(CoapExchange exchange) {
        exchange.respond(CoAP.ResponseCode.METHOD_NOT_ALLOWED);
    }

    public void handlePUT(CoapExchange exchange) {
        exchange.respond(CoAP.ResponseCode.METHOD_NOT_ALLOWED);
    }

    public void handleDELETE(CoapExchange exchange) {
        exchange.respond(CoAP.ResponseCode.METHOD_NOT_ALLOWED);
    }

    public void handleFETCH(CoapExchange exchange) {
        exchange.respond(CoAP.ResponseCode.METHOD_NOT_ALLOWED);
    }

    public void handlePATCH(CoapExchange exchange) {
        exchange.respond(CoAP.ResponseCode.METHOD_NOT_ALLOWED);
    }

    public void handleIPATCH(CoapExchange exchange) {
        exchange.respond(CoAP.ResponseCode.METHOD_NOT_ALLOWED);
    }

    public void checkObserveRelation(Exchange exchange, Response response) {
        ObserveRelation relation = exchange.getRelation();
        if (relation != null && !relation.isCanceled() && response.isSuccess()) {
            if (!relation.isEstablished()) {
                relation.setEstablished();
                addObserveRelation(relation);
            } else if (this.observeType != null && response.getType() == null) {
                response.setType(this.observeType);
            }
            response.getOptions().setObserve(this.notificationOrderer.getCurrent());
        }
    }

    @Override // org.eclipse.californium.core.server.resources.Resource
    public synchronized void add(Resource child) {
        if (child.getName() == null) {
            throw new NullPointerException("Child must have a name");
        }
        if (child.getParent() != null) {
            child.getParent().delete(child);
        }
        this.children.put(child.getName(), child);
        child.setParent(this);
        for (ResourceObserver obs : this.observers) {
            obs.addedChild(child);
        }
    }

    public synchronized CoapResource add(CoapResource child) {
        add((Resource) child);
        return this;
    }

    public synchronized CoapResource add(CoapResource... children) {
        for (CoapResource child : children) {
            add(child);
        }
        return this;
    }

    @Override // org.eclipse.californium.core.server.resources.Resource
    public synchronized boolean delete(Resource child) {
        if (child.getParent() == this && this.children.remove(child.getName(), child)) {
            child.setParent(null);
            child.setPath(null);
            for (ResourceObserver obs : this.observers) {
                obs.removedChild(child);
            }
            return true;
        }
        return false;
    }

    public synchronized void delete() {
        Resource parent = getParent();
        if (parent != null) {
            parent.delete(this);
        }
        if (isObservable()) {
            clearAndNotifyObserveRelations(CoAP.ResponseCode.NOT_FOUND);
        }
    }

    public void clearObserveRelations() {
        clearAndNotifyObserveRelations(null, null);
    }

    public void clearAndNotifyObserveRelations(CoAP.ResponseCode code) {
        clearAndNotifyObserveRelations(null, code);
    }

    public void clearAndNotifyObserveRelations(final ObserveRelationFilter filter, final CoAP.ResponseCode code) {
        if (code != null && code.isSuccess()) {
            throw new IllegalArgumentException("Only error-responses are supported, not a " + code + "/" + code.name() + "!");
        }
        for (ObserveRelation relation : this.observeRelations) {
            final Exchange exchange = relation.getExchange();
            exchange.execute(new Runnable() { // from class: org.eclipse.californium.core.CoapResource.1
                @Override // java.lang.Runnable
                public void run() {
                    ObserveRelation relation2 = exchange.getRelation();
                    if (relation2 != null && relation2.isEstablished()) {
                        if (code != null && (null == filter || filter.accept(relation2))) {
                            Response response = new Response(code);
                            response.setType(CoAP.Type.CON);
                            exchange.sendResponse(response);
                            return;
                        }
                        relation2.cancel();
                    }
                }
            });
        }
    }

    @Override // org.eclipse.californium.core.server.resources.Resource
    public Resource getParent() {
        return this.parent;
    }

    @Override // org.eclipse.californium.core.server.resources.Resource
    public void setParent(Resource parent) {
        this.parent = parent;
        if (parent != null) {
            this.path = parent.getPath() + parent.getName() + "/";
        }
        adjustChildrenPath();
    }

    @Override // org.eclipse.californium.core.server.resources.Resource
    public Resource getChild(String name) {
        return this.children.get(name);
    }

    @Override // org.eclipse.californium.core.server.resources.Resource
    public void addObserver(ResourceObserver observer) {
        this.observers.add(observer);
    }

    @Override // org.eclipse.californium.core.server.resources.Resource
    public void removeObserver(ResourceObserver observer) {
        this.observers.remove(observer);
    }

    @Override // org.eclipse.californium.core.server.resources.Resource
    public ResourceAttributes getAttributes() {
        return this.attributes;
    }

    @Override // org.eclipse.californium.core.server.resources.Resource
    public String getName() {
        return this.name;
    }

    @Override // org.eclipse.californium.core.server.resources.Resource
    public boolean isCachable() {
        return true;
    }

    @Override // org.eclipse.californium.core.server.resources.Resource
    public String getPath() {
        return this.path;
    }

    @Override // org.eclipse.californium.core.server.resources.Resource
    public String getURI() {
        return getPath() + getName();
    }

    @Override // org.eclipse.californium.core.server.resources.Resource
    public synchronized void setPath(String path) {
        String old = this.path;
        this.path = path;
        for (ResourceObserver obs : this.observers) {
            obs.changedPath(old);
        }
        adjustChildrenPath();
    }

    @Override // org.eclipse.californium.core.server.resources.Resource
    public synchronized void setName(String name) {
        if (name == null) {
            throw new NullPointerException("name must not be null!");
        }
        String old = this.name;
        Resource parent = getParent();
        if (parent != null) {
            synchronized (parent) {
                parent.delete(this);
                this.name = name;
                parent.add(this);
            }
        } else {
            this.name = name;
        }
        adjustChildrenPath();
        for (ResourceObserver obs : this.observers) {
            obs.changedName(old);
        }
    }

    private void adjustChildrenPath() {
        String childpath = this.path + this.name + "/";
        for (Resource child : this.children.values()) {
            child.setPath(childpath);
        }
    }

    @Override // org.eclipse.californium.core.server.resources.Resource
    public boolean isVisible() {
        return this.visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    @Override // org.eclipse.californium.core.server.resources.Resource
    public boolean isObservable() {
        return this.observable;
    }

    public void setObservable(boolean observable) {
        this.observable = observable;
    }

    public void setObserveType(CoAP.Type type) {
        if (type == CoAP.Type.ACK || type == CoAP.Type.RST) {
            throw new IllegalArgumentException("Only CON and NON notifications are allowed or null for no changes by the framework");
        }
        this.observeType = type;
    }

    @Override // org.eclipse.californium.core.server.resources.Resource
    public void addObserveRelation(ObserveRelation relation) {
        ObserveRelation previous = this.observeRelations.addAndGetPrevious(relation);
        if (previous != null) {
            LOGGER.info("replacing observe relation between {} and resource {} (new {}, size {})", new Object[]{relation.getKey(), getURI(), relation.getExchange(), Integer.valueOf(this.observeRelations.getSize())});
            for (ResourceObserver obs : this.observers) {
                obs.removedObserveRelation(previous);
            }
        } else {
            LOGGER.info("successfully established observe relation between {} and resource {} ({}, size {})", new Object[]{relation.getKey(), getURI(), relation.getExchange(), Integer.valueOf(this.observeRelations.getSize())});
        }
        for (ResourceObserver obs2 : this.observers) {
            obs2.addedObserveRelation(relation);
        }
    }

    @Override // org.eclipse.californium.core.server.resources.Resource
    public void removeObserveRelation(ObserveRelation relation) {
        if (this.observeRelations.remove(relation)) {
            LOGGER.info("remove observe relation between {} and resource {} ({}, size {})", new Object[]{relation.getKey(), getURI(), relation.getExchange(), Integer.valueOf(this.observeRelations.getSize())});
            for (ResourceObserver obs : this.observers) {
                obs.removedObserveRelation(relation);
            }
        }
    }

    public int getObserverCount() {
        return this.observeRelations.getSize();
    }

    public void changed() {
        changed(null);
    }

    public void changed(final ObserveRelationFilter filter) {
        Executor executor = getExecutor();
        if (executor == null) {
            if (this.recursionProtection.isHeldByCurrentThread()) {
                throw new IllegalStateException("Recursion detected! Please call \"changed()\" using an executor.");
            }
            this.recursionProtection.lock();
            try {
                notifyObserverRelations(filter);
                return;
            } finally {
                this.recursionProtection.unlock();
            }
        }
        executor.execute(new Runnable() { // from class: org.eclipse.californium.core.CoapResource.2
            @Override // java.lang.Runnable
            public void run() {
                CoapResource.this.notifyObserverRelations(filter);
            }
        });
    }

    protected void notifyObserverRelations(ObserveRelationFilter filter) {
        this.notificationOrderer.getNextObserveNumber();
        for (ObserveRelation relation : this.observeRelations) {
            if (null == filter || filter.accept(relation)) {
                relation.notifyObservers();
            }
        }
    }

    @Override // org.eclipse.californium.core.server.resources.Resource
    public Collection<Resource> getChildren() {
        return this.children.values();
    }

    @Override // org.eclipse.californium.core.server.resources.Resource
    public Executor getExecutor() {
        Resource parent = getParent();
        if (parent != null) {
            return parent.getExecutor();
        }
        return null;
    }

    public void execute(Runnable task) {
        Executor executor = getExecutor();
        if (executor == null) {
            task.run();
        } else {
            executor.execute(task);
        }
    }

    public void executeAndWait(final Runnable task) throws InterruptedException {
        final Semaphore semaphore = new Semaphore(0);
        execute(new Runnable() { // from class: org.eclipse.californium.core.CoapResource.3
            @Override // java.lang.Runnable
            public void run() {
                task.run();
                semaphore.release();
            }
        });
        semaphore.acquire();
    }
}
