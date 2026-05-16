package com.keenon.sdk.api;

import com.google.gson.annotations.SerializedName;
import com.keenon.common.constant.PeanutConstants;
import com.keenon.common.utils.GsonUtil;
import com.keenon.common.utils.LogUtils;
import com.keenon.sdk.coap.adapter.CoapCommond;
import com.keenon.sdk.coap.adapter.CoapResponse;
import com.keenon.sdk.external.IDataCallback;
import com.keenon.sdk.hedera.model.ApiCallback;
import com.keenon.sdk.hedera.model.ApiData;
import com.keenon.sdk.hedera.model.ApiError;
import com.keenon.sdk.proxy.sender.SenderManager;
import com.keenon.sdk.proxy.sender.anno.LinkAdapter;
import java.io.Serializable;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/RuntimetLocationApi.class */
@LinkAdapter
@CoapCommond(path = "/runtime/queryId")
public class RuntimetLocationApi {
    private IDataCallback callBack;

    @CoapResponse
    ApiCallback coapCallback = new ApiCallback<String>() { // from class: com.keenon.sdk.api.RuntimetLocationApi.1
        @Override // com.keenon.sdk.hedera.model.ApiCallback, com.keenon.sdk.hedera.model.ICallback
        public void onSuccess(String result) {
            LogUtils.i(PeanutConstants.TAG_API, "[RuntimetLocationApi][onSuccess: " + result + "]");
            Bean bean = (Bean) GsonUtil.gson2Bean(result, Bean.class);
            String jsonResult = GsonUtil.bean2String(bean);
            LogUtils.d(PeanutConstants.TAG_API, "[RuntimetLocationApi][onSuccess json: " + jsonResult + "]");
            if (RuntimetLocationApi.this.callBack == null) {
                return;
            }
            RuntimetLocationApi.this.callBack.success(jsonResult);
        }

        @Override // com.keenon.sdk.hedera.model.ApiCallback, com.keenon.sdk.hedera.model.ICallback
        public void onFail(ApiError error) {
            if (RuntimetLocationApi.this.callBack == null) {
                return;
            }
            RuntimetLocationApi.this.callBack.error(error);
        }
    };

    public void send(IDataCallback callBack) {
        this.callBack = callBack;
        SenderManager.getInstance().send(this);
    }

    /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/RuntimetLocationApi$Bean.class */
    public static class Bean extends ApiData {
        private DataBean data;

        public DataBean getData() {
            return this.data;
        }

        public void setData(DataBean data) {
            this.data = data;
        }

        /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/RuntimetLocationApi$Bean$DataBean.class */
        public class DataBean implements Serializable {

            @SerializedName("DstId")
            private int dstId;

            @SerializedName("DstName")
            private String dstName;

            public DataBean() {
            }

            public int getDstId() {
                return this.dstId;
            }

            public void setDstId(int dstId) {
                this.dstId = dstId;
            }

            public String getDstName() {
                return this.dstName;
            }

            public void setDstName(String dstName) {
                this.dstName = dstName;
            }
        }
    }
}
