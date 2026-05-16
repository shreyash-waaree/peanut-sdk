package com.keenon.peanut.sample.receiver;

import android.util.Base64;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;

/**
 * Decodes the Peanut SDK's LiDAR payload into a usable point cloud plus summary metrics.
 *
 * <p>The Peanut SDK emits an OBSERVE payload shaped like:
 * <pre>
 *   {
 *     "data": {
 *       "count":  N,
 *       "base64": "...",    // may be present
 *       "bytes":  [b0, b1, ...]  // 7 bytes per point (see below)
 *     },
 *     "topic":  "SensorLidarApi",
 *     ...
 *   }
 * </pre>
 *
 * <p>Each point packs 2D coordinates (centimetres, signed) into <b>7 bytes</b>
 * (from {@code SensorLidarApi.Bean.LidarBean.getPointXYs()} in
 * {@code peanut-sdk-release.aar}):
 * <ul>
 *   <li>{@code b0}:     signBitX high nibble</li>
 *   <li>{@code b1}:     signBitX low nibble (high 4 bits) + X high nibble (low 4 bits)</li>
 *   <li>{@code b2}:     X low byte</li>
 *   <li>{@code b3}:     (reserved / ignored by SDK)</li>
 *   <li>{@code b4}:     signBitY high nibble</li>
 *   <li>{@code b5}:     signBitY low nibble (high 4 bits) + Y high nibble (low 4 bits)</li>
 *   <li>{@code b6}:     Y low byte</li>
 * </ul>
 * The SDK's logic (quirky but accurate):
 * <pre>
 *   signBitX = (b0 << 4) | ((b1 & 0xF0) >>> 4);
 *   dataX    = ((b1 & 0x0F) << 8) | (b2 & 0xFF);
 *   X_cm     = (signBitX == 1) ? dataX : (dataX - 2 * dataX);   // i.e. -dataX when signBit != 1
 * </pre>
 *
 * <p>We also tolerate payloads where the data arrives as a generic JSON {@code ranges}
 * / {@code distances} / {@code range} / {@code points} array — the SampleApp previously
 * supported that shape and we keep it as a fallback so this class replaces the old
 * summary builder.
 */
public final class LidarDecoder {

  /** Point cap — Integer.MAX_VALUE means all points. Adjustable at runtime from the UI. */
  public static volatile int MAX_POINTS = Integer.MAX_VALUE;

  /** 8 cardinal/ordinal sectors, 45 degrees wide, starting at -22.5 degrees. */
  public static final int SECTOR_COUNT = 8;

  /** Sensor horizon used for the radar view and "closest sector" highlights. */
  public static final double DEFAULT_HORIZON_M = 15.0;

  private LidarDecoder() {}

  /** Immutable snapshot returned by {@link #decode(String)}. */
  public static final class Scan {
    public final int pointCount;
    public final float[] xMeters;     // length == pointCount (may be 0)
    public final float[] yMeters;     // length == pointCount
    public final double minMeters;    // POSITIVE_INFINITY if empty
    public final double maxMeters;    // NEGATIVE_INFINITY if empty
    public final double meanMeters;   // NaN if empty
    public final double closestMeters;        // NaN if empty
    public final double closestBearingDeg;    // 0=forward (+x), +90=left (+y), NaN if empty
    /** Min distance per 45-degree sector (8 entries), NaN if sector had no returns. */
    public final double[] sectorsMeters;

    public Scan(
        int pointCount,
        float[] x,
        float[] y,
        double min,
        double max,
        double mean,
        double closest,
        double bearing,
        double[] sectors) {
      this.pointCount = pointCount;
      this.xMeters = x;
      this.yMeters = y;
      this.minMeters = min;
      this.maxMeters = max;
      this.meanMeters = mean;
      this.closestMeters = closest;
      this.closestBearingDeg = bearing;
      this.sectorsMeters = sectors;
    }

    public static Scan empty() {
      return new Scan(0, new float[0], new float[0],
          Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NaN,
          Double.NaN, Double.NaN, newNanSectors());
    }

    public boolean hasPoints() {
      return pointCount > 0;
    }

    /** "rays N · min X · max Y · mean Z" (or "Unavailable" when empty). */
    public String summary() {
      if (!hasPoints()) return "Unavailable";
      return String.format(Locale.getDefault(),
          "rays %d \u00B7 min %.2f m \u00B7 max %.2f m \u00B7 mean %.2f m",
          pointCount, minMeters, maxMeters, meanMeters);
    }
  }

  private static double[] newNanSectors() {
    double[] s = new double[SECTOR_COUNT];
    for (int i = 0; i < SECTOR_COUNT; i++) s[i] = Double.NaN;
    return s;
  }

