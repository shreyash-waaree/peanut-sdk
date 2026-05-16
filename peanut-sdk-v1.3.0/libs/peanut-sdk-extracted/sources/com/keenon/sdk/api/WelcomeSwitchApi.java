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

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/WelcomeSwitchApi.class */
@LinkAdapter
@CoapCommond(path = "/welcome/switch", requestType = RequestEnum.POST)
public class WelcomeSwitchApi {
    private boolean cmd;
    private IDataCallback callback;

    @CoapResponse
    ApiCallback coapCallback = new ApiCallback<String>() { // from class: com.keenon.sdk.api.WelcomeSwitchApi.1
        @Override // com.keenon.sdk.hedera.model.ApiCallback, com.keenon.sdk.hedera.model.ICallback
        public void onSuccess(String result) {
            LogUtils.i(PeanutConstants.TAG_API, "[WelcomeSwitchApi][onSuccess: " + result + "]");
            ApiData bean = (ApiData) GsonUtil.gson2Bean(result, ApiData.class);
            String jsonResult = GsonUtil.bean2String(bean);
            LogUtils.d(PeanutConstants.TAG_API, "[WelcomeSwitchApi][onSuccess json: " + jsonResult + "]");
            if (WelcomeSwitchApi.this.callback == null) {
                return;
            }
            WelcomeSwitchApi.this.callback.success(jsonResult);
        }

        @Override // com.keenon.sdk.hedera.model.ApiCallback, com.keenon.sdk.hedera.model.ICallback
        public void onFail(ApiError error) {
            if (WelcomeSwitchApi.this.callback == null) {
                return;
            }
            WelcomeSwitchApi.this.callback.error(error);
        }
    };

    @CoapParams
    public String CoapParams() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put(AntiFallCalibrationApi.PARAM_CMD, this.cmd);
        } catch (JSONException e) {
            LogUtils.e(PeanutConstants.TAG_API, "[WelcomeSwitchApi]", (Exception) e);
        }
        return jsonObject.toString();
    }

    public void send(boolean cmd, IDataCallback callBack) {
        this.callback = callBack;
        this.cmd = cmd;
        SenderManager.getInstance().send(this);
    }
}
