package com.keenon.common.utils.gsontypeadapter;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import com.keenon.common.constant.PeanutConstants;
import com.keenon.common.utils.LogUtils;
import java.io.IOException;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/common/utils/gsontypeadapter/StringTypeAdapter.class */
public class StringTypeAdapter extends TypeAdapter<String> {
    private static final String TAG = "[StringTypeAdapter]";

    public void write(JsonWriter out, String value) throws IOException {
        try {
            if (value == null) {
                out.nullValue();
            } else {
                out.value(value);
            }
        } catch (Exception e) {
            LogUtils.e(PeanutConstants.TAG_UTIL, TAG, e);
        }
    }

    /* JADX INFO: renamed from: read, reason: merged with bridge method [inline-methods] */
    public String m26read(JsonReader in) throws IOException {
        try {
            if (in.peek() == JsonToken.NULL) {
                in.nextNull();
                LogUtils.w(PeanutConstants.TAG_UTIL, "[StringTypeAdapter][null is not a string]");
                return "";
            }
        } catch (Exception e) {
            LogUtils.e(PeanutConstants.TAG_UTIL, TAG, e);
        }
        return in.nextString();
    }
}
