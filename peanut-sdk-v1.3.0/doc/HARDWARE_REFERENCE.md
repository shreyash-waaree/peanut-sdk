# Keenon Peanut Robot — Hardware Reference & SDK Function Guide

> **Version:** Peanut SDK v1.3.0  
> **Last updated:** 2026-03-27  
> **Scope:** All physical hardware components present on the Keenon Peanut robot, their purpose, and the SDK functions that control or read them.

---

## Table of Contents

1. [Overview](#overview)
2. [Architecture Summary](#architecture-summary)
3. [Hardware Components](#hardware-components)
   - [1. Drive Motors (Chassis)](#1-drive-motors-chassis)
   - [2. Head Motor](#2-head-motor)
   - [3. LiDAR (Navigation)](#3-lidar-navigation)
   - [4. LiDAR Plate Sensor (Tray Detection)](#4-lidar-plate-sensor-tray-detection)
   - [5. Depth / Vision Sensors](#5-depth--vision-sensors)
   - [6. Infrared Sensors](#6-infrared-sensors)
   - [7. Anti-Fall LiDAR](#7-anti-fall-lidar)
   - [8. Sonar (Ultrasonic) Sensor](#8-sonar-ultrasonic-sensor)
   - [9. Bump / Collision Sensors](#9-bump--collision-sensors)
   - [10. Touch Sensor](#10-touch-sensor)
   - [11. IMU (Inertial Measurement Unit)](#11-imu-inertial-measurement-unit)
   - [12. PSD Sensor](#12-psd-sensor)
   - [13. Gazer (Locator / Tag Sensor)](#13-gazer-locator--tag-sensor)
   - [14. LED Light Strip](#14-led-light-strip)
   - [15. Doors (Compartment Doors)](#15-doors-compartment-doors)
   - [16. UV Lamp (Disinfection)](#16-uv-lamp-disinfection)
   - [17. Plasma Air Purifier](#17-plasma-air-purifier)
   - [18. Jacking Mechanism](#18-jacking-mechanism)
   - [19. Battery](#19-battery)
   - [20. Emotion / Face Display](#20-emotion--face-display)
   - [21. Call Bell (Wireless Bell Module)](#21-call-bell-wireless-bell-module)
   - [22. STM32 Microcontroller](#22-stm32-microcontroller)
   - [23. Gravity / Tilt Sensor (Plate)](#23-gravity--tilt-sensor-plate)
   - [24. Button Panel](#24-button-panel)
   - [25. OTA Update Engine](#25-ota-update-engine)
4. [API-Layer Controls (No Physical Hardware, Logic Only)](#api-layer-controls-no-physical-hardware-logic-only)
   - [Navigation & Movement APIs](#navigation--movement-apis)
   - [Charging APIs](#charging-apis)
   - [Map APIs](#map-apis)
   - [Scheduling APIs](#scheduling-apis)
   - [Disinfection Control APIs](#disinfection-control-apis)
5. [Common SDK Patterns](#common-sdk-patterns)
6. [Device ID Quick Reference](#device-id-quick-reference)

---

## Overview

The Keenon Peanut robot is a service robot built on an Android-based platform. It communicates with its hardware subsystems through the **Peanut SDK**, which abstracts all physical hardware behind Java classes using either a CoAP (Constrained Application Protocol) or SCM-IoT binary protocol.

There are two layers of SDK interaction:

| Layer | Classes | Purpose |
|---|---|---|
| **Sensor layer** | `com.keenon.sdk.sensor.*` | Higher-level, stateful wrappers — manage lifecycle, caching, and event callbacks |
| **API layer** | `com.keenon.sdk.api.*` | Low-level one-shot CoAP request/response wrappers for individual hardware commands |

---

## Architecture Summary

```
Android App
    │
    └── PeanutSDK (singleton)
            │
            ├── Sensor Layer (stateful singletons)
            │       SensorDoor, SensorLight, SensorUVLamp, SensorEmotion,
            │       SensorMotor, SensorHeadMotor, SensorBattery,
            │       SensorCallBell, SensorJacking, SensorPlasma,
            │       SensorLidarPlate, SensorOTA, SensorMap, ...
            │
            └── API Layer (stateless one-shot)
                    DoorOpenApi, DoorCloseApi, LightDisplayApi,
                    SensorGazerApi, SensorLidarApi, NavigationActionApi,
                    ChargeAutoApi, EmotionRunApi, DisinfectUltravioletApi, ...
```

All hardware communication flows through `SCMIoTSender` (serial/USB/bridge) or CoAP over the robot's internal Wi-Fi bridge.

---

## Hardware Components

---

### 1. Drive Motors (Chassis)

**What it is:** The two main drive wheels (differential-drive chassis) that move the robot forward, backward, and rotate it in place.

**Sensor class:** `SensorMotor` (`com.keenon.sdk.sensor.Motor.SensorMotor`)

**SDK Functions:**

| Function | Description |
|---|---|
| `SensorMotor.ins()` | Returns the singleton instance of the motor sensor. |
| `getMotorState()` | Queries the current state of the drive motors (running, stopped, fault). The result is delivered as a `MotorStatus` object via the `REPORT_STM32_MOTOR_STATE_ACK` event. |
| `release()` | Cleans up all motor callbacks and listeners. Call this when the app is shutting down. |

**Related API classes:**
- `MotorEnableApi` — Enables or disables the drive motor controller.
- `MotorHealthApi` — Retrieves the health/diagnostic status of the motor hardware.
- `MotorSpeedApi` — Sets or reads the motor's target speed.
- `MotorSMaxApi` — Sets the maximum allowed speed limit for the motors.
- `MotorGMaxApi` — Sets the maximum allowed acceleration (G-force limit).
- `MotorHrcApi` — Controls motor high-resolution current settings.
- `MotorManualApi` — Puts the motor into a manually-controlled mode (used for testing/maintenance).
- `MotorEncoderApi` — Reads encoder data (wheel rotation count/position odometry).

**Events fired:**
- `REPORT_STM32_MOTOR_STATE_ACK` — Motor state has been reported.
- `REPORT_STM32_MOTOR_STATE_ERROR_ACK` — Motor state read failed.

---

### 2. Head Motor

**What it is:** A motorized neck/head joint that physically tilts or rotates the robot's head display. Supports both single-axis (yaw/nod) and dual-axis (X + Y) movement.

**Sensor class:** `SensorHeadMotor` (`com.keenon.sdk.sensor.headmotor.SensorHeadMotor`)

**Device ID:** `66`

**SDK Functions:**

| Function | Description |
|---|---|
| `SensorHeadMotor.getInstance()` | Returns the singleton. |
| `getHeadMotorState()` | Reads the current motor state (idle/moving) and its current angle positions (angleX, angleY). Result fires `REPLAY_GET_HEAD_MOTOR_STATE_ACK`. |
| `onControlHeadMotorPlay(MotorAction action)` | Sends a movement command using a predefined `MotorAction` (e.g. nod, shake, center). |
| `onControlHeadMotorPlay(MotorAction action, short angle)` | Moves the head to a specific single-axis angle (degrees, as unsigned short). |
| `onControlHeadMotorPlay(MotorAction action, short angleX, short angleY)` | Moves the head to a specific dual-axis position using both horizontal (X) and vertical (Y) angles simultaneously. |
| `release()` | Unregisters all callbacks. |

**Events fired:**
- `REPORT_HEAD_MOTOR_STATE_EVENT` — Periodic state update with current angles.
- `PLAY_CONTROL_HEAD_MOTOR_ACK` — Acknowledgement that a movement command was received.
- `REPLAY_GET_HEAD_MOTOR_STATE_ACK` — Response to `getHeadMotorState()`.
- `REPORT_HEAD_MOTOR_FAULT_EVENT` — Hardware fault detected.

---

### 3. LiDAR (Navigation)

**What it is:** A 2D rotating laser distance scanner (LiDAR) used for the robot's primary obstacle avoidance and SLAM-based navigation. This is the main sensor that builds the map of the environment.

**API classes:**
- `SensorLidarApi` — Reads the current LiDAR scan data (distance measurements in a 360° sweep).
- `SensorLidarT2Api` — LiDAR data for T2-generation hardware.
- `SensorAntiFallLidarApi` — Dedicated anti-fall LiDAR (see section 7).

**SDK Functions (via `SensorLidarApi`):**

| Function | Description |
|---|---|
| `send(IDataCallback)` | Performs a one-time GET request for the current LiDAR scan. The callback receives the raw scan data. |
| `observe(IDataCallback)` | Subscribes to continuous LiDAR scan updates. The callback is fired repeatedly as new scans arrive. |
| `cancel()` | Cancels the active subscription. |

**Data returned:** Raw distance measurements across 360° angles from the robot's center.

---

### 4. LiDAR Plate Sensor (Tray Detection)

**What it is:** A downward-facing LiDAR used to detect the presence and status of items on the robot's tray or delivery plate. It measures across 4 individual plate zones.

**Sensor class:** `SensorLidarPlate` (`com.keenon.sdk.sensor.lidar.SensorLidarPlate`)

**Device ID:** `15`

**SDK Functions:**

| Function | Description |
|---|---|
| `SensorLidarPlate.getInstance()` | Returns the singleton. |
| `detect()` | Triggers a one-shot detection scan across all 4 tray plate zones. Result fires `LIDAR_PLATE_DETECT` event with 4 status values (one per plate zone, IDs 1–4). |
| `release()` | Releases the sensor (no-op in current implementation). |

**Events fired:**
- `LIDAR_PLATE_DETECT` — Detection result containing a list of `DataBean` objects (id = 1–4, status = 0/1 for empty/occupied).

**Calibration:** See `SensorLidarPlateCalibration` for factory-level calibration of tray LiDAR parameters.

---

### 5. Depth / Vision Sensors

**What it is:** RGB-D depth camera(s) used for 3D environment perception, obstacle recognition, and in some models, person-following.

**API classes:**
- `SensorDepthApi` — V1 depth sensor interface.
- `SensorDepthApiV2` — V2 depth sensor with improved resolution.
- `SensorDepthT2Api` — T2-hardware depth sensor variant.
- `SensorVisionDepth` (sensor layer) — Sensor wrapper.

**SDK Functions (via `SensorDepthApiV2`):**

| Function | Description |
|---|---|
| `send(IDataCallback)` | Single depth frame request. |
| `observe(IDataCallback)` | Subscribe to continuous depth frames. |
| `cancel()` | Stop subscription. |

---

### 6. Infrared Sensors

**What it is:** Short-range infrared (IR) proximity sensors mounted on the robot body for close-range obstacle detection (typically the front and sides).

**API classes:**
- `SensorInfraredApi` — Current IR sensor interface.
- `SensorInfraredApiOld` — Legacy IR sensor interface (older hardware).

**SDK Functions:**

| Function | Description |
|---|---|
| `send(IDataCallback)` | Reads current IR proximity values. |
| `observe(IDataCallback)` | Subscribe to continuous IR readings. |
| `cancel()` | Stop subscription. |

---

### 7. Anti-Fall LiDAR

**What it is:** Downward-facing laser sensors that detect drop-offs, stairs, and ledges in the robot's path to prevent the robot from falling. These are separate from the navigation LiDAR.

**API class:** `SensorAntiFallLidarApi`

**Sensor layer:** `AntiFallStatusApi` — Subscribes to fault/triggered status reports.

**SDK Functions:**

| Function | Description |
|---|---|
| `send(IDataCallback)` | Reads the current anti-fall sensor reading. |
| `observe(IDataCallback)` | Subscribe to continuous anti-fall readings. |
| `cancel()` | Stop subscription. |

**Calibration:** `AntiFallCalibrationApi` — Resets or calibrates anti-fall sensor thresholds.

---

### 8. Sonar (Ultrasonic) Sensor

**What it is:** Ultrasonic distance sensors for short-to-medium range obstacle detection, typically supplementing the LiDAR in areas where the laser cannot detect transparent objects.

**API class:** `SensorSonarApi`

**SDK Functions:**

| Function | Description |
|---|---|
| `send(IDataCallback)` | Single sonar distance reading request. |
| `observe(IDataCallback)` | Subscribe to continuous sonar readings. |
| `cancel()` | Stop subscription. |

---

### 9. Bump / Collision Sensors

**What it is:** Physical contact bumpers mounted on the robot's exterior that detect when the robot physically collides with an object or person. Used as a last-resort safety trigger.

**API classes:**
- `SensorCollisionApi` — General collision/bump sensor.
- `SensorBumpStatusApi` — Reports the detailed bump sensor status (which zone was hit).

**SDK Functions:**

| Function | Description |
|---|---|
| `send(IDataCallback)` | Reads current bump sensor status. |
| `observe(IDataCallback)` | Subscribe to bump events (fires on contact). |
| `cancel()` | Stop subscription. |

---

### 10. Touch Sensor

**What it is:** Capacitive or resistive touch sensor(s) on the robot body (usually the head or torso) that let users interact with the robot by physical touch.

**API classes:**
- `SensorTouchApi` — Current touch sensor interface.
- `SensorTouchApiOld` — Legacy touch sensor interface.

**SDK Functions:**

| Function | Description |
|---|---|
| `send(IDataCallback)` | Reads the current touch sensor state. |
| `observe(IDataCallback)` | Subscribe to touch events (fires on every touch/release). |
| `cancel()` | Stop subscription. |

---

### 11. IMU (Inertial Measurement Unit)

**What it is:** A 6-axis or 9-axis IMU (accelerometer + gyroscope ± magnetometer) that measures the robot's orientation, angular velocity, and linear acceleration. Used for balance correction and slope detection.

**API classes:**
- `SensorImuApi` — Raw IMU data feed.
- `SensorImuAngleApi` — Processed angle readings (roll, pitch, yaw) derived from the IMU.
- `ImuResetApi` — Resets/recalibrates the IMU to a known orientation.
- `NavigationRobotOnSlopeApi` — Reports when the IMU detects the robot is on a slope.

**SDK Functions (via `SensorImuApi`):**

| Function | Description |
|---|---|
| `send(IDataCallback)` | Single IMU reading. |
| `observe(IDataCallback)` | Subscribe to continuous IMU data stream. |
| `cancel()` | Stop subscription. |

**SDK Functions (via `ImuResetApi`):**

| Function | Description |
|---|---|
| `send(IDataCallback)` | Sends a reset command to zero out the IMU's reference orientation. |

---

### 12. PSD Sensor

**What it is:** A PSD (Position Sensitive Detector) optical sensor. Detects distance by the angle of reflected infrared light. Used for close-range environment awareness, typically for docking/charging alignment.

**API class:** `SensorPsdApi`

**SDK Functions:**

| Function | Description |
|---|---|
| `send(IDataCallback)` | Single PSD distance reading. |
| `observe(IDataCallback)` | Subscribe to continuous PSD readings. |
| `cancel()` | Stop subscription. |

---

### 13. Gazer (Locator / Tag Sensor)

**What it is:** A sensor (likely UWB or visual tag reader) that detects and locates positioning beacons/tags placed in the environment. Used for label-based (QR/UWB/RFID) positioning mode as an alternative to LiDAR-SLAM.

**API class:** `SensorGazerApi` (`com.keenon.sdk.api.SensorGazerApi`)

**CoAP endpoint:** `/sensor/gazer`

**SDK Functions:**

| Function | Description |
|---|---|
| `send(IDataCallback)` | Performs a one-time GET of all visible locator tags. Returns a `Bean` containing `DataBean` (count + base64 raw data) and `LocatorBean` (parsed list of up to N locators). |
| `observe(IDataCallback)` | Subscribes to continuous tag updates — the callback fires whenever a tag enters or updates in the sensor's field of view. |
| `cancel()` | Cancels the active subscription. |

**Data returned per locator tag (`LocatorData`):**

| Field | Type | Description |
|---|---|---|
| `id` | `int` | Tag hardware ID |
| `degreeAngle` | `float` | Angle to the tag in degrees |
| `centimeterX` | `float` | X position of the tag in centimeters |
| `centimeterY` | `float` | Y position of the tag in centimeters |
| `centimeterZ` | `float` | Z (height) position of the tag in centimeters |

**Also related:** `SensorRfid` sensor — handles RFID-based tag detection for similar label-positioning use.

---

### 14. LED Light Strip

**What it is:** RGB LED strips or matrices mounted on the robot body (typically around the base, torso, or head ring). Used for visual status indication, mood expression, and ambient lighting during interaction.

**Sensor class:** `SensorLight` (`com.keenon.sdk.sensor.light.SensorLight`)

**API class:** `LightDisplayApi`

**SDK Functions:**

| Function | Description |
|---|---|
| `SensorLight.getInstance()` | Returns the singleton. |
| `displayLight(int lightId, Integer intensity, int mode)` | Simple display command — set a specific light zone (`lightId`) to a given `intensity` and animation `mode`. Fires `PLAY_LIGHT_WARNING_ACK` on completion. |
| `play(LightConfig lightConfig)` | Advanced playback. Internally selects `playSingleColor()` or `playMultiColor()` based on the config `type`. |
| `setRGB(LightConfig lightConfig)` | Directly sets the RGB color of the strip without an animation. |
| `onStartHeartBeatV3()` | Starts an automatic 2-second periodic heartbeat light pulse (keeps the V3 LED controller alive). |
| `onStopHeartBeatV3()` | Stops the heartbeat. |
| `setHeartBeatDev(int dev)` | Changes which device ID receives the heartbeat signal. |
| `release()` | Cleans up the sensor. |

**`LightConfig` key fields:**

| Field | Description |
|---|---|
| `devId` | Target light device ID |
| `type` | `0` = single color, `1` = multi-color |
| `ver` | Protocol version: `"v1.0"`, `"v2.0"`, or `"v3.0"` |
| `blink` | Blink parameters: `onTime`, `offTime`, `repeat` |
| `beads` | LED bead configuration: `num`, `head`, `bytes` (RGB array) |
| `effect` | Animation effect: `id` (effect index), `time` (duration ms) |
| `color` | RGBA: `mask`, `r`, `g`, `b` |

**Also related:** `door/LightUsbVersionApi` — version negotiation for USB-connected door-integrated lights.

---

### 15. Doors (Compartment Doors)

**What it is:** Motorized compartment doors that open and close to reveal or conceal storage/delivery compartments. Multiple doors can be fitted on one robot (up to 4 supported by default: IDs 22–25).

**Sensor class:** `SensorDoor` (`com.keenon.sdk.sensor.door.SensorDoor`)

**Default device IDs:** `22, 23, 24, 25`

**SDK Functions:**

| Function | Description |
|---|---|
| `SensorDoor.getInstance()` | Returns the singleton. |
| `getDoorStatus(int doorId, IDataCallback2<DoorStatusInfo>)` | Queries the open/closed status of a single door. The callback receives a `DoorStatusInfo` with `doorId` and a status byte. |
| `getDoorStatus(IDataCallback2, int... doorIds)` | Batch version — queries multiple doors. If more than one door is queried, requests are sent sequentially with a configurable interval (default 30 ms) to avoid bus collision. |
| `getAllDoorStatusUseCache()` | Returns all door statuses from the local cache. If any status is still the default (unread) value, it fetches fresh data from hardware before notifying. Publishes results to the `DOOR_SWITCH_STATUS` topic. |
| `setDoorSwitch(int doorId, boolean isOpen)` | Opens (`true`) or closes (`false`) a specific door by ID. |
| `setDoorSwitch(boolean isOpen, int... doorIds)` | Opens or closes multiple doors in sequence. |
| `changeSupportDevices(int[] doorDevices)` | Reconfigures which device IDs are treated as doors (allows custom hardware configurations). |
| `changeCmsExeInterval(int interval)` | Changes the delay (in ms) between sequential door commands to prevent serial bus collisions. |
| `release()` | Unregisters all callbacks and listeners. |

**Related API classes:**
- `DoorOpenApi` — Sends an open command to a specific door device.
- `DoorCloseApi` — Sends a close command to a specific door device.
- `DoorStatusApi` — Reads door open/close status via CoAP.
- `DoorLockStatusApi` / `DoorLockAllStatusApi` / `DoorLockSingleStatusApi` — Read the lock mechanism status.
- `DoorFaultApi` — Reads fault codes reported by door hardware.
- `DoorLimitFaultApi` — Reads limit-switch fault status (e.g., door jammed).
- `DoorTypeApi` — Reads which type of door hardware is fitted.
- `DoorTypeObserveApi` — Subscribes to door type changes.
- `DoorStatusObserveApi` — Subscribes to real-time door status changes.
- `DoorTypeFourSetApi`, `DoorTypeThreeSetApi`, `DoorTypeDoubleSetApi`, `DoorTypeAutoSetApi` — Configures door type (4-door, 3-door, 2-door, auto-detect).

**Events fired:**
- `DOOR_SWITCH_STATUS` (topic) — Published whenever door status changes.
- `DOOR_SWITCH_FAULT` (topic) — Published when a door fault is reported.
- `SET_DOOR_SWITCH_ACK` — Fires after a setDoorSwitch command is acknowledged.

---

### 16. UV Lamp (Disinfection)

**What it is:** Ultraviolet germicidal lamps mounted on the robot body. Used to disinfect surfaces or air as the robot navigates a space. Up to 5 UV lamp devices are supported (IDs 44–48).

**Sensor class:** `SensorUVLamp` (`com.keenon.sdk.sensor.uvlamp.SensorUVLamp`)

**Default device IDs:** `44, 45, 46, 47, 48`

**SDK Functions:**

| Function | Description |
|---|---|
| `SensorUVLamp.getInstance()` | Returns the singleton. |
| `getUVLampSwitch(int dev, IDataCallback2<Boolean>)` | Queries whether a specific UV lamp is currently on (`true`) or off (`false`). |
| `setUVLampSwitch(int dev, boolean isOpen)` | Turns a specific UV lamp on (`true`) or off (`false`). Fires `SET_UV_LAMP_SWITCH_ACK` on completion. |
| `setDevs(int... devs)` | Reconfigures which device IDs are treated as UV lamps (allows custom wiring). |
| `release()` | Unregisters all callbacks. |

**Events fired:**
- `SET_UV_LAMP_SWITCH_ACK` — Acknowledgement for on/off command.
- `UV_LAMP_FAULT` (topic) — Published when any UV lamp reports a hardware fault. The payload contains `UVLampStatusInfo` with a list of per-lamp status codes.

**UV lamp fault status codes (`UVLampStatus`):**
- `NO_FAULT` — Normal operation.
- Other codes indicate specific hardware faults.

**Also related:**
- `DisinfectUltravioletApi` — Low-level CoAP API for UV-based disinfection control.
- `DisinfectStatusApi` — Reports the current overall disinfection session status.
- `NavigationDisinfectStatusApi` — Reports whether a disinfection navigation task is in progress.
- `NavigationDisinfectActionApi` — Starts or stops a navigation-integrated disinfection task.

---

### 17. Plasma Air Purifier

**What it is:** A plasma ionizer and fan assembly used for air purification. The plasma unit generates ions to neutralize airborne pathogens and pollutants, while the fan controls airflow rate.

**Sensor class:** `SensorPlasma` (`com.keenon.sdk.sensor.plasma.SensorPlasma`)

**Device IDs:** Environment sensor `41`, Plasma switch `42`, Fan `43`

**SDK Functions:**

| Function | Description |
|---|---|
| `SensorPlasma.getInstance()` | Returns the singleton. |
| `subscribePlasmaEnvironmentInfo()` | Subscribes to environment data published by the plasma sensor (air quality metrics). Result fires `REPORT_PLASMA_ENVIRONMENT_INFO_ACK` and is also published to the `PLASMA_ENVIRONMENT` topic. |
| `setPlasmaEnvironmentSwitch(boolean isOpen)` | Enables or disables the environment monitoring sensor on the plasma unit. |
| `setPlasmaSwitch(boolean isOpen)` | Turns the plasma ionizer itself on (`true`) or off (`false`). Fires `SET_PLASMA_SWITCH_ACK`. |
| `setFanSwitchAndLevel(FanLeve level)` | Sets the fan speed level. `FanLeve.FAN_LEVEL_0` = off, `FAN_LEVEL_1` = low, `FAN_LEVEL_2` = medium, `FAN_LEVEL_3` = high. Fires `SET_PLASMA_FAN_SWITCH_ACK`. |
| `getFanSwitchState(IDataCallback)` | Reads the current fan on/off state and speed level. |
| `release()` | Unregisters all callbacks. |

**Events fired:**
- `REPORT_PLASMA_ENVIRONMENT_INFO_ACK` — Environment data received.
- `SET_PLASMA_ENVIRONMENT_SWITCH_ACK` — Environment sensor switch acknowledged.
- `SET_PLASMA_SWITCH_ACK` — Plasma ionizer switch acknowledged.
- `SET_PLASMA_FAN_SWITCH_ACK` — Fan control acknowledged.
- `REPORT_PLASMA_FAULT` — Hardware fault on devices 41, 42, or 43.
- `PLASMA_FAULT` (topic) — Fault info published to SDK topic.
- `PLASMA_ENVIRONMENT` (topic) — Environment data published to SDK topic.

---

### 18. Jacking Mechanism

**What it is:** A motorized vertical lifting platform (jack) on the robot body. Used to physically raise or lower a tray or payload to a specific height — for example, to pick up or deliver items at different shelf or table heights.

**Sensor class:** `SensorJacking` (`com.keenon.sdk.sensor.jacking.SensorJacking`)

**Device ID:** `57`

**SDK Functions:**

| Function | Description |
|---|---|
| `SensorJacking.getInstance()` | Returns the singleton. |
| `getJackingState()` | Reads the current position/state of the jacking mechanism (raised, lowered, mid-travel, fault). Result fires `GET_JACKING_STATE_ACK`. |
| `onControlUpRise()` | Commands the jacking platform to raise to its upper limit. Fires `CONTROL_JACKING_ACK` on completion. |
| `onControlDrop()` | Commands the jacking platform to lower to its lowest position. Fires `CONTROL_JACKING_ACK` on completion. |
| `release()` | Unregisters all callbacks. |

**Events fired:**
- `GET_JACKING_STATE_ACK` — Response to getJackingState query.
- `CONTROL_JACKING_ACK` — Command acknowledged.
- `REPORT_JACKING_STATE` — Pushed automatically when jacking state changes.

**Also related:** `NavigationJackingStateApi` — Reports jacking state during navigation tasks.

---

### 19. Battery

**What it is:** The robot's main lithium-ion battery pack. Provides power to all subsystems.

**Sensor class:** `SensorBattery` (`com.keenon.sdk.sensor.battery.SensorBattery`)

**Device ID:** `56`

**SDK Functions:**

| Function | Description |
|---|---|
| `SensorBattery.ins()` | Returns the singleton. |
| `enterTransportModel(ApiCallback<Integer>)` | Puts the battery into transport/shipping mode, which safely reduces battery activity for long-term storage or transport. The callback receives an integer error code (0 = success). |
| `release()` | Unregisters battery callbacks. |

**Related API classes:**
- `BatteryStatusApi` — Reads the current battery level, voltage, charging state, and health status.
- `ChargeAutoApi` — Enables automatic return-to-charger behavior.
- `ChargeManualApi` — Triggers a manual charge command.
- `ChargeDefaultApi` — Restores the default charging behavior.
- `ChargeStopApi` — Stops active charging.
- `ChargeNumbersApi` — Reads the total number of charge cycles completed.
- `ChargeMatchTimesApi` — Reads how many times the robot successfully docked to the charger.

**Events fired:**
- Battery state changes are published to relevant SDK topics — the battery sensor subscribes to `TopicName.BOTTOM_RAW` for low-level BMS (Battery Management System) events.

---

### 20. Emotion / Face Display

**What it is:** An embedded display (LCD/OLED screen on the robot's face) that shows animated facial expressions, emotions, or custom images to communicate the robot's state to users.

**Sensor class:** `SensorEmotion` (`com.keenon.sdk.sensor.emotion.SensorEmotion`)

**SDK Functions:**

| Function | Description |
|---|---|
| `SensorEmotion.getInstance()` | Returns the singleton. |
| `runEmotion(IDataCallback, int version, String emotion, Boolean force, Integer repeat)` | Plays a facial animation. `version` selects hardware board version (1 or 2). `emotion` is a base64-encoded animation data string. `force` overrides any currently playing animation. `repeat` controls loop count. |
| `runLocalEmotion(IDataCallback, int emotionId)` | Plays a pre-stored local emotion animation by its integer ID — no data transfer needed. |
| `setIdleEmotion(IDataCallback, String emotion)` | Sets the default "idle" face that plays when the robot has no other animation running. |
| `setStartupEmotion(IDataCallback, String emotion)` | Sets the face animation that plays automatically on robot startup/boot. |
| `release()` | Stops any in-progress emotion and cleans up the handler thread. |

**Version 2 transmission protocol:** Emotion data is split into 1920-byte packets (max 10 frames each, 192 bytes/frame). The SDK manages the multi-step handshake:
1. Send start packet (with config header)
2. Send data packets (repeating until all frames sent)
3. Send end packet
4. Receive completion via `EMOTION_STATUS` topic subscription

**Error codes:**
| Code | Meaning |
|---|---|
| `ERROR_CRC` | Data checksum error |
| `ERROR_ALREADY_RUN` | Another animation is already playing |
| `ERROR_DATA_DROPPED` | Packet loss during transfer |
| `ERROR_PICTURE_LIMIT` | Too many frames in animation |
| `ERROR_TIMEOUT` | Transfer timed out |

**Related API classes:**
- `EmotionRunApi` — V1 emotion run command.
- `EmotionRunLocalApi` — Run a local pre-stored emotion.
- `EmotionSendStartApi` — Start emotion data transfer (V2 protocol).
- `EmotionSendDataApi` — Send an emotion data packet (V2 protocol).
- `EmotionSendEndApi` — End emotion data transfer (V2 protocol).
- `EmotionSetIdleApi` — Set idle emotion.
- `EmotionSetStartUpApi` — Set startup emotion.
- `EmotionStatusApi` / `EmotionProgressApi` — Subscribe to emotion playback status.

---

### 21. Call Bell (Wireless Bell Module)

**What it is:** A wireless call bell system that allows patients, customers, or users in a space (e.g., hospital rooms, restaurant tables) to call the robot by pressing a paired button device. The robot receives the call task and can respond.

**Sensor class:** `SensorCallBell` (`com.keenon.sdk.sensor.callbell.SensorCallBell`)

**Device ID:** `65`

**SDK Functions:**

| Function | Description |
|---|---|
| `SensorCallBell.getInstance()` | Returns the singleton. |
| `getDeviceList()` | Requests the list of all paired bell devices currently reachable. Fires `RECEIVED_CALL_BELL_DEVICE_LIST` event. The result includes gateway SNs, relay SNs, and schedule device SNs. |
| `initBell(BellConfig config)` | Initializes/registers the robot's bell configuration — sets `stayTime` (how long the robot stays at destination), `keyEvent` type, `robotState`, and whether the bell is enabled. Uses auto-retry until acknowledged. |
| `updateBellState(BellConfig config)` | Updates the robot's current state (e.g., idle, busy) so bell devices show correct availability. Auto-retries until acknowledged. |
| `replyTaskState(int taskId, BellInterface.TaskState)` | Sends a task completion reply back to the bell network — notifies the bell that the robot has accepted, completed, or rejected the task. Auto-retries. |
| `onStartHeartBeat()` | Starts sending a heartbeat signal every 5 seconds to keep the bell network connection alive. |
| `onStopHeartBeat()` | Stops the heartbeat. |
| `release()` | Full cleanup of handler threads, scheduled executor, callbacks. |

**Events fired:**
- `RECEIVED_CALL_BELL_TASK` — A bell device has called the robot (contains task ID).
- `RECEIVED_CALL_BELL_DEVICE_LIST` — Device list received.
- `REPLAY_CALL_BELL_INIT_ACK` — initBell acknowledged.
- `REPLAY_CALL_BELL_UPDATE_STATE_ACK` — updateBellState acknowledged.
- `REPLAY_CALL_BELL_TASK_STATE_ACK` — replyTaskState acknowledged.

---

### 22. STM32 Microcontroller

**What it is:** An embedded STM32 ARM microcontroller on the robot's lower chassis board that manages low-level hardware I/O — motor drivers, sensor power, relay outputs. The Android SBC communicates with it over a serial/USB bridge.

**Sensor class:** `SensorStm32` (`com.keenon.sdk.sensor.stm32.SensorStm32`)

**Related class:** `AntiMotorSwitch` — Controls whether the STM32 anti-motor safety switch is active (prevents motors from running during certain conditions).

---

### 23. Gravity / Tilt Sensor (Plate)

**What it is:** A gravity/tilt sensor integrated into the robot's delivery plate or tray. Detects whether the tray is level, tilted, or has been disturbed — used to confirm items are placed securely.

**Sensor class:** `SensorGravityPlate` (`com.keenon.sdk.sensor.gravity.SensorGravityPlate`)

---

### 24. Button Panel

**What it is:** Physical buttons on the robot body (e.g., emergency stop, task confirm, volume) that users can press to directly interact with the robot or trigger actions.

**API classes:**
- `ButtonStatusApi` — Reads which buttons are currently pressed/held.
- `ButtonEnableApi` — Enables or disables specific buttons (used to lock out controls in autonomous mode).

**SDK Functions (via `ButtonStatusApi`):**

| Function | Description |
|---|---|
| `send(IDataCallback)` | One-time read of current button states. |
| `observe(IDataCallback)` | Subscribe to real-time button press/release events. |
| `cancel()` | Stop subscription. |

---

### 25. OTA Update Engine

**What it is:** The over-the-air firmware update system for all robot subsystems (STM32 firmware, ROS navigation stack, door controller firmware, etc.).

**Sensor class:** `SensorOTA` (`com.keenon.sdk.sensor.ota.SensorOTA`)

**Supported target devices (`OTAConfig.Device`):**
- `ROBOT_ARM` — Robot arm sub-controller firmware.
- `DOOR` — Door controller firmware.
- `ROS` — Robot Operating System navigation stack.
- `STM32` — Lower chassis microcontroller firmware.
- `GENERAL` — Generic device (uses ROS OTA channel).

**SDK Functions:**

| Function | Description |
|---|---|
| `SensorOTA.getInstance()` | Returns the singleton. |
| `start(OTAConfig config, IOta.Listener listener)` | Starts an OTA update session. `config` specifies the target device, firmware file path, and OTA protocol version (V1 or V2). `listener` receives progress callbacks. |
| `stop()` | Cancels an in-progress OTA update. |
| `release()` | Fully releases the OTA engine. |

**OTA Protocol versions:**
- `V1` — Uses `RosOTAImpl` (original binary transfer over serial).
- `V2` — Uses `RosOTAImplV2` (improved with chunked transfer and CRC verification).

**Also related:**
- `UpdateStateApi` — Reports the overall system firmware update state.
- `UpdateDownloadApi` — Manages firmware package download from server.
- `UpdateStartApi` / `UpdateCancelApi` / `UpdateRebootApi` / `UpdateHandleApi` — Control the update lifecycle.
- `SCMUpgradeTransApi` / `SCMUpgradeResetApi` / `SCMUpgradePackInfoApi` / `SCMUpgradeLastPackApi` — Low-level SCM board firmware upgrade primitives.

---

## API-Layer Controls (No Physical Hardware, Logic Only)

These APIs do not directly correspond to a physical component but orchestrate hardware systems together.

---

### Navigation & Movement APIs

| API | Description |
|---|---|
| `NavigationActionApi` | Start, stop, pause, resume a navigation task. |
| `NavigationSetTargetApi` | Set a single destination pose for the robot to navigate to. |
| `NavigationSetMultiTargetApi` | Set multiple waypoints for sequential navigation. |
| `NavigationSpeedApi` | Adjust the robot's navigation speed (within safe limits). |
| `NavigationStableModeApi` | Enable stable (slow, smooth) navigation mode for fragile payloads. |
| `NavigationSportModeApi` | Enable sport (faster) navigation mode. |
| `NavigationRemainingPathApi` | Query the remaining distance to the current destination. |
| `NavigationDestInfoApi` | Get information about the current navigation destination. |
| `NavigationDestPoseApi` / `NavigationDestPoseApiV2` | Get the precise destination pose being navigated to. |
| `NavigationTaskInfoApi` | Get detailed information about the current active navigation task. |
| `NavigationCruiseApi` | Control cruise/patrol navigation mode. |
| `NavigationCustomTaskApi` | Submit a custom structured navigation task. |
| `NavigationVendorActionApi` | Control vendor-specific navigation extensions. |
| `NavigationShortPath` | Requests a short-path recalculation mid-navigation. |
| `NavigationAllApi` | Composite API covering multiple navigation operations. |
| `NavigationPathApi` | Get the planned navigation path as a sequence of poses. |
| `NavigationSetBlockTimeoutApi` | Configure how long the robot waits when its path is blocked. |
| `PositionActionApi` | Trigger positioning/localization actions. |
| `PositionStatusApi` | Read current localization status. |
| `PositionPosInfoApi` | Read current estimated position. |
| `PositionNearestPoseInfoApi` | Read the nearest known map pose to current position. |
| `PositionLocationStatusApi` | Read whether the robot is currently localized. |
| `PositionNotifyApi` | Subscribe to position update notifications. |
| `PositionRequestApi` | Request a new position estimate. |
| `PositionOriginJudgeApi` | Check whether the robot is at its origin/home pose. |

---

### Charging APIs

| API | Description |
|---|---|
| `ChargeAutoApi` | Enable automatic return-to-charger behavior. |
| `ChargeManualApi` | Trigger a manual dock-and-charge command. |
| `ChargeDefaultApi` | Restore default charging behavior. |
| `ChargeStopApi` | Stop an active charging session. |
| `ChargeNumbersApi` | Read total charge cycles count. |
| `ChargeMatchTimesApi` | Read successful docking count. |

---

### Map APIs

| API | Description |
|---|---|
| `MapDownloadApi` / `MapDownloadNewApi` / `MapDownloadV2Api` / `MapDownloadOptApi` | Download a navigation map from the robot's map server. |
| `MapDownLoadStatusV2Api` / `MapDownLoadActionV2Api` | Monitor and control map download progress. |
| `MapUploadApi` / `MapUploadOptApi` / `MapUploadObsApi` | Upload an updated map to the robot. |
| `MapInfoApi` | Read metadata about currently loaded maps. |
| `MapCmdRequestApi` / `MapCmdReplyApi` | Low-level map command request/reply protocol. |
| `MapOptStatusApi` | Get the status of a map optimization operation. |
| `SensorMap` / `MapManager` (sensor layer) | Higher-level map management: load, save, and switch maps. |

---

### Scheduling APIs

| API | Description |
|---|---|
| `ScheduleStatusApi` | Read the current scheduled task status. |
| `ScheduleActionApi` | Start or stop a scheduled task. |
| `ScheduleStatusActionApi` | Combined status + action for schedules. |
| `ScheduleLiveApi` | Monitor a schedule in real time. |
| `ScheduleModuleApi` | Read/write schedule module configuration. |
| `SetScheduleOriginApi` | Set the return-to-origin behavior for scheduled tasks. |

---

### Disinfection Control APIs

| API | Description |
|---|---|
| `DisinfectStatusApi` | Read the current disinfection session status. |
| `DisinfectUltravioletApi` | Control UV lamp disinfection specifically. |
| `DisinfectAtomizerApi` | Control atomizer (spray) disinfection. |
| `DisinfectFanApi` | Control the disinfection fan. |
| `DisinfectCabinApi` | Control the disinfection cabin (enclosed space UV). |
| `DisinfectDrainageApi` | Control liquid drainage for atomizer refill. |
| `DisinfectLiquidApi` | Monitor disinfectant liquid level. |
| `NavigationDisinfectActionApi` | Start/stop a robot disinfection navigation route. |
| `NavigationDisinfectStatusApi` / `NavigationDisinfectionInfoApi` | Monitor disinfection navigation status. |

---

## Common SDK Patterns

### Singleton Access
All sensors use thread-safe double-checked locking:
```java
SensorDoor door = SensorDoor.getInstance();
SensorLight light = SensorLight.getInstance();
SensorUVLamp uvLamp = SensorUVLamp.getInstance();
```

### Callback Interface
All async operations use `IDataCallback`:
```java
public interface IDataCallback {
    void success(String jsonResult);
    void error(ApiError error);
}
```

### One-Shot vs. Subscribe
- **`send(callback)`** — Request once, get one response.
- **`observe(callback)`** — Subscribe for continuous updates.
- **`cancel()`** — Unsubscribe.

### Lifecycle Management
Always call `release()` when done to avoid memory/connection leaks:
```java
SensorDoor.getInstance().release();
SensorLight.getInstance().release();
```

### Communication Modes
All sensor requests can be routed through different physical channels:

| Mode | Method | Description |
|---|---|---|
| USB Direct | `setUSBDirect(true)` | Direct USB cable from Android to hardware |
| Serial Direct | `setSerialDirect(true)` | Direct UART serial connection |
| Trans (Bridge) | `setTrans(true)` | Routed through the robot's internal bridge/ROS |
| Remote | Default | Routed through network (CoAP over Wi-Fi) |

---

## Device ID Quick Reference

| Device ID | Hardware Component |
|---|---|
| `0` | Drive Motor (chassis) |
| `6` | LED Light Strip (V3 heartbeat target) |
| `15` | LiDAR Plate Sensor |
| `22–25` | Compartment Doors (1–4) |
| `41` | Plasma Environment Sensor |
| `42` | Plasma Ionizer Switch |
| `43` | Plasma Fan |
| `44–48` | UV Lamps (1–5) |
| `56` | Battery (BMS) |
| `57` | Jacking Mechanism |
| `65` | Call Bell Module |
| `66` | Head Motor |

---

*This document was generated from the Peanut SDK v1.3.0 source code. For the latest updates, refer to the SDK release notes and the source files under `peanut-sdk-v1.3.0/libs/peanut-sdk-extracted/sources/com/keenon/sdk/`.*
