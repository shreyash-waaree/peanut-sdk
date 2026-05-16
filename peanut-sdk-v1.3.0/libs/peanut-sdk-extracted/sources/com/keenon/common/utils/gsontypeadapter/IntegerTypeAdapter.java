package com.keenon.common.utils.gsontypeadapter;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import com.keenon.common.constant.PeanutConstants;
import com.keenon.common.utils.LogUtils;
import java.io.IOException;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/common/utils/gsontypeadapter/IntegerTypeAdapter.class */
public class IntegerTypeAdapter extends TypeAdapter<Integer> {
    private static final String TAG = "[IntegerTypeAdapter]";

    public void write(JsonWriter out, Integer value) throws IOException {
        if (value == null) {
            try {
                value = 0;
            } catch (Exception e) {
                LogUtils.e(PeanutConstants.TAG_UTIL, TAG, e);
                return;
            }
        }
        out.value(value);
    }

    /* JADX INFO: renamed from: read, reason: merged with bridge method [inline-methods] */
    public Integer m23read(JsonReader in) throws IOException {
        try {
            if (in.peek() == JsonToken.NULL) {
                in.nextNull();
                LogUtils.w(PeanutConstants.TAG_UTIL, "[IntegerTypeAdapter][null is not a number]");
                return 0;
            }
            if (in.peek() == JsonToken.BOOLEAN) {
                in.nextBoolean();
                LogUtils.w(PeanutConstants.TAG_UTIL, "[IntegerTypeAdapter][null is not a number]");
                return 0;
            }
            if (in.peek() == JsonToken.STRING) {
                String str = in.nextString();
                if (NumberUtils.isIntOrLong(str)) {
                    return Integer.valueOf(Integer.parseInt(str));
                }
                LogUtils.w(PeanutConstants.TAG_UTIL, "[IntegerTypeAdapter][null is not a number]");
                return 0;
            }
            Integer value = Integer.valueOf(in.nextInt());
            return value;
        } catch (Exception e) {
            LogUtils.e(PeanutConstants.TAG_UTIL, TAG, e);
            return 0;
        }
    }
}
