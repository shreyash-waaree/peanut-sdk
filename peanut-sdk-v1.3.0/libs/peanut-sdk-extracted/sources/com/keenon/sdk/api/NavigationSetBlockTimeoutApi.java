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
import com.keenon.sdk.serial.adapter.SerialCommand;
import org.json.JSONException;
import org.json.JSONObject;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/NavigationSetBlockTimeoutApi.class */
@LinkAdapter
@CoapCommond(path = "/navigation/block_state_duration", requestType = RequestEnum.POST)
@SerialCommand(action = "62", body = "25")
public class NavigationSetBlockTimeoutApi {
    private IDataCallback callBack;

    @CoapResponse
    ApiCallback coapCallback = new ApiCallback<String>() { // from class: com.keenon.sdk.api.NavigationSetBlockTimeoutApi.1
        @Override // com.keenon.sdk.hedera.model.ApiCallback, com.keenon.sdk.hedera.model.ICallback
        public void onSuccess(String result) {
            LogUtils.i(PeanutConstants.TAG_API, "[NavigationSetBlockTimeoutApi][onSuccess: " + result + "]");
            ApiData bean = (ApiData) GsonUtil.gson2Bean(result, ApiData.class);
            String jsonResult = GsonUtil.bean2String(bean);
            LogUtils.d(PeanutConstants.TAG_API, "[NavigationSetBlockTimeoutApi][onSuccess json: " + jsonResult + "]");
            if (NavigationSetBlockTimeoutApi.this.callBack == null) {
                return;
            }
            NavigationSetBlockTimeoutApi.this.callBack.success(jsonResult);
        }

        @Override // com.keenon.sdk.hedera.model.ApiCallback, com.keenon.sdk.hedera.model.ICallback
        public void onFail(ApiError error) {
            if (NavigationSetBlockTimeoutApi.this.callBack == null) {
                return;
            }
            NavigationSetBlockTimeoutApi.this.callBack.error(error);
        }
    };
    private int timeout;

    @CoapParams
    public String CoapParams() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("duration", this.timeout);
        } catch (JSONException e) {
            LogUtils.e(PeanutConstants.TAG_API, "[NavigationSetBlockTimeoutApi]", (Exception) e);
        }
        return jsonObject.toString();
    }

    public void send(IDataCallback callBack, int target) {
        this.callBack = callBack;
        this.timeout = target;
        SenderManager.getInstance().send(this);
    }
}
