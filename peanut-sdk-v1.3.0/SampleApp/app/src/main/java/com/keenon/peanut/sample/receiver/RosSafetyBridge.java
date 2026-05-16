package com.keenon.peanut.sample.receiver;

import android.util.Base64;
import android.util.Log;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Alternative source for the short-range / safety sensor stack: subscribes to
 * three rosbridge topics on the algorithm board and converts each into the
 * shape the SampleApp's IMU/sonar/collision/bump cards expect.
 *
 * <p>Topics observed live on this robot:
 * <ul>
 *   <li>{@code /bump} ({@code sensor_msgs/PointCloud2}) — 28 contact-sensor
 *       positions per cycle. Each point carries its sensor location (x, y) and
 *       a {@code z} that acts as a contact flag: the driver reports
 *       {@code z = -10.0} as a "no contact" sentinel. We treat any point
 *       with {@code z > BUMP_CONTACT_Z_THRESHOLD} as a real bump.</li>
 *   <li>{@code /fdl/obs_low} ({@code sensor_msgs/PointCloud2}) — fused short-
 *       range obstacle cloud (the role sonar would play on a simpler robot).
 *       Each point is an obstacle in {@code base_link}; we take the minimum
 *       horizontal distance as the "sonar reading".</li>
 *   <li>{@code /SDS_collision_points} ({@code sensor_msgs/PointCloud2}) —
 *       collision-check output. Only publishes during near-collision events;
 *       a publish within the last few seconds means {@code collision = YES}.</li>
 * </ul>
 */
public class RosSafetyBridge {

  private static final String TAG = "RosSafetyBridge";

  public static final String DEFAULT_URI = "ws://192.168.64.20:9090";
  public static final String TOPIC_BUMP = "/bump";
  public static final String TOPIC_OBS_LOW = "/fdl/obs_low";
  public static final String TOPIC_COLLISION = "/SDS_collision_points";

  private static final int DEFAULT_THROTTLE_MS = 200;
  /** Points with z > this are treated as real contacts; below = sentinel. */
  private static final double BUMP_CONTACT_Z_THRESHOLD = -1.0;
  /** A SDS_collision_points publish counts as "collision now" for this long. */
  private static final long COLLISION_HOLD_MS = 1500L;

  /** Latest aggregate state — single immutable snapshot updated on each publish. */
  public static final class Snapshot {
    public final long timestampMs;
    public final boolean bumped;          // any /bump point above contact threshold
    public final int bumpActiveCount;     // how many sensors are currently active
    public final int bumpTotalSensors;    // how many sensors the driver reported
    public final double sonarMinM;        // closest /fdl/obs_low horizontal distance, NaN if none
    public final int obsLowPointCount;
    public final boolean collision;       // /SDS_collision_points received recently
    public final long lastCollisionMs;    // SystemClock.elapsedRealtime of last collision pub

    public Snapshot(long ts, boolean bumped, int bumpActive, int bumpTotal,
                    double sonarMinM, int obsLow, boolean collision, long lastCollMs) {
      this.timestampMs = ts;
      this.bumped = bumped;
      this.bumpActiveCount = bumpActive;
      this.bumpTotalSensors = bumpTotal;
      this.sonarMinM = sonarMinM;
      this.obsLowPointCount = obsLow;
      this.collision = collision;
      this.lastCollisionMs = lastCollMs;
    }

    public static Snapshot empty() {
      return new Snapshot(0L, false, 0, 0, Double.NaN, 0, false, 0L);
    }
  }

  public interface Listener {
    void onSnapshot(Snapshot s);
    void onStatus(String status, boolean isError);
  }

  private final String uri;
  private final Listener listener;

  private WebSocketClient client;
  private volatile boolean running = false;

  // Latest per-topic state — combined into a Snapshot on every publish.
  private volatile boolean lastBumped = false;
  private volatile int lastBumpActive = 0;
  private volatile int lastBumpTotal = 0;
  private volatile double lastSonarMinM = Double.NaN;
  private volatile int lastObsLowCount = 0;
  private volatile long lastCollisionMs = 0L;

