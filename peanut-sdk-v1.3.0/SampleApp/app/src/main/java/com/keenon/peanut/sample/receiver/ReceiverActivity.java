package com.keenon.peanut.sample.receiver;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.keenon.peanut.sample.R;
import com.keenon.peanut.sample.util.BaseActivity;
import com.keenon.sdk.external.PeanutSDK;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Status dashboard and remote control receiver activity.
 *
 * Shows:
 *   - Robot IP address
 *   - SDK initialization state
 *   - WebSocket server status (running/stopped)
 *   - Phone client connection status
 *   - Current movement command
 *   - Scrollable activity log
 *
 * Starts WebSocketServerService and receives broadcast updates.
 */
public class ReceiverActivity extends BaseActivity {

    private static final String TAG = "RobotReceiver";
    private static final String LOG_INFO = "INFO";
    private static final String LOG_ERROR = "ERROR";
    private static final int MAX_LOG_LINES = 300;

    // --- Status display views ---
    private TextView tvRobotIp;
    private TextView tvSdkState;
    private TextView tvServerStatus;
    private TextView tvClientStatus;
    private TextView tvCurrentCommand;
    private TextView tvLog;
    private ScrollView svLog;
    private Button btnStartStop;

    private final StringBuilder logBuilder = new StringBuilder();
    private final SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
    private int logLineCount = 0;

    private WebSocketServerService wsService;
    private boolean isBound = false;
    private boolean isServiceStarted = false;
    private boolean receiversRegistered = false;

    // Diagnostics Views
    private TextView tvDiagBattery;
    private TextView tvDiagPosition;
    private TextView tvDiagMotor;
    private TextView tvDiagSonar;
    private TextView tvDiagCollision;
    private TextView tvDiagTopicHeartbeat;

    // Command/motion estimation state (UI-only; does not affect control)
    private volatile int lastCommandDirection = CommandParser.COMMAND_UNKNOWN;
    private double lastPosX = Double.NaN;
    private double lastPosY = Double.NaN;
    private long lastPosTimeMs = 0L;
    private static final double MOTION_DISTANCE_THRESHOLD = 0.05d; // meters-ish; conservative
    private static final long MOTION_WINDOW_MS = 2000L;

    // =========================================================================
    // Lifecycle
    // =========================================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_receiver);
        setButtonBack();

        initViews();
        displayStaticInfo();

