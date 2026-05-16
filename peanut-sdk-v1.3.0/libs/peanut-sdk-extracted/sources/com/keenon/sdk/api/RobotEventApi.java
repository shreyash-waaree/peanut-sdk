package com.keenon.sdk.api;

import com.keenon.common.constant.PeanutConstants;
import com.keenon.common.utils.GsonUtil;
import com.keenon.common.utils.LogUtils;
import com.keenon.sdk.coap.adapter.CoapCommond;
import com.keenon.sdk.coap.adapter.CoapResponse;
import com.keenon.sdk.external.IDataCallback;
import com.keenon.sdk.external.PeanutSDK;
import com.keenon.sdk.hedera.model.ApiCallback;
import com.keenon.sdk.hedera.model.ApiData;
import com.keenon.sdk.hedera.model.ApiError;
import com.keenon.sdk.hedera.model.RequestEnum;
import com.keenon.sdk.proxy.sender.SenderManager;
import com.keenon.sdk.proxy.sender.anno.LinkAdapter;
import com.keenon.sdk.proxy.sender.anno.RequestType;
import java.util.List;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/RobotEventApi.class */
@LinkAdapter
@CoapCommond(path = "/diagnosis/robotEvent", requestType = RequestEnum.GET)
public class RobotEventApi {
    private IDataCallback callBack;
    private RequestEnum requestEnum = RequestEnum.GET;

    @CoapResponse
    ApiCallback coapCallback = new ApiCallback<String>() { // from class: com.keenon.sdk.api.RobotEventApi.1
        @Override // com.keenon.sdk.hedera.model.ApiCallback, com.keenon.sdk.hedera.model.ICallback
        public void onSuccess(String result) {
            LogUtils.i(PeanutConstants.TAG_API, "[RobotEventApi][onSuccess: " + result + "]");
            if (result == null || "null".equalsIgnoreCase(result)) {
                return;
            }
            Bean bean = (Bean) GsonUtil.gson2Bean(result, Bean.class);
            String jsonResult = GsonUtil.bean2String(bean);
            LogUtils.d(PeanutConstants.TAG_API, "[RobotEventApi][onSuccess json: " + jsonResult + "]");
            if (RobotEventApi.this.callBack == null) {
                return;
            }
            if (RobotEventApi.this.requestEnum == RequestEnum.GET) {
                RobotEventApi.this.callBack.success(jsonResult);
            } else if (RobotEventApi.this.requestEnum == RequestEnum.OBSERVER) {
                PeanutSDK.getInstance().notifyApiSuccess(this, jsonResult);
            }
        }

        @Override // com.keenon.sdk.hedera.model.ApiCallback, com.keenon.sdk.hedera.model.ICallback
        public void onFail(ApiError error) {
            if (RobotEventApi.this.callBack == null) {
                return;
            }
            if (RobotEventApi.this.requestEnum == RequestEnum.GET) {
                RobotEventApi.this.callBack.error(error);
            } else if (RobotEventApi.this.requestEnum == RequestEnum.OBSERVER) {
                PeanutSDK.getInstance().notifyApiError(this, error);
            }
        }
    };

    /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/RobotEventApi$Bean.class */
    public static class Bean extends ApiData {
        public List<DataBean> data;

        /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/RobotEventApi$Bean$DataBean.class */
        public static class DataBean {
            public String type;
            public String id;
            public String parent;
            public int status;
            public String info;
            public int code;
        }
    }

    @RequestType
    public RequestEnum requestType() {
        return this.requestEnum;
    }

    public void send(IDataCallback callBack) {
        this.callBack = callBack;
        SenderManager.getInstance().send(this);
    }

    public void observe(IDataCallback callBack) {
        this.requestEnum = RequestEnum.OBSERVER;
        send(callBack);
    }

    public void cancel() {
        SenderManager.getInstance().cancel(this);
    }
}
