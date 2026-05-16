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
import com.keenon.sdk.serial.adapter.SerialResponse;
import com.keenon.sdk.serial.base.SerialData;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/DoorTypeDoubleSetApi.class */
@LinkAdapter(board = PeanutConstants.BoardType.DOOR, link = PeanutConstants.LinkType.COM)
@SerialCommand(action = "71", body = "21")
public class DoorTypeDoubleSetApi {
    private IDataCallback callBack;

    @SerialResponse
    ApiCallback serialCallback = new ApiCallback<SerialData>() { // from class: com.keenon.sdk.api.DoorTypeDoubleSetApi.1
        @Override // com.keenon.sdk.hedera.model.ApiCallback, com.keenon.sdk.hedera.model.ICallback
        public void onSuccess(SerialData result) {
            LogUtils.i(PeanutConstants.TAG_API, "[DoorTypeDoubleSetApi][onSuccess raw: " + ByteUtils.toString(result.getData()) + "]");
            ApiData bean = new ApiData();
            bean.setCode(0);
            bean.setStatus(ByteUtils.bytesToInt(ByteUtils.ObjectToByte(result.getStatus()), 0));
            String jsonResult = GsonUtil.bean2String(bean);
            LogUtils.d(PeanutConstants.TAG_API, "[DoorTypeDoubleSetApi][onSuccess json: " + jsonResult + "]");
            if (DoorTypeDoubleSetApi.this.callBack == null) {
                return;
            }
            DoorTypeDoubleSetApi.this.callBack.success(jsonResult);
        }

        @Override // com.keenon.sdk.hedera.model.ApiCallback, com.keenon.sdk.hedera.model.ICallback
        public void onFail(ApiError error) {
            if (DoorTypeDoubleSetApi.this.callBack == null) {
                return;
            }
            DoorTypeDoubleSetApi.this.callBack.error(error);
        }
    };

    public void send(IDataCallback callBack) {
        this.callBack = callBack;
        SenderManager.getInstance().send(this);
    }
}
