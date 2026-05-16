package com.keenon.sdk.api;

import com.keenon.common.constant.PeanutConstants;
import com.keenon.common.utils.ByteUtils;
import com.keenon.common.utils.GsonUtil;
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

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/SCMUpgradePackInfoApi.class */
@LinkAdapter(board = PeanutConstants.BoardType.DOOR, link = PeanutConstants.LinkType.COM)
@SerialCommand(action = "20", body = "23", status = "01")
public class SCMUpgradePackInfoApi {
    private IDataCallback callBack;
    private byte[] payload;

    @SerialResponse
    ApiCallback serialCallback = new ApiCallback<SerialData>() { // from class: com.keenon.sdk.api.SCMUpgradePackInfoApi.1
        @Override // com.keenon.sdk.hedera.model.ApiCallback, com.keenon.sdk.hedera.model.ICallback
        public void onSuccess(SerialData result) {
            LogUtils.i(PeanutConstants.TAG_API, "[SCMUpgradePkgInfoApi][onSuccess raw: " + ByteUtils.toString(result.getData()) + "]");
            Bean bean = new Bean();
            bean.setBytes(result.getData());
            String jsonResult = GsonUtil.bean2String(bean);
            LogUtils.d(PeanutConstants.TAG_API, "[SCMUpgradePkgInfoApi][onSuccess json: " + jsonResult + "]");
            if (SCMUpgradePackInfoApi.this.callBack == null) {
                return;
            }
            SCMUpgradePackInfoApi.this.callBack.success(jsonResult);
        }

        @Override // com.keenon.sdk.hedera.model.ApiCallback, com.keenon.sdk.hedera.model.ICallback
        public void onFail(ApiError error) {
            if (SCMUpgradePackInfoApi.this.callBack == null) {
                return;
            }
            SCMUpgradePackInfoApi.this.callBack.error(error);
        }
    };

    @SerialParams
    public Byte[] SerialParams() {
        return ByteUtils.ByteToObject(this.payload);
    }

    public void send(IDataCallback callBack, byte[] params) {
        this.callBack = callBack;
        this.payload = params;
        SenderManager.getInstance().send(this);
    }

    /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/SCMUpgradePackInfoApi$Bean.class */
    public static class Bean {
        private int code;

        public int getCode() {
            return this.code;
        }

        public void setCode(int code) {
            this.code = code;
        }

        public void setBytes(Byte[] bytes) {
            this.code = ByteUtils.bytesToInt2(ByteUtils.ObjectToByte(bytes), 0);
        }
    }
}
