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

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/DevicesMoveControlApi.class */
@LinkAdapter
@CoapCommond(path = "/devices/moveControl", requestType = RequestEnum.POST)
public class DevicesMoveControlApi {
    private IDataCallback callBack;
    private ParamBean paramBean;

    @CoapResponse
    ApiCallback coapCallback = new ApiCallback<String>() { // from class: com.keenon.sdk.api.DevicesMoveControlApi.1
        @Override // com.keenon.sdk.hedera.model.ApiCallback, com.keenon.sdk.hedera.model.ICallback
        public void onSuccess(String result) {
            LogUtils.i(PeanutConstants.TAG_API, "[DevicesMoveControlApi][onSuccess: " + result + "]");
            ApiData bean = (ApiData) GsonUtil.gson2Bean(result, ApiData.class);
            String jsonResult = GsonUtil.bean2String(bean);
            LogUtils.d(PeanutConstants.TAG_API, "[DevicesMoveControlApi][onSuccess json: " + jsonResult + "]");
            if (DevicesMoveControlApi.this.callBack == null) {
                return;
            }
            DevicesMoveControlApi.this.callBack.success(jsonResult);
        }

        @Override // com.keenon.sdk.hedera.model.ApiCallback, com.keenon.sdk.hedera.model.ICallback
        public void onFail(ApiError error) {
            if (DevicesMoveControlApi.this.callBack == null) {
                return;
            }
            DevicesMoveControlApi.this.callBack.error(error);
        }
    };

    @CoapParams
    public String CoapParams() {
        JSONObject jsonObject = new JSONObject();
        try {
            if (this.paramBean != null) {
                jsonObject.put("reset", this.paramBean.reset);
                jsonObject.put("linear", this.paramBean.linear);
                jsonObject.put("angular", this.paramBean.angular);
                jsonObject.put("direction", this.paramBean.direction);
                jsonObject.put("time", this.paramBean.time);
            }
        } catch (JSONException e) {
            LogUtils.e(PeanutConstants.TAG_API, "[NavigationJackingState]", (Exception) e);
        }
        return jsonObject.toString();
    }

    public void send(IDataCallback callBack, ParamBean bean) {
        this.callBack = callBack;
        this.paramBean = bean;
        SenderManager.getInstance().send(this);
    }

    /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/DevicesMoveControlApi$ParamBean.class */
    public static class ParamBean {
        public int reset;
        public double linear;
        public double angular;
        public double direction;
        public double time;

        public ParamBean() {
            this.reset = 0;
            this.linear = 0.0d;
            this.angular = 0.0d;
            this.direction = 0.0d;
            this.time = 0.0d;
        }

        public ParamBean(int reset, double linear, double angular, double direction, double time) {
            this.reset = 0;
            this.linear = 0.0d;
            this.angular = 0.0d;
            this.direction = 0.0d;
            this.time = 0.0d;
            this.reset = reset;
            this.linear = linear;
            this.angular = angular;
            this.direction = direction;
            this.time = time;
        }
    }
}
