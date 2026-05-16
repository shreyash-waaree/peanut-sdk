package com.keenon.sdk.base.model;

import android.graphics.Color;
import android.graphics.RectF;

import java.util.Random;

/**
 * Mirrors Keenon SDK {@code KNBox} so {@code libyolov5.so} JNI matches field names,
 * constructor {@code (FFFFIFFFFF)V}, and return type {@code KNBox[]} from
 * {@link com.keenon.sdk.yolo.KNYoloExecutor#detectData}.
 */
public class KNBox {
    public int label;
    private float score;
    public boolean show = true;
    public float x0;
    public float x0_value2;
    public float x1;
    public float x1_value4;
    public float y0;
    public float y0_value3;
    public float y1;
    public float y1_value5;

    @Deprecated
    public String getLabel() {
        return "";
    }

    public KNBox(float f, float f2, float f3, float f4, int i,
                 float f5, float f6, float f7, float f8, float f9) {
        this.x0 = f;
        this.y0 = f2;
        this.x1 = f3;
        this.y1 = f4;
        this.x0_value2 = f6;
        this.x1_value4 = f8;
        this.y0_value3 = f7;
        this.y1_value5 = f9;
        this.label = i;
        this.score = f5;
    }

    public RectF getRect() {
        return new RectF(this.x0, this.y0, this.x1, this.y1);
    }

    public float getScore() {
        return this.score;
    }

    public int getColor() {
        Random random = new Random(this.label);
        return Color.argb(255, random.nextInt(256), random.nextInt(256), random.nextInt(256));
    }
}
