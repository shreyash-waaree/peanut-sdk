# Peanut SDK — Overview, Purpose & Usage

> **Target artifact:** [libs/peanut-sdk-release.aar](../libs/peanut-sdk-release.aar)
> **Decompiled sources:** [libs/peanut-sdk-extracted/](../libs/peanut-sdk-extracted/)
> **Sample integrator:** [SampleApp/](../SampleApp/)
> **Last updated:** 2026-04-19

---

## 1. What is `peanut-sdk-release.aar`?

`peanut-sdk-release.aar` is the **Keenon Peanut SDK** — an Android Archive (`.aar`) library published by **Keenon Robotics** (package root `com.keenon.*`, manifest package `com.keenon.peanut`, version `1.5.0-bate1` inside the embedded `AndroidManifest.xml`, shipped as SDK release **v1.3.0**).

It is the **Android-side control library for Keenon "Peanut" service robots** — the food‑delivery, hotel‑service and disinfection robots (models such as T10, W3, T8). The robot itself runs a Linux/ROS "lower" computer (the chassis controller, sometimes called the "UNIK 3‑in‑1 board") and an Android "upper" computer (the tablet the user interacts with). This AAR is what an Android application running on the upper computer uses to **talk to the chassis**: move the robot, open its doors, read its battery, trigger its UV lamp, play a facial emotion, upload a map, receive sensor streams, run OTA upgrades, and so on.

