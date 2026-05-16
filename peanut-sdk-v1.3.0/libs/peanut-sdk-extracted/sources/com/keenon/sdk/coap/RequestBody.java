package com.keenon.sdk.coap;

import android.util.Base64;
import com.keenon.sdk.hedera.model.MsgType;
import com.keenon.sdk.hedera.model.RequestEnum;
import org.eclipse.californium.core.coap.CoAP;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/coap/RequestBody.class */
public class RequestBody {
    private String uri;
    private String path;
    private byte[] requestParams;
    private RequestEnum type;
    private CoAP.Type coapType;
    private Integer acceptId;
    private String tag;

    public RequestBody(String uri, String path, RequestEnum type, MsgType msgType) {
        this.uri = uri;
        this.path = path;
        this.type = type;
        this.coapType = CoAP.Type.valueOf(msgType.value);
    }

    public String getUri() {
        return this.uri + this.path;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getPath() {
        return this.path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getTag() {
        return this.tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public byte[] getRequestParams() {
        return this.requestParams;
    }

    public void setRequestParams(String jsonParams) {
        this.requestParams = jsonParams.getBytes(CoAP.UTF8_CHARSET);
    }

    public void setParamsBase64(String content) {
        this.requestParams = Base64.decode(content, 0);
    }

    public void setRequestParams(byte[] data) {
        this.requestParams = data;
    }

    public RequestEnum getType() {
        return this.type;
    }

    public void setType(RequestEnum type) {
        this.type = type;
    }

    public CoAP.Type getCoapType() {
        return this.coapType;
    }

    public void setCoapType(CoAP.Type coapType) {
        this.coapType = coapType;
    }

    public Integer getAcceptId() {
        return this.acceptId;
    }

    public void setAcceptId(Integer acceptId) {
        this.acceptId = acceptId;
    }
}
