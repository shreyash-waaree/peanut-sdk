package com.keenon.sdk.sensor.ota.base;

import androidx.annotation.NonNull;
import java.util.HashMap;
import java.util.Map;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/sensor/ota/base/Device.class */
public enum Device {
    GENERAL(-100, "general"),
    UNKNOWN(-2, "unknown"),
    APP(-1, "app"),
    ALGORITHM(0, "algorithm"),
    ROBOT_ARM(1, "robot-arm"),
    STM32(2, "stm32"),
    MOTOR(3, "motor"),
    ANDROID(4, "android"),
    EYE(5, "eye"),
    POWER(6, "power"),
    GAZER(7, "gazer"),
    LIDAR_FRONT(8, "lidar_front"),
    LIDAR_BACK(9, "lidar_back"),
    ROS(11, "ros"),
    VISION_LEFT(12, "vision_left"),
    VISION_RIGHT(13, "vision_right"),
    VISION_SINGLE(14, "vision_single"),
    DOOR(16, "gd32f1-door-ctrl"),
    SCHEDULE(17, "schedule"),
    VSLAM(18, "vslam");

    public final int id;
    public final String name;
    private static Map<Integer, Device> sIdDeviceMap = new HashMap();
    private static Map<String, Device> sNameDeviceMap = new HashMap();

    Device(int id, String name) {
        this.id = id;
        this.name = name;
    }

    private static void checkBuildMap() {
        if (!sIdDeviceMap.isEmpty()) {
            return;
        }
        Device[] devices = {APP, ALGORITHM, ROBOT_ARM, STM32, MOTOR, ANDROID, EYE, POWER, GAZER, LIDAR_FRONT, LIDAR_BACK, ROS, VISION_LEFT, VISION_RIGHT, VISION_SINGLE, DOOR, SCHEDULE, VSLAM};
        for (Device device : devices) {
            sIdDeviceMap.put(Integer.valueOf(device.id), device);
            sNameDeviceMap.put(device.name, device);
        }
    }

    public static Device valueOfId(int id) {
        checkBuildMap();
        if (sIdDeviceMap.containsKey(Integer.valueOf(id))) {
            return sIdDeviceMap.get(Integer.valueOf(id));
        }
        return UNKNOWN;
    }

    public static Device valueOfName(String name) {
        checkBuildMap();
        if (sNameDeviceMap.containsKey(name)) {
            return sNameDeviceMap.get(name);
        }
        return UNKNOWN;
    }

    @Override // java.lang.Enum
    @NonNull
    public String toString() {
        return this.id + "@" + this.name;
    }
}
