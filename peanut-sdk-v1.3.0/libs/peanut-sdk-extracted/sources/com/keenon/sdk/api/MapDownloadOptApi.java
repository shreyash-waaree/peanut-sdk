package com.keenon.sdk.api;

import com.keenon.common.constant.PeanutConstants;
import com.keenon.common.utils.LogUtils;
import com.keenon.sdk.coap.adapter.CoapCommond;
import com.keenon.sdk.coap.adapter.CoapParams;
import com.keenon.sdk.coap.adapter.CoapResponse;
import com.keenon.sdk.coap.adapter.IProgressCallBack;
import com.keenon.sdk.external.IProgressCallback;
import com.keenon.sdk.hedera.model.ApiError;
import com.keenon.sdk.hedera.model.RequestEnum;
import com.keenon.sdk.proxy.sender.SenderManager;
import com.keenon.sdk.proxy.sender.anno.LinkAdapter;
import org.eclipse.californium.core.coap.CoAP;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/MapDownloadOptApi.class */
@LinkAdapter
@CoapCommond(path = "/map/downloadoptmap", requestType = RequestEnum.POST_LARGE)
public class MapDownloadOptApi {
    private IProgressCallback callBack;

    @CoapResponse
    IProgressCallBack coapCallback = new IProgressCallBack<String>() { // from class: com.keenon.sdk.api.MapDownloadOptApi.1
        @Override // com.keenon.sdk.hedera.model.ICallback
        public void onSuccess(String result) {
            LogUtils.i(PeanutConstants.TAG_API, "[MapDownloadOptApi][onSuccess: " + result + "]");
            if (MapDownloadOptApi.this.callBack == null) {
                return;
            }
            MapDownloadOptApi.this.callBack.success(result);
        }

        @Override // com.keenon.sdk.coap.adapter.IProgressCallBack
        public void progress(int percent) {
            if (MapDownloadOptApi.this.callBack == null) {
                return;
            }
            MapDownloadOptApi.this.callBack.progress(percent);
        }

        @Override // com.keenon.sdk.hedera.model.ICallback
        public void onFail(ApiError error) {
            if (MapDownloadOptApi.this.callBack == null) {
                return;
            }
            MapDownloadOptApi.this.callBack.error(error);
        }
    };
    private byte[] file;

    @CoapParams
    public String CoapParams() {
        return this.file == null ? "" : new String(this.file, CoAP.UTF8_CHARSET);
    }

    public void send(IProgressCallback callBack, byte[] file) {
        this.callBack = callBack;
        this.file = file;
        SenderManager.getInstance().send(this);
    }
}
