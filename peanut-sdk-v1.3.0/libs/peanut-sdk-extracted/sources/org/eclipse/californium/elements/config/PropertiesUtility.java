package org.eclipse.californium.elements.config;

import org.eclipse.californium.elements.util.SslContextUtil;
import org.eclipse.californium.elements.util.StringUtil;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-element-connector.jar:org/eclipse/californium/elements/config/PropertiesUtility.class */
public class PropertiesUtility {
    private static final String QUOTED = "=:#!\\";
    private static final String SUBSTITUDED = "\t\n\r\f";
    private static final String SUBSTITUDES = "tnrf";
    private static final char[] HEX_DIGIT = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};

    public static String normalize(String value, boolean escapeSpace) {
        int length = value.length();
        StringBuilder builder = new StringBuilder(length);
        for (int index = 0; index < length; index++) {
            char aChar = value.charAt(index);
            if ((aChar == ' ' && escapeSpace) || QUOTED.indexOf(aChar) >= 0) {
                builder.append('\\');
                builder.append(aChar);
            } else {
                int substituteIndex = SUBSTITUDED.indexOf(aChar);
                if (substituteIndex >= 0) {
                    builder.append('\\');
                    builder.append(SUBSTITUDES.charAt(substituteIndex));
                } else if (aChar < ' ' || aChar >= 128) {
                    appendUnicode(aChar, builder);
                } else {
                    builder.append(aChar);
                }
            }
        }
        return builder.toString();
    }

    public static String normalizeComments(String comments) {
        char nextChar;
        if (comments == null) {
            return SslContextUtil.PARAMETER_SEPARATOR;
        }
        int length = comments.length();
        boolean eol = false;
        StringBuilder builder = new StringBuilder(length + 1);
        builder.append('#');
        builder.append(' ');
        int lineLength = 0;
        int index = 0;
        while (index < length) {
            char aChar = comments.charAt(index);
            if (aChar == '\r' && index + 1 < length && (nextChar = comments.charAt(index + 1)) == '\n') {
                index++;
                aChar = nextChar;
            }
            if (aChar == '\n' || aChar == '\r' || (lineLength > 64 && Character.isWhitespace(aChar))) {
                lineLength = 0;
                builder.append(StringUtil.lineSeparator());
                eol = true;
            } else {
                if (eol) {
                    if (aChar != '#' && aChar != '!') {
                        builder.append('#');
                        builder.append(' ');
                    }
                    eol = false;
                }
                if (aChar < ' ' || aChar >= 128) {
                    lineLength += 6;
                    appendUnicode(aChar, builder);
                } else {
                    lineLength++;
                    builder.append(aChar);
                }
            }
            index++;
        }
        return builder.toString();
    }

    public static void appendUnicode(char c, StringBuilder builder) {
        builder.append('\\').append('u');
        builder.append(HEX_DIGIT[(c >> '\f') & 15]);
        builder.append(HEX_DIGIT[(c >> '\b') & 15]);
        builder.append(HEX_DIGIT[(c >> 4) & 15]);
        builder.append(HEX_DIGIT[c & 15]);
    }
}
