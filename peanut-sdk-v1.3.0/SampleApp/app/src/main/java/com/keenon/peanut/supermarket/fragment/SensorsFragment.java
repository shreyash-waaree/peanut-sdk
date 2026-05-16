package com.keenon.peanut.supermarket.fragment;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.keenon.peanut.sample.R;
import com.keenon.peanut.sample.receiver.LidarDecoder;
import com.keenon.peanut.sample.receiver.SensorDataManager;
import com.keenon.peanut.supermarket.widget.LidarRadarView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;

/**
 * Live sensor dashboard — pairs the camera preview tab. Receives a 5 Hz unified JSON from
 * {@link SensorDataManager} and renders one card per sensor (battery, position, IMU, LiDAR,
 * sonar, collision, bump, motor, navigation, charging, topic rates).
 */
public class SensorsFragment extends Fragment implements SensorDataManager.SensorDataListener {

  private static final String TAG = "SensorsFragment";
  /** Snooze threshold: if a topic hasn't updated for this long, heartbeat text turns red. */
  private static final long HEARTBEAT_STALE_MS = 2500L;

  private final Handler main = new Handler(Looper.getMainLooper());

  private TextView tvStatus;
  private TextView tvBatteryValue;
  private ProgressBar pbBattery;
  private TextView tvBatteryHb;
  private TextView tvPositionValue;
  private TextView tvPositionMeta;
  private TextView tvPositionHb;
  private TextView tvPositionPath;
  private TextView tvImuValue;
  private TextView tvImuHb;
  private TextView tvImuPath;
  private Switch swImuRos;
  private TextView tvLidarValue;
  private TextView tvLidarHb;
  private TextView tvLidarClosest;
  private TextView tvLidarSectors;
  private TextView tvLidarPath;
  private TextView tvLidarStatus;
  private Switch swLidarDemo;
  private Switch swLidarRos;
  private LidarRadarView lidarRadar;
  private EditText etLidarRange;
  private Button btnLidarRangeApply;
  private EditText etLidarMaxPoints;
  private Button btnLidarMaxPointsApply;
  private TextView tvSonarValue;
  private ProgressBar pbSonar;
  private TextView tvSonarHb;
  private Switch swSafetyRos;
  private TextView tvCollisionValue;
  private TextView tvBumpValue;
  private TextView tvMotorValue;
  private TextView tvNavValue;
  private TextView tvChargeValue;
  private TextView tvTopicRates;

