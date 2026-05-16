package com.keenon.sdk.sensor.common;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/sensor/common/SensorEvent.class */
public class SensorEvent {
    public static final String REQUEST_FAILED = "_sensor.request.failed";
    public static final String VISION_DEPTH_CAPTURE = "_sensor.vision.depth.capture";
    public static final String LIDAR_PLATE_DETECT = "_sensor.lidar.plate.detect";
    public static final String PLAY_LIGHT_WARNING_ACK = "_sensor.light.warning.ack";
    public static final String PLAY_LIGHT_SINGLE_ACK = "_sensor.light.playSingle.ack";
    public static final String PLAY_LIGHT_MULTI_V1_ACK = "_sensor.light.playMulti.v1.ack";
    public static final String SET_LIGHT_RGB_V1_ACK = "_sensor.light.setRGB.v1.ack";
    public static final String PLAY_LIGHT_MULTI_V2_ACK = "_sensor.light.playMulti.v2.ack";
    public static final String PLAY_LIGHT_MULTI_V3_ACK = "_sensor.light.playMulti.v3.ack";
    public static final String PLAY_LIGHT_HeartBeat_V3_ACK = "_sensor.light.heartBeat.v3.ack";
    public static final String SET_LIGHT_RGB_V2_ACK = "_sensor.light.setRGB.v2.ack";
    public static final String SET_UV_LAMP_SWITCH_ACK = "_sensor.uvlamp.setSwitch.ack";
    public static final String SET_DOOR_SWITCH_ACK = "_sensor.door.setSwitch.ack";
    public static final String SET_PLASMA_SWITCH_ACK = "_sensor.plasma.setSwitch.ack";
    public static final String SET_PLASMA_FAN_SWITCH_ACK = "_sensor.plasma.fan.setSwitch.ack";
    public static final String SET_PLASMA_ENVIRONMENT_SWITCH_ACK = "_sensor.plasma.evm.setSwitch.ack";
    public static final String REPORT_PLASMA_ENVIRONMENT_INFO_ACK = "_sensor.plasma.evm.report.ack";
    public static final String REPORT_PLASMA_FAULT = "_sensor.plasma.fault";
    public static final String CONTROL_JACKING_ACK = "_sensor.jacking.ack";
    public static final String GET_JACKING_STATE_ACK = "_sensor.jacking.get.state.ack";
    public static final String REPORT_JACKING_STATE = "_sensor.jacking.event.state.ack";
    public static final String REPLAY_CALL_BELL_INIT_ACK = "_sensor.call.bell.init.ack";
    public static final String REPLAY_CALL_BELL_UPDATE_STATE_ACK = "_sensor.call.bell.update.state.ack";
    public static final String REPLAY_CALL_BELL_TASK_STATE_ACK = "_sensor.call.bell.update.task.state.ack";
    public static final String RECEIVED_CALL_BELL_TASK = "_sensor.call.bell.received.task";
    public static final String RECEIVED_CALL_BELL_DEVICE_LIST = "_sensor.call.bell.received.device.list";
    public static final String REPORT_STM32_ANTI_MOTOR_SWITCH_ACK = "_sensor.stm32.ani.motor.switch.ack";
    public static final String REPORT_STM32_ANTI_MOTOR_SWITCH_ERROR_ACK = "_sensor.stm32.ani.motor.switch.error.ack";
    public static final String REPORT_STM32_MOTOR_STATE_ACK = "_sensor.stm32.motor.state.ack";
    public static final String REPORT_STM32_MOTOR_STATE_ERROR_ACK = "_sensor.stm32.motor.state.error.ack";
    public static final String REPLAY_GET_HEAD_MOTOR_STATE_ACK = "_sensor.head.motor.state.ack";
    public static final String PLAY_CONTROL_HEAD_MOTOR_ACK = "_sensor.play.head.motor.ack";
    public static final String REPORT_HEAD_MOTOR_STATE_EVENT = "_sensor.play.head.motor.event";
    public static final String REPORT_HEAD_MOTOR_FAULT_EVENT = "_sensor.play.head.motor.fault";
    public static final String GET_PLATE_LIDAR_CB_STATE = "_sensor.plate.lidar.cb.state";
    public static final String GET_PLATE_LIDAR_CB_EVENT = "_sensor.plate.lidar.cb.event";
    public static final String GET_PLATE_LIDAR_CB_FAIL = "_sensor.plate.lidar.cb.fail";
    public static final String GET_PLATE_LIDAR_CB_PROP = "_sensor.plate.lidar.cb.prop";
    public static final String REPORT_MAP_DOWNLOAD_STATUS = "_sensor.map.download.status";
    public static final String REPORT_MAP_DOWNLOAD_FAULT_EVENT = "_sensor.map.download.fault.event";
}
