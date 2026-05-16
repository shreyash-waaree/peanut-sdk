package com.keenon.common.external;

import android.content.Context;
import com.keenon.common.constant.PeanutConstants;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/common/external/PeanutConfig.class */
public class PeanutConfig {
    private static Context context;
    private static String appId;
    private static String appSecret;
    private static final Config CONFIG = new Config();
    private static PeanutConstants.LinkType linkType = PeanutConstants.LinkType.COM_COAP;
    private static String linkCom = PeanutConstants.COM1;
    private static String linkIP = PeanutConstants.LOCAL_LINK_PROXY;
    private static int linkPort = 5683;
    private static String emotionLinkCOM = PeanutConstants.COM2;
    private static String doorLinkCOM = PeanutConstants.COM2;
    private static String lightLinkCOM = PeanutConstants.COM3_USB0;
    private static int doorNum = 1;
    private static long connectionTimeout = 60000;
    private static long requestTimeout = PeanutConstants.REQUEST_TIMEOUT;
    private static boolean logEnable = PeanutConstants.LOG_ENABLE;
    private static boolean logWriteFileEnable = PeanutConstants.LOG_WRITE_FILE_ENABLE;
    private static int logLevel = PeanutConstants.LOG_LEVEL;
    private static boolean logbackEnable = PeanutConstants.LOGBACK_ENABLE;
    private static boolean logbackPrintLogCatEnable = PeanutConstants.LOGBACK_PRINT_LOGCAT_ENABLE;
    private static boolean logbackWriteFileEnable = PeanutConstants.LOGBACK_WRITE_FILE_ENABLE;
    private static boolean umLogEnable = PeanutConstants.UM_LOGBACK_ENABLE;

    public static Config getConfig() {
        return CONFIG;
    }

    public static Context getContext() {
        return context;
    }

    public static PeanutConstants.LinkType getLinkType() {
        return linkType;
    }

    public static String getLinkCom() {
        return linkCom;
    }

    public static String getLinkIP() {
        return linkIP;
    }

    public static int getLinkPort() {
        return linkPort;
    }

    public static String getLinkUrl() {
        String protocol;
        switch (linkType) {
            case COAP:
            case COM_COAP:
                protocol = PeanutConstants.COAP_PROTOCOL;
                break;
            case HTTP:
                protocol = PeanutConstants.HTTP_PROTOCOL;
                break;
            default:
                protocol = PeanutConstants.COAP_PROTOCOL;
                break;
        }
        return protocol + linkIP + ":" + linkPort;
    }

    public static String getEmotionLinkCOM() {
        return emotionLinkCOM;
    }

    public static String getLightLinkCOM() {
        return lightLinkCOM;
    }

    public static String getDoorLinkCOM() {
        return doorLinkCOM;
    }

    public static int getDoorNum() {
        return doorNum;
    }

    public static long getConnectionTimeout() {
        return connectionTimeout;
    }

    public static long getRequestTimeout() {
        return requestTimeout;
    }

    public static boolean isLogEnable() {
        return logEnable;
    }

    public static int getLogLevel() {
        return logLevel;
    }

    public static boolean isLogbackEnable() {
        return logbackEnable;
    }

    public static boolean isLogbackPrintLogCatEnable() {
        return logbackPrintLogCatEnable;
    }

    public static boolean isLogbackWriteFileEnable() {
        return logbackWriteFileEnable;
    }

    public static boolean isUmLogEnable() {
        return umLogEnable;
    }

    public static boolean isLogWriteFileEnable() {
        return logWriteFileEnable;
    }

    public static String getAppId() {
        return appId;
    }

    public static String getSecret() {
        return appSecret;
    }

    /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/common/external/PeanutConfig$Config.class */
    public static class Config {
        public Config setContext(Context ctx) {
            Context unused = PeanutConfig.context = ctx;
            return this;
        }

        public Config setLinkType(PeanutConstants.LinkType type) {
            PeanutConstants.LinkType unused = PeanutConfig.linkType = type;
            return this;
        }

        public Config setLinkCOM(String com2) {
            String unused = PeanutConfig.linkCom = com2;
            return this;
        }

        public Config setLinkIP(String ip) {
            String unused = PeanutConfig.linkIP = ip;
            return this;
        }

        public Config setLinkPort(int port) {
            int unused = PeanutConfig.linkPort = port;
            return this;
        }

        public Config setEmotionLinkCOM(String com2) {
            String unused = PeanutConfig.emotionLinkCOM = com2;
            return this;
        }

        public Config setDoorLinkCOM(String com2) {
            String unused = PeanutConfig.doorLinkCOM = com2;
            return this;
        }

        public Config setLightLinkCOM(String com2) {
            String unused = PeanutConfig.lightLinkCOM = com2;
            return this;
        }

        public Config setDoorNum(int num) {
            int unused = PeanutConfig.doorNum = num;
            return this;
        }

        public Config setConnectionTimeout(long timeout) {
            long unused = PeanutConfig.connectionTimeout = timeout;
            return this;
        }

        public Config setRequestTimeout(long timeout) {
            long unused = PeanutConfig.requestTimeout = timeout;
            return this;
        }

        public Config enableLog(boolean enable) {
            boolean unused = PeanutConfig.logEnable = enable;
            return this;
        }

        public Config enableLogWriteFile(boolean enable) {
            boolean unused = PeanutConfig.logWriteFileEnable = enable;
            return this;
        }

        public Config enableLogBackWriteFile(boolean enable) {
            boolean unused = PeanutConfig.logbackWriteFileEnable = enable;
            return this;
        }

        public Config setLogLevel(int level) {
            int unused = PeanutConfig.logLevel = level;
            return this;
        }

        public Config setAppId(String id) {
            String unused = PeanutConfig.appId = id;
            return this;
        }

        public Config setSecret(String secret) {
            String unused = PeanutConfig.appSecret = secret;
            return this;
        }

        public Config enableLogBack(boolean enable) {
            boolean unused = PeanutConfig.logbackEnable = enable;
            return this;
        }

        public Config enableLogBackPrintLogCat(boolean enable) {
            boolean unused = PeanutConfig.logbackPrintLogCatEnable = enable;
            return this;
        }

        public Config enableUMLog(boolean enable) {
            boolean unused = PeanutConfig.umLogEnable = enable;
            return this;
        }
    }
}
