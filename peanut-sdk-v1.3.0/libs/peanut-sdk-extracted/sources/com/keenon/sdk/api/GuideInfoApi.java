package com.keenon.sdk.api;

import com.google.gson.reflect.TypeToken;
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

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/GuideInfoApi.class */
@LinkAdapter
@CoapCommond(path = "/guide/info")
public class GuideInfoApi {
    private IDataCallback callBack;
    private RequestEnum requestEnum = RequestEnum.GET;

    @CoapResponse
    ApiCallback coapCallback = new ApiCallback<String>() { // from class: com.keenon.sdk.api.GuideInfoApi.1
        /* JADX WARN: Type inference failed for: r1v5, types: [com.keenon.sdk.api.GuideInfoApi$1$1] */
        @Override // com.keenon.sdk.hedera.model.ApiCallback, com.keenon.sdk.hedera.model.ICallback
        public void onSuccess(String result) {
            LogUtils.i(PeanutConstants.TAG_API, "[GuideInfoApi][onSuccess: " + result + "]");
            List<Bean.DataBean> list = GsonUtil.gson2List(result, new TypeToken<List<Bean.DataBean>>() { // from class: com.keenon.sdk.api.GuideInfoApi.1.1
            }.getType());
            Bean bean = new Bean();
            bean.setTopic(getClass());
            bean.setData(list);
            String jsonResult = GsonUtil.bean2String(bean);
            LogUtils.d(PeanutConstants.TAG_API, "[GuideInfoApi][onSuccess json: " + jsonResult + "]");
            if (GuideInfoApi.this.callBack == null) {
                return;
            }
            if (GuideInfoApi.this.requestEnum == RequestEnum.GET) {
                GuideInfoApi.this.callBack.success(jsonResult);
            } else if (GuideInfoApi.this.requestEnum == RequestEnum.OBSERVER) {
                PeanutSDK.getInstance().notifyApiSuccess(this, jsonResult);
            }
        }

        @Override // com.keenon.sdk.hedera.model.ApiCallback, com.keenon.sdk.hedera.model.ICallback
        public void onFail(ApiError error) {
            if (GuideInfoApi.this.callBack == null) {
                return;
            }
            if (GuideInfoApi.this.requestEnum == RequestEnum.GET) {
                GuideInfoApi.this.callBack.error(error);
            } else if (GuideInfoApi.this.requestEnum == RequestEnum.OBSERVER) {
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

    /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/GuideInfoApi$Bean.class */
    public static class Bean extends ApiData {
        private List<DataBean> data;

        public List<DataBean> getData() {
            return this.data;
        }

        public void setData(List<DataBean> data) {
            this.data = data;
        }

        /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/GuideInfoApi$Bean$DataBean.class */
        public class DataBean {
            private int id;
            private int direction;
            private double distance;
            private String dec;

            public DataBean() {
            }

            public String getDec() {
                return this.dec;
            }

            public void setDec(String dec) {
                this.dec = dec;
            }

            public int getId() {
                return this.id;
            }

            public void setId(int id) {
                this.id = id;
            }

            public int getDirection() {
                return this.direction;
            }

            public void setDirection(int direction) {
                this.direction = direction;
            }

            public double getDistance() {
                return this.distance;
            }

            public void setDistance(double distance) {
                this.distance = distance;
            }
        }
    }
}
