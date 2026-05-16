package com.keenon.sdk.api;

import com.keenon.common.constant.PeanutConstants;
import com.keenon.common.utils.LogUtils;
import com.keenon.sdk.coap.adapter.CoapCommond;
import com.keenon.sdk.coap.adapter.CoapParams;
import com.keenon.sdk.coap.adapter.CoapResponse;
import com.keenon.sdk.external.IDataCallback;
import com.keenon.sdk.hedera.model.ApiCallback;
import com.keenon.sdk.hedera.model.ApiError;
import com.keenon.sdk.hedera.model.RequestEnum;
import com.keenon.sdk.proxy.sender.SenderManager;
import com.keenon.sdk.proxy.sender.anno.LinkAdapter;
import org.json.JSONException;
import org.json.JSONObject;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/ScheduleModuleApi.class */
@LinkAdapter
@CoapCommond(path = "/schedule/module", requestType = RequestEnum.POST)
public class ScheduleModuleApi {
    private IDataCallback callBack;

    @CoapResponse
    ApiCallback coapCallback = new ApiCallback<String>() { // from class: com.keenon.sdk.api.ScheduleModuleApi.1
        @Override // com.keenon.sdk.hedera.model.ApiCallback, com.keenon.sdk.hedera.model.ICallback
        public void onSuccess(String result) {
            LogUtils.i(PeanutConstants.TAG_API, "[ScheduleModuleApi][onSuccess: " + result + "]");
            if (ScheduleModuleApi.this.callBack == null) {
                return;
            }
            ScheduleModuleApi.this.callBack.success(result);
        }

        @Override // com.keenon.sdk.hedera.model.ApiCallback, com.keenon.sdk.hedera.model.ICallback
        public void onFail(ApiError error) {
            if (ScheduleModuleApi.this.callBack == null) {
                return;
            }
            ScheduleModuleApi.this.callBack.error(error);
        }
    };
    private int channel;
    private int number;

    @CoapParams
    public String CoapParams() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("channel", this.channel);
            jsonObject.put("number", this.number);
        } catch (JSONException e) {
            LogUtils.e(PeanutConstants.TAG_API, "[ScheduleModuleApi]", (Exception) e);
        }
        return jsonObject.toString();
    }

    public void send(IDataCallback callBack, int channel, int number) {
        this.callBack = callBack;
        this.channel = channel;
        this.number = number;
        SenderManager.getInstance().send(this);
    }
}
