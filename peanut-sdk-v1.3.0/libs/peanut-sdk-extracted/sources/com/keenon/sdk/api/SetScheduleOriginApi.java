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

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/SetScheduleOriginApi.class */
@LinkAdapter
@CoapCommond(path = "/runtime/setScheduleOrigin", requestType = RequestEnum.POST)
public class SetScheduleOriginApi {
    private IDataCallback callBack;
    private int dst;

    @CoapResponse
    ApiCallback coapCallback = new ApiCallback<String>() { // from class: com.keenon.sdk.api.SetScheduleOriginApi.1
        @Override // com.keenon.sdk.hedera.model.ApiCallback, com.keenon.sdk.hedera.model.ICallback
        public void onSuccess(String result) {
            LogUtils.i(PeanutConstants.TAG_API, "[SettScheduleOriginApi][onSuccess: " + result + "]");
            if (result == null) {
                return;
            }
            Bean bean = (Bean) GsonUtil.gson2Bean(result, Bean.class);
            String jsonResult = GsonUtil.bean2String(bean);
            LogUtils.d(PeanutConstants.TAG_API, "[SettScheduleOriginApi][onSuccess json: " + jsonResult + "]");
            if (SetScheduleOriginApi.this.callBack == null) {
                return;
            }
            SetScheduleOriginApi.this.callBack.success(jsonResult);
        }

        @Override // com.keenon.sdk.hedera.model.ApiCallback, com.keenon.sdk.hedera.model.ICallback
        public void onFail(ApiError error) {
            if (SetScheduleOriginApi.this.callBack == null) {
                return;
            }
            SetScheduleOriginApi.this.callBack.error(error);
        }
    };

    /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/SetScheduleOriginApi$Bean.class */
    public static class Bean extends ApiData {
        public DataBean data;

        /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/SetScheduleOriginApi$Bean$DataBean.class */
        public static class DataBean {
            public int code;
        }
    }

    @CoapParams
    public String CoapParams() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("pose_id", this.dst);
        } catch (JSONException e) {
            LogUtils.e(PeanutConstants.TAG_API, "[SettScheduleOriginApi]", (Exception) e);
        }
        return jsonObject.toString();
    }

    public void send(IDataCallback callBack, int dst) {
        this.callBack = callBack;
        this.dst = dst;
        SenderManager.getInstance().send(this);
    }
}
