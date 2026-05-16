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
import org.json.JSONException;
import org.json.JSONObject;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/MapDownloadV2Api.class */
@LinkAdapter
@CoapCommond(path = "/runtime/download", requestType = RequestEnum.POST)
public class MapDownloadV2Api {
    private IDataCallback callBack;

    @CoapResponse
    ApiCallback coapCallback = new ApiCallback<String>() { // from class: com.keenon.sdk.api.MapDownloadV2Api.1
        @Override // com.keenon.sdk.hedera.model.ApiCallback, com.keenon.sdk.hedera.model.ICallback
        public void onSuccess(String result) {
            LogUtils.i(PeanutConstants.TAG_API, "[MapDownloadV2Api][onSuccess: " + result + "]");
            Bean bean = (Bean) GsonUtil.gson2Bean(result, Bean.class);
            String jsonResult = GsonUtil.bean2String(bean);
            LogUtils.d(PeanutConstants.TAG_API, "[MapDownloadV2Api][onSuccess json: " + jsonResult + "]");
            if (MapDownloadV2Api.this.callBack == null) {
                return;
            }
            MapDownloadV2Api.this.callBack.success(jsonResult);
        }

        @Override // com.keenon.sdk.hedera.model.ApiCallback, com.keenon.sdk.hedera.model.ICallback
        public void onFail(ApiError error) {
            if (MapDownloadV2Api.this.callBack == null) {
                return;
            }
            MapDownloadV2Api.this.callBack.error(error);
        }
    };
    private ParamSendDownloadInfo param;

    @CoapParams
    public String CoapParams() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("md5", this.param.getMd5());
            jsonObject.put("url", this.param.getUrl());
            jsonObject.put("type", this.param.getType());
            jsonObject.put("mapUuid", this.param.getMapUuid());
        } catch (JSONException e) {
            LogUtils.e(PeanutConstants.TAG_API, "[MapDownloadV2Api]", (Exception) e);
        }
        return jsonObject.toString();
    }

    public void send(ParamSendDownloadInfo param, IDataCallback callBack) {
        this.callBack = callBack;
        this.param = param;
        SenderManager.getInstance().send(this);
    }

    /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/MapDownloadV2Api$ParamSendDownloadInfo.class */
    public static class ParamSendDownloadInfo {
        private String md5;
        private String url;
        private String type;
        private String mapUuid;

        public ParamSendDownloadInfo() {
        }

        public ParamSendDownloadInfo(String md5, String url, String type, String mapUuid) {
            this.md5 = md5;
            this.url = url;
            this.type = type;
            this.mapUuid = mapUuid;
        }

        public String getMd5() {
            return this.md5;
        }

        public void setMd5(String md5) {
            this.md5 = md5;
        }

        public String getUrl() {
            return this.url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getType() {
            return this.type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getMapUuid() {
            return this.mapUuid;
        }

        public void setMapUuid(String mapUuid) {
            this.mapUuid = mapUuid;
        }
    }

    /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/MapDownloadV2Api$Bean.class */
    public static class Bean extends ApiData {
        private DataBean data;

        public DataBean getData() {
            return this.data;
        }

        public void setData(DataBean data) {
            this.data = data;
        }

        /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/MapDownloadV2Api$Bean$DataBean.class */
        public class DataBean {
            private int status;

            public DataBean() {
            }

            public int getStatus() {
                return this.status;
            }

            public void setStatus(int status) {
                this.status = status;
            }
        }
    }
}
