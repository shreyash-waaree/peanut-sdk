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

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/DevicesCheckApi.class */
@LinkAdapter
@CoapCommond(path = "/devices/reportCheck")
public class DevicesCheckApi {
    private IDataCallback callBack;
    private RequestEnum requestEnum = RequestEnum.GET;

    @CoapResponse
    ApiCallback coapCallback = new ApiCallback<String>() { // from class: com.keenon.sdk.api.DevicesCheckApi.1
        @Override // com.keenon.sdk.hedera.model.ApiCallback, com.keenon.sdk.hedera.model.ICallback
        public void onSuccess(String result) {
            LogUtils.i(PeanutConstants.TAG_API, "[ReportCheckApi][onSuccess: " + result + "]");
            Bean bean = (Bean) GsonUtil.gson2Bean(result, Bean.class);
            bean.setTopic(getClass());
            String jsonResult = GsonUtil.bean2String(bean);
            LogUtils.d(PeanutConstants.TAG_API, "[ReportCheckApi][onSuccess json: " + jsonResult + "]");
            if (DevicesCheckApi.this.callBack == null) {
                return;
            }
            if (DevicesCheckApi.this.requestEnum == RequestEnum.GET) {
                DevicesCheckApi.this.callBack.success(jsonResult);
            } else if (DevicesCheckApi.this.requestEnum == RequestEnum.OBSERVER) {
                PeanutSDK.getInstance().notifyApiSuccess(this, jsonResult);
            }
        }

        @Override // com.keenon.sdk.hedera.model.ApiCallback, com.keenon.sdk.hedera.model.ICallback
        public void onFail(ApiError error) {
            if (DevicesCheckApi.this.callBack == null) {
                return;
            }
            if (DevicesCheckApi.this.requestEnum == RequestEnum.GET) {
                DevicesCheckApi.this.callBack.error(error);
            } else if (DevicesCheckApi.this.requestEnum == RequestEnum.OBSERVER) {
                PeanutSDK.getInstance().notifyApiError(this, error);
            }
        }
    };

    @RequestType
    public RequestEnum requestType() {
        return this.requestEnum;
    }

    public void observe(IDataCallback callBack) {
        this.requestEnum = RequestEnum.OBSERVER;
        send(callBack);
    }

    public void cancel() {
        SenderManager.getInstance().cancel(this);
    }

    public void send(IDataCallback callBack) {
        this.callBack = callBack;
        SenderManager.getInstance().send(this);
    }

    /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/DevicesCheckApi$Bean.class */
    public static class Bean extends ApiData {
        private List<DataBean> data;

        public List<DataBean> getData() {
            return this.data;
        }

        public void setData(List<DataBean> data) {
            this.data = data;
        }

        /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/DevicesCheckApi$Bean$DataBean.class */
        public static class DataBean {
            private int id;
            private String name;
            private int status;
            private DetailBean detail;

            public int getId() {
                return this.id;
            }

            public void setId(int id) {
                this.id = id;
            }

            public String getName() {
                return this.name;
            }

            public void setName(String name) {
                this.name = name;
            }

            public int getStatus() {
                return this.status;
            }

            public void setStatus(int status) {
                this.status = status;
            }

            public DetailBean getDetail() {
                return this.detail;
            }

            public void setDetail(DetailBean detail) {
                this.detail = detail;
            }

            /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/DevicesCheckApi$Bean$DataBean$DetailBean.class */
            public static class DetailBean {
                private VersionBean version;

                public VersionBean getVersion() {
                    return this.version;
                }

                public void setVersion(VersionBean version) {
                    this.version = version;
                }

                /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/DevicesCheckApi$Bean$DataBean$DetailBean$VersionBean.class */
                public static class VersionBean {
                    private String sw;
                    private String hw;

                    public String getSw() {
                        return this.sw;
                    }

                    public void setSw(String sw) {
                        this.sw = sw;
                    }

                    public String getHw() {
                        return this.hw;
                    }

                    public void setHw(String hw) {
                        this.hw = hw;
                    }
                }
            }
        }
    }
}
