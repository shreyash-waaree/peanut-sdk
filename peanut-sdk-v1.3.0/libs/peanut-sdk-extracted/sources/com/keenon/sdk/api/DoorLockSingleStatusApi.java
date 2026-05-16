package com.keenon.sdk.api;

import com.keenon.common.constant.PeanutConstants;
import com.keenon.common.utils.ByteUtils;
import com.keenon.common.utils.GsonUtil;
import com.keenon.common.utils.LogUtils;
import com.keenon.sdk.external.IDataCallback;
import com.keenon.sdk.hedera.model.ApiCallback;
import com.keenon.sdk.hedera.model.ApiError;
import com.keenon.sdk.proxy.sender.SenderManager;
import com.keenon.sdk.proxy.sender.anno.LinkAdapter;
import com.keenon.sdk.serial.adapter.SerialCommand;
import com.keenon.sdk.serial.adapter.SerialParams;
import com.keenon.sdk.serial.adapter.SerialResponse;
import com.keenon.sdk.serial.base.SerialData;
import java.io.Serializable;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/DoorLockSingleStatusApi.class */
@LinkAdapter(board = PeanutConstants.BoardType.DOOR, link = PeanutConstants.LinkType.COM)
@SerialCommand(action = "64", body = "22")
public class DoorLockSingleStatusApi {
    private IDataCallback callBack;

    @SerialResponse
    ApiCallback serialCallback = new ApiCallback<SerialData>() { // from class: com.keenon.sdk.api.DoorLockSingleStatusApi.1
        @Override // com.keenon.sdk.hedera.model.ApiCallback, com.keenon.sdk.hedera.model.ICallback
        public void onSuccess(SerialData result) {
            LogUtils.i(PeanutConstants.TAG_API, "[DoorLockSingleStatusApi][onSuccess raw: " + ByteUtils.toString(result.getData()) + "]");
            Bean bean = DoorLockSingleStatusApi.this.new Bean();
            bean.setBytes(result.getData());
            String jsonResult = GsonUtil.bean2String(bean);
            LogUtils.d(PeanutConstants.TAG_API, "[DoorLockSingleStatusApi][onSuccess json: " + jsonResult + "]");
            if (DoorLockSingleStatusApi.this.callBack == null) {
                return;
            }
            DoorLockSingleStatusApi.this.callBack.success(jsonResult);
        }

        @Override // com.keenon.sdk.hedera.model.ApiCallback, com.keenon.sdk.hedera.model.ICallback
        public void onFail(ApiError error) {
            if (DoorLockSingleStatusApi.this.callBack == null) {
                return;
            }
            DoorLockSingleStatusApi.this.callBack.error(error);
        }
    };
    private int doorId;

    @SerialParams
    public Byte[] SerialParams() {
        return new Byte[]{Byte.valueOf((byte) (this.doorId & 255))};
    }

    public void send(IDataCallback callBack, int doorId) {
        this.callBack = callBack;
        this.doorId = doorId;
        SenderManager.getInstance().send(this);
    }

    /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/DoorLockSingleStatusApi$Bean.class */
    public class Bean implements Serializable {
        private int status;
        private int code;
        private String msg;
        private DataBean data;

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
            dataBean.setId(bytes[0].byteValue() & 255);
            dataBean.setStatus(bytes[1].byteValue() & 255);
            dataBean.setProgress(bytes[2].byteValue() & 255);
            setData(dataBean);
        }

        public String toString() {
            return "{status=" + this.status + ", code=" + this.code + ", msg=" + this.msg + ", data=" + this.data.toString() + '}';
        }

        /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/DoorLockSingleStatusApi$Bean$DataBean.class */
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
