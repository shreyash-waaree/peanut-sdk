package com.keenon.sdk.api;

import com.keenon.common.constant.PeanutConstants;
import com.keenon.common.utils.ByteUtils;
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
import com.keenon.sdk.serial.adapter.SerialParams;
import com.keenon.sdk.serial.adapter.SerialResponse;
import com.keenon.sdk.serial.base.SerialData;
import org.json.JSONException;
import org.json.JSONObject;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/RuntimeWorkModeApi.class */
@LinkAdapter
@CoapCommond(path = "/runtime/workmode", requestType = RequestEnum.POST)
@SerialCommand(action = "62", body = "20")
public class RuntimeWorkModeApi {
    private IDataCallback callBack;

    @CoapResponse
    ApiCallback coapCallback = new ApiCallback<String>() { // from class: com.keenon.sdk.api.RuntimeWorkModeApi.1
        @Override // com.keenon.sdk.hedera.model.ApiCallback, com.keenon.sdk.hedera.model.ICallback
        public void onSuccess(String result) {
            LogUtils.i(PeanutConstants.TAG_API, "[RuntimeWorkModeApi][onSuccess: " + result + "]");
            ApiData bean = (ApiData) GsonUtil.gson2Bean(result, ApiData.class);
            String jsonResult = GsonUtil.bean2String(bean);
            LogUtils.d(PeanutConstants.TAG_API, "[RuntimeWorkModeApi][onSuccess json: " + jsonResult + "]");
            if (RuntimeWorkModeApi.this.callBack == null) {
                return;
            }
            RuntimeWorkModeApi.this.callBack.success(jsonResult);
        }

        @Override // com.keenon.sdk.hedera.model.ApiCallback, com.keenon.sdk.hedera.model.ICallback
        public void onFail(ApiError error) {
            if (RuntimeWorkModeApi.this.callBack == null) {
                return;
            }
            RuntimeWorkModeApi.this.callBack.error(error);
        }
    };

    @SerialResponse
    ApiCallback serialCallback = new ApiCallback<SerialData>() { // from class: com.keenon.sdk.api.RuntimeWorkModeApi.2
        @Override // com.keenon.sdk.hedera.model.ApiCallback, com.keenon.sdk.hedera.model.ICallback
        public void onSuccess(SerialData result) {
            LogUtils.i(PeanutConstants.TAG_API, "[RuntimeWorkModeApi][onSuccess raw: " + ByteUtils.toString(result.getData()) + "]");
            ApiData bean = new ApiData();
            bean.setStatus(result.getStatus());
            bean.setCode(result.getData());
            String jsonResult = GsonUtil.bean2String(bean);
            LogUtils.d(PeanutConstants.TAG_API, "[RuntimeWorkModeApi][onSuccess json: " + jsonResult + "]");
            RuntimeWorkModeApi.this.callBack.success(jsonResult);
        }

        @Override // com.keenon.sdk.hedera.model.ApiCallback, com.keenon.sdk.hedera.model.ICallback
        public void onFail(ApiError error) {
            RuntimeWorkModeApi.this.callBack.error(error);
        }
    };
    private int mode;

    @CoapParams
    public String CoapParams() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("mode", this.mode);
        } catch (JSONException e) {
            LogUtils.e(PeanutConstants.TAG_API, "[RuntimeWorkModeApi]", (Exception) e);
        }
        return jsonObject.toString();
    }

    @SerialParams
    public Byte[] SerialParams() {
        return ByteUtils.ByteToObject(new byte[]{(byte) this.mode});
    }

    public void send(IDataCallback callBack, int mode) {
        this.mode = mode;
        this.callBack = callBack;
        SenderManager.getInstance().send(this);
    }
}
