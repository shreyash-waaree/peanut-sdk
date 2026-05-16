package com.keenon.common.utils.gsontypeadapter;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import com.keenon.common.constant.PeanutConstants;
import com.keenon.common.utils.LogUtils;
import java.io.IOException;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/common/utils/gsontypeadapter/FloatTypeAdapter.class */
public class FloatTypeAdapter extends TypeAdapter<Float> {
    private static final String TAG = "[FloatTypeAdapter]";

    public void write(JsonWriter out, Float value) throws IOException {
        if (value == null) {
            try {
                value = Float.valueOf(0.0f);
            } catch (Exception e) {
                LogUtils.e(PeanutConstants.TAG_UTIL, TAG, e);
                return;
            }
        }
        out.value(value);
    }

    /* JADX INFO: renamed from: read, reason: merged with bridge method [inline-methods] */
    public Float m22read(JsonReader in) throws IOException {
        try {
            if (in.peek() == JsonToken.NULL) {
                in.nextNull();
                LogUtils.w(PeanutConstants.TAG_UTIL, "[FloatTypeAdapter][null is not a number]");
                return Float.valueOf(0.0f);
            }
            if (in.peek() == JsonToken.BOOLEAN) {
                in.nextBoolean();
                LogUtils.w(PeanutConstants.TAG_UTIL, "[FloatTypeAdapter][null is not a number]");
                return Float.valueOf(0.0f);
            }
            if (in.peek() == JsonToken.STRING) {
                String str = in.nextString();
                if (NumberUtils.isFloatOrDouble(str)) {
                    return Float.valueOf(Float.parseFloat(str));
                }
                LogUtils.w(PeanutConstants.TAG_UTIL, "[FloatTypeAdapter][null is not a number]");
                return Float.valueOf(0.0f);
            }
            Float value = Float.valueOf(in.nextString());
            return value;
        } catch (Exception e) {
            LogUtils.e(PeanutConstants.TAG_UTIL, TAG, e);
            return Float.valueOf(0.0f);
        }
    }
}
