package com.keenon.common.error;

import com.google.gson.JsonSyntaxException;
import java.io.CharConversionException;
import java.io.EOFException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.ObjectStreamException;
import java.io.SyncFailedException;
import java.io.UTFDataFormatException;
import java.io.UnsupportedEncodingException;
import java.net.BindException;
import java.net.ConnectException;
import java.net.HttpRetryException;
import java.net.MalformedURLException;
import java.net.NoRouteToHostException;
import java.net.PortUnreachableException;
import java.net.ProtocolException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.net.UnknownServiceException;
import java.util.concurrent.TimeoutException;
import org.json.JSONException;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/common/error/PeanutError.class */
public class PeanutError {
    public static final int BASE = 203000;
    private static final int INIT = 203000;
    public static final int INIT_NULL_CONTEXT = 203000;
    public static final int INIT_NULL_COMPONENT = 203001;
    public static final int INIT_PERMISSION_DENIED = 203002;
    private static final int AUTH = 203020;
    public static final int AUTH_FAILED = 203021;
    public static final int AUTH_PARAMS_NULL = 203022;
    public static final int AUTH_INVALID_SECRET = 203023;
    public static final int AUTH_INVALID_LICENSE = 203024;
    public static final int AUTH_INVALID_SN = 203025;
    public static final int AUTH_INVALID_APP = 203026;
    public static final int AUTH_INVALID_APPID = 203027;
    public static final int AUTH_INVALID_FP = 203028;
    public static final int AUTH_INVALID_DISTANCE = 203029;
    public static final int AUTH_FP_ENCODE_EXCEPTION = 203030;
    public static final int AUTH_FP_DECODE_EXCEPTION = 203031;
    public static final int AUTH_EXPIRED = 203032;
    private static final int PROPERTIES = 203040;
    private static final int OTHER = 203060;
    private static final int CONNECTION = 203100;
    public static final int CONNECTION_TIMEOUT = 203101;
    public static final int CONNECTION_RESTORE = 203102;
    private static final int SERIAL = 203150;
    public static final int SERIAL_RESPONSE_ERROR = 203151;
    public static final int SERIAL_RESPONSE_NULL_DATA = 203152;
    private static final int COAP = 203170;
    public static final int COAP_RESPONSE_ERROR = 203171;
    public static final int COAP_RESPONSE_NULL_DATA = 203172;
    public static final int COAP_RESPONSE_TIMEOUT = 203173;
    private static final int SOCKET = 203190;
    private static final int HTTP = 203220;
    public static final int HTTP_RESPONSE_ERROR = 203221;
    public static final int HTTP_RESPONSE_PARSE_EXCEPTION = 203222;
    public static final int HTTP_RESPONSE_TIMEOUT = 203223;
    public static final int HTTP_CONNECTION_EXCEPTION = 203224;
    public static final int WEBSOCKET_NOT_CONNECTED = 203225;
    private static final int PM = 203300;
    public static final int PM_DEFAULT = 203300;
    public static final int PM_TIME_OUT = 203301;
    public static final int PM_PARAM_NO_DST = 203302;
    public static final int PM_NO_PATH = 203303;
    public static final int PM_NO_MATCH_TO_CHARGING_PILE_TIMEOUT = 203304;
    public static final int PM_MATCH_TO_CHARGING_PILE_AT_LEAST_ONE_TIME = 203305;
    public static final int PM_NO_LABEL = 203306;
    public static final int PM_NO_RADAR = 203307;
    public static final int PM_NO_STM32 = 203308;
    public static final int PM_UNDEFINED_ERR = 203309;
    public static final int PM_NO_5V = 203310;
    public static final int PM_CHARGE_INTERRUPT = 203311;
    public static final int PM_GIVE_UP_CHARGE = 203312;
    public static final int PM_FSM_NOT_SET = 203313;
    public static final int PM_RETRY_MAX_LIMIT = 203314;
    public static final int PM_BATTERY_STATUS_ERROR = 203315;
    public static final int PM_LOW_CURRENT = 203316;
    public static final int PM_AUTO_CHARGED_FAILED = 203317;
    private static final int MOTOR = 203350;
    private static final int NAVI = 203400;
    public static final int NAVI_ROUTE_NO_NODE = 203401;
    public static final int NAVI_ROUTE_NO_PATH = 203402;
    public static final int NAVI_ROUTE_INVALID_POSE = 203403;
    public static final int NAVI_ROUTE_LIMIT = 203404;
    public static final int NAVI_NO_DEST = 203405;
    public static final int NAVI_NO_PATH = 203406;
    public static final int NAVI_NO_FSM = 203407;
    public static final int NAVI_IN_CHARGING = 203408;
    public static final int NAVI_NO_ARRIVAL = 203409;
    public static final int NAVI_NOT_IDLE = 203410;
    public static final int NAVI_REPEAT_LIMIT = 203411;
    private static final int RUNTIME = 203449;
    private static final int DOOR = 203500;
    public static final int DOOR_TYPE_UNKNOW = 203501;
    public static final int DOOR_DOORID_ILLEGAL = 203502;
    public static final int DOOR_GATINGFSM_FAIL = 203503;
    private static final int TRANSPORT = 203550;
    public static final int TRANSPORT_ERROR_TIMEOUT = 203551;
    public static final int TRANSPORT_ERROR_ZIP_FAILED = 203552;
    public static final int TRANSPORT_ERROR_ZIP_PATH_INVALID = 203553;
    public static final int TRANSPORT_ERROR_EMPTY_DATA = 203554;
    public static final int TRANSPORT_ERROR_ROBOT_CHECK_MD5_FAILED = 203555;
    public static final int TRANSPORT_ERROR_UNZIP_FAILED = 203556;
    public static final int TRANSPORT_ERROR_EMPTY_FILE = 203557;
    public static final int TRANSPORT_ERROR_EMPTY_ZIP = 203558;
    public static final int TRANSPORT_ERROR_EMPTY_UPLOAD_FILE = 203559;
    public static final int TRANSPORT_ERROR_UNZIP_FILE_INVALID = 203560;
    public static final int ERROR_CHECK_MD5_FAILED = 203561;
    private static final int OTA = 203600;
    public static final int OTA_DEVICE_NOT_SUPPORT = 203601;
    public static final int OTA_UNKNOWN_VERSION = 203602;
    public static final int OTA_IN_PROGRESS = 203603;
    public static final int OTA_SAME_VERSION = 203604;
    public static final int OTA_DEVICE_NULL = 203605;
    public static final int OTA_FW_NOT_EXIST = 203606;
    public static final int OTA_FW_FILE_INVALID = 203607;
    public static final int OTA_FW_MD5_EMPTY = 203608;
    public static final int OTA_FW_MD5_NOT_MATCH = 203609;
    public static final int OTA_LOW_VERSION_DEPENDENCY = 203610;
    public static final int OTA_FW_PACK_INFO_NULL = 203611;
    public static final int OTA_FW_APP_FILE_NULL = 203612;
    public static final int OTA_APP_EXCEPTION_INTO = 203613;
    public static final int IllegalStateException = 100;
    public static final int IllegalArgumentException = 101;
    public static final int NumberFormatException = 102;
    public static final int JSONException = 103;
    public static final int BindException = 104;
    public static final int ConnectException = 105;
    public static final int HttpRetryException = 106;
    public static final int MalformedURLException = 107;
    public static final int NoRouteToHostException = 108;
    public static final int PortUnreachableException = 109;
    public static final int ProtocolException = 110;
    public static final int SocketException = 111;
    public static final int SocketTimeoutException = 112;
    public static final int TimeoutException = 113;
    public static final int UnknownHostException = 114;
    public static final int UnknownServiceException = 115;
    public static final int URISyntaxException = 116;
    public static final int CharConversionException = 117;
    public static final int EOFException = 118;
    public static final int FileNotFoundException = 119;
    public static final int InterruptedIOException = 120;
    public static final int ObjectStreamException = 121;
    public static final int SyncFailedException = 122;
    public static final int UnsupportedEncodingException = 123;
    public static final int UTFDataFormatException = 124;
    public static final int IOException = 125;
    public static final int NullPointerException = 126;
    public static final int UnsupportedOperationException = 127;
    public static final int Exception = 128;
    public static final int JsonSyntaxException = 137;
    public static Listener mListener;

