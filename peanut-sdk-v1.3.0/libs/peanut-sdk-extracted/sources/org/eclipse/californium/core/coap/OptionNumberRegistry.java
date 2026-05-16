package org.eclipse.californium.core.coap;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-californium-core.jar:org/eclipse/californium/core/coap/OptionNumberRegistry.class */
public final class OptionNumberRegistry {
    public static final int UNKNOWN = -1;
    public static final int RESERVED_0 = 0;
    public static final int IF_MATCH = 1;
    public static final int URI_HOST = 3;
    public static final int ETAG = 4;
    public static final int IF_NONE_MATCH = 5;
    public static final int URI_PORT = 7;
    public static final int LOCATION_PATH = 8;
    public static final int URI_PATH = 11;
    public static final int CONTENT_FORMAT = 12;
    public static final int MAX_AGE = 14;
    public static final int URI_QUERY = 15;
    public static final int ACCEPT = 17;
    public static final int LOCATION_QUERY = 20;
    public static final int PROXY_URI = 35;
    public static final int PROXY_SCHEME = 39;
    public static final int SIZE1 = 60;
    public static final int RESERVED_1 = 128;
    public static final int RESERVED_2 = 132;
    public static final int RESERVED_3 = 136;
    public static final int RESERVED_4 = 140;
    public static final int OBSERVE = 6;
    public static final int BLOCK2 = 23;
    public static final int BLOCK1 = 27;
    public static final int SIZE2 = 28;
    public static final int OSCORE = 9;
    public static final int NO_RESPONSE = 258;

    /* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-californium-core.jar:org/eclipse/californium/core/coap/OptionNumberRegistry$Defaults.class */
    public static class Defaults {
        public static final long MAX_AGE = 60;
    }

    /* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-californium-core.jar:org/eclipse/californium/core/coap/OptionNumberRegistry$Names.class */
    public static class Names {
        public static final String Reserved = "Reserved";
        public static final String If_Match = "If-Match";
        public static final String Uri_Host = "Uri-Host";
        public static final String ETag = "ETag";
        public static final String If_None_Match = "If-None-Match";
        public static final String Uri_Port = "Uri-Port";
        public static final String Location_Path = "Location-Path";
        public static final String Uri_Path = "Uri-Path";
        public static final String Content_Format = "Content-Format";
        public static final String Max_Age = "Max-Age";
        public static final String Uri_Query = "Uri-Query";
        public static final String Accept = "Accept";
        public static final String Location_Query = "Location-Query";
        public static final String Proxy_Uri = "Proxy-Uri";
        public static final String Proxy_Scheme = "Proxy-Scheme";
        public static final String Size1 = "Size1";
        public static final String Observe = "Observe";
        public static final String Block2 = "Block2";
        public static final String Block1 = "Block1";
        public static final String Size2 = "Size2";
        public static final String Object_Security = "Object-Security";
        public static final String No_Response = "No-Response";
    }

    /* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-californium-core.jar:org/eclipse/californium/core/coap/OptionNumberRegistry$OptionFormat.class */
    public enum OptionFormat {
        INTEGER,
        STRING,
        OPAQUE,
        UNKNOWN,
        EMPTY
    }

    public static OptionFormat getFormatByNr(int optionNumber) {
        switch (optionNumber) {
            case 1:
            case 4:
            case 9:
                return OptionFormat.OPAQUE;
            case 3:
            case 8:
            case 11:
            case 15:
            case 20:
            case 35:
            case 39:
                return OptionFormat.STRING;
            case 5:
                return OptionFormat.EMPTY;
            case 6:
            case 7:
            case 12:
            case 14:
            case 17:
            case 23:
            case BLOCK1 /* 27 */:
            case SIZE2 /* 28 */:
            case 60:
            case NO_RESPONSE /* 258 */:
                return OptionFormat.INTEGER;
            default:
                return OptionFormat.UNKNOWN;
        }
    }

    public static boolean isCritical(int optionNumber) {
        return (optionNumber & 1) != 0;
    }

    public static boolean isElective(int optionNumber) {
        return (optionNumber & 1) == 0;
    }

    public static boolean isUnsafe(int optionNumber) {
        return (optionNumber & 2) > 0;
    }

    public static boolean isSafe(int optionNumber) {
        return !isUnsafe(optionNumber);
    }

    public static boolean isNoCacheKey(int optionNumber) {
        return (optionNumber & 30) == 28;
    }

    public static boolean isCacheKey(int optionNumber) {
        return !isNoCacheKey(optionNumber);
    }

    public static boolean isSingleValue(int optionNumber) {
        switch (optionNumber) {
            case 1:
            case 4:
            case 8:
            case 11:
            case 15:
            case 20:
                return false;
            case 3:
            case 5:
            case 6:
            case 7:
            case 9:
            case 12:
            case 14:
            case 17:
            case 23:
            case BLOCK1 /* 27 */:
            case SIZE2 /* 28 */:
            case 35:
            case 39:
            case 60:
            case NO_RESPONSE /* 258 */:
            default:
                return true;
        }
    }

