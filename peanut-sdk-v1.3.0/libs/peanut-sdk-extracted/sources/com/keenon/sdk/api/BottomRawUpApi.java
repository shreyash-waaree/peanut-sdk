package com.keenon.sdk.api;

import com.keenon.common.constant.PeanutConstants;
import com.keenon.common.utils.GsonUtil;
import com.keenon.common.utils.LogUtils;
import com.keenon.common.utils.StringUtils;
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
import com.keenon.sdk.scmIot.SCMIoTSender;
import java.util.ArrayList;
import java.util.List;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/BottomRawUpApi.class */
@LinkAdapter
@CoapCommond(path = "/through/up")
public class BottomRawUpApi {
    private IDataCallback callBack;
    private RequestEnum requestEnum = RequestEnum.GET;

    @CoapResponse
    ApiCallback coapCallback = new ApiCallback<String>() { // from class: com.keenon.sdk.api.BottomRawUpApi.1
        @Override // com.keenon.sdk.hedera.model.ApiCallback, com.keenon.sdk.hedera.model.ICallback
        public void onSuccess(String result) {
            LogUtils.i(PeanutConstants.TAG_API, "[BottomRawUpApi][onSuccess: " + result + "]");
            Bean bean = (Bean) GsonUtil.gson2Bean(result, Bean.class);
            if (bean != null && bean.getData() != null) {
                for (String transItem : bean.getData().getTransList()) {
                    String jsonResult = SCMIoTSender.receiveResponse(transItem);
                    LogUtils.d(PeanutConstants.TAG_API, "[BottomRawUpApi][onSuccess json: " + jsonResult + "]");
                    if (BottomRawUpApi.this.callBack == null) {
                        return;
                    }
                    if (StringUtils.isNotEmpty(jsonResult)) {
                        if (BottomRawUpApi.this.requestEnum == RequestEnum.GET) {
                            BottomRawUpApi.this.callBack.success(jsonResult);
                        } else if (BottomRawUpApi.this.requestEnum == RequestEnum.OBSERVER) {
                            PeanutSDK.getInstance().notifyApiSuccess(this, jsonResult);
                        }
                    }
                }
            }
        }

        @Override // com.keenon.sdk.hedera.model.ApiCallback, com.keenon.sdk.hedera.model.ICallback
        public void onFail(ApiError error) {
            if (BottomRawUpApi.this.callBack == null) {
                return;
            }
            if (BottomRawUpApi.this.requestEnum == RequestEnum.GET) {
                BottomRawUpApi.this.callBack.error(error);
            } else if (BottomRawUpApi.this.requestEnum == RequestEnum.OBSERVER) {
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

    /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/BottomRawUpApi$Bean.class */
    public static class Bean extends ApiData {
        private DataBean data;

        public DataBean getData() {
            return this.data;
        }

        public void setData(DataBean data) {
            this.data = data;
        }

        /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/BottomRawUpApi$Bean$DataBean.class */
        public class DataBean {
            private String trans;
            private List<String> transList;
            private int aisleType;

            public DataBean() {
            }

            public int getAisleType() {
                return this.aisleType;
            }

            public void setAisleType(int aisleType) {
                this.aisleType = aisleType;
            }

            public String getTrans() {
                return this.trans;
            }

            public void setTrans(String trans) {
                this.trans = trans;
            }

            public List<String> getTransList() {
                if (null == this.transList || 0 == this.transList.size()) {
                    this.transList = new ArrayList();
                    if (StringUtils.isNotEmpty(this.trans)) {
                        this.transList.add(this.trans);
                    }
                }
                return this.transList;
            }

            public void setTransList(List<String> transList) {
                this.transList = transList;
            }
        }
    }
}
