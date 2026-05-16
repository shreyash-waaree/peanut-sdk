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

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/RuntimeShootPresetApi.class */
@LinkAdapter
@CoapCommond(path = "/runtime/shootpreset")
public class RuntimeShootPresetApi {
    private IDataCallback callBack;
    private RequestEnum requestEnum = RequestEnum.GET;

    @CoapResponse
    ApiCallback coapCallback = new ApiCallback<String>() { // from class: com.keenon.sdk.api.RuntimeShootPresetApi.1
        @Override // com.keenon.sdk.hedera.model.ApiCallback, com.keenon.sdk.hedera.model.ICallback
        public void onSuccess(String result) {
            LogUtils.i(PeanutConstants.TAG_API, "[RuntimeShootPresetApi][onSuccess: " + result + "]");
            Bean bean = (Bean) GsonUtil.gson2Bean(result, Bean.class);
            bean.setTopic(getClass());
            String jsonResult = GsonUtil.bean2String(bean);
            LogUtils.d(PeanutConstants.TAG_API, "[RuntimeShootPresetApi][onSuccess json: " + jsonResult + "]");
            if (RuntimeShootPresetApi.this.callBack == null) {
                return;
            }
            if (RuntimeShootPresetApi.this.requestEnum == RequestEnum.GET) {
                RuntimeShootPresetApi.this.callBack.success(jsonResult);
            } else if (RuntimeShootPresetApi.this.requestEnum == RequestEnum.OBSERVER) {
                PeanutSDK.getInstance().notifyApiSuccess(this, jsonResult);
            }
        }

        @Override // com.keenon.sdk.hedera.model.ApiCallback, com.keenon.sdk.hedera.model.ICallback
        public void onFail(ApiError error) {
            if (RuntimeShootPresetApi.this.callBack == null) {
                return;
            }
            if (RuntimeShootPresetApi.this.requestEnum == RequestEnum.GET) {
                RuntimeShootPresetApi.this.callBack.error(error);
            } else if (RuntimeShootPresetApi.this.requestEnum == RequestEnum.OBSERVER) {
                PeanutSDK.getInstance().notifyApiError(this, error);
            }
        }
    };

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

    /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/RuntimeShootPresetApi$Bean.class */
    public static class Bean extends ApiData {
        private DataBean data;

        /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/RuntimeShootPresetApi$Bean$DataBean.class */
        public class DataBean {
            private long code;
            private String msg;
            private String time_stamp;

            public DataBean() {
            }

            public long getCode() {
                return this.code;
            }

            public void setCode(long code) {
                this.code = code;
            }

            public String getMsg() {
                return this.msg;
            }

            public void setMsg(String msg) {
                this.msg = msg;
            }

            public String getTime_stamp() {
                return this.time_stamp;
            }

            public void setTime_stamp(String time_stamp) {
                this.time_stamp = time_stamp;
            }
        }
    }
}
