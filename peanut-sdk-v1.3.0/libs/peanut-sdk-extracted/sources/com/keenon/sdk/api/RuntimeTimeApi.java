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
import org.json.JSONException;
import org.json.JSONObject;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/RuntimeTimeApi.class */
@LinkAdapter
@CoapCommond(path = "/runtime/time", requestType = RequestEnum.POST)
public class RuntimeTimeApi {
    private IDataCallback callBack;
    private long timestamp = Long.MIN_VALUE;

    @CoapResponse
    ApiCallback coapCallback = new ApiCallback<String>() { // from class: com.keenon.sdk.api.RuntimeTimeApi.1
        @Override // com.keenon.sdk.hedera.model.ApiCallback, com.keenon.sdk.hedera.model.ICallback
        public void onSuccess(String result) {
            LogUtils.i(PeanutConstants.TAG_API, "[RuntimeTimeApi][onSuccess: " + result + "]");
            ApiData bean = (ApiData) GsonUtil.gson2Bean(result, ApiData.class);
            String jsonResult = GsonUtil.bean2String(bean);
            LogUtils.d(PeanutConstants.TAG_API, "[RuntimeTimeApi][onSuccess json: " + jsonResult + "]");
            if (RuntimeTimeApi.this.callBack == null) {
                return;
            }
            RuntimeTimeApi.this.callBack.success(jsonResult);
        }

        @Override // com.keenon.sdk.hedera.model.ApiCallback, com.keenon.sdk.hedera.model.ICallback
        public void onFail(ApiError error) {
            if (RuntimeTimeApi.this.callBack == null) {
                return;
            }
            RuntimeTimeApi.this.callBack.error(error);
        }
    };

    @CoapParams
    public String CoapParams() {
        if (this.timestamp <= 0) {
            return null;
        }
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("timestamp", this.timestamp);
        } catch (JSONException e) {
            LogUtils.e(PeanutConstants.TAG_API, "[RuntimeTimeApi]", (Exception) e);
        }
        return jsonObject.toString();
    }

    public void send(IDataCallback callBack, long timestamp) {
        this.callBack = callBack;
        this.timestamp = timestamp;
        SenderManager.getInstance().send(this);
    }
}
