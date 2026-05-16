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

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/ScheduleLiveApi.class */
@LinkAdapter
@CoapCommond(path = "/schedule/live")
public class ScheduleLiveApi {
    private IDataCallback callBack;
    private RequestEnum requestEnum = RequestEnum.GET;

    @CoapResponse
    ApiCallback coapCallback = new ApiCallback<String>() { // from class: com.keenon.sdk.api.ScheduleLiveApi.1
        @Override // com.keenon.sdk.hedera.model.ApiCallback, com.keenon.sdk.hedera.model.ICallback
        public void onSuccess(String result) {
            LogUtils.i(PeanutConstants.TAG_API, "[ScheduleLiveApi][onSuccess: " + result + "]");
            Bean bean = (Bean) GsonUtil.gson2Bean(result, Bean.class);
            bean.setTopic(getClass());
            String jsonResult = GsonUtil.bean2String(bean);
            LogUtils.d(PeanutConstants.TAG_API, "[ScheduleLiveApi][onSuccess json: " + jsonResult + "]");
            if (ScheduleLiveApi.this.callBack == null) {
                return;
            }
            if (ScheduleLiveApi.this.requestEnum == RequestEnum.GET) {
                ScheduleLiveApi.this.callBack.success(jsonResult);
            } else if (ScheduleLiveApi.this.requestEnum == RequestEnum.OBSERVER) {
                PeanutSDK.getInstance().notifyApiSuccess(this, jsonResult);
            }
        }

        @Override // com.keenon.sdk.hedera.model.ApiCallback, com.keenon.sdk.hedera.model.ICallback
        public void onFail(ApiError error) {
            if (ScheduleLiveApi.this.callBack == null) {
                return;
            }
            if (ScheduleLiveApi.this.requestEnum == RequestEnum.GET) {
                ScheduleLiveApi.this.callBack.error(error);
            } else if (ScheduleLiveApi.this.requestEnum == RequestEnum.OBSERVER) {
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

    /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/ScheduleLiveApi$Bean.class */
    public static class Bean extends ApiData {
        private DataBean data;

        public DataBean getData() {
            return this.data;
        }

        public void setData(DataBean data) {
            this.data = data;
        }

        /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/ScheduleLiveApi$Bean$DataBean.class */
        public class DataBean {
            private LocalBean local;
            private List<OtherBean> other;

            public DataBean() {
            }

            public LocalBean getLocal() {
                return this.local;
            }

            public void setLocal(LocalBean local) {
                this.local = local;
            }

            public List<OtherBean> getOther() {
                return this.other;
            }

            public void setOther(List<OtherBean> other) {
                this.other = other;
            }

            /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/ScheduleLiveApi$Bean$DataBean$LocalBean.class */
            public class LocalBean {
                private int id;
                private int status;
                private double x;
                private double y;
                private List<Integer> connection;

                public LocalBean() {
                }

                public int getId() {
                    return this.id;
                }

                public void setId(int id) {
                    this.id = id;
                }

                public int getStatus() {
                    return this.status;
                }

                public void setStatus(int status) {
                    this.status = status;
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

                public List<Integer> getConnection() {
                    return this.connection;
                }

                public void setConnection(List<Integer> connection) {
                    this.connection = connection;
                }

                public boolean isSchedulerIng() {
                    return (this.status == 0 || this.status == -1) ? false : true;
                }
            }

            /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/ScheduleLiveApi$Bean$DataBean$OtherBean.class */
            public class OtherBean {
                private int id;
                private int status;
                private double x;
                private double y;
                private List<Integer> connection;

                public OtherBean() {
                }

                public int getId() {
                    return this.id;
                }

                public void setId(int id) {
                    this.id = id;
                }

                public int getStatus() {
                    return this.status;
                }

                public void setStatus(int status) {
                    this.status = status;
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

                public List<Integer> getConnection() {
                    return this.connection;
                }

                public void setConnection(List<Integer> connection) {
                    this.connection = connection;
                }

                public boolean isSchedulerIng() {
                    return (this.status == 0 || this.status == -1) ? false : true;
                }
            }
        }
    }
}
