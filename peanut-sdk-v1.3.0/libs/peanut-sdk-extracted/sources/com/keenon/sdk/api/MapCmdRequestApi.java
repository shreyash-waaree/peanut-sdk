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
import org.eclipse.californium.core.coap.CoAP;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/MapCmdRequestApi.class */
@LinkAdapter
@CoapCommond(path = "/map/commandrequest", requestType = RequestEnum.POST)
public class MapCmdRequestApi {
    private IDataCallback callBack;

    @CoapResponse
    ApiCallback coapCallback = new ApiCallback<String>() { // from class: com.keenon.sdk.api.MapCmdRequestApi.1
        @Override // com.keenon.sdk.hedera.model.ApiCallback, com.keenon.sdk.hedera.model.ICallback
        public void onSuccess(String result) {
            LogUtils.i(PeanutConstants.TAG_API, "[MapCmdRequestApi][onSuccess: " + result + "]");
            if (MapCmdRequestApi.this.callBack == null) {
                return;
            }
            MapCmdRequestApi.this.callBack.success(result);
        }

        @Override // com.keenon.sdk.hedera.model.ApiCallback, com.keenon.sdk.hedera.model.ICallback
        public void onFail(ApiError error) {
            if (MapCmdRequestApi.this.callBack == null) {
                return;
            }
            MapCmdRequestApi.this.callBack.error(error);
        }
    };
    private byte[] data;

    @CoapParams
    public String CoapParams() {
        return this.data == null ? "" : new String(this.data, CoAP.UTF8_CHARSET);
    }

    public void send(IDataCallback callBack, byte[] data) {
        this.callBack = callBack;
        this.data = data;
        SenderManager.getInstance().send(this);
    }
}
