package org.eclipse.californium.elements.config;

import java.util.List;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-element-connector.jar:org/eclipse/californium/elements/config/DefinitionUtils.class */
public class DefinitionUtils {
    public static <E extends Enum<?>> Class<E> getClass(E[] eArr) {
        if (eArr == null) {
            throw new NullPointerException("Enums must not be null!");
        }
        if (eArr.length == 0) {
            throw new IllegalArgumentException("Enums must not be empty!");
        }
        return (Class<E>) eArr[0].getClass();
    }

    public static String toString(List<String> list, boolean brackets) {
        if (list == null) {
            throw new NullPointerException("List must not be null!");
        }
        StringBuilder message = new StringBuilder();
        if (brackets) {
            message.append('[');
        }
        for (String in : list) {
            message.append(in).append(", ");
        }
        message.setLength(message.length() - 2);
        if (brackets) {
            message.append(']');
        }
        return message.toString();
    }

    public static <E extends Enum<?>> String toNames(List<E> list, boolean brackets) {
        if (list == null) {
            throw new NullPointerException("List must not be null!");
        }
        StringBuilder message = new StringBuilder();
        if (brackets) {
            message.append('[');
        }
        for (E in : list) {
            message.append(in.name()).append(", ");
        }
        message.setLength(message.length() - 2);
        if (brackets) {
            message.append(']');
        }
        return message.toString();
    }

    public static <E extends Enum<?>> E toValue(String text, List<E> values) {
        if (text == null) {
            throw new NullPointerException("Text must not be null!");
        }
        if (values == null) {
            throw new NullPointerException("values must not be null!");
        }
        for (E in : values) {
            if (in.name().equals(text)) {
                return in;
            }
        }
        return null;
    }
}