  /**
   * Decodes the unified LiDAR payload cached by {@code SensorDataManager}.
   * Returns {@link Scan#empty()} for null / malformed / empty inputs.
   */
  public static Scan decode(String rawPayload) {
    if (rawPayload == null) return Scan.empty();
    String trimmed = rawPayload.trim();
    if (trimmed.isEmpty() || "{}".equals(trimmed)) return Scan.empty();

    try {
      JSONObject root = new JSONObject(trimmed);
      JSONObject data = root.has("data") ? root.optJSONObject("data") : root;
      if (data == null) return Scan.empty();

      byte[] bytes = extractBytes(data);
      if (bytes != null && bytes.length >= 7) {
        return fromPackedBytes(bytes);
      }
      return fromGenericArrays(data);
    } catch (JSONException ignored) {
      return Scan.empty();
    }
  }

  /**
   * Reads the byte array out of the SDK {@code data} object.
   * Prefers the raw {@code bytes} list; falls back to {@code base64}.
   */
  private static byte[] extractBytes(JSONObject data) {
    Object bytesObj = data.opt("bytes");
    if (bytesObj instanceof JSONArray) {
      JSONArray arr = (JSONArray) bytesObj;
      int len = arr.length();
      if (len > 0) {
        byte[] out = new byte[len];
        for (int i = 0; i < len; i++) {
          out[i] = (byte) (arr.optInt(i, 0) & 0xFF);
        }
        return out;
      }
    }
    String base64 = data.optString("base64", "");
    if (base64 != null && !base64.isEmpty()) {
      try {
        return Base64.decode(base64, Base64.DEFAULT);
      } catch (IllegalArgumentException ignored) {
        return null;
      }
    }
    return null;
  }

