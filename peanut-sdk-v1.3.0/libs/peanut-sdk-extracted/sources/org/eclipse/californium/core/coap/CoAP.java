package org.eclipse.californium.core.coap;

import com.keenon.sdk.scmIot.protopack.base.ProtoTopic;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import org.eclipse.californium.elements.util.StandardCharsets;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-californium-core.jar:org/eclipse/californium/core/coap/CoAP.class */
public final class CoAP {
    public static final int VERSION = 1;
    public static final String PROTOCOL_UDP = "UDP";
    public static final String PROTOCOL_DTLS = "DTLS";
    public static final String PROTOCOL_TCP = "TCP";
    public static final String PROTOCOL_TLS = "TLS";
    public static final String COAP_URI_SCHEME = "coap";
    public static final String COAP_TCP_URI_SCHEME = "coap+tcp";
    public static final String COAP_SECURE_TCP_URI_SCHEME = "coaps+tcp";
    public static final String COAP_SECURE_URI_SCHEME = "coaps";
    public static final String URI_SCHEME_SEPARATOR = "://";
    public static final int DEFAULT_COAP_PORT = 5683;
    public static final int DEFAULT_COAP_SECURE_PORT = 5684;
    public static final Charset UTF8_CHARSET = StandardCharsets.UTF_8;
    public static final InetAddress MULTICAST_IPV4 = new InetSocketAddress("224.0.1.187", 0).getAddress();
    public static final InetAddress MULTICAST_IPV6_LINKLOCAL = new InetSocketAddress("[FF02::FD]", 0).getAddress();
    public static final InetAddress MULTICAST_IPV6_SITELOCAL = new InetSocketAddress("[FF05::FD]", 0).getAddress();
    private static final Map<String, Code> codeMap = new HashMap();
    private static final Map<String, ResponseCode> responseCodeMap = new HashMap();

    private CoAP() {
    }

    public static int getCodeClass(int code) {
        return (code & 224) >> 5;
    }

    public static int getCodeDetail(int code) {
        return code & 31;
    }

