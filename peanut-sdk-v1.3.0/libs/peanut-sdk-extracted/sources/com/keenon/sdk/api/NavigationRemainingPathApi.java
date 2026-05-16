package com.keenon.sdk.api;

import com.keenon.common.constant.PeanutConstants;
import com.keenon.common.utils.GsonUtil;
import com.keenon.common.utils.LogUtils;
import com.keenon.sdk.coap.adapter.CoapCommond;
import com.keenon.sdk.coap.adapter.CoapResponse;
import com.keenon.sdk.external.IDataCallback;
import com.keenon.sdk.hedera.model.ApiCallback;
import com.keenon.sdk.hedera.model.ApiData;
import com.keenon.sdk.hedera.model.ApiError;
import com.keenon.sdk.hedera.model.RequestEnum;
import com.keenon.sdk.proxy.sender.SenderManager;
import com.keenon.sdk.proxy.sender.anno.LinkAdapter;
import com.keenon.sdk.proxy.sender.anno.RequestType;
import java.util.List;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/NavigationRemainingPathApi.class */
@LinkAdapter
@CoapCommond(path = "/navigation/remainPath")
public class NavigationRemainingPathApi {
    private IDataCallback callBack;
    private RequestEnum requestEnum = RequestEnum.GET;

    @CoapResponse
    ApiCallback coapCallback = new ApiCallback<String>() { // from class: com.keenon.sdk.api.NavigationRemainingPathApi.1
        @Override // com.keenon.sdk.hedera.model.ApiCallback, com.keenon.sdk.hedera.model.ICallback
        public void onSuccess(String result) {
            LogUtils.i(PeanutConstants.TAG_API, "[NavigationRemainingPathApi][onSuccess: " + result + "]");
            Bean dataBean = (Bean) GsonUtil.gson2Bean(result, Bean.class);
            String jsonResult = GsonUtil.bean2String(dataBean);
            LogUtils.d(PeanutConstants.TAG_API, "[NavigationRemainingPathApi][onSuccess json: " + jsonResult + "]");
            if (NavigationRemainingPathApi.this.callBack == null) {
                return;
            }
            NavigationRemainingPathApi.this.callBack.success(jsonResult);
        }

        @Override // com.keenon.sdk.hedera.model.ApiCallback, com.keenon.sdk.hedera.model.ICallback
        public void onFail(ApiError error) {
            if (NavigationRemainingPathApi.this.callBack == null) {
                return;
            }
            NavigationRemainingPathApi.this.callBack.error(error);
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

    /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/NavigationRemainingPathApi$Bean.class */
    public static class Bean extends ApiData {
        private DataBean data;

        public DataBean getData() {
            return this.data;
        }

        public void setData(DataBean data) {
            this.data = data;
        }

        /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/NavigationRemainingPathApi$Bean$DataBean.class */
        public static class DataBean {
            private int dst;
            private List<Integer> remainingPath;

            public String toString() {
                return "DataBean{dst=" + this.dst + ", remainingPath=" + this.remainingPath + '}';
            }

            public int getDst() {
                return this.dst;
            }

            public void setDst(int dst) {
                this.dst = dst;
            }

            public List<Integer> getRemainingPath() {
                return this.remainingPath;
            }

            public void setRemainingPath(List<Integer> remainingPath) {
                this.remainingPath = remainingPath;
            }
        }

        public String toString() {
            return "Bean{data=" + this.data + '}';
        }
    }
}
