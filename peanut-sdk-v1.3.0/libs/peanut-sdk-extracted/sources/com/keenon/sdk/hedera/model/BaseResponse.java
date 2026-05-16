package com.keenon.sdk.hedera.model;

import com.google.gson.Gson;
import com.keenon.common.utils.GsonUtil;
import java.io.Serializable;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/hedera/model/BaseResponse.class */
public class BaseResponse<T> implements Serializable {
    private int status;
    private int code;
    private String msg;
    private String topic;
    private T data;

    public int getStatus() {
        return this.status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public int getCode() {
        return this.code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMsg() {
        return this.msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public String getTopic() {
        return this.topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public T getData() {
        return this.data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public static BaseResponse getSelf(String jsonString) {
        BaseResponse dataBean = (BaseResponse) GsonUtil.gson2Bean(jsonString, BaseResponse.class);
        return dataBean;
    }

    public static <T> List<T> getList(String jsonString, Type typeOfT) {
        String data = GsonUtil.getData(jsonString);
        return GsonUtil.gson2List(data, typeOfT);
    }

    public static String getDataStr(String jsonString) {
        return GsonUtil.getData(jsonString);
    }

    public static <T> T getData(String str, Class<T> cls) {
        return (T) fromJson(str, cls).getData();
    }

    public static BaseResponse fromJson(String jsonString, Class clazz) {
        Gson gson = new Gson();
        Type objectType = type(BaseResponse.class, clazz);
        return (BaseResponse) gson.fromJson(jsonString, objectType);
    }

    static ParameterizedType type(final Class raw, final Type... args) {
        return new ParameterizedType() { // from class: com.keenon.sdk.hedera.model.BaseResponse.1
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
