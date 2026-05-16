package com.keenon.sdk.proxy.adapter;

import com.keenon.common.constant.PeanutConstants;
import com.keenon.common.external.PeanutConfig;
import com.keenon.common.utils.LogUtils;
import com.keenon.sdk.coap.PeanutCoapClient;
import com.keenon.sdk.coap.RequestBody;
import com.keenon.sdk.coap.adapter.CoapCommond;
import com.keenon.sdk.hedera.base.ILinkAdapter;
import com.keenon.sdk.hedera.model.ICallback;
import com.keenon.sdk.hedera.model.MsgType;
import com.keenon.sdk.hedera.model.RequestEnum;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/proxy/adapter/CoapLinkAdapter.class */
public class CoapLinkAdapter implements ILinkAdapter {
    private static final String TAG = "[CoapLinkAdapter]";
    private String url = PeanutConfig.getLinkUrl();
    private String path;
    private String tag;
    private String requestParams;
    private long timeout;
    private RequestEnum requestEnum;
    private MsgType msgType;
    private ICallback callBack;

    public CoapLinkAdapter() {
        PingTask.getInstance().start();
    }

    public void setRequestParams(String requestParams) {
        this.requestParams = requestParams;
    }

    public void setRequest(CoapCommond command) {
        this.path = command.path();
        this.tag = this.path;
        this.requestEnum = command.requestType();
        this.msgType = command.msgType();
        this.timeout = command.timeout();
    }

    private boolean isInvalidCommand() {
        if (this.path == null || this.tag == null || this.requestEnum == null || this.msgType == null) {
            return true;
        }
        return false;
    }

    public void setRequestEnum(RequestEnum requestEnum) {
        this.requestEnum = requestEnum;
    }

    public void setCallBack(ICallback callBack) {
        this.callBack = callBack;
    }

    public RequestBody buildRequest() {
        RequestBody body = new RequestBody(this.url, this.path, this.requestEnum, this.msgType);
        if (this.requestParams != null && !this.requestParams.isEmpty()) {
            if (this.requestEnum == RequestEnum.FILE) {
                body.setParamsBase64(this.requestParams);
            } else {
                body.setRequestParams(this.requestParams);
            }
        }
        body.setTag(this.path);
        return body;
    }

    private void cancelSubscribeByTag() {
        if (this.tag != null && !this.tag.isEmpty()) {
            LogUtils.d(PeanutConstants.TAG_SDK, "[CoapLinkAdapter][cancelSubscribeByTag][" + this.tag + "]");
            PeanutCoapClient.getInstance().cancelCoapObserver(this.tag);
        }
    }

    @Override // com.keenon.sdk.hedera.base.ILinkAdapter
    public void reset() {
        this.requestParams = null;
        this.path = null;
        this.tag = null;
        this.requestEnum = null;
        this.msgType = null;
        this.timeout = 60000L;
        this.callBack = null;
    }

    @Override // com.keenon.sdk.hedera.base.ILinkAdapter
    public void release() {
        LogUtils.i(PeanutConstants.TAG_SDK, "[CoapLinkAdapter][release]");
        reset();
        PeanutCoapClient.getInstance().release();
        PingTask.getInstance().stop();
    }

    @Override // com.keenon.sdk.hedera.base.ILinkAdapter
    public void adapt(boolean cancel) {
        if (isInvalidCommand()) {
            LogUtils.w(PeanutConstants.TAG_SDK, "[CoapLinkAdapter][adapt][invalid]");
        } else {
            if (cancel) {
                cancelSubscribeByTag();
                return;
            }
            logRequest();
            RequestBody body = buildRequest();
            PeanutCoapClient.getInstance().request(body, this.callBack);
        }
    }

    private void logRequest() {
        LogUtils.d(PeanutConstants.TAG_SDK, "[CoapLinkAdapter][adapt][url: " + this.url + ", path: " + this.path + ", type: " + this.requestEnum + ", msgType: " + this.msgType + ", params: " + this.requestParams + "]");
    }
}
