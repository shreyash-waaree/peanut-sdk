package com.keenon.sdk.api;

import com.keenon.common.constant.PeanutConstants;
import com.keenon.common.utils.GsonUtil;
import com.keenon.common.utils.LogUtils;
import com.keenon.sdk.coap.adapter.CoapCommond;
import com.keenon.sdk.coap.adapter.CoapParams;
import com.keenon.sdk.coap.adapter.CoapResponse;
import com.keenon.sdk.coap.adapter.IProgressCallBack;
import com.keenon.sdk.external.IProgressCallback;
import com.keenon.sdk.hedera.model.ApiData;
import com.keenon.sdk.hedera.model.ApiError;
import com.keenon.sdk.hedera.model.RequestEnum;
import com.keenon.sdk.proxy.sender.SenderManager;
import com.keenon.sdk.proxy.sender.anno.LinkAdapter;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.Request;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/MapDownloadApi.class */
@LinkAdapter
@CoapCommond(path = "/map/downloadmap", requestType = RequestEnum.POST_LARGE)
public class MapDownloadApi {
    private IProgressCallback callBack;

    @CoapResponse
    IProgressCallBack coapCallback = new IProgressCallBack<String>() { // from class: com.keenon.sdk.api.MapDownloadApi.1
        @Override // com.keenon.sdk.hedera.model.ICallback
        public void onSuccess(String result) {
            LogUtils.i(PeanutConstants.TAG_API, "[MapDownloadApi][onSuccess: " + result + "]");
            ApiData bean = (ApiData) GsonUtil.gson2Bean(result, ApiData.class);
            String jsonResult = GsonUtil.bean2String(bean);
            LogUtils.d(PeanutConstants.TAG_API, "[MapDownloadApi][onSuccess json: " + jsonResult + "]");
            if (MapDownloadApi.this.callBack == null) {
                return;
            }
            MapDownloadApi.this.callBack.success(jsonResult);
        }

        @Override // com.keenon.sdk.coap.adapter.IProgressCallBack
        public void progress(int percent) {
            if (MapDownloadApi.this.callBack == null) {
                return;
            }
            MapDownloadApi.this.callBack.progress(percent);
        }

        @Override // com.keenon.sdk.hedera.model.ICallback
        public void onFail(ApiError error) {
            if (MapDownloadApi.this.callBack == null) {
                return;
            }
            MapDownloadApi.this.callBack.error(error);
        }

        @Override // com.keenon.sdk.coap.adapter.IProgressCallBack
        public void readyToSend(Request request) {
            if (MapDownloadApi.this.callBack == null) {
                return;
            }
            MapDownloadApi.this.callBack.readyToSend(request);
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
