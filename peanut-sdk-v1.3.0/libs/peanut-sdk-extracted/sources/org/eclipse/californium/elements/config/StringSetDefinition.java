package org.eclipse.californium.elements.config;

import java.util.Arrays;
import java.util.List;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-element-connector.jar:org/eclipse/californium/elements/config/StringSetDefinition.class */
public class StringSetDefinition extends BasicDefinition<String> {
    private final String defaultValue;
    private final List<String> values;
    private final String valuesDocumentation;

    public StringSetDefinition(String key, String documentation, String... values) {
        super(key, documentation, String.class, null);
        if (values == null) {
            throw new NullPointerException("Value set must not be null!");
        }
        if (values.length == 0) {
            throw new IllegalArgumentException("Value set must not be empty!");
        }
        for (String in : values) {
            if (in == null) {
                throw new IllegalArgumentException("Value set must not contain null!");
            }
        }
        boolean found = false;
        String defaultValue = values[0];
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
            this.values = Arrays.asList((String[]) Arrays.copyOfRange(values, 1, values.length));
        } else {
            this.defaultValue = null;
            this.values = Arrays.asList((String[]) Arrays.copyOf(values, values.length));
        }
        this.valuesDocumentation = DefinitionUtils.toString(this.values, true);
    }

    public StringSetDefinition(String key, String documentation, String defaultValue, String[] values) {
        super(key, documentation, String.class, null);
        if (values == null) {
            throw new NullPointerException("Value set must not be null!");
        }
        if (values.length == 0) {
            throw new IllegalArgumentException("Value set must not be empty!");
        }
        for (String in : values) {
            if (in == null) {
                throw new IllegalArgumentException("Value set must not contain null!");
            }
        }
        this.values = Arrays.asList((String[]) Arrays.copyOf(values, values.length));
        this.valuesDocumentation = DefinitionUtils.toString(this.values, true);
        try {
            this.defaultValue = checkValue(defaultValue);
        } catch (ValueException ex) {
            throw new IllegalArgumentException(ex.getMessage());
        }
    }

    @Override // org.eclipse.californium.elements.config.DocumentedDefinition
    public String getTypeName() {
        return "StringSet";
    }

    @Override // org.eclipse.californium.elements.config.DocumentedDefinition
    public String writeValue(String value) {
        return value;
    }

    @Override // org.eclipse.californium.elements.config.DocumentedDefinition
    public String getDocumentation() {
        return super.getDocumentation() + "\n" + this.valuesDocumentation + ".";
    }

    @Override // org.eclipse.californium.elements.config.DocumentedDefinition
    public String getDefaultValue() {
        return this.defaultValue;
    }

    @Override // org.eclipse.californium.elements.config.DocumentedDefinition
    public String checkValue(String value) throws ValueException {
        if (value == null || this.values.contains(value)) {
            return value;
        }
        throw new IllegalArgumentException(value + " is not in " + this.valuesDocumentation);
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
    public String parseValue(String value) throws ValueException {
        if (this.values.contains(value)) {
            return value;
        }
        throw new ValueException(value + " is not in " + this.valuesDocumentation);
    }
}
