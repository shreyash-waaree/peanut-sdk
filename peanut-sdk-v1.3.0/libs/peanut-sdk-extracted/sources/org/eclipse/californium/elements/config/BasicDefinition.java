package org.eclipse.californium.elements.config;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-element-connector.jar:org/eclipse/californium/elements/config/BasicDefinition.class */
public abstract class BasicDefinition<T> extends DocumentedDefinition<T> {
    protected BasicDefinition(String key, String documentation, Class<T> valueType, T defaultValue) {
        super(key, documentation, valueType, defaultValue);
    }
}