  private static Scan fromPackedBytes(byte[] bytes) {
    int rawPoints = bytes.length / 7;
    if (rawPoints <= 0) return Scan.empty();

    int keepEvery = Math.max(1, (int) Math.ceil(rawPoints / (double) MAX_POINTS));
    int expected = (rawPoints + keepEvery - 1) / keepEvery;

    float[] xs = new float[expected];
    float[] ys = new float[expected];
    double min = Double.POSITIVE_INFINITY;
    double max = Double.NEGATIVE_INFINITY;
    double sum = 0d;
    int kept = 0;
    double closest = Double.POSITIVE_INFINITY;
    double closestBearing = Double.NaN;
    double[] sectors = newNanSectors();

    for (int iPoint = 0; iPoint < rawPoints; iPoint++) {
      if ((iPoint % keepEvery) != 0) continue;
      int off = iPoint * 7;
      int b0 = bytes[off] & 0xFF;
      int b1 = bytes[off + 1] & 0xFF;
      int b2 = bytes[off + 2] & 0xFF;
      int b4 = bytes[off + 4] & 0xFF;
      int b5 = bytes[off + 5] & 0xFF;
      int b6 = bytes[off + 6] & 0xFF;

      int signBitX = (b0 << 4) | ((b1 & 0xF0) >>> 4);
      int dataX = ((b1 & 0x0F) << 8) | b2;
      int signBitY = (b4 << 4) | ((b5 & 0xF0) >>> 4);
      int dataY = ((b5 & 0x0F) << 8) | b6;

      int cmX = signBitX == 1 ? dataX : -dataX;
      int cmY = signBitY == 1 ? dataY : -dataY;

      if (cmX == 0 && cmY == 0) continue;

      float xm = cmX / 100f;
      float ym = cmY / 100f;
      double d = Math.hypot(xm, ym);
      if (!Double.isFinite(d) || d <= 0d) continue;

      if (kept < xs.length) {
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

      int sectorIdx = sectorIndex(xm, ym);
      if (sectorIdx >= 0) {
        if (Double.isNaN(sectors[sectorIdx]) || d < sectors[sectorIdx]) {
          sectors[sectorIdx] = d;
        }
      }
    }

    if (kept == 0) return Scan.empty();

    if (kept < xs.length) {
      float[] xsTrim = new float[kept];
      float[] ysTrim = new float[kept];
      System.arraycopy(xs, 0, xsTrim, 0, kept);
      System.arraycopy(ys, 0, ysTrim, 0, kept);
      xs = xsTrim;
      ys = ysTrim;
    }

    double mean = sum / kept;
    return new Scan(kept, xs, ys, min, max, mean, closest, closestBearing, sectors);
  }

  /**
   * Fallback for payloads where the robot / simulator emits a generic JSON distance
   * array instead of the packed 7-byte per-point format. We treat each value as a
   * distance (m) with angle {@code i * (2π / N)} — accurate only when the source
   * really is a 360-degree sweep; otherwise the angles are nominal and callers
   * should still treat the summary as informative.
   */
  private static Scan fromGenericArrays(JSONObject data) {
    JSONArray arr = null;
    for (String key : new String[]{"ranges", "distances", "range", "points"}) {
      Object v = data.opt(key);
      if (v instanceof JSONArray && ((JSONArray) v).length() > 0) {
        arr = (JSONArray) v;
        break;
      }
    }
    if (arr == null || arr.length() == 0) return Scan.empty();

    int n = arr.length();
    int keepEvery = Math.max(1, (int) Math.ceil(n / (double) MAX_POINTS));
    int expected = (n + keepEvery - 1) / keepEvery;
    float[] xs = new float[expected];
    float[] ys = new float[expected];

    double min = Double.POSITIVE_INFINITY;
    double max = Double.NEGATIVE_INFINITY;
    double sum = 0d;
    int kept = 0;
    double closest = Double.POSITIVE_INFINITY;
    double closestBearing = Double.NaN;
    double[] sectors = newNanSectors();

    for (int i = 0; i < n; i++) {
      double d;
      Object item = arr.opt(i);
      if (item instanceof Number) {
        d = ((Number) item).doubleValue();
      } else if (item instanceof JSONObject) {
        JSONObject o = (JSONObject) item;
        d = o.optDouble("dist", o.optDouble("distance", Double.NaN));
        if (Double.isNaN(d)) {
          double x = o.optDouble("x", Double.NaN);
          double y = o.optDouble("y", Double.NaN);
          if (!Double.isNaN(x) && !Double.isNaN(y)) {
            d = Math.hypot(x, y);
          }
        }
      } else {
        continue;
      }
      if (!Double.isFinite(d) || d <= 0d) continue;

      if ((i % keepEvery) == 0 && kept < xs.length) {
        double angleRad = (2.0 * Math.PI * i) / n;
        xs[kept] = (float) (Math.cos(angleRad) * d);
        ys[kept] = (float) (Math.sin(angleRad) * d);
        int sIdx = sectorIndex(xs[kept], ys[kept]);
        if (sIdx >= 0) {
          if (Double.isNaN(sectors[sIdx]) || d < sectors[sIdx]) {
            sectors[sIdx] = d;
          }
        }
        kept++;
      }

      if (d < min) min = d;
      if (d > max) max = d;
      sum += d;
      if (d < closest) {
        closest = d;
        closestBearing = Math.toDegrees((2.0 * Math.PI * i) / n);
        if (closestBearing > 180d) closestBearing -= 360d;
      }
    }

    if (kept == 0) return Scan.empty();

    if (kept < xs.length) {
      float[] xsTrim = new float[kept];
      float[] ysTrim = new float[kept];
      System.arraycopy(xs, 0, xsTrim, 0, kept);
      System.arraycopy(ys, 0, ysTrim, 0, kept);
      xs = xsTrim;
      ys = ysTrim;
    }

    double mean = sum / kept;
    return new Scan(kept, xs, ys, min, max, mean, closest, closestBearing, sectors);
  }

  /**
   * Synthetic 360° distance sweep (reuses {@link #fromGenericArrays} via {@link #decode})
   * so the Developer → Sensors radar can be exercised when CoAP {@code /sensor/lidar}
   * returns an empty payload (LiDAR motor off, algorithm board idle, etc.).
   */
  public static Scan demoScanForUi() {
    try {
      int n = 360;
      JSONArray ranges = new JSONArray();
      for (int i = 0; i < n; i++) {
        double ang = (i / (double) n) * 2.0 * Math.PI;
        // Nominal room radius ~1.45 m with gentle ripple
        double d = 1.45 + 0.12 * Math.sin(ang * 4.0);
        // Narrow closer return in front (+x): indices near 0
        if (i <= 14 || i >= n - 14) {
          d = Math.min(d, 0.72 + 0.03 * Math.sin(i));
        }
        // Side tables slightly closer on left (+y convention in generic path)
        if (i >= 78 && i <= 102) {
          d = Math.min(d, 0.95);
        }
        ranges.put(d);
      }
      JSONObject data = new JSONObject();
      data.put("ranges", ranges);
      JSONObject root = new JSONObject();
      root.put("data", data);
      return decode(root.toString());
    } catch (JSONException e) {
      return Scan.empty();
    }
  }

  /**
   * Returns a sector index 0..7 for the given XY (m) using 45-degree wedges:
   * {@code 0 = front (±22.5°), 1 = front-left, 2 = left, 3 = back-left, 4 = back,
   * 5 = back-right, 6 = right, 7 = front-right}. Returns -1 for (0,0).
   */
  public static int sectorIndex(double x, double y) {
    if (x == 0d && y == 0d) return -1;
    double deg = Math.toDegrees(Math.atan2(y, x));
    deg = (deg + 360d + 22.5d) % 360d;
    int idx = (int) (deg / 45d);
    if (idx < 0) idx = 0;
    if (idx >= SECTOR_COUNT) idx = SECTOR_COUNT - 1;
    return idx;
  }

  /** Human label for the 8 sectors, matching {@link #sectorIndex(double, double)}. */
  public static String sectorLabel(int idx) {
    switch (idx) {
      case 0: return "F";
      case 1: return "FL";
      case 2: return "L";
      case 3: return "BL";
      case 4: return "B";
      case 5: return "BR";
      case 6: return "R";
      case 7: return "FR";
      default: return "?";
    }
  }

  /**
   * Converts a bearing in degrees (atan2 convention, +x = 0, +y = +90) to a short
   * label like "F", "FL", "L", etc.
   */
  public static String bearingLabel(double deg) {
    if (Double.isNaN(deg)) return "—";
    double norm = (deg + 360d + 22.5d) % 360d;
    int idx = (int) (norm / 45d);
    return sectorLabel(idx);
  }
}
