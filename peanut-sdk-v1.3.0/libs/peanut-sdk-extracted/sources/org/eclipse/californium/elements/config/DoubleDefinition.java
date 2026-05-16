package org.eclipse.californium.elements.config;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-element-connector.jar:org/eclipse/californium/elements/config/DoubleDefinition.class */
public class DoubleDefinition extends BasicDefinition<Double> {
    private final Double minimumValue;

    public DoubleDefinition(String key, String documentation) {
        super(key, documentation, Double.class, null);
        this.minimumValue = null;
    }

    public DoubleDefinition(String key, String documentation, Double defaultValue) {
        super(key, documentation, Double.class, defaultValue);
        this.minimumValue = null;
    }

    public DoubleDefinition(String key, String documentation, Double defaultValue, Double minimumValue) {
        super(key, documentation, Double.class, defaultValue);
        this.minimumValue = minimumValue;
    }

    @Override // org.eclipse.californium.elements.config.DocumentedDefinition
    public String getTypeName() {
        return "Double";
    }

    @Override // org.eclipse.californium.elements.config.DocumentedDefinition
    public String writeValue(Double value) {
        return value.toString();
    }

    @Override // org.eclipse.californium.elements.config.DocumentedDefinition
    public Double checkValue(Double value) throws ValueException {
        if (this.minimumValue != null && value != null && value.doubleValue() < this.minimumValue.doubleValue()) {
            throw new ValueException("Value " + value + " must be not less than " + this.minimumValue + "!");
        }
        return value;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // org.eclipse.californium.elements.config.DocumentedDefinition
    public Double parseValue(String value) {
        return Double.valueOf(Double.parseDouble(value));
    }
}
