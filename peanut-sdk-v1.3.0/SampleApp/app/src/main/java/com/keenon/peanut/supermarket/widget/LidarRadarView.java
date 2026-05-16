package com.keenon.peanut.supermarket.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import com.keenon.peanut.sample.receiver.LidarDecoder;

import java.util.Locale;

/**
 * Top-down, robot-centric visualisation of one LiDAR scan.
 *
 * <p>The robot sits at the centre pointing up (screen-up = +x = robot forward).
 * Rings at 1 m / 2 m / 3 m / 5 m give scale; each LiDAR return is a coloured
 * dot whose hue indicates proximity (red = very close, green = far).
 * The closest hit is drawn as a filled disc with a thin connecting line so the
 * operator can see the critical direction at a glance.
 *
 * <p>The view redraws at whatever rate the fragment feeds it — typically 5 Hz.
 * It is deliberately cheap: no animations, no bitmaps, no allocations in
 * {@link #onDraw(Canvas)} besides primitive paint state.
 */
public class LidarRadarView extends View {

  private static final float DEFAULT_HORIZON_M = (float) LidarDecoder.DEFAULT_HORIZON_M;

  private final Paint ringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final Paint ringLabelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final Paint crossPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final Paint robotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final Paint headingPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final Paint pointPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final Paint closestPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final Paint closestLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final Paint emptyLabelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

  @Nullable private LidarDecoder.Scan scan;
  private float horizonMeters = DEFAULT_HORIZON_M;

  public LidarRadarView(Context context) {
    this(context, null);
  }

  public LidarRadarView(Context context, @Nullable AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public LidarRadarView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    init();
  }

  private void init() {
    ringPaint.setStyle(Paint.Style.STROKE);
    ringPaint.setStrokeWidth(dp(1f));
    ringPaint.setColor(Color.parseColor("#D8DEEA"));

    ringLabelPaint.setColor(Color.parseColor("#8892A6"));
    ringLabelPaint.setTextSize(dp(10f));

    crossPaint.setStyle(Paint.Style.STROKE);
    crossPaint.setStrokeWidth(dp(0.75f));
    crossPaint.setColor(Color.parseColor("#E4E8F1"));

    robotPaint.setStyle(Paint.Style.FILL);
    robotPaint.setColor(Color.parseColor("#1565C0"));

    headingPaint.setStyle(Paint.Style.STROKE);
    headingPaint.setStrokeWidth(dp(1.2f));
    headingPaint.setColor(Color.parseColor("#1565C0"));

    pointPaint.setStyle(Paint.Style.FILL);
    pointPaint.setColor(Color.parseColor("#1976D2"));

    closestPaint.setStyle(Paint.Style.FILL);
    closestPaint.setColor(Color.parseColor("#D32F2F"));

    closestLinePaint.setStyle(Paint.Style.STROKE);
    closestLinePaint.setStrokeWidth(dp(1.2f));
    closestLinePaint.setColor(Color.parseColor("#E57373"));

    emptyLabelPaint.setColor(Color.parseColor("#8892A6"));
    emptyLabelPaint.setTextSize(dp(12f));
    emptyLabelPaint.setTextAlign(Paint.Align.CENTER);
  }

  /** Replaces the shown scan. Pass {@code null} or an empty scan to show the "no data" state. */
  public void setScan(@Nullable LidarDecoder.Scan next) {
    this.scan = next;
    invalidate();
  }

  /**
   * Sets the display radius (metres) before points are clamped to the outer ring.
   * Values outside {@code [1, 30]} are ignored. Default is 5 m.
   */
  public void setHorizonMeters(float meters) {
    if (meters >= 1f && meters <= 30f) {
      horizonMeters = meters;
      invalidate();
    }
  }

