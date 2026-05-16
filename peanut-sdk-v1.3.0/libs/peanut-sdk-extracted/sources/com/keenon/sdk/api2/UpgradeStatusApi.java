package com.keenon.sdk.api2;

import com.keenon.common.constant.PeanutConstants;
import com.keenon.common.utils.GsonUtil;
import com.keenon.common.utils.LogUtils;
import com.keenon.sdk.external.IDataCallback;
import com.keenon.sdk.external.PeanutSDK;
import com.keenon.sdk.hedera.model.ApiCallback;
import com.keenon.sdk.hedera.model.ApiData;
import com.keenon.sdk.hedera.model.ApiError;
import com.keenon.sdk.hedera.model.RequestEnum;
import com.keenon.sdk.http.adapter.HttpCommand;
import com.keenon.sdk.http.adapter.HttpResponse;
import com.keenon.sdk.proxy.sender.SenderManager;
import com.keenon.sdk.proxy.sender.anno.LinkAdapter;
import com.keenon.sdk.proxy.sender.anno.RequestType;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api2/UpgradeStatusApi.class */
@LinkAdapter(link = PeanutConstants.LinkType.HTTP, custom = true)
@HttpCommand(path = "/upgrade/status", interval = 200)
public class UpgradeStatusApi {
    private IDataCallback callBack;
    private RequestEnum requestType = RequestEnum.GET;

    @HttpResponse
    ApiCallback httpCallback = new ApiCallback<String>() { // from class: com.keenon.sdk.api2.UpgradeStatusApi.1
        @Override // com.keenon.sdk.hedera.model.ApiCallback, com.keenon.sdk.hedera.model.ICallback
        public void onSuccess(String result) {
            LogUtils.i(PeanutConstants.TAG_API, "[UpgradeStatusApi][onSuccess: " + result + "]");
            Bean bean = (Bean) GsonUtil.gson2Bean(result, Bean.class);
            String jsonResult = GsonUtil.bean2String(bean);
            if (UpgradeStatusApi.this.requestType == RequestEnum.GET) {
                UpgradeStatusApi.this.callBack.success(jsonResult);
            } else if (UpgradeStatusApi.this.requestType == RequestEnum.OBSERVER) {
                PeanutSDK.getInstance().notifyApiSuccess(this, jsonResult);
            }
        }

        @Override // com.keenon.sdk.hedera.model.ApiCallback, com.keenon.sdk.hedera.model.ICallback
        public void onFail(ApiError error) {
            if (UpgradeStatusApi.this.requestType == RequestEnum.GET) {
                UpgradeStatusApi.this.callBack.error(error);
            } else if (UpgradeStatusApi.this.requestType == RequestEnum.OBSERVER) {
                PeanutSDK.getInstance().notifyApiError(this, error);
            }
        }
    };

    @RequestType
    public RequestEnum requestType() {
        return this.requestType;
    }

    public void send(IDataCallback callBack) {
        this.callBack = callBack;
        SenderManager.getInstance().send(this);
    }

    public void observe(IDataCallback callBack) {
        this.requestType = RequestEnum.OBSERVER;
        send(callBack);
    }

    public void cancel() {
        SenderManager.getInstance().cancel(this);
    }

    /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api2/UpgradeStatusApi$Bean.class */
    public class Bean extends ApiData {
        private DataBean data;

        public Bean() {
        }

        public DataBean getData() {
            return this.data;
        }

        public void setData(DataBean data) {
            this.data = data;
        }

        /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api2/UpgradeStatusApi$Bean$DataBean.class */
        public class DataBean {
            private int status;
            private int percent;

            public DataBean() {
            }

            public int getStatus() {
                return this.status;
            }

            public void setStatus(int status) {
                this.status = status;
            }

            public int getPercent() {
                return this.percent;
            }

            public void setPercent(int percent) {
                this.percent = percent;
            }
        }
    }
}
