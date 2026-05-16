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

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/MapDownLoadActionV2Api.class */
@LinkAdapter
@CoapCommond(path = "/runtime/download_action", requestType = RequestEnum.POST)
public class MapDownLoadActionV2Api {
    private int action;
    private IDataCallback callBack;

    @CoapResponse
    ApiCallback coapCallback = new ApiCallback<String>() { // from class: com.keenon.sdk.api.MapDownLoadActionV2Api.1
        @Override // com.keenon.sdk.hedera.model.ApiCallback, com.keenon.sdk.hedera.model.ICallback
        public void onSuccess(String result) {
            LogUtils.i(PeanutConstants.TAG_API, "[MapDownLoadActionV2Api][onSuccess: " + result + "]");
            ApiData bean = (ApiData) GsonUtil.gson2Bean(result, ApiData.class);
            String jsonResult = GsonUtil.bean2String(bean);
            LogUtils.d(PeanutConstants.TAG_API, "[MapDownLoadActionV2Api][onSuccess json: " + jsonResult + "]");
            if (MapDownLoadActionV2Api.this.callBack == null) {
                return;
            }
            MapDownLoadActionV2Api.this.callBack.success(jsonResult);
        }

        @Override // com.keenon.sdk.hedera.model.ApiCallback, com.keenon.sdk.hedera.model.ICallback
        public void onFail(ApiError error) {
            if (MapDownLoadActionV2Api.this.callBack == null) {
                return;
            }
            MapDownLoadActionV2Api.this.callBack.error(error);
        }
    };

    @CoapParams
    public String CoapParams() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("action", this.action);
        } catch (JSONException e) {
            LogUtils.e(PeanutConstants.TAG_API, "[MapDownLoadActionV2Api]", (Exception) e);
        }
        return jsonObject.toString();
    }

    public void send(IDataCallback callBack, int action) {
        this.callBack = callBack;
        this.action = action;
        SenderManager.getInstance().send(this);
    }
}