  public RosSafetyBridge(Listener listener) {
    this(DEFAULT_URI, listener);
  }

  public RosSafetyBridge(String uri, Listener listener) {
    this.uri = uri;
    this.listener = listener;
  }

  public synchronized void start() {
    if (running) return;
    running = true;
    try {
      client = new WebSocketClient(new URI(uri)) {
        @Override public void onOpen(ServerHandshake h) {
          Log.i(TAG, "rosbridge connected");
          emitStatus("connected, subscribing to bump/obs_low/collision", false);
          subscribe(TOPIC_BUMP, "sensor_msgs/PointCloud2");
          subscribe(TOPIC_OBS_LOW, "sensor_msgs/PointCloud2");
          subscribe(TOPIC_COLLISION, "sensor_msgs/PointCloud2");
        }
        @Override public void onMessage(String message) { handle(message); }
        @Override public void onClose(int code, String reason, boolean remote) {
          Log.w(TAG, "rosbridge closed code=" + code + " remote=" + remote);
          emitStatus("closed: " + reason, true);
        }
        @Override public void onError(Exception ex) {
          Log.e(TAG, "rosbridge error", ex);
          emitStatus("error: " + ex.getClass().getSimpleName() + " " + ex.getMessage(), true);
        }

        private void subscribe(String topic, String type) {
          try {
            JSONObject sub = new JSONObject();
            sub.put("op", "subscribe");
            sub.put("topic", topic);
            sub.put("type", type);
            sub.put("throttle_rate", DEFAULT_THROTTLE_MS);
            sub.put("queue_length", 1);
            send(sub.toString());
          } catch (JSONException e) {
            Log.e(TAG, "subscribe " + topic + " failed", e);
          }
        }
      };
      client.setConnectionLostTimeout(30);
      client.connect();
    } catch (URISyntaxException e) {
      running = false;
      emitStatus("bad URI: " + uri, true);
    }
  }

  public synchronized void stop() {
    running = false;
    if (client != null) {
      for (String t : new String[]{TOPIC_BUMP, TOPIC_OBS_LOW, TOPIC_COLLISION}) {
        try {
          if (client.isOpen()) {
            JSONObject u = new JSONObject();
            u.put("op", "unsubscribe");
            u.put("topic", t);
            client.send(u.toString());
          }
        } catch (Exception ignored) { }
      }
      try { client.closeBlocking(); }
      catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
      client = null;
    }
    lastBumped = false;
    lastBumpActive = 0;
    lastBumpTotal = 0;
    lastSonarMinM = Double.NaN;
    lastObsLowCount = 0;
    lastCollisionMs = 0L;
  }

  public boolean isRunning() { return running; }

  private void emitStatus(String msg, boolean err) {
    if (listener != null) {
      try { listener.onStatus(msg, err); } catch (Exception ignored) { }
    }
  }

  private void handle(String raw) {
    if (raw == null || raw.isEmpty()) return;
    try {
      JSONObject obj = new JSONObject(raw);
      String op = obj.optString("op");
      if ("status".equals(op)) {
        emitStatus(obj.optString("level") + ": " + obj.optString("msg"),
                "error".equalsIgnoreCase(obj.optString("level")));
        return;
      }
      if (!"publish".equals(op)) return;
      String topic = obj.optString("topic");
      JSONObject msg = obj.optJSONObject("msg");
      if (msg == null) return;

      switch (topic) {
        case TOPIC_BUMP:        parseBump(msg);      break;
        case TOPIC_OBS_LOW:     parseObsLow(msg);    break;
        case TOPIC_COLLISION:   parseCollision(msg); break;
        default: return;
      }

      Snapshot snap = new Snapshot(
              android.os.SystemClock.elapsedRealtime(),
              lastBumped, lastBumpActive, lastBumpTotal,
              lastSonarMinM, lastObsLowCount,
              isCollisionLive(), lastCollisionMs);
      if (listener != null) listener.onSnapshot(snap);
    } catch (JSONException e) {
      Log.w(TAG, "non-JSON rosbridge frame", e);
    } catch (Exception e) {
      Log.w(TAG, "safety parse failed", e);
    }
  }

