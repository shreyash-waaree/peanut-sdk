package com.keenon.sdk.api;

import com.keenon.common.constant.PeanutConstants;
import com.keenon.common.utils.ByteUtils;
import com.keenon.common.utils.GsonUtil;
import com.keenon.common.utils.LogUtils;
import com.keenon.sdk.external.IDataCallback;
import com.keenon.sdk.hedera.model.ApiCallback;
import com.keenon.sdk.hedera.model.ApiData;
import com.keenon.sdk.hedera.model.ApiError;
import com.keenon.sdk.proxy.sender.SenderManager;
import com.keenon.sdk.proxy.sender.anno.LinkAdapter;
import com.keenon.sdk.serial.adapter.SerialCommand;
import com.keenon.sdk.serial.adapter.SerialParams;
import com.keenon.sdk.serial.adapter.SerialResponse;
import com.keenon.sdk.serial.base.SerialData;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/EmotionRunLocalApi.class */
@LinkAdapter(board = PeanutConstants.BoardType.EMOTION, link = PeanutConstants.LinkType.COM)
@SerialCommand(action = "6a", body = "24")
public class EmotionRunLocalApi {
    private IDataCallback callBack;

    @SerialResponse
    ApiCallback serialCallback = new ApiCallback<SerialData>() { // from class: com.keenon.sdk.api.EmotionRunLocalApi.1
        @Override // com.keenon.sdk.hedera.model.ApiCallback, com.keenon.sdk.hedera.model.ICallback
        public void onSuccess(SerialData result) {
            LogUtils.i(PeanutConstants.TAG_API, "[EmotionRunLocalApi][onSuccess raw: " + ByteUtils.toString(result.getData()) + "]");
            ApiData bean = new ApiData();
            bean.setStatus(ByteUtils.bytesToInt(ByteUtils.ObjectToByte(result.getStatus()), 0));
            String jsonResult = GsonUtil.bean2String(bean);
            LogUtils.d(PeanutConstants.TAG_API, "[EmotionRunLocalApi][onSuccess json: " + jsonResult + "]");
            if (EmotionRunLocalApi.this.callBack == null) {
                return;
            }
            EmotionRunLocalApi.this.callBack.success(jsonResult);
        }

        @Override // com.keenon.sdk.hedera.model.ApiCallback, com.keenon.sdk.hedera.model.ICallback
        public void onFail(ApiError error) {
            if (EmotionRunLocalApi.this.callBack == null) {
                return;
            }
            EmotionRunLocalApi.this.callBack.error(error);
        }
    };
    private Integer emotionId;

    @SerialParams
    public Byte[] SerialParams() {
        return new Byte[]{Byte.valueOf((byte) (this.emotionId.intValue() & 255))};
    }

    public void send(IDataCallback callBack, Integer emotionId) {
        this.callBack = callBack;
        this.emotionId = emotionId;
        SenderManager.getInstance().send(this);
    }
}
