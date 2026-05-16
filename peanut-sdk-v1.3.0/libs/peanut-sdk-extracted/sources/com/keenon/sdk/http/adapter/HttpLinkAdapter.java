package com.keenon.sdk.http.adapter;

import com.keenon.common.constant.PeanutConstants;
import com.keenon.common.external.PeanutConfig;
import com.keenon.common.utils.LogUtils;
import com.keenon.sdk.hedera.base.ILinkAdapter;
import com.keenon.sdk.hedera.model.ICallback;
import com.keenon.sdk.hedera.model.RequestEnum;
import com.keenon.sdk.http.HttpRequestBody;
import com.keenon.sdk.http.OkHttpFactory;
import com.keenon.sdk.http.PeanutHttpClient;
import com.keenon.sdk.http.ws.PushManager;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/http/adapter/HttpLinkAdapter.class */
public class HttpLinkAdapter implements ILinkAdapter {
    private static final String TAG = "[HttpLinkAdapter]";
    private String baseUrl = getUrl();
    private String url;
    private String path;
    private String tag;
    private String requestParams;
    private long timeout;
    private RequestEnum requestType;
    private long interval;
    private ICallback callBack;

    public HttpLinkAdapter() {
        PushManager.getInstance().init(PeanutConfig.getContext(), OkHttpFactory.getInstance().getClient());
    }

    private String getUrl() {
        return PeanutConstants.HTTP_PROTOCOL + PeanutConfig.getLinkIP() + ":" + PeanutConstants.HTTP_PORT;
    }

    public void setRequestParams(String requestParams) {
        this.requestParams = requestParams;
    }

    public void setRequest(HttpCommand command) {
        this.path = command.path();
        this.url = this.baseUrl;
        this.tag = this.path;
        this.requestType = command.requestType();
        this.interval = command.interval();
    }

    private boolean isInvalidCommand() {
        if (this.path == null || this.tag == null || this.requestType == null) {
            return true;
        }
        return false;
    }

    public void setRequestType(RequestEnum requestType) {
        this.requestType = requestType;
    }

    public void setCallBack(ICallback callBack) {
        this.callBack = callBack;
    }

    public HttpRequestBody buildRequest() {
        HttpRequestBody body = new HttpRequestBody(this.url, this.path, this.requestType);
        body.setTag(this.path);
        body.setInterval(this.interval);
        body.setParams(this.requestParams);
        return body;
    }

    @Override // com.keenon.sdk.hedera.base.ILinkAdapter
    public void reset() {
        this.requestParams = null;
        this.path = null;
        this.tag = null;
        this.requestType = null;
        this.callBack = null;
    }

    @Override // com.keenon.sdk.hedera.base.ILinkAdapter
    public void release() {
        LogUtils.i(PeanutConstants.TAG_SDK, "[HttpLinkAdapter][release]");
        reset();
        PushManager.getInstance().release();
    }

    @Override // com.keenon.sdk.hedera.base.ILinkAdapter
    public void adapt(boolean cancel) {
        LogUtils.v(PeanutConstants.TAG_SDK, "[HttpLinkAdapter][adapt]");
        if (isInvalidCommand()) {
            LogUtils.w(PeanutConstants.TAG_SDK, "[HttpLinkAdapter][adapt][invalid]");
            return;
        }
        if (cancel) {
            cancelSubscribeByTag();
            return;
        }
        HttpRequestBody requestBody = buildRequest();
        if (requestBody == null) {
            LogUtils.w(PeanutConstants.TAG_SDK, "[HttpLinkAdapter][adapt][null request]");
        } else {
            PeanutHttpClient.getInstance().send(requestBody, this.callBack);
        }
    }

    private void cancelSubscribeByTag() {
        if (this.tag != null && !this.tag.isEmpty()) {
            LogUtils.d(PeanutConstants.TAG_SDK, "[HttpLinkAdapter][cancelSubscribeByTag]");
            PeanutHttpClient.getInstance().cancelObserver(this.tag);
        }
    }
}
