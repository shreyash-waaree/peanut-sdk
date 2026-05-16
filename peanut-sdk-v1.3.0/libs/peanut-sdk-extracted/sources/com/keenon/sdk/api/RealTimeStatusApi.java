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
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/RealTimeStatusApi.class */
@LinkAdapter
@CoapCommond(path = "/diagnosis/realtimeStatus", requestType = RequestEnum.POST)
public class RealTimeStatusApi {
    private IDataCallback callBack;
    private int id;
    private List<Integer> sceneIdList;
    private String jsonString;

    @CoapResponse
    ApiCallback coapCallback = new ApiCallback<String>() { // from class: com.keenon.sdk.api.RealTimeStatusApi.1
        @Override // com.keenon.sdk.hedera.model.ApiCallback, com.keenon.sdk.hedera.model.ICallback
        public void onSuccess(String result) {
            LogUtils.i(PeanutConstants.TAG_API, "[RealTimeStatusApi][onSuccess: " + result + "]");
            Bean bean = (Bean) GsonUtil.gson2Bean(result, Bean.class);
            String jsonResult = GsonUtil.bean2String(bean);
            LogUtils.d(PeanutConstants.TAG_API, "[RealTimeStatusApi][onSuccess json: " + jsonResult + "]");
            if (RealTimeStatusApi.this.callBack == null) {
                return;
            }
            RealTimeStatusApi.this.callBack.success(jsonResult);
        }

        @Override // com.keenon.sdk.hedera.model.ApiCallback, com.keenon.sdk.hedera.model.ICallback
        public void onFail(ApiError error) {
            if (RealTimeStatusApi.this.callBack == null) {
                return;
            }
            RealTimeStatusApi.this.callBack.error(error);
        }
    };

    public void send(IDataCallback callBack, int id, List<Integer> sceneIdList) {
        this.callBack = callBack;
        this.id = id;
        this.sceneIdList = sceneIdList;
        this.jsonString = "";
        SenderManager.getInstance().send(this);
    }

    public void send(IDataCallback callBack, String jsonString) {
        this.callBack = callBack;
        this.jsonString = jsonString;
        SenderManager.getInstance().send(this);
    }

    @CoapParams
    public String CoapParams() {
        if (this.jsonString != null && !this.jsonString.isEmpty()) {
            return this.jsonString;
        }
        JSONObject jSONObject = new JSONObject();
        JSONObject data = new JSONObject();
        JSONObject JsonScene = new JSONObject();
        JSONArray jsonArray = new JSONArray();
        try {
            data.put("id", this.id);
            for (Integer sceneId : this.sceneIdList) {
                data.put("id", sceneId);
                jsonArray.put(JsonScene);
            }
            data.put("scene", jsonArray);
            jSONObject.put("module", data);
        } catch (JSONException e) {
            LogUtils.e(PeanutConstants.TAG_API, "[RealTimeStatusApi]", (Exception) e);
        }
        return jSONObject.toString();
    }

    /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/RealTimeStatusApi$Bean.class */
    public static class Bean extends ApiData {
        public DataBean data;

        /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/RealTimeStatusApi$Bean$DataBean.class */
        public class DataBean {
            public int id;
            public List<SceneBean> scene;

            public DataBean() {
            }

            /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/RealTimeStatusApi$Bean$DataBean$SceneBean.class */
            public class SceneBean {
                public int id;
                public String value;
                public int state;

                public SceneBean() {
                }
            }
        }
    }
}
