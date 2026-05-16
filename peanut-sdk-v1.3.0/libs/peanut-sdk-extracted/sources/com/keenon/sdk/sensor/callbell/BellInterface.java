package com.keenon.sdk.sensor.callbell;

import com.keenon.common.error.PeanutError;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/sensor/callbell/BellInterface.class */
public interface BellInterface {
    public static final int DEVICE_GATEWAY = 0;
    public static final int DEVICE_RELAY = 1;
    public static final int DEVICE_SCHEDULE = 2;
    public static final String MSG_TYPE_INIT_CONFIG = "initConfig";
    public static final String MSG_TYPE_UPDATE_ROBOT_STATE = "updateRobotState";
    public static final String MSG_TYPE_REPLY_TASK_STATE = "replyTaskState";

    /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/sensor/callbell/BellInterface$KeyEvent.class */
    public enum KeyEvent {
        ALL("全接收", 100),
        NON("不接收", PeanutError.IllegalArgumentException),
        A("A", 1),
        B("B", 2),
        C("C", 3),
        D("D", 4);

        private String name;
        public int code;

        KeyEvent(String name, int code) {
            this.name = name;
            this.code = code;
        }

        public int getCode() {
            return this.code;
        }
    }

    /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/sensor/callbell/BellInterface$RobotState.class */
    public enum RobotState {
        FREE("空闲", 0),
        BUSY("忙碌", 1);

        private String name;
        public int code;

        RobotState(String name, int code) {
            this.name = name;
            this.code = code;
        }

        public int getCode() {
            return this.code;
        }
    }

    /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/sensor/callbell/BellInterface$TaskState.class */
    public enum TaskState {
        REFUSE("拒绝任务", 0),
        ACCEPT("接受任务", 1),
        FINISH("任务完成", 2),
        CANCEL("取消成功", 3),
        REFUSE_AND_CLEAN("拒绝并且清除", 4);

        private String name;
        public int code;

        TaskState(String name, int code) {
            this.name = name;
            this.code = code;
        }

        public int getCode() {
            return this.code;
        }
    }
}
