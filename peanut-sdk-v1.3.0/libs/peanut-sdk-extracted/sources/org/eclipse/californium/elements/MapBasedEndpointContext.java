package org.eclipse.californium.elements;

import java.net.InetSocketAddress;
import java.security.Principal;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.eclipse.californium.elements.util.Bytes;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-element-connector.jar:org/eclipse/californium/elements/MapBasedEndpointContext.class */
public class MapBasedEndpointContext extends AddressEndpointContext {
    public static final Definitions<Definition<?>> ATTRIBUTE_DEFINITIONS = new Definitions<Definition<?>>("EndpointContextAttributes") { // from class: org.eclipse.californium.elements.MapBasedEndpointContext.1
        @Override // org.eclipse.californium.elements.Definitions
        public Definition<?> addIfAbsent(Definition<?> definition) {
            if (definition == null) {
                throw new NullPointerException();
            }
            Class<?> valueType = definition.getValueType();
            if (valueType != String.class && valueType != Integer.class && valueType != Long.class && valueType != Boolean.class && valueType != InetSocketAddress.class && !Bytes.class.isAssignableFrom(valueType)) {
                throw new IllegalArgumentException(valueType + " is not supported, only String, Integer, Long, Boolean, InetSocketAddress and Bytes!");
            }
            return super.addIfAbsent(definition);
        }
    };
    public static final String KEY_PREFIX_NONE_CRITICAL = "*";
    private final boolean hasCriticalEntries;
    private final Map<Definition<?>, Object> entries;

    public MapBasedEndpointContext(InetSocketAddress peerAddress, Principal peerIdentity, Attributes attributes) {
        this(peerAddress, null, peerIdentity, attributes);
    }

    public MapBasedEndpointContext(InetSocketAddress peerAddress, String virtualHost, Principal peerIdentity, Attributes attributes) {
        super(peerAddress, virtualHost, peerIdentity);
        if (attributes == null) {
            throw new NullPointerException("missing attributes map, must not be null!");
        }
        attributes.lock();
        this.entries = Collections.unmodifiableMap(attributes.entries);
        this.hasCriticalEntries = findCriticalEntries(this.entries);
    }

    private static final boolean findCriticalEntries(Map<Definition<?>, Object> attributes) {
        for (Definition<?> key : attributes.keySet()) {
            if (!key.getKey().startsWith("*")) {
                return true;
            }
        }
        return false;
    }

    @Override // org.eclipse.californium.elements.AddressEndpointContext, org.eclipse.californium.elements.EndpointContext
    public <T> T get(Definition<T> definition) {
        return (T) this.entries.get(definition);
    }

    @Override // org.eclipse.californium.elements.AddressEndpointContext, org.eclipse.californium.elements.EndpointContext
    public Map<Definition<?>, Object> entries() {
        return this.entries;
    }

    @Override // org.eclipse.californium.elements.AddressEndpointContext, org.eclipse.californium.elements.EndpointContext
    public boolean hasCriticalEntries() {
        return this.hasCriticalEntries;
    }

    @Override // org.eclipse.californium.elements.AddressEndpointContext
    public String toString() {
        return String.format("MAP(%s)", getPeerAddressAsString());
    }

    public static MapBasedEndpointContext setEntries(EndpointContext context, Attributes attributes) {
        return new MapBasedEndpointContext(context.getPeerAddress(), context.getVirtualHost(), context.getPeerIdentity(), attributes);
    }

    public static MapBasedEndpointContext addEntries(EndpointContext context, Attributes attributes) {
        Attributes allAttributes = new Attributes(context.entries());
        allAttributes.addAll(attributes);
        return setEntries(context, allAttributes);
    }

    public static MapBasedEndpointContext removeEntries(EndpointContext context, Definition<?>... attributes) {
        if (attributes == null) {
            throw new NullPointerException("attributes must not null!");
        }
        Attributes entries = new Attributes(context.entries());
        for (int index = 0; index < attributes.length; index++) {
            try {
                Definition<?> key = attributes[index];
                if (!entries.remove(key)) {
                    throw new IllegalArgumentException(index + ". key '" + key + "' is not contained");
                }
            } catch (IllegalArgumentException ex) {
                throw new IllegalArgumentException(index + ". " + ex.getMessage());
            } catch (NullPointerException ex2) {
                throw new NullPointerException(index + ". " + ex2.getMessage());
            }
        }
        return new MapBasedEndpointContext(context.getPeerAddress(), context.getVirtualHost(), context.getPeerIdentity(), entries);
    }

    /* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-element-connector.jar:org/eclipse/californium/elements/MapBasedEndpointContext$Attributes.class */
    public static final class Attributes {
        private final Map<Definition<?>, Object> entries;
        private volatile boolean lock;

        public Attributes() {
            this.entries = new HashMap();
        }

        private Attributes(Map<Definition<?>, Object> entries) {
            this.entries = new HashMap();
            this.entries.putAll(entries);
        }

        public Attributes lock() {
            this.lock = true;
            return this;
        }

        public Attributes addAll(Attributes attributes) {
            if (this.lock) {
                throw new IllegalStateException("Already in use!");
            }
            this.entries.putAll(attributes.entries);
            return this;
        }

        public <T> Attributes add(Definition<T> definition, T value) {
            if (this.lock) {
                throw new IllegalStateException("Already in use!");
            }
            if (null == definition) {
                throw new NullPointerException("key is null");
            }
            if (null == value && !definition.getKey().startsWith("*")) {
                throw new NullPointerException("value is null");
            }
            if (!MapBasedEndpointContext.ATTRIBUTE_DEFINITIONS.contains(definition)) {
                throw new IllegalArgumentException(definition + " is not supported!");
            }
            if (value == null) {
                this.entries.remove(definition);
            } else if (this.entries.put(definition, value) != null) {
                throw new IllegalArgumentException("'" + definition + "' already contained!");
            }
            return this;
        }

        public <T> boolean contains(Definition<T> key) {
            if (null == key) {
                throw new NullPointerException("key is null");
            }
            return this.entries.containsKey(key);
        }

        public <T> boolean remove(Definition<T> key) {
            if (this.lock) {
                throw new IllegalStateException("Already in use!");
            }
            if (null == key) {
                throw new NullPointerException("key is null");
            }
            return this.entries.remove(key) != null;
        }

        public int hashCode() {
            return this.entries.hashCode();
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj != null && (obj instanceof Attributes)) {
                Attributes other = (Attributes) obj;
                return this.entries.equals(other.entries);
            }
            return false;
        }
    }
}
