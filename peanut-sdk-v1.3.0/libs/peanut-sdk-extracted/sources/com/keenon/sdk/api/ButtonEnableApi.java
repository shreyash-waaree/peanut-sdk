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

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/ButtonEnableApi.class */
@LinkAdapter
@CoapCommond(path = "/button/enable", requestType = RequestEnum.POST)
public class ButtonEnableApi {
    private IDataCallback callBack;

    @CoapResponse
    ApiCallback coapCallback = new ApiCallback<String>() { // from class: com.keenon.sdk.api.ButtonEnableApi.1
        @Override // com.keenon.sdk.hedera.model.ApiCallback, com.keenon.sdk.hedera.model.ICallback
        public void onSuccess(String result) {
            LogUtils.i(PeanutConstants.TAG_API, "[ButtonEnableApi][onSuccess: " + result + "]");
            ApiData bean = (ApiData) GsonUtil.gson2Bean(result, ApiData.class);
            String jsonResult = GsonUtil.bean2String(bean);
            LogUtils.d(PeanutConstants.TAG_API, "[ButtonEnableApi][onSuccess json: " + jsonResult + "]");
            if (ButtonEnableApi.this.callBack == null) {
                return;
            }
            ButtonEnableApi.this.callBack.success(jsonResult);
        }

        @Override // com.keenon.sdk.hedera.model.ApiCallback, com.keenon.sdk.hedera.model.ICallback
        public void onFail(ApiError error) {
            if (ButtonEnableApi.this.callBack == null) {
                return;
            }
            ButtonEnableApi.this.callBack.error(error);
        }
    };
    private boolean enable;

    @CoapParams
    public String CoapParams() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("enable", this.enable);
        } catch (JSONException e) {
            LogUtils.e(PeanutConstants.TAG_API, "[ButtonEnableApi]", (Exception) e);
        }
        return jsonObject.toString();
    }

    public void send(IDataCallback callBack, boolean enable) {
        this.callBack = callBack;
        this.enable = enable;
        SenderManager.getInstance().send(this);
    }
}
