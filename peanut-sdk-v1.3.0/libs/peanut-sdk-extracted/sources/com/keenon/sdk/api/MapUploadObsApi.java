package com.keenon.sdk.api;

import com.keenon.common.constant.PeanutConstants;
import com.keenon.common.utils.LogUtils;
import com.keenon.sdk.coap.adapter.CoapCommond;
import com.keenon.sdk.coap.adapter.CoapParams;
import com.keenon.sdk.coap.adapter.CoapResponse;
import com.keenon.sdk.external.IDataCallback;
import com.keenon.sdk.hedera.model.ApiCallback;
import com.keenon.sdk.hedera.model.ApiError;
import com.keenon.sdk.proxy.sender.SenderManager;
import com.keenon.sdk.proxy.sender.anno.LinkAdapter;
import org.eclipse.californium.core.coap.CoAP;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/MapUploadObsApi.class */
@LinkAdapter
@CoapCommond(path = "/map/uploadobs")
public class MapUploadObsApi {
    private IDataCallback callBack;

    @CoapResponse
    ApiCallback coapCallback = new ApiCallback<String>() { // from class: com.keenon.sdk.api.MapUploadObsApi.1
        @Override // com.keenon.sdk.hedera.model.ApiCallback, com.keenon.sdk.hedera.model.ICallback
        public void onSuccess(String result) {
            LogUtils.i(PeanutConstants.TAG_API, "[MapUploadObsApi][onSuccess: " + result + "]");
            if (MapUploadObsApi.this.callBack == null) {
                return;
            }
            MapUploadObsApi.this.callBack.success(result);
        }

        @Override // com.keenon.sdk.hedera.model.ApiCallback, com.keenon.sdk.hedera.model.ICallback
        public void onFail(ApiError error) {
            if (MapUploadObsApi.this.callBack == null) {
                return;
            }
            MapUploadObsApi.this.callBack.error(error);
        }
    };
    private byte[] file;

    @CoapParams
    public String CoapParams() {
        return this.file == null ? "" : new String(this.file, CoAP.UTF8_CHARSET);
    }

    public void send(IDataCallback callBack, byte[] file) {
        this.callBack = callBack;
        this.file = file;
        SenderManager.getInstance().send(this);
    }
}
