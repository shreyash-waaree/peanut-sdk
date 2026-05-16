package com.keenon.sdk.hedera.model;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/hedera/model/ApiError.class */
public class ApiError {
    public int mid;
    public int code;
    public String msg;
    public String tag;

    public ApiError() {
    }

    public ApiError(ApiCode code) {
        this.code = code.code;
        this.msg = code.msg;
    }

    public int getMid() {
        return this.mid;
    }

    public void setMid(int mid) {
        this.mid = mid;
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

    public String getTag() {
        return this.tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public String toString() {
        return "ApiError{mid=" + this.mid + ", code=" + this.code + ", msg='" + this.msg + "', tag='" + this.tag + "'}";
    }
}
