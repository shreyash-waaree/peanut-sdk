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
import com.keenon.sdk.hedera.model.RequestEnum;
import com.keenon.sdk.proxy.sender.SenderManager;
import com.keenon.sdk.proxy.sender.anno.LinkAdapter;
import com.keenon.sdk.proxy.sender.anno.RequestType;
import com.keenon.sdk.serial.adapter.SerialCommand;
import com.keenon.sdk.serial.adapter.SerialResponse;
import com.keenon.sdk.serial.base.SerialData;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/NavigationStatusApi.class */
@LinkAdapter
@CoapCommond(path = "/navigation/status")
@SerialCommand(action = "60", body = "24")
public class NavigationStatusApi {
    private IDataCallback callBack;
    private RequestEnum requestEnum = RequestEnum.GET;

    @CoapResponse
    ApiCallback coapCallback = new ApiCallback<String>() { // from class: com.keenon.sdk.api.NavigationStatusApi.1
        @Override // com.keenon.sdk.hedera.model.ApiCallback, com.keenon.sdk.hedera.model.ICallback
        public void onSuccess(String result) {
            LogUtils.i(PeanutConstants.TAG_API, "[NavigationStatusApi][onSuccess: " + result + "]");
            Bean bean = (Bean) GsonUtil.gson2Bean(result, Bean.class);
            bean.setTopic(getClass());
            String jsonResult = GsonUtil.bean2String(bean);
            LogUtils.d(PeanutConstants.TAG_API, "[NavigationStatusApi][onSuccess json: " + jsonResult + "]");
            if (NavigationStatusApi.this.callBack == null) {
                return;
            }
            if (NavigationStatusApi.this.requestEnum == RequestEnum.GET) {
                NavigationStatusApi.this.callBack.success(jsonResult);
            } else if (NavigationStatusApi.this.requestEnum == RequestEnum.OBSERVER) {
                PeanutSDK.getInstance().notifyApiSuccess(this, jsonResult);
            }
        }

        @Override // com.keenon.sdk.hedera.model.ApiCallback, com.keenon.sdk.hedera.model.ICallback
        public void onFail(ApiError error) {
            if (NavigationStatusApi.this.callBack == null) {
                return;
            }
            if (NavigationStatusApi.this.requestEnum == RequestEnum.GET) {
                NavigationStatusApi.this.callBack.error(error);
            } else if (NavigationStatusApi.this.requestEnum == RequestEnum.OBSERVER) {
                PeanutSDK.getInstance().notifyApiError(this, error);
            }
        }
    };

    @SerialResponse
    ApiCallback serialCallback = new ApiCallback<SerialData>() { // from class: com.keenon.sdk.api.NavigationStatusApi.2
        @Override // com.keenon.sdk.hedera.model.ApiCallback, com.keenon.sdk.hedera.model.ICallback
        public void onSuccess(SerialData result) {
            LogUtils.i(PeanutConstants.TAG_API, "[NavigationStatusApi][onSuccess raw: " + ByteUtils.toString(result.getData()) + "]");
            Bean bean = new Bean();
            bean.setTopic(getClass());
            bean.setBytes(result.getData());
            String jsonResult = GsonUtil.bean2String(bean);
            LogUtils.d(PeanutConstants.TAG_API, "[NavigationStatusApi][onSuccess json: " + jsonResult + "]");
            if (NavigationStatusApi.this.requestEnum == RequestEnum.GET) {
                NavigationStatusApi.this.callBack.success(jsonResult);
            } else if (NavigationStatusApi.this.requestEnum == RequestEnum.OBSERVER) {
                PeanutSDK.getInstance().notifyApiSuccess(this, jsonResult);
            }
        }

        @Override // com.keenon.sdk.hedera.model.ApiCallback, com.keenon.sdk.hedera.model.ICallback
        public void onFail(ApiError error) {
            if (NavigationStatusApi.this.requestEnum == RequestEnum.GET) {
                NavigationStatusApi.this.callBack.error(error);
            } else if (NavigationStatusApi.this.requestEnum == RequestEnum.OBSERVER) {
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

    /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/NavigationStatusApi$Bean.class */
    public static class Bean extends ApiData {
        private DataBean data;

        public DataBean getData() {
            return this.data;
        }

        public void setData(DataBean data) {
            this.data = data;
        }

        public void setBytes(Byte[] bytes) {
            setCode(0);
            setStatus(0);
            setMsg(PeanutConstants.API_SUCCESS_MESSAGE);
            DataBean dataBean = new DataBean();
            if (bytes.length > 0) {
                dataBean.setStatus(bytes[0].byteValue() & 255);
            }
            if (bytes.length >= 19) {
                ByteBuffer byteBuffer = ByteBuffer.allocateDirect(bytes.length).order(ByteOrder.LITTLE_ENDIAN);
                byteBuffer.put(ByteUtils.ObjectToByte(bytes, bytes.length));
                byteBuffer.rewind();
                byteBuffer.get();
                dataBean.setDst(byteBuffer.getShort());
                dataBean.setTotal_length(byteBuffer.getFloat());
                dataBean.setRemain_length(byteBuffer.getFloat());
                dataBean.setTotal_time(byteBuffer.getFloat());
                dataBean.setRemain_time(byteBuffer.getFloat());
            }
            setData(dataBean);
        }

        /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/NavigationStatusApi$Bean$DataBean.class */
        public class DataBean {
            private int schedule;
            private int status;
            private int dst;
            private float total_length;
            private float remain_length;
            private float total_time;
            private float remain_time;
            private String desc;

            public DataBean() {
            }

            public String getDesc() {
                return this.desc;
            }

            public void setDesc(String desc) {
                this.desc = desc;
            }

            public int getStatus() {
                return this.status;
            }

            public void setStatus(int status) {
                this.status = status;
            }

            public int getDst() {
                return this.dst;
            }

            public void setDst(int dst) {
                this.dst = dst;
            }

            public float getTotal_length() {
                return this.total_length;
            }

            public void setTotal_length(float total_length) {
                this.total_length = total_length;
            }

            public float getRemain_length() {
                return this.remain_length;
            }

            public void setRemain_length(float remain_length) {
                this.remain_length = remain_length;
            }

            public float getTotal_time() {
                return this.total_time;
            }

            public void setTotal_time(float total_time) {
                this.total_time = total_time;
            }

            public float getRemain_time() {
                return this.remain_time;
            }

            public void setRemain_time(float remain_time) {
                this.remain_time = remain_time;
            }

            public int getSchedule() {
                return this.schedule;
            }

            public void setSchedule(int schedule) {
                this.schedule = schedule;
            }
        }
    }
}
