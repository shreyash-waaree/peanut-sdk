package com.keenon.sdk.api;

import com.keenon.common.constant.PeanutConstants;
import com.keenon.common.utils.ByteUtils;
import com.keenon.common.utils.LogUtils;
import com.keenon.sdk.external.IDataCallback;
import com.keenon.sdk.hedera.model.ApiCallback;
import com.keenon.sdk.hedera.model.ApiError;
import com.keenon.sdk.proxy.sender.SenderManager;
import com.keenon.sdk.proxy.sender.anno.LinkAdapter;
import com.keenon.sdk.serial.adapter.SerialCommand;
import com.keenon.sdk.serial.adapter.SerialParams;
import com.keenon.sdk.serial.adapter.SerialResponse;
import com.keenon.sdk.serial.base.SerialData;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/SCMUpgradeLastPackApi.class */
@LinkAdapter(board = PeanutConstants.BoardType.DOOR, link = PeanutConstants.LinkType.COM)
@SerialCommand(action = "20", body = "23", status = "04")
public class SCMUpgradeLastPackApi {
    private IDataCallback callBack;
    private byte[] payload;

    @SerialResponse
    ApiCallback serialCallback = new ApiCallback<SerialData>() { // from class: com.keenon.sdk.api.SCMUpgradeLastPackApi.1
        @Override // com.keenon.sdk.hedera.model.ApiCallback, com.keenon.sdk.hedera.model.ICallback
        public void onSuccess(SerialData result) {
            LogUtils.i(PeanutConstants.TAG_API, "[SCMUpgradeLastPackApi][onSuccess raw: " + ByteUtils.toString(result.getData()) + "]");
            if (SCMUpgradeLastPackApi.this.callBack == null) {
            }
        }

        @Override // com.keenon.sdk.hedera.model.ApiCallback, com.keenon.sdk.hedera.model.ICallback
        public void onFail(ApiError error) {
            if (SCMUpgradeLastPackApi.this.callBack == null) {
                return;
            }
            SCMUpgradeLastPackApi.this.callBack.error(error);
        }
    };

    @SerialParams
    public Byte[] SerialParams() {
        return ByteUtils.ByteToObject(this.payload);
    }

    public void send(IDataCallback callBack, String md5) {
        this.callBack = callBack;
        this.payload = ByteUtils.hexStrToBytes(md5);
        SenderManager.getInstance().send(this);
    }
}
