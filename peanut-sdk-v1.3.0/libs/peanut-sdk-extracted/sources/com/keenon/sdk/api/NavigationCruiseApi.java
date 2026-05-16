package com.keenon.sdk.api;

import com.keenon.common.constant.PeanutConstants;
import com.keenon.common.utils.LogUtils;
import com.keenon.sdk.coap.adapter.CoapCommond;
import com.keenon.sdk.coap.adapter.CoapResponse;
import com.keenon.sdk.external.IDataCallback;
import com.keenon.sdk.hedera.model.ApiCallback;
import com.keenon.sdk.hedera.model.ApiError;
import com.keenon.sdk.hedera.model.RequestEnum;
import com.keenon.sdk.proxy.sender.SenderManager;
import com.keenon.sdk.proxy.sender.anno.LinkAdapter;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/NavigationCruiseApi.class */
@LinkAdapter
@CoapCommond(path = "/navigation/cruise", requestType = RequestEnum.POST)
public class NavigationCruiseApi {
    private IDataCallback callBack;

    @CoapResponse
    ApiCallback coapCallback = new ApiCallback<String>() { // from class: com.keenon.sdk.api.NavigationCruiseApi.1
        @Override // com.keenon.sdk.hedera.model.ApiCallback, com.keenon.sdk.hedera.model.ICallback
        public void onSuccess(String result) {
            LogUtils.i(PeanutConstants.TAG_API, "[NavigationCruiseApi][onSuccess: " + result + "]");
            if (NavigationCruiseApi.this.callBack == null) {
                return;
            }
            NavigationCruiseApi.this.callBack.success(result);
        }

        @Override // com.keenon.sdk.hedera.model.ApiCallback, com.keenon.sdk.hedera.model.ICallback
        public void onFail(ApiError error) {
            if (NavigationCruiseApi.this.callBack == null) {
                return;
            }
            NavigationCruiseApi.this.callBack.error(error);
        }
    };
    private int switchState;
    private int from;
    private int to;

    public void send(IDataCallback callBack, int switchState, int from, int to) {
        this.callBack = callBack;
        this.switchState = switchState;
        this.from = from;
        this.to = to;
        SenderManager.getInstance().send(this);
    }
}
