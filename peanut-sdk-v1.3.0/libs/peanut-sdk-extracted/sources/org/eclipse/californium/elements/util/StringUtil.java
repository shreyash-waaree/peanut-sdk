package org.eclipse.californium.elements.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.util.regex.Pattern;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-element-connector.jar:org/eclipse/californium/elements/util/StringUtil.class */
public class StringUtil {
    public static final char NO_SEPARATOR = 0;
    public static final boolean SUPPORT_HOST_STRING;
    public static final String CALIFORNIUM_VERSION;
    private static final Pattern HOSTNAME_PATTERN = Pattern.compile("^(([a-zA-Z0-9]|[a-zA-Z0-9][a-zA-Z0-9\\-]*[a-zA-Z0-9])\\.)*([A-Za-z0-9]|[A-Za-z0-9][A-Za-z0-9\\-]*[A-Za-z0-9])$");
    private static final Pattern IP_PATTERN = Pattern.compile("^(\\[[0-9a-fA-F:]+(%\\w+)?\\]|[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3})$");
    public static final String lineSeparator = System.getProperty("line.separator");
    private static final char[] BIN_TO_HEX_ARRAY = "0123456789ABCDEF".toCharArray();
    private static final String[] TABS = new String[10];

    static {
        String tab = "";
        for (int i = 0; i < TABS.length; i++) {
            TABS[i] = tab;
            tab = tab + "\t";
        }
        boolean support = false;
        try {
            Method method = InetSocketAddress.class.getMethod("getHostString", new Class[0]);
            support = method != null;
        } catch (NoSuchMethodException e) {
        }
        SUPPORT_HOST_STRING = support;
        String version = null;
        Package pack = StringUtil.class.getPackage();
        if (pack != null) {
            version = pack.getImplementationVersion();
            if ("0.0".equals(version)) {
                version = null;
            }
        }
        CALIFORNIUM_VERSION = version;
    }

    public static String toHostString(InetSocketAddress socketAddress) {
        if (SUPPORT_HOST_STRING) {
            return socketAddress.getHostString();
        }
        InetAddress address = socketAddress.getAddress();
        if (address != null) {
            String textAddress = address.toString();
            if (textAddress.startsWith("/")) {
                return textAddress.substring(1);
            }
            return address.getHostName();
        }
        return socketAddress.getHostName();
    }

    public static String indentation(int indentIndex) {
        if (indentIndex < 0) {
            return "";
        }
        if (indentIndex >= TABS.length) {
            return TABS[TABS.length - 1];
        }
        return TABS[indentIndex];
    }

    public static String lineSeparator() {
        return lineSeparator;
    }

    public static char[] hex2CharArray(String hex) {
        if (hex == null) {
            return null;
        }
        int length = hex.length();
        if ((1 & length) != 0) {
            throw new IllegalArgumentException("'" + hex + "' has odd length!");
        }
        int length2 = length / 2;
        char[] result = new char[length2];
        int indexSrc = 0;
        for (int indexDest = 0; indexDest < length2; indexDest++) {
            int digit = Character.digit(hex.charAt(indexSrc), 16);
            if (digit < 0) {
                throw new IllegalArgumentException("'" + hex + "' digit " + indexSrc + " is not hexadecimal!");
            }
            result[indexDest] = (char) (digit << 4);
            int indexSrc2 = indexSrc + 1;
            int digit2 = Character.digit(hex.charAt(indexSrc2), 16);
            if (digit2 < 0) {
                throw new IllegalArgumentException("'" + hex + "' digit " + indexSrc2 + " is not hexadecimal!");
            }
            int i = indexDest;
            result[i] = (char) (result[i] | ((char) digit2));
            indexSrc = indexSrc2 + 1;
        }
        return result;
    }

    public static String charArray2hex(char[] charArray) {
        if (charArray != null) {
            int length = charArray.length;
            StringBuilder builder = new StringBuilder(length * 2);
            for (char c : charArray) {
                int value = c & 255;
                builder.append(BIN_TO_HEX_ARRAY[value >>> 4]);
                builder.append(BIN_TO_HEX_ARRAY[value & 15]);
            }
            return builder.toString();
        }
        return null;
    }

    public static byte[] hex2ByteArray(String hex) {
        if (hex == null) {
            return null;
        }
        int length = hex.length();
        if ((1 & length) != 0) {
            throw new IllegalArgumentException("'" + hex + "' has odd length!");
        }
        int length2 = length / 2;
        byte[] result = new byte[length2];
        int indexSrc = 0;
        for (int indexDest = 0; indexDest < length2; indexDest++) {
            int digit = Character.digit(hex.charAt(indexSrc), 16);
            if (digit < 0) {
                throw new IllegalArgumentException("'" + hex + "' digit " + indexSrc + " is not hexadecimal!");
            }
            result[indexDest] = (byte) (digit << 4);
            int indexSrc2 = indexSrc + 1;
            int digit2 = Character.digit(hex.charAt(indexSrc2), 16);
            if (digit2 < 0) {
                throw new IllegalArgumentException("'" + hex + "' digit " + indexSrc2 + " is not hexadecimal!");
            }
            int i = indexDest;
            result[i] = (byte) (result[i] | ((byte) digit2));
            indexSrc = indexSrc2 + 1;
        }
        return result;
    }

