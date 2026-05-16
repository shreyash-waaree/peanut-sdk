package com.keenon.common.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import com.keenon.common.constant.PeanutConstants;
import com.keenon.common.utils.gsontypeadapter.BooleanTypeAdapter;
import com.keenon.common.utils.gsontypeadapter.DoubleTypeAdapter;
import com.keenon.common.utils.gsontypeadapter.FloatTypeAdapter;
import com.keenon.common.utils.gsontypeadapter.IntegerTypeAdapter;
import com.keenon.common.utils.gsontypeadapter.LongTypeAdapter;
import com.keenon.common.utils.gsontypeadapter.StringTypeAdapter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import org.json.JSONException;
import org.json.JSONObject;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/common/utils/GsonUtil.class */
public class GsonUtil {
    private static final String TAG = "[GsonUtil]";
    private static Gson gson;

    static {
        gson = null;
        if (gson == null) {
            gson = new GsonBuilder().registerTypeAdapter(Integer.class, new IntegerTypeAdapter()).registerTypeAdapter(Integer.TYPE, new IntegerTypeAdapter()).registerTypeAdapter(Double.class, new DoubleTypeAdapter()).registerTypeAdapter(Double.TYPE, new DoubleTypeAdapter()).registerTypeAdapter(Long.class, new LongTypeAdapter()).registerTypeAdapter(Long.TYPE, new LongTypeAdapter()).registerTypeAdapter(Float.class, new FloatTypeAdapter()).registerTypeAdapter(Float.TYPE, new FloatTypeAdapter()).registerTypeAdapter(Boolean.class, new BooleanTypeAdapter()).registerTypeAdapter(Boolean.TYPE, new BooleanTypeAdapter()).registerTypeAdapter(String.class, new StringTypeAdapter()).create();
        }
    }

    private GsonUtil() {
    }

    public static Gson getGson() {
        return gson;
    }

    public static String bean2String(Object object) {
        String gsonString = null;
        if (gson != null) {
            gsonString = gson.toJson(object);
        }
        return gsonString;
    }

    public static <T> T gson2Bean(String str, Class<T> cls) {
        Object objFromJson = null;
        if (gson != null) {
            objFromJson = gson.fromJson(str, cls);
        }
        return (T) objFromJson;
    }

    public static <T> T gson2Bean(String str, Type type) {
        Object objFromJson = null;
        if (gson != null) {
            objFromJson = gson.fromJson(str, type);
        }
        return (T) objFromJson;
    }

    public static <T> List<T> gson2List(String gsonString, Type typeOfT) {
        List<T> list = null;
        if (gson != null) {
            list = (List) gson.fromJson(gsonString, typeOfT);
        }
        return list;
    }

    public static <T> List<Map<String, T>> gson2ListMaps(String str) {
        List<Map<String, T>> list = null;
        if (gson != null) {
            list = (List) gson.fromJson(str, new TypeToken<List<Map<String, T>>>() { // from class: com.keenon.common.utils.GsonUtil.1
            }.getType());
        }
        return list;
    }

    public static <T> Map<String, T> gson2Maps(String str) {
        Map<String, T> map = null;
        if (gson != null) {
            map = (Map) gson.fromJson(str, new TypeToken<Map<String, T>>() { // from class: com.keenon.common.utils.GsonUtil.2
            }.getType());
        }
        return map;
    }

    public static boolean isJson(String content) {
        try {
            JsonElement jsonElement = JsonParser.parseString(content);
            if (jsonElement == null || !jsonElement.isJsonObject()) {
                return false;
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static String getData(String jsonString) {
        if (jsonString != null) {
            try {
                JSONObject jsonObject = new JSONObject(jsonString);
                return jsonObject.optString("data");
            } catch (JSONException e) {
                LogUtils.e(PeanutConstants.TAG_UTIL, TAG, (Exception) e);
                return "";
            }
        }
        return "";
    }

    public static ParameterizedType type(final Class raw, final Type... args) {
        return new ParameterizedType() { // from class: com.keenon.common.utils.GsonUtil.3
            @Override // java.lang.reflect.ParameterizedType
            public Type getRawType() {
                return raw;
            }

            @Override // java.lang.reflect.ParameterizedType
            public Type[] getActualTypeArguments() {
                return args;
            }

            @Override // java.lang.reflect.ParameterizedType
            public Type getOwnerType() {
                return null;
            }
        };
    }
}