    /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/common/error/PeanutError$Listener.class */
    public interface Listener {
        void onUpdate(int i);
    }

    public static synchronized void registerListener(Listener listener) {
        mListener = listener;
    }

    public static synchronized void removeListener(Listener listener) {
        mListener = null;
    }

    public static int getExceptionCode(Exception e) {
        if (e instanceof IllegalStateException) {
            return 100;
        }
        if (e instanceof IllegalArgumentException) {
            return IllegalArgumentException;
        }
        if (e instanceof NumberFormatException) {
            return NumberFormatException;
        }
        if (e instanceof UnknownHostException) {
            return UnknownHostException;
        }
        if (e instanceof SocketTimeoutException) {
            return 112;
        }
        if (e instanceof TimeoutException) {
            return TimeoutException;
        }
        if (e instanceof UnknownServiceException) {
            return UnknownServiceException;
        }
        if (e instanceof URISyntaxException) {
            return URISyntaxException;
        }
        if (e instanceof CharConversionException) {
            return CharConversionException;
        }
        if (e instanceof EOFException) {
            return EOFException;
        }
        if (e instanceof FileNotFoundException) {
            return FileNotFoundException;
        }
        if (e instanceof InterruptedIOException) {
            return InterruptedIOException;
        }
        if (e instanceof ObjectStreamException) {
            return ObjectStreamException;
        }
        if (e instanceof SyncFailedException) {
            return SyncFailedException;
        }
        if (e instanceof UnsupportedEncodingException) {
            return UnsupportedEncodingException;
        }
        if (e instanceof UnsupportedOperationException) {
            return UnsupportedOperationException;
        }
        if (e instanceof NullPointerException) {
            return NullPointerException;
        }
        if (e instanceof UTFDataFormatException) {
            return UTFDataFormatException;
        }
        if (e instanceof BindException) {
            return BindException;
        }
        if (e instanceof ConnectException) {
            return HTTP_CONNECTION_EXCEPTION;
        }
        if (e instanceof HttpRetryException) {
            return HttpRetryException;
        }
        if (e instanceof MalformedURLException) {
            return MalformedURLException;
        }
        if (e instanceof NoRouteToHostException) {
            return NoRouteToHostException;
        }
        if (e instanceof PortUnreachableException) {
            return PortUnreachableException;
        }
        if (e instanceof ProtocolException) {
            return 110;
        }
        if (e instanceof SocketException) {
            return SocketException;
        }
        if (e instanceof JSONException) {
            return JSONException;
        }
        if (e instanceof IOException) {
            return IOException;
        }
        if (e instanceof JsonSyntaxException) {
            return JsonSyntaxException;
        }
        return 128;
    }

    public static boolean isNetWorkConnectError(int code) {
        return code == 111 || code == 112 || code == 113 || code == 105 || code == 125;
    }

    public static void notify(Exception e) {
        notify(getExceptionCode(e));
    }

    public static void notify(int code) {
        if (mListener != null) {
            mListener.onUpdate(code);
        }
    }
}
