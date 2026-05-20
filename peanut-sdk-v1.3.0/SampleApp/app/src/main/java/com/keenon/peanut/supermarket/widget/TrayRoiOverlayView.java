package com.keenon.peanut.supermarket.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Transparent overlay on the camera preview for oval tray ROI (4 taps) and optional slot guides.
 * Used by Count3 ({@code setSlotRects}) and Count2 ({@code setSlotGrid}).
 */
public class TrayRoiOverlayView extends View {

    public enum Mode { EDITING, LOCKED }
    public enum TapStep { TOP, BOTTOM, LEFT, RIGHT, DONE }

    public static final class Ellipse {
        public final float cx, cy, rx, ry;
        public Ellipse(float cx, float cy, float rx, float ry) {
            this.cx = cx; this.cy = cy; this.rx = rx; this.ry = ry;
        }
    }

    public interface Listener {
        void onStepChanged(TapStep step);
        void onEllipseReady(Ellipse ellipse);
    }

    private Mode mode = Mode.EDITING;
    private TapStep step = TapStep.TOP;

    @Nullable private Float tapTopX;
    @Nullable private Float tapTopY;
    @Nullable private Float tapBottomX;
    @Nullable private Float tapBottomY;
    @Nullable private Float tapLeftX;
    @Nullable private Float tapLeftY;
    @Nullable private Float tapRightX;
    @Nullable private Float tapRightY;

    @Nullable private Ellipse ellipse;
    @Nullable private Listener listener;

    private final List<RectF> slotRects = new ArrayList<>();
    private int slotGridRows;
    private int slotGridCols;

    private final Paint paintDim;
    private final Paint paintDot;
    private final Paint paintEllipse;
    private final Paint paintEllipseLocked;
    private final Paint paintGuide;
    private final Paint paintSlot;
    private final Paint paintGrid;

    public TrayRoiOverlayView(Context context) { this(context, null); }
    public TrayRoiOverlayView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setBackgroundColor(Color.TRANSPARENT);
        setClickable(true);

        paintDim = new Paint();
        paintDim.setColor(Color.argb(70, 0, 0, 0));

        paintDot = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintDot.setColor(Color.parseColor("#F59E0B"));
        paintDot.setStyle(Paint.Style.FILL);

        paintEllipse = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintEllipse.setColor(Color.parseColor("#F59E0B"));
        paintEllipse.setStyle(Paint.Style.STROKE);
        paintEllipse.setStrokeWidth(4f);

        paintEllipseLocked = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintEllipseLocked.setColor(Color.parseColor("#88F59E0B"));
        paintEllipseLocked.setStyle(Paint.Style.STROKE);
        paintEllipseLocked.setStrokeWidth(3f);

        paintGuide = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintGuide.setColor(Color.parseColor("#80FFFFFF"));
        paintGuide.setStyle(Paint.Style.STROKE);
        paintGuide.setStrokeWidth(1.5f);

        paintSlot = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintSlot.setColor(Color.parseColor("#CC2196F3"));
        paintSlot.setStyle(Paint.Style.STROKE);
        paintSlot.setStrokeWidth(2.5f);

