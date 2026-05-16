package com.keenon.sdk.api;

import com.keenon.common.constant.PeanutConstants;
import com.keenon.common.utils.ByteUtils;
import com.keenon.common.utils.GsonUtil;
import com.keenon.common.utils.LogUtils;
import com.keenon.sdk.external.IDataCallback;
import com.keenon.sdk.external.PeanutSDK;
import com.keenon.sdk.hedera.model.ApiCallback;
import com.keenon.sdk.hedera.model.ApiData;
import com.keenon.sdk.hedera.model.ApiError;
import com.keenon.sdk.hedera.model.RequestEnum;
import com.keenon.sdk.proxy.sender.SenderManager;
import com.keenon.sdk.proxy.sender.anno.LinkAdapter;
import com.keenon.sdk.proxy.sender.anno.RequestType;
import com.keenon.sdk.serial.adapter.SerialCommand;
import com.keenon.sdk.serial.adapter.SerialResponse;
import com.keenon.sdk.serial.base.SerialData;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/EmotionStatusApi.class */
@LinkAdapter(board = PeanutConstants.BoardType.EMOTION, link = PeanutConstants.LinkType.COM)
@SerialCommand(action = "68", body = "25")
public class EmotionStatusApi {
    private IDataCallback callBack;
    private RequestEnum requestEnum = RequestEnum.GET;

    @SerialResponse
    ApiCallback serialCallback = new ApiCallback<SerialData>() { // from class: com.keenon.sdk.api.EmotionStatusApi.1
        @Override // com.keenon.sdk.hedera.model.ApiCallback, com.keenon.sdk.hedera.model.ICallback
        public void onSuccess(SerialData result) {
            LogUtils.i(PeanutConstants.TAG_API, "[EmotionStatusApi][onSuccess raw: " + ByteUtils.toString(result.getData()) + "]");
            ApiData bean = new ApiData();
            bean.setTopic(getClass());
            bean.setStatus(ByteUtils.bytesToInt(ByteUtils.ObjectToByte(result.getStatus()), 0));
            bean.setCode(ByteUtils.bytesToInt(ByteUtils.ObjectToByte(result.getData()), 0));
            String jsonResult = GsonUtil.bean2String(bean);
            LogUtils.d(PeanutConstants.TAG_API, "[EmotionStatusApi][onSuccess json: " + jsonResult + "]");
            if (EmotionStatusApi.this.callBack == null) {
                return;
            }
            if (EmotionStatusApi.this.requestEnum == RequestEnum.GET) {
                EmotionStatusApi.this.callBack.success(jsonResult);
            } else if (EmotionStatusApi.this.requestEnum == RequestEnum.OBSERVER) {
                PeanutSDK.getInstance().notifyApiSuccess(this, jsonResult);
            }
        }

        @Override // com.keenon.sdk.hedera.model.ApiCallback, com.keenon.sdk.hedera.model.ICallback
        public void onFail(ApiError error) {
            if (EmotionStatusApi.this.callBack == null) {
                return;
            }
            if (EmotionStatusApi.this.requestEnum == RequestEnum.GET) {
                EmotionStatusApi.this.callBack.error(error);
            } else if (EmotionStatusApi.this.requestEnum == RequestEnum.OBSERVER) {
                PeanutSDK.getInstance().notifyApiError(this, error);
            }
        }
    };

    @RequestType
    public RequestEnum requestType() {
        return this.requestEnum;
    }

    public void observe(IDataCallback callBack) {
        this.requestEnum = RequestEnum.OBSERVER;
        send(callBack);
    }

    public void cancel() {
        SenderManager.getInstance().cancel(this);
    }

    public void send(IDataCallback callBack) {
        this.callBack = callBack;
        SenderManager.getInstance().send(this);
    }
}
