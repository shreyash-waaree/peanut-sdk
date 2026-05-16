package com.keenon.common.utils.gsontypeadapter;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import com.keenon.common.constant.PeanutConstants;
import com.keenon.common.utils.LogUtils;
import java.io.IOException;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/common/utils/gsontypeadapter/BooleanTypeAdapter.class */
public class BooleanTypeAdapter extends TypeAdapter<Boolean> {
    private static final String TAG = "[BooleanTypeAdapter]";

    public void write(JsonWriter out, Boolean value) throws IOException {
        if (value == null) {
            try {
                value = false;
            } catch (Exception e) {
                LogUtils.e(PeanutConstants.TAG_UTIL, TAG, e);
                return;
            }
        }
        out.value(value);
    }

    /* JADX INFO: renamed from: read, reason: merged with bridge method [inline-methods] */
    public Boolean m20read(JsonReader in) throws IOException {
        try {
            if (in.peek() == JsonToken.NULL) {
                in.nextNull();
                LogUtils.w(PeanutConstants.TAG_UTIL, "[BooleanTypeAdapter][null is not a boolean]");
                return false;
            }
            if (in.peek() == JsonToken.NUMBER) {
                Double value = Double.valueOf(in.nextDouble());
                LogUtils.w(PeanutConstants.TAG_UTIL, "[BooleanTypeAdapter][null is not a boolean]");
                return Boolean.valueOf(value == Double.valueOf(1.0d));
            }
            if (in.peek() == JsonToken.STRING) {
                String str = in.nextString();
                if (NumberUtils.isFloatOrDouble(str)) {
                    return Boolean.valueOf(Double.valueOf(str) == Double.valueOf(1.0d));
                }
                LogUtils.w(PeanutConstants.TAG_UTIL, "[BooleanTypeAdapter][null is not a boolean]");
            }
            return Boolean.valueOf(in.nextBoolean());
        } catch (NumberFormatException e) {
            LogUtils.e(PeanutConstants.TAG_UTIL, TAG, (Exception) e);
            return false;
        } catch (Exception e2) {
            LogUtils.e(PeanutConstants.TAG_UTIL, TAG, e2);
            return false;
        }
    }
}
