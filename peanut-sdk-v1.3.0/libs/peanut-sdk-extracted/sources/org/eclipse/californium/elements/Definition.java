package org.eclipse.californium.elements;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-element-connector.jar:org/eclipse/californium/elements/Definition.class */
public class Definition<T> {
    private final String key;
    private final Class<T> valueType;

    public Definition(String key, Class<T> valueType) {
        this(key, valueType, null);
    }

    public Definition(String key, Class<T> valueType, Definitions<Definition<?>> definitions) {
        if (key == null) {
            throw new NullPointerException("Key must not be null!");
        }
        if (valueType == null) {
            throw new NullPointerException("Value Type must not be null!");
        }
        if (key.isEmpty()) {
            throw new IllegalArgumentException("Key must not be empty!");
        }
        this.key = key;
        this.valueType = valueType;
        if (definitions != null) {
            definitions.add(this);
        }
    }

    public final Class<T> getValueType() {
        return this.valueType;
    }

    public final String getKey() {
        return this.key;
    }

    public String toString() {
        return this.key;
    }
}
