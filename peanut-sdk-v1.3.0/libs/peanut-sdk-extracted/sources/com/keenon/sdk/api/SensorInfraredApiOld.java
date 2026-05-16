package com.keenon.sdk.api;

import com.keenon.common.constant.PeanutConstants;
import com.keenon.common.utils.ByteUtils;
import com.keenon.common.utils.GsonUtil;
import com.keenon.common.utils.LogUtils;
import com.keenon.sdk.constant.ApiConstants;
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
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/SensorInfraredApiOld.class */
@LinkAdapter(link = PeanutConstants.LinkType.COM, com = PeanutConstants.COM2, custom = true)
@SerialCommand(action = "68", body = "2b")
public class SensorInfraredApiOld {
    private IDataCallback callBack;
    private RequestEnum requestEnum = RequestEnum.GET;

    @SerialResponse
    ApiCallback serialCallback = new ApiCallback<SerialData>() { // from class: com.keenon.sdk.api.SensorInfraredApiOld.1
        @Override // com.keenon.sdk.hedera.model.ApiCallback, com.keenon.sdk.hedera.model.ICallback
        public void onSuccess(SerialData result) {
            LogUtils.i(PeanutConstants.TAG_API, "[SensorInfraredApiOld][onSuccess raw: " + ByteUtils.toString(result.getData()) + "]");
            Bean bean = new Bean();
            bean.setBytes(result.getData());
            bean.setTopic(getClass());
            String jsonResult = GsonUtil.bean2String(bean);
            LogUtils.d(PeanutConstants.TAG_API, "[SensorInfraredApiOld][onSuccess json: " + jsonResult + "]");
            if (SensorInfraredApiOld.this.callBack == null) {
                return;
            }
            if (SensorInfraredApiOld.this.requestEnum == RequestEnum.GET) {
                SensorInfraredApiOld.this.callBack.success(jsonResult);
            } else if (SensorInfraredApiOld.this.requestEnum == RequestEnum.OBSERVER) {
                PeanutSDK.getInstance().notifyApiSuccess(this, jsonResult);
            }
        }

        @Override // com.keenon.sdk.hedera.model.ApiCallback, com.keenon.sdk.hedera.model.ICallback
        public void onFail(ApiError error) {
            if (SensorInfraredApiOld.this.callBack == null) {
                return;
            }
            if (SensorInfraredApiOld.this.requestEnum == RequestEnum.GET) {
                SensorInfraredApiOld.this.callBack.error(error);
            } else if (SensorInfraredApiOld.this.requestEnum == RequestEnum.OBSERVER) {
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

    /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/SensorInfraredApiOld$Bean.class */
    public static class Bean extends ApiData implements Serializable {
        private List<DataBean> data;

        public List<DataBean> getData() {
            return this.data;
        }

        public void setData(List<DataBean> data) {
            this.data = data;
        }

        protected void setBytes(Byte[] bytes) {
            setCode(0);
            setStatus(0);
            setMsg(PeanutConstants.API_SUCCESS_MESSAGE);
            List<DataBean> dataList = new ArrayList<>(3);
            for (int i = 0; i < 3; i++) {
                dataList.add(new DataBean(isBitSet(bytes[1].byteValue(), i), isBitSet(bytes[0].byteValue(), i)));
            }
            setData(dataList);
        }

        private static boolean isBitSet(byte b, int num) {
            return (b & (1 << num)) > 0;
        }

        /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/SensorInfraredApiOld$Bean$DataBean.class */
        public static class DataBean {
            boolean ifChanged;
            boolean ifDetected;

            public boolean isIfChanged() {
                return this.ifChanged;
            }

            public void setIfChanged(boolean ifChanged) {
                this.ifChanged = ifChanged;
            }

            public boolean isIfDetected() {
                return this.ifDetected;
            }

            public void setIfDetected(boolean ifDetected) {
                this.ifDetected = ifDetected;
            }

            public DataBean(boolean ifChanged, boolean ifDetected) {
                this.ifChanged = ifChanged;
                this.ifDetected = ifDetected;
            }

            public String toString() {
                return "(" + this.ifChanged + ApiConstants.DELIMITER_COMMA + this.ifDetected + ')';
            }
        }
    }
}
