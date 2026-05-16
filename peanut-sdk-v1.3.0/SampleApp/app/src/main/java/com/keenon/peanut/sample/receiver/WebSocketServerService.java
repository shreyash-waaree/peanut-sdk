package com.keenon.peanut.sample.receiver;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;
import java.util.Collection;

/**
 * Background service running a WebSocket server on port 8765.
 * Receives movement commands from a remote phone client and dispatches
 * them to RobotMovementManager. Also handles RemoteController camera commands
 * (CAMERA_INFO / CAMERA_START / CAMERA_SWITCH / CAMERA_STOP) via {@link RobotCameraStreamer}.
 *
 * Broadcasts status updates via LocalBroadcastManager:
 *   ACTION_SERVER_STATUS  — server started/stopped
 *   ACTION_CLIENT_STATUS  — client connected/disconnected
 *   ACTION_COMMAND        — command received
 *   ACTION_LOG            — log messages
 */
public class WebSocketServerService extends Service {

    private static final String TAG = "RobotReceiver";
    private static final String LOG_INFO = "INFO";
    private static final String LOG_ERROR = "ERROR";
    private static final String LOG_COMMAND = "COMMAND";

    public static final int WS_PORT = 8765;
    public static final String CHANNEL_ID = "robot_receiver_channel";

    // Service control actions (explicit ownership; UI is optional)
    public static final String ACTION_START_SERVER = "com.keenon.peanut.sample.receiver.action.START_SERVER";
    public static final String ACTION_STOP_SERVER = "com.keenon.peanut.sample.receiver.action.STOP_SERVER";

    // Broadcast actions
    public static final String ACTION_SERVER_STATUS = "com.keenon.peanut.sample.receiver.SERVER_STATUS";
    public static final String ACTION_CLIENT_STATUS = "com.keenon.peanut.sample.receiver.CLIENT_STATUS";
    public static final String ACTION_COMMAND = "com.keenon.peanut.sample.receiver.COMMAND";
    public static final String ACTION_LOG = "com.keenon.peanut.sample.receiver.LOG";

    // Broadcast extras
    public static final String EXTRA_STATUS = "status";
    public static final String EXTRA_MESSAGE = "message";
    public static final String EXTRA_COMMAND = "command";
    public static final String EXTRA_DIRECTION = "direction";

    private RobotWebSocketServer wsServer;
    private RobotCameraStreamer cameraStreamer;
    private volatile boolean isServerRunning = false;
    private volatile boolean isClientConnected = false;
    private final IBinder binder = new LocalBinder();

    public class LocalBinder extends Binder {
        public WebSocketServerService getService() {
            return WebSocketServerService.this;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "WebSocketServerService created");
        createNotificationChannel();

        // Ensure PeanutSDK is initialized even when UI is not visible.
        SdkBootstrap.ensureInit(getApplicationContext(), errorCode -> {
            broadcastLog("SDK init: " + errorCode);
        });

        cameraStreamer = new RobotCameraStreamer();

        SensorDataManager.getInstance().init();
        SensorDataManager.getInstance().addListener(sensorDataListener);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "WebSocketServerService onStartCommand");
        startForegroundNotification();

        String action = intent != null ? intent.getAction() : null;
        if (ACTION_STOP_SERVER.equals(action)) {
            broadcastLog("Stopping server (requested)");
            stopWebSocketServer();
            RobotMovementManager.getInstance().release();
            SensorDataManager.getInstance().release();
            stopForeground(true);
            stopSelf();
            return START_NOT_STICKY;
        }

