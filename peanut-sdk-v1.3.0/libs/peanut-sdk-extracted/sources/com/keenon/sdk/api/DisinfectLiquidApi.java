package com.keenon.sdk.api;

import com.keenon.common.constant.PeanutConstants;
import com.keenon.common.utils.ByteUtils;
import com.keenon.common.utils.GsonUtil;
import com.keenon.common.utils.LogUtils;
import com.keenon.sdk.external.IDataCallback;
import com.keenon.sdk.external.PeanutSDK;
import com.keenon.sdk.hedera.model.ApiCallback;
import com.keenon.sdk.hedera.model.ApiData;
import com.keenon.sdk.hedera.model.ApiError;
import com.keenon.sdk.hedera.model.RequestEnum;
import com.keenon.sdk.proxy.sender.SenderManager;
import com.keenon.sdk.proxy.sender.anno.LinkAdapter;
import com.keenon.sdk.proxy.sender.anno.RequestType;
import com.keenon.sdk.serial.adapter.SerialCommand;
import com.keenon.sdk.serial.adapter.SerialResponse;
import com.keenon.sdk.serial.base.SerialData;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/DisinfectLiquidApi.class */
@LinkAdapter(link = PeanutConstants.LinkType.COM, com = PeanutConstants.COM2, custom = true)
@SerialCommand(action = "53", body = "21")
public class DisinfectLiquidApi {
    private IDataCallback callBack;
    private RequestEnum requestEnum = RequestEnum.GET;

    @SerialResponse
    ApiCallback serialCallback = new ApiCallback<SerialData>() { // from class: com.keenon.sdk.api.DisinfectLiquidApi.1
        @Override // com.keenon.sdk.hedera.model.ApiCallback, com.keenon.sdk.hedera.model.ICallback
        public void onSuccess(SerialData result) {
            LogUtils.i(PeanutConstants.TAG_API, "[DisinfectLiquidApi][onSuccess raw: " + ByteUtils.toString(result.getData()) + "]");
            Bean bean = new Bean();
            bean.setTopic(getClass());
            bean.setBytes(result.getData());
            String jsonResult = GsonUtil.bean2String(bean);
            LogUtils.d(PeanutConstants.TAG_API, "[DisinfectLiquidApi][onSuccess json: " + jsonResult + "]");
            if (DisinfectLiquidApi.this.callBack == null) {
                return;
            }
            if (DisinfectLiquidApi.this.requestEnum == RequestEnum.GET) {
                DisinfectLiquidApi.this.callBack.success(jsonResult);
            } else if (DisinfectLiquidApi.this.requestEnum == RequestEnum.OBSERVER) {
                PeanutSDK.getInstance().notifyApiSuccess(this, jsonResult);
            }
        }

        @Override // com.keenon.sdk.hedera.model.ApiCallback, com.keenon.sdk.hedera.model.ICallback
        public void onFail(ApiError error) {
            if (DisinfectLiquidApi.this.callBack == null) {
                return;
            }
            if (DisinfectLiquidApi.this.requestEnum == RequestEnum.GET) {
                DisinfectLiquidApi.this.callBack.error(error);
            } else if (DisinfectLiquidApi.this.requestEnum == RequestEnum.OBSERVER) {
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

    /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/DisinfectLiquidApi$Bean.class */
    public static class Bean extends ApiData {
        private DataBean data;

        public DataBean getData() {
            return this.data;
        }

        public void setData(DataBean data) {
            this.data = data;
        }

        protected void setBytes(Byte[] bytes) {
            setCode(0);
            setStatus(0);
            setMsg(PeanutConstants.API_SUCCESS_MESSAGE);
            DataBean dataBean = new DataBean();
            if (bytes.length >= 2) {
                dataBean.setLevel(((bytes[0].byteValue() & 255) << 8) + (bytes[1].byteValue() & 255));
            } else if (bytes.length == 1) {
                dataBean.setPercent(bytes[0].byteValue() & 255);
            }
            setData(dataBean);
        }

        /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/DisinfectLiquidApi$Bean$DataBean.class */
        public class DataBean {
            private int percent;
            private int level;

            public DataBean() {
            }

            public int getPercent() {
                return this.percent;
            }

            public void setPercent(int progress) {
                this.percent = progress;
            }

            public int getLevel() {
                return this.level;
            }

            public void setLevel(int level) {
                this.level = level;
            }
        }
    }
}
