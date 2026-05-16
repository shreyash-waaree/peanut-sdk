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

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/LightDisplayApi.class */
@LinkAdapter(board = PeanutConstants.BoardType.EMOTION, link = PeanutConstants.LinkType.COM)
@SerialCommand(action = "6a", body = "23")
public class LightDisplayApi {
    private IDataCallback callBack;

    @SerialResponse
    ApiCallback serialCallback = new ApiCallback<SerialData>() { // from class: com.keenon.sdk.api.LightDisplayApi.1
        @Override // com.keenon.sdk.hedera.model.ApiCallback, com.keenon.sdk.hedera.model.ICallback
        public void onSuccess(SerialData result) {
            LogUtils.i(PeanutConstants.TAG_API, "[LightDisplayApi][onSuccess raw: " + ByteUtils.toString(result.getData()) + "]");
            ApiData bean = new ApiData();
            bean.setStatus(ByteUtils.bytesToInt(ByteUtils.ObjectToByte(result.getStatus()), 0));
            String jsonResult = GsonUtil.bean2String(bean);
            LogUtils.d(PeanutConstants.TAG_API, "[LightDisplayApi][onSuccess json: " + jsonResult + "]");
            if (LightDisplayApi.this.callBack == null) {
                return;
            }
            LightDisplayApi.this.callBack.success(jsonResult);
        }

        @Override // com.keenon.sdk.hedera.model.ApiCallback, com.keenon.sdk.hedera.model.ICallback
        public void onFail(ApiError error) {
            if (LightDisplayApi.this.callBack == null) {
                return;
            }
            LightDisplayApi.this.callBack.error(error);
        }
    };
    private int lightId;
    private Integer intensity;
    private int mode;

    @SerialParams
    public Byte[] SerialParams() {
        Byte[] bytes = {Byte.valueOf((byte) (this.lightId & 255)), Byte.valueOf((byte) (this.intensity.intValue() & 255)), Byte.valueOf((byte) (this.mode & 255))};
        return bytes;
    }

    public void send(IDataCallback callBack, int lightId, Integer intensity, int mode) {
        this.callBack = callBack;
        this.lightId = lightId;
        this.intensity = intensity;
        this.mode = mode;
        SenderManager.getInstance().send(this);
    }
}
