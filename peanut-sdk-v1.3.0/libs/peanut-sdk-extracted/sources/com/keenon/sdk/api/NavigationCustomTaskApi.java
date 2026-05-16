package com.keenon.sdk.api;

import com.keenon.common.constant.PeanutConstants;
import com.keenon.common.utils.GsonUtil;
import com.keenon.common.utils.LogUtils;
import com.keenon.sdk.coap.adapter.CoapCommond;
import com.keenon.sdk.coap.adapter.CoapParams;
import com.keenon.sdk.coap.adapter.CoapResponse;
import com.keenon.sdk.external.IDataCallback;
import com.keenon.sdk.hedera.model.ApiCallback;
import com.keenon.sdk.hedera.model.ApiData;
import com.keenon.sdk.hedera.model.ApiError;
import com.keenon.sdk.hedera.model.RequestEnum;
import com.keenon.sdk.proxy.sender.SenderManager;
import com.keenon.sdk.proxy.sender.anno.LinkAdapter;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/NavigationCustomTaskApi.class */
@LinkAdapter
@CoapCommond(path = "/navigation/customTask", requestType = RequestEnum.POST)
public class NavigationCustomTaskApi {
    private IDataCallback callBack;
    private int dst;
    private JSONArray pathArray;

    @CoapResponse
    ApiCallback coapCallback = new ApiCallback<String>() { // from class: com.keenon.sdk.api.NavigationCustomTaskApi.1
        @Override // com.keenon.sdk.hedera.model.ApiCallback, com.keenon.sdk.hedera.model.ICallback
        public void onSuccess(String result) {
            LogUtils.i(PeanutConstants.TAG_API, "[NavigationCustomTaskApi][onSuccess: " + result + "]");
            ApiData bean = (ApiData) GsonUtil.gson2Bean(result, ApiData.class);
            String jsonResult = GsonUtil.bean2String(bean);
            LogUtils.d(PeanutConstants.TAG_API, "[NavigationCustomTaskApi][onSuccess json: " + jsonResult + "]");
            if (NavigationCustomTaskApi.this.callBack == null) {
                return;
            }
            NavigationCustomTaskApi.this.callBack.success(jsonResult);
        }

        @Override // com.keenon.sdk.hedera.model.ApiCallback, com.keenon.sdk.hedera.model.ICallback
        public void onFail(ApiError error) {
            if (NavigationCustomTaskApi.this.callBack == null) {
                return;
            }
            NavigationCustomTaskApi.this.callBack.error(error);
        }
    };
    private List<Integer> path = new ArrayList();

    @CoapParams
    public String CoapParams() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("dst", this.dst);
            jsonObject.putOpt("path", getPathArray());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject.toString();
    }

    private JSONArray getPathArray() {
        this.pathArray = new JSONArray();
        if (this.path.size() > 0) {
            for (Integer integer : this.path) {
                this.pathArray.put(integer);
            }
        }
        return this.pathArray;
    }

    public void send(IDataCallback callBack, int dst, List<Integer> path) {
        this.callBack = callBack;
        this.dst = dst;
        if (path != null) {
            this.path = path;
        }
        SenderManager.getInstance().send(this);
    }
}