  @Override
  protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);
    int w = getWidth();
    int h = getHeight();
    if (w <= 0 || h <= 0) return;

    float cx = w * 0.5f;
    float cy = h * 0.5f;
    float maxRadiusPx = Math.min(cx, cy) - dp(6f);
    if (maxRadiusPx <= 0) return;

    drawGrid(canvas, cx, cy, maxRadiusPx);
    drawRobot(canvas, cx, cy);

    LidarDecoder.Scan s = scan;
    if (s == null || !s.hasPoints()) {
      canvas.drawText("Waiting for LiDAR…", cx, cy + maxRadiusPx + dp(16f), emptyLabelPaint);
      return;
    }

    float metersToPx = maxRadiusPx / horizonMeters;
    PointF closestPx = null;

    for (int i = 0; i < s.pointCount; i++) {
      float xm = s.xMeters[i];
      float ym = s.yMeters[i];
      double d = Math.hypot(xm, ym);
      if (d > horizonMeters) {
        float scale = (float) (horizonMeters / d);
        xm *= scale;
        ym *= scale;
      }
      float px = cx + ym * metersToPx;   // +y is screen-right (robot-left)
      float py = cy - xm * metersToPx;   // +x is screen-up (robot-forward)
      pointPaint.setColor(colorForDistance(d));
      canvas.drawCircle(px, py, dp(1.6f), pointPaint);
    }

    if (Double.isFinite(s.closestMeters)
        && Double.isFinite(s.closestBearingDeg)
        && s.closestMeters > 0d) {
      double capped = Math.min(s.closestMeters, horizonMeters);
      double rad = Math.toRadians(s.closestBearingDeg);
      float cpx = cx + (float) (Math.sin(rad) * capped * metersToPx);
      float cpy = cy - (float) (Math.cos(rad) * capped * metersToPx);
      closestPx = new PointF(cpx, cpy);
    }

    if (closestPx != null) {
      canvas.drawLine(cx, cy, closestPx.x, closestPx.y, closestLinePaint);
      canvas.drawCircle(closestPx.x, closestPx.y, dp(3.5f), closestPaint);
      String label = String.format(Locale.getDefault(), "%.2f m",
          Math.min(s.closestMeters, horizonMeters));
      canvas.drawText(label, closestPx.x + dp(5f), closestPx.y - dp(5f), ringLabelPaint);
    }
  }

  private void drawGrid(Canvas canvas, float cx, float cy, float maxRadiusPx) {
    float[] ringMeters;
    if (horizonMeters >= 4.5f) {
      ringMeters = new float[]{1f, 2f, 3f, horizonMeters};
    } else if (horizonMeters >= 2.5f) {
      ringMeters = new float[]{1f, 2f, horizonMeters};
    } else {
      ringMeters = new float[]{1f, horizonMeters};
    }
    float metersToPx = maxRadiusPx / horizonMeters;
    for (float m : ringMeters) {
      canvas.drawCircle(cx, cy, m * metersToPx, ringPaint);
      canvas.drawText(
          String.format(Locale.getDefault(), "%.0f m", m),
          cx + dp(3f),
          cy - m * metersToPx + dp(10f),
          ringLabelPaint);
    }
    canvas.drawLine(cx, cy - maxRadiusPx, cx, cy + maxRadiusPx, crossPaint);
    canvas.drawLine(cx - maxRadiusPx, cy, cx + maxRadiusPx, cy, crossPaint);
  }

  private void drawRobot(Canvas canvas, float cx, float cy) {
    canvas.drawCircle(cx, cy, dp(3f), robotPaint);
    canvas.drawLine(cx, cy, cx, cy - dp(14f), headingPaint);
  }

  /**
   * Returns a colour ramp: red < 0.5 m, orange < 1.0 m, amber < 1.5 m, blue < 3.0 m,
   * green-blue beyond. Keeps the visual cue simple and readable at a glance.
   */
  private static int colorForDistance(double meters) {
    if (meters < 0.5d) return Color.parseColor("#D32F2F");
    if (meters < 1.0d) return Color.parseColor("#F57C00");
    if (meters < 1.5d) return Color.parseColor("#FBC02D");
    if (meters < 3.0d) return Color.parseColor("#1976D2");
    return Color.parseColor("#2E7D32");
  }

  private float dp(float px) {
    return px * getResources().getDisplayMetrics().density;
  }
}
