# Keenon Peanut Robot — Hardware Architecture Flowchart

> **Version:** Peanut SDK v1.3.0  
> **Last updated:** 2026-03-27

---

## Full Hardware & SDK Architecture

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        ANDROID APP (Top Layer)                              │
│   KeenonApiDemoMain ──► HomeFragment / FormFragment / VoiceFragment         │
└────────────────────────────────┬────────────────────────────────────────────┘
                                 │
                    ┌────────────▼────────────┐
                    │   PeanutSDK (Singleton)  │
                    │  init() / release()      │
                    └─────┬────────┬──────────┘
                          │        │
           ┌──────────────▼──┐  ┌──▼────────────────┐
           │  Sensor Layer   │  │    API Layer        │
           │  (Stateful)     │  │    (Stateless)      │
           └──────┬──────────┘  └───────┬─────────────┘
                  │                     │
                  └──────────┬──────────┘
                             │
              ┌──────────────▼──────────────┐
              │       SCMIoTSender           │
              │   (Protocol Router)          │
              └────┬──────────┬─────────────┘
                   │          │
          ┌────────▼──┐  ┌────▼─────────┐
          │ USB Direct│  │ Serial/Trans  │
          │ (Android) │  │ (ROS Bridge)  │
          └────┬──────┘  └────┬──────────┘
               │              │
               └──────┬───────┘
                      │
         ┌────────────▼────────────────────────────────────────────┐
         │                   STM32 (Chassis MCU)                    │
         │              Device ID: 0  (Motor) etc.                  │
         └──┬───┬───┬───┬───┬───┬───┬───┬───┬───┬───┬─────────────┘
            │   │   │   │   │   │   │   │   │   │   │
    ┌───────┘   │   │   │   │   │   │   │   │   │   └──────────────┐
    │           │   │   │   │   │   │   │   │   │                  │
    ▼           ▼   ▼   ▼   ▼   ▼   ▼   ▼   ▼   ▼                  ▼
DRIVE        HEAD  DOOR DOOR UVLAMP PLASMA JACK CALL BATTERY    LED
MOTORS       MOTOR  22  23   44-48   41-43   57  BELL  56      LIGHTS
(Wheel)      (66)  -25        │      │       │    65   │        │
 SensorMotor SensorHd SensorDoor SensorUV SensorPlasma│ SensorCallBell
              eadMotor         SensorJacking SensorBattery SensorLight
```

---

## Sensor Layer — Component Connections

```
SensorMotor ──────────────────────────────► Drive Wheels (Chassis)
  │  getMotorState()
  └─ Event: REPORT_STM32_MOTOR_STATE_ACK

SensorHeadMotor (Dev: 66) ────────────────► Head Tilt/Rotate Motor
  │  getHeadMotorState()
  │  onControlHeadMotorPlay(action, angle)
  └─ Events: REPORT_HEAD_MOTOR_STATE_EVENT
             PLAY_CONTROL_HEAD_MOTOR_ACK

SensorDoor (Dev: 22-25) ─────────────────► Compartment Doors (x4)
  │  getDoorStatus(doorId, callback)
  │  setDoorSwitch(doorId, isOpen)
  └─ Events: DOOR_SWITCH_STATUS (topic)
             DOOR_SWITCH_FAULT (topic)

SensorLight ──────────────────────────────► LED Strip / Ring
  │  displayLight(id, intensity, mode)
  │  play(LightConfig)   ──► Single Color
  │                      ──► Multi Color V1/V2/V3
  │  setRGB(LightConfig)
  │  onStartHeartBeatV3() ──► Every 2 sec keep-alive
  └─ Events: PLAY_LIGHT_SINGLE_ACK
             PLAY_LIGHT_MULTI_V1/V2/V3_ACK

SensorUVLamp (Dev: 44-48) ───────────────► UV Germicidal Lamps (x5)
  │  getUVLampSwitch(dev, callback)
  │  setUVLampSwitch(dev, isOpen)
  │  setDevs(int... devs)
  └─ Events: SET_UV_LAMP_SWITCH_ACK
             UV_LAMP_FAULT (topic)

SensorPlasma (Dev: 41,42,43) ────────────► Plasma Ionizer + Fan
  │  subscribePlasmaEnvironmentInfo()
  │  setPlasmaSwitch(isOpen)
  │  setFanSwitchAndLevel(FanLeve)
  │  getFanSwitchState(callback)
  └─ Events: SET_PLASMA_SWITCH_ACK
             SET_PLASMA_FAN_SWITCH_ACK
             PLASMA_FAULT (topic)

SensorJacking (Dev: 57) ─────────────────► Vertical Lift Platform
  │  getJackingState()
  │  onControlUpRise()
  │  onControlDrop()
  └─ Events: GET_JACKING_STATE_ACK
             CONTROL_JACKING_ACK
             REPORT_JACKING_STATE

SensorBattery (Dev: 56) ─────────────────► Battery Pack (BMS)
  │  enterTransportModel(callback)
  └─ Subscribes: BOTTOM_RAW (BMS events)

