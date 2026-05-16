package com.keenon.sdk.api;

import android.util.Base64;
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
import org.eclipse.californium.core.coap.Request;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/UpdateDownloadApi.class */
@LinkAdapter
@CoapCommond(path = "/update/download", requestType = RequestEnum.FILE)
public class UpdateDownloadApi {
    private IProgressCallback callBack;

    @CoapResponse
    IProgressCallBack coapCallback = new IProgressCallBack<String>() { // from class: com.keenon.sdk.api.UpdateDownloadApi.1
        @Override // com.keenon.sdk.coap.adapter.IProgressCallBack
        public void readyToSend(Request request) {
            LogUtils.i(PeanutConstants.TAG_API, "[UpdateDownloadApi][readyToSend]");
            if (UpdateDownloadApi.this.callBack == null) {
                return;
            }
            UpdateDownloadApi.this.callBack.readyToSend(request);
        }

        @Override // com.keenon.sdk.hedera.model.ICallback
        public void onSuccess(String result) {
            LogUtils.i(PeanutConstants.TAG_API, "[UpdateDownloadApi][onSuccess: " + result + "]");
            ApiData bean = (ApiData) GsonUtil.gson2Bean(result, ApiData.class);
            String jsonResult = GsonUtil.bean2String(bean);
            LogUtils.d(PeanutConstants.TAG_API, "[UpdateDownloadApi][onSuccess json: " + jsonResult + "]");
            if (UpdateDownloadApi.this.callBack == null) {
                return;
            }
            UpdateDownloadApi.this.callBack.success(jsonResult);
        }

        @Override // com.keenon.sdk.hedera.model.ICallback
        public void onFail(ApiError error) {
            if (UpdateDownloadApi.this.callBack == null) {
                return;
            }
            UpdateDownloadApi.this.callBack.error(error);
        }

        @Override // com.keenon.sdk.coap.adapter.IProgressCallBack
        public void progress(int percent) {
            LogUtils.d(PeanutConstants.TAG_API, "[UpdateDownloadApi][progress: " + percent + "]");
            if (UpdateDownloadApi.this.callBack == null) {
                return;
            }
            UpdateDownloadApi.this.callBack.progress(percent);
        }
    };
    private byte[] file;

    @CoapParams
    public String CoapParams() {
        return this.file == null ? "" : Base64.encodeToString(this.file, 0);
    }

    public void send(IProgressCallback callBack, byte[] file) {
        this.callBack = callBack;
        this.file = file;
        SenderManager.getInstance().send(this);
    }
}
