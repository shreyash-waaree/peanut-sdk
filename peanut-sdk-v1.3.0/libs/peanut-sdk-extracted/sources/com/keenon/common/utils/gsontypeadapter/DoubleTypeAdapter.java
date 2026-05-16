package com.keenon.common.utils.gsontypeadapter;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import com.keenon.common.constant.PeanutConstants;
import com.keenon.common.utils.LogUtils;
import java.io.IOException;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/common/utils/gsontypeadapter/DoubleTypeAdapter.class */
public class DoubleTypeAdapter extends TypeAdapter<Double> {
    private static final String TAG = "[DoubleTypeAdapter]";

    public void write(JsonWriter out, Double value) throws IOException {
        if (value == null) {
            try {
                value = Double.valueOf(0.0d);
            } catch (Exception e) {
                LogUtils.e(PeanutConstants.TAG_UTIL, TAG, e);
                return;
            }
        }
        out.value(value);
    }

    /* JADX INFO: renamed from: read, reason: merged with bridge method [inline-methods] */
    public Double m21read(JsonReader in) throws IOException {
        try {
            if (in.peek() == JsonToken.NULL) {
                in.nextNull();
                LogUtils.w(PeanutConstants.TAG_UTIL, "[DoubleTypeAdapter][null is not a number]");
                return Double.valueOf(0.0d);
            }
            if (in.peek() == JsonToken.BOOLEAN) {
                in.nextBoolean();
                LogUtils.w(PeanutConstants.TAG_UTIL, "[DoubleTypeAdapter][null is not a number]");
                return Double.valueOf(0.0d);
            }
            if (in.peek() == JsonToken.STRING) {
                String str = in.nextString();
                if (NumberUtils.isFloatOrDouble(str)) {
                    return Double.valueOf(Double.parseDouble(str));
                }
                LogUtils.w(PeanutConstants.TAG_UTIL, "[DoubleTypeAdapter][null is not a number]");
                return Double.valueOf(0.0d);
            }
            Double value = Double.valueOf(in.nextDouble());
            return Double.valueOf(value == null ? 0.0d : value.doubleValue());
        } catch (NumberFormatException e) {
            LogUtils.e(PeanutConstants.TAG_UTIL, TAG, (Exception) e);
            return Double.valueOf(0.0d);
        } catch (Exception e2) {
            LogUtils.e(PeanutConstants.TAG_UTIL, TAG, e2);
            return Double.valueOf(0.0d);
        }
    }
}
