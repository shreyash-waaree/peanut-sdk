package com.keenon.sdk.hedera.model;

import com.keenon.common.constant.PeanutConstants;
import com.keenon.common.utils.ReflectUtil;
import java.io.Serializable;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/hedera/model/ApiData.class */
public class ApiData implements Serializable {
    public static final int STATUS_SUCCESS = 0;
    private int status;
    private int code;
    private String msg;
    private String topic;

    public ApiData() {
        setCode(0);
        setStatus(0);
        setMsg(PeanutConstants.API_SUCCESS_MESSAGE);
    }

    public int getCode() {
        return this.code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public void setCode(Byte[] bytes) {
        if (bytes.length > 0) {
            setCode(bytes[0].byteValue() & 255);
        } else {
            setCode(1);
        }
    }

    public String getMsg() {
        return this.msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public int getStatus() {
        return this.status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public boolean isSuccess() {
        return this.status == 0;
    }

    public void setStatus(Byte[] bytes) {
        if (bytes.length > 0) {
            setStatus(bytes[0].byteValue() & 255);
        } else {
            setCode(1);
        }
    }

    public String getTopic() {
        return this.topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public void setTopic(Class clz) {
        setTopic(ReflectUtil.getClassName(clz));
    }
}
