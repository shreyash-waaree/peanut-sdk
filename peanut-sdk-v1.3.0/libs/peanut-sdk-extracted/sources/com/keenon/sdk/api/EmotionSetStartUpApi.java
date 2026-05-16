package com.keenon.sdk.api;

import android.util.Base64;
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

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/EmotionSetStartUpApi.class */
@LinkAdapter(board = PeanutConstants.BoardType.EMOTION, link = PeanutConstants.LinkType.COM)
@SerialCommand(action = "69", body = "25")
public class EmotionSetStartUpApi {
    private IDataCallback callBack;

    @SerialResponse
    ApiCallback serialCallback = new ApiCallback<SerialData>() { // from class: com.keenon.sdk.api.EmotionSetStartUpApi.1
        @Override // com.keenon.sdk.hedera.model.ApiCallback, com.keenon.sdk.hedera.model.ICallback
        public void onSuccess(SerialData result) {
            LogUtils.i(PeanutConstants.TAG_API, "[EmotionSetStartUpApi][onSuccess raw: " + ByteUtils.toString(result.getData()) + "]");
            ApiData bean = new ApiData();
            bean.setStatus(ByteUtils.bytesToInt(ByteUtils.ObjectToByte(result.getStatus()), 0));
            String jsonResult = GsonUtil.bean2String(bean);
            LogUtils.d(PeanutConstants.TAG_API, "[EmotionSetStartUpApi][onSuccess json: " + jsonResult + "]");
            if (EmotionSetStartUpApi.this.callBack == null) {
                return;
            }
            EmotionSetStartUpApi.this.callBack.success(jsonResult);
        }

        @Override // com.keenon.sdk.hedera.model.ApiCallback, com.keenon.sdk.hedera.model.ICallback
        public void onFail(ApiError error) {
            if (EmotionSetStartUpApi.this.callBack == null) {
                return;
            }
            EmotionSetStartUpApi.this.callBack.error(error);
        }
    };
    private String emotion;

    @SerialParams
    public Byte[] SerialParams() {
        byte[] bytes = Base64.decode(this.emotion, 0);
        return ByteUtils.ByteToObject(bytes);
    }

    public void send(IDataCallback callBack, String emotion) {
        this.callBack = callBack;
        this.emotion = emotion;
        SenderManager.getInstance().send(this);
    }
}
