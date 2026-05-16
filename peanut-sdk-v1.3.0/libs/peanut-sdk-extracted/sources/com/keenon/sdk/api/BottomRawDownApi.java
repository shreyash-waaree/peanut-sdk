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
import java.util.HashMap;
import org.json.JSONException;
import org.json.JSONObject;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/BottomRawDownApi.class */
@LinkAdapter
@CoapCommond(path = "/through/down", requestType = RequestEnum.POST)
public class BottomRawDownApi {
    public static final String PARAM_CHANNEL = "type";
    public static final String PARAM_TRANS = "trans";
    private HashMap<String, Object> params;
    private IDataCallback callBack;

    @CoapResponse
    ApiCallback coapCallback = new ApiCallback<String>() { // from class: com.keenon.sdk.api.BottomRawDownApi.1
        @Override // com.keenon.sdk.hedera.model.ApiCallback, com.keenon.sdk.hedera.model.ICallback
        public void onSuccess(String result) {
            LogUtils.i(PeanutConstants.TAG_API, "[BottomRawDownApi][onSuccess: " + result + "]");
            ApiData bean = (ApiData) GsonUtil.gson2Bean(result, ApiData.class);
            String jsonResult = GsonUtil.bean2String(bean);
            LogUtils.d(PeanutConstants.TAG_API, "[BottomRawDownApi][onSuccess json: " + jsonResult + "]");
            if (BottomRawDownApi.this.callBack == null) {
                return;
            }
            BottomRawDownApi.this.callBack.success(jsonResult);
        }

        @Override // com.keenon.sdk.hedera.model.ApiCallback, com.keenon.sdk.hedera.model.ICallback
        public void onFail(ApiError error) {
            if (BottomRawDownApi.this.callBack == null) {
                return;
            }
            BottomRawDownApi.this.callBack.error(error);
        }
    };

    /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/BottomRawDownApi$Channel.class */
    public static class Channel {
        public static final int STM32 = 0;
        public static final int BELL = 1;
    }

    @CoapParams
    public String CoapParams() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("trans", this.params.get("trans"));
            jsonObject.put("type", this.params.get("type"));
        } catch (JSONException e) {
            LogUtils.e(PeanutConstants.TAG_API, "[BottomRawDownApi]", (Exception) e);
        }
        return jsonObject.toString();
    }

    public void send(IDataCallback callBack, HashMap<String, Object> params) {
        this.callBack = callBack;
        this.params = params;
        SenderManager.getInstance().send(this);
    }
}
