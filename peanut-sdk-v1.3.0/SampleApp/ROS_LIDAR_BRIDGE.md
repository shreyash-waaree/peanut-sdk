# ROS LiDAR Bridge — adding a rosbridge-backed LiDAR source to the SampleApp

**Date**: 2026-04-24
**Affected app**: `com.keenon.peanut.sample` (SampleApp)
**Min build**: Android Gradle project under `SampleApp/`
**Robot model**: Keenon Peanut T10 (RK3288 Android head + RK3399 ROS algorithm board)

---

## 1. Problem

The SampleApp already contained a complete on-device LiDAR visualization
(`LidarRadarView` → `SensorsFragment` → `SensorDataManager`), but it was wired
to the Peanut SDK's CoAP endpoint `OBSERVE /sensor/lidar`. On this robot that
endpoint returned an empty payload:

```
ApiError{mid=14240, code=203172, msg='coap response empty payload', tag='/sensor/lidar'}
seq 0 · 0.0/s · last never
```

The cause: `/sensor/lidar` is populated by the main **Peanut service**
(`com.keenon.peanut.peanutservice`), not by the sample. Only the sample app
was running, so nobody was pushing LiDAR frames into the SDK CoAP bus.

Meanwhile, the robot's ROS stack **was** streaming LiDAR to
`ws://192.168.64.20:9090` (rosbridge) on topic `/scan` at ~10 Hz native —
verified out-of-band with a plain Python client (`fetch_lidar.py`).

**Goal**: surface that rosbridge stream inside the existing LiDAR card with a
user-controlled toggle, without replacing the CoAP path or touching the
renderer.

---

## 2. What data the app now receives

Exactly one LiDAR topic — the front-facing 2D scanner.

| Field | Value |
|---|---|
| rosbridge URI | `ws://192.168.64.20:9090` |
| ROS topic | `/scan` |
| ROS message type | `sensor_msgs/LaserScan` |
| TF frame (`header.frame_id`) | `laser_link` |
| Angular range | `angle_min = −π/2`, `angle_max = +π/2` (180° FOV, front-facing) |
| Angular resolution | `angle_increment ≈ 0.00582 rad` (0.333°/step) |
| Points per scan (`ranges[]` length) | 541 |
| Distance range | `range_min = 0.05 m`, `range_max = 30.0 m` |
| Delivered rate | 5 Hz (we set `throttle_rate: 200` on subscribe) |
| Native rate on the robot | ~10 Hz |
| Encoding | Plain JSON floats — **no base64, no compression** |
| Bytes per frame on the wire | ~13 KB of JSON (human-readable) |

The robot also publishes `/scan_orig`, `/scan_filter`, `/scan_matching_task`
(all `sensor_msgs/LaserScan`), plus many `sensor_msgs/PointCloud2` topics
(`/mx/cloud1`, `/fdl/rawcloud`, …). Only `/scan` is wired up right now — but
the new class takes the topic as a constructor argument, so swapping is a
one-line change.

Rejected candidate: `/scan_base_map`. It is advertised but its type is a
**custom** `web_backend/PointArray` whose `.msg` definition rosbridge can't
resolve → subscribing yields silence with no error. That is the "hex/base64
blob" we saw earlier; it's not ROS hiding the data, it's rosbridge skipping
unknown types. We stay on stock `sensor_msgs/LaserScan`.

---

## 3. End-to-end data flow

```
Robot RK3399 (192.168.64.20)                Android tablet (192.168.64.10 · eth0)
                                            com.keenon.peanut.sample
┌──────────────────────┐                    ┌──────────────────────────────────┐
│ LiDAR driver         │                    │ SensorsFragment                  │
│  /scan (10 Hz)       │                    │  (Developer → Sensors tab)       │
│  sensor_msgs/        │                    │  ┌────────────────────────────┐  │
│  LaserScan           │                    │  │ LidarRadarView.setScan(..) │  │
└─────────┬────────────┘                    │  └────────────┬───────────────┘  │
          │ ROS pub                         │               │ 5 Hz redraws      │
          v                                 │               v                   │
┌──────────────────────┐                    │  ┌────────────────────────────┐  │
│ rosbridge_server     │                    │  │ SensorDataManager          │  │
│  port 9090 (WS)      │ <─── WebSocket ────┤  │  applyLidarFields()        │  │
│  JSON framing        │    (eth0 → ROS)    │  │  - picks ROS scan when    │  │
└──────────────────────┘                    │  │    lidarRosMode == true   │  │
                                            │  └────────────┬───────────────┘  │
                                            │               ^ latestRosScan    │
                                            │               │                   │
                                            │  ┌────────────┴───────────────┐  │
                                            │  │ RosLidarBridge (new)       │  │
                                            │  │  Java-WebSocket client     │  │
                                            │  │  parses LaserScan          │  │
                                            │  │  builds LidarDecoder.Scan  │  │
                                            │  └────────────────────────────┘  │
                                            └──────────────────────────────────┘
```

