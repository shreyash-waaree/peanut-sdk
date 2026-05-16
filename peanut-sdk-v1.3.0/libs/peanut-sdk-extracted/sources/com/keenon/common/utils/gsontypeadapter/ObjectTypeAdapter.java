package com.keenon.common.utils.gsontypeadapter;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import com.keenon.common.constant.PeanutConstants;
import com.keenon.common.utils.GsonUtil;
import com.keenon.common.utils.LogUtils;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/common/utils/gsontypeadapter/ObjectTypeAdapter.class */
public class ObjectTypeAdapter extends TypeAdapter<Object> {
    private static final String TAG = "[ObjectTypeAdapter]";
    private final TypeAdapter<Object> delegate = GsonUtil.getGson().getAdapter(Object.class);

    public void write(JsonWriter out, Object value) throws IOException {
        this.delegate.write(out, value);
    }

    /* JADX INFO: renamed from: com.keenon.common.utils.gsontypeadapter.ObjectTypeAdapter$1, reason: invalid class name */
    /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/common/utils/gsontypeadapter/ObjectTypeAdapter$1.class */
    static /* synthetic */ class AnonymousClass1 {
        static final /* synthetic */ int[] $SwitchMap$com$google$gson$stream$JsonToken = new int[JsonToken.values().length];

        static {
            try {
                $SwitchMap$com$google$gson$stream$JsonToken[JsonToken.BEGIN_ARRAY.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$com$google$gson$stream$JsonToken[JsonToken.BEGIN_OBJECT.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                $SwitchMap$com$google$gson$stream$JsonToken[JsonToken.STRING.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
            try {
                $SwitchMap$com$google$gson$stream$JsonToken[JsonToken.NUMBER.ordinal()] = 4;
            } catch (NoSuchFieldError e4) {
            }
            try {
                $SwitchMap$com$google$gson$stream$JsonToken[JsonToken.BOOLEAN.ordinal()] = 5;
            } catch (NoSuchFieldError e5) {
            }
            try {
                $SwitchMap$com$google$gson$stream$JsonToken[JsonToken.NULL.ordinal()] = 6;
            } catch (NoSuchFieldError e6) {
            }
        }
    }

    public Object read(JsonReader in) throws IOException {
        JsonToken token = in.peek();
        switch (AnonymousClass1.$SwitchMap$com$google$gson$stream$JsonToken[token.ordinal()]) {
            case 1:
                List<Object> list = new ArrayList<>();
                in.beginArray();
                while (in.hasNext()) {
                    list.add(read(in));
                }
                in.endArray();
                return list;
            case 2:
                Map<String, Object> map = new HashMap<>();
                in.beginObject();
                while (in.hasNext()) {
                    map.put(in.nextName(), read(in));
                }
                in.endObject();
                return map;
            case 3:
                return in.nextString();
            case 4:
                double dbNum = in.nextDouble();
                if (dbNum > 9.223372036854776E18d) {
                    return Double.valueOf(dbNum);
                }
                int intNum = (int) dbNum;
                if (intNum == dbNum) {
                    return Integer.valueOf(intNum);
                }
                long lngNum = (long) dbNum;
                if (dbNum == lngNum) {
                    return Long.valueOf(lngNum);
                }
                return Double.valueOf(dbNum);
            case 5:
                return Boolean.valueOf(in.nextBoolean());
            case 6:
                in.nextNull();
                return null;
            default:
                LogUtils.e(PeanutConstants.TAG_UTIL, "[ObjectTypeAdapter][illegal type]");
                throw new IllegalStateException();
        }
    }
}
