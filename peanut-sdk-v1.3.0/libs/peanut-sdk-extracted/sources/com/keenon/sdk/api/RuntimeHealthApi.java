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

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/RuntimeHealthApi.class */
@LinkAdapter
@CoapCommond(path = "/runtime/error")
public class RuntimeHealthApi {
    private IDataCallback callBack;
    private RequestEnum requestEnum = RequestEnum.GET;

    @CoapResponse
    ApiCallback coapCallback = new ApiCallback<String>() { // from class: com.keenon.sdk.api.RuntimeHealthApi.1
        @Override // com.keenon.sdk.hedera.model.ApiCallback, com.keenon.sdk.hedera.model.ICallback
        public void onSuccess(String result) {
            LogUtils.i(PeanutConstants.TAG_API, "[RuntimeHealthApi][onSuccess: " + result + "]");
            Bean bean = (Bean) GsonUtil.gson2Bean(result, Bean.class);
            bean.setTopic(getClass());
            String jsonResult = GsonUtil.bean2String(bean);
            LogUtils.d(PeanutConstants.TAG_API, "[RuntimeHealthApi][onSuccess json: " + jsonResult + "]");
            if (RuntimeHealthApi.this.callBack == null) {
                return;
            }
            if (RuntimeHealthApi.this.requestEnum == RequestEnum.GET) {
                RuntimeHealthApi.this.callBack.success(jsonResult);
            } else if (RuntimeHealthApi.this.requestEnum == RequestEnum.OBSERVER) {
                PeanutSDK.getInstance().notifyApiSuccess(this, jsonResult);
            }
        }

        @Override // com.keenon.sdk.hedera.model.ApiCallback, com.keenon.sdk.hedera.model.ICallback
        public void onFail(ApiError error) {
            if (RuntimeHealthApi.this.callBack == null) {
                return;
            }
            if (RuntimeHealthApi.this.requestEnum == RequestEnum.GET) {
                RuntimeHealthApi.this.callBack.error(error);
            } else if (RuntimeHealthApi.this.requestEnum == RequestEnum.OBSERVER) {
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

    /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/RuntimeHealthApi$Bean.class */
    public static class Bean extends ApiData {
        private List<DataBean> data;

        /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/RuntimeHealthApi$Bean$DataBean.class */
        public class DataBean {
            private int code;
            private String desc;
            private String title;
            private String data;
            private PoseBean pose;

            public DataBean() {
            }

            public int getCode() {
                return this.code;
            }

            public void setCode(int code) {
                this.code = code;
            }

            public String getDesc() {
                return this.desc;
            }

            public void setDesc(String desc) {
                this.desc = desc;
            }

            public String getTitle() {
                return this.title;
            }

            public void setTitle(String title) {
                this.title = title;
            }

            public String getData() {
                return this.data;
            }

            public void setData(String data) {
                this.data = data;
            }

            public PoseBean getPose() {
                return this.pose;
            }

            public void setPose(PoseBean pose) {
                this.pose = pose;
            }

            /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/RuntimeHealthApi$Bean$DataBean$PoseBean.class */
            public class PoseBean {
                private double x;
                private double y;
                private double phi;

                public PoseBean() {
                }

                public double getX() {
                    return this.x;
                }

                public void setX(double x) {
                    this.x = x;
                }

                public double getY() {
                    return this.y;
                }

                public void setY(double y) {
                    this.y = y;
                }

                public double getPhi() {
                    return this.phi;
                }

                public void setPhi(double phi) {
                    this.phi = phi;
                }
            }
        }
    }
}
