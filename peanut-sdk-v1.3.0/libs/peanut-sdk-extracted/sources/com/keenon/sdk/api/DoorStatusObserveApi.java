package com.keenon.sdk.api;

import com.keenon.common.constant.PeanutConstants;
import com.keenon.common.utils.ByteUtils;
import com.keenon.common.utils.GsonUtil;
import com.keenon.common.utils.LogUtils;
import com.keenon.sdk.external.IDataCallback;
import com.keenon.sdk.external.PeanutSDK;
import com.keenon.sdk.hedera.model.ApiCallback;
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
import java.util.Arrays;
import java.util.List;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/DoorStatusObserveApi.class */
@LinkAdapter(board = PeanutConstants.BoardType.DOOR, link = PeanutConstants.LinkType.COM)
@SerialCommand(action = "E4", body = "20")
public class DoorStatusObserveApi {
    private IDataCallback callBack;
    private RequestEnum requestEnum = RequestEnum.OBSERVER;

    @SerialResponse
    ApiCallback serialCallback = new ApiCallback<SerialData>() { // from class: com.keenon.sdk.api.DoorStatusObserveApi.1
        @Override // com.keenon.sdk.hedera.model.ApiCallback, com.keenon.sdk.hedera.model.ICallback
        public void onSuccess(SerialData result) {
            LogUtils.i(PeanutConstants.TAG_API, "[DoorStatusObserveApi][onSuccess raw: " + ByteUtils.toString(result.getData()) + "]");
            Bean bean = DoorStatusObserveApi.this.new Bean();
            bean.setBytes(result.getData());
            String jsonResult = GsonUtil.bean2String(bean);
            LogUtils.d(PeanutConstants.TAG_API, "[DoorStatusObserveApi][onSuccess json: " + jsonResult + "]");
            if (DoorStatusObserveApi.this.callBack == null) {
                return;
            }
            if (DoorStatusObserveApi.this.requestEnum == RequestEnum.GET) {
                DoorStatusObserveApi.this.callBack.success(jsonResult);
            } else if (DoorStatusObserveApi.this.requestEnum == RequestEnum.OBSERVER) {
                PeanutSDK.getInstance().notifyApiSuccess(this, jsonResult);
            }
        }

        @Override // com.keenon.sdk.hedera.model.ApiCallback, com.keenon.sdk.hedera.model.ICallback
        public void onFail(ApiError error) {
            if (DoorStatusObserveApi.this.callBack == null) {
                return;
            }
            if (DoorStatusObserveApi.this.requestEnum == RequestEnum.GET) {
                DoorStatusObserveApi.this.callBack.error(error);
            } else if (DoorStatusObserveApi.this.requestEnum == RequestEnum.OBSERVER) {
                PeanutSDK.getInstance().notifyApiError(this, error);
            }
        }
    };

    @RequestType
    public RequestEnum requestType() {
        return this.requestEnum;
    }

    private void send(IDataCallback callBack) {
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

    /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/DoorStatusObserveApi$Bean.class */
    public class Bean implements Serializable {
        private int status;
        private int code;
        private String msg;
        private List<DataBean> data;

        public Bean() {
        }

        public int getStatus() {
            return this.status;
        }

        public void setStatus(int status) {
            this.status = status;
        }

        public int getCode() {
            return this.code;
        }

        public void setCode(int code) {
            this.code = code;
        }

        public String getMsg() {
            return this.msg;
        }

        public void setMsg(String msg) {
            this.msg = msg;
        }

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
            List<DataBean> dataList = new ArrayList<>();
            int size = bytes.length / 3;
            for (int i = 0; i < size; i++) {
                int start = i * 3;
                int end = (i + 1) * 3;
                Byte[] bytesUnit = (Byte[]) Arrays.copyOfRange(bytes, start, end);
                DataBean data = new DataBean();
                data.setId(bytesUnit[0].byteValue() & 255);
                data.setStatus(bytesUnit[1].byteValue() & 255);
                data.setProgress(bytesUnit[2].byteValue() & 255);
                dataList.add(data);
            }
            setData(dataList);
        }

        public String toString() {
            return "{status=" + this.status + ", code=" + this.code + ", msg=" + this.msg + ", data=" + this.data.toString() + '}';
        }

        /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/DoorStatusObserveApi$Bean$DataBean.class */
        public class DataBean {
            private int id;
            private int status;
            private int progress;

            public DataBean() {
            }

            public int getId() {
                return this.id;
            }

            public void setId(int id) {
                this.id = id;
            }

            public int getStatus() {
                return this.status;
            }

            public void setStatus(int status) {
                this.status = status;
            }

            public int getProgress() {
                return this.progress;
            }

            public void setProgress(int progress) {
                this.progress = progress;
            }

            public String toString() {
                return "{id=" + this.id + ", status=" + this.status + ", progress=" + this.progress + '}';
            }
        }
    }
}
