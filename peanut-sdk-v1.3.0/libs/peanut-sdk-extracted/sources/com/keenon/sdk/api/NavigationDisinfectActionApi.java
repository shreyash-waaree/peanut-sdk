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

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/NavigationDisinfectActionApi.class */
@LinkAdapter
@CoapCommond(path = "/navigation/disinfectAction", requestType = RequestEnum.POST)
public class NavigationDisinfectActionApi {
    private String action;
    private int id;
    private float speed;
    private IDataCallback callBack;

    @CoapResponse
    ApiCallback coapCallback = new ApiCallback<String>() { // from class: com.keenon.sdk.api.NavigationDisinfectActionApi.1
        @Override // com.keenon.sdk.hedera.model.ApiCallback, com.keenon.sdk.hedera.model.ICallback
        public void onSuccess(String result) {
            LogUtils.i(PeanutConstants.TAG_API, "[NavigationDisinfectActionApi][onSuccess: " + result + "]");
            ApiData bean = (ApiData) GsonUtil.gson2Bean(result, ApiData.class);
            String jsonResult = GsonUtil.bean2String(bean);
            LogUtils.d(PeanutConstants.TAG_API, "[NavigationDisinfectActionApi][onSuccess json: " + jsonResult + "]");
            if (NavigationDisinfectActionApi.this.callBack == null) {
                return;
            }
            NavigationDisinfectActionApi.this.callBack.success(jsonResult);
        }

        @Override // com.keenon.sdk.hedera.model.ApiCallback, com.keenon.sdk.hedera.model.ICallback
        public void onFail(ApiError error) {
            if (NavigationDisinfectActionApi.this.callBack == null) {
                return;
            }
            NavigationDisinfectActionApi.this.callBack.error(error);
        }
    };

    @CoapParams
    public String CoapParams() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("action", this.action);
            JSONObject data = new JSONObject();
            data.put("id", this.id);
            data.put("speed", this.speed);
            jsonObject.put("data", data);
        } catch (JSONException e) {
            LogUtils.e(PeanutConstants.TAG_API, "[NavigationDisinfectActionApi]", (Exception) e);
        }
        return jsonObject.toString();
    }

    public void send(IDataCallback callBack, String action, int id, float speed) {
        this.callBack = callBack;
        this.action = action;
        this.id = id;
        this.speed = speed;
        SenderManager.getInstance().send(this);
    }
}
