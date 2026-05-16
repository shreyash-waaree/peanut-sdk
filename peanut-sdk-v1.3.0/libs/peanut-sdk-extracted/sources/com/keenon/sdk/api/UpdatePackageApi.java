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

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/UpdatePackageApi.class */
@LinkAdapter
@CoapCommond(path = "/update/package", requestType = RequestEnum.POST)
public class UpdatePackageApi {
    private IDataCallback callBack;

    @CoapResponse
    ApiCallback coapCallback = new ApiCallback<String>() { // from class: com.keenon.sdk.api.UpdatePackageApi.1
        @Override // com.keenon.sdk.hedera.model.ApiCallback, com.keenon.sdk.hedera.model.ICallback
        public void onSuccess(String result) {
            LogUtils.i(PeanutConstants.TAG_API, "[UpdatePackageApi][onSuccess: " + result + "]");
            ApiData bean = (ApiData) GsonUtil.gson2Bean(result, ApiData.class);
            String jsonResult = GsonUtil.bean2String(bean);
            LogUtils.d(PeanutConstants.TAG_API, "[UpdatePackageApi][onSuccess json: " + jsonResult + "]");
            if (UpdatePackageApi.this.callBack == null) {
                return;
            }
            UpdatePackageApi.this.callBack.success(jsonResult);
        }

        @Override // com.keenon.sdk.hedera.model.ApiCallback, com.keenon.sdk.hedera.model.ICallback
        public void onFail(ApiError error) {
            if (UpdatePackageApi.this.callBack == null) {
                return;
            }
            UpdatePackageApi.this.callBack.error(error);
        }
    };
    private String packageInfo;

    @CoapParams
    public String CoapParams() {
        return this.packageInfo;
    }

    public void send(IDataCallback callBack, String packageInfo) {
        this.callBack = callBack;
        this.packageInfo = packageInfo;
        SenderManager.getInstance().send(this);
    }
}
