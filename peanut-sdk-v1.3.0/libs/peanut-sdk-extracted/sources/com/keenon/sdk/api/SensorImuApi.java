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
import java.io.Serializable;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/SensorImuApi.class */
@LinkAdapter
@CoapCommond(path = "/sensor/imu")
public class SensorImuApi {
    private IDataCallback callBack;
    private RequestEnum requestEnum = RequestEnum.GET;

    @CoapResponse
    ApiCallback coapCallback = new ApiCallback<String>() { // from class: com.keenon.sdk.api.SensorImuApi.1
        @Override // com.keenon.sdk.hedera.model.ApiCallback, com.keenon.sdk.hedera.model.ICallback
        public void onSuccess(String result) {
            LogUtils.i(PeanutConstants.TAG_API, "[SensorImuApi][onSuccess: " + result + "]");
            Bean bean = (Bean) GsonUtil.gson2Bean(result, Bean.class);
            bean.setTopic(getClass());
            String jsonResult = GsonUtil.bean2String(bean);
            LogUtils.d(PeanutConstants.TAG_API, "[SensorImuApi][onSuccess json: " + jsonResult + "]");
            if (SensorImuApi.this.callBack == null) {
                return;
            }
            if (SensorImuApi.this.requestEnum == RequestEnum.GET) {
                SensorImuApi.this.callBack.success(jsonResult);
            } else if (SensorImuApi.this.requestEnum == RequestEnum.OBSERVER) {
                PeanutSDK.getInstance().notifyApiSuccess(this, jsonResult);
            }
        }

        @Override // com.keenon.sdk.hedera.model.ApiCallback, com.keenon.sdk.hedera.model.ICallback
        public void onFail(ApiError error) {
            if (SensorImuApi.this.callBack == null) {
                return;
            }
            if (SensorImuApi.this.requestEnum == RequestEnum.GET) {
                SensorImuApi.this.callBack.error(error);
            } else if (SensorImuApi.this.requestEnum == RequestEnum.OBSERVER) {
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

    /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/SensorImuApi$Bean.class */
    public static class Bean extends ApiData implements Serializable {
        private int status;
        private int code;
        private String msg;
        private DataBean data;

        @Override // com.keenon.sdk.hedera.model.ApiData
        public int getStatus() {
            return this.status;
        }

        @Override // com.keenon.sdk.hedera.model.ApiData
        public void setStatus(int status) {
            this.status = status;
        }

        @Override // com.keenon.sdk.hedera.model.ApiData
        public int getCode() {
            return this.code;
        }

        @Override // com.keenon.sdk.hedera.model.ApiData
        public void setCode(int code) {
            this.code = code;
        }

        @Override // com.keenon.sdk.hedera.model.ApiData
        public String getMsg() {
            return this.msg;
        }

        @Override // com.keenon.sdk.hedera.model.ApiData
        public void setMsg(String msg) {
            this.msg = msg;
        }

        public DataBean getData() {
            return this.data;
        }

        public void setData(DataBean data) {
            this.data = data;
        }

        public String toString() {
            return "{status=" + this.status + ", code=" + this.code + ", msg=" + this.msg + ", data=" + this.data.toString() + '}';
        }

        /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/SensorImuApi$Bean$DataBean.class */
        public static class DataBean {
            private int imu;

            public int getImu() {
                return this.imu;
            }

            public void setImu(int imu) {
                this.imu = imu;
            }

            public String toString() {
                return "{imu=" + this.imu + '}';
            }
        }
    }
}
