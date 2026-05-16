package org.eclipse.californium.elements.config;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-element-connector.jar:org/eclipse/californium/elements/config/BooleanDefinition.class */
public class BooleanDefinition extends BasicDefinition<Boolean> {
    public BooleanDefinition(String key, String documentation) {
        super(key, documentation, Boolean.class, null);
    }

    public BooleanDefinition(String key, String documentation, Boolean defaultValue) {
        super(key, documentation, Boolean.class, defaultValue);
    }

    @Override // org.eclipse.californium.elements.config.DocumentedDefinition
    public String getTypeName() {
        return "Boolean";
    }

    @Override // org.eclipse.californium.elements.config.DocumentedDefinition
    public String writeValue(Boolean value) {
        return value.toString();
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // org.eclipse.californium.elements.config.DocumentedDefinition
    public Boolean parseValue(String value) {
        return Boolean.valueOf(Boolean.parseBoolean(value));
    }
}
