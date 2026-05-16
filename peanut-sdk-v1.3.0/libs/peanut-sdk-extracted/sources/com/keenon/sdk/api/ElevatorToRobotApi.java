package com.keenon.sdk.api;

import com.keenon.common.constant.PeanutConstants;
import com.keenon.common.utils.GsonUtil;
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
import com.keenon.sdk.proxy.sender.anno.RequestType;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/ElevatorToRobotApi.class */
@LinkAdapter
@CoapCommond(path = "/elevator/forwardToRobot")
public class ElevatorToRobotApi {
    private IDataCallback callBack;
    private String info;
    private RequestEnum requestEnum = RequestEnum.POST;

    @CoapResponse
    ApiCallback coapCallback = new ApiCallback<String>() { // from class: com.keenon.sdk.api.ElevatorToRobotApi.1
        @Override // com.keenon.sdk.hedera.model.ApiCallback, com.keenon.sdk.hedera.model.ICallback
        public void onSuccess(String result) {
            LogUtils.i(PeanutConstants.TAG_API, "[ElevatorToRobotApi][onSuccess: " + result + "]");
            Bean bean = (Bean) GsonUtil.gson2Bean(result, Bean.class);
            bean.setTopic(getClass());
            String jsonResult = GsonUtil.bean2String(bean);
            LogUtils.d(PeanutConstants.TAG_API, "[ElevatorToRobotApi][onSuccess json: " + jsonResult + "]");
            if (ElevatorToRobotApi.this.callBack == null) {
                return;
            }
            ElevatorToRobotApi.this.callBack.success(jsonResult);
        }

        @Override // com.keenon.sdk.hedera.model.ApiCallback, com.keenon.sdk.hedera.model.ICallback
        public void onFail(ApiError error) {
            if (ElevatorToRobotApi.this.callBack == null) {
                return;
            }
            ElevatorToRobotApi.this.callBack.error(error);
        }
    };

    @RequestType
    public RequestEnum requestType() {
        return this.requestEnum;
    }

    @CoapParams
    public String CoapParams() {
        return this.info;
    }

    public void send(IDataCallback callBack, String info) {
        this.callBack = callBack;
        this.info = info;
        SenderManager.getInstance().send(this);
    }

    /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/ElevatorToRobotApi$Bean.class */
    public static class Bean extends ApiData {
        private DataBean data;

        public DataBean getData() {
            return this.data;
        }

        public void setData(DataBean data) {
            this.data = data;
        }

        /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/ElevatorToRobotApi$Bean$DataBean.class */
        public class DataBean {
            private String payload;

            public DataBean() {
            }

            public String getPayload() {
                return this.payload;
            }

            public void setPayload(String payload) {
                this.payload = payload;
            }
        }
    }
}
