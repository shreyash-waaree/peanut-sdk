package com.keenon.common.utils.gsontypeadapter;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import com.keenon.common.constant.PeanutConstants;
import com.keenon.common.utils.LogUtils;
import java.io.IOException;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/common/utils/gsontypeadapter/LongTypeAdapter.class */
public class LongTypeAdapter extends TypeAdapter<Long> {
    private static final String TAG = "[LongTypeAdapter]";

    public void write(JsonWriter out, Long value) throws IOException {
        if (value == null) {
            try {
                value = 0L;
            } catch (Exception e) {
                LogUtils.e(PeanutConstants.TAG_UTIL, TAG, e);
                return;
            }
        }
        out.value(value);
    }

    /* JADX INFO: renamed from: read, reason: merged with bridge method [inline-methods] */
    public Long m24read(JsonReader in) throws IOException {
        try {
            if (in.peek() == JsonToken.NULL) {
                in.nextNull();
                LogUtils.w(PeanutConstants.TAG_UTIL, "[LongTypeAdapter][null is not a number]");
                return 0L;
            }
            if (in.peek() == JsonToken.BOOLEAN) {
                in.nextBoolean();
                LogUtils.w(PeanutConstants.TAG_UTIL, "[LongTypeAdapter][null is not a number]");
                return 0L;
            }
            if (in.peek() == JsonToken.STRING) {
                String str = in.nextString();
                if (NumberUtils.isIntOrLong(str)) {
                    return Long.valueOf(Long.parseLong(str));
                }
                LogUtils.w(PeanutConstants.TAG_UTIL, "[LongTypeAdapter][null is not a number]");
                return 0L;
            }
            Long value = Long.valueOf(in.nextLong());
            return value;
        } catch (Exception e) {
            LogUtils.e(PeanutConstants.TAG_UTIL, TAG, e);
            return 0L;
        }
    }
}
