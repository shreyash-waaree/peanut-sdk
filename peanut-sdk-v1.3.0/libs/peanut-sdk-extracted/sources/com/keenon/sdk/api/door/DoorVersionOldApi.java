package com.keenon.sdk.api.door;

import com.keenon.common.constant.PeanutConstants;
import com.keenon.common.utils.ByteUtils;
import com.keenon.common.utils.GsonUtil;
import com.keenon.common.utils.LogUtils;
import com.keenon.sdk.api.door.DoorVersionCompatApi;
import com.keenon.sdk.external.IDataCallback;
import com.keenon.sdk.hedera.model.ApiCallback;
import com.keenon.sdk.hedera.model.ApiError;
import com.keenon.sdk.hedera.model.RequestEnum;
import com.keenon.sdk.proxy.sender.SenderManager;
import com.keenon.sdk.proxy.sender.anno.LinkAdapter;
import com.keenon.sdk.proxy.sender.anno.RequestType;
import com.keenon.sdk.serial.adapter.SerialCommand;
import com.keenon.sdk.serial.adapter.SerialResponse;
import com.keenon.sdk.serial.base.SerialData;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/door/DoorVersionOldApi.class */
@LinkAdapter(board = PeanutConstants.BoardType.DOOR, link = PeanutConstants.LinkType.COM)
@SerialCommand(action = "04", body = "27")
public class DoorVersionOldApi {
    private IDataCallback callBack;

    @SerialResponse
    ApiCallback serialCallback = new ApiCallback<SerialData>() { // from class: com.keenon.sdk.api.door.DoorVersionOldApi.1
        @Override // com.keenon.sdk.hedera.model.ApiCallback, com.keenon.sdk.hedera.model.ICallback
        public void onSuccess(SerialData result) {
            LogUtils.i(PeanutConstants.TAG_API, "[DoorVersionOldApi][onSuccess raw: " + ByteUtils.toString(result.getData()) + "]");
            String versionInfo = new String(ByteUtils.ObjectToByte(result.getData()));
            LogUtils.i(PeanutConstants.TAG_API, "[DoorVersionOldApi][onSuccess raw: " + versionInfo + "]");
            DoorVersionCompatApi.VersionInfo dataBean = new DoorVersionCompatApi.VersionInfo();
            dataBean.setId("16");
            dataBean.setName("gd32f1-door-ctrl");
            dataBean.setHw("");
            dataBean.setSw("");
            if (!versionInfo.contains(":")) {
                dataBean.setSw(versionInfo);
            }
            DoorVersionCompatApi.Bean bean = new DoorVersionCompatApi.Bean();
            bean.setCode(0);
            bean.setStatus(0);
            bean.setTopic("DoorVersionOldApi");
            bean.setData(dataBean);
            String jsonResult = GsonUtil.bean2String(bean);
            LogUtils.d(PeanutConstants.TAG_API, "[DoorVersionOldApi][onSuccess json: " + jsonResult + "]");
            if (DoorVersionOldApi.this.callBack == null) {
                return;
            }
            DoorVersionOldApi.this.callBack.success(jsonResult);
        }

        @Override // com.keenon.sdk.hedera.model.ApiCallback, com.keenon.sdk.hedera.model.ICallback
        public void onFail(ApiError error) {
            if (DoorVersionOldApi.this.callBack == null) {
                return;
            }
            DoorVersionOldApi.this.callBack.error(error);
        }
    };
    private RequestEnum requestEnum = RequestEnum.GET;

    @RequestType
    public RequestEnum requestType() {
        return this.requestEnum;
    }

    public void send(IDataCallback callBack) {
        this.callBack = callBack;
        SenderManager.getInstance().send(this);
    }
}