Step-by-step:

1. User flips **ROS bridge** switch on in the LiDAR card.
2. `SensorsFragment` calls `SensorDataManager.setLidarRosMode(true)`.
3. `SensorDataManager` lazily constructs a `RosLidarBridge` and calls `start()`.
4. `RosLidarBridge` opens a WebSocket to `ws://192.168.64.20:9090`.
5. On `onOpen`, it sends the rosbridge v2.0 subscribe op:
   ```json
   {"op":"subscribe","topic":"/scan","type":"sensor_msgs/LaserScan",
    "throttle_rate":200,"queue_length":1}
   ```
6. For each incoming `{"op":"publish","topic":"/scan","msg":{...}}` frame,
   `handleRosbridgeMessage` parses JSON and calls `buildScan(msg)`.
7. `buildScan` walks `ranges[]`, converts each reading into Cartesian (see §4),
   and returns an immutable `LidarDecoder.Scan`.
8. The listener stores that scan into `SensorDataManager.latestRosScan` and
   bumps the `hbLidar` heartbeat (so the UI shows "5.0/s · last just now"
   instead of "last never").
9. Every 200 ms, `SensorDataManager`'s existing scheduled executor builds the
   unified JSON. In `applyLidarFields`, the ROS branch picks `latestRosScan`
   and labels the summary `(ros) rays … · min … · max … · mean …`.
10. `SensorsFragment.onSensorDataUpdated` parses the JSON, finds the scan
    payload, and calls `lidarRadar.setScan(scan)` → `invalidate()` → canvas
    redraws the radar at 5 Hz.

When the toggle is flipped off:

1. `setLidarRosMode(false)` is called.
2. `RosLidarBridge.stop()` sends `{"op":"unsubscribe","topic":"/scan"}` on the
   open socket, then blocks on `closeBlocking()`.
