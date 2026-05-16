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

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/DeviceRebootApi.class */
@LinkAdapter
@CoapCommond(path = "/devices/reboot", requestType = RequestEnum.POST)
public class DeviceRebootApi {
    private IDataCallback callBack;

    @CoapResponse
    ApiCallback coapCallback = new ApiCallback<String>() { // from class: com.keenon.sdk.api.DeviceRebootApi.1
        @Override // com.keenon.sdk.hedera.model.ApiCallback, com.keenon.sdk.hedera.model.ICallback
        public void onSuccess(String result) {
            LogUtils.i(PeanutConstants.TAG_API, "[DeviceRebootApi][onSuccess: " + result + "]");
            ApiData bean = (ApiData) GsonUtil.gson2Bean(result, ApiData.class);
            String jsonResult = GsonUtil.bean2String(bean);
            LogUtils.d(PeanutConstants.TAG_API, "[DeviceRebootApi][onSuccess json: " + jsonResult + "]");
            if (DeviceRebootApi.this.callBack == null) {
                return;
            }
            DeviceRebootApi.this.callBack.success(jsonResult);
        }

        @Override // com.keenon.sdk.hedera.model.ApiCallback, com.keenon.sdk.hedera.model.ICallback
        public void onFail(ApiError error) {
            if (DeviceRebootApi.this.callBack == null) {
                return;
            }
            DeviceRebootApi.this.callBack.error(error);
        }
    };
    private DeviceRebootParam param;

    /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/DeviceRebootApi$DeviceRebootParam.class */
    public static class DeviceRebootParam {
        public int id;
        public String name;
        public boolean isReposition;
    }

    @CoapParams
    public String CoapParams() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("id", this.param.id);
            jsonObject.put("name", this.param.name);
            jsonObject.put("isReposition", this.param.isReposition);
        } catch (JSONException e) {
            LogUtils.e(PeanutConstants.TAG_API, "[DeviceRebootApi]", (Exception) e);
        }
        return jsonObject.toString();
    }

    public void send(IDataCallback callBack, DeviceRebootParam param) {
        this.callBack = callBack;
        this.param = param;
        SenderManager.getInstance().send(this);
    }
}
