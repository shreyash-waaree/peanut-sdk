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

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/NavigationShortPath.class */
@LinkAdapter
@CoapCommond(path = "/navigation/shortest_path_activate", requestType = RequestEnum.POST)
public class NavigationShortPath {
    private IDataCallback callBack;
    private int active_path_state;

    @CoapResponse
    ApiCallback coapCallback = new ApiCallback<String>() { // from class: com.keenon.sdk.api.NavigationShortPath.1
        @Override // com.keenon.sdk.hedera.model.ApiCallback, com.keenon.sdk.hedera.model.ICallback
        public void onSuccess(String result) {
            LogUtils.i(PeanutConstants.TAG_API, "[ChargeStopApi][onSuccess: " + result + "]");
            ApiData bean = (ApiData) GsonUtil.gson2Bean(result, ApiData.class);
            String jsonResult = GsonUtil.bean2String(bean);
            LogUtils.d(PeanutConstants.TAG_API, "[ChargeStopApi][onSuccess json: " + jsonResult + "]");
            if (NavigationShortPath.this.callBack == null) {
                return;
            }
            NavigationShortPath.this.callBack.success(jsonResult);
        }

        @Override // com.keenon.sdk.hedera.model.ApiCallback, com.keenon.sdk.hedera.model.ICallback
        public void onFail(ApiError error) {
            if (NavigationShortPath.this.callBack == null) {
                return;
            }
            NavigationShortPath.this.callBack.error(error);
        }
    };

    @CoapParams
    public String CoapParams() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("active_path_state", this.active_path_state);
        } catch (JSONException e) {
            LogUtils.e(PeanutConstants.TAG_API, "[MotorSMaxApi]", (Exception) e);
        }
        return jsonObject.toString();
    }

    public void send(IDataCallback callBack, int state) {
        this.callBack = callBack;
        this.active_path_state = state;
        SenderManager.getInstance().send(this);
    }
}