        // Default: start (also covers null intent on START_STICKY restart)
        startWebSocketServer();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "WebSocketServerService destroying");
        SensorDataManager.getInstance().removeListener(sensorDataListener);
        stopWebSocketServer();
        if (cameraStreamer != null) {
            cameraStreamer.release();
            cameraStreamer = null;
        }
        // Service owns movement lifecycle; ensure motors are safely locked & mode restored.
        RobotMovementManager.getInstance().release();
        super.onDestroy();
    }

    // =========================================================================
    // WebSocket Server Management
    // =========================================================================

    private void startWebSocketServer() {
        if (isServerRunning) {
            Log.i(LOG_INFO, "Server already running, skipping start");
            return;
        }

        try {
            wsServer = new RobotWebSocketServer(WS_PORT);
            wsServer.setReuseAddr(true);
            wsServer.start();
            isServerRunning = true;
            Log.i(LOG_INFO, "WebSocket server started on port " + WS_PORT);
            broadcastServerStatus(true, "Server started on port " + WS_PORT);
            broadcastLog("WebSocket server started on port " + WS_PORT);
        } catch (Exception e) {
            Log.e(LOG_ERROR, "Failed to start WebSocket server", e);
            isServerRunning = false;
            broadcastServerStatus(false, "Failed to start: " + e.getMessage());
            broadcastLog("ERROR: Server start failed — " + e.getMessage());
        }
    }

    private void stopWebSocketServer() {
        if (cameraStreamer != null) {
            cameraStreamer.stop(getApplicationContext());
        }
        if (wsServer != null) {
            try {
                // Stop movement immediately on server shutdown
                RobotMovementManager.getInstance().stop();

                wsServer.stop(500);  // 500ms timeout for clean shutdown
                Log.i(LOG_INFO, "WebSocket server stopped");
            } catch (InterruptedException e) {
                Log.e(LOG_ERROR, "Error stopping WebSocket server", e);
                Thread.currentThread().interrupt();
            }
            wsServer = null;
        }
        isServerRunning = false;
        isClientConnected = false;
        broadcastServerStatus(false, "Server stopped");
    }

    // =========================================================================
    // Notification (required for foreground service on API 26+)
    // =========================================================================

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Robot Receiver Service",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Keeps the WebSocket server running for remote control");
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) {
                nm.createNotificationChannel(channel);
            }
        }
    }

    private void startForegroundNotification() {
        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(this, CHANNEL_ID);
        } else {
            builder = new Notification.Builder(this);
        }

        Notification notification = builder
                .setContentTitle("Robot Remote Receiver")
                .setContentText("WebSocket server running on port " + WS_PORT)
                .setSmallIcon(android.R.drawable.ic_menu_compass)
                .setOngoing(true)
                .build();

        startForeground(1001, notification);
    }

    // =========================================================================
    // Broadcast Helpers
    // =========================================================================

    private void broadcastServerStatus(boolean running, String message) {
        Intent intent = new Intent(ACTION_SERVER_STATUS);
        intent.putExtra(EXTRA_STATUS, running);
        intent.putExtra(EXTRA_MESSAGE, message);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void broadcastClientStatus(boolean connected, String message) {
        Intent intent = new Intent(ACTION_CLIENT_STATUS);
        intent.putExtra(EXTRA_STATUS, connected);
        intent.putExtra(EXTRA_MESSAGE, message);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void broadcastCommand(String rawCommand, int direction) {
        Intent intent = new Intent(ACTION_COMMAND);
        intent.putExtra(EXTRA_COMMAND, rawCommand);
        intent.putExtra(EXTRA_DIRECTION, direction);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void broadcastLog(String message) {
        Intent intent = new Intent(ACTION_LOG);
        intent.putExtra(EXTRA_MESSAGE, message);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    // =========================================================================
    // Public Accessors
    // =========================================================================

    public boolean isServerRunning() {
        return isServerRunning;
    }

    public boolean isClientConnected() {
        return isClientConnected;
    }

    private boolean anyWebSocketConnections() {
        try {
            if (wsServer == null) {
                return false;
            }
            Collection<WebSocket> conns = wsServer.getConnections();
            return conns != null && !conns.isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * RemoteController camera protocol (same port as motion). Returns true if handled.
     */
    private boolean handleCameraMessage(WebSocket conn, String normalized) {
        if (cameraStreamer == null) {
            return false;
        }
        if ("CAMERA_INFO".equals(normalized)) {
            int n = cameraStreamer.getCameraCount();
            safeSend(conn, "CAMERA_INFO:count=" + n);
            broadcastLog("CAMERA_INFO → count=" + n);
            return true;
        }
        if (normalized.startsWith("CAMERA_START:")) {
            int id = parseTrailingInt(normalized, "CAMERA_START:".length());
            if (id < 0) {
                safeSend(conn, "ERR:CAMERA:INVALID_ID");
                return true;
            }
            cameraStreamer.startCamera(getApplicationContext(), id, conn, false);
            return true;
        }
        if (normalized.startsWith("CAMERA_SWITCH:")) {
            int id = parseTrailingInt(normalized, "CAMERA_SWITCH:".length());
            if (id < 0) {
                safeSend(conn, "ERR:CAMERA:INVALID_ID");
                return true;
            }
            cameraStreamer.startCamera(getApplicationContext(), id, conn, true);
            return true;
        }
        if ("CAMERA_STOP".equals(normalized)) {
            cameraStreamer.stop(getApplicationContext());
            safeSend(conn, "ACK:CAMERA_STOP");
            return true;
        }
        return false;
    }

    private static int parseTrailingInt(String normalized, int startIndex) {
        try {
            return Integer.parseInt(normalized.substring(startIndex).trim());
        } catch (Exception e) {
            return -1;
        }
    }

    // =========================================================================
    // Sensor Data Listener
    // =========================================================================

    private final SensorDataManager.SensorDataListener sensorDataListener = new SensorDataManager.SensorDataListener() {
        @Override
        public void onSensorDataUpdated(String jsonPayload) {
            // Push to all connected WebSocket clients efficiently
            if (isServerRunning && wsServer != null && anyWebSocketConnections()) {
                wsServer.broadcast(jsonPayload);
            }
        }
    };

    // =========================================================================
    // Inner WebSocket Server
    // =========================================================================

    private class RobotWebSocketServer extends WebSocketServer {

        public RobotWebSocketServer(int port) {
            super(new InetSocketAddress(port));
        }

        @Override
        public void onOpen(WebSocket conn, ClientHandshake handshake) {
            String clientAddr = conn != null && conn.getRemoteSocketAddress() != null
                    ? conn.getRemoteSocketAddress().toString() : "unknown";
            Log.i(LOG_INFO, "Client connected: " + clientAddr);
            isClientConnected = true;

            // Prepare movement when the first client connects (extra clients e.g. camera use another socket)
            if (getConnections().size() == 1) {
                RobotMovementManager.getInstance().prepare();
            }

            broadcastClientStatus(true, "Phone connected: " + clientAddr);
            broadcastLog("Client connected: " + clientAddr);

            // Send welcome acknowledgment to client
            safeSend(conn, "CONNECTED:ROBOT_RECEIVER_READY");
        }

        @Override
        public void onClose(WebSocket conn, int code, String reason, boolean remote) {
            if (cameraStreamer != null) {
                cameraStreamer.onWebSocketClosed(conn);
            }
            String clientAddr = conn != null && conn.getRemoteSocketAddress() != null
                    ? conn.getRemoteSocketAddress().toString() : "unknown";
            Log.i(LOG_INFO, "Client disconnected: " + clientAddr + " reason: " + reason);

            int remaining = getConnections().size();
            if (remaining == 0) {
                isClientConnected = false;
                RobotMovementManager.getInstance().stop();
                broadcastClientStatus(false, "Phone disconnected" + (reason != null ? ": " + reason : ""));
                broadcastLog("Client disconnected — movement stopped (no clients left)");
            } else {
                isClientConnected = true;
                broadcastLog("Client disconnected — " + remaining + " connection(s) still active");
            }
        }

        @Override
        public void onMessage(WebSocket conn, String message) {
            if (message == null) {
                broadcastLog("Received empty command");
                safeSend(conn, "ERR:EMPTY_COMMAND");
                return;
            }
            String normalized = message.trim();
            if (normalized.isEmpty()) {
                broadcastLog("Received blank command");
                safeSend(conn, "ERR:EMPTY_COMMAND");
                return;
            }
            Log.i(LOG_COMMAND, "Received command: " + normalized);

            if (handleCameraMessage(conn, normalized)) {
                return;
            }

            int direction = CommandParser.parse(normalized);
            broadcastCommand(normalized, direction);

            if (direction == CommandParser.COMMAND_STOP) {
                Log.i(LOG_COMMAND, "STOP command received");
                RobotMovementManager.getInstance().stop();
                broadcastLog("Command: STOP — movement halted");
                safeSend(conn, "ACK:STOP");

            } else if (direction == CommandParser.COMMAND_UNKNOWN) {
                Log.w(LOG_COMMAND, "Unknown command: " + normalized);
                broadcastLog("Unknown command: " + normalized);
                safeSend(conn, "ERR:UNKNOWN_COMMAND:" + normalized);

            } else {
                String dirName = CommandParser.directionToString(direction);
                Log.i(LOG_COMMAND, "Executing: " + dirName);
                RobotMovementManager.getInstance().executeDirection(direction);
                broadcastLog("Command: " + dirName + " — executing");
                safeSend(conn, "ACK:" + dirName);
            }
        }

        @Override
        public void onError(WebSocket conn, Exception ex) {
            Log.e(LOG_ERROR, "WebSocket error", ex);
            broadcastLog("WebSocket error: " + ex.getMessage());

            // Safety: stop movement on any error
            RobotMovementManager.getInstance().stop();
        }

        @Override
        public void onStart() {
            Log.i(LOG_INFO, "WebSocket server onStart callback");
        }
    }

    private void safeSend(WebSocket conn, String payload) {
        if (conn == null || payload == null) {
            return;
        }
        try {
            if (conn.isOpen()) {
                conn.send(payload);
            }
        } catch (Exception e) {
            Log.e(LOG_ERROR, "Failed to send websocket payload", e);
        }
    }
}
