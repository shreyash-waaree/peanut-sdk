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

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/NavigationDisinfectStatusApi.class */
@LinkAdapter
@CoapCommond(path = "/navigation/disinfectStatus")
public class NavigationDisinfectStatusApi {
    private IDataCallback callBack;
    private RequestEnum requestEnum = RequestEnum.GET;

    @CoapResponse
    ApiCallback coapCallback = new ApiCallback<String>() { // from class: com.keenon.sdk.api.NavigationDisinfectStatusApi.1
        @Override // com.keenon.sdk.hedera.model.ApiCallback, com.keenon.sdk.hedera.model.ICallback
        public void onSuccess(String result) {
            LogUtils.i(PeanutConstants.TAG_API, "[NavigationDisinfectStatusApi][onSuccess: " + result + "]");
            Bean bean = (Bean) GsonUtil.gson2Bean(result, Bean.class);
            bean.setTopic(getClass());
            String jsonResult = GsonUtil.bean2String(bean);
            LogUtils.d(PeanutConstants.TAG_API, "[NavigationDisinfectStatusApi][onSuccess json: " + jsonResult + "]");
            if (NavigationDisinfectStatusApi.this.callBack == null) {
                return;
            }
            if (NavigationDisinfectStatusApi.this.requestEnum == RequestEnum.GET) {
                NavigationDisinfectStatusApi.this.callBack.success(jsonResult);
            } else if (NavigationDisinfectStatusApi.this.requestEnum == RequestEnum.OBSERVER) {
                PeanutSDK.getInstance().notifyApiSuccess(this, jsonResult);
            }
        }

        @Override // com.keenon.sdk.hedera.model.ApiCallback, com.keenon.sdk.hedera.model.ICallback
        public void onFail(ApiError error) {
            if (NavigationDisinfectStatusApi.this.callBack == null) {
                return;
            }
            if (NavigationDisinfectStatusApi.this.requestEnum == RequestEnum.GET) {
                NavigationDisinfectStatusApi.this.callBack.error(error);
            } else if (NavigationDisinfectStatusApi.this.requestEnum == RequestEnum.OBSERVER) {
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

    /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/NavigationDisinfectStatusApi$Bean.class */
    public static class Bean extends ApiData {
        public DataBean data;

        /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/NavigationDisinfectStatusApi$Bean$DataBean.class */
        public static class DataBean {
            private int status;
            private double cleanArea;
            private double cleanTime;
            private double cleanPercent;
            private List<CleanPathBean> cleanPath;
            private List<UncleanPathBean> uncleanPath;

            public int getStatus() {
                return this.status;
            }

            public void setStatus(int status) {
                this.status = status;
            }

            public double getCleanArea() {
                return this.cleanArea;
            }

            public void setCleanArea(double cleanArea) {
                this.cleanArea = cleanArea;
            }

            public double getCleanTime() {
                return this.cleanTime;
            }

            public void setCleanTime(double cleanTime) {
                this.cleanTime = cleanTime;
            }

            public double getCleanPercent() {
                return this.cleanPercent;
            }

            public void setCleanPercent(double cleanPercent) {
                this.cleanPercent = cleanPercent;
            }

            public List<CleanPathBean> getCleanPath() {
                return this.cleanPath;
            }

            public void setCleanPath(List<CleanPathBean> cleanPath) {
                this.cleanPath = cleanPath;
            }

            public List<UncleanPathBean> getUncleanPath() {
                return this.uncleanPath;
            }

            public void setUncleanPath(List<UncleanPathBean> uncleanPath) {
                this.uncleanPath = uncleanPath;
            }

            /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/NavigationDisinfectStatusApi$Bean$DataBean$CleanPathBean.class */
            public static class CleanPathBean {
                private double x;
                private double y;
                private double z;

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

                public double getZ() {
                    return this.z;
                }

                public void setZ(double z) {
                    this.z = z;
                }
            }

            /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/NavigationDisinfectStatusApi$Bean$DataBean$UncleanPathBean.class */
            public static class UncleanPathBean {
                private double x;
                private double y;
                private double z;

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

                public double getZ() {
                    return this.z;
                }

                public void setZ(double z) {
                    this.z = z;
                }
            }
        }
    }
}
