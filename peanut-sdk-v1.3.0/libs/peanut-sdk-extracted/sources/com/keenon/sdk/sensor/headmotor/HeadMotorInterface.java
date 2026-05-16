package com.keenon.sdk.sensor.headmotor;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/sensor/headmotor/HeadMotorInterface.class */
public interface HeadMotorInterface {

    /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/sensor/headmotor/HeadMotorInterface$MotorAction.class */
    public enum MotorAction {
        RESET("校准", (byte) 0),
        PA("垂直", (byte) 1),
        PB("倾斜-1", (byte) 2),
        PC("点头", (byte) 3),
        PD("摇头", (byte) 4),
        PE("左右张望", (byte) 5),
        PF("转向", (byte) 6),
        PG("倾斜-2", (byte) 7),
        PH("左转动作", (byte) 8),
        PI("右转动作", (byte) 9),
        PJ("跟踪人脸", (byte) -16),
        PK("垂直动作2(水平居中，垂直1°)", (byte) 10);

        private String name;
        public byte code;

        MotorAction(String name, byte code) {
            this.name = name;
            this.code = code;
        }

        public byte getCode() {
            return this.code;
        }
    }
}
