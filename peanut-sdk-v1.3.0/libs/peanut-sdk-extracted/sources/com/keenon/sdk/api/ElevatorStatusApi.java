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

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/ElevatorStatusApi.class */
@LinkAdapter
@CoapCommond(path = "/elevator/status")
public class ElevatorStatusApi {
    private IDataCallback callBack;
    private RequestEnum requestEnum = RequestEnum.GET;

    @CoapResponse
    ApiCallback coapCallback = new ApiCallback<String>() { // from class: com.keenon.sdk.api.ElevatorStatusApi.1
        @Override // com.keenon.sdk.hedera.model.ApiCallback, com.keenon.sdk.hedera.model.ICallback
        public void onSuccess(String result) {
            LogUtils.i(PeanutConstants.TAG_API, "[ElevatorStatusApi][onSuccess: " + result + "]");
            Bean bean = (Bean) GsonUtil.gson2Bean(result, Bean.class);
            bean.setTopic(getClass());
            String jsonResult = GsonUtil.bean2String(bean);
            LogUtils.d(PeanutConstants.TAG_API, "[ElevatorStatusApi][onSuccess json: " + jsonResult + "]");
            if (ElevatorStatusApi.this.callBack == null) {
                return;
            }
            if (ElevatorStatusApi.this.requestEnum == RequestEnum.GET) {
                ElevatorStatusApi.this.callBack.success(jsonResult);
            } else if (ElevatorStatusApi.this.requestEnum == RequestEnum.OBSERVER) {
                PeanutSDK.getInstance().notifyApiSuccess(this, jsonResult);
            }
        }

        @Override // com.keenon.sdk.hedera.model.ApiCallback, com.keenon.sdk.hedera.model.ICallback
        public void onFail(ApiError error) {
            if (ElevatorStatusApi.this.callBack == null) {
                return;
            }
            if (ElevatorStatusApi.this.requestEnum == RequestEnum.GET) {
                ElevatorStatusApi.this.callBack.error(error);
            } else if (ElevatorStatusApi.this.requestEnum == RequestEnum.OBSERVER) {
                PeanutSDK.getInstance().notifyApiError(this, error);
            }
        }
    };

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

    /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/ElevatorStatusApi$Bean.class */
    public static class Bean extends ApiData {
        private DataBean data;

        public DataBean getData() {
            return this.data;
        }

        public void setData(DataBean data) {
            this.data = data;
        }

        /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/ElevatorStatusApi$Bean$DataBean.class */
        public class DataBean {
            private int status;
            private int dstFloor;
            private int curFloor;
            private int elevatorId;
            private int errorCode;
            private String taskNo;
            private int retryTimes;

            public DataBean() {
            }

            public int getRetryTimes() {
                return this.retryTimes;
            }

            public void setRetryTimes(int retryTimes) {
                this.retryTimes = retryTimes;
            }

            public int getElevatorId() {
                return this.elevatorId;
            }

            public void setElevatorId(int elevatorId) {
                this.elevatorId = elevatorId;
            }

            public int getErrorCode() {
                return this.errorCode;
            }

            public void setErrorCode(int errorCode) {
                this.errorCode = errorCode;
            }

            public String getTaskNo() {
                return this.taskNo;
            }

            public void setTaskNo(String taskNo) {
                this.taskNo = taskNo;
            }

            public int getStatus() {
                return this.status;
            }

            public void setStatus(int status) {
                this.status = status;
            }

            public int getDstFloor() {
                return this.dstFloor;
            }

            public void setDstFloor(int dstFloor) {
                this.dstFloor = dstFloor;
            }

            public int getCurFloor() {
                return this.curFloor;
            }

            public void setCurFloor(int curFloor) {
                this.curFloor = curFloor;
            }
        }
    }
}
