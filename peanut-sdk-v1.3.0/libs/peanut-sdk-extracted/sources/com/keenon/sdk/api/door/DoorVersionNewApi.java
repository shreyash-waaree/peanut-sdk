package com.keenon.sdk.api.door;

import com.keenon.common.constant.PeanutConstants;
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

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/door/DoorVersionNewApi.class */
@LinkAdapter(board = PeanutConstants.BoardType.DOOR, link = PeanutConstants.LinkType.COM)
@SerialCommand(action = "04", body = "28")
public class DoorVersionNewApi {
    private IDataCallback callBack;

    @SerialResponse
    ApiCallback serialCallback = new ApiCallback<SerialData>() { // from class: com.keenon.sdk.api.door.DoorVersionNewApi.1
        /* JADX WARN: Code restructure failed: missing block: B:12:0x00bf, code lost:
        
            r0.setHw(r0.getString("hw"));
            r0.setSw(r0.getString("sw"));
            r0.setName(r0.getString("name"));
         */
        /* JADX WARN: Removed duplicated region for block: B:18:0x013f A[RETURN] */
        /* JADX WARN: Removed duplicated region for block: B:19:0x0140  */
        @Override // com.keenon.sdk.hedera.model.ApiCallback, com.keenon.sdk.hedera.model.ICallback
        /*
            Code decompiled incorrectly, please refer to instructions dump.
            To view partially-correct code enable 'Show inconsistent code' option in preferences
        */
        public void onSuccess(com.keenon.sdk.serial.base.SerialData r5) {
            /*
                Method dump skipped, instruction units count: 335
                To view this dump change 'Code comments level' option to 'DEBUG'
            */
            throw new UnsupportedOperationException("Method not decompiled: com.keenon.sdk.api.door.DoorVersionNewApi.AnonymousClass1.onSuccess(com.keenon.sdk.serial.base.SerialData):void");
        }

        @Override // com.keenon.sdk.hedera.model.ApiCallback, com.keenon.sdk.hedera.model.ICallback
        public void onFail(ApiError error) {
            if (DoorVersionNewApi.this.callBack == null) {
                return;
            }
            DoorVersionNewApi.this.callBack.error(error);
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