    public static String formatCode(int code) {
        return formatCode(getCodeClass(code), getCodeDetail(code));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static String formatCode(int codeClass, int codeDetail) {
        return String.format("%d.%02d", Integer.valueOf(codeClass), Integer.valueOf(codeDetail));
    }

    public static String getSchemeForProtocol(String protocol) {
        if (PROTOCOL_UDP.equalsIgnoreCase(protocol)) {
            return COAP_URI_SCHEME;
        }
        if (PROTOCOL_DTLS.equalsIgnoreCase(protocol)) {
            return COAP_SECURE_URI_SCHEME;
        }
        if (PROTOCOL_TCP.equalsIgnoreCase(protocol)) {
            return COAP_TCP_URI_SCHEME;
        }
        if (PROTOCOL_TLS.equalsIgnoreCase(protocol)) {
            return COAP_SECURE_TCP_URI_SCHEME;
        }
        throw new IllegalArgumentException("Protocol " + protocol + " not supported!");
    }

    public static String getProtocolForScheme(String scheme) {
        if (COAP_URI_SCHEME.equalsIgnoreCase(scheme)) {
            return PROTOCOL_UDP;
        }
        if (COAP_SECURE_URI_SCHEME.equalsIgnoreCase(scheme)) {
            return PROTOCOL_DTLS;
        }
        if (COAP_TCP_URI_SCHEME.equalsIgnoreCase(scheme)) {
            return PROTOCOL_TCP;
        }
        if (COAP_SECURE_TCP_URI_SCHEME.equalsIgnoreCase(scheme)) {
            return PROTOCOL_TLS;
        }
        throw new IllegalArgumentException("Scheme " + scheme + " not supported!");
    }

    public static boolean isTcpProtocol(String protocol) {
        return PROTOCOL_TCP.equalsIgnoreCase(protocol) || PROTOCOL_TLS.equalsIgnoreCase(protocol);
    }

    public static boolean isSecureProtocol(String protocol) {
        return PROTOCOL_DTLS.equalsIgnoreCase(protocol) || PROTOCOL_TLS.equalsIgnoreCase(protocol);
    }

    public static boolean isTcpScheme(String uriScheme) {
        return COAP_TCP_URI_SCHEME.equalsIgnoreCase(uriScheme) || COAP_SECURE_TCP_URI_SCHEME.equalsIgnoreCase(uriScheme);
    }

    public static boolean isSecureScheme(String uriScheme) {
        return COAP_SECURE_URI_SCHEME.equalsIgnoreCase(uriScheme) || COAP_SECURE_TCP_URI_SCHEME.equalsIgnoreCase(uriScheme);
    }

    public static boolean isSupportedScheme(String uriScheme) {
        return COAP_URI_SCHEME.equalsIgnoreCase(uriScheme) || COAP_TCP_URI_SCHEME.equalsIgnoreCase(uriScheme) || COAP_SECURE_URI_SCHEME.equalsIgnoreCase(uriScheme) || COAP_SECURE_TCP_URI_SCHEME.equalsIgnoreCase(uriScheme);
    }

    public static int getDefaultPort(String uriScheme) {
        if (COAP_URI_SCHEME.equalsIgnoreCase(uriScheme)) {
            return 5683;
        }
        if (COAP_SECURE_URI_SCHEME.equalsIgnoreCase(uriScheme)) {
            return DEFAULT_COAP_SECURE_PORT;
        }
        if (COAP_TCP_URI_SCHEME.equalsIgnoreCase(uriScheme)) {
            return 5683;
        }
        if (COAP_SECURE_TCP_URI_SCHEME.equalsIgnoreCase(uriScheme)) {
            return DEFAULT_COAP_SECURE_PORT;
        }
        throw new IllegalArgumentException("URI scheme '" + uriScheme + "' is not supported!");
    }

    public static String getSchemeFromUri(String uri) {
        int index = uri.indexOf(URI_SCHEME_SEPARATOR);
        if (index > 0) {
            return uri.substring(0, index);
        }
        return null;
    }

    public static boolean isRequest(int code) {
        return code >= 1 && code <= 31;
    }

    public static boolean isResponse(int code) {
        return code >= 64 && code <= 191;
    }

    public static boolean isEmptyMessage(int code) {
        return code == 0;
    }

    public static boolean isObservable(Code code) {
        return code == Code.GET || code == Code.FETCH;
    }

    public static String toCodeString(int rawCode) {
        String result = formatCode(rawCode);
        try {
            if (isRequest(rawCode)) {
                Code code = Code.valueOf(rawCode);
                result = result + "/" + code.text;
            } else if (isResponse(rawCode)) {
                ResponseCode code2 = ResponseCode.valueOf(rawCode);
                result = result + "/" + code2.text;
            } else if (isEmptyMessage(rawCode)) {
                result = result + "/EMPTY";
            }
        } catch (MessageFormatException e) {
        }
        return result;
    }

    /* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-californium-core.jar:org/eclipse/californium/core/coap/CoAP$Type.class */
    public enum Type {
        CON(0),
        NON(1),
        ACK(2),
        RST(3);

        public final int value;

        Type(int value) {
            this.value = value;
        }

        public static Type valueOf(int value) {
            switch (value) {
                case 0:
                    return CON;
                case 1:
                    return NON;
                case 2:
                    return ACK;
                case 3:
                    return RST;
                default:
                    throw new IllegalArgumentException("Unknown CoAP type " + value);
            }
        }
    }

    /* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-californium-core.jar:org/eclipse/californium/core/coap/CoAP$CodeClass.class */
    public enum CodeClass {
        REQUEST(0),
        SUCCESS_RESPONSE(2),
        ERROR_RESPONSE(4),
        SERVER_ERROR_RESPONSE(5),
        SIGNAL(7);

        public final int value;

        CodeClass(int value) {
            this.value = value;
        }

        public static CodeClass valueOf(int value) {
            switch (value) {
                case 0:
                    return REQUEST;
                case 1:
                case 3:
                case 6:
                default:
                    throw new MessageFormatException(String.format("Unknown CoAP class code: %d", Integer.valueOf(value)));
                case 2:
                    return SUCCESS_RESPONSE;
                case 4:
                    return ERROR_RESPONSE;
                case 5:
                    return SERVER_ERROR_RESPONSE;
                case 7:
                    return SIGNAL;
            }
        }
    }

    /* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-californium-core.jar:org/eclipse/californium/core/coap/CoAP$Code.class */
    public enum Code {
        GET(1),
        POST(2),
        PUT(3),
        DELETE(4),
        FETCH(5),
        PATCH(6),
        IPATCH(7),
        CUSTOM_30(30);

        public final int value;
        public final String text;

        Code(int value) {
            this.value = value;
            this.text = CoAP.formatCode(CoAP.getCodeClass(value), CoAP.getCodeDetail(value));
            CoAP.codeMap.put(this.text, this);
        }

        public static Code valueOf(int value) {
            int codeClass = CoAP.getCodeClass(value);
            int codeDetail = CoAP.getCodeDetail(value);
            if (codeClass > 0) {
                throw new MessageFormatException(String.format("Not a CoAP request code: %s", CoAP.formatCode(codeClass, codeDetail)));
            }
            switch (codeDetail) {
                case 1:
                    return GET;
                case 2:
                    return POST;
                case 3:
                    return PUT;
                case 4:
                    return DELETE;
                case 5:
                    return FETCH;
                case 6:
                    return PATCH;
                case 7:
                    return IPATCH;
                case 8:
                case 9:
                case 10:
                case 11:
                case 12:
                case 13:
                case 14:
                case 15:
                case 16:
                case 17:
                case 18:
                case 19:
                case 20:
                case 21:
                case 22:
                case 23:
                case 24:
                case 25:
                case NoResponseOption.SUPPRESS_ALL /* 26 */:
                case OptionNumberRegistry.BLOCK1 /* 27 */:
                case OptionNumberRegistry.SIZE2 /* 28 */:
                case ProtoTopic.CONTROL_CALL_BELL /* 29 */:
                default:
                    throw new MessageFormatException(String.format("Unknown CoAP request code: %s", CoAP.formatCode(codeClass, codeDetail)));
                case ProtoTopic.CONTROL_ACK /* 30 */:
                    return CUSTOM_30;
            }
        }

        public static Code valueOfText(String value) {
            return (Code) CoAP.codeMap.get(value);
        }
    }

    /* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-californium-core.jar:org/eclipse/californium/core/coap/CoAP$ResponseCode.class */
    public enum ResponseCode {
        _UNKNOWN_SUCCESS_CODE(CodeClass.SUCCESS_RESPONSE, 0),
        CREATED(CodeClass.SUCCESS_RESPONSE, 1),
        DELETED(CodeClass.SUCCESS_RESPONSE, 2),
        VALID(CodeClass.SUCCESS_RESPONSE, 3),
        CHANGED(CodeClass.SUCCESS_RESPONSE, 4),
        CONTENT(CodeClass.SUCCESS_RESPONSE, 5),
        CONTINUE(CodeClass.SUCCESS_RESPONSE, 31),
        BAD_REQUEST(CodeClass.ERROR_RESPONSE, 0),
        UNAUTHORIZED(CodeClass.ERROR_RESPONSE, 1),
        BAD_OPTION(CodeClass.ERROR_RESPONSE, 2),
        FORBIDDEN(CodeClass.ERROR_RESPONSE, 3),
        NOT_FOUND(CodeClass.ERROR_RESPONSE, 4),
        METHOD_NOT_ALLOWED(CodeClass.ERROR_RESPONSE, 5),
        NOT_ACCEPTABLE(CodeClass.ERROR_RESPONSE, 6),
        REQUEST_ENTITY_INCOMPLETE(CodeClass.ERROR_RESPONSE, 8),
        CONFLICT(CodeClass.ERROR_RESPONSE, 9),
        PRECONDITION_FAILED(CodeClass.ERROR_RESPONSE, 12),
        REQUEST_ENTITY_TOO_LARGE(CodeClass.ERROR_RESPONSE, 13),
        UNSUPPORTED_CONTENT_FORMAT(CodeClass.ERROR_RESPONSE, 15),
        UNPROCESSABLE_ENTITY(CodeClass.ERROR_RESPONSE, 22),
        TOO_MANY_REQUESTS(CodeClass.ERROR_RESPONSE, 29),
        INTERNAL_SERVER_ERROR(CodeClass.SERVER_ERROR_RESPONSE, 0),
        NOT_IMPLEMENTED(CodeClass.SERVER_ERROR_RESPONSE, 1),
        BAD_GATEWAY(CodeClass.SERVER_ERROR_RESPONSE, 2),
        SERVICE_UNAVAILABLE(CodeClass.SERVER_ERROR_RESPONSE, 3),
        GATEWAY_TIMEOUT(CodeClass.SERVER_ERROR_RESPONSE, 4),
        PROXY_NOT_SUPPORTED(CodeClass.SERVER_ERROR_RESPONSE, 5);

        public final int value;
        public final int codeClass;
        public final int codeDetail;
        public final String text;

        ResponseCode(CodeClass codeClass, int codeDetail) {
            this.codeClass = codeClass.value;
            this.codeDetail = codeDetail;
            this.value = (codeClass.value << 5) | codeDetail;
            this.text = CoAP.formatCode(codeClass.value, codeDetail);
            CoAP.responseCodeMap.put(this.text, this);
        }

        public boolean isSuccess() {
            return this.codeClass == CodeClass.SUCCESS_RESPONSE.value;
        }

        public boolean isClientError() {
            return this.codeClass == CodeClass.ERROR_RESPONSE.value;
        }

        public boolean isServerError() {
            return this.codeClass == CodeClass.SERVER_ERROR_RESPONSE.value;
        }

        public static ResponseCode valueOf(int value) {
            int codeClass = CoAP.getCodeClass(value);
            int codeDetail = CoAP.getCodeDetail(value);
            switch (codeClass) {
                case 2:
                    return valueOfSuccessCode(codeDetail);
                case 3:
                default:
                    throw new MessageFormatException(String.format("Not a CoAP response code: %s", CoAP.formatCode(codeClass, codeDetail)));
                case 4:
                    return valueOfClientErrorCode(codeDetail);
                case 5:
                    return valueOfServerErrorCode(codeDetail);
            }
        }

        public static ResponseCode valueOfText(String value) {
            return (ResponseCode) CoAP.responseCodeMap.get(value);
        }

        private static ResponseCode valueOfSuccessCode(int codeDetail) {
            switch (codeDetail) {
                case 1:
                    return CREATED;
                case 2:
                    return DELETED;
                case 3:
                    return VALID;
                case 4:
                    return CHANGED;
                case 5:
                    return CONTENT;
                case 31:
                    return CONTINUE;
                default:
                    return _UNKNOWN_SUCCESS_CODE;
            }
        }

        private static ResponseCode valueOfClientErrorCode(int codeDetail) {
            switch (codeDetail) {
                case 0:
                    return BAD_REQUEST;
                case 1:
                    return UNAUTHORIZED;
                case 2:
                    return BAD_OPTION;
                case 3:
                    return FORBIDDEN;
                case 4:
                    return NOT_FOUND;
                case 5:
                    return METHOD_NOT_ALLOWED;
                case 6:
                    return NOT_ACCEPTABLE;
                case 7:
                case 10:
                case 11:
                case 14:
                case 16:
                case 17:
                case 18:
                case 19:
                case 20:
                case 21:
                case 23:
                case 24:
                case 25:
                case NoResponseOption.SUPPRESS_ALL /* 26 */:
                case OptionNumberRegistry.BLOCK1 /* 27 */:
                case OptionNumberRegistry.SIZE2 /* 28 */:
                default:
                    return BAD_REQUEST;
                case 8:
                    return REQUEST_ENTITY_INCOMPLETE;
                case 9:
                    return CONFLICT;
                case 12:
                    return PRECONDITION_FAILED;
                case 13:
                    return REQUEST_ENTITY_TOO_LARGE;
                case 15:
                    return UNSUPPORTED_CONTENT_FORMAT;
                case 22:
                    return UNPROCESSABLE_ENTITY;
                case ProtoTopic.CONTROL_CALL_BELL /* 29 */:
                    return TOO_MANY_REQUESTS;
            }
        }

        private static ResponseCode valueOfServerErrorCode(int codeDetail) {
            switch (codeDetail) {
                case 0:
                    return INTERNAL_SERVER_ERROR;
                case 1:
                    return NOT_IMPLEMENTED;
                case 2:
                    return BAD_GATEWAY;
                case 3:
                    return SERVICE_UNAVAILABLE;
                case 4:
                    return GATEWAY_TIMEOUT;
                case 5:
                    return PROXY_NOT_SUPPORTED;
                default:
                    return INTERNAL_SERVER_ERROR;
            }
        }

        @Override // java.lang.Enum
        public String toString() {
            return this.text;
        }
    }

    /* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-californium-core.jar:org/eclipse/californium/core/coap/CoAP$MessageFormat.class */
    public final class MessageFormat {
        public static final int LENGTH_NIBBLE_BITS = 4;
        public static final int VERSION_BITS = 2;
        public static final int TYPE_BITS = 2;
        public static final int TOKEN_LENGTH_BITS = 4;
        public static final int CODE_BITS = 8;
        public static final int MESSAGE_ID_BITS = 16;
        public static final int OPTION_DELTA_BITS = 4;
        public static final int OPTION_LENGTH_BITS = 4;
        public static final byte PAYLOAD_MARKER = -1;
        public static final int VERSION = 1;
        public static final int EMPTY_CODE = 0;
        public static final int REQUEST_CODE_LOWER_BOUND = 1;
        public static final int REQUEST_CODE_UPPER_BOUND = 31;
        public static final int RESPONSE_CODE_LOWER_BOUND = 64;
        public static final int RESPONSE_CODE_UPPER_BOUND = 191;

        private MessageFormat() {
        }
    }
}
