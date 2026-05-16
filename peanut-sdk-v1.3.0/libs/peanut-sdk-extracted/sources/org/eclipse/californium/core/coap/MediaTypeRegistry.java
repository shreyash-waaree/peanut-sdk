package org.eclipse.californium.core.coap;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-californium-core.jar:org/eclipse/californium/core/coap/MediaTypeRegistry.class */
public class MediaTypeRegistry {
    public static final int TEXT_PLAIN = 0;
    public static final int APPLICATION_LINK_FORMAT = 40;
    public static final int APPLICATION_XML = 41;
    public static final int APPLICATION_OCTET_STREAM = 42;
    public static final int APPLICATION_XMPP_XML = 46;
    public static final int APPLICATION_EXI = 47;
    public static final int APPLICATION_JSON = 50;
    public static final int APPLICATION_CBOR = 60;
    public static final int APPLICATION_SENML_JSON = 110;
    public static final int APPLICATION_SENML_CBOR = 112;
    public static final int APPLICATION_VND_OMA_LWM2M_TLV = 11542;
    public static final int APPLICATION_VND_OMA_LWM2M_JSON = 11543;
    public static final int MAX_TYPE = 65535;
    public static final int UNDEFINED = -1;
    private static final Map<Integer, MediaTypeDefintion> registry = new ConcurrentHashMap();

    static {
        addPrintable(0, "text/plain", "txt", true);
        addPrintable(40, "application/link-format", "wlnk", false);
        addPrintable(41, "application/xml", "xml", false);
        addNonePrintable(42, "application/octet-stream", "bin");
        addPrintable(46, "application/xmpp+xml", "xmpp", false);
        addNonePrintable(47, "application/exi", "exi");
        addPrintable(50, "application/json", "json", false);
        addNonePrintable(60, "application/cbor", "cbor");
        addPrintable(110, "application/senml+json", "json", false);
        addNonePrintable(112, "application/senml+cbor", "cbor");
        addNonePrintable(APPLICATION_VND_OMA_LWM2M_TLV, "application/vnd.oma.lwm2m+tlv", "tlv");
        addPrintable(APPLICATION_VND_OMA_LWM2M_JSON, "application/vnd.oma.lwm2m+json", "json", false);
    }

    public static Set<Integer> getAllMediaTypes() {
        return registry.keySet();
    }

    public static MediaTypeDefintion getDefinition(int mediaType) {
        return registry.get(Integer.valueOf(mediaType));
    }

    public static boolean isKnown(int mediaType) {
        return registry.containsKey(Integer.valueOf(mediaType));
    }

    public static boolean isPrintable(int mediaType) {
        MediaTypeDefintion definition = registry.get(Integer.valueOf(mediaType));
        if (definition != null) {
            return definition.isPrintable();
        }
        return false;
    }

    public static boolean isCharsetConvertible(int mediaType) {
        MediaTypeDefintion definition = registry.get(Integer.valueOf(mediaType));
        if (definition != null) {
            return definition.isCharsetConvertible();
        }
        return false;
    }

    public static int parse(String type) {
        if (type == null) {
            return -1;
        }
        for (MediaTypeDefintion defintion : registry.values()) {
            if (defintion.match(type)) {
                return defintion.getType().intValue();
            }
        }
        return -1;
    }

    public static int[] parseWildcard(String wildcard) {
        List<Integer> matches = new LinkedList<>();
        if (wildcard.equals("*/*")) {
            Iterator<MediaTypeDefintion> it = registry.values().iterator();
            while (it.hasNext()) {
                matches.add(it.next().getType());
            }
        } else if (wildcard.endsWith("/*")) {
            Pattern pattern = Pattern.compile(wildcard.replace("*", ".*"));
            for (MediaTypeDefintion defintion : registry.values()) {
                if (defintion.match(pattern)) {
                    matches.add(defintion.getType());
                }
            }
        } else {
            for (MediaTypeDefintion defintion2 : registry.values()) {
                if (defintion2.match(wildcard)) {
                    matches.add(defintion2.getType());
                }
            }
        }
        int[] result = new int[matches.size()];
        for (int index = 0; index < result.length; index++) {
            result[index] = matches.get(index).intValue();
        }
        return result;
    }

    public static String toFileExtension(int mediaType) {
        MediaTypeDefintion definition = registry.get(Integer.valueOf(mediaType));
        if (definition != null) {
            return definition.getFileExtension();
        }
        return "unknown_" + mediaType;
    }

    public static String toString(int mediaType) {
        if (mediaType == -1) {
            return "undefined";
        }
        MediaTypeDefintion definition = registry.get(Integer.valueOf(mediaType));
        if (definition != null) {
            return definition.getMime();
        }
        return "unknown/" + mediaType;
    }

    private static void addNonePrintable(int mediaType, String mime, String extension) {
        add(new MediaTypeDefintion(Integer.valueOf(mediaType), mime, extension));
    }

    private static void addPrintable(int mediaType, String mime, String extension, boolean isCharsetConvertible) {
        add(new MediaTypeDefintion(Integer.valueOf(mediaType), mime, extension, isCharsetConvertible));
    }

    public static void add(MediaTypeDefintion definition) {
        registry.put(definition.getType(), definition);
    }

    /* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-californium-core.jar:org/eclipse/californium/core/coap/MediaTypeRegistry$MediaTypeDefintion.class */
    public static class MediaTypeDefintion {
        private final Integer type;
        private final String mime;
        private final String fileExtension;
        private final boolean isPrintable;
        private final boolean isCharsetConvertible;

        public MediaTypeDefintion(Integer type, String mime, String fileExtension) {
            if (type == null) {
                throw new NullPointerException("type must not be null!");
            }
            if (mime == null) {
                throw new NullPointerException("mime must not be null!");
            }
            if (fileExtension == null) {
                throw new NullPointerException("file extension must not be null!");
            }
            this.type = type;
            this.mime = mime;
            this.fileExtension = fileExtension;
            this.isPrintable = false;
            this.isCharsetConvertible = false;
        }

        public MediaTypeDefintion(Integer type, String mime, String fileExtension, boolean isCharsetConvertible) {
            if (type == null) {
                throw new NullPointerException("type must not be null!");
            }
            if (mime == null) {
                throw new NullPointerException("mime must not be null!");
            }
            if (fileExtension == null) {
                throw new NullPointerException("file extension must not be null!");
            }
            this.type = type;
            this.mime = mime;
            this.fileExtension = fileExtension;
            this.isPrintable = true;
            this.isCharsetConvertible = isCharsetConvertible;
        }

        public boolean match(String mime) {
            return this.mime.equalsIgnoreCase(mime);
        }

        public boolean match(Pattern mimePattern) {
            return mimePattern.matcher(this.mime).matches();
        }

        public Integer getType() {
            return this.type;
        }

        public String getMime() {
            return this.mime;
        }

        public String getFileExtension() {
            return this.fileExtension;
        }

        public boolean isPrintable() {
            return this.isPrintable;
        }

        public boolean isCharsetConvertible() {
            return this.isCharsetConvertible;
        }
    }
}
