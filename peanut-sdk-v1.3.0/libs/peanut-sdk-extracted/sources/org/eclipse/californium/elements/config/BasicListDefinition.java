package org.eclipse.californium.elements.config;

import java.util.Collections;
import java.util.List;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-element-connector.jar:org/eclipse/californium/elements/config/BasicListDefinition.class */
public abstract class BasicListDefinition<T> extends BasicDefinition<List<T>> {
    protected BasicListDefinition(String key, String documentation, List<T> defaultValue) {
        super(key, documentation, List.class, defaultValue);
    }

    @Override // org.eclipse.californium.elements.config.DocumentedDefinition
    public List<T> checkValue(List<T> value) throws ValueException {
        if (value != null) {
            try {
                value.remove(-1);
            } catch (IndexOutOfBoundsException e) {
                value = Collections.unmodifiableList(value);
            } catch (UnsupportedOperationException e2) {
            }
        }
        return value;
    }
}
