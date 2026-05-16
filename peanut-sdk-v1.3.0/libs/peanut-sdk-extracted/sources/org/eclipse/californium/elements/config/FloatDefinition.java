package org.eclipse.californium.elements.config;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-element-connector.jar:org/eclipse/californium/elements/config/FloatDefinition.class */
public class FloatDefinition extends BasicDefinition<Float> {
    private final Float minimumValue;

    public FloatDefinition(String key, String documentation) {
        super(key, documentation, Float.class, null);
        this.minimumValue = null;
    }

    public FloatDefinition(String key, String documentation, Float defaultValue) {
        super(key, documentation, Float.class, defaultValue);
        this.minimumValue = null;
    }

    public FloatDefinition(String key, String documentation, Float defaultValue, Float minimumValue) {
        super(key, documentation, Float.class, defaultValue);
        this.minimumValue = minimumValue;
    }

    @Override // org.eclipse.californium.elements.config.DocumentedDefinition
    public String getTypeName() {
        return "Float";
    }

    @Override // org.eclipse.californium.elements.config.DocumentedDefinition
    public String writeValue(Float value) {
        return value.toString();
    }

    @Override // org.eclipse.californium.elements.config.DocumentedDefinition
    public Float checkValue(Float value) throws ValueException {
        if (this.minimumValue != null && value != null && value.floatValue() < this.minimumValue.floatValue()) {
            throw new ValueException("Value " + value + " must be not less than " + this.minimumValue + "!");
        }
        return value;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // org.eclipse.californium.elements.config.DocumentedDefinition
    public Float parseValue(String value) {
        return Float.valueOf(Float.parseFloat(value));
    }
}
