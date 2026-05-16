package com.keenon.peanut.supermarket.stream;

import android.util.Base64;
import android.util.Log;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Collection;

/**
 * Tiny WebSocket server running on the robot (port 8765).
 *
 * Clients (the PC dashboard) connect and receive JSON messages:
 *
 *   { "type": "frame",
 *     "cam": 0,
 *     "jpeg": "<base64>",
 *     "detections": [ { "label":"cell phone","score":0.87,"cx":0.45 }, ... ],
 *     "fps": 3.2,
 *     "ms": 120 }
 *
 * Only the latest frame is broadcast — if the client is slow, frames are dropped,
 * never queued. This prevents memory growth on low-RAM robots.
 *
 * Usage:
 *   server = new DetectionStreamServer(8765);
 *   server.start();
 *   ...
 *   server.pushFrame(camId, jpegBytes, detectionsJson, fps, ms);
 *   ...
 *   server.stop();
 */
public class DetectionStreamServer extends WebSocketServer {

    private static final String TAG = "StreamServer";

    // Limit broadcast rate — no point sending faster than the browser renders.
    private static final long MIN_BROADCAST_INTERVAL_MS = 200; // ~5 fps max to PC

    private volatile long lastBroadcastAtMs = 0;

    public DetectionStreamServer(int port) {
        // Bind all IPv4 interfaces (explicit) — some builds behave oddly with one-arg
        // InetSocketAddress(port); USB / Wi‑Fi hotspot debugging expects 0.0.0.0:8765.
        super(new InetSocketAddress(inet4Wildcard(), port));
        setReuseAddr(true);
        setConnectionLostTimeout(30);
    }

    private static InetAddress inet4Wildcard() {
        try {
            return InetAddress.getByName("0.0.0.0");
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        Log.i(TAG, "PC dashboard connected: " + conn.getRemoteSocketAddress());
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        Log.i(TAG, "PC dashboard disconnected: " + reason);
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        // Dashboard can send {"type":"ping"} — ignore for now.
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        Log.w(TAG, "WebSocket error", ex);
    }

    @Override
    public void onStart() {
        Log.i(TAG, "DetectionStreamServer listening on port " + getPort());
    }

    /**
     * Called from the detect thread after each inference pass.
     *
     * @param camId          active camera index
     * @param jpeg           raw JPEG bytes of the frame (may be null if no frame available)
     * @param detectionsJson JSON array string, e.g. [{"label":"cup","score":0.9,"cx":0.5}]
     * @param fps            current inference fps
     * @param inferenceMs    last inference latency in ms
     */
    public void pushFrame(int camId, byte[] jpeg, String detectionsJson, float fps, long inferenceMs) {
        Collection<WebSocket> clients = getConnections();
        if (clients.isEmpty()) return;

        long now = System.currentTimeMillis();
        if (now - lastBroadcastAtMs < MIN_BROADCAST_INTERVAL_MS) return;
        lastBroadcastAtMs = now;

        try {
            StringBuilder sb = new StringBuilder(jpeg != null ? jpeg.length * 4 / 3 + 256 : 256);
            sb.append("{\"type\":\"frame\",\"cam\":").append(camId);
            sb.append(",\"fps\":").append(String.format("%.1f", fps));
            sb.append(",\"ms\":").append(inferenceMs);
            sb.append(",\"detections\":").append(detectionsJson != null ? detectionsJson : "[]");
            if (jpeg != null && jpeg.length > 0) {
                sb.append(",\"jpeg\":\"");
                sb.append(Base64.encodeToString(jpeg, Base64.NO_WRAP));
                sb.append("\"");
            }
            sb.append("}");
            String msg = sb.toString();
            for (WebSocket ws : clients) {
                try {
                    if (ws.isOpen()) ws.send(msg);
                } catch (Exception ignored) {}
            }
        } catch (Throwable t) {
            Log.w(TAG, "pushFrame failed", t);
        }
    }

    /** Check if at least one PC browser is watching. */
    public boolean hasClients() {
        return !getConnections().isEmpty();
    }
}