SensorEmotion ────────────────────────────► Face Display (LCD/OLED)
  │  runEmotion(callback, version, data, force, repeat)
  │  runLocalEmotion(callback, emotionId)
  │  setIdleEmotion(callback, emotion)
  │  setStartupEmotion(callback, emotion)
  └─ State Machine: IDLE → SEND_START → SEND_DATA → SEND_END → DONE

SensorCallBell (Dev: 65) ────────────────► Wireless Bell Devices
  │  initBell(config)
  │  updateBellState(config)
  │  getDeviceList()
  │  replyTaskState(taskId, state)
  │  onStartHeartBeat()  ──► Every 5 sec keep-alive
  └─ Events: RECEIVED_CALL_BELL_TASK
             RECEIVED_CALL_BELL_DEVICE_LIST

SensorLidarPlate (Dev: 15) ──────────────► Tray LiDAR (4 zones)
  │  detect()
  └─ Event: LIDAR_PLATE_DETECT (plate 1-4 status)

SensorOTA ────────────────────────────────► Firmware Update Engine
  │  start(config, listener)  ──► RobotOTAImpl (ARM)
  │                           ──► DoorOTAImpl  (Door)
  │                           ──► RosOTAImpl   (ROS/STM32)
  └─ stop() / release()
```

---

## API Layer — Physical Sensor APIs

```
SensorGazerApi   (/sensor/gazer) ─────────► Locator Tag Sensor
  send() / observe() / cancel()
  Returns: LocatorData[id, degreeAngle, X, Y, Z cm]

SensorLidarApi   ─────────────────────────► Navigation LiDAR (360°)
  send() / observe() / cancel()

SensorDepthApiV2 ─────────────────────────► Depth Camera (RGB-D)
  send() / observe() / cancel()

SensorInfraredApi ────────────────────────► IR Proximity Sensors
  send() / observe() / cancel()

SensorSonarApi   ─────────────────────────► Ultrasonic Sensors
  send() / observe() / cancel()

SensorCollisionApi ───────────────────────► Bump / Collision Bumpers
  send() / observe() / cancel()

SensorTouchApi   ─────────────────────────► Touch Sensor
  send() / observe() / cancel()

SensorImuApi     ─────────────────────────► IMU (Accelerometer + Gyro)
  send() / observe() / cancel()

SensorImuAngleApi ────────────────────────► Processed Tilt Angles
  send() / observe() / cancel()

SensorAntiFallLidarApi ───────────────────► Anti-Fall Cliff Sensors
  send() / observe() / cancel()

SensorPsdApi     ─────────────────────────► PSD Optical Distance Sensor
  send() / observe() / cancel()

ButtonStatusApi  ─────────────────────────► Physical Buttons
  send() / observe() / cancel()
```

---

## Navigation & System Flow

```
Navigation Flow:
NavigationSetTargetApi ──► NavigationActionApi (START)
        │
        ├──► NavigationSpeedApi (adjust speed)
        ├──► NavigationRemainingPathApi (monitor progress)
        ├──► NavigationStableModeApi (fragile payload mode)
        ├──► NavigationSportModeApi  (fast mode)
        └──► NavigationActionApi (STOP / PAUSE / RESUME)

Position Flow:
PositionActionApi ──► Triggers localization
        │
        ├──► PositionStatusApi    (is robot localized?)
        ├──► PositionPosInfoApi   (current X, Y, heading)
        └──► PositionNotifyApi    (subscribe to position updates)

Charging Flow:
ChargeAutoApi ──► Robot auto-docks when battery low
ChargeManualApi ──► Force dock now
ChargeStopApi ──► Stop charging session

Map Flow:
MapDownloadApi ──► Download map from server
        │
        ├──► MapDownLoadStatusV2Api  (monitor download)
        ├──► SensorMap (load / switch active map)
        └──► MapUploadApi (upload modified map)

OTA Firmware Update Flow:
SensorOTA.start(config, listener)
        │
        ├──► Device = ROBOT_ARM ──► RobotOTAImpl
        ├──► Device = DOOR      ──► DoorOTAImpl
        └──► Device = ROS/STM32 ──► RosOTAImpl (V1) or RosOTAImplV2 (V2)

Disinfection Flow:
DisinfectUltravioletApi ──► UV Lamp on
NavigationDisinfectActionApi ──► Robot navigates + disinfects
DisinfectAtomizerApi ──► Spray disinfectant
DisinfectFanApi ──► Fan for air circulation
NavigationDisinfectStatusApi ──► Monitor completion
```

---

## Communication Channel Decision Tree

```
App sends hardware command
          │
          ▼
    USB Direct mode?
    ─────────────────
         YES ──► setUSBDirect(true) ──► Direct USB to STM32/Door/LED
         │
         NO
         │
    Serial Direct mode?
    ──────────────────────
         YES ──► setSerialDirect(true) ──► UART serial to STM32
         │
         NO
         │
    Trans (Bridge) mode?
    ─────────────────────────
         YES ──► setTrans(true) ──► Through ROS/network bridge
         │
         NO
         │
    Default (Remote)
         │
         └──► CoAP over Wi-Fi ──► Robot's internal CoAP server
```

---

*See [HARDWARE_REFERENCE.md](HARDWARE_REFERENCE.md) for detailed descriptions of each hardware component and its SDK functions.*
