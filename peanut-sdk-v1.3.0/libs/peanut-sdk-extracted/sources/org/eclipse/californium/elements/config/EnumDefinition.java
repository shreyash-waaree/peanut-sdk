package org.eclipse.californium.elements.config;

import java.lang.Enum;
import java.util.Arrays;
import java.util.List;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-element-connector.jar:org/eclipse/californium/elements/config/EnumDefinition.class */
public class EnumDefinition<E extends Enum<?>> extends BasicDefinition<E> {
    private final E defaultValue;
    private final List<E> values;
    private final String valuesDocumentation;

    public EnumDefinition(String key, String documentation, E... values) {
        super(key, documentation, DefinitionUtils.getClass(values), null);
        if (values == null) {
            throw new NullPointerException("Enum set must not be null!");
        }
        if (values.length == 0) {
            throw new IllegalArgumentException("Enum set must not be empty!");
        }
        for (E in : values) {
            if (in == null) {
                throw new IllegalArgumentException("Enum set must not contain null!");
            }
        }
        boolean found = false;
        E defaultValue = values[0];
        int index = 1;
        while (true) {
            if (index >= values.length) {
                break;
            }
            if (!values[index].equals(defaultValue)) {
                index++;
            } else {
                found = true;
                break;
            }
        }
        if (found) {
            this.defaultValue = defaultValue;
            this.values = Arrays.asList((Enum[]) Arrays.copyOfRange(values, 1, values.length));
        } else {
            this.defaultValue = null;
            this.values = Arrays.asList((Enum[]) Arrays.copyOf(values, values.length));
        }
        this.valuesDocumentation = DefinitionUtils.toNames(Arrays.asList(values), true);
    }

    public EnumDefinition(String key, String documentation, E defaultValue, E[] values) {
        super(key, documentation, DefinitionUtils.getClass(values), null);
        if (values == null) {
            throw new NullPointerException("Enum set must not be null!");
        }
        if (values.length == 0) {
            throw new IllegalArgumentException("Enum set must not be empty!");
        }
        for (E in : values) {
            if (in == null) {
                throw new IllegalArgumentException("Enum set must not contain null!");
            }
        }
        this.defaultValue = defaultValue;
        this.values = Arrays.asList((Enum[]) Arrays.copyOf(values, values.length));
        this.valuesDocumentation = DefinitionUtils.toNames(this.values, true);
        if (defaultValue != null) {
            isAssignableFrom(defaultValue);
        }
    }

    @Override // org.eclipse.californium.elements.config.DocumentedDefinition
    public String writeValue(E value) {
        return value.name();
    }

    @Override // org.eclipse.californium.elements.config.DocumentedDefinition
    public String getDocumentation() {
        return super.getDocumentation() + "\n" + this.valuesDocumentation + ".";
    }

    @Override // org.eclipse.californium.elements.config.DocumentedDefinition
    public E getDefaultValue() {
        return this.defaultValue;
    }

    @Override // org.eclipse.californium.elements.config.DocumentedDefinition
    protected boolean isAssignableFrom(Object value) {
        if (this.values.contains(value)) {
            return true;
        }
        if (super.isAssignableFrom(value)) {
            throw new IllegalArgumentException(value + " is not in " + this.valuesDocumentation);
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // org.eclipse.californium.elements.config.DocumentedDefinition
    public E parseValue(String str) throws ValueException {
        E e = (E) DefinitionUtils.toValue(str, this.values);
        if (e == null) {
            throw new ValueException(str + " is not in " + this.valuesDocumentation);
        }
        return e;
    }
}
