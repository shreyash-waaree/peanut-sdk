package com.keenon.sdk.http;

import com.keenon.sdk.hedera.model.RequestEnum;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/http/HttpRequestBody.class */
public class HttpRequestBody {
    private String uri;
    private String path;
    private String params;
    private RequestEnum method;
    private long interval;
    private String tag;

    public HttpRequestBody(String uri, String path, RequestEnum method) {
        this.uri = uri;
        this.path = path;
        this.method = method;
    }

    public String getUri() {
        return this.uri + this.path;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getTag() {
        return this.tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public String getParams() {
        return this.params;
    }

    public void setParams(String jsonParams) {
        this.params = jsonParams;
    }

    public RequestEnum getMethod() {
        return this.method;
    }

    public void setMethod(RequestEnum method) {
        this.method = method;
    }

    public String getPath() {
        return this.path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public long getInterval() {
        return this.interval;
    }

    public void setInterval(long interval) {
        this.interval = interval;
    }
}
