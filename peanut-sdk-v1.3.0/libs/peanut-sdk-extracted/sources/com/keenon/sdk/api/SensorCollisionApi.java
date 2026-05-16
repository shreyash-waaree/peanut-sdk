package com.keenon.sdk.api;

import com.google.gson.annotations.SerializedName;
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

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/SensorCollisionApi.class */
@LinkAdapter
@CoapCommond(path = "/sensor/bump")
public class SensorCollisionApi {
    private IDataCallback callBack;
    private RequestEnum requestEnum = RequestEnum.GET;

    @CoapResponse
    ApiCallback coapCallback = new ApiCallback<String>() { // from class: com.keenon.sdk.api.SensorCollisionApi.1
        @Override // com.keenon.sdk.hedera.model.ApiCallback, com.keenon.sdk.hedera.model.ICallback
        public void onSuccess(String result) {
            LogUtils.i(PeanutConstants.TAG_API, "[SensorCollisionApi][onSuccess: " + result + "]");
            Bean bean = (Bean) GsonUtil.gson2Bean(result, Bean.class);
            bean.setTopic(getClass());
            String jsonResult = GsonUtil.bean2String(bean);
            LogUtils.d(PeanutConstants.TAG_API, "[SensorCollisionApi][onSuccess json: " + jsonResult + "]");
            if (SensorCollisionApi.this.callBack == null) {
                return;
            }
            if (SensorCollisionApi.this.requestEnum == RequestEnum.GET) {
                SensorCollisionApi.this.callBack.success(jsonResult);
            } else if (SensorCollisionApi.this.requestEnum == RequestEnum.OBSERVER) {
                PeanutSDK.getInstance().notifyApiSuccess(this, jsonResult);
            }
        }

        @Override // com.keenon.sdk.hedera.model.ApiCallback, com.keenon.sdk.hedera.model.ICallback
        public void onFail(ApiError error) {
            if (SensorCollisionApi.this.callBack == null) {
                return;
            }
            if (SensorCollisionApi.this.requestEnum == RequestEnum.GET) {
                SensorCollisionApi.this.callBack.error(error);
            } else if (SensorCollisionApi.this.requestEnum == RequestEnum.OBSERVER) {
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

    /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/SensorCollisionApi$Bean.class */
    public static class Bean extends ApiData {
        private DataBean data;

        public DataBean getData() {
            return this.data;
        }

        public void setData(DataBean data) {
            this.data = data;
        }

        /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/SensorCollisionApi$Bean$DataBean.class */
        public class DataBean {
            private int state;
            private boolean right;

            @SerializedName("mid-right")
            private boolean midright;

            @SerializedName("mid-left")
            private boolean midleft;
            private boolean left;
            private boolean back;
            private boolean front;

            public DataBean() {
            }

            public int getState() {
                return this.state;
            }

            public void setState(int state) {
                this.state = state;
            }

            public boolean isRight() {
                return this.right;
            }

            public void setRight(boolean right) {
                this.right = right;
            }

            public boolean isMidright() {
                return this.midright;
            }

            public void setMidright(boolean midright) {
                this.midright = midright;
            }

            public boolean isMidleft() {
                return this.midleft;
            }

            public void setMidleft(boolean midleft) {
                this.midleft = midleft;
            }

            public boolean isLeft() {
                return this.left;
            }

            public void setLeft(boolean left) {
                this.left = left;
            }

            public boolean isBack() {
                return this.back;
            }

            public void setBack(boolean back) {
                this.back = back;
            }

            public boolean isFront() {
                return this.front;
            }

            public void setFront(boolean front) {
                this.front = front;
            }
        }
    }
}