  private boolean isCollisionLive() {
    if (lastCollisionMs == 0L) return false;
    long age = android.os.SystemClock.elapsedRealtime() - lastCollisionMs;
    return age >= 0L && age < COLLISION_HOLD_MS;
  }

  /**
   * Decode the binary point payload of a PointCloud2 message. Returns the
   * unpacked points as a flat {@code [x0, y0, z0, x1, y1, z1, …]} array.
   * Non-finite or zero-length payloads return an empty array.
   */
  private static float[] decodePoints(JSONObject msg) {
    String b64 = msg.optString("data", "");
    if (b64.isEmpty()) return new float[0];
    int width = msg.optInt("width", 0);
    int height = msg.optInt("height", 1);
    int pointStep = msg.optInt("point_step", 0);
    int n = Math.max(0, width * height);
    if (n == 0 || pointStep < 12) return new float[0];

    byte[] raw;
    try {
      raw = Base64.decode(b64, Base64.DEFAULT);
    } catch (IllegalArgumentException e) {
      return new float[0];
    }
    if (raw.length < n * pointStep) {
      n = raw.length / pointStep;
    }
    boolean bigEndian = msg.optBoolean("is_bigendian", false);
    ByteBuffer bb = ByteBuffer.wrap(raw)
            .order(bigEndian ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN);

    // Find x/y/z offsets from the fields[] array (default to 0/4/8 if missing).
    int offX = 0, offY = 4, offZ = 8;
    JSONArray fields = msg.optJSONArray("fields");
    if (fields != null) {
      for (int i = 0; i < fields.length(); i++) {
        JSONObject f = fields.optJSONObject(i);
        if (f == null) continue;
        String name = f.optString("name", "");
        int off = f.optInt("offset", -1);
        if ("x".equals(name) && off >= 0) offX = off;
        else if ("y".equals(name) && off >= 0) offY = off;
        else if ("z".equals(name) && off >= 0) offZ = off;
      }
    }

    float[] out = new float[n * 3];
    for (int i = 0; i < n; i++) {
      int base = i * pointStep;
      out[i * 3]     = bb.getFloat(base + offX);
      out[i * 3 + 1] = bb.getFloat(base + offY);
      out[i * 3 + 2] = bb.getFloat(base + offZ);
    }
    return out;
  }

  private void parseBump(JSONObject msg) {
    float[] pts = decodePoints(msg);
    int total = pts.length / 3;
    int active = 0;
    for (int i = 0; i < total; i++) {
      double z = pts[i * 3 + 2];
      if (Double.isFinite(z) && z > BUMP_CONTACT_Z_THRESHOLD) active++;
    }
    lastBumpTotal = total;
    lastBumpActive = active;
    lastBumped = active > 0;
  }

  private void parseObsLow(JSONObject msg) {
    float[] pts = decodePoints(msg);
    int n = pts.length / 3;
    double minH = Double.POSITIVE_INFINITY;
    for (int i = 0; i < n; i++) {
      double x = pts[i * 3];
      double y = pts[i * 3 + 1];
      double d = Math.hypot(x, y);
      if (Double.isFinite(d) && d > 0d && d < minH) minH = d;
    }
    lastObsLowCount = n;
    lastSonarMinM = (minH == Double.POSITIVE_INFINITY) ? Double.NaN : minH;
  }

  private void parseCollision(JSONObject msg) {
    float[] pts = decodePoints(msg);
    if (pts.length > 0) {
      lastCollisionMs = android.os.SystemClock.elapsedRealtime();
    }
  }
}
