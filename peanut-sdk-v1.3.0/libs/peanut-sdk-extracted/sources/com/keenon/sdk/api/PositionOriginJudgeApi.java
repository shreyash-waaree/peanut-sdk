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
import org.json.JSONObject;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/PositionOriginJudgeApi.class */
@LinkAdapter
@CoapCommond(path = "/position/originJudge", requestType = RequestEnum.POST)
public class PositionOriginJudgeApi {
    private double distance;
    private IDataCallback callBack;

    @CoapResponse
    ApiCallback coapCallback = new ApiCallback<String>() { // from class: com.keenon.sdk.api.PositionOriginJudgeApi.1
        @Override // com.keenon.sdk.hedera.model.ApiCallback, com.keenon.sdk.hedera.model.ICallback
        public void onSuccess(String result) {
            LogUtils.i(PeanutConstants.TAG_API, "[PositionOriginJudgeApi][onSuccess: " + result + "]");
            if (PositionOriginJudgeApi.this.callBack != null) {
                PositionOriginJudgeApi.this.callBack.success(result);
            }
        }

        @Override // com.keenon.sdk.hedera.model.ApiCallback, com.keenon.sdk.hedera.model.ICallback
        public void onFail(ApiError error) {
            if (PositionOriginJudgeApi.this.callBack != null) {
                PositionOriginJudgeApi.this.callBack.error(error);
            }
        }
    };

    @CoapParams
    public String CoapParams() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("distance", this.distance);
        } catch (Exception e) {
            LogUtils.e(PeanutConstants.TAG_API, "[PositionOriginJudgeApi]", e);
        }
        return jsonObject.toString();
    }

    public void send(double distance, IDataCallback callBack) {
        this.distance = distance;
        this.callBack = callBack;
        SenderManager.getInstance().send(this);
    }
}
