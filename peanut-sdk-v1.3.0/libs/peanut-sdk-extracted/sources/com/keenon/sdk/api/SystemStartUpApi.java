package com.keenon.sdk.api;

import com.keenon.common.constant.PeanutConstants;
import com.keenon.common.utils.GsonUtil;
import com.keenon.common.utils.LogUtils;
import com.keenon.sdk.coap.adapter.CoapCommond;
import com.keenon.sdk.coap.adapter.CoapResponse;
import com.keenon.sdk.external.IDataCallback;
import com.keenon.sdk.external.PeanutSDK;
import com.keenon.sdk.hedera.model.ApiCallback;
import com.keenon.sdk.hedera.model.ApiData;
import com.keenon.sdk.hedera.model.ApiError;
import com.keenon.sdk.hedera.model.RequestEnum;
import com.keenon.sdk.proxy.sender.SenderManager;
import com.keenon.sdk.proxy.sender.anno.LinkAdapter;
import com.keenon.sdk.proxy.sender.anno.RequestType;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/SystemStartUpApi.class */
@LinkAdapter
@CoapCommond(path = "/runtime/startup", requestType = RequestEnum.GET)
public class SystemStartUpApi {
    private IDataCallback callBack;
    private RequestEnum requestEnum = RequestEnum.GET;

    @CoapResponse
    ApiCallback coapCallback = new ApiCallback<String>() { // from class: com.keenon.sdk.api.SystemStartUpApi.1
        @Override // com.keenon.sdk.hedera.model.ApiCallback, com.keenon.sdk.hedera.model.ICallback
        public void onSuccess(String result) {
            LogUtils.i(PeanutConstants.TAG_API, "[SystemStartUpApi][onSuccess: " + result + "]");
            if (result == null) {
                return;
            }
            Bean bean = (Bean) GsonUtil.gson2Bean(result, Bean.class);
            String jsonResult = GsonUtil.bean2String(bean);
            LogUtils.d(PeanutConstants.TAG_API, "[SystemStartUpApi][onSuccess json: " + jsonResult + "]");
            if (SystemStartUpApi.this.callBack == null) {
                return;
            }
            if (SystemStartUpApi.this.requestEnum == RequestEnum.GET) {
                SystemStartUpApi.this.callBack.success(jsonResult);
            } else if (SystemStartUpApi.this.requestEnum == RequestEnum.OBSERVER) {
                PeanutSDK.getInstance().notifyApiSuccess(this, jsonResult);
            }
        }

        @Override // com.keenon.sdk.hedera.model.ApiCallback, com.keenon.sdk.hedera.model.ICallback
        public void onFail(ApiError error) {
            if (SystemStartUpApi.this.callBack == null) {
                return;
            }
            if (SystemStartUpApi.this.requestEnum == RequestEnum.GET) {
                SystemStartUpApi.this.callBack.error(error);
            } else if (SystemStartUpApi.this.requestEnum == RequestEnum.OBSERVER) {
                PeanutSDK.getInstance().notifyApiError(this, error);
            }
        }
    };

    /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/SystemStartUpApi$Bean.class */
    public static class Bean extends ApiData {
        public DataBean data;

        /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/SystemStartUpApi$Bean$DataBean.class */
        public static class DataBean {
            public int status;
        }
    }

    @RequestType
    public RequestEnum requestType() {
        return this.requestEnum;
    }

    public void send(IDataCallback callBack) {
        this.callBack = callBack;
        SenderManager.getInstance().send(this);
    }

    public void observe(IDataCallback callBack) {
        this.requestEnum = RequestEnum.OBSERVER;
        send(callBack);
    }

    public void cancel() {
        SenderManager.getInstance().cancel(this);
    }
}
