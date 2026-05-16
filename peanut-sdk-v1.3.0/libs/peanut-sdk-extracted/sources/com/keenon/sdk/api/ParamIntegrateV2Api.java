package com.keenon.sdk.api;

import com.keenon.common.constant.PeanutConstants;
import com.keenon.common.utils.GsonUtil;
import com.keenon.common.utils.LogUtils;
import com.keenon.sdk.coap.adapter.CoapCommond;
import com.keenon.sdk.coap.adapter.CoapParams;
import com.keenon.sdk.coap.adapter.CoapResponse;
import com.keenon.sdk.coap.adapter.IProgressCallBack;
import com.keenon.sdk.external.IDataCallback;
import com.keenon.sdk.hedera.model.ApiData;
import com.keenon.sdk.hedera.model.ApiError;
import com.keenon.sdk.hedera.model.RequestEnum;
import com.keenon.sdk.proxy.sender.SenderManager;
import com.keenon.sdk.proxy.sender.anno.LinkAdapter;
import org.eclipse.californium.core.coap.Request;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/ParamIntegrateV2Api.class */
@LinkAdapter
@CoapCommond(path = "/param/integrate/v2", requestType = RequestEnum.POST_LARGE)
public class ParamIntegrateV2Api {
    private IDataCallback callBack;

    @CoapResponse
    IProgressCallBack coapCallback = new IProgressCallBack<String>() { // from class: com.keenon.sdk.api.ParamIntegrateV2Api.1
        @Override // com.keenon.sdk.hedera.model.ICallback
        public void onSuccess(String result) {
            LogUtils.i(PeanutConstants.TAG_API, "[ParamIntegrateV2Api][onSuccess: " + result + "]");
            Bean bean = (Bean) GsonUtil.gson2Bean(result, Bean.class);
            String jsonResult = GsonUtil.bean2String(bean);
            LogUtils.d(PeanutConstants.TAG_API, "[ParamIntegrateV2Api][onSuccess json: " + jsonResult + "]");
            if (ParamIntegrateV2Api.this.callBack == null) {
                return;
            }
            ParamIntegrateV2Api.this.callBack.success(jsonResult);
        }

        @Override // com.keenon.sdk.hedera.model.ICallback
        public void onFail(ApiError error) {
            if (ParamIntegrateV2Api.this.callBack == null) {
                return;
            }
            ParamIntegrateV2Api.this.callBack.error(error);
        }

        @Override // com.keenon.sdk.coap.adapter.IProgressCallBack
        public void readyToSend(Request request) {
        }

        @Override // com.keenon.sdk.coap.adapter.IProgressCallBack
        public void progress(int percent) {
        }
    };
    private String params;

    @CoapParams
    public String CoapParams() {
        return this.params;
    }

    public void send(IDataCallback callBack, String params) {
        this.callBack = callBack;
        this.params = params;
        SenderManager.getInstance().send(this);
    }

    /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/ParamIntegrateV2Api$Bean.class */
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

        /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/ParamIntegrateV2Api$Bean$DataBean.class */
        public class DataBean {
            private String errorStr;
            private String result;

            public DataBean() {
            }

            public String getResult() {
                return this.result;
            }

            public void setResult(String result) {
                this.result = result;
            }

            public String getErrorStr() {
                return this.errorStr;
            }

            public void setErrorStr(String errorStr) {
                this.errorStr = errorStr;
            }
        }
    }
}