  @Nullable
  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_dev_sensors, container, false);
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    tvStatus = view.findViewById(R.id.tv_sensors_status);
    tvBatteryValue = view.findViewById(R.id.tv_battery_value);
    pbBattery = view.findViewById(R.id.pb_battery);
    tvBatteryHb = view.findViewById(R.id.tv_battery_hb);
    tvPositionValue = view.findViewById(R.id.tv_position_value);
    tvPositionMeta = view.findViewById(R.id.tv_position_meta);
    tvPositionHb = view.findViewById(R.id.tv_position_hb);
    tvPositionPath = view.findViewById(R.id.tv_position_path);
    tvImuValue = view.findViewById(R.id.tv_imu_value);
    tvImuHb = view.findViewById(R.id.tv_imu_hb);
    tvImuPath = view.findViewById(R.id.tv_imu_path);
    swImuRos = view.findViewById(R.id.sw_imu_ros);
    swImuRos.setChecked(SensorDataManager.getInstance().isImuRosMode());
    swImuRos.setOnCheckedChangeListener((buttonView, isChecked) ->
        SensorDataManager.getInstance().setImuRosMode(isChecked));
    tvLidarValue = view.findViewById(R.id.tv_lidar_value);
    tvLidarHb = view.findViewById(R.id.tv_lidar_hb);
    tvLidarClosest = view.findViewById(R.id.tv_lidar_closest);
    tvLidarSectors = view.findViewById(R.id.tv_lidar_sectors);
    tvLidarPath = view.findViewById(R.id.tv_lidar_path);
    tvLidarStatus = view.findViewById(R.id.tv_lidar_status);
    swLidarDemo = view.findViewById(R.id.sw_lidar_demo);
    swLidarRos = view.findViewById(R.id.sw_lidar_ros);
    lidarRadar = view.findViewById(R.id.view_lidar_radar);
    etLidarRange = view.findViewById(R.id.et_lidar_range);
    btnLidarRangeApply = view.findViewById(R.id.btn_lidar_range_apply);
    etLidarRange.setText(String.valueOf((int) LidarDecoder.DEFAULT_HORIZON_M));
    btnLidarRangeApply.setOnClickListener(v -> applyLidarRange());
    etLidarRange.setOnEditorActionListener((v, actionId, event) -> {
      if (actionId == EditorInfo.IME_ACTION_DONE) { applyLidarRange(); return true; }
      return false;
    });
    etLidarMaxPoints = view.findViewById(R.id.et_lidar_max_points);
    btnLidarMaxPointsApply = view.findViewById(R.id.btn_lidar_max_points_apply);
    btnLidarMaxPointsApply.setOnClickListener(v -> applyLidarMaxPoints());
    etLidarMaxPoints.setOnEditorActionListener((v, actionId, event) -> {
      if (actionId == EditorInfo.IME_ACTION_DONE) { applyLidarMaxPoints(); return true; }
      return false;
    });
    swLidarDemo.setChecked(SensorDataManager.getInstance().isLidarDemoMode());
    swLidarDemo.setOnCheckedChangeListener((buttonView, isChecked) -> {
      SensorDataManager.getInstance().setLidarDemoMode(isChecked);
      if (isChecked && swLidarRos != null && swLidarRos.isChecked()) {
        swLidarRos.setChecked(false);
      }
    });
    swLidarRos.setChecked(SensorDataManager.getInstance().isLidarRosMode());
    swLidarRos.setOnCheckedChangeListener((buttonView, isChecked) -> {
      SensorDataManager.getInstance().setLidarRosMode(isChecked);
      if (isChecked && swLidarDemo != null && swLidarDemo.isChecked()) {
        swLidarDemo.setChecked(false);
      }
    });
    tvSonarValue = view.findViewById(R.id.tv_sonar_value);
    pbSonar = view.findViewById(R.id.pb_sonar);
    tvSonarHb = view.findViewById(R.id.tv_sonar_hb);
    swSafetyRos = view.findViewById(R.id.sw_safety_ros);
    swSafetyRos.setChecked(SensorDataManager.getInstance().isSafetyRosMode());
    swSafetyRos.setOnCheckedChangeListener((buttonView, isChecked) ->
        SensorDataManager.getInstance().setSafetyRosMode(isChecked));
    tvCollisionValue = view.findViewById(R.id.tv_collision_value);
    tvBumpValue = view.findViewById(R.id.tv_bump_value);
    tvMotorValue = view.findViewById(R.id.tv_motor_value);
    tvNavValue = view.findViewById(R.id.tv_nav_value);
    tvChargeValue = view.findViewById(R.id.tv_charge_value);
    tvTopicRates = view.findViewById(R.id.tv_topic_rates);
  }

  @Override
  public void onResume() {
    super.onResume();
    SensorDataManager mgr = SensorDataManager.getInstance();
    try {
      mgr.init();
    } catch (Throwable t) {
      Log.w(TAG, "SensorDataManager.init failed", t);
    }
    mgr.addListener(this);
    tvStatus.setText("Listening…");
  }

  @Override
  public void onPause() {
    SensorDataManager.getInstance().removeListener(this);
    super.onPause();
  }

  @Override
  public void onSensorDataUpdated(String jsonPayload) {
    // Manager callback is on its scheduled executor thread — marshal to main.
    main.post(() -> applySnapshot(jsonPayload));
  }

  private void applySnapshot(String jsonPayload) {
    if (getView() == null) return;
    if (jsonPayload == null || jsonPayload.isEmpty()) {
      tvStatus.setText("No data");
      return;
    }
    try {
      JSONObject root = new JSONObject(jsonPayload);
      JSONObject data = root.optJSONObject("data");
      JSONObject meta = root.optJSONObject("meta");
      if (data == null) {
        tvStatus.setText("No data");
        return;
      }
      tvStatus.setText("Live");

      // Battery
      int battery = data.optInt("battery_percent", -1);
      if (battery >= 0 && battery <= 100) {
        tvBatteryValue.setText(battery + "%");
        pbBattery.setProgress(battery);
      } else {
        tvBatteryValue.setText("—");
        pbBattery.setProgress(0);
      }

      // Position (x / y / theta) — values come from data.robotPos.{x,y,rotation}
      // published at ~5 Hz on /position/robotPosInfo while the robot is localised.
      String x = optDoubleAsText(data, "position_x", "%.2f");
      String y = optDoubleAsText(data, "position_y", "%.2f");
      String theta = optDoubleAsText(data, "position_theta", "%.2f");
      tvPositionValue.setText("x: " + x + " m   y: " + y + " m   \u03B8: " + theta + " rad");
      String floor = optIntAsText(data, "position_floor");
      String curName = optString(data, "position_cur_name", "");
      StringBuilder metaLine = new StringBuilder();
      metaLine.append("Floor ").append(floor);
      if (curName != null && !curName.isEmpty()) metaLine.append(" · ").append(curName);
      tvPositionMeta.setText(metaLine.toString());

      // IMU — yaw/pitch/roll arrive via /sensor/imuangle (separate topic from /sensor/imu).
      // When imu_health is present it reflects the gyro health flag from /sensor/imu.
      String yaw = optDoubleAsText(data, "imu_yaw", "%.1f");
      String pitch = optDoubleAsText(data, "imu_pitch", "%.1f");
      String roll = optDoubleAsText(data, "imu_roll", "%.1f");
      int imuHealth = data.optInt("imu_health", -1);
      String healthLabel = imuHealth == 1 ? "ok" : imuHealth == 0 ? "fault" : "—";
      tvImuValue.setText("Yaw " + yaw + "\u00B0   Pitch " + pitch + "\u00B0   Roll " + roll
          + "\u00B0   (gyro: " + healthLabel + ")");

      // LiDAR — summary line + closest-hit callout + per-sector min distances + radar view.
      // "Unavailable" is replaced with a more helpful hint if the LiDAR topic has simply
      // not produced any frames yet (so the user can distinguish "still booting" from
      // "decoder saw 0 valid points in a frame").
      String lidarSummary = optString(data, "lidar_summary", "Unavailable");
      int lidarPoints = data.optInt("lidar_point_count", -1);
      boolean lidarIsDemo = data.optBoolean("lidar_is_demo", false);
      JSONObject lidarHb = meta != null ? meta.optJSONObject("hb_lidar") : null;
      long lidarLastMs = lidarHb != null ? lidarHb.optLong("last_update_ms", 0L) : 0L;
      if (!lidarIsDemo && lidarPoints == 0 && lidarLastMs == 0L) {
        lidarSummary = "Waiting for first frame (OBSERVE /sensor/lidar)";
      }
      tvLidarValue.setText(lidarSummary);
      String lidarErr = optString(data, "lidar_error_hint", "");
      if (lidarIsDemo) {
        tvLidarStatus.setVisibility(View.VISIBLE);
        tvLidarStatus.setText("Demo pattern — not from the robot. Turn off to attempt live /sensor/lidar.");
      } else if (lidarErr != null && !lidarErr.isEmpty()) {
        tvLidarStatus.setVisibility(View.VISIBLE);
        tvLidarStatus.setText(lidarErr);
      } else {
        tvLidarStatus.setVisibility(View.GONE);
        tvLidarStatus.setText("");
      }
      renderLidarExtras(data);

      // Sonar
      String sonarText = optString(data, "sonar_distance", "—");
      tvSonarValue.setText(sonarText.equals("No reading") ? "—" : sonarText + " m");
      double sonarMeters = tryParseDouble(sonarText);
      // Map 0–3 m to 100–0% on the bar (closer object -> fuller bar as "proximity").
      int proximityPct = 0;
      if (!Double.isNaN(sonarMeters) && sonarMeters >= 0d) {
        double clamped = Math.min(3.0, sonarMeters);
        proximityPct = (int) Math.round((1.0 - clamped / 3.0) * 100.0);
      }
      pbSonar.setProgress(Math.max(0, Math.min(100, proximityPct)));

      // Collision
      String collision = optString(data, "collision_state", "—");
      tvCollisionValue.setText(collision);
      tvCollisionValue.setBackgroundColor(colorForState(collision));
      tvCollisionValue.setTextColor(
          "YES".equalsIgnoreCase(collision) ? Color.WHITE : Color.parseColor("#0E1230"));

      // Bump / Touch
      String bump = optString(data, "bump_state", "—");
      tvBumpValue.setText(bump);
      tvBumpValue.setBackgroundColor(colorForState(bump));
      tvBumpValue.setTextColor(
          "YES".equalsIgnoreCase(bump) ? Color.WHITE : Color.parseColor("#0E1230"));

      // Motor / Navigation / Charging
      int motorState = data.optInt("motor_state", -1);
      tvMotorValue.setText(motorState >= 0 ? String.valueOf(motorState) : "—");
      int navState = data.optInt("nav_state", -1);
      tvNavValue.setText(navState >= 0 ? String.valueOf(navState) : "—");
      tvChargeValue.setText(optString(data, "charge_state", "—"));

      // Heartbeats + topic rates + per-card CoAP request paths
      if (meta != null) {
        long now = SystemClock.elapsedRealtime();
        applyHeartbeat(tvBatteryHb, meta.optJSONObject("hb_battery"), now);
        applyHeartbeat(tvPositionHb, meta.optJSONObject("hb_position"), now);
        applyHeartbeatMerged(tvImuHb,
            meta.optJSONObject("hb_imu"), meta.optJSONObject("hb_imu_angle"), now);
        applyHeartbeat(tvLidarHb, meta.optJSONObject("hb_lidar"), now);
        applyHeartbeat(tvSonarHb, meta.optJSONObject("hb_sonar"), now);

        JSONObject paths = meta.optJSONObject("coap_paths");
        if (paths != null) {
          tvPositionPath.setText("SDK: " + paths.optString("position", "?"));
          tvImuPath.setText("SDK: " + paths.optString("imu_angle", "?") + "  +  "
              + paths.optString("imu", "?"));
          tvLidarPath.setText("SDK: " + paths.optString("lidar", "?"));
        }
        tvTopicRates.setText(buildRatesText(meta));
      }

    } catch (JSONException e) {
      Log.w(TAG, "Bad sensor JSON", e);
      tvStatus.setText("Bad payload");
    }
  }

  private static String optString(JSONObject o, String key, String fallback) {
    String v = o.optString(key, "");
    if (v == null || v.isEmpty() || "null".equalsIgnoreCase(v)) return fallback;
    return v;
  }

  private static String optDoubleAsText(JSONObject o, String key, String fmt) {
    Object v = o.opt(key);
    if (v instanceof Number) {
      double d = ((Number) v).doubleValue();
      if (Double.isFinite(d)) {
        return String.format(Locale.getDefault(), fmt, d);
      }
    }
    return "—";
  }

  private static String optIntAsText(JSONObject o, String key) {
    Object v = o.opt(key);
    if (v instanceof Number) {
      return String.valueOf(((Number) v).intValue());
    }
    return "—";
  }

  private static double tryParseDouble(String s) {
    if (s == null || s.isEmpty()) return Double.NaN;
    try {
      return Double.parseDouble(s);
    } catch (NumberFormatException e) {
      return Double.NaN;
    }
  }

  private static int colorForState(String state) {
    if (state == null) return Color.parseColor("#E0E0E0");
    if ("YES".equalsIgnoreCase(state)) return Color.parseColor("#D32F2F");
    if ("NO".equalsIgnoreCase(state)) return Color.parseColor("#C8E6C9");
    return Color.parseColor("#E0E0E0");
  }

  /**
   * Renders the IMU heartbeat using whichever of the two component topics
   * (/sensor/imu and /sensor/imuangle) is the most recently updated. The IMU
   * card depends on both, so we want the UI to flip to "stale" only when both
   * are stale — that way the developer immediately sees whether the angles
   * (imuangle) or the gyro flag (imu) went silent.
   */
  private static void applyHeartbeatMerged(TextView target,
      JSONObject imu, JSONObject imuAngle, long nowMs) {
    JSONObject freshest = imu;
    long imuMs = imu != null ? imu.optLong("last_update_ms", 0L) : 0L;
    long angMs = imuAngle != null ? imuAngle.optLong("last_update_ms", 0L) : 0L;
    if (angMs > imuMs) freshest = imuAngle;
    applyHeartbeat(target, freshest, nowMs);
  }

  private static void applyHeartbeat(TextView target, JSONObject hb, long nowMs) {
    if (hb == null) {
      target.setText("No updates yet");
      target.setTextColor(Color.parseColor("#607D8B"));
      return;
    }
    long lastUpdate = hb.optLong("last_update_ms", 0L);
    double ups = hb.optDouble("ups", 0d);
    int seq = hb.optInt("seq", 0);
    long ageMs = lastUpdate > 0 ? nowMs - lastUpdate : -1L;
    String ageText = ageMs < 0 ? "never"
        : ageMs < 1000 ? ageMs + " ms ago"
        : String.format(Locale.getDefault(), "%.1f s ago", ageMs / 1000.0);
    target.setText(String.format(Locale.getDefault(),
        "seq %d · %.1f/s · last %s", seq, ups, ageText));
    boolean stale = ageMs < 0 || ageMs > HEARTBEAT_STALE_MS;
    target.setTextColor(stale ? Color.parseColor("#D32F2F") : Color.parseColor("#607D8B"));
  }

  /**
   * Renders the LiDAR card's secondary widgets: the "closest hit" callout, the
   * per-sector min-distance grid, and the 2D radar visualisation. The values come
   * from the unified snapshot produced by {@code SensorDataManager#applyLidarFields}.
   */
  private void renderLidarExtras(JSONObject data) {
    double closest = data.optDouble("lidar_closest_m", Double.NaN);
    String dirLabel = optString(data, "lidar_closest_dir_label", "—");
    int pointCount = data.optInt("lidar_point_count", 0);

    if (Double.isFinite(closest) && closest > 0d) {
      double bearing = data.optDouble("lidar_closest_dir_deg", Double.NaN);
      String bearingText = Double.isFinite(bearing)
          ? String.format(Locale.getDefault(), "%+.0f\u00B0", bearing)
          : "—";
      tvLidarClosest.setText(String.format(Locale.getDefault(),
          "Closest %.2f m toward %s (%s) \u00B7 %d rays",
          closest, dirLabel, bearingText, pointCount));
    } else {
      tvLidarClosest.setText("Closest — (no returns yet)");
    }

    tvLidarSectors.setText(formatSectors(data.optJSONArray("lidar_sectors_m")));

    if (lidarRadar != null) {
      lidarRadar.setScan(buildScanFromSnapshot(data));
    }
  }

  /**
   * Re-hydrates a {@link LidarDecoder.Scan} from the fields written by the manager.
   * We rebuild only what the radar view needs (XY points + the closest-bearing
   * overlay); the other fields stay {@code NaN} where unused.
   */
  private static LidarDecoder.Scan buildScanFromSnapshot(JSONObject data) {
    JSONArray xs = data.optJSONArray("lidar_points_x");
    JSONArray ys = data.optJSONArray("lidar_points_y");
    if (xs == null || ys == null) return LidarDecoder.Scan.empty();
    int n = Math.min(xs.length(), ys.length());
    if (n <= 0) return LidarDecoder.Scan.empty();

    float[] xArr = new float[n];
    float[] yArr = new float[n];
    for (int i = 0; i < n; i++) {
      xArr[i] = (float) xs.optDouble(i, 0d);
      yArr[i] = (float) ys.optDouble(i, 0d);
    }
    double min = data.optDouble("lidar_min_m", Double.POSITIVE_INFINITY);
    double max = data.optDouble("lidar_max_m", Double.NEGATIVE_INFINITY);
    double mean = data.optDouble("lidar_mean_m", Double.NaN);
    double closest = data.optDouble("lidar_closest_m", Double.NaN);
    double bearing = data.optDouble("lidar_closest_dir_deg", Double.NaN);

    JSONArray sArr = data.optJSONArray("lidar_sectors_m");
    double[] sectors = new double[LidarDecoder.SECTOR_COUNT];
    for (int i = 0; i < sectors.length; i++) {
      if (sArr != null && i < sArr.length() && !sArr.isNull(i)) {
        sectors[i] = sArr.optDouble(i, Double.NaN);
      } else {
        sectors[i] = Double.NaN;
      }
    }
    return new LidarDecoder.Scan(n, xArr, yArr, min, max, mean, closest, bearing, sectors);
  }

  /**
   * Turns the 8-entry sector array into a two-line grid ("F 1.2  FL 0.8  L 2.1 …").
   * Sectors with no return show a dash.
   */
  private static String formatSectors(JSONArray sectors) {
    if (sectors == null) return "Sectors: —";
    StringBuilder sb = new StringBuilder("Sectors (m):");
    for (int i = 0; i < LidarDecoder.SECTOR_COUNT; i++) {
      if (i == 4) sb.append('\n');
      sb.append("  ");
      sb.append(LidarDecoder.sectorLabel(i));
      sb.append(' ');
      if (i < sectors.length() && !sectors.isNull(i)) {
        double v = sectors.optDouble(i, Double.NaN);
        sb.append(Double.isFinite(v)
            ? String.format(Locale.getDefault(), "%.2f", v)
            : "—");
      } else {
        sb.append("—");
      }
    }
    return sb.toString();
  }

  private void applyLidarMaxPoints() {
    if (etLidarMaxPoints == null) return;
    String text = etLidarMaxPoints.getText().toString().trim();
    if (text.isEmpty()) {
      LidarDecoder.MAX_POINTS = Integer.MAX_VALUE;
      Toast.makeText(getContext(), "Max points: all (no cap)", Toast.LENGTH_SHORT).show();
      return;
    }
    try {
      int pts = Integer.parseInt(text);
      if (pts < 1) {
        Toast.makeText(getContext(), "Enter a number ≥ 1, or leave blank for all", Toast.LENGTH_SHORT).show();
        return;
      }
      LidarDecoder.MAX_POINTS = pts;
      Toast.makeText(getContext(), "Max points set to " + pts, Toast.LENGTH_SHORT).show();
    } catch (NumberFormatException e) {
      Toast.makeText(getContext(), "Enter a valid integer", Toast.LENGTH_SHORT).show();
    }
  }

  private void applyLidarRange() {
    if (etLidarRange == null || lidarRadar == null) return;
    String text = etLidarRange.getText().toString().trim();
    try {
      float meters = Float.parseFloat(text);
      if (meters < 1f || meters > 30f) {
        Toast.makeText(getContext(), "Range must be between 1 and 30 m", Toast.LENGTH_SHORT).show();
        return;
      }
      lidarRadar.setHorizonMeters(meters);
      Toast.makeText(getContext(), "Radar range set to " + meters + " m", Toast.LENGTH_SHORT).show();
    } catch (NumberFormatException e) {
      Toast.makeText(getContext(), "Enter a valid number", Toast.LENGTH_SHORT).show();
    }
  }

  private static String buildRatesText(JSONObject meta) {
    String[] names = {
        "hb_battery", "hb_position", "hb_navigation", "hb_imu", "hb_imu_angle",
        "hb_lidar", "hb_sonar", "hb_motor", "hb_collision", "hb_bump", "hb_charge",
    };
    StringBuilder sb = new StringBuilder();
    for (String n : names) {
      JSONObject hb = meta.optJSONObject(n);
      if (hb == null) continue;
      String label = n.startsWith("hb_") ? n.substring(3) : n;
      double ups = hb.optDouble("ups", 0d);
      int seq = hb.optInt("seq", 0);
      sb.append(String.format(Locale.getDefault(),
          "%-10s %5.1f/s (seq %d)%n", label, ups, seq));
    }
    return sb.length() == 0 ? "—" : sb.toString().trim();
  }
}
