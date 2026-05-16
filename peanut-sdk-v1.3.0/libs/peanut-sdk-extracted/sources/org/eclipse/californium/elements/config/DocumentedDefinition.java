package org.eclipse.californium.elements.config;

import org.eclipse.californium.elements.Definition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-element-connector.jar:org/eclipse/californium/elements/config/DocumentedDefinition.class */
public abstract class DocumentedDefinition<T> extends Definition<T> {
    protected static final Logger LOGGER = LoggerFactory.getLogger(DocumentedDefinition.class);
    private final String documentation;
    private final T defaultValue;

    public abstract String writeValue(T t);

    protected abstract T parseValue(String str) throws ValueException;

    DocumentedDefinition(String key, String documentation, Class<T> valueType, T defaultValue) {
        super(key, valueType);
        this.documentation = documentation;
        this.defaultValue = defaultValue;
    }

    public String getTypeName() {
        return getValueType().getSimpleName();
    }

    public String getDocumentation() {
        return this.documentation;
    }

    public T getDefaultValue() {
        return this.defaultValue;
    }

    public T readValue(String value) {
        String errorMessage;
        if (value == null) {
            String errorMessage2 = String.format("Key '%s': textual value must not be null!", getKey());
            throw new NullPointerException(errorMessage2);
        }
        if (useTrim()) {
            value = value.trim();
        }
        if (value.isEmpty()) {
            String errorMessage3 = String.format("Key '%s': textual value must not be empty!", getKey());
            throw new IllegalArgumentException(errorMessage3);
        }
        try {
            T result = parseValue(value);
            return checkValue(result);
        } catch (NumberFormatException e) {
            errorMessage = String.format("Key '%s': value '%s' is no %s", getKey(), value, getTypeName());
            throw new IllegalArgumentException(errorMessage);
        } catch (IllegalArgumentException e2) {
            errorMessage = String.format("Key '%s': value '%s' %s", getKey(), value, e2.getMessage());
            throw new IllegalArgumentException(errorMessage);
        } catch (ValueException e3) {
            errorMessage = String.format("Key '%s': %s", getKey(), e3.getMessage());
            throw new IllegalArgumentException(errorMessage);
        }
    }

    public T checkValue(T value) throws ValueException {
        return value;
    }

    protected boolean isAssignableFrom(Object value) {
        return getValueType().isInstance(value);
    }

    /* JADX WARN: Multi-variable type inference failed */
    protected Object checkRawValue(Object obj) throws ValueException {
        return checkValue(obj);
    }

    protected boolean useTrim() {
        return true;
    }

    /* JADX WARN: Multi-variable type inference failed */
    protected String write(Object obj) {
        return writeValue(obj);
    }
}
