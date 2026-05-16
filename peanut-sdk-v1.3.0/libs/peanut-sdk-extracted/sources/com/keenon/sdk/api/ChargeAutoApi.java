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

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/ChargeAutoApi.class */
@LinkAdapter
@CoapCommond(path = "/charge/auto", requestType = RequestEnum.POST)
public class ChargeAutoApi {
    private IDataCallback callBack;
    private int pile = Integer.MIN_VALUE;

    @CoapResponse
    ApiCallback coapCallback = new ApiCallback<String>() { // from class: com.keenon.sdk.api.ChargeAutoApi.1
        @Override // com.keenon.sdk.hedera.model.ApiCallback, com.keenon.sdk.hedera.model.ICallback
        public void onSuccess(String result) {
            LogUtils.i(PeanutConstants.TAG_API, "[ChargeAutoApi][onSuccess: " + result + "]");
            ApiData bean = (ApiData) GsonUtil.gson2Bean(result, ApiData.class);
            String jsonResult = GsonUtil.bean2String(bean);
            LogUtils.d(PeanutConstants.TAG_API, "[ChargeAutoApi][onSuccess json: " + jsonResult + "]");
            if (ChargeAutoApi.this.callBack == null) {
                return;
            }
            ChargeAutoApi.this.callBack.success(jsonResult);
        }

        @Override // com.keenon.sdk.hedera.model.ApiCallback, com.keenon.sdk.hedera.model.ICallback
        public void onFail(ApiError error) {
            if (ChargeAutoApi.this.callBack == null) {
                return;
            }
            ChargeAutoApi.this.callBack.error(error);
        }
    };

    @CoapParams
    public String CoapParams() {
        if (this.pile <= 0) {
            return null;
        }
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("dst", this.pile);
        } catch (JSONException e) {
            LogUtils.e(PeanutConstants.TAG_API, "[ChargeAutoApi]", (Exception) e);
        }
        return jsonObject.toString();
    }

    public void send(IDataCallback callBack, int pile) {
        this.callBack = callBack;
        this.pile = pile;
        SenderManager.getInstance().send(this);
    }
}
