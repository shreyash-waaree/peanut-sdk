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

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/SensorImuAngleApi.class */
@LinkAdapter
@CoapCommond(path = "/sensor/imuangle")
public class SensorImuAngleApi {
    private IDataCallback callBack;
    private RequestEnum requestEnum = RequestEnum.GET;

    @CoapResponse
    ApiCallback coapCallback = new ApiCallback<String>() { // from class: com.keenon.sdk.api.SensorImuAngleApi.1
        @Override // com.keenon.sdk.hedera.model.ApiCallback, com.keenon.sdk.hedera.model.ICallback
        public void onSuccess(String result) {
            LogUtils.i(PeanutConstants.TAG_API, "[SensorImuAngleApi][onSuccess: " + result + "]");
            Bean bean = (Bean) GsonUtil.gson2Bean(result, Bean.class);
            bean.setTopic(getClass());
            String jsonResult = GsonUtil.bean2String(bean);
            LogUtils.d(PeanutConstants.TAG_API, "[SensorImuAngleApi][onSuccess json: " + jsonResult + "]");
            if (SensorImuAngleApi.this.callBack == null) {
                return;
            }
            if (SensorImuAngleApi.this.requestEnum == RequestEnum.GET) {
                SensorImuAngleApi.this.callBack.success(jsonResult);
            } else if (SensorImuAngleApi.this.requestEnum == RequestEnum.OBSERVER) {
                PeanutSDK.getInstance().notifyApiSuccess(this, jsonResult);
            }
        }

        @Override // com.keenon.sdk.hedera.model.ApiCallback, com.keenon.sdk.hedera.model.ICallback
        public void onFail(ApiError error) {
            if (SensorImuAngleApi.this.callBack == null) {
                return;
            }
            if (SensorImuAngleApi.this.requestEnum == RequestEnum.GET) {
                SensorImuAngleApi.this.callBack.error(error);
            } else if (SensorImuAngleApi.this.requestEnum == RequestEnum.OBSERVER) {
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

    /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/SensorImuAngleApi$Bean.class */
    public static class Bean extends ApiData implements Serializable {
        private DataBean data;

        public DataBean getData() {
            return this.data;
        }

        public void setData(DataBean data) {
            this.data = data;
        }

        /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/SensorImuAngleApi$Bean$DataBean.class */
        public static class DataBean {
            private float pitch;
            private float roll;
            private float yaw;

            public float getPitch() {
                return this.pitch;
            }

            public void setPitch(float pitch) {
                this.pitch = pitch;
            }

            public float getRoll() {
                return this.roll;
            }

            public void setRoll(float roll) {
                this.roll = roll;
            }

            public float getYaw() {
                return this.yaw;
            }

            public void setYaw(float yaw) {
                this.yaw = yaw;
            }

            public String toString() {
                return "{pitch=" + this.pitch + ", roll=" + this.roll + ", yaw=" + this.yaw + '}';
            }
        }
    }
}
