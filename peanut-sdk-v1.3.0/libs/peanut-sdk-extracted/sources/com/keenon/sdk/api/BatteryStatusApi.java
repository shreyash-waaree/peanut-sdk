package com.keenon.sdk.api;

import com.keenon.common.constant.PeanutConstants;
import com.keenon.common.utils.ByteUtils;
import com.keenon.common.utils.GsonUtil;
import com.keenon.common.utils.LogUtils;
import com.keenon.sdk.coap.adapter.CoapCommond;
import com.keenon.sdk.coap.adapter.CoapResponse;
import com.keenon.sdk.external.IDataCallback;
import com.keenon.sdk.external.PeanutSDK;
import com.keenon.sdk.hedera.model.ApiCallback;
import com.keenon.sdk.hedera.model.ApiData;
import com.keenon.sdk.hedera.model.ApiError;
import com.keenon.sdk.hedera.model.MsgType;
import com.keenon.sdk.hedera.model.RequestEnum;
import com.keenon.sdk.proxy.sender.SenderManager;
import com.keenon.sdk.proxy.sender.anno.LinkAdapter;
import com.keenon.sdk.proxy.sender.anno.RequestType;
import com.keenon.sdk.serial.adapter.SerialCommand;
import com.keenon.sdk.serial.adapter.SerialResponse;
import com.keenon.sdk.serial.base.SerialData;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/BatteryStatusApi.class */
@LinkAdapter
@CoapCommond(path = "/charge/status", msgType = MsgType.CON)
@SerialCommand(action = "60", body = "20")
public class BatteryStatusApi {
    private IDataCallback callBack;
    private RequestEnum requestEnum = RequestEnum.GET;

    @CoapResponse
    ApiCallback coapCallback = new ApiCallback<String>() { // from class: com.keenon.sdk.api.BatteryStatusApi.1
        @Override // com.keenon.sdk.hedera.model.ApiCallback, com.keenon.sdk.hedera.model.ICallback
        public void onSuccess(String result) {
            LogUtils.i(PeanutConstants.TAG_API, "[BatteryStatusApi][onSuccess: " + result + "]");
            Bean bean = (Bean) GsonUtil.gson2Bean(result, Bean.class);
            bean.setTopic(getClass());
            String jsonResult = GsonUtil.bean2String(bean);
            LogUtils.d(PeanutConstants.TAG_API, "[BatteryStatusApi][onSuccess json: " + jsonResult + "]");
            if (BatteryStatusApi.this.callBack == null) {
                return;
            }
            if (BatteryStatusApi.this.requestEnum == RequestEnum.GET) {
                BatteryStatusApi.this.callBack.success(jsonResult);
            } else if (BatteryStatusApi.this.requestEnum == RequestEnum.OBSERVER) {
                PeanutSDK.getInstance().notifyApiSuccess(this, jsonResult);
            }
        }

        @Override // com.keenon.sdk.hedera.model.ApiCallback, com.keenon.sdk.hedera.model.ICallback
        public void onFail(ApiError error) {
            if (BatteryStatusApi.this.callBack == null) {
                return;
            }
            if (BatteryStatusApi.this.requestEnum == RequestEnum.GET) {
                BatteryStatusApi.this.callBack.error(error);
            } else if (BatteryStatusApi.this.requestEnum == RequestEnum.OBSERVER) {
                PeanutSDK.getInstance().notifyApiError(this, error);
            }
        }
    };

    @SerialResponse
    ApiCallback serialCallback = new ApiCallback<SerialData>() { // from class: com.keenon.sdk.api.BatteryStatusApi.2
        @Override // com.keenon.sdk.hedera.model.ApiCallback, com.keenon.sdk.hedera.model.ICallback
        public void onSuccess(SerialData result) {
            LogUtils.i(PeanutConstants.TAG_API, "[BatteryStatusApi][onSuccess raw: " + ByteUtils.toString(result.getData()) + "]");
            Bean bean = new Bean();
            bean.setTopic(getClass());
            bean.setBytes(result.getData());
            String jsonResult = GsonUtil.bean2String(bean);
            LogUtils.d(PeanutConstants.TAG_API, "[BatteryStatusApi][onSuccess json: " + jsonResult + "]");
            if (BatteryStatusApi.this.requestEnum == RequestEnum.GET) {
                BatteryStatusApi.this.callBack.success(jsonResult);
            } else if (BatteryStatusApi.this.requestEnum == RequestEnum.OBSERVER) {
                PeanutSDK.getInstance().notifyApiSuccess(this, jsonResult);
            }
        }

        @Override // com.keenon.sdk.hedera.model.ApiCallback, com.keenon.sdk.hedera.model.ICallback
        public void onFail(ApiError error) {
            if (BatteryStatusApi.this.requestEnum == RequestEnum.GET) {
                BatteryStatusApi.this.callBack.error(error);
            } else if (BatteryStatusApi.this.requestEnum == RequestEnum.OBSERVER) {
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

    /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/BatteryStatusApi$Bean.class */
    public static class Bean extends ApiData {
        private DataBean data;

        public DataBean getData() {
            return this.data;
        }

        public void setData(DataBean data) {
            this.data = data;
        }

        protected void setBytes(Byte[] bytes) {
            setCode(0);
            setStatus(0);
            setMsg(PeanutConstants.API_SUCCESS_MESSAGE);
            DataBean dataBean = new DataBean();
            if (bytes.length >= 2) {
                dataBean.setEvent(bytes[0].byteValue() & 255);
                dataBean.setPower(bytes[1].byteValue() & 255);
            }
            setData(dataBean);
        }

        /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/BatteryStatusApi$Bean$DataBean.class */
        public class DataBean {
            private int power;
            private int event;

            public DataBean() {
            }

            public int getPower() {
                return this.power;
            }

            public void setPower(int power) {
                this.power = power;
            }

            public int getEvent() {
                return this.event;
            }

            public void setEvent(int event) {
                this.event = event;
            }
        }
    }
}
