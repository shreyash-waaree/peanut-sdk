package org.eclipse.californium.core.server.resources;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.eclipse.californium.core.coap.LinkFormat;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-californium-core.jar:org/eclipse/californium/core/server/resources/ResourceAttributes.class */
public class ResourceAttributes {
    private final ConcurrentMap<String, AttributeValues> attributes = new ConcurrentHashMap();

    public int getCount() {
        return this.attributes.size();
    }

    public String getTitle() {
        if (containsAttribute(LinkFormat.TITLE)) {
            return getAttributeValues(LinkFormat.TITLE).get(0);
        }
        return null;
    }

    public void setTitle(String title) {
        findAttributeValues(LinkFormat.TITLE).setOnly(title);
    }

    public void addResourceType(String type) {
        findAttributeValues(LinkFormat.RESOURCE_TYPE).add(type);
    }

    public void clearResourceType() {
        this.attributes.remove(LinkFormat.RESOURCE_TYPE);
    }

    public List<String> getResourceTypes() {
        return getAttributeValues(LinkFormat.RESOURCE_TYPE);
    }

    public void addInterfaceDescription(String description) {
        findAttributeValues(LinkFormat.INTERFACE_DESCRIPTION).add(description);
    }

    public List<String> getInterfaceDescriptions() {
        return getAttributeValues(LinkFormat.INTERFACE_DESCRIPTION);
    }

    public void setMaximumSizeEstimate(String size) {
        findAttributeValues(LinkFormat.MAX_SIZE_ESTIMATE).setOnly(size);
    }

    public void setMaximumSizeEstimate(int size) {
        findAttributeValues(LinkFormat.MAX_SIZE_ESTIMATE).setOnly(Integer.toString(size));
    }

    public String getMaximumSizeEstimate() {
        return findAttributeValues(LinkFormat.MAX_SIZE_ESTIMATE).getFirst();
    }

    public void addContentType(int type) {
        findAttributeValues(LinkFormat.CONTENT_TYPE).add(Integer.toString(type));
    }

    public List<String> getContentTypes() {
        return getAttributeValues(LinkFormat.CONTENT_TYPE);
    }

    public void clearContentType() {
        this.attributes.remove(LinkFormat.CONTENT_TYPE);
    }

    public void setObservable() {
        findAttributeValues(LinkFormat.OBSERVABLE).setOnly("");
    }

    public boolean hasObservable() {
        return !getAttributeValues(LinkFormat.OBSERVABLE).isEmpty();
    }

    public void setAttribute(String attr, String value) {
        findAttributeValues(attr).setOnly(value);
    }

    public void addAttribute(String attr) {
        addAttribute(attr, "");
    }

    public void addAttribute(String attr, String value) {
        findAttributeValues(attr).add(value);
    }

    public void clearAttribute(String attr) {
        this.attributes.remove(attr);
    }

    public boolean containsAttribute(String attr) {
        return this.attributes.containsKey(attr);
    }

    public Set<String> getAttributeKeySet() {
        return this.attributes.keySet();
    }

    public List<String> getAttributeValues(String attr) {
        AttributeValues list = this.attributes.get(attr);
        if (list == null) {
            return Collections.emptyList();
        }
        return list.getAll();
    }

    private AttributeValues findAttributeValues(String attr) {
        AttributeValues list = this.attributes.get(attr);
        if (list == null) {
            list = new AttributeValues();
            AttributeValues prev = this.attributes.putIfAbsent(attr, list);
            if (prev != null) {
                return prev;
            }
        }
        return list;
    }

    /* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-californium-core.jar:org/eclipse/californium/core/server/resources/ResourceAttributes$AttributeValues.class */
    private static final class AttributeValues {
        private final List<String> list;

        private AttributeValues() {
            this.list = Collections.synchronizedList(new LinkedList());
        }

        /* JADX INFO: Access modifiers changed from: private */
        public List<String> getAll() {
            return this.list;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void add(String value) {
            this.list.add(value);
        }

        /* JADX INFO: Access modifiers changed from: private */
        public synchronized String getFirst() {
            return this.list.isEmpty() ? "" : this.list.get(0);
        }

        /* JADX INFO: Access modifiers changed from: private */
        public synchronized void setOnly(String value) {
            this.list.clear();
            if (value != null) {
                this.list.add(value);
            }
        }
    }
}