        SensorDataManager.getInstance().init();
        SensorDataManager.getInstance().addListener(sensorDataListener);
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceivers();
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceivers();
    }

    @Override
    protected void onDestroy() {
        Log.i(LOG_INFO, "ReceiverActivity onDestroy");

        SensorDataManager.getInstance().removeListener(sensorDataListener);
        // Note: We don't necessarily call release() on SensorDataManager here 
        // to optionally let it run if the service is still active.

        // UI is not the owner of core logic.
        // If user started the service, it should keep running in background.
        if (isBound) {
            try {
                unbindService(serviceConnection);
            } catch (IllegalArgumentException e) {
                Log.w(LOG_ERROR, "Service already unbound");
            }
            isBound = false;
        }

        super.onDestroy();
    }

    // =========================================================================
    // UI Initialization
    // =========================================================================

    private void initViews() {
        tvRobotIp = findViewById(R.id.tv_robot_ip);
        tvSdkState = findViewById(R.id.tv_sdk_state);
        tvServerStatus = findViewById(R.id.tv_server_status);
        tvClientStatus = findViewById(R.id.tv_client_status);
        tvCurrentCommand = findViewById(R.id.tv_current_command);
        tvLog = findViewById(R.id.tv_receiver_log);
        svLog = findViewById(R.id.sv_receiver_log);
        btnStartStop = findViewById(R.id.btn_start_stop_server);

        tvDiagBattery = findViewById(R.id.tv_diag_battery);
        tvDiagPosition = findViewById(R.id.tv_diag_position);
        tvDiagMotor = findViewById(R.id.tv_diag_motor);
        tvDiagSonar = findViewById(R.id.tv_diag_sonar);
        tvDiagCollision = findViewById(R.id.tv_diag_collision);
        tvDiagTopicHeartbeat = findViewById(R.id.tv_diag_topic_heartbeat);

        btnStartStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isServiceStarted) {
                    stopReceiverService();
                } else {
                    startReceiverService();
                }
            }
        });

        // Initial UI state
        tvServerStatus.setText("Stopped");
        tvServerStatus.setTextColor(Color.RED);
        tvClientStatus.setText("No client");
        tvClientStatus.setTextColor(Color.GRAY);
        tvCurrentCommand.setText("NONE");
        tvCurrentCommand.setTextColor(Color.GRAY);
    }

    private void displayStaticInfo() {
        // Robot IP from WiFi
        String ip = getDeviceIpAddress();
        tvRobotIp.setText(ip != null ? ip : "Unknown (No WiFi)");

        // SDK state
        try {
            PeanutSDK sdk = PeanutSDK.getInstance();
            if (sdk != null) {
                tvSdkState.setText("Initialized");
                tvSdkState.setTextColor(Color.GREEN);
            } else {
                tvSdkState.setText("Not initialized");
                tvSdkState.setTextColor(Color.RED);
            }
        } catch (Exception e) {
            tvSdkState.setText("Error: " + e.getMessage());
            tvSdkState.setTextColor(Color.RED);
        }

        appendLog("ReceiverActivity started");
        appendLog("Robot IP: " + (ip != null ? ip : "Unknown"));
        appendLog("WebSocket port: " + WebSocketServerService.WS_PORT);
    }

    // =========================================================================
    // Service Lifecycle
    // =========================================================================

    private void startReceiverService() {
        if (isServiceStarted) {
            return;
        }
        Log.i(LOG_INFO, "Starting WebSocketServerService");
        Intent intent = new Intent(this, WebSocketServerService.class);
        intent.setAction(WebSocketServerService.ACTION_START_SERVER);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }

        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
        isServiceStarted = true;
        btnStartStop.setText("Stop Server");
        appendLog("Starting WebSocket server...");
    }

    private void stopReceiverService() {
        Log.i(LOG_INFO, "Stopping WebSocketServerService");

        if (isBound) {
            try {
                unbindService(serviceConnection);
            } catch (IllegalArgumentException e) {
                Log.w(LOG_ERROR, "Service already unbound");
            }
            isBound = false;
        }

        Intent intent = new Intent(this, WebSocketServerService.class);
        intent.setAction(WebSocketServerService.ACTION_STOP_SERVER);
        // Deliver stop request to service so it can cleanup safely.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
        isServiceStarted = false;

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                btnStartStop.setText("Start Server");
                tvServerStatus.setText("Stopped");
                tvServerStatus.setTextColor(Color.RED);
                tvClientStatus.setText("No client");
                tvClientStatus.setTextColor(Color.GRAY);
                tvCurrentCommand.setText("NONE");
                tvCurrentCommand.setTextColor(Color.GRAY);
            }
        });

        appendLog("Server stopped");
    }

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            if (service instanceof WebSocketServerService.LocalBinder) {
                WebSocketServerService.LocalBinder binder = (WebSocketServerService.LocalBinder) service;
                wsService = binder.getService();
                isBound = true;
                Log.i(LOG_INFO, "Bound to WebSocketServerService");
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            wsService = null;
            isBound = false;
            Log.i(LOG_INFO, "Unbound from WebSocketServerService");
        }
    };

    // =========================================================================
    // Broadcast Receivers & Data Listeners
    // =========================================================================

    private final SensorDataManager.SensorDataListener sensorDataListener = new SensorDataManager.SensorDataListener() {
        @Override
        public void onSensorDataUpdated(final String jsonPayload) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        org.json.JSONObject payload = new org.json.JSONObject(jsonPayload);
                        org.json.JSONObject data = payload.optJSONObject("data");
                        org.json.JSONObject meta = payload.optJSONObject("meta");
                        if (data != null) {
                            String batteryStr = formatBattery(data.optInt("battery_percent", -1));
                            double x = data.optDouble("position_x", Double.NaN);
                            double y = data.optDouble("position_y", Double.NaN);
                            String posStr = formatPosition(x, y);

                            int motorState = data.optInt("motor_state", -1);
                            String controllerStr = formatMotorControllerState(motorState);
                            String motionStr = estimateMotionState(lastCommandDirection, x, y);
                            String motorStr = controllerStr + " | Motion: " + motionStr;
                            String sonarStr = formatSonar(data.opt("sonar_distance"));
                            String collisionStr = formatCollision(data.optString("collision_state", ""));

                            tvDiagBattery.setText("Battery: " + batteryStr);
                            tvDiagPosition.setText("Position: " + posStr);
                            tvDiagMotor.setText("Motor: " + motorStr);
                            tvDiagSonar.setText("Sonar: " + sonarStr);
                            tvDiagCollision.setText("Collision: " + collisionStr);
                        }

                        if (tvDiagTopicHeartbeat != null) {
                            tvDiagTopicHeartbeat.setText(formatHeartbeatPanel(meta));
                        }
                    } catch (Exception e) {
                        Log.e(LOG_ERROR, "Error parsing sensor data for UI", e);
                    }
                }
            });
        }
    };

    private void registerReceivers() {
        if (receiversRegistered) {
            return;
        }
        LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(this);
        lbm.registerReceiver(serverStatusReceiver, new IntentFilter(WebSocketServerService.ACTION_SERVER_STATUS));
        lbm.registerReceiver(clientStatusReceiver, new IntentFilter(WebSocketServerService.ACTION_CLIENT_STATUS));
        lbm.registerReceiver(commandReceiver, new IntentFilter(WebSocketServerService.ACTION_COMMAND));
        lbm.registerReceiver(logReceiver, new IntentFilter(WebSocketServerService.ACTION_LOG));
        receiversRegistered = true;
    }

    private void unregisterReceivers() {
        if (!receiversRegistered) {
            return;
        }
        LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(this);
        lbm.unregisterReceiver(serverStatusReceiver);
        lbm.unregisterReceiver(clientStatusReceiver);
        lbm.unregisterReceiver(commandReceiver);
        lbm.unregisterReceiver(logReceiver);
        receiversRegistered = false;
    }

    private final BroadcastReceiver serverStatusReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final boolean running = intent.getBooleanExtra(WebSocketServerService.EXTRA_STATUS, false);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (running) {
                        tvServerStatus.setText("Running (port " + WebSocketServerService.WS_PORT + ")");
                        tvServerStatus.setTextColor(Color.GREEN);
                    } else {
                        tvServerStatus.setText("Stopped");
                        tvServerStatus.setTextColor(Color.RED);
                    }
                }
            });
        }
    };

    private final BroadcastReceiver clientStatusReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final boolean connected = intent.getBooleanExtra(WebSocketServerService.EXTRA_STATUS, false);
            final String message = intent.getStringExtra(WebSocketServerService.EXTRA_MESSAGE);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (connected) {
                        tvClientStatus.setText("Connected");
                        tvClientStatus.setTextColor(Color.GREEN);
                    } else {
                        tvClientStatus.setText("Disconnected");
                        tvClientStatus.setTextColor(Color.RED);
                        tvCurrentCommand.setText("NONE");
                        tvCurrentCommand.setTextColor(Color.GRAY);
                    }
                }
            });
            appendLog(message != null ? message : (connected ? "Client connected" : "Client disconnected"));
        }
    };

    private final BroadcastReceiver commandReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String rawCommand = intent.getStringExtra(WebSocketServerService.EXTRA_COMMAND);
            final int direction = intent.getIntExtra(WebSocketServerService.EXTRA_DIRECTION, CommandParser.COMMAND_UNKNOWN);
            final String dirName = CommandParser.directionToString(direction);

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    tvCurrentCommand.setText(dirName);
                    if (direction == CommandParser.COMMAND_STOP) {
                        tvCurrentCommand.setTextColor(Color.YELLOW);
                    } else if (direction == CommandParser.COMMAND_UNKNOWN) {
                        tvCurrentCommand.setTextColor(Color.RED);
                    } else {
                        tvCurrentCommand.setTextColor(Color.CYAN);
                    }
                }
            });
            lastCommandDirection = direction;
            if (rawCommand != null) {
                appendLog("Command received: " + rawCommand);
            }
        }
    };

    private final BroadcastReceiver logReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String message = intent.getStringExtra(WebSocketServerService.EXTRA_MESSAGE);
            if (message != null) {
                appendLog(message);
            }
        }
    };

    // =========================================================================
    // Helpers
    // =========================================================================

    private void appendLog(final String message) {
        final String timestamp = sdf.format(new Date());
        final String line = "[" + timestamp + "] " + message;
        Log.d(TAG, line);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                logBuilder.append(line).append("\n");
                logLineCount++;
                if (logLineCount > MAX_LOG_LINES) {
                    int firstLineEnd = logBuilder.indexOf("\n");
                    if (firstLineEnd >= 0) {
                        logBuilder.delete(0, firstLineEnd + 1);
                        logLineCount--;
                    }
                }
                tvLog.setText(logBuilder.toString());
                svLog.post(new Runnable() {
                    @Override
                    public void run() {
                        svLog.fullScroll(View.FOCUS_DOWN);
                    }
                });
            }
        });
    }

    /**
     * Get the device's WiFi IP address.
     */
    private String getDeviceIpAddress() {
        try {
            WifiManager wm = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            if (wm != null) {
                WifiInfo wifiInfo = wm.getConnectionInfo();
                int ipInt = wifiInfo.getIpAddress();
                if (ipInt != 0) {
                    return String.format(Locale.US, "%d.%d.%d.%d",
                            (ipInt & 0xff),
                            (ipInt >> 8 & 0xff),
                            (ipInt >> 16 & 0xff),
                            (ipInt >> 24 & 0xff));
                }
            }
        } catch (Exception e) {
            Log.e(LOG_ERROR, "Error getting IP", e);
        }
        return null;
    }

    private String formatBattery(int batteryPercent) {
        return batteryPercent >= 0 ? batteryPercent + "%" : "Unavailable";
    }

    private String formatPosition(double x, double y) {
        if (Double.isFinite(x) && Double.isFinite(y)) {
            return String.format(Locale.getDefault(), "X: %.2f, Y: %.2f", x, y);
        }
        return "Unavailable";
    }

    private String formatMotor(int motorState) {
        // Deprecated: physical movement must NOT be inferred from motor_state.
        return formatMotorControllerState(motorState);
    }

    private String formatMotorControllerState(int motorState) {
        if (motorState < 0) {
            return "Unknown";
        }
        // Treat non-zero as "Active" (controller engaged), not physical motion.
        if (motorState == 0) {
            return "Ready";
        }
        return "Active";
    }

    private String estimateMotionState(int lastDirection, double x, double y) {
        if (lastDirection == CommandParser.COMMAND_STOP) {
            return "Stopped";
        }
        if (!Double.isFinite(x) || !Double.isFinite(y)) {
            return "Unconfirmed";
        }

        long now = android.os.SystemClock.elapsedRealtime();
        if (Double.isFinite(lastPosX) && Double.isFinite(lastPosY) && lastPosTimeMs > 0L) {
            double dx = x - lastPosX;
            double dy = y - lastPosY;
            double dist = Math.sqrt(dx * dx + dy * dy);
            long dt = now - lastPosTimeMs;
            if (dt <= MOTION_WINDOW_MS && dist >= MOTION_DISTANCE_THRESHOLD) {
                lastPosX = x;
                lastPosY = y;
                lastPosTimeMs = now;
                return "Moving";
            }
        }

        // Update baseline even when not moving (for future deltas)
        lastPosX = x;
        lastPosY = y;
        lastPosTimeMs = now;
        return "Unconfirmed";
    }

    private String formatSonar(Object sonarValue) {
        if (sonarValue instanceof Number) {
            double value = ((Number) sonarValue).doubleValue();
            if (Double.isFinite(value) && value >= 0d) {
                return String.format(Locale.getDefault(), "%.2f", value);
            }
            return "No reading";
        }
        if (sonarValue != null) {
            String s = String.valueOf(sonarValue).trim();
            if (!s.isEmpty() && !"N/A".equalsIgnoreCase(s) && !"null".equalsIgnoreCase(s)) {
                return s;
            }
        }
        return "No reading";
    }

    private String formatCollision(String collisionRaw) {
        if ("YES".equalsIgnoreCase(collisionRaw) || "true".equalsIgnoreCase(collisionRaw)) {
            return "Detected";
        } else if ("NO".equalsIgnoreCase(collisionRaw) || "false".equalsIgnoreCase(collisionRaw)) {
            return "Clear";
        }
        return "Unknown";
    }

    private String formatHeartbeatPanel(org.json.JSONObject meta) {
        if (meta == null) {
            return "POS: --\nNAV: --\nSONAR: --";
        }
        return "POS: " + formatHeartbeatLine(meta.optJSONObject("hb_position")) + "\n"
                + "NAV: " + formatHeartbeatLine(meta.optJSONObject("hb_navigation")) + "\n"
                + "SONAR: " + formatHeartbeatLine(meta.optJSONObject("hb_sonar"));
    }

    private String formatHeartbeatLine(org.json.JSONObject hb) {
        if (hb == null) {
            return "--";
        }
        long lastUpdate = hb.optLong("last_update_ms", 0L);
        long lastChange = hb.optLong("last_change_ms", 0L);
        double ups = hb.optDouble("ups", 0d);

        String sinceUpdate = lastUpdate > 0 ? (android.os.SystemClock.elapsedRealtime() - lastUpdate) + "ms ago" : "--";
        String changing = (lastChange > 0 && lastUpdate > 0 && lastChange == lastUpdate) ? "changing" : "stable";

        // Keep compact; avoid flooding UI with decimals
        String upsStr = ups > 0 ? String.format(java.util.Locale.getDefault(), "%.1f/s", ups) : "0.0/s";
        return sinceUpdate + ", " + changing + ", " + upsStr;
    }
}
