package com.keenon.sdk.coap;

import android.util.Base64;
import com.keenon.common.constant.PeanutConstants;
import com.keenon.common.utils.ByteUtils;
import com.keenon.common.utils.LogUtils;
import com.keenon.sdk.hedera.model.ApiCode;
import com.keenon.sdk.hedera.model.ApiError;
import com.keenon.sdk.hedera.model.ICallback;
import com.keenon.sdk.hedera.model.RequestEnum;
import org.eclipse.californium.core.CoapHandler;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.coap.Request;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/coap/BaseCoapHandler.class */
public class BaseCoapHandler implements CoapHandler {
    private static final String TAG = "[BaseCoapHandler]";
    private final RequestBody body;
    private final Request request;
    private final ICallback callback;

    public BaseCoapHandler(RequestBody body, Request request, ICallback callback) {
        this.body = body;
        this.request = request;
        this.callback = callback;
    }

    @Override // org.eclipse.californium.core.CoapHandler
    public void onLoad(CoapResponse response) {
        handleCallback(response);
    }

    @Override // org.eclipse.californium.core.CoapHandler
    public void onError() {
        LogUtils.e(PeanutConstants.TAG_SDK, "[BaseCoapHandler][onError][" + this.body.getTag() + "]");
        handleCallback(null);
    }

    public void handleCallback(CoapResponse response) {
        if (this.callback == null) {
            return;
        }
        if (this.request == null) {
            ApiError error = new ApiError(ApiCode.COAP_RESPONSE_ERROR);
            error.setMid(response.advanced().getMID());
            error.setMsg(response.getCode().text + ' ' + response.getCode().name());
            error.setTag(this.body.getTag());
            this.callback.onFail(error);
            return;
        }
        if (response == null) {
            ApiError error2 = new ApiError(ApiCode.COAP_RESPONSE_TIMEOUT);
            error2.setMid(this.request.getMID());
            error2.setTag(this.body.getTag());
            this.callback.onFail(error2);
            return;
        }
        if (response.isSuccess()) {
            if (response.getResponseText() == null || response.getResponseText().length() == 0) {
                ApiError error3 = new ApiError(ApiCode.COAP_RESPONSE_EMPTY);
                error3.setMid(response.advanced().getMID());
                error3.setTag(this.body.getTag());
                this.callback.onFail(error3);
                return;
            }
            byte[] payload = response.getPayload();
            if (this.body.getType() == RequestEnum.GET_LARGE) {
                this.callback.onSuccess(payload == null ? "" : Base64.encodeToString(payload, 0));
                return;
            } else if (this.body.getType() == RequestEnum.GET_RAW || this.body.getType() == RequestEnum.OBSERVER_RAW) {
                this.callback.onSuccess(ByteUtils.bytesToHexStr(payload));
                return;
            } else {
                this.callback.onSuccess(response.getResponseText());
                return;
            }
        }
        ApiError error4 = new ApiError(ApiCode.COAP_RESPONSE_ERROR);
        error4.setMid(response.advanced().getMID());
        error4.setMsg(response.getCode().text + ' ' + response.getCode().name());
        error4.setTag(this.body.getTag());
        this.callback.onFail(error4);
    }
}
