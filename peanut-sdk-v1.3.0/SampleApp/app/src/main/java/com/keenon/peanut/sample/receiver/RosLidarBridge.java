package com.keenon.peanut.sample.receiver;

import android.util.Log;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Alternative LiDAR source: subscribes to the robot's rosbridge WebSocket and converts
 * each {@code sensor_msgs/LaserScan} publish into a {@link LidarDecoder.Scan}.
 *
 * <p>Used when the native {@code /sensor/lidar} CoAP endpoint returns empty payloads
 * (e.g. the main Peanut service isn't running). The rosbridge topic {@code /scan} is
 * already populated by the ROS stack on the robot's algorithm board at
 * {@code ws://192.168.64.20:9090}, so we can read the same physical sensor through
 * a different transport and feed the existing radar UI.
 */
public class RosLidarBridge {

  private static final String TAG = "RosLidarBridge";
  public static final String DEFAULT_URI = "ws://192.168.64.20:9090";
  public static final String DEFAULT_TOPIC = "/scan";
  /** Rosbridge throttle in ms between publishes (UI only needs ~5 Hz). */
  private static final int DEFAULT_THROTTLE_MS = 200;

  public interface Listener {
    void onScan(LidarDecoder.Scan scan);
    void onStatus(String status, boolean isError);
  }

  private final String uri;
  private final String topic;
  private final Listener listener;

  private WebSocketClient client;
  private volatile boolean running = false;

  public RosLidarBridge(Listener listener) {
    this(DEFAULT_URI, DEFAULT_TOPIC, listener);
  }

  public RosLidarBridge(String uri, String topic, Listener listener) {
    this.uri = uri;
    this.topic = topic;
    this.listener = listener;
  }

  public synchronized void start() {
    if (running) return;
    running = true;
    try {
      client = new WebSocketClient(new URI(uri)) {
        @Override
        public void onOpen(ServerHandshake handshakedata) {
          Log.i(TAG, "rosbridge connected, subscribing to " + topic);
          emitStatus("connected, subscribing to " + topic, false);
          try {
            JSONObject sub = new JSONObject();
            sub.put("op", "subscribe");
            sub.put("topic", topic);
            sub.put("type", "sensor_msgs/LaserScan");
            sub.put("throttle_rate", DEFAULT_THROTTLE_MS);
            sub.put("queue_length", 1);
            send(sub.toString());
          } catch (JSONException e) {
            Log.e(TAG, "subscribe build failed", e);
          }
        }

        @Override
        public void onMessage(String message) {
          handleRosbridgeMessage(message);
        }

        @Override
        public void onClose(int code, String reason, boolean remote) {
          Log.w(TAG, "rosbridge closed code=" + code + " remote=" + remote + " reason=" + reason);
          emitStatus("closed: " + reason, true);
        }

        @Override
        public void onError(Exception ex) {
          Log.e(TAG, "rosbridge error", ex);
          emitStatus("error: " + ex.getClass().getSimpleName() + " " + ex.getMessage(), true);
        }
      };
      client.setConnectionLostTimeout(30);
      client.connect();
    } catch (URISyntaxException e) {
      running = false;
      Log.e(TAG, "bad rosbridge URI " + uri, e);
      emitStatus("bad URI: " + uri, true);
    }
  }

  public synchronized void stop() {
    running = false;
    if (client != null) {
      try {
        JSONObject unsub = new JSONObject();
        unsub.put("op", "unsubscribe");
        unsub.put("topic", topic);
        if (client.isOpen()) {
          client.send(unsub.toString());
        }
      } catch (Exception ignored) {
      }
      try {
        client.closeBlocking();
      } catch (InterruptedException ignored) {
        Thread.currentThread().interrupt();
      }
      client = null;
    }
  }

  public boolean isRunning() {
    return running;
  }

  private void emitStatus(String msg, boolean err) {
    if (listener != null) {
      try {
        listener.onStatus(msg, err);
      } catch (Exception ignored) {
      }
    }
  }

  private void handleRosbridgeMessage(String raw) {
    if (raw == null || raw.isEmpty()) return;
    try {
      JSONObject obj = new JSONObject(raw);
      String op = obj.optString("op");
      if ("status".equals(op)) {
        String level = obj.optString("level");
        String msg = obj.optString("msg");
        Log.i(TAG, "rosbridge status " + level + ": " + msg);
        emitStatus(level + ": " + msg, "error".equalsIgnoreCase(level));
        return;
      }
      if (!"publish".equals(op)) return;
      if (!topic.equals(obj.optString("topic"))) return;
      JSONObject msg = obj.optJSONObject("msg");
      if (msg == null) return;
      LidarDecoder.Scan scan = buildScan(msg);
      if (listener != null) {
        listener.onScan(scan);
      }
    } catch (JSONException e) {
      Log.w(TAG, "non-JSON rosbridge frame, dropping", e);
    } catch (Exception e) {
      Log.w(TAG, "scan conversion failed", e);
    }
  }

  /**
   * Build a {@link LidarDecoder.Scan} from a rosbridge {@code sensor_msgs/LaserScan}
   * message. ROS frame convention: +x = robot forward, +y = robot left, angles CCW
   * from +x — matches {@link LidarDecoder} so no remapping is needed.
   */
  private static LidarDecoder.Scan buildScan(JSONObject msg) throws JSONException {
    double angleMin = msg.optDouble("angle_min", 0d);
    double angleInc = msg.optDouble("angle_increment", 0d);
    double rangeMin = msg.optDouble("range_min", 0d);
    double rangeMax = msg.optDouble("range_max", Double.POSITIVE_INFINITY);
    JSONArray ranges = msg.optJSONArray("ranges");
    if (ranges == null || ranges.length() == 0) return LidarDecoder.Scan.empty();

    int n = ranges.length();
    int keepEvery = Math.max(1, (int) Math.ceil(n / (double) LidarDecoder.MAX_POINTS));
    int expected = (n + keepEvery - 1) / keepEvery;
    float[] xs = new float[expected];
    float[] ys = new float[expected];

    double min = Double.POSITIVE_INFINITY;
    double max = Double.NEGATIVE_INFINITY;
    double sum = 0d;
    int kept = 0;
    double closest = Double.POSITIVE_INFINITY;
    double closestBearing = Double.NaN;
    double[] sectors = new double[LidarDecoder.SECTOR_COUNT];
    for (int i = 0; i < sectors.length; i++) sectors[i] = Double.NaN;

    for (int i = 0; i < n; i++) {
      Object v = ranges.opt(i);
      if (!(v instanceof Number)) continue;
      double d = ((Number) v).doubleValue();
      if (!Double.isFinite(d) || d <= 0d) continue;
      if (d < rangeMin || d > rangeMax) continue;

      double a = angleMin + i * angleInc;
      float xm = (float) (Math.cos(a) * d);
      float ym = (float) (Math.sin(a) * d);

      if ((i % keepEvery) == 0 && kept < xs.length) {
        xs[kept] = xm;
        ys[kept] = ym;
        kept++;
      }

      if (d < min) min = d;
      if (d > max) max = d;
      sum += d;

      if (d < closest) {
        closest = d;
        closestBearing = Math.toDegrees(Math.atan2(ym, xm));
      }

      int sIdx = LidarDecoder.sectorIndex(xm, ym);
      if (sIdx >= 0) {
        if (Double.isNaN(sectors[sIdx]) || d < sectors[sIdx]) {
          sectors[sIdx] = d;
        }
      }
    }

    if (kept == 0) return LidarDecoder.Scan.empty();
    if (kept < xs.length) {
      float[] xt = new float[kept];
      float[] yt = new float[kept];
      System.arraycopy(xs, 0, xt, 0, kept);
      System.arraycopy(ys, 0, yt, 0, kept);
      xs = xt;
      ys = yt;
    }

    double mean = sum / kept;
    return new LidarDecoder.Scan(kept, xs, ys, min, max, mean, closest, closestBearing, sectors);
  }
}
