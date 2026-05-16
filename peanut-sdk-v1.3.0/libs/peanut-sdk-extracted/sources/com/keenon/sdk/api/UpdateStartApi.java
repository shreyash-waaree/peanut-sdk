package com.keenon.sdk.api;

import com.keenon.common.constant.PeanutConstants;
import com.keenon.common.utils.LogUtils;
import com.keenon.sdk.coap.adapter.CoapCommond;
import com.keenon.sdk.coap.adapter.CoapParams;
import com.keenon.sdk.coap.adapter.CoapResponse;
import com.keenon.sdk.external.IDataCallback;
import com.keenon.sdk.hedera.model.ApiCallback;
import com.keenon.sdk.hedera.model.ApiError;
import com.keenon.sdk.hedera.model.RequestEnum;
import com.keenon.sdk.proxy.sender.SenderManager;
import com.keenon.sdk.proxy.sender.anno.LinkAdapter;
import org.eclipse.californium.elements.DtlsEndpointContext;
import org.json.JSONException;
import org.json.JSONObject;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/UpdateStartApi.class */
@LinkAdapter
@CoapCommond(path = "/update/start", requestType = RequestEnum.POST)
public class UpdateStartApi {
    private IDataCallback callBack;

    @CoapResponse
    ApiCallback coapCallback = new ApiCallback<String>() { // from class: com.keenon.sdk.api.UpdateStartApi.1
        @Override // com.keenon.sdk.hedera.model.ApiCallback, com.keenon.sdk.hedera.model.ICallback
        public void onSuccess(String result) {
            LogUtils.i(PeanutConstants.TAG_API, "[UpgradeActionApi][onSuccess: " + result + "]");
            if (UpdateStartApi.this.callBack == null) {
                return;
            }
            UpdateStartApi.this.callBack.success(result);
        }

        @Override // com.keenon.sdk.hedera.model.ApiCallback, com.keenon.sdk.hedera.model.ICallback
        public void onFail(ApiError error) {
            if (UpdateStartApi.this.callBack == null) {
                return;
            }
            UpdateStartApi.this.callBack.error(error);
        }
    };
    private int force;

    @CoapParams
    public String CoapParams() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put(DtlsEndpointContext.HANDSHAKE_MODE_FORCE, this.force);
        } catch (JSONException e) {
            LogUtils.e(PeanutConstants.TAG_API, "[UpgradeActionApi]", (Exception) e);
        }
        return jsonObject.toString();
    }

    public void send(IDataCallback callBack, int force) {
        this.callBack = callBack;
        this.force = force;
        SenderManager.getInstance().send(this);
    }
}
