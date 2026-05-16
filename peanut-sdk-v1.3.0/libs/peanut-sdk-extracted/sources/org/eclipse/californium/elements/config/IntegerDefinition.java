package org.eclipse.californium.elements.config;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-element-connector.jar:org/eclipse/californium/elements/config/IntegerDefinition.class */
public class IntegerDefinition extends BasicDefinition<Integer> {
    private final Integer minimumValue;

    public IntegerDefinition(String key, String documentation) {
        super(key, documentation, Integer.class, null);
        this.minimumValue = null;
    }

    public IntegerDefinition(String key, String documentation, Integer defaultValue) {
        super(key, documentation, Integer.class, defaultValue);
        this.minimumValue = null;
    }

    public IntegerDefinition(String key, String documentation, Integer defaultValue, Integer minimumValue) {
        super(key, documentation, Integer.class, defaultValue);
        this.minimumValue = minimumValue;
    }

    @Override // org.eclipse.californium.elements.config.DocumentedDefinition
    public String getTypeName() {
        return "Integer";
    }

    @Override // org.eclipse.californium.elements.config.DocumentedDefinition
    public String writeValue(Integer value) {
        return value.toString();
    }

    @Override // org.eclipse.californium.elements.config.DocumentedDefinition
    public Integer checkValue(Integer value) throws ValueException {
        if (this.minimumValue != null && value != null && value.intValue() < this.minimumValue.intValue()) {
            throw new ValueException("Value " + value + " must be not less than " + this.minimumValue + "!");
        }
        return value;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // org.eclipse.californium.elements.config.DocumentedDefinition
    public Integer parseValue(String value) {
        return Integer.valueOf(Integer.parseInt(value));
    }
}
