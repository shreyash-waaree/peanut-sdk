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
import java.util.List;
import org.json.JSONException;
import org.json.JSONObject;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/GuideSetTargetApi.class */
@LinkAdapter
@CoapCommond(path = "/guide/dst", requestType = RequestEnum.POST)
public class GuideSetTargetApi {
    private IDataCallback callBack;

    @CoapResponse
    ApiCallback coapCallback = new ApiCallback<String>() { // from class: com.keenon.sdk.api.GuideSetTargetApi.1
        @Override // com.keenon.sdk.hedera.model.ApiCallback, com.keenon.sdk.hedera.model.ICallback
        public void onSuccess(String result) {
            LogUtils.i(PeanutConstants.TAG_API, "[GuideSetTargetApi][onSuccess: " + result + "]");
            ApiData bean = (ApiData) GsonUtil.gson2Bean(result, ApiData.class);
            String jsonResult = GsonUtil.bean2String(bean);
            LogUtils.d(PeanutConstants.TAG_API, "[GuideSetTargetApi][onSuccess json: " + jsonResult + "]");
            if (GuideSetTargetApi.this.callBack == null) {
                return;
            }
            GuideSetTargetApi.this.callBack.success(jsonResult);
        }

        @Override // com.keenon.sdk.hedera.model.ApiCallback, com.keenon.sdk.hedera.model.ICallback
        public void onFail(ApiError error) {
            if (GuideSetTargetApi.this.callBack == null) {
                return;
            }
            GuideSetTargetApi.this.callBack.error(error);
        }
    };
    private List<Integer> targetList;

    @CoapParams
    public String CoapParams() {
        if (this.targetList.isEmpty()) {
            return null;
        }
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("dst", this.targetList);
        } catch (JSONException e) {
            LogUtils.e(PeanutConstants.TAG_API, "[GuideSetTargetApi]", (Exception) e);
        }
        return jsonObject.toString();
    }

    public void send(IDataCallback callBack, List<Integer> targetList) {
        this.callBack = callBack;
        this.targetList = targetList;
        SenderManager.getInstance().send(this);
    }
}
