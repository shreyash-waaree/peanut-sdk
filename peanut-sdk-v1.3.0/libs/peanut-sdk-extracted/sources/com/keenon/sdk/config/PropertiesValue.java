package com.keenon.sdk.config;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/config/PropertiesValue.class */
public class PropertiesValue {
    public static String getRobotLinkType() {
        return PeanutProperties.getValue(PropertiesKey.APP_ROBOT_LINK_TYPE, "");
    }

    public static String getRobotLinkCOM() {
        return PeanutProperties.getValue(PropertiesKey.APP_ROBOT_LINK_COM, "");
    }

    public static String getRobotLinkIP() {
        return PeanutProperties.getValue(PropertiesKey.APP_ROBOT_LINK_IP, "");
    }

    public static int getRobotLinkPort() {
        return PeanutProperties.getValue(PropertiesKey.APP_ROBOT_LINK_PORT, 0);
    }

    public static String getEmotionLinkCOM() {
        return PeanutProperties.getValue(PropertiesKey.APP_EMOTION_LINK_COM, "");
    }

    public static String getDoorLinkCOM() {
        return PeanutProperties.getValue(PropertiesKey.APP_DOOR_LINK_COM, "");
    }

    public static int getDoorNum() {
        return PeanutProperties.getValue(PropertiesKey.ROBOT_DOOR_NUM, 0);
    }

    public static boolean isHrcEnable() {
        return PeanutProperties.getValue(PropertiesKey.APP_HRC_ENABLE, 0) == 1;
    }

    public static boolean isJerkEnable() {
        return PeanutProperties.getValue(PropertiesKey.APP_JERK_ENABLE, 0) == 1;
    }

    public static int getPalletNum() {
        return PeanutProperties.getValue(PropertiesKey.APP_PALLET_NUM, 3);
    }

    public static int getLocationType() {
        return PeanutProperties.getValue(PropertiesKey.MACHINE_LOCATION_TYPE, 0);
    }

    public static int getLidarType() {
        return PeanutProperties.getValue(PropertiesKey.ROBOT_LIDAR_TYPE, -1);
    }

    public static String getMachineType() {
        return PeanutProperties.getValue(PropertiesKey.MACHINE_TYPE, "T1");
    }

    public static String getRobotType() {
        return PeanutProperties.getValue(PropertiesKey.ROBOT_TYPE, "");
    }

    public static int getRms() {
        return PeanutProperties.getValue(PropertiesKey.ROBOT_MAX_SPEED, 90);
    }

    public static String getRpt() {
        return PeanutProperties.getValue(PropertiesKey.ROBOT_PATH_TYPE, "");
    }

    public static int getRcr() {
        return PeanutProperties.getValue(PropertiesKey.ROBOT_RADIUS, -1);
    }

    public static int getRln() {
        return PeanutProperties.getValue(PropertiesKey.RLN, -1);
    }

    public static String getRls() {
        return PeanutProperties.getValue(PropertiesKey.RLS, "");
    }

    public static int getRll() {
        return PeanutProperties.getValue(PropertiesKey.ROBOT_LOG_LEVEL, -1);
    }

    public static int getRsh() {
        return PeanutProperties.getValue(PropertiesKey.RSH, -1);
    }

    public static String getRsi() {
        return PeanutProperties.getValue(PropertiesKey.RSI, "");
    }

    public static String getRso() {
        return PeanutProperties.getValue(PropertiesKey.RSO, "");
    }

    public static String getRsoh() {
        return PeanutProperties.getValue(PropertiesKey.RSOH, "");
    }

    public static String getRss() {
        return PeanutProperties.getValue(PropertiesKey.RSS, "");
    }

    public static String getRsc() {
        return PeanutProperties.getValue(PropertiesKey.RSC, "");
    }

    public static int getRdt() {
        return PeanutProperties.getValue(PropertiesKey.RDT, -1);
    }

    public static int getRdn() {
        return PeanutProperties.getValue(PropertiesKey.RDN, -1);
    }

    public static boolean isExpressEnable() {
        return PeanutProperties.getValue(PropertiesKey.APP_EXPRESS_ENABLE, 1) == 1;
    }

    public static boolean getRef() {
        return PeanutProperties.getValue(PropertiesKey.ROBOT_LIDAR_FILTER_ENABLE, -1) == 1;
    }

    public static boolean getRei() {
        return PeanutProperties.getValue(PropertiesKey.ROBOT_IMU_ENABLE, -1) == 1;
    }

    public static boolean getRsm() {
        return PeanutProperties.getValue(PropertiesKey.RSM, -1) == 1;
    }

    public static boolean getRec() {
        return PeanutProperties.getValue(PropertiesKey.ROBOT_COREDUMP_ENABLE, -1) == 1;
    }
}