3. `latestRosScan` is nulled, `rosBridgeStatus` is cleared, `applyLidarFields`
   falls back to the CoAP path (or demo, if that's on).

---

## 4. Coordinate conversion — why no remap is needed

**ROS convention** (`laser_link`, per REP 103):

- Axes: `+x` = robot forward, `+y` = robot left, `+z` = up.
- `angle` in `sensor_msgs/LaserScan` measured CCW from `+x`.
- A point at range `r` and beam index `i` is located at:
  ```
  angle = angle_min + i * angle_increment
  X = r * cos(angle)    # forward
  Y = r * sin(angle)    # left
  ```

**App's `LidarDecoder.Scan` convention** (already established by the CoAP path):

- `xMeters[]` is forward, `yMeters[]` is left.
- Bearing = `atan2(y, x)` in degrees (0 = forward, +90 = left, −90 = right).
- Sector index returned by `LidarDecoder.sectorIndex(x, y)` uses the same
  atan2 convention, so sector labels (F / FL / L / BL / B / BR / R / FR)
  line up without remapping.

The two conventions match, so `RosLidarBridge.buildScan` writes the raw
`(cos a · r, sin a · r)` into the Scan and lets everything downstream run
untouched.

---

## 5. Files changed

All paths are relative to
`peanut-sdk-v1.3.0/peanut-sdk-v1.3.0/SampleApp/`.

### 5.1 New: [RosLidarBridge.java](app/src/main/java/com/keenon/peanut/sample/receiver/RosLidarBridge.java)

Self-contained WebSocket client and scan converter. Key members:

| Symbol | Role |
|---|---|
| `DEFAULT_URI = "ws://192.168.64.20:9090"` | rosbridge endpoint |
| `DEFAULT_TOPIC = "/scan"` | ROS topic to subscribe to |
| `DEFAULT_THROTTLE_MS = 200` | passed to rosbridge as `throttle_rate` (ms) |
| `Listener.onScan(Scan)` | delivered on the WS worker thread |
| `Listener.onStatus(String, boolean)` | connect / error text for UI |
| `start()` / `stop()` | idempotent; `stop()` sends unsubscribe first |
| `buildScan(JSONObject msg)` | `LaserScan` → `LidarDecoder.Scan` |

`buildScan` reproduces the same bookkeeping `LidarDecoder` does for the
packed-byte path — min/max/mean distance, closest point + bearing, 8 sector
minimums — so the UI sees exactly the same `Scan` fields regardless of
source.

Decimation: if a scan has more than `LidarDecoder.MAX_POINTS` (720) returns,
points are kept at stride `ceil(n / MAX_POINTS)` to keep rendering cheap.
For the 541-point `/scan` that stride is 1 (no decimation).

Dependency: `org.java-websocket:Java-WebSocket:1.5.3` — already in
[app/build.gradle](app/build.gradle) (originally added for the remote-control
WebSocket server; we reuse it as a client).

### 5.2 Edited: [SensorDataManager.java](app/src/main/java/com/keenon/peanut/sample/receiver/SensorDataManager.java)

- Added fields: `lidarRosMode`, `latestRosScan`, `latestRosScanMs`,
  `rosBridgeStatus`, `rosBridge`.
- Added public API:
  - `setLidarRosMode(boolean)` — synchronized; creates/starts/stops
    `RosLidarBridge`; clears `latestRosScan` on disable.
  - `isLidarRosMode()`, `getRosBridgeStatus()`.
- Reworked `applyLidarFields` into a three-branch select:
  1. ROS mode → use `latestRosScan` (or `Scan.empty()` if not yet arrived).
  2. Demo mode → synthetic `LidarDecoder.demoScanForUi()`.
  3. Default → decode the CoAP bytes in `latestLidar`.
- Exposed two new JSON fields in the unified payload: `lidar_is_ros` and
  `lidar_ros_status` (bridge connection text). `lidar_summary` is now
  prefixed with `(ros) ` or `(demo) ` when appropriate.

### 5.3 Edited: [SensorsFragment.java](app/src/main/java/com/keenon/peanut/supermarket/fragment/SensorsFragment.java)

- Added `private Switch swLidarRos;`.
- In `onViewCreated`, binds `R.id.sw_lidar_ros`, wires an `OnCheckedChange`
  listener to `setLidarRosMode`, and makes the two toggles mutually exclusive
  (turning one on turns the other off).

### 5.4 Edited: [fragment_dev_sensors.xml](app/src/main/res/layout/fragment_dev_sensors.xml)

- New `LinearLayout` row inside the LiDAR `MaterialCardView`, directly below
  the existing "Synthetic demo" row. Contains:
  - `TextView` labelled `"ROS bridge (ws://192.168.64.20:9090 /scan)"`
  - `Switch` with id `@+id/sw_lidar_ros`
- No other layout changes — row fits inside the card's vertical
  `LinearLayout`, so the renderer, stats, sectors and path hint below it are
  untouched.

---

## 6. rosbridge message shapes (as observed)

Handshake response from `ws://192.168.64.20:9090`:
```
HTTP/1.1 101 Switching Protocols
Upgrade: websocket
Connection: Upgrade
Sec-WebSocket-Accept: …
```

Our subscribe frame (text opcode):
```json
{"op":"subscribe","topic":"/scan","type":"sensor_msgs/LaserScan",
 "throttle_rate":200,"queue_length":1}
```

Publish frames we receive (one per scan, every ~200 ms):
```json
{
  "op": "publish",
  "topic": "/scan",
  "msg": {
    "header": {
      "seq": 0,
      "stamp": {"secs": 1777014706, "nsecs": 789020697},
      "frame_id": "laser_link"
    },
    "angle_min": -1.5707963705062866,
    "angle_max":  1.5707963705062866,
    "angle_increment": 0.005817763973027468,
    "time_increment": 0.0,
    "scan_time": 0.0,
    "range_min": 0.05000000074505806,
    "range_max": 30.0,
    "ranges":       [0.864, 0.837, 0.809, …  541 floats total],
    "intensities":  []
  }
}
```

Notes:

- `ranges[i]` can be `null` (represents NaN / no return). `buildScan`
  type-checks with `instanceof Number` and skips non-finite values.
- Returns outside `[range_min, range_max]` are skipped as invalid hits.
- `intensities` is empty for this sensor; we ignore it.

---

## 7. Lifecycle and thread safety

- The WebSocket worker lives on a thread spawned by `Java-WebSocket`. All
  `onOpen` / `onMessage` / `onClose` / `onError` callbacks land there.
- `latestRosScan` and `rosBridgeStatus` are `volatile` and written
  exclusively from that thread; read by the scheduled executor thread that
  drives `buildUnifiedJson()` every 200 ms. Atomic read/write on object
  references is sufficient here — there is no multi-field invariant.
- `setLidarRosMode(boolean)` is synchronized on `SensorDataManager.this`;
  `start()` and `stop()` on `RosLidarBridge` are synchronized on the bridge
  instance. Concurrent toggle spam can't leak connections.
- `stop()` sends the unsubscribe op before closing, so rosbridge properly
  drops the subscriber-side registration.
- On `ws://` error (no route / refused), `Java-WebSocket` fires `onClose`
  remote=true and we retain `latestRosScan` as the last good frame.
  Nothing currently auto-reconnects — user toggles off/on to retry. Flag for
  a future patch if it matters.

---

## 8. Verification

### 8.1 Unit-level sanity (off-device)

`fetch_lidar.py` in the repo root pulls live `/scan` data over the exact
same protocol (rosbridge v2.0 subscribe over WebSocket). Used during
development as a ground-truth oracle:

- Same 541 points
- Same frame_id `laser_link`
- Same 180° span
- Same float-array `ranges` encoding

If `fetch_lidar.py` returns data and the app doesn't, the regression is in
our Java code; if neither does, the ROS side is down.

### 8.2 On-device

`adb logcat -s RosLidarBridge:* SensorDataManager:* SENSOR_ERROR:*`

Expected on toggle-on:
```
RosLidarBridge   I  rosbridge connected, subscribing to /scan
SENSOR_INFO      I  (heartbeat updates every ~200 ms)
```

UI signals:

- LiDAR card summary changes from `Unavailable` to `(ros) rays 541 · min 0.13 m · max 4.95 m · mean 1.42 m`
- `tv_lidar_hb` shows `seq N · 5.0/s · last just now`
- `LidarRadarView` draws a rough fan-shaped scatter (180° FOV), with the
  closest return marked by a red disc and a thin line from the robot center
- The 8 sectors line populates; back sectors (B, BL, BR) stay `—` because a
  front-only LaserScan never illuminates them

### 8.3 Failure modes the UI surfaces

- WebSocket refused → status `error: ConnectException …`, summary stays `Unavailable`
- Wrong topic → subscribe succeeds, no publishes, summary stays `Unavailable`
- Robot reboot mid-session → `onClose remote=true`, last scan stays in view
  until the user toggles off/on

---

## 9. How to build and install

```bash
cd peanut-sdk-v1.3.0/peanut-sdk-v1.3.0/SampleApp
./gradlew.bat :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.keenon.peanut.sample/com.keenon.peanut.supermarket.SupermarketActivity
```

Then on the tablet: **Developer → Sensors → LiDAR card → flip "ROS bridge"**.

---

## 10. Minor notes and decisions

- **Why not replace the CoAP path?** Because on a properly-provisioned robot
  (Peanut service running, SDK CoAP producer alive) the CoAP path is both
  faster (no JSON serialization) and richer (packed 7-byte-per-point format
  can carry more points). Keeping the toggle lets a single APK work in both
  setups.
- **Why Java-WebSocket instead of OkHttp / Netty?** It's already in the app's
  dependency graph for the remote-control server; zero new libs.
- **Why `throttle_rate: 200`?** Matches the existing 5 Hz UI refresh cadence
  — anything faster is thrown away by the fragment's redraw loop anyway,
  and 5 Hz keeps JSON parsing light on the RK3288.
- **Why not fall back automatically when CoAP returns empty?** Auto-fallback
  is easy to add later (watch `lastLidarErrorMs` and arm the bridge if
  stale > N seconds), but for first rollout an explicit toggle is clearer
  and avoids surprising users who intentionally bring the Peanut service
  up/down.
- **IPv4 hard-coded**: `192.168.64.20` is the robot-internal ROS address;
  same constant appears in the peanut_service app
  ([RosSocketClientManager.java:19](../../Peanut_servive%20main%20app/app/src/main/java/com/keenon/peanut/peanutservice/server/webSocket/client/RosSocketClientManager.java#L19)).
  If a future robot moves it, `RosLidarBridge.DEFAULT_URI` is the one place
  to change.
