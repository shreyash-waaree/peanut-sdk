package org.eclipse.californium.elements;

import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.eclipse.californium.elements.Definition;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-element-connector.jar:org/eclipse/californium/elements/Definitions.class */
public class Definitions<T extends Definition<?>> implements Iterable<T> {
    private final String name;
    private final ConcurrentMap<String, T> definitions;

    public Definitions(String name) {
        this.definitions = new ConcurrentHashMap();
        this.name = name;
    }

    public Definitions(Definitions<T> definitions) {
        this(definitions.getName(), definitions);
    }

    public Definitions(String name, Definitions<T> definitions) {
        this.definitions = new ConcurrentHashMap();
        this.name = name;
        this.definitions.putAll(definitions.definitions);
    }

    public String getName() {
        return this.name;
    }

    public Definitions<T> add(T definition) {
        Definition definitionAddIfAbsent = addIfAbsent(definition);
        if (definitionAddIfAbsent != null && definitionAddIfAbsent != definition) {
            throw new IllegalArgumentException(this.name + " already contains " + definition.getKey() + "!");
        }
        return this;
    }

    public T addIfAbsent(T definition) {
        if (definition == null) {
            throw new NullPointerException();
        }
        return this.definitions.putIfAbsent(definition.getKey(), definition);
    }

    public boolean contains(T definition) {
        return definition == get(definition.getKey());
    }

    public T get(String key) {
        return this.definitions.get(key);
    }

    @Override // java.lang.Iterable
    public Iterator<T> iterator() {
        return this.definitions.values().iterator();
    }
}
