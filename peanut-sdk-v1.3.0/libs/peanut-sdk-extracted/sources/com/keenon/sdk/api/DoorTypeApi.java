package com.keenon.sdk.api;

import com.keenon.common.constant.PeanutConstants;
import com.keenon.common.utils.ByteUtils;
import com.keenon.common.utils.GsonUtil;
import com.keenon.common.utils.LogUtils;
import com.keenon.sdk.external.IDataCallback;
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

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/DoorTypeApi.class */
@LinkAdapter(board = PeanutConstants.BoardType.DOOR, link = PeanutConstants.LinkType.COM)
@SerialCommand(action = "74", body = "20")
public class DoorTypeApi {
    private IDataCallback callBack;
    private RequestEnum requestEnum = RequestEnum.OBSERVER;

    @SerialResponse
    ApiCallback serialCallback = new ApiCallback<SerialData>() { // from class: com.keenon.sdk.api.DoorTypeApi.1
        @Override // com.keenon.sdk.hedera.model.ApiCallback, com.keenon.sdk.hedera.model.ICallback
        public void onSuccess(SerialData result) {
            LogUtils.i(PeanutConstants.TAG_API, "[DoorTypeApi][onSuccess raw: " + ByteUtils.toString(result.getData()) + "]");
            Bean bean = new Bean();
            bean.setTopic(getClass());
            bean.setBytes(result.getData());
            String jsonResult = GsonUtil.bean2String(bean);
            LogUtils.d(PeanutConstants.TAG_API, "[DoorTypeApi][onSuccess json: " + jsonResult + "]");
            if (DoorTypeApi.this.callBack == null) {
                return;
            }
            DoorTypeApi.this.callBack.success(jsonResult);
        }

        @Override // com.keenon.sdk.hedera.model.ApiCallback, com.keenon.sdk.hedera.model.ICallback
        public void onFail(ApiError error) {
            if (DoorTypeApi.this.callBack == null) {
                return;
            }
            DoorTypeApi.this.callBack.error(error);
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

    /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/DoorTypeApi$Bean.class */
    public static class Bean extends ApiData {
        private DataBean data;

        public DataBean getData() {
            return this.data;
        }

        public void setData(DataBean data) {
            this.data = data;
        }

        protected void setBytes(Byte[] bytes) {
            int gatingType;
            setCode(0);
            setStatus(0);
            setMsg(PeanutConstants.API_SUCCESS_MESSAGE);
            DataBean dataBean = new DataBean();
            if ((bytes[0].byteValue() & 255) == 255 && (bytes[1].byteValue() & 255) == 255) {
                gatingType = 1;
            } else if ((bytes[0].byteValue() & 255) == 0 && (bytes[1].byteValue() & 255) == 0) {
                gatingType = 2;
            } else if ((bytes[0].byteValue() & 255) == 0 && (bytes[1].byteValue() & 255) == 255) {
                gatingType = 3;
            } else {
                gatingType = 4;
            }
            dataBean.status = gatingType;
            setData(dataBean);
        }

        /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/DoorTypeApi$Bean$DataBean.class */
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
