package com.keenon.sdk.api;

import com.keenon.common.constant.PeanutConstants;
import com.keenon.common.utils.ByteUtils;
import com.keenon.common.utils.LogUtils;
import com.keenon.sdk.external.IDataCallback;
import com.keenon.sdk.hedera.model.ApiCallback;
import com.keenon.sdk.hedera.model.ApiCode;
import com.keenon.sdk.hedera.model.ApiError;
import com.keenon.sdk.proxy.sender.SenderManager;
import com.keenon.sdk.proxy.sender.anno.LinkAdapter;
import com.keenon.sdk.scmIot.SCMIoTSender;
import com.keenon.sdk.serial.adapter.SerialCommand;
import com.keenon.sdk.serial.adapter.SerialParams;
import com.keenon.sdk.serial.adapter.SerialResponse;
import com.keenon.sdk.serial.base.SerialData;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/SCMThingModeV1Api.class */
@LinkAdapter(board = PeanutConstants.BoardType.EMOTION, link = PeanutConstants.LinkType.COM)
@SerialCommand(action = "07", body = "01")
public class SCMThingModeV1Api {
    private IDataCallback callBack;
    private Byte[] payload;

    @SerialResponse
    ApiCallback serialCallback = new ApiCallback<SerialData>() { // from class: com.keenon.sdk.api.SCMThingModeV1Api.1
        @Override // com.keenon.sdk.hedera.model.ApiCallback, com.keenon.sdk.hedera.model.ICallback
        public void onSuccess(SerialData result) {
            if (SCMThingModeV1Api.this.callBack == null) {
                return;
            }
            LogUtils.i(PeanutConstants.TAG_API, "[SCMThingModelV1Api][onSuccess raw: " + ByteUtils.toString(result.getData()) + "]");
            if (result.getData() == null) {
                ApiError error = new ApiError(ApiCode.SCM_RESPONSE_EMPTY);
                SCMThingModeV1Api.this.callBack.error(error);
            } else {
                SCMIoTSender.receiveResponse(ByteUtils.ObjectToByte(result.getData()));
            }
        }

        @Override // com.keenon.sdk.hedera.model.ApiCallback, com.keenon.sdk.hedera.model.ICallback
        public void onFail(ApiError error) {
            if (SCMThingModeV1Api.this.callBack == null) {
                return;
            }
            SCMThingModeV1Api.this.callBack.error(error);
        }
    };

    @SerialParams
    public Byte[] SerialParams() {
        return this.payload;
    }

    public void send(IDataCallback callBack, Byte[] payload) {
        this.callBack = callBack;
        this.payload = payload;
        SenderManager.getInstance().send(this);
    }
}
