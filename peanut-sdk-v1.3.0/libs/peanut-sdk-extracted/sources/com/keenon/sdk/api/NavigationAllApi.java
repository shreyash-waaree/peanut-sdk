package com.keenon.sdk.api;

import com.keenon.common.constant.PeanutConstants;
import com.keenon.common.utils.LogUtils;
import com.keenon.sdk.coap.adapter.CoapCommond;
import com.keenon.sdk.coap.adapter.CoapResponse;
import com.keenon.sdk.external.IDataCallback;
import com.keenon.sdk.hedera.model.ApiCallback;
import com.keenon.sdk.hedera.model.ApiData;
import com.keenon.sdk.hedera.model.ApiError;
import com.keenon.sdk.proxy.sender.SenderManager;
import com.keenon.sdk.proxy.sender.anno.LinkAdapter;
import java.util.List;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/NavigationAllApi.class */
@LinkAdapter
@CoapCommond(path = "/navigation/all")
public class NavigationAllApi {
    private IDataCallback callBack;

    @CoapResponse
    ApiCallback coapCallback = new ApiCallback<String>() { // from class: com.keenon.sdk.api.NavigationAllApi.1
        @Override // com.keenon.sdk.hedera.model.ApiCallback, com.keenon.sdk.hedera.model.ICallback
        public void onSuccess(String result) {
            LogUtils.i(PeanutConstants.TAG_API, "[NavigationAllApi][onSuccess: " + result + "]");
            if (NavigationAllApi.this.callBack == null) {
                return;
            }
            NavigationAllApi.this.callBack.success(result);
        }

        @Override // com.keenon.sdk.hedera.model.ApiCallback, com.keenon.sdk.hedera.model.ICallback
        public void onFail(ApiError error) {
            if (NavigationAllApi.this.callBack == null) {
                return;
            }
            NavigationAllApi.this.callBack.error(error);
        }
    };

    public void send(IDataCallback callBack) {
        this.callBack = callBack;
        SenderManager.getInstance().send(this);
    }

    /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/NavigationAllApi$Bean.class */
    public static class Bean extends ApiData {
        private DataBean data;

        public DataBean getData() {
            return this.data;
        }

        public void setData(DataBean data) {
            this.data = data;
        }

        /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/NavigationAllApi$Bean$DataBean.class */
        public class DataBean {
            private int count;
            private List<Integer> list;
            private List<Integer> chargers;
            private List<Integer> origins;
            private List<Integer> elevators;

            public DataBean() {
            }

            public int getCount() {
                return this.count;
            }

            public void setCount(int count) {
                this.count = count;
            }

            public List<Integer> getList() {
                return this.list;
            }

            public void setList(List<Integer> list) {
                this.list = list;
            }

            public List<Integer> getChargers() {
                return this.chargers;
            }

            public void setChargers(List<Integer> chargers) {
                this.chargers = chargers;
            }

            public List<Integer> getOrigins() {
                return this.origins;
            }

            public void setOrigins(List<Integer> origins) {
                this.origins = origins;
            }

            public List<Integer> getElevators() {
                return this.elevators;
            }

            public void setElevators(List<Integer> elevators) {
                this.elevators = elevators;
            }
        }
    }
}
