package com.keenon.sdk.sensor.door;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/sensor/door/DoorDefine.class */
public class DoorDefine {

    /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/sensor/door/DoorDefine$DoorFaultCode.class */
    public @interface DoorFaultCode {
        public static final int NO_FAULT = 0;
        public static final int LIMIT_SWITCH_FAULT = 1;
        public static final int DOOR_OVERCURRENT_FAULT = 2;
    }

    /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/sensor/door/DoorDefine$DoorStatus.class */
    public @interface DoorStatus {
        public static final int CLOSED = 0;
        public static final int OPENED = 255;
        public static final int DEFAULT = 17;
    }
}
