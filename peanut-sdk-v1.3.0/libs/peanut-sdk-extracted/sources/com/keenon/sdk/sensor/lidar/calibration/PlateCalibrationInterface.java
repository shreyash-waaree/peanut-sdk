package com.keenon.sdk.sensor.lidar.calibration;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/sensor/lidar/calibration/PlateCalibrationInterface.class */
public interface PlateCalibrationInterface {

    /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/sensor/lidar/calibration/PlateCalibrationInterface$Opcode.class */
    public enum Opcode {
        READER("读取当前雷达参数", 0),
        WRITER("写入当前雷达参数", 1),
        CALIBRATION("雷达标定", 2),
        OPEN_POINT("点云上报开启", 3),
        CLOSE_POINT("点云上报关闭", 4);

        private String name;
        public int code;

        Opcode(String name, int code) {
            this.name = name;
            this.code = code;
        }

        public int getCode() {
            return this.code;
        }
    }

    /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/sensor/lidar/calibration/PlateCalibrationInterface$PlateIds.class */
    public enum PlateIds {
        FIRST_FLOOR("一层餐盘雷达", 16),
        SECOND_FLOOR("二层餐盘雷达", 17),
        THREE_LAYERS("三层餐盘雷达", 18);

        private String name;
        public int code;

        PlateIds(String name, int code) {
            this.name = name;
            this.code = code;
        }

        public int getCode() {
            return this.code;
        }
    }
}
