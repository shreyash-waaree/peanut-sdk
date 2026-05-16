package com.keenon.sdk.api;

import com.keenon.common.constant.PeanutConstants;
import com.keenon.common.utils.LogUtils;
import com.keenon.sdk.coap.adapter.CoapCommond;
import com.keenon.sdk.coap.adapter.CoapResponse;
import com.keenon.sdk.external.IDataCallback;
import com.keenon.sdk.hedera.model.ApiCallback;
import com.keenon.sdk.hedera.model.ApiError;
import com.keenon.sdk.proxy.sender.SenderManager;
import com.keenon.sdk.proxy.sender.anno.LinkAdapter;
import java.io.Serializable;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/MotorEncoderApi.class */
@LinkAdapter
@CoapCommond(path = "/motor/encoder")
public class MotorEncoderApi {
    private IDataCallback callBack;

    @CoapResponse
    ApiCallback coapCallback = new ApiCallback<String>() { // from class: com.keenon.sdk.api.MotorEncoderApi.1
        @Override // com.keenon.sdk.hedera.model.ApiCallback, com.keenon.sdk.hedera.model.ICallback
        public void onSuccess(String result) {
            LogUtils.i(PeanutConstants.TAG_API, "[MotorEncoderApi][onSuccess: " + result + "]");
            if (MotorEncoderApi.this.callBack == null) {
                return;
            }
            MotorEncoderApi.this.callBack.success(result);
        }

        @Override // com.keenon.sdk.hedera.model.ApiCallback, com.keenon.sdk.hedera.model.ICallback
        public void onFail(ApiError error) {
            if (MotorEncoderApi.this.callBack == null) {
                return;
            }
            MotorEncoderApi.this.callBack.error(error);
        }
    };

    public void send(IDataCallback callBack) {
        this.callBack = callBack;
        SenderManager.getInstance().send(this);
    }

    /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/MotorEncoderApi$Bean.class */
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

        public String toString() {
            return "{status=" + this.status + ", code=" + this.code + ", msg=" + this.msg + ", data=" + this.data.toString() + '}';
        }

        /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/MotorEncoderApi$Bean$DataBean.class */
        public class DataBean {
            private int left;
            private int right;

            public DataBean() {
            }

            public int getLeft() {
                return this.left;
            }

            public void setLeft(int left) {
                this.left = left;
            }

            public int getRight() {
                return this.right;
            }

            public void setRight(int right) {
                this.right = right;
            }

            public String toString() {
                return "{left=" + this.left + ", right=" + this.right + '}';
            }
        }
    }
}
