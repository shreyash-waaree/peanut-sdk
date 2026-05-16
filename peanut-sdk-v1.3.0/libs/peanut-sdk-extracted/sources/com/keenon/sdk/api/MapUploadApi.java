package com.keenon.sdk.api;

import com.keenon.common.constant.PeanutConstants;
import com.keenon.common.utils.LogUtils;
import com.keenon.sdk.coap.adapter.CoapCommond;
import com.keenon.sdk.coap.adapter.CoapResponse;
import com.keenon.sdk.coap.adapter.IProgressCallBack;
import com.keenon.sdk.external.IProgressCallback;
import com.keenon.sdk.hedera.model.ApiError;
import com.keenon.sdk.hedera.model.RequestEnum;
import com.keenon.sdk.proxy.sender.SenderManager;
import com.keenon.sdk.proxy.sender.anno.LinkAdapter;
import org.eclipse.californium.core.coap.Request;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/MapUploadApi.class */
@LinkAdapter
@CoapCommond(path = "/map/uploadmap", requestType = RequestEnum.GET_LARGE)
public class MapUploadApi {
    private IProgressCallback callBack;

    @CoapResponse
    IProgressCallBack coapCallback = new IProgressCallBack<String>() { // from class: com.keenon.sdk.api.MapUploadApi.1
        @Override // com.keenon.sdk.hedera.model.ICallback
        public void onSuccess(String result) {
            LogUtils.i(PeanutConstants.TAG_API, "[MapUploadApi][onSuccess]");
            if (MapUploadApi.this.callBack == null) {
                return;
            }
            MapUploadApi.this.callBack.success(result);
        }

        @Override // com.keenon.sdk.coap.adapter.IProgressCallBack
        public void progress(int percent) {
            if (MapUploadApi.this.callBack == null) {
                return;
            }
            MapUploadApi.this.callBack.progress(percent);
        }

        @Override // com.keenon.sdk.hedera.model.ICallback
        public void onFail(ApiError error) {
            if (MapUploadApi.this.callBack == null) {
                return;
            }
            MapUploadApi.this.callBack.error(error);
        }

        @Override // com.keenon.sdk.coap.adapter.IProgressCallBack
        public void readyToSend(Request request) {
            if (MapUploadApi.this.callBack == null) {
                return;
            }
            MapUploadApi.this.callBack.readyToSend(request);
        }
    };

    public void send(IProgressCallback callBack) {
        this.callBack = callBack;
        SenderManager.getInstance().send(this);
    }
}