The `.aar.jadx` file you saw in `libs/` is **not** code — it is only a [jadx](https://github.com/skylot/jadx) decompiler project file (editor tab state, cache paths). The actual Java sources are in `libs/peanut-sdk-extracted/sources/` which is what jadx produced when the `.aar` was decompiled.

Inside the extracted archive:

| Location | Contents |
|---|---|
| `sources/com/keenon/sdk/external/PeanutSDK.java` | Public entry point (singleton) |
| `sources/com/keenon/sdk/component/` | 14 high‑level feature "components" (battery, door, motor, nav, etc.) |
| `sources/com/keenon/sdk/api/` | 170+ low‑level request/response classes (one per wire command) |
| `sources/com/keenon/sdk/api2/` | Versioned (v2) API additions (`UpgradeActionApi`, `UpgradeStatusApi`) |
| `sources/com/keenon/sdk/apiProxy/` | Proxied APIs that go through cloud/other gateways (UV lamp, plasma, door switch) |
| `sources/com/keenon/sdk/coap/` | CoAP client used to talk to the chassis |
| `sources/com/keenon/sdk/serial/` | Serial‑port transport (for older/USB‑connected chassis boards) |
| `sources/com/keenon/sdk/http/` + `/tftp/` | HTTP + TFTP transports (file upload/download, maps, OTA) |
| `sources/com/keenon/sdk/sensor/` | Sensor drivers: lidar, IMU, depth, RFID, UV lamp, plasma, emotion, jacking, head motor, battery, STM32, vision |
| `sources/com/keenon/sdk/proxy/` | Link adapter that routes each API over the right transport |
| `sources/com/keenon/sdk/scmIot/` | Lower‑level IoT/SCM protocol (STM32 microcontroller messaging) |
| `sources/com/keenon/sdk/auth/` | License/offline auth (`OfflineAuth`, `LicenseInfo`) |
| `sources/com/keenon/sdk/hedera/` | Protocol adapter used by the API request pipeline |
| `sources/com/keenon/common/` | Shared constants, error codes, logging, utils |
| `sources/org/apache/`, `sources/org/eclipse/` | Vendored third‑party: Apache Commons Net (TFTP), Eclipse Californium (CoAP) |
| `resources/jni/` | Native `.so` libraries (native logging/CoAP helpers) |
| `resources/assets/`, `resources/R.txt`, `proguard.txt` | Assets, resource table, consumer ProGuard rules |

---

## 2. Why is this SDK used in this project?

This project (`Peanut_service apk file`) is a **Waaree Energies** Android application that runs on the Peanut robot's tablet to provide a custom customer‑facing UX (voice form, hardware panel, supermarket/promo flows — see `SampleApp/app/src/main/java/com/keenon/peanut/supermarket/` and `SampleApp/app/src/main/java/com/keenon/peanut/sample/`). Without the SDK the app could only draw a UI; it could not actually drive the robot.

The SDK is used because it is the **only supported way** for an Android application to:

1. **Authenticate** against the robot using the Keenon App ID / Secret licence pair (`OfflineAuth.start(context, appId, secret)`). Until auth returns `0` no component commands are usable.
2. **Drive the chassis** — send navigation targets, pause/resume/stop, manual motor control (forward/back/left/right), set max speed, set stable/sport mode.
3. **Read telemetry** — battery, IMU, lidar scans, depth frames, infrared, sonar, PSD, collision/touch bumps, odometry, heartbeat — via a publish/subscribe topic model (see `TopicName.java`).
4. **Actuate hardware** — open/close compartment doors, lock doors, switch door type, run UV lamp, cabin/drainage/atomizer/fan disinfection, LED light strip, emotion face display, call‑bell configuration.
5. **Manage maps** — upload / download / optimize maps, request map info, track map download progress (v1 and v2 flows).
6. **Do OTA / firmware upgrades** — `UpgradeActionApi`, `UpgradeStatusApi`, `UpdateComponent`, `SCMUpgrade*Api` (STM32 firmware), plus TFTP transport.
7. **Integrate with elevators & access‑control gates** (`ElevatorComponent`, `GateToRobotApi`) so the robot can ride lifts and pass through electronic doors in hotels.
8. **Handle vendor and schedule features** — matching a robot to a cloud "vendor" (operator), running scheduled tasks, live schedule events.

In short: this project embeds the AAR because **the robot is a Keenon Peanut**, and Keenon's own library is what speaks the robot's proprietary CoAP + serial + SCM protocols.

---

## 3. How is it used?

### 3.1 Lifecycle (from `PeanutSDK.java`)

```java
// 1. Get the singleton
PeanutSDK sdk = PeanutSDK.getInstance();

// 2. Initialize with an Android Context and a listener for status codes
sdk.init(getApplicationContext(), new PeanutSDK.ErrorListener() {
    @Override public void onInit(int statusCode) {
        // statusCode == PeanutSDK.SDK_INIT_SUCCESS (0) on success
        // Other codes (>= 203000) are errors defined in PeanutError
    }
});

// 3. Use components (see 3.3)
sdk.navigation().setTarget(cb, 5);

// 4. Release when done
sdk.release();
```

`init()` performs, in order (see `PeanutSDK.init` → `initAuth`):

1. `LogUtils.init` with config from `PeanutConfig` (log level / write‑to‑file).
2. Android permission pre‑check on `PeanutConstants.SDK_PERMISSION_LIST` (throws `RuntimeException` if denied).
3. File‑based `LogManager` init (logback) at `PeanutConstants.LOG_PATH`.
4. `OfflineAuth.start(context, appId, secret)` — fails init if licensing fails.
5. Starts `PeanutStatistics` (Umeng), a `PingTask` that watches the CoAP link, and `PeanutSensors` (global sensor bus).
6. Registers the 14 `Component` singletons in `ComponentManager`.

### 3.2 Component surface

`PeanutSDK` exposes 14 convenience accessors, each returning one `Component` that groups related APIs:

| Accessor | Class | What it does |
|---|---|---|
| `sdk.robot()` | `BaseComponent` | Low‑level link config (link type, COM port, IP, emotion/door COM). |
| `sdk.battery()` | `BatteryComponent` | Battery status, auto/manual/default charge, stop charging, charge‑pile matching. |
| `sdk.device()` | `DeviceComponent` | Device tree, board info, version, reboot, E‑stop button, params get/set (v1/v2), self‑check, call‑bell config, STM32 ping. |
| `sdk.disinfect()` | `DisinfectComponent` | UV lamp, cabin, drainage, atomizer, fan‑speed level; disinfection task area & action. |
| `sdk.door()` | `DoorComponent` | Open / close / state per door, all‑door state, door‑type (2/3/4‑door + auto), lock status, fault observers. |
| `sdk.elevator()` | `ElevatorComponent` | Elevator status, relay messages server→robot and vice‑versa, gate control. |
| `sdk.map()` | `MapComponent` | Upload / download / optimize maps (v1 + v2), map info, obstacle upload, progress callbacks. |
| `sdk.motor()` | `MotorComponent` | Enable/disable, HRC, encoder, speed, min/max speed, manual forward/backward/left/right, `moveControl`. |
| `sdk.navigation()` | `NavigationComponent` | `setTarget`, multi‑target, cruise, pause (80) / resume (67) / stop (83) / reset (82), paths, dest pose, short path, custom task, guide list, schedule origin. |
| `sdk.runtime()` | `RuntimeComponent` | Work mode, IP, odometry, position, heartbeat, time sync, VSLAM picture, system‑startup state, WiFi, welcome switch, resource config. |
| `sdk.schedule()` | `ScheduleComponent` | Schedule module / status / live stream. |
| `sdk.update()` | `UpdateComponent` | OTA package, download, start, state, cancel, reboot, handle. |
| `sdk.vendor()` | `VendorComponent` | Vendor match, vendor move‑in/out, vendor target. |
| `sdk.deviceNode()` | `DeviceNodeComponent` | Device‑node query API. |

Each component method is a **one‑liner that constructs an `Api` object and calls `.send(callback, args...)`**. The work lives in the individual `Api` classes.

### 3.3 Request/response pattern

Every public API (e.g. `NavigationSetTargetApi`) is annotated so the same Java class can be sent over multiple transports:

```java
@LinkAdapter
@CoapCommond(path = "/navigation/dst", requestType = RequestEnum.POST)
@SerialCommand(action = "62", body = "25")
public class NavigationSetTargetApi {
    @CoapParams public String CoapParams()  { /* builds JSON */ }
    @SerialParams public Byte[] SerialParams() { /* builds bytes */ }
    @CoapResponse   ApiCallback coapCallback   = ...
    @SerialResponse ApiCallback serialCallback = ...
    public void send(IDataCallback cb, int target) {
        this.callBack = cb;  this.target = target;
        SenderManager.getInstance().send(this);
    }
}
```

`SenderManager` consults `LinkAdapterFactory` (configured from `BaseComponent` parsing `PropertiesValue`) to pick **CoAP over UDP** (default, to the chassis), **serial** (USB/RS‑232 on older boards), **HTTP**, or **TFTP** (file transfer). The response is funnelled back through `ApiCallback` into the user‑supplied `IDataCallback` / `IDataCallback2<T>` / `IProgressCallback`.

Callbacks are always:

```java
public interface IDataCallback {
    void success(String json);         // JSON payload from robot
    void error(ApiError error);        // code + message
}
```

Long‑running transfers (maps, OTA) use `IProgressCallback extends IDataCallback` with `readyToSend(Request)` + `progress(int)`.

### 3.4 Subscribing to streams (publish/subscribe)

Streaming telemetry is not request/response — instead the app subscribes to a *topic* identified by the API class name (see `TopicName.java`):

```java
PeanutSDK.getInstance().subscribe(TopicName.BATTERY_STATUS, new IDataCallback() {
    @Override public void success(String json) { /* battery update */ }
    @Override public void error(ApiError e)    { /* ... */ }
});
// ...
PeanutSDK.getInstance().unSubscribe(TopicName.BATTERY_STATUS, cb);
```

Internally `subscribeInternal` uses reflection to find the matching API class in `PeanutConstants.API_PACKAGE`, `API_PACKAGE_V2` or `API_PACKAGE_PROXY` (see `mathTopicPath`) and invoke its `observe` / `cancel` methods. Available topics include: `BATTERY_STATUS`, `DOOR_STATUS`, `MOTOR_STATUS`, `NAVIGATION_STATUS`, `NAVIGATION_PATH`, `RUNTIME_HEARTBEAT`, `RUNTIME_HEALTH`, `SENSOR_LIDAR`, `SENSOR_IMU`, `SENSOR_DEPTH`, `SENSOR_COLLISION`, `SENSOR_TOUCH`, `SENSOR_INFRARED`, `SENSOR_PSD`, `SENSOR_SONAR`, `EMOTION_STATUS`, `PLASMA_FAULT`, `UV_LAMP_FAULT`, `UPGRADE_STATUS`, `SCHEDULE_LIVE`, `ROBOT_EVENT`, `POSITION_*`, and more.

### 3.5 Events, constants and error codes

- **`PeanutEvent`** — runtime event constants (`EVENT_CONNECT_SUCCESS = 10006`, `EVENT_CONNECT_TIMEOUT = 10007`, `EVENT_WORK_MODE_CHANGED`, `EVENT_MOTOR_STATUS_CHANGED`, `EVENT_LOCATION_SUCCESS`, …).
- **`ApiConstants`** — success/error codes (`CODE_SUCCESS=0`, `CODE_IN_CHARGING=19`, `CODE_DST_NOT_EXIST=35`, `CODE_NO_PATH=81`, `CODE_INVALID_POS=256`), motor direction enum, work‑mode enum (`AUTO=1`, `BUILD_MAP=2`, `MFG_TEST=3`, `CHARGER=4`, `MANUAL=5`, `ADAPTER_CHARGER=6`, `SELF_TEST=7`, `ANTI_FALL=8`), robot‑check labels.
- **`PeanutError`** — namespaced error codes (`INIT_PERMISSION_DENIED`, licence failures, etc.) returned via `ErrorListener.onInit`.

### 3.6 How this project wires it up

`SampleApp/` is Keenon's reference integrator and the scaffolding this project is built on top of:

- `com.keenon.peanut.sample.DemoApplication` — calls `PeanutSDK.getInstance().init(...)` in `Application.onCreate`.
- `com.keenon.peanut.sample.receiver.SdkBootstrap` — boot‑time SDK bring‑up.
- `com.keenon.peanut.sample.chassis.*Demo` — one per component: `ChargerDemo`, `DoorDemo`, `MapDemo`, `MotorDemo`, `NavigationDemo`, `T3DoorDemo`, `T8LightDemo`, `RosMapDemo` — each shows the minimal UI + SDK calls for that subsystem.
- `com.keenon.peanut.sample.receiver.RobotMovementManager` + `WebSocketServerService` + `RobotCameraStreamer` — the "remote control" extension this project uses to drive the robot from a backend over WebSocket, forwarding into `motor()` / `navigation()` calls.
- `com.keenon.peanut.supermarket.*` — the Waaree supermarket / promo UX that *uses* the hardware + voice pipelines but is not part of the upstream SDK.

The linkage is declared in `SampleApp/app/build.gradle` as a flat `libs/` dependency on `peanut-sdk-release.aar`, which is what makes `com.keenon.sdk.external.PeanutSDK` importable from the app's Java sources.

---

## 4. Module‑by‑module reference (extracted AAR)

### 4.1 `com.keenon.sdk.external` — public API surface
`PeanutSDK` (singleton, init/release/subscribe/unsubscribe + 14 component accessors), `IDataCallback`, `IDataCallback2<T>`, `IProgressCallback`, `IDeviceNodeCallback`, `PeanutEvent`.

### 4.2 `com.keenon.sdk.component` — feature components
14 components listed in §3.2 plus subfolders: `charger/`, `disinfection/`, `gating/`, `navigation/`, `param/` (param‑key validation), `runtime/`, `transport/`.

### 4.3 `com.keenon.sdk.api` + `api2` + `apiProxy` — wire commands
170+ classes. Grouped by domain:

- **Navigation** — `NavigationSetTargetApi`, `NavigationActionApi` (pause/resume/stop/reset), `NavigationPathApi`, `NavigationCruiseApi`, `NavigationShortPath`, `NavigationCustomTaskApi`, `NavigationSpeedApi`, `NavigationSportModeApi`, `NavigationStableModeApi`, `NavigationRobotOnSlopeApi`, `GuideInfoApi`, `GuideSetTargetApi`, …
- **Battery & charger** — `BatteryStatusApi`, `ChargeAutoApi`, `ChargeManualApi`, `ChargeDefaultApi`, `ChargeStopApi`, `ChargeMatchTimesApi`, `ChargeNumbersApi`.
- **Door** — `DoorOpenApi`, `DoorCloseApi`, `DoorStatusApi`, `DoorLockStatusApi`, `DoorLockAllStatusApi`, `DoorTypeFourSetApi`, `DoorTypeDoubleSetApi`, `DoorTypeThreeSetApi`, `DoorTypeThreeReverseSetApi`, `DoorTypeAutoSetApi`, `DoorFaultApi`, `DoorLimitFaultApi`, `DoorTypeObserveApi`.
- **Motor** — `MotorEnableApi`, `MotorEncoderApi`, `MotorHealthApi`, `MotorHrcApi`, `MotorManualApi`, `MotorGMaxApi`, `MotorSMaxApi`, `MotorSpeedApi`, `MotorStatusApi`, `DevicesMoveControlApi`.
- **Disinfection** — `DisinfectCabinApi`, `DisinfectDrainageApi`, `DisinfectAtomizerApi`, `DisinfectUltravioletApi`, `DisinfectFanApi`, `DisinfectStatusApi`, `DisinfectLiquidApi`, `NavigationDisinfect*Api`.
- **Device / self‑check** — `DeviceListApi`, `DeviceInfoApi`, `DeviceVersionApi`, `DeviceRebootApi`, `DevicesCheckApi`, `ButtonEnableApi`, `ButtonStatusApi`, `RealTimeStatusApi`, `SelfCheckApi`, `RobotDeviceCheck*Api`, `RobotDevicePostCheck*Api`, `RuntimeHealthApi`, `RuntimeHeartbeatApi`.
- **Map** — `MapCmdRequestApi`, `MapCmdReplyApi`, `MapDownloadApi`, `MapDownloadNewApi`, `MapDownloadOptApi`, `MapDownloadV2Api`, `MapDownLoadActionV2Api`, `MapDownLoadStatusV2Api`, `MapUploadApi`, `MapUploadObsApi`, `MapUploadOptApi`, `MapInfoApi`.
- **Sensors** — `SensorLidar*Api`, `SensorImu*Api`, `SensorDepth*Api`, `SensorInfrared*Api`, `SensorPsdApi`, `SensorSonarApi`, `SensorTouch*Api`, `SensorCollisionApi`, `SensorBumpStatusApi`, `SensorGazerApi`, `SensorAntiFallLidarApi`, `SensorStatusApi`.
- **Emotion / light / welcome** — `Emotion*Api` (run / progress / idle / start‑up / send data), `LightDisplayApi`, `WelcomeSwitchApi`, plus `api/light/` and `api/door/` sub‑packages.
- **OTA / upgrade** — `Update*Api` (cancel/download/handle/package/reboot/start/state), `SCMUpgrade*Api` (STM32), `api2.UpgradeActionApi` + `UpgradeStatusApi`.
- **Elevator / gate** — `ElevatorStatusApi`, `ElevatorToRobotApi`, `ElevatorToServerApi`, `GateToRobotApi`, `GateToServerApi`.
- **Position / VSLAM** — `PositionActionApi`, `PositionPosInfoApi`, `PositionLocationStatusApi`, `PositionNearestPoseInfoApi`, `PositionOriginJudgeApi`, `PositionRequestApi`, `PositionNotifyApi`, `PositionStatusApi`, `VslamPictureApi`.
- **Vendor** — `VendorMatchApi`, `VendorMoveInApi`, `VendorMoveOutApi`, `VendorSetTargetApi`.
- **Schedule** — `ScheduleActionApi`, `ScheduleLiveApi`, `ScheduleModuleApi`, `ScheduleStatusApi`, `ScheduleStatusActionApi`, `SetScheduleOriginApi`, `TaskStatusApi`.
- **Proxied (cloud/STM)** — `apiProxy.UVLampFaultApi`, `PlasmaFaultApi`, `PlasmaEnvironmentApi`, `DoorSwitchStatusApi`, `DoorSwitchFaultApi`.

### 4.4 Transports
- `coap/` — `PeanutCoapClient`, `BaseCoapHandler`, `CoAPPing`, `RequestBody`, and the `@CoapCommond` / `@CoapParams` / `@CoapResponse` adapter annotations.
- `serial/` — `HederaClient(Factory)`, `SerialPing`, plus `@SerialCommand` / `@SerialParams` / `@SerialResponse` annotations.
- `http/` — `PeanutHttpClient`, `OkHttpFactory`, `BaseHttpHandler`, `ws/` (WebSocket).
- `tftp/` — `TFTPServerManager`, `TFTPService` (file transfer for maps/firmware, backed by Apache Commons Net in `org.apache`).
- `proxy/` — `LinkAdapterFactory`, `SenderManager`, `PingTask`, reading active link from `PropertiesValue` / `PeanutConfig`.
- `scmIot/` — `SCMIoTSender`, `bean/`, `protopack/`, `transfer/` — lower‑level STM32 IoT packets (used for call‑bell, LED, fan, etc.).
- `hedera/` — the internal request/response model (`ApiData`, `ApiError`, `ApiCallback`, `ICallback`, `RequestEnum`) all APIs use.

### 4.5 Sensors subsystem
`com.keenon.sdk.sensor.common.{PeanutSensors, Sensor, Event, SensorObserver}` is a global observer bus. On init it attaches every hardware sensor driver under `sensor/lidar|Motor|battery|door|emotion|jacking|plasma|uvlamp|light|headmotor|rfid|gravity|stm32|callbell|map|ota|vision|calibration/`. `PeanutSDK` registers itself as a global observer so any sensor event can be forwarded.

### 4.6 Support packages
- `auth/` — `OfflineAuth` (licence check using `AppId`/`Secret` from `PeanutConfig`), `AuthInfo`, `LicenseInfo`.
- `config/` — `PeanutProperties`, `PropertiesKey`, `PropertiesValue` (reads runtime link config — type, COM port, IP, port, door/emotion COMs).
- `constant/` — `TopicName`, `ApiConstants`, `PeanutTips`.
- `common/` — cross‑cutting: `log/` (LogManager + Umeng statistics), `error/` (PeanutError + ExceptionMessage), `utils/` (reflection, threading, byte/json utils), shared `external/PeanutConfig`.

### 4.7 Native + resources
- `resources/jni/<arch>/` — native shared libraries bundled inside the AAR.
- `resources/AndroidManifest.xml` — declares `android.permission.INTERNET`, minSdk 19, targetSdk 29.
- `resources/proguard.txt` — consumer ProGuard rules so the host app keeps SDK classes.
- `resources/R.txt` — resource symbol table.

### 4.8 Vendored third‑parties
- `org.apache.commons.net.*` — used by `tftp/`.
- `org.eclipse.californium.*` — Californium CoAP stack, the transport `PeanutCoapClient` builds on.

---

## 5. End‑to‑end example (what a minimal integration looks like)

```java
public class DemoApplication extends Application {
    @Override public void onCreate() {
        super.onCreate();
        PeanutSDK.getInstance().init(this, status -> {
            if (status == PeanutSDK.SDK_INIT_SUCCESS) {
                // subscribe to battery
                PeanutSDK.getInstance().subscribe(TopicName.BATTERY_STATUS, new IDataCallback() {
                    public void success(String json) { Log.i("bat", json); }
                    public void error(ApiError e)    { Log.e("bat", e.toString()); }
                });
                // drive to target #3
                PeanutSDK.getInstance().navigation().setTarget(new IDataCallback() {
                    public void success(String json) { /* arrived / accepted */ }
                    public void error(ApiError e)    { /* CODE_DST_NOT_EXIST, etc. */ }
                }, 3);
            }
        });
    }
}
```

This is the same three‑step pattern the sample `*Demo` classes use — init → call `PeanutSDK.getInstance().<component>().<method>(cb, args)` → handle success/error.

---

## 6. Related documents

- [HARDWARE_REFERENCE.md](HARDWARE_REFERENCE.md) — per‑hardware breakdown (which API drives which physical component).
- [HARDWARE_FLOWCHART.md](HARDWARE_FLOWCHART.md) — transport/architecture flowcharts.
- [Peanut SDKV1.3.0 (English).docx](Peanut%20SDKV1.3.0%20%28English%29.docx) — Keenon's original SDK manual.
- [Open Platform Document V2.2.0 (2).pdf](Open%20Platform%20Document%20V2.2.0%20%282%29.pdf) — Keenon cloud / open‑platform spec this SDK slots into.
- [SampleApp/REMOTE_CONTROL_EXTENSION.md](../SampleApp/REMOTE_CONTROL_EXTENSION.md) — WebSocket remote‑control extension built on top.
- [SampleApp/VOICE_FORM_EXTENSION.md](../SampleApp/VOICE_FORM_EXTENSION.md) — Voice + Form UI extension.
