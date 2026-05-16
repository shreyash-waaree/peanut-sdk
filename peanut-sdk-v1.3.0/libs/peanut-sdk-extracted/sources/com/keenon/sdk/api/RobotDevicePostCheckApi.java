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
import org.eclipse.californium.core.coap.LinkFormat;
import org.json.JSONException;
import org.json.JSONObject;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/RobotDevicePostCheckApi.class */
@LinkAdapter
@CoapCommond(path = "/diagnosis/deviceCheck", requestType = RequestEnum.POST)
public class RobotDevicePostCheckApi {
    private IDataCallback callBack;
    private RobotCheckParams mParams;

    @CoapResponse
    ApiCallback coapCallback = new ApiCallback<String>() { // from class: com.keenon.sdk.api.RobotDevicePostCheckApi.1
        @Override // com.keenon.sdk.hedera.model.ApiCallback, com.keenon.sdk.hedera.model.ICallback
        public void onSuccess(String result) {
            LogUtils.i(PeanutConstants.TAG_API, "[RobotDeviceCheckApi][onSuccess: " + result + "]");
            ApiData bean = (ApiData) GsonUtil.gson2Bean(result, ApiData.class);
            String jsonResult = GsonUtil.bean2String(bean);
            LogUtils.d(PeanutConstants.TAG_API, "[RobotDeviceCheckApi][onSuccess json: " + jsonResult + "]");
            if (RobotDevicePostCheckApi.this.callBack == null) {
                return;
            }
            RobotDevicePostCheckApi.this.callBack.success(jsonResult);
        }

        @Override // com.keenon.sdk.hedera.model.ApiCallback, com.keenon.sdk.hedera.model.ICallback
        public void onFail(ApiError error) {
            if (RobotDevicePostCheckApi.this.callBack == null) {
                return;
            }
            RobotDevicePostCheckApi.this.callBack.error(error);
        }
    };

    /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/RobotDevicePostCheckApi$RobotCheckParams.class */
    public static class RobotCheckParams {
        public String type;
        public double standard;
        public double offset;
        public int count;
    }

    @CoapParams
    public String CoapParams() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("type", this.mParams.type);
            jsonObject.put("standard", this.mParams.standard);
            jsonObject.put("offset", this.mParams.offset);
            jsonObject.put(LinkFormat.COUNT, this.mParams.count);
        } catch (JSONException e) {
            LogUtils.e(PeanutConstants.TAG_API, "[RobotDeviceCheckApi]", (Exception) e);
        }
        return jsonObject.toString();
    }

    public void send(IDataCallback callBack, RobotCheckParams params) {
        this.callBack = callBack;
        this.mParams = params;
        SenderManager.getInstance().send(this);
    }
}
