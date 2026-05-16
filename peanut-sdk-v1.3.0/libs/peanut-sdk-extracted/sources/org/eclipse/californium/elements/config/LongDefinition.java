package org.eclipse.californium.elements.config;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-element-connector.jar:org/eclipse/californium/elements/config/LongDefinition.class */
public class LongDefinition extends BasicDefinition<Long> {
    private final Long minimumValue;

    public LongDefinition(String key, String documentation) {
        super(key, documentation, Long.class, null);
        this.minimumValue = null;
    }

    public LongDefinition(String key, String documentation, Long defaultValue) {
        super(key, documentation, Long.class, defaultValue);
        this.minimumValue = null;
    }

    public LongDefinition(String key, String documentation, Long defaultValue, Long minimumValue) {
        super(key, documentation, Long.class, defaultValue);
        this.minimumValue = minimumValue;
    }

    @Override // org.eclipse.californium.elements.config.DocumentedDefinition
    public String getTypeName() {
        return "Long";
    }

    @Override // org.eclipse.californium.elements.config.DocumentedDefinition
    public String writeValue(Long value) {
        return value.toString();
    }

    @Override // org.eclipse.californium.elements.config.DocumentedDefinition
    public Long checkValue(Long value) throws ValueException {
        if (this.minimumValue != null && value != null && value.longValue() < this.minimumValue.longValue()) {
            throw new ValueException("Value " + value + " must be not less than " + this.minimumValue + "!");
        }
        return value;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // org.eclipse.californium.elements.config.DocumentedDefinition
    public Long parseValue(String value) {
        return Long.valueOf(Long.parseLong(value));
    }
}