        paintGrid = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintGrid.setColor(Color.parseColor("#66FFFFFF"));
        paintGrid.setStyle(Paint.Style.STROKE);
        paintGrid.setStrokeWidth(1.2f);
    }

    /** Detected slot boxes in view coordinates (Count3). Pass null to clear. */
    public void setSlotRects(@Nullable List<RectF> rects) {
        slotRects.clear();
        if (rects != null) {
            slotRects.addAll(rects);
        }
        invalidate();
    }

    /** Uniform rows×cols guide inside oval AABB (Count2). */
    public void setSlotGrid(int rows, int cols) {
        this.slotGridRows = Math.max(0, rows);
        this.slotGridCols = Math.max(0, cols);
        invalidate();
    }

    public void setListener(@Nullable Listener listener) { this.listener = listener; }

    public Mode getMode() { return mode; }
    public TapStep getStep() { return step; }
    @Nullable public Ellipse getEllipse() { return ellipse; }

    public void resetTaps() {
        tapTopX = tapTopY = tapBottomX = tapBottomY = null;
        tapLeftX = tapLeftY = tapRightX = tapRightY = null;
        ellipse = null;
        slotRects.clear();
        slotGridRows = slotGridCols = 0;
        step = TapStep.TOP;
        mode = Mode.EDITING;
        invalidate();
        if (listener != null) listener.onStepChanged(step);
    }

    public void lockEllipse() {
        if (ellipse == null) return;
        mode = Mode.LOCKED;
        invalidate();
    }

    public void restoreEllipse(Ellipse e, boolean locked) {
        this.ellipse = e;
        this.mode = locked ? Mode.LOCKED : Mode.EDITING;
        this.step = TapStep.DONE;
        this.tapTopX = e.cx;
        this.tapTopY = e.cy - e.ry;
        this.tapBottomX = e.cx;
        this.tapBottomY = e.cy + e.ry;
        this.tapLeftX = e.cx - e.rx;
        this.tapLeftY = e.cy;
        this.tapRightX = e.cx + e.rx;
        this.tapRightY = e.cy;
        invalidate();
    }

    public void clear() {
        resetTaps();
        ellipse = null;
        mode = Mode.EDITING;
        invalidate();
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (mode != Mode.EDITING) return false;
        if (ev.getActionMasked() != MotionEvent.ACTION_DOWN) return true;
        float x = ev.getX();
        float y = ev.getY();
        switch (step) {
            case TOP:
                tapTopX = x;
                tapTopY = y;
                step = TapStep.BOTTOM;
                break;
            case BOTTOM:
                tapBottomX = x;
                tapBottomY = y;
                step = TapStep.LEFT;
                break;
            case LEFT:
                tapLeftX = x;
                tapLeftY = y;
                step = TapStep.RIGHT;
                break;
            case RIGHT:
                tapRightX = x;
                tapRightY = y;
                step = TapStep.DONE;
                ellipse = computeEllipse();
                if (listener != null && ellipse != null) listener.onEllipseReady(ellipse);
                break;
            case DONE:
                break;
        }
        invalidate();
        if (listener != null) listener.onStepChanged(step);
        return true;
    }

    @Nullable
    private Ellipse computeEllipse() {
        if (tapTopX == null || tapTopY == null || tapBottomX == null || tapBottomY == null
                || tapLeftX == null || tapLeftY == null || tapRightX == null || tapRightY == null) {
            return null;
        }
        float left   = Math.min(tapLeftX, tapRightX);
        float right  = Math.max(tapLeftX, tapRightX);
        float top    = Math.min(tapTopY, tapBottomY);
        float bottom = Math.max(tapTopY, tapBottomY);
        float cx = (left + right) / 2f;
        float cy = (top + bottom) / 2f;
        float rx = Math.max(Math.abs(tapRightX - cx), Math.abs(cx - tapLeftX));
        float ry = Math.max(Math.abs(tapBottomY - cy), Math.abs(cy - tapTopY));
        if (rx < 8f || ry < 8f) return null;
        return new Ellipse(cx, cy, rx, ry);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();
        if (w == 0 || h == 0) return;

        if (mode == Mode.LOCKED) {
            if (ellipse != null) {
                float ex0 = ellipse.cx - ellipse.rx;
                float ey0 = ellipse.cy - ellipse.ry;
                float ex1 = ellipse.cx + ellipse.rx;
                float ey1 = ellipse.cy + ellipse.ry;
                canvas.drawOval(ex0, ey0, ex1, ey1, paintEllipseLocked);
                if (slotGridRows > 1 && slotGridCols > 1) {
                    float gw = ex1 - ex0;
                    float gh = ey1 - ey0;
                    for (int c = 1; c < slotGridCols; c++) {
                        float x = ex0 + gw * c / (float) slotGridCols;
                        canvas.drawLine(x, ey0, x, ey1, paintGrid);
                    }
                    for (int r = 1; r < slotGridRows; r++) {
                        float y = ey0 + gh * r / (float) slotGridRows;
                        canvas.drawLine(ex0, y, ex1, y, paintGrid);
                    }
                }
            }
            for (RectF slot : slotRects) {
                canvas.drawRect(slot, paintSlot);
            }
            return;
        }

        canvas.drawRect(0, 0, w, h, paintDim);
        canvas.drawLine(w / 2f, 0, w / 2f, h, paintGuide);
        canvas.drawLine(0, h / 2f, w, h / 2f, paintGuide);

        if (tapTopX != null && tapTopY != null) {
            canvas.drawCircle(tapTopX, tapTopY, 14f, paintDot);
        }
        if (tapBottomX != null && tapBottomY != null) {
            canvas.drawCircle(tapBottomX, tapBottomY, 14f, paintDot);
        }
        if (tapLeftX != null && tapLeftY != null) {
            canvas.drawCircle(tapLeftX, tapLeftY, 14f, paintDot);
        }
        if (tapRightX != null && tapRightY != null) {
            canvas.drawCircle(tapRightX, tapRightY, 14f, paintDot);
        }

        if (ellipse != null) {
            canvas.drawOval(ellipse.cx - ellipse.rx, ellipse.cy - ellipse.ry,
                    ellipse.cx + ellipse.rx, ellipse.cy + ellipse.ry,
                    paintEllipse);
        }
    }
}