    public static void assertValue(int optionNumber, long value) {
        try {
            int length = ((64 - Long.numberOfLeadingZeros(value)) + 7) / 8;
            assertValueLength(optionNumber, length);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(ex.getMessage() + " Value " + value);
        }
    }

    public static void assertValueLength(int optionNumber, int valueLength) {
        int min = 0;
        int max = 65804;
        switch (optionNumber) {
            case 1:
                max = 8;
                break;
            case 3:
            case 39:
                min = 1;
                max = 255;
                break;
            case 4:
                min = 1;
                max = 8;
                break;
            case 5:
                max = 0;
                break;
            case 6:
            case 23:
            case BLOCK1 /* 27 */:
                max = 3;
                break;
            case 7:
            case 12:
            case 17:
                max = 2;
                break;
            case 8:
            case 9:
            case 11:
            case 15:
            case 20:
                max = 255;
                break;
            case 14:
            case SIZE2 /* 28 */:
            case 60:
                max = 4;
                break;
            case 35:
                min = 1;
                max = 1034;
                break;
            case NO_RESPONSE /* 258 */:
                max = 1;
                break;
        }
        if (valueLength < min || valueLength > max) {
            String name = toString(optionNumber);
            if (min == max) {
                if (min == 0) {
                    throw new IllegalArgumentException("Option " + name + " value of " + valueLength + " bytes must be empty.");
                }
                throw new IllegalArgumentException("Option " + name + " value of " + valueLength + " bytes must be " + min + " bytes.");
            }
            throw new IllegalArgumentException("Option " + name + " value of " + valueLength + " bytes must be in range of [" + min + "-" + max + "] bytes.");
        }
    }

    public static boolean isUriOption(int optionNumber) {
        boolean result = optionNumber == 3 || optionNumber == 11 || optionNumber == 7 || optionNumber == 15;
        return result;
    }

    public static String toString(int optionNumber) {
        switch (optionNumber) {
            case 0:
            case 128:
            case RESERVED_2 /* 132 */:
            case RESERVED_3 /* 136 */:
            case RESERVED_4 /* 140 */:
                return Names.Reserved;
            case 1:
                return Names.If_Match;
            case 3:
                return Names.Uri_Host;
            case 4:
                return Names.ETag;
            case 5:
                return Names.If_None_Match;
            case 6:
                return Names.Observe;
            case 7:
                return Names.Uri_Port;
            case 8:
                return Names.Location_Path;
            case 9:
                return Names.Object_Security;
            case 11:
                return Names.Uri_Path;
            case 12:
                return Names.Content_Format;
            case 14:
                return Names.Max_Age;
            case 15:
                return Names.Uri_Query;
            case 17:
                return Names.Accept;
            case 20:
                return Names.Location_Query;
            case 23:
                return Names.Block2;
            case BLOCK1 /* 27 */:
                return Names.Block1;
            case SIZE2 /* 28 */:
                return Names.Size2;
            case 35:
                return Names.Proxy_Uri;
            case 39:
                return Names.Proxy_Scheme;
            case 60:
                return Names.Size1;
            case NO_RESPONSE /* 258 */:
                return Names.No_Response;
            default:
                return String.format("Unknown (%d)", Integer.valueOf(optionNumber));
        }
    }

    public static int toNumber(String name) {
        if (Names.If_Match.equals(name)) {
            return 1;
        }
        if (Names.Uri_Host.equals(name)) {
            return 3;
        }
        if (Names.ETag.equals(name)) {
            return 4;
        }
        if (Names.If_None_Match.equals(name)) {
            return 5;
        }
        if (Names.Uri_Port.equals(name)) {
            return 7;
        }
        if (Names.Location_Path.equals(name)) {
            return 8;
        }
        if (Names.Uri_Path.equals(name)) {
            return 11;
        }
        if (Names.Content_Format.equals(name)) {
            return 12;
        }
        if (Names.Max_Age.equals(name)) {
            return 14;
        }
        if (Names.Uri_Query.equals(name)) {
            return 15;
        }
        if (Names.Accept.equals(name)) {
            return 17;
        }
        if (Names.Location_Query.equals(name)) {
            return 20;
        }
        if (Names.Proxy_Uri.equals(name)) {
            return 35;
        }
        if (Names.Proxy_Scheme.equals(name)) {
            return 39;
        }
        if (Names.Observe.equals(name)) {
            return 6;
        }
        if (Names.Block2.equals(name)) {
            return 23;
        }
        if (Names.Block1.equals(name)) {
            return 27;
        }
        if (Names.Size2.equals(name)) {
            return 28;
        }
        if (Names.Size1.equals(name)) {
            return 60;
        }
        if (Names.Object_Security.equals(name)) {
            return 9;
        }
        if (Names.No_Response.equals(name)) {
            return NO_RESPONSE;
        }
        return -1;
    }

    private OptionNumberRegistry() {
    }
}
