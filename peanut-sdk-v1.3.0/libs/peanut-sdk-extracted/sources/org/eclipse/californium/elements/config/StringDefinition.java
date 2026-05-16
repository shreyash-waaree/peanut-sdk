package org.eclipse.californium.elements.config;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-element-connector.jar:org/eclipse/californium/elements/config/StringDefinition.class */
public class StringDefinition extends BasicDefinition<String> {
    public StringDefinition(String key, String documentation) {
        super(key, documentation, String.class, null);
    }

    public StringDefinition(String key, String documentation, String defaultValue) {
        super(key, documentation, String.class, defaultValue);
    }

    @Override // org.eclipse.californium.elements.config.DocumentedDefinition
    public String getTypeName() {
        return "String";
    }

    @Override // org.eclipse.californium.elements.config.DocumentedDefinition
    public String writeValue(String value) {
        return value;
    }

    @Override // org.eclipse.californium.elements.config.DocumentedDefinition
    protected boolean useTrim() {
        return false;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // org.eclipse.californium.elements.config.DocumentedDefinition
    public String parseValue(String value) {
        return value;
    }
}
