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

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/SCMUpgradeTransApi.class */
@LinkAdapter(board = PeanutConstants.BoardType.DOOR, link = PeanutConstants.LinkType.COM)
@SerialCommand(action = "20", body = "23", status = "02")
public class SCMUpgradeTransApi {
    private IDataCallback callBack;
    private byte[] payload;

    @SerialResponse
    ApiCallback serialCallback = new ApiCallback<SerialData>() { // from class: com.keenon.sdk.api.SCMUpgradeTransApi.1
        @Override // com.keenon.sdk.hedera.model.ApiCallback, com.keenon.sdk.hedera.model.ICallback
        public void onSuccess(SerialData result) {
            LogUtils.i(PeanutConstants.TAG_API, "[SCMUpgradeTransApi][onSuccess raw: " + ByteUtils.toString(result.getData()) + "]");
            Bean bean = SCMUpgradeTransApi.this.new Bean();
            bean.setIndex(ByteUtils.ObjectToByte(result.getData()));
            bean.setCode(ByteUtils.ObjectToByte(result.getStatus()));
            String jsonResult = GsonUtil.bean2String(bean);
            LogUtils.d(PeanutConstants.TAG_API, "[SCMUpgradeTransApi][onSuccess json: " + jsonResult + "]");
            if (SCMUpgradeTransApi.this.callBack == null) {
                return;
            }
            SCMUpgradeTransApi.this.callBack.success(jsonResult);
        }

        @Override // com.keenon.sdk.hedera.model.ApiCallback, com.keenon.sdk.hedera.model.ICallback
        public void onFail(ApiError error) {
            if (SCMUpgradeTransApi.this.callBack == null) {
                return;
            }
            SCMUpgradeTransApi.this.callBack.error(error);
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

    /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/SCMUpgradeTransApi$Bean.class */
    public class Bean {
        private int code;
        private int index;

        public Bean() {
        }

        public int getCode() {
            return this.code;
        }

        public void setCode(int code) {
            this.code = code;
        }

        public int getIndex() {
            return this.index;
        }

        public void setIndex(int index) {
            this.index = index;
        }

        public void setIndex(byte[] bytes) {
            this.index = ByteUtils.bytesToInt3(bytes, 0);
        }

        public void setCode(byte[] bytes) {
            this.code = ByteUtils.bytesToInt(bytes, 0);
        }
    }
}
