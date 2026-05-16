package com.keenon.sdk.api;

import com.keenon.common.constant.PeanutConstants;
import com.keenon.common.utils.LogUtils;
import com.keenon.sdk.coap.adapter.CoapCommond;
import com.keenon.sdk.coap.adapter.CoapParams;
import com.keenon.sdk.coap.adapter.CoapResponse;
import com.keenon.sdk.external.IDataCallback;
import com.keenon.sdk.hedera.model.ApiCallback;
import com.keenon.sdk.hedera.model.ApiData;
import com.keenon.sdk.hedera.model.ApiError;
import com.keenon.sdk.hedera.model.RequestEnum;
import com.keenon.sdk.proxy.sender.SenderManager;
import com.keenon.sdk.proxy.sender.anno.LinkAdapter;
import org.json.JSONException;
import org.json.JSONObject;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/FdlCmdStatusApi.class */
@LinkAdapter
@CoapCommond(path = "/fdl/checkStatus", requestType = RequestEnum.POST)
public class FdlCmdStatusApi {
    private String mType;
    private IDataCallback callback;

    @CoapResponse
    ApiCallback coapCallback = new ApiCallback<String>() { // from class: com.keenon.sdk.api.FdlCmdStatusApi.1
        @Override // com.keenon.sdk.hedera.model.ApiCallback, com.keenon.sdk.hedera.model.ICallback
        public void onSuccess(String result) {
            LogUtils.i(PeanutConstants.TAG_API, "[FdlCmdStatusApi][onSuccess: " + result + "]");
            if (FdlCmdStatusApi.this.callback == null) {
                return;
            }
            FdlCmdStatusApi.this.callback.success(result);
        }

        @Override // com.keenon.sdk.hedera.model.ApiCallback, com.keenon.sdk.hedera.model.ICallback
        public void onFail(ApiError error) {
            if (FdlCmdStatusApi.this.callback == null) {
                return;
            }
            FdlCmdStatusApi.this.callback.error(error);
        }
    };

    @CoapParams
    public String CoapParams() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("type", this.mType);
        } catch (JSONException e) {
            LogUtils.e(PeanutConstants.TAG_API, "[FdlCmdStatusApi]", (Exception) e);
        }
        return jsonObject.toString();
    }

    public void send(String type, IDataCallback callBack) {
        this.callback = callBack;
        this.mType = type;
        SenderManager.getInstance().send(this);
    }

    /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/FdlCmdStatusApi$FdlCmdStatusBean.class */
    public static class FdlCmdStatusBean extends ApiData {
        private FdlCmdStatusInfo data;

        public FdlCmdStatusInfo getData() {
            return this.data;
        }

        public void setData(FdlCmdStatusInfo data) {
            this.data = data;
        }

        /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/FdlCmdStatusApi$FdlCmdStatusBean$FdlCmdStatusInfo.class */
        public static class FdlCmdStatusInfo {
            private String type;
            private int status;
            private String msg;

            public String getType() {
                return this.type;
            }

            public void setType(String type) {
                this.type = type;
            }

            public int getStatus() {
                return this.status;
            }

            public void setStatus(int status) {
                this.status = status;
            }

            public String getMsg() {
                return this.msg;
            }

            public void setMsg(String msg) {
                this.msg = msg;
            }

            public boolean isFinish() {
                return 1 == this.status || 2 == this.status;
            }
        }
    }
}
