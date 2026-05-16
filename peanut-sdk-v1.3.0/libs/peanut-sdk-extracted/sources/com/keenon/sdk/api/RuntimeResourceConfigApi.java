package com.keenon.sdk.api;

import com.keenon.common.constant.PeanutConstants;
import com.keenon.common.utils.GsonUtil;
import com.keenon.common.utils.LogUtils;
import com.keenon.sdk.coap.adapter.CoapCommond;
import com.keenon.sdk.coap.adapter.CoapParams;
import com.keenon.sdk.coap.adapter.CoapResponse;
import com.keenon.sdk.external.IDataCallback;
import com.keenon.sdk.hedera.model.ApiCallback;
import com.keenon.sdk.hedera.model.ApiData;
import com.keenon.sdk.hedera.model.ApiError;
import com.keenon.sdk.hedera.model.RequestEnum;
import com.keenon.sdk.proxy.sender.SenderManager;
import com.keenon.sdk.proxy.sender.anno.LinkAdapter;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/RuntimeResourceConfigApi.class */
@LinkAdapter
@CoapCommond(path = "/runtime/resourceConfig", requestType = RequestEnum.POST)
public class RuntimeResourceConfigApi {
    private IDataCallback callBack;
    private String payloadJson;

    @CoapResponse
    ApiCallback coapCallback = new ApiCallback<String>() { // from class: com.keenon.sdk.api.RuntimeResourceConfigApi.1
        @Override // com.keenon.sdk.hedera.model.ApiCallback, com.keenon.sdk.hedera.model.ICallback
        public void onSuccess(String result) {
            LogUtils.i(PeanutConstants.TAG_API, "[RuntimeResourceConfigApi][onSuccess: " + result + "]");
            Bean bean = (Bean) GsonUtil.gson2Bean(result, Bean.class);
            String jsonResult = GsonUtil.bean2String(bean);
            LogUtils.d(PeanutConstants.TAG_API, "[RuntimeResourceConfigApi][onSuccess json: " + jsonResult + "]");
            if (RuntimeResourceConfigApi.this.callBack == null) {
                return;
            }
            RuntimeResourceConfigApi.this.callBack.success(jsonResult);
        }

        @Override // com.keenon.sdk.hedera.model.ApiCallback, com.keenon.sdk.hedera.model.ICallback
        public void onFail(ApiError error) {
            if (RuntimeResourceConfigApi.this.callBack == null) {
                return;
            }
            RuntimeResourceConfigApi.this.callBack.error(error);
        }
    };

    @CoapParams
    public String CoapParams() {
        return this.payloadJson;
    }

    public void send(IDataCallback callBack, String payloadJson) {
        this.callBack = callBack;
        this.payloadJson = payloadJson;
        SenderManager.getInstance().send(this);
    }

    /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/RuntimeResourceConfigApi$Bean.class */
    public class Bean extends ApiData {
        private DataBean data;

        public Bean() {
        }

        public DataBean getData() {
            return this.data;
        }

        public void setData(DataBean data) {
            this.data = data;
        }

        /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/RuntimeResourceConfigApi$Bean$DataBean.class */
        public class DataBean {
            private String result;

            public DataBean() {
            }

            public String getResult() {
                return this.result;
            }

            public void setResult(String result) {
                this.result = result;
            }
        }
    }
}
