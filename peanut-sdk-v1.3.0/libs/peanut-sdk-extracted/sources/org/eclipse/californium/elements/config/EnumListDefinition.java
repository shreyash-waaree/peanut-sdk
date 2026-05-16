package org.eclipse.californium.elements.config;

import com.keenon.sdk.constant.ApiConstants;
import java.lang.Enum;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-element-connector.jar:org/eclipse/californium/elements/config/EnumListDefinition.class */
public class EnumListDefinition<E extends Enum<?>> extends BasicListDefinition<E> {
    private final List<E> defaultValue;
    private final Class<E> itemType;
    private final String typeName;
    private final List<E> values;
    private final String valuesDocumentation;
    private final int minimumItems;

    public EnumListDefinition(String key, String documentation, E[] values) {
        this(key, documentation, null, 0, values);
    }

    public EnumListDefinition(String key, String documentation, List<E> defaultValue, E[] values) {
        this(key, documentation, defaultValue, 0, values);
    }

    public EnumListDefinition(String str, String str2, List<E> list, int i, E[] eArr) {
        super(str, str2, null);
        if (eArr == null) {
            throw new NullPointerException("Enum set must not be null!");
        }
        if (eArr.length == 0) {
            throw new IllegalArgumentException("Enum set must not be empty!");
        }
        for (E e : eArr) {
            if (e == null) {
                throw new IllegalArgumentException("Enum set must not contain null!");
            }
        }
        this.itemType = (Class<E>) eArr[0].getClass();
        this.typeName = "List<" + this.itemType.getSimpleName() + ">";
        this.values = Arrays.asList((Enum[]) Arrays.copyOf(eArr, eArr.length));
        this.valuesDocumentation = DefinitionUtils.toNames(this.values, true);
        this.minimumItems = i;
        try {
            this.defaultValue = checkValue((List) list);
        } catch (ValueException e2) {
            throw new IllegalArgumentException(e2.getMessage());
        }
    }

    @Override // org.eclipse.californium.elements.config.DocumentedDefinition
    public String getTypeName() {
        return this.typeName;
    }

    @Override // org.eclipse.californium.elements.config.DocumentedDefinition
    public String writeValue(List<E> value) {
        return DefinitionUtils.toNames(value, false);
    }

    /* JADX WARN: Multi-variable type inference failed */
    @Override // org.eclipse.californium.elements.config.BasicListDefinition, org.eclipse.californium.elements.config.DocumentedDefinition
    public List<E> checkValue(List<E> list) throws ValueException {
        if (list != 0) {
            if (list.size() < this.minimumItems) {
                if (list.isEmpty()) {
                    throw new ValueException("Values must not be empty!");
                }
                throw new ValueException("Values with " + list.size() + " items must not contain less items than " + this.minimumItems + "!");
            }
            for (E e : list) {
                if (!this.values.contains(e)) {
                    if (this.itemType.isInstance(e)) {
                        throw new IllegalArgumentException(e + " is not in " + this.valuesDocumentation);
                    }
                    throw new IllegalArgumentException(e + " is no " + this.itemType.getSimpleName());
                }
            }
        }
        return super.checkValue((List) list);
    }

    @Override // org.eclipse.californium.elements.config.DocumentedDefinition
    public List<E> getDefaultValue() {
        return this.defaultValue;
    }

    @Override // org.eclipse.californium.elements.config.DocumentedDefinition
    public String getDocumentation() {
        return super.getDocumentation() + "\nList of " + this.valuesDocumentation + ".";
    }

    @Override // org.eclipse.californium.elements.config.DocumentedDefinition
    protected boolean isAssignableFrom(Object value) {
        if (value instanceof List) {
            for (Object item : (List) value) {
                if (!this.itemType.isInstance(item)) {
                    throw new IllegalArgumentException(item + " is no " + this.itemType.getSimpleName());
                }
            }
            return true;
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // org.eclipse.californium.elements.config.DocumentedDefinition
    public List<E> parseValue(String value) throws ValueException {
        String[] list = value.split(ApiConstants.DELIMITER_COMMA);
        ArrayList arrayList = new ArrayList(list.length);
        for (String str : list) {
            String valueItem = str.trim();
            Enum value2 = DefinitionUtils.toValue(valueItem, this.values);
            if (value2 == null) {
                throw new ValueException(valueItem + " is not in " + this.valuesDocumentation);
            }
            arrayList.add(value2);
        }
        return arrayList;
    }
}
