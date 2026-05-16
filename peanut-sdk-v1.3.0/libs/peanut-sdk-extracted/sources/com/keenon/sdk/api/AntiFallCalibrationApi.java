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
import java.util.HashMap;
import org.json.JSONException;
import org.json.JSONObject;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/AntiFallCalibrationApi.class */
@LinkAdapter
@CoapCommond(path = "/fdl/manu", requestType = RequestEnum.POST)
public class AntiFallCalibrationApi {
    public static final String PARAM_CMD = "cmd";
    public static final String PARAM_SET = "set";
    private HashMap<String, Object> params;
    private IDataCallback callback;

    @CoapResponse
    ApiCallback coapCallback = new ApiCallback<String>() { // from class: com.keenon.sdk.api.AntiFallCalibrationApi.1
        @Override // com.keenon.sdk.hedera.model.ApiCallback, com.keenon.sdk.hedera.model.ICallback
        public void onSuccess(String result) {
            LogUtils.i(PeanutConstants.TAG_API, "[AntiFallCalibrationApi][onSuccess: " + result + "]");
            ApiData bean = (ApiData) GsonUtil.gson2Bean(result, ApiData.class);
            String jsonResult = GsonUtil.bean2String(bean);
            LogUtils.d(PeanutConstants.TAG_API, "[AntiFallCalibrationApi][onSuccess json: " + jsonResult + "]");
            if (AntiFallCalibrationApi.this.callback == null) {
                return;
            }
            AntiFallCalibrationApi.this.callback.success(jsonResult);
        }

        @Override // com.keenon.sdk.hedera.model.ApiCallback, com.keenon.sdk.hedera.model.ICallback
        public void onFail(ApiError error) {
            if (AntiFallCalibrationApi.this.callback == null) {
                return;
            }
            AntiFallCalibrationApi.this.callback.error(error);
        }
    };

    @CoapParams
    public String CoapParams() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put(PARAM_CMD, this.params.get(PARAM_CMD));
            jsonObject.put(PARAM_SET, this.params.get(PARAM_SET));
        } catch (JSONException e) {
            LogUtils.e(PeanutConstants.TAG_API, "[AntiFallCalibrationApi]", (Exception) e);
        }
        LogUtils.i(PeanutConstants.TAG_API, "[AntiFallCalibrationApi]" + jsonObject.toString());
        return jsonObject.toString();
    }

    public void send(IDataCallback callBack, HashMap<String, Object> params) {
        this.callback = callBack;
        this.params = params;
        SenderManager.getInstance().send(this);
    }
}
