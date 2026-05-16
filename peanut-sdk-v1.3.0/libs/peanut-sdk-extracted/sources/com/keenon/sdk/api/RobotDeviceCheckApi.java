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

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/RobotDeviceCheckApi.class */
@LinkAdapter
@CoapCommond(path = "/diagnosis/deviceCheck", requestType = RequestEnum.POST)
public class RobotDeviceCheckApi {
    private static final String ONLINECHECK = "OnlineCheck";
    private static final String GAZERCHECK = "GazerCheck";
    private String type;
    private IDataCallback callBack;

    @CoapResponse
    ApiCallback coapCallback = new ApiCallback<String>() { // from class: com.keenon.sdk.api.RobotDeviceCheckApi.1
        @Override // com.keenon.sdk.hedera.model.ApiCallback, com.keenon.sdk.hedera.model.ICallback
        public void onSuccess(String result) {
            LogUtils.i(PeanutConstants.TAG_API, "[RobotDeviceCheckApi][onSuccess: " + result + "]");
            ApiData bean = (ApiData) GsonUtil.gson2Bean(result, ApiData.class);
            String jsonResult = GsonUtil.bean2String(bean);
            LogUtils.d(PeanutConstants.TAG_API, "[RobotDeviceCheckApi][onSuccess json: " + jsonResult + "]");
            if (RobotDeviceCheckApi.this.callBack == null) {
                return;
            }
            RobotDeviceCheckApi.this.callBack.success(jsonResult);
        }

        @Override // com.keenon.sdk.hedera.model.ApiCallback, com.keenon.sdk.hedera.model.ICallback
        public void onFail(ApiError error) {
            if (RobotDeviceCheckApi.this.callBack == null) {
                return;
            }
            RobotDeviceCheckApi.this.callBack.error(error);
        }
    };

    @CoapParams
    public String CoapParams() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("type", this.type);
        } catch (JSONException e) {
            LogUtils.e(PeanutConstants.TAG_API, "[RobotDeviceCheckApi]", (Exception) e);
        }
        return jsonObject.toString();
    }

    public void send(IDataCallback callBack, int type) {
        this.callBack = callBack;
        switch (type) {
            case 1:
                this.type = "OnlineCheck";
                break;
            case 2:
                this.type = "GazerCheck";
                break;
        }
        SenderManager.getInstance().send(this);
    }
}
