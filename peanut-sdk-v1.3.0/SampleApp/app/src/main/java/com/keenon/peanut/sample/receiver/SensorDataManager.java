package com.keenon.peanut.sample.receiver;

import android.util.Log;
import android.os.SystemClock;

import com.keenon.sdk.constant.TopicName;
import com.keenon.sdk.external.IDataCallback;
import com.keenon.sdk.external.PeanutSDK;
import com.keenon.sdk.hedera.model.ApiError;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class SensorDataManager {

    private static final String TAG = "SensorDataManager";
    private static final String LOG_INFO = "SENSOR_INFO";
    private static final String LOG_ERROR = "SENSOR_ERROR";
    private static final boolean ENABLE_VERBOSE_SENSOR_LOGS = false;
    private static volatile SensorDataManager instance;

    // Listeners for unified data updates
    public interface SensorDataListener {
        void onSensorDataUpdated(String jsonPayload);
    }
    private final Set<SensorDataListener> listeners = Collections.newSetFromMap(new ConcurrentHashMap<SensorDataListener, Boolean>());

    // State Variables for caching latest verified sensor JSON
    private volatile String latestBattery = "{}";
    private volatile String latestPosition = "{}";
    private volatile String latestNavStatus = "{}";
    // SENSOR_IMU = /sensor/imu   → returns only {"imu": 0/1} (gyro health flag).
    // SENSOR_IMU_ANGLE = /sensor/imuangle → returns {"yaw","pitch","roll"} in degrees.
    // Both are cached so we can emit a single merged IMU view to the UI.
    private volatile String latestImu = "{}";
    private volatile String latestImuAngle = "{}";
    private volatile String latestCollision = "{}";
    private volatile String latestBump = "{}";
    private volatile String latestSonar = "{}";
    private volatile String latestMotor = "{}";
    private volatile String latestCharge = "{}";
    private volatile String latestLidar = "{}";

    /** When true, {@link #applyLidarFields} feeds {@link LidarDecoder#demoScanForUi()} so the radar UI works without live CoAP bytes. */
    private volatile boolean lidarDemoMode = false;

    /** When true, {@link #applyLidarFields} uses the latest {@link #latestRosScan}
     *  coming from {@link RosLidarBridge} instead of decoding CoAP bytes. */
    private volatile boolean lidarRosMode = false;
    private volatile LidarDecoder.Scan latestRosScan = null;
    private volatile long latestRosScanMs = 0L;
    private volatile String rosBridgeStatus = "";
    private RosLidarBridge rosBridge;

    /** When true, the IMU section in the unified JSON is driven by {@link RosImuBridge}
     *  (rosbridge subscribe to {@code /imu_raw}) instead of the CoAP caches. */
    private volatile boolean imuRosMode = false;
    private volatile RosImuBridge.Sample latestRosImu = null;
    private volatile long latestRosImuMs = 0L;
    private volatile String rosImuStatus = "";
    private RosImuBridge rosImuBridge;

    /** When true, sonar/bump/collision in the unified JSON come from {@link RosSafetyBridge}
     *  (rosbridge subscriptions to {@code /bump}, {@code /fdl/obs_low},
     *  {@code /SDS_collision_points}) instead of the CoAP caches. */
    private volatile boolean safetyRosMode = false;
    private volatile RosSafetyBridge.Snapshot latestRosSafety = null;
    private volatile long latestRosSafetyMs = 0L;
    private volatile String rosSafetyStatus = "";
    private RosSafetyBridge rosSafetyBridge;

    /** Last LiDAR subscribe / observe failure (e.g. empty payload from /sensor/lidar). */
    private volatile String lastLidarErrorMsg = "";
    private volatile long lastLidarErrorMs = 0L;

    private ScheduledExecutorService executorService;
    private volatile boolean isInitialized = false;

    // Topic heartbeats (timestamps use elapsedRealtime for monotonic comparisons)
    private final TopicHeartbeat hbPosition = new TopicHeartbeat("POSITION_POS_INFO");
    private final TopicHeartbeat hbNav = new TopicHeartbeat("NAVIGATION_STATUS");
    private final TopicHeartbeat hbSonar = new TopicHeartbeat("SENSOR_SONAR");
    private final TopicHeartbeat hbImu = new TopicHeartbeat("SENSOR_IMU");
    private final TopicHeartbeat hbImuAngle = new TopicHeartbeat("SENSOR_IMU_ANGLE");
    private final TopicHeartbeat hbLidar = new TopicHeartbeat("SENSOR_LIDAR");
    private final TopicHeartbeat hbBattery = new TopicHeartbeat("BATTERY_STATUS");
    private final TopicHeartbeat hbMotor = new TopicHeartbeat("MOTOR_STATUS");
    private final TopicHeartbeat hbCollision = new TopicHeartbeat("SENSOR_COLLISION");
    private final TopicHeartbeat hbBump = new TopicHeartbeat("SENSOR_BUMP_STATUS");
    private final TopicHeartbeat hbCharge = new TopicHeartbeat("CHARGE_MATCH_TIMES");

    private SensorDataManager() {
    }

    public static SensorDataManager getInstance() {
        if (instance == null) {
            synchronized (SensorDataManager.class) {
                if (instance == null) {
                    instance = new SensorDataManager();
                }
            }
        }
        return instance;
    }

    /** Enable synthetic LiDAR for the Developer → Sensors card (no robot payload required). */
    public void setLidarDemoMode(boolean enabled) {
        lidarDemoMode = enabled;
    }

    public boolean isLidarDemoMode() {
        return lidarDemoMode;
    }

    /**
     * Enable/disable the rosbridge-backed LiDAR source. When enabled, a
     * {@link RosLidarBridge} connects to {@link RosLidarBridge#DEFAULT_URI} and
     * subscribes to {@code /scan}; incoming scans replace the CoAP payload as
     * the input to the radar view.
     */
    public synchronized void setLidarRosMode(boolean enabled) {
        if (lidarRosMode == enabled) return;
        lidarRosMode = enabled;
        if (enabled) {
            if (rosBridge == null) {
                rosBridge = new RosLidarBridge(new RosLidarBridge.Listener() {
                    @Override public void onScan(LidarDecoder.Scan scan) {
                        latestRosScan = scan;
                        latestRosScanMs = SystemClock.elapsedRealtime();
                        hbLidar.onUpdate("ros:" + (scan == null ? 0 : scan.pointCount));
                    }
                    @Override public void onStatus(String status, boolean isError) {
                        rosBridgeStatus = status == null ? "" : status;
                        if (isError) {
                            Log.w(TAG, "ros lidar bridge: " + rosBridgeStatus);
                        } else {
                            Log.i(TAG, "ros lidar bridge: " + rosBridgeStatus);
                        }
                    }
                });
            }
            rosBridge.start();
        } else {
            if (rosBridge != null) {
                rosBridge.stop();
                rosBridge = null;
            }
            latestRosScan = null;
            rosBridgeStatus = "";
        }
    }

    public boolean isLidarRosMode() {
        return lidarRosMode;
    }

    public String getRosBridgeStatus() {
        return rosBridgeStatus;
    }

    /**
     * Enable/disable the rosbridge-backed IMU source. When enabled, a
     * {@link RosImuBridge} connects to {@link RosImuBridge#DEFAULT_URI} and
     * subscribes to {@code /imu_raw}; incoming quaternions replace
     * {@code /sensor/imuangle} as the input to the IMU card.
     */
    public synchronized void setImuRosMode(boolean enabled) {
        if (imuRosMode == enabled) return;
        imuRosMode = enabled;
        if (enabled) {
            if (rosImuBridge == null) {
                rosImuBridge = new RosImuBridge(new RosImuBridge.Listener() {
                    @Override public void onSample(RosImuBridge.Sample sample) {
                        latestRosImu = sample;
                        latestRosImuMs = SystemClock.elapsedRealtime();
                        hbImuAngle.onUpdate("ros:" + sample.yawDeg);
                    }
                    @Override public void onStatus(String status, boolean isError) {
                        rosImuStatus = status == null ? "" : status;
                        if (isError) Log.w(TAG, "ros imu bridge: " + rosImuStatus);
                        else Log.i(TAG, "ros imu bridge: " + rosImuStatus);
                    }
                });
            }
            rosImuBridge.start();
        } else {
            if (rosImuBridge != null) {
                rosImuBridge.stop();
                rosImuBridge = null;
            }
            latestRosImu = null;
            rosImuStatus = "";
        }
    }

    public boolean isImuRosMode() {
        return imuRosMode;
    }

    public String getRosImuStatus() {
        return rosImuStatus;
    }

    /**
     * Enable/disable the rosbridge-backed safety stack: drives sonar, bump
     * and collision cards from a single WebSocket subscribed to
     * {@link RosSafetyBridge#TOPIC_BUMP}, {@link RosSafetyBridge#TOPIC_OBS_LOW}
     * and {@link RosSafetyBridge#TOPIC_COLLISION}.
     */
    public synchronized void setSafetyRosMode(boolean enabled) {
        if (safetyRosMode == enabled) return;
        safetyRosMode = enabled;
        if (enabled) {
            if (rosSafetyBridge == null) {
                rosSafetyBridge = new RosSafetyBridge(new RosSafetyBridge.Listener() {
                    @Override public void onSnapshot(RosSafetyBridge.Snapshot s) {
                        latestRosSafety = s;
                        latestRosSafetyMs = SystemClock.elapsedRealtime();
                        // bump heartbeats so the UI shows "live" instead of stale
                        hbBump.onUpdate("ros:" + s.bumpActiveCount);
                        hbSonar.onUpdate("ros:" + s.sonarMinM);
                        hbCollision.onUpdate("ros:" + (s.collision ? 1 : 0));
                    }
                    @Override public void onStatus(String status, boolean isError) {
                        rosSafetyStatus = status == null ? "" : status;
                        if (isError) Log.w(TAG, "ros safety bridge: " + rosSafetyStatus);
                        else Log.i(TAG, "ros safety bridge: " + rosSafetyStatus);
                    }
                });
            }
            rosSafetyBridge.start();
        } else {
            if (rosSafetyBridge != null) {
                rosSafetyBridge.stop();
                rosSafetyBridge = null;
            }
            latestRosSafety = null;
            rosSafetyStatus = "";
        }
    }

    public boolean isSafetyRosMode() {
        return safetyRosMode;
    }

    public String getRosSafetyStatus() {
        return rosSafetyStatus;
    }

    public void addListener(SensorDataListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    public void removeListener(SensorDataListener listener) {
        if (listener != null) {
            listeners.remove(listener);
        }
    }

    public synchronized void init() {
        if (isInitialized) return;
        isInitialized = true;
        logInfo("Initializing and subscribing to sensor topics");

        // Subscribe to Battery
        PeanutSDK.getInstance().subscribe(TopicName.BATTERY_STATUS, new IDataCallback() {
            @Override public void success(String result) {
                String r = safeRaw(result);
                hbBattery.onUpdate(r);
                latestBattery = r;
            }
            @Override public void error(ApiError error) { logSensorError("BATTERY_STATUS", error); }
        });

        // Subscribe to Position
        // CoAP path: OBSERVE /position/robotPosInfo
        // Response shape:
        //   {"code":0,"msg":"ok","data":{
        //       "floor":  <int>,                 // map floor number
        //       "buildDesc": ["..."],            // optional string tags
        //       "elevatorPosState": <int>,       // 0 = not in elevator
        //       "robotPos": {                    // live chassis pose in map coords
        //           "x":  <double, metres>,
        //           "y":  <double, metres>,
        //           "rotation": <double, radians — yaw around Z>
        //           "curId": <int>, "curName": <string>, "curDistance": <double>
        //       }
        //    }, "topic":"com.keenon.sdk.api.PositionPosInfoApi"}
        //
        // Typical update rate on the algorithm board: ~5 Hz while localised,
        // silent while the robot is not localised (no valid pose yet).
        PeanutSDK.getInstance().subscribe(TopicName.POSITION_POS_INFO, new IDataCallback() {
            @Override public void success(String result) { setPosition(result); }
            @Override public void error(ApiError error) { logSensorError("POSITION_POS_INFO", error); }
        });

        // Subscribe to Navigation Status
        PeanutSDK.getInstance().subscribe(TopicName.NAVIGATION_STATUS, new IDataCallback() {
            @Override public void success(String result) { setNav(result); }
            @Override public void error(ApiError error) { logSensorError("NAVIGATION_STATUS", error); }
        });

        // Subscribe to IMU (gyro health flag only)
        // CoAP path: OBSERVE /sensor/imu  → response: {"data":{"imu":<int>}}
        //   imu == 0  → gyroscope faulted / uncalibrated
        //   imu == 1  → gyroscope ok
        // NOTE: this topic does NOT carry yaw/pitch/roll — those come from SENSOR_IMU_ANGLE
        // (see next subscription). Both topics update at ~1 Hz on the algorithm board.
        PeanutSDK.getInstance().subscribe(TopicName.SENSOR_IMU, new IDataCallback() {
            @Override public void success(String result) {
                String r = safeRaw(result);
                hbImu.onUpdate(r);
                latestImu = r;
                logSensorVerbose("IMU", latestImu);
            }
            @Override public void error(ApiError error) { logSensorError("SENSOR_IMU", error); }
        });

        // Subscribe to IMU Angle (orientation)
        // CoAP path: OBSERVE /sensor/imuangle → response: {"data":{"yaw":..,"pitch":..,"roll":..}}
        // Degrees, updated at ~1 Hz. Matches the peanut_service reference app — which
        // internally wires /sensor/imu AND /sensor/imuangle together via SensorImuProxyApi
        // before surfacing a combined IMU view.
        PeanutSDK.getInstance().subscribe(TopicName.SENSOR_IMU_ANGLE, new IDataCallback() {
            @Override public void success(String result) {
                String r = safeRaw(result);
                hbImuAngle.onUpdate(r);
                latestImuAngle = r;
                logSensorVerbose("IMU_ANGLE", latestImuAngle);
            }
            @Override public void error(ApiError error) { logSensorError("SENSOR_IMU_ANGLE", error); }
        });

        // Subscribe to LiDAR (navigation 360° lidar)
        // CoAP path: OBSERVE /sensor/lidar  (RequestEnum.OBSERVER_RAW)
        // Response is a hex string of packed 7-byte-per-point XY centimetre values.
        // The SDK converts the hex → byte[] and wraps it as:
        //   {"data":{"count":<int>,"base64":"...","bytes":[b0,b1,...]},
        //    "topic":"com.keenon.sdk.api.SensorLidarApi"}
        // A single scan can contain several hundred points (~5 Hz when the LiDAR
        // driver is healthy; no payloads while the robot is in STANDBY / the
        // LiDAR motor is off).
        PeanutSDK.getInstance().subscribe(TopicName.SENSOR_LIDAR, new IDataCallback() {
            @Override public void success(String result) {
                String r = safeRaw(result);
                hbLidar.onUpdate(r);
                latestLidar = r;
                lastLidarErrorMsg = "";
                lastLidarErrorMs = 0L;
                logSensorVerbose("LIDAR", result);
            }
            @Override public void error(ApiError error) {
                recordLidarError(error);
                logSensorError("SENSOR_LIDAR", error);
            }
        });

        // Subscribe to Collision
        PeanutSDK.getInstance().subscribe(TopicName.SENSOR_COLLISION, new IDataCallback() {
            @Override public void success(String result) {
                String r = safeRaw(result);
                hbCollision.onUpdate(r);
                latestCollision = r;
            }
            @Override public void error(ApiError error) { logSensorError("SENSOR_COLLISION", error); }
        });

        // Subscribe to Bump
        PeanutSDK.getInstance().subscribe(TopicName.SENSOR_BUMP_STATUS, new IDataCallback() {
            @Override public void success(String result) {
                String r = safeRaw(result);
                hbBump.onUpdate(r);
                latestBump = r;
                logSensorVerbose("TOUCH/BUMP", latestBump);
            }
            @Override public void error(ApiError error) { logSensorError("SENSOR_BUMP_STATUS", error); }
        });

        // Subscribe to Sonar
        PeanutSDK.getInstance().subscribe(TopicName.SENSOR_SONAR, new IDataCallback() {
            @Override public void success(String result) { setSonar(result); }
            @Override public void error(ApiError error) { logSensorError("SENSOR_SONAR", error); }
        });

        // Subscribe to Motor Status
        PeanutSDK.getInstance().subscribe(TopicName.MOTOR_STATUS, new IDataCallback() {
            @Override public void success(String result) {
                String r = safeRaw(result);
                hbMotor.onUpdate(r);
                latestMotor = r;
            }
            @Override public void error(ApiError error) { logSensorError("MOTOR_STATUS", error); }
        });

        // Subscribe to Charging Status (Charge Match Times)
        PeanutSDK.getInstance().subscribe(TopicName.CHARGE_MATCH_TIMES, new IDataCallback() {
            @Override public void success(String result) {
                String r = safeRaw(result);
                hbCharge.onUpdate(r);
                latestCharge = r;
                logSensorVerbose("CHARGING", latestCharge);
            }
            @Override public void error(ApiError error) { logSensorError("CHARGE_MATCH_TIMES", error); }
        });

        // Start Throttled Emission (5 Hz / every 200 ms)
        startThrottledEmission();
    }

    private void startThrottledEmission() {
        if (executorService != null && !executorService.isShutdown()) {
            return;
        }
        executorService = Executors.newSingleThreadScheduledExecutor();
        executorService.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                if (listeners.isEmpty()) return;
                
                String combinedJson = buildUnifiedJson();
                for (SensorDataListener listener : listeners) {
                    try {
                        listener.onSensorDataUpdated(combinedJson);
                    } catch (Exception e) {
                        Log.e(LOG_ERROR, "Listener dispatch failure", e);
                    }
                }
            }
        }, 200, 200, TimeUnit.MILLISECONDS);
    }

    public synchronized void release() {
        if (!isInitialized) return;
        logInfo("Releasing manager");
        
        if (executorService != null) {
            executorService.shutdownNow();
            executorService = null;
        }
        listeners.clear();
        isInitialized = false;
        // Not calling unSubscribe on PeanutSDK because the SDK automatically cleans up topics
        // if the entire module shuts down, but we simply keep the singletons active.
    }

    private String buildUnifiedJson() {
        JSONObject out = new JSONObject();
        try {
            out.put("type", "sensor_snapshot");
            JSONObject data = new JSONObject();
            
            // Battery
            data.put("battery_percent", extractInt(latestBattery, "power", "batteryLevel", "level"));
            
            // Position (live x/y/theta and floor)
            // /position/robotPosInfo returns data.robotPos.{x,y,rotation}; the current
            // pose lives one level deep — not at data.x/data.y. Prior to this fix the
            // SampleApp was reading data.x/data.y directly, which is why the card
            // stayed blank even when the robot was moving. We still fall back to the
            // flat shape in case a future SDK build publishes it at the root.
            JSONObject posRaw = getPayloadData(latestPosition);
            JSONObject robotPos = posRaw != null ? posRaw.optJSONObject("robotPos") : null;
            if (robotPos != null) {
                data.put("position_x", extractDoubleObj(robotPos, "x", "posX"));
                data.put("position_y", extractDoubleObj(robotPos, "y", "posY"));
            } else {
                data.put("position_x", extractDoubleObj(posRaw, "x", "posX"));
                data.put("position_y", extractDoubleObj(posRaw, "y", "posY"));
            }
            if (posRaw != null) {
                data.put("position_floor", posRaw.has("floor") ? posRaw.optInt("floor", 0) : JSONObject.NULL);
                data.put("position_cur_name", posRaw.optString("curName", ""));
            } else {
                data.put("position_floor", JSONObject.NULL);
                data.put("position_cur_name", "");
            }
            
            // Motor
            data.put("motor_state", extractInt(latestMotor, "status", "state"));
            
            // Nav
            data.put("nav_state", extractInt(latestNavStatus, "state", "status"));
            
            // Sonar / Collision / Bump — three "safety" cards. When safetyRosMode is
            // on, all three come from a single RosSafetyBridge snapshot (live ROS
            // topics /fdl/obs_low, /SDS_collision_points, /bump). Otherwise CoAP.
            data.put("safety_is_ros", safetyRosMode);
            data.put("safety_ros_status", rosSafetyStatus == null ? "" : rosSafetyStatus);
            String sonarDist;
            boolean isCol;
            if (safetyRosMode && latestRosSafety != null) {
                RosSafetyBridge.Snapshot ss = latestRosSafety;
                if (Double.isFinite(ss.sonarMinM)) {
                    sonarDist = String.format(java.util.Locale.getDefault(),
                            "%.2f (ros /fdl/obs_low, n=%d)", ss.sonarMinM, ss.obsLowPointCount);
                } else if (ss.obsLowPointCount == 0) {
                    sonarDist = "(ros) no obstacles in low-cloud";
                } else {
                    sonarDist = "No reading";
                }
                isCol = ss.collision;
            } else {
                JSONObject sonarRaw = getPayloadData(latestSonar);
                sonarDist = "No reading";
                if (sonarRaw != null) {
                    Object sonar = sonarRaw.opt("distance");
                    if (sonar instanceof Number) {
                        double s = ((Number) sonar).doubleValue();
                        if (Double.isFinite(s) && s >= 0d) {
                            sonarDist = String.format(java.util.Locale.getDefault(), "%.2f", s);
                        }
                    } else if (sonar != null) {
                        String s = String.valueOf(sonar).trim();
                        if (!s.isEmpty() && !"null".equalsIgnoreCase(s) && !"N/A".equalsIgnoreCase(s)) {
                            sonarDist = s;
                        }
                    }
                }
                JSONObject colRaw = getPayloadData(latestCollision);
                isCol = false;
                if (colRaw != null) {
                    isCol = colRaw.optBoolean("collision", false) || colRaw.optBoolean("isCollision", false);
                }
            }
            data.put("sonar_distance", sonarDist);
            data.put("collision_state", isCol ? "YES" : "NO");

            // Charge
            JSONObject chargeRaw = getPayloadData(latestCharge);
            data.put("charge_state", extractChargeState(chargeRaw));

            // IMU — combined view from /sensor/imu (gyro flag) AND /sensor/imuangle
            // (yaw/pitch/roll). Both topics are cached independently; angles come
            // from the imuangle topic while the health flag comes from imu.
            // When imuRosMode == true, the feed comes from /imu_raw via rosbridge
            // instead: quaternion orientation is converted to yaw/pitch/roll
            // degrees, and health is forced to 1 since ROS publishes imply a live
            // driver.
            data.put("imu_is_ros", imuRosMode);
            data.put("imu_ros_status", rosImuStatus == null ? "" : rosImuStatus);
            if (imuRosMode && latestRosImu != null) {
                RosImuBridge.Sample s = latestRosImu;
                data.put("imu_summary", String.format(java.util.Locale.getDefault(),
                        "(ros) Yaw %.1f, Pitch %.1f, Roll %.1f",
                        s.yawDeg, s.pitchDeg, s.rollDeg));
                data.put("imu_yaw", finiteOrNaNJson(s.yawDeg));
                data.put("imu_pitch", finiteOrNaNJson(s.pitchDeg));
                data.put("imu_roll", finiteOrNaNJson(s.rollDeg));
                data.put("imu_health", 1);
                data.put("imu_ang_vel_x", finiteOrNaNJson(s.angVelX));
                data.put("imu_ang_vel_y", finiteOrNaNJson(s.angVelY));
                data.put("imu_ang_vel_z", finiteOrNaNJson(s.angVelZ));
                data.put("imu_lin_acc_x", finiteOrNaNJson(s.linAccX));
                data.put("imu_lin_acc_y", finiteOrNaNJson(s.linAccY));
                data.put("imu_lin_acc_z", finiteOrNaNJson(s.linAccZ));
                data.put("imu_frame_id", s.frameId == null ? "" : s.frameId);
            } else {
                JSONObject imuRaw = getPayloadData(latestImu);               // {"imu": 0|1}
                JSONObject imuAngleRaw = getPayloadData(latestImuAngle);     // {"yaw","pitch","roll"}
                data.put("imu_summary", buildImuSummary(imuAngleRaw));
                data.put("imu_yaw",
                        imuAngleRaw != null
                                ? finiteOrNaNJson(imuAngleRaw.optDouble("yaw", Double.NaN))
                                : JSONObject.NULL);
                data.put("imu_pitch",
                        imuAngleRaw != null
                                ? finiteOrNaNJson(imuAngleRaw.optDouble("pitch", Double.NaN))
                                : JSONObject.NULL);
                data.put("imu_roll",
                        imuAngleRaw != null
                                ? finiteOrNaNJson(imuAngleRaw.optDouble("roll", Double.NaN))
                                : JSONObject.NULL);
                data.put("imu_health",
                        imuRaw != null && imuRaw.has("imu")
                                ? imuRaw.optInt("imu", -1)
                                : JSONObject.NULL);
            }

            // Bump / Touch — same toggle as sonar/collision (safetyRosMode).
            String bumpState;
            if (safetyRosMode && latestRosSafety != null) {
                RosSafetyBridge.Snapshot ss = latestRosSafety;
                if (ss.bumped) {
                    bumpState = String.format(java.util.Locale.getDefault(),
                            "YES (ros %d/%d sensors)", ss.bumpActiveCount, ss.bumpTotalSensors);
                } else if (ss.bumpTotalSensors > 0) {
                    bumpState = String.format(java.util.Locale.getDefault(),
                            "NO (ros %d sensors monitored)", ss.bumpTotalSensors);
                } else {
                    bumpState = "(ros) no /bump frames yet";
                }
            } else {
                JSONObject bumpRaw = getPayloadData(latestBump);
                bumpState = bumpRaw == null
                        ? "NO" : (bumpRaw.optBoolean("bump", false) || bumpRaw.optBoolean("touched", false)
                        || bumpRaw.optInt("status", 0) != 0 ? "YES" : "NO");
            }
            data.put("bump_state", bumpState);

            // Position theta (chassis yaw in radians)
            // robotPos.rotation is the authoritative yaw in PeanutSDK v1.3.0.
            // Fall back to theta/yaw/orientation at the root for legacy shapes.
            if (robotPos != null && robotPos.has("rotation")) {
                data.put("position_theta",
                        finiteOrNaNJson(robotPos.optDouble("rotation", Double.NaN)));
            } else {
                data.put("position_theta", posRaw != null
                        ? extractDoubleObj(posRaw, "theta", "yaw", "orientation")
                        : JSONObject.NULL);
            }

            // LiDAR — rich snapshot (summary + scalar metrics + sector min distances
            // + compact XY point cloud so the fragment can render a radar view).
            applyLidarFields(data, latestLidar);

            out.put("data", data);

            // Heartbeat meta (safe additive fields; does not affect existing parsing)
            JSONObject meta = new JSONObject();
            meta.put("hb_position", hbPosition.toJson());
            meta.put("hb_navigation", hbNav.toJson());
            meta.put("hb_sonar", hbSonar.toJson());
            meta.put("hb_imu", hbImu.toJson());
            meta.put("hb_imu_angle", hbImuAngle.toJson());
            meta.put("hb_lidar", hbLidar.toJson());
            meta.put("hb_battery", hbBattery.toJson());
            meta.put("hb_motor", hbMotor.toJson());
            meta.put("hb_collision", hbCollision.toJson());
            meta.put("hb_bump", hbBump.toJson());
            meta.put("hb_charge", hbCharge.toJson());
            meta.put("lidar_demo_mode", lidarDemoMode);

            // Emit CoAP path hints so the Developer UI can show which SDK path is
            // backing each card (useful when "unavailable" vs "no data yet").
            // Paths come from @CoapCommond annotations on the SDK Api classes
            // (peanut-sdk-v1.3.0 / com.keenon.sdk.api.*Api.java).
            JSONObject paths = new JSONObject();
            paths.put("position",   "OBSERVE /position/robotPosInfo");
            paths.put("imu",        "OBSERVE /sensor/imu");
            paths.put("imu_angle",  "OBSERVE /sensor/imuangle");
            paths.put("lidar",      "OBSERVE /sensor/lidar (raw bytes)");
            paths.put("sonar",      "OBSERVE /sensor/sonar");
            paths.put("collision",  "OBSERVE /sensor/bump");
            paths.put("bump",       "OBSERVE /sensor/bumpStatus");
            paths.put("motor",      "OBSERVE /motor/status");
            paths.put("battery",    "OBSERVE /charge/status");            // BatteryStatusApi → /charge/status
            paths.put("nav",        "OBSERVE /navigation/status");
            paths.put("charge",     "OBSERVE /charge/matchTimes");         // ChargeMatchTimesApi
            meta.put("coap_paths", paths);

            out.put("meta", meta);

            if (ENABLE_VERBOSE_SENSOR_LOGS) {
                Log.d(TAG, "Emitted sensor snapshot: " + out.toString());
            }

        } catch (JSONException e) {
            Log.e(LOG_ERROR, "Error building unified sensor JSON", e);
        }
        return out.toString();
    }

    private JSONObject getPayloadData(String rawResult) {
        if (rawResult == null || rawResult.trim().isEmpty() || rawResult.equals("{}")) return null;
        try {
            JSONObject root = new JSONObject(rawResult);
            if (root.has("data")) {
                return root.optJSONObject("data");
            }
            return root; // sometimes no 'data' wrapper
        } catch (JSONException e) {
            return null;
        }
    }

    private int extractInt(String rawResult, String... keys) {
        JSONObject payload = getPayloadData(rawResult);
        if (payload != null) {
            for (String key : keys) {
                if (payload.has(key)) {
                    Object value = payload.opt(key);
                    if (value instanceof Number) {
                        return ((Number) value).intValue();
                    }
                }
            }
        }
        return -1;
    }
    
    private Object extractDoubleObj(JSONObject payload, String... keys) {
        if (payload != null) {
            for (String key : keys) {
                if (payload.has(key)) {
                    double val = payload.optDouble(key, Double.NaN);
                    if (!Double.isNaN(val) && Double.isFinite(val)) {
                        return val;
                    }
                }
            }
        }
        return JSONObject.NULL;
    }

    private String safeRaw(String raw) {
        if (raw == null) {
            return "{}";
        }
        String trimmed = raw.trim();
        return trimmed.isEmpty() ? "{}" : trimmed;
    }

    private String extractChargeState(JSONObject chargeRaw) {
        if (chargeRaw == null) {
            return "Unknown";
        }
        if (chargeRaw.optBoolean("charging", false) || chargeRaw.optBoolean("isCharging", false)) {
            return "Charging";
        }
        int status = chargeRaw.optInt("status", -1);
        if (status == 1) {
            return "Charging";
        }
        if (status == 0) {
            return "Not charging";
        }
        return "Unknown";
    }

    private String buildImuSummary(JSONObject imuRaw) {
        if (imuRaw == null) {
            return "Unavailable";
        }
        double yaw = finiteOrNaN(imuRaw.optDouble("yaw", Double.NaN));
        double pitch = finiteOrNaN(imuRaw.optDouble("pitch", Double.NaN));
        double roll = finiteOrNaN(imuRaw.optDouble("roll", Double.NaN));
        if (Double.isNaN(yaw) && Double.isNaN(pitch) && Double.isNaN(roll)) {
            return "Unavailable";
        }
        return String.format(java.util.Locale.getDefault(), "Yaw %.1f, Pitch %.1f, Roll %.1f",
                Double.isNaN(yaw) ? 0d : yaw,
                Double.isNaN(pitch) ? 0d : pitch,
                Double.isNaN(roll) ? 0d : roll);
    }

    private double finiteOrNaN(double v) {
        return Double.isFinite(v) ? v : Double.NaN;
    }

    /** Returns the double if finite, else {@link JSONObject#NULL} so JSON still parses cleanly. */
    private Object finiteOrNaNJson(double v) {
        return Double.isFinite(v) ? v : JSONObject.NULL;
    }

    /**
     * Decodes the cached raw LiDAR payload (emitted by the SDK's {@code SensorLidarApi}
     * — a hex/base64 blob of 7-byte-per-point XY coordinates in cm) and writes a
     * compact, consumer-friendly set of fields into {@code data}:
     *
     * <ul>
     *   <li>{@code lidar_summary}            — single line (backwards-compatible)</li>
     *   <li>{@code lidar_point_count}        — integer number of valid returns</li>
     *   <li>{@code lidar_min_m / max_m / mean_m} — distance statistics</li>
     *   <li>{@code lidar_closest_m}          — nearest return distance</li>
     *   <li>{@code lidar_closest_dir_deg}    — bearing in degrees (+x = 0, +y = +90)</li>
     *   <li>{@code lidar_closest_dir_label}  — short label (F/FL/L/BL/B/BR/R/FR)</li>
     *   <li>{@code lidar_sectors_m}          — array of 8 min distances (one per 45° wedge)</li>
     *   <li>{@code lidar_points_x / lidar_points_y} — decimated XY arrays (metres)</li>
     * </ul>
     *
     * <p>All fields are present even when the scan is empty (using {@link JSONObject#NULL}
     * or "Unavailable" where appropriate) so downstream UI code never has to probe for
     * missing keys.
     */
    private void applyLidarFields(JSONObject data, String rawLidar) throws JSONException {
        final LidarDecoder.Scan scan;
        final String sourceTag;
        if (lidarRosMode) {
            scan = latestRosScan != null ? latestRosScan : LidarDecoder.Scan.empty();
            sourceTag = "(ros) ";
        } else if (lidarDemoMode) {
            scan = LidarDecoder.demoScanForUi();
            sourceTag = "(demo) ";
        } else {
            scan = LidarDecoder.decode(rawLidar);
            sourceTag = "";
        }

        data.put("lidar_is_demo", lidarDemoMode);
        data.put("lidar_is_ros", lidarRosMode);
        data.put("lidar_ros_status", rosBridgeStatus == null ? "" : rosBridgeStatus);
        String errHint = "";
        if (!lidarDemoMode && !lidarRosMode) {
            long ago = SystemClock.elapsedRealtime() - lastLidarErrorMs;
            if (!scan.hasPoints() && lastLidarErrorMs > 0L && ago >= 0L && ago < 120_000L
                    && lastLidarErrorMsg != null && !lastLidarErrorMsg.isEmpty()) {
                errHint = lastLidarErrorMsg;
            }
        } else if (lidarRosMode && !scan.hasPoints() && !rosBridgeStatus.isEmpty()) {
            errHint = "rosbridge: " + rosBridgeStatus;
        }
        data.put("lidar_error_hint", errHint);
        data.put("lidar_summary", sourceTag + scan.summary());
        data.put("lidar_point_count", scan.pointCount);
        data.put("lidar_min_m", scan.hasPoints() ? finiteOrNaNJson(scan.minMeters) : JSONObject.NULL);
        data.put("lidar_max_m", scan.hasPoints() ? finiteOrNaNJson(scan.maxMeters) : JSONObject.NULL);
        data.put("lidar_mean_m", scan.hasPoints() ? finiteOrNaNJson(scan.meanMeters) : JSONObject.NULL);
        data.put("lidar_closest_m", scan.hasPoints() ? finiteOrNaNJson(scan.closestMeters) : JSONObject.NULL);
        data.put("lidar_closest_dir_deg",
                scan.hasPoints() ? finiteOrNaNJson(scan.closestBearingDeg) : JSONObject.NULL);
        data.put("lidar_closest_dir_label",
                scan.hasPoints() ? LidarDecoder.bearingLabel(scan.closestBearingDeg) : "—");

        JSONArray sectors = new JSONArray();
        for (int i = 0; i < scan.sectorsMeters.length; i++) {
            double v = scan.sectorsMeters[i];
            sectors.put(Double.isFinite(v) ? (Object) v : JSONObject.NULL);
        }
        data.put("lidar_sectors_m", sectors);

        JSONArray xs = new JSONArray();
        JSONArray ys = new JSONArray();
        for (int i = 0; i < scan.pointCount; i++) {
            xs.put(round3(scan.xMeters[i]));
            ys.put(round3(scan.yMeters[i]));
        }
        data.put("lidar_points_x", xs);
        data.put("lidar_points_y", ys);
    }

    private static double round3(float v) {
        return Math.round(v * 1000d) / 1000d;
    }

    private void logInfo(String message) {
        Log.i(LOG_INFO, message);
    }

    private void logSensorVerbose(String topic, String value) {
        if (ENABLE_VERBOSE_SENSOR_LOGS) {
            Log.d(TAG, topic + ": " + value);
        }
    }

    private void logSensorError(String topic, ApiError error) {
        Log.e(LOG_ERROR, "Subscribe error on " + topic + ": " + String.valueOf(error));
    }

    private void recordLidarError(ApiError error) {
        String s = error == null ? "" : String.valueOf(error);
        if (s.length() > 240) {
            s = s.substring(0, 237) + "...";
        }
        lastLidarErrorMsg = s;
        lastLidarErrorMs = SystemClock.elapsedRealtime();
    }

    private class TopicHeartbeat {
        private final String name;
        private volatile long lastUpdateMs = 0L;
        private volatile long lastChangeMs = 0L;
        private volatile int seq = 0;
        private volatile int changeSeq = 0;
        private volatile int windowCount = 0;
        private volatile long windowStartMs = 0L;
        private volatile double updatesPerSec = 0d;
        private volatile int lastHash = 0;

        TopicHeartbeat(String name) {
            this.name = name;
        }

        synchronized void onUpdate(String raw) {
            long now = SystemClock.elapsedRealtime();
            lastUpdateMs = now;
            seq++;

            int h = raw != null ? raw.hashCode() : 0;
            if (seq == 1 || h != lastHash) {
                lastHash = h;
                lastChangeMs = now;
                changeSeq++;
            }

            if (windowStartMs == 0L) {
                windowStartMs = now;
                windowCount = 0;
            }
            windowCount++;
            long dt = now - windowStartMs;
            if (dt >= 1000L) {
                updatesPerSec = (windowCount * 1000d) / (double) dt;
                windowStartMs = now;
                windowCount = 0;
            }
        }

        JSONObject toJson() throws JSONException {
            JSONObject o = new JSONObject();
            o.put("name", name);
            o.put("last_update_ms", lastUpdateMs);
            o.put("last_change_ms", lastChangeMs);
            o.put("seq", seq);
            o.put("change_seq", changeSeq);
            o.put("ups", updatesPerSec);
            return o;
        }
    }

    // Hook heartbeats into the existing cached-topic updates
    // (minimal: do not restructure subscriptions)
    private String setPosition(String result) {
        String r = safeRaw(result);
        hbPosition.onUpdate(r);
        latestPosition = r;
        return r;
    }

    private String setNav(String result) {
        String r = safeRaw(result);
        hbNav.onUpdate(r);
        latestNavStatus = r;
        return r;
    }

    private String setSonar(String result) {
        String r = safeRaw(result);
        hbSonar.onUpdate(r);
        latestSonar = r;
        return r;
    }
}