    public static String byteArray2Hex(byte[] byteArray) {
        if (byteArray == null) {
            return null;
        }
        if (byteArray.length == 0) {
            return "";
        }
        return byteArray2HexString(byteArray, (char) 0, 0);
    }

    public static String byteArray2HexString(byte[] byteArray) {
        return byteArray2HexString(byteArray, (char) 0, 0);
    }

    public static String byteArray2HexString(byte[] byteArray, char sep, int max) {
        if (byteArray != null && byteArray.length != 0) {
            if (max == 0 || max > byteArray.length) {
                max = byteArray.length;
            }
            StringBuilder builder = new StringBuilder(max * (sep == 0 ? 2 : 3));
            for (int index = 0; index < max; index++) {
                int value = byteArray[index] & 255;
                builder.append(BIN_TO_HEX_ARRAY[value >>> 4]);
                builder.append(BIN_TO_HEX_ARRAY[value & 15]);
                if (sep != 0 && index < max - 1) {
                    builder.append(sep);
                }
            }
            return builder.toString();
        }
        return "--";
    }

    public static byte[] base64ToByteArray(String base64) {
        int pad = base64.length() % 4;
        if (pad > 0) {
            int pad2 = 4 - pad;
            if (pad2 == 1) {
                base64 = base64 + "=";
            } else if (pad2 == 2) {
                base64 = base64 + "==";
            } else {
                throw new IllegalArgumentException("'" + base64 + "' invalid base64!");
            }
        }
        try {
            return Base64.decode(base64);
        } catch (IOException e) {
            return Bytes.EMPTY;
        }
    }

    public static String byteArrayToBase64(byte[] bytes) {
        return Base64.encodeBytes(bytes);
    }

    public static String trunc(String text, int maxLength) {
        if (text != null && maxLength > 0 && maxLength < text.length()) {
            return text.substring(0, maxLength);
        }
        return text;
    }

    public static String toDisplayString(byte[] data, int limit) {
        if (data == null) {
            return "<no data>";
        }
        if (data.length == 0) {
            return "<empty data>";
        }
        if (data.length < limit) {
            limit = data.length;
        }
        boolean text = true;
        int length = data.length;
        int i = 0;
        while (true) {
            if (i < length) {
                byte b = data[i];
                if (32 > b) {
                    switch (b) {
                        case 9:
                        case 10:
                        case 13:
                            break;
                        case 11:
                        case 12:
                        default:
                            text = false;
                            break;
                    }
                }
                i++;
            }
        }
        if (text) {
            CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder();
            decoder.onMalformedInput(CodingErrorAction.REPORT);
            decoder.onUnmappableCharacter(CodingErrorAction.REPORT);
            ByteBuffer in = ByteBuffer.wrap(data);
            CharBuffer out = CharBuffer.allocate(limit);
            CoderResult result = decoder.decode(in, out, true);
            decoder.flush(out);
            out.flip();
            if (CoderResult.OVERFLOW == result) {
                return "\"" + ((Object) out) + "\".. " + data.length + " bytes";
            }
            if (!result.isError()) {
                return "\"" + ((Object) out) + "\"";
            }
        }
        String hex = byteArray2HexString(data, ' ', limit);
        if (data.length > limit) {
            hex = hex + ".." + data.length + " bytes";
        }
        return hex;
    }

    public static String toString(InetAddress address) {
        if (address == null) {
            return null;
        }
        return address.getHostAddress();
    }

    public static String toString(InetSocketAddress address) {
        String host;
        if (address == null) {
            return null;
        }
        if (SUPPORT_HOST_STRING) {
            host = toHostString(address);
        } else {
            InetAddress addr = address.getAddress();
            if (addr != null) {
                host = toString(addr);
            } else {
                host = "<unresolved>";
            }
        }
        if (address.getAddress() instanceof Inet6Address) {
            return "[" + host + "]:" + address.getPort();
        }
        return host + ":" + address.getPort();
    }

    public static String toString(SocketAddress address) {
        if (address == null) {
            return null;
        }
        if (address instanceof InetSocketAddress) {
            return toString((InetSocketAddress) address);
        }
        return address.toString();
    }

