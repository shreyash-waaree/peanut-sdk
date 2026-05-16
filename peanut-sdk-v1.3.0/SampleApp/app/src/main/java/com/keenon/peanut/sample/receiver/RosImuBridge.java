package com.keenon.peanut.sample.receiver;

import android.util.Log;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Alternative IMU source: subscribes to {@code /imu_raw} (type
 * {@code sensor_msgs/Imu}) on the robot's rosbridge WebSocket and converts
 * each publish into yaw/pitch/roll degrees plus the raw angular velocity and
 * linear acceleration vectors.
 *
 * <p>Used when the Peanut SDK's CoAP paths ({@code /sensor/imu},
 * {@code /sensor/imuangle}) are not being populated (e.g. the main Peanut
 * service isn't running). The ROS stack on the algorithm board publishes
 * to {@code /imu_raw} directly, so this gives the IMU card a live feed from
 * the same physical inertial unit through a different transport.
 *
 * <p><b>Note on units</b>: {@code /imu_raw} on this robot reports
 * {@code linear_acceleration} in <em>milli-g</em> (1 g ≈ 1000), not m/s² —
 * the z-axis reads about {@code 1047} at rest, which is gravity. We store
 * the raw vector unchanged and let the UI decide what to display; the main
 * card only needs yaw/pitch/roll degrees.
 */
public class RosImuBridge {

  private static final String TAG = "RosImuBridge";
  public static final String DEFAULT_URI = "ws://192.168.64.20:9090";
  public static final String DEFAULT_TOPIC = "/imu_raw";
  private static final int DEFAULT_THROTTLE_MS = 200;

  /** Immutable snapshot delivered to the listener on each publish. */
  public static final class Sample {
    public final double yawDeg;
    public final double pitchDeg;
    public final double rollDeg;
    public final double angVelX;   // rad/s
    public final double angVelY;
    public final double angVelZ;
    public final double linAccX;   // raw units from /imu_raw (milli-g on this robot)
    public final double linAccY;
    public final double linAccZ;
    public final String frameId;

    public Sample(double yaw, double pitch, double roll,
                  double avx, double avy, double avz,
                  double lax, double lay, double laz,
                  String frameId) {
      this.yawDeg = yaw;
      this.pitchDeg = pitch;
      this.rollDeg = roll;
      this.angVelX = avx;
      this.angVelY = avy;
      this.angVelZ = avz;
      this.linAccX = lax;
      this.linAccY = lay;
      this.linAccZ = laz;
      this.frameId = frameId;
    }
  }

  public interface Listener {
    void onSample(Sample sample);
    void onStatus(String status, boolean isError);
  }

  private final String uri;
  private final String topic;
  private final Listener listener;

  private WebSocketClient client;
  private volatile boolean running = false;

  public RosImuBridge(Listener listener) {
    this(DEFAULT_URI, DEFAULT_TOPIC, listener);
  }

  public RosImuBridge(String uri, String topic, Listener listener) {
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
            sub.put("type", "sensor_msgs/Imu");
            sub.put("throttle_rate", DEFAULT_THROTTLE_MS);
            sub.put("queue_length", 1);
            send(sub.toString());
          } catch (JSONException e) {
            Log.e(TAG, "subscribe build failed", e);
          }
        }

        @Override
        public void onMessage(String message) {
          handle(message);
        }

        @Override
        public void onClose(int code, String reason, boolean remote) {
          Log.w(TAG, "rosbridge closed code=" + code + " remote=" + remote);
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

  private void handle(String raw) {
    if (raw == null || raw.isEmpty()) return;
    try {
      JSONObject obj = new JSONObject(raw);
      String op = obj.optString("op");
      if ("status".equals(op)) {
        String level = obj.optString("level");
        String msg = obj.optString("msg");
        emitStatus(level + ": " + msg, "error".equalsIgnoreCase(level));
        return;
      }
      if (!"publish".equals(op)) return;
      if (!topic.equals(obj.optString("topic"))) return;
      JSONObject msg = obj.optJSONObject("msg");
      if (msg == null) return;

      JSONObject orient = msg.optJSONObject("orientation");
      JSONObject av = msg.optJSONObject("angular_velocity");
      JSONObject la = msg.optJSONObject("linear_acceleration");
      JSONObject hdr = msg.optJSONObject("header");
      String frameId = hdr != null ? hdr.optString("frame_id", "") : "";

      double qx = orient != null ? orient.optDouble("x", 0d) : 0d;
      double qy = orient != null ? orient.optDouble("y", 0d) : 0d;
      double qz = orient != null ? orient.optDouble("z", 0d) : 0d;
      double qw = orient != null ? orient.optDouble("w", 1d) : 1d;

      double[] ypr = quatToEulerDeg(qx, qy, qz, qw);

      double avx = av != null ? av.optDouble("x", 0d) : 0d;
      double avy = av != null ? av.optDouble("y", 0d) : 0d;
      double avz = av != null ? av.optDouble("z", 0d) : 0d;
      double lax = la != null ? la.optDouble("x", 0d) : 0d;
      double lay = la != null ? la.optDouble("y", 0d) : 0d;
      double laz = la != null ? la.optDouble("z", 0d) : 0d;

      if (listener != null) {
        listener.onSample(new Sample(
            ypr[0], ypr[1], ypr[2],
            avx, avy, avz,
            lax, lay, laz,
            frameId));
      }
    } catch (JSONException e) {
      Log.w(TAG, "non-JSON rosbridge frame", e);
    } catch (Exception e) {
      Log.w(TAG, "imu parse failed", e);
    }
  }

  /**
   * ZYX (yaw-pitch-roll) Euler conversion. Order: [yaw, pitch, roll] in degrees.
   * Yaw   = rotation around +Z
   * Pitch = rotation around +Y
   * Roll  = rotation around +X
   */
  private static double[] quatToEulerDeg(double x, double y, double z, double w) {
    double sinr_cosp = 2.0 * (w * x + y * z);
    double cosr_cosp = 1.0 - 2.0 * (x * x + y * y);
    double roll = Math.atan2(sinr_cosp, cosr_cosp);
    double sinp = 2.0 * (w * y - z * x);
    double pitch;
    if (Math.abs(sinp) >= 1.0) {
      pitch = Math.copySign(Math.PI / 2.0, sinp);
    } else {
      pitch = Math.asin(sinp);
    }
    double siny_cosp = 2.0 * (w * z + x * y);
    double cosy_cosp = 1.0 - 2.0 * (y * y + z * z);
    double yaw = Math.atan2(siny_cosp, cosy_cosp);
    return new double[]{Math.toDegrees(yaw), Math.toDegrees(pitch), Math.toDegrees(roll)};
  }
}