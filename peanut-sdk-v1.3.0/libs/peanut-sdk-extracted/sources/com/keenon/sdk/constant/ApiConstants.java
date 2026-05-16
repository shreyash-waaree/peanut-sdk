package com.keenon.sdk.constant;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/constant/ApiConstants.class */
public class ApiConstants {
    public static final int CODE_SUCCESS = 0;
    public static final int CODE_IN_CHARGING = 19;
    public static final int CODE_DST_NOT_EXIST = 35;
    public static final int CODE_NO_PATH = 81;
    public static final int CODE_INVALID_POS = 256;
    public static final String DELIMITER_COMMA = ",";
    public static final int MOTOR_ENABLE_UNLOCK = 0;
    public static final int MOTOR_ENABLE_LOCK = 1;

    /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/constant/ApiConstants$MotorMove.class */
    public static class MotorMove {
        public static final int FRONT = 1;
        public static final int BACK = 2;
        public static final int LEFT = 3;
        public static final int RIGHT = 4;
    }

    /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/constant/ApiConstants$MotorStatus.class */
    public static class MotorStatus {
        public static final int CODE_BUTTON_UNLOCKED = 16;
        public static final int CODE_APP_UNLOCKED = 32;
        public static final int CODE_ERR_COM_RIGHT = 49;
        public static final int CODE_ERR_COM_LEFT = 50;
        public static final int CODE_ERR_INNER_RIGHT = 51;
        public static final int CODE_ERR_INNER_LEFT = 52;
        public static final int CODE_LOCKED = 255;
    }

    /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/constant/ApiConstants$RobotCheck.class */
    public static class RobotCheck {
        public static final String ONLINE_CHECK = "OnlineCheck";
        public static final String GAZER_CHECK = "GazerCheck";
        public static final String LIDAR_CHECK_RANGING = "LidarCheckRang";
        public static final String LIDAR_CHECK_MATCHING = "LidarCheckMatch";
        public static final String DEPTH_CHECK = "DepthCheck";
        public static final String GAZER_VERIFY = "GazerVerify";
        public static final String LIDAR_VERIFY = "LidarVerify";
        public static final String DEPTH_VERIFY = "DepthVerify";
        public static final String UNION_CALIBRATION_VERIFY = "UnionCalibration";
    }

    /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/constant/ApiConstants$WorkMode.class */
    public static class WorkMode {
        public static final int AUTO = 1;
        public static final int BUILD_MAP = 2;
        public static final int MFG_TEST = 3;
        public static final int CHARGER = 4;
        public static final int MANUAL = 5;
        public static final int ADAPTER_CHARGER = 6;
        public static final int SELF_TEST = 7;
        public static final int ANTI_FALL = 8;
    }
}