    public static String toDisplayString(InetSocketAddress address) {
        String name;
        if (address == null) {
            return null;
        }
        InetAddress addr = address.getAddress();
        if (addr != null && addr.isAnyLocalAddress()) {
            return "port " + address.getPort();
        }
        String name2 = SUPPORT_HOST_STRING ? toHostString(address) : "";
        String host = addr != null ? toString(addr) : "<unresolved>";
        if (name2.equals(host)) {
            name = "";
        } else {
            name = name2 + "/";
        }
        if (address.getAddress() instanceof Inet6Address) {
            return name + "[" + host + "]:" + address.getPort();
        }
        return name + host + ":" + address.getPort();
    }

    public static Object toLog(final SocketAddress address) {
        if (address == null) {
            return null;
        }
        return new Object() { // from class: org.eclipse.californium.elements.util.StringUtil.1
            public String toString() {
                if (address instanceof InetSocketAddress) {
                    return StringUtil.toDisplayString((InetSocketAddress) address);
                }
                return address.toString();
            }
        };
    }

    public static boolean isValidHostName(String name) {
        if (name == null) {
            return false;
        }
        return HOSTNAME_PATTERN.matcher(name).matches();
    }

    public static boolean isLiteralIpAddress(String address) {
        if (address == null) {
            return false;
        }
        return IP_PATTERN.matcher(address).matches();
    }

    public static String getUriHostname(InetAddress address) throws URISyntaxException {
        int pos;
        if (address == null) {
            throw new NullPointerException("address must not be null!");
        }
        String host = address.getHostAddress();
        if (address instanceof Inet6Address) {
            Inet6Address address6 = (Inet6Address) address;
            if ((address6.getScopedInterface() != null || address6.getScopeId() > 0) && (pos = host.indexOf(37)) > 0 && pos + 1 < host.length()) {
                String scope = host.substring(pos + 1);
                String hostAddress = host.substring(0, pos);
                host = hostAddress + "%25" + scope;
                try {
                    new URI(null, null, host, -1, null, null, null);
                } catch (URISyntaxException e) {
                    String scope2 = scope.replaceAll("[-._~]", "");
                    if (!scope2.isEmpty()) {
                        host = hostAddress + "%25" + scope2;
                        try {
                            new URI(null, null, host, -1, null, null, null);
                        } catch (URISyntaxException e2) {
                            throw e;
                        }
                    } else {
                        host = hostAddress;
                    }
                }
            }
        }
        return host;
    }

    public static String normalizeLoggingTag(String tag) {
        if (tag == null) {
            tag = "";
        } else if (!tag.isEmpty() && !tag.endsWith(" ")) {
            tag = tag + " ";
        }
        return tag;
    }

    public static String toDisplayString(Certificate cert) {
        int indentIndex = 0;
        String[] lines = cert.toString().split("\n");
        StringBuilder text = new StringBuilder();
        for (String str : lines) {
            String line = str.trim();
            if (!line.isEmpty()) {
                int indent = indentDelta(line);
                if (indent < 0 && line.length() == 1) {
                    indentIndex += indent;
                    indent = 0;
                }
                text.append(indentation(indentIndex)).append(line).append("\n");
                indentIndex += indent;
            } else {
                text.append("\n");
            }
        }
        return text.toString();
    }

    private static int indentDelta(String line) {
        int index = 0;
        int i = line.length();
        while (i > 0) {
            i--;
            char c = line.charAt(i);
            if (c == '[') {
                index++;
            } else if (c == ']') {
                index--;
            }
        }
        if (index != 0 && line.matches("\\d+:\\s+.*")) {
            return 0;
        }
        return index;
    }

    public static String toDisplayString(PublicKey publicKey) {
        return publicKey.toString().replaceAll("\n\\s+", "\n");
    }

    public static String getConfiguration(String name) {
        String property;
        String value = System.getenv(name);
        if ((value == null || value.isEmpty()) && (property = System.getProperty(name)) != null) {
            value = property;
        }
        return value;
    }

    public static Long getConfigurationLong(String name) {
        String value = getConfiguration(name);
        if (value != null && !value.isEmpty()) {
            try {
                return Long.valueOf(value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    public static Boolean getConfigurationBoolean(String name) {
        String value = getConfiguration(name);
        if (value != null && !value.isEmpty()) {
            return Boolean.valueOf(value);
        }
        return null;
    }

    public static String readFile(File file, String defaultText) {
        String content = defaultText;
        if (file.canRead()) {
            try {
                FileReader reader = new FileReader(file);
                try {
                    BufferedReader lineReader = new BufferedReader(reader);
                    content = lineReader.readLine();
                    lineReader.close();
                    reader.close();
                } catch (Throwable th) {
                    try {
                        reader.close();
                    } catch (Throwable th2) {
                        th.addSuppressed(th2);
                    }
                    throw th;
                }
            } catch (FileNotFoundException e) {
            } catch (IOException e2) {
            }
        }
        return content;
    }
}
