package com.keenon.sdk.api;

import com.keenon.common.constant.PeanutConstants;
import com.keenon.common.utils.ByteUtils;
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
import com.keenon.sdk.serial.adapter.SerialCommand;
import com.keenon.sdk.serial.adapter.SerialParams;
import com.keenon.sdk.serial.adapter.SerialResponse;
import com.keenon.sdk.serial.base.SerialData;
import org.json.JSONException;
import org.json.JSONObject;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/NavigationSetTargetApi.class */
@LinkAdapter
@CoapCommond(path = "/navigation/dst", requestType = RequestEnum.POST)
@SerialCommand(action = "62", body = "25")
public class NavigationSetTargetApi {
    private IDataCallback callBack;
    private int target;

    @CoapResponse
    ApiCallback coapCallback = new ApiCallback<String>() { // from class: com.keenon.sdk.api.NavigationSetTargetApi.1
        @Override // com.keenon.sdk.hedera.model.ApiCallback, com.keenon.sdk.hedera.model.ICallback
        public void onSuccess(String result) {
            LogUtils.i(PeanutConstants.TAG_API, "[NavigationSetTargetApi][onSuccess: " + result + "]");
            ApiData bean = (ApiData) GsonUtil.gson2Bean(result, ApiData.class);
            String jsonResult = GsonUtil.bean2String(bean);
            LogUtils.d(PeanutConstants.TAG_API, "[NavigationSetTargetApi][onSuccess json: " + jsonResult + "]");
            if (NavigationSetTargetApi.this.callBack == null) {
                return;
            }
            NavigationSetTargetApi.this.callBack.success(jsonResult);
        }

        @Override // com.keenon.sdk.hedera.model.ApiCallback, com.keenon.sdk.hedera.model.ICallback
        public void onFail(ApiError error) {
            if (NavigationSetTargetApi.this.callBack == null) {
                return;
            }
            NavigationSetTargetApi.this.callBack.error(error);
        }
    };

    @SerialResponse
    ApiCallback serialCallback = new ApiCallback<SerialData>() { // from class: com.keenon.sdk.api.NavigationSetTargetApi.2
        @Override // com.keenon.sdk.hedera.model.ApiCallback, com.keenon.sdk.hedera.model.ICallback
        public void onSuccess(SerialData result) {
            LogUtils.i(PeanutConstants.TAG_API, "[NavigationSetTargetApi][onSuccess raw: " + ByteUtils.toString(result.getData()) + "]");
            ApiData bean = new ApiData();
            bean.setStatus(result.getStatus());
            bean.setCode(result.getData());
            String jsonResult = GsonUtil.bean2String(bean);
            LogUtils.d(PeanutConstants.TAG_API, "[NavigationSetTargetApi][onSuccess json: " + jsonResult + "]");
            NavigationSetTargetApi.this.callBack.success(jsonResult);
        }

        @Override // com.keenon.sdk.hedera.model.ApiCallback, com.keenon.sdk.hedera.model.ICallback
        public void onFail(ApiError error) {
            NavigationSetTargetApi.this.callBack.error(error);
        }
    };
    private int taskType = -1;
    private int takeControl = -1;

    @CoapParams
    public String CoapParams() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("dst", this.target);
            if (this.taskType != -1) {
                jsonObject.put("taskType", this.taskType);
            }
            if (this.takeControl == 1) {
                jsonObject.put("takeControl", this.takeControl);
            }
        } catch (JSONException e) {
            LogUtils.e(PeanutConstants.TAG_API, "[NavigationSetTargetApi]", (Exception) e);
        }
        return jsonObject.toString();
    }

    @SerialParams
    public Byte[] SerialParams() {
        return new Byte[]{Byte.valueOf((byte) (this.target & 255)), Byte.valueOf((byte) ((this.target & 65280) >> 8))};
    }

    public void send(IDataCallback callBack, int target) {
        this.callBack = callBack;
        this.target = target;
        SenderManager.getInstance().send(this);
    }

    public void send(IDataCallback callBack, int target, int taskType) {
        this.callBack = callBack;
        this.target = target;
        this.taskType = taskType;
        SenderManager.getInstance().send(this);
    }

    public void send(IDataCallback callBack, int target, int taskType, int takeControl) {
        this.callBack = callBack;
        this.target = target;
        this.taskType = taskType;
        this.takeControl = takeControl;
        SenderManager.getInstance().send(this);
    }
}
