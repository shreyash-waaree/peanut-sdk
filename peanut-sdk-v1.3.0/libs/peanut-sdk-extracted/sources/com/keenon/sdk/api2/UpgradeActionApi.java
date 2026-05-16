package com.keenon.sdk.api2;

import com.keenon.common.constant.PeanutConstants;
import com.keenon.common.utils.GsonUtil;
import com.keenon.common.utils.LogUtils;
import com.keenon.sdk.external.IDataCallback;
import com.keenon.sdk.hedera.model.ApiCallback;
import com.keenon.sdk.hedera.model.ApiError;
import com.keenon.sdk.hedera.model.RequestEnum;
import com.keenon.sdk.http.adapter.HttpCommand;
import com.keenon.sdk.http.adapter.HttpParams;
import com.keenon.sdk.http.adapter.HttpResponse;
import com.keenon.sdk.proxy.sender.SenderManager;
import com.keenon.sdk.proxy.sender.anno.LinkAdapter;
import org.json.JSONException;
import org.json.JSONObject;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api2/UpgradeActionApi.class */
@LinkAdapter(link = PeanutConstants.LinkType.HTTP, custom = true)
@HttpCommand(path = "/upgrade/action", requestType = RequestEnum.POST)
public class UpgradeActionApi {
    public static final String ACTION_START = "start";
    public static final String ACTION_STOP = "stop";
    private IDataCallback callBack;

    @HttpResponse
    ApiCallback httpCallback = new ApiCallback<String>() { // from class: com.keenon.sdk.api2.UpgradeActionApi.1
        @Override // com.keenon.sdk.hedera.model.ApiCallback, com.keenon.sdk.hedera.model.ICallback
        public void onSuccess(String result) {
            LogUtils.i(PeanutConstants.TAG_API, "[UpgradeActionApi][onSuccess: " + result + "]");
            UpgradeActionApi.this.callBack.success(result);
        }

        @Override // com.keenon.sdk.hedera.model.ApiCallback, com.keenon.sdk.hedera.model.ICallback
        public void onFail(ApiError error) {
            UpgradeActionApi.this.callBack.error(error);
        }
    };
    private String action;
    private Package data;

    @HttpParams
    public String Params() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("action", this.action);
            if (this.data != null) {
                jsonObject.put("data", new JSONObject(GsonUtil.bean2String(this.data)));
            }
        } catch (JSONException e) {
            LogUtils.e(PeanutConstants.TAG_API, "[UpgradeActionApi]", (Exception) e);
        }
        return jsonObject.toString();
    }

    public void send(IDataCallback callBack, String action, Package data) {
        this.callBack = callBack;
        this.action = action;
        this.data = data;
        SenderManager.getInstance().send(this);
    }

    /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api2/UpgradeActionApi$Package.class */
    public static class Package {
        String obj;
        String url;
        String md5;

        public String getObj() {
            return this.obj;
        }

        public void setObj(String obj) {
            this.obj = obj;
        }

        public String getUrl() {
            return this.url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getMd5() {
            return this.md5;
        }

        public void setMd5(String md5) {
            this.md5 = md5;
        }
    }
}
