package com.keenon.sdk.api;

import com.keenon.common.constant.PeanutConstants;
import com.keenon.common.error.ExceptionMessage;
import com.keenon.common.utils.ByteUtils;
import com.keenon.common.utils.GsonUtil;
import com.keenon.common.utils.LogUtils;
import com.keenon.sdk.coap.adapter.CoapCommond;
import com.keenon.sdk.coap.adapter.CoapResponse;
import com.keenon.sdk.external.IDataCallback;
import com.keenon.sdk.hedera.model.ApiCallback;
import com.keenon.sdk.hedera.model.ApiData;
import com.keenon.sdk.hedera.model.ApiError;
import com.keenon.sdk.hedera.model.RequestEnum;
import com.keenon.sdk.proxy.sender.SenderManager;
import com.keenon.sdk.proxy.sender.anno.LinkAdapter;
import com.keenon.sdk.proxy.sender.anno.RequestType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/MotorHealthApi.class */
@LinkAdapter
@CoapCommond(path = "/motor/health")
public class MotorHealthApi {
    private IDataCallback callBack;
    private RequestEnum requestEnum = RequestEnum.GET;

    @CoapResponse
    ApiCallback coapCallback = new ApiCallback<String>() { // from class: com.keenon.sdk.api.MotorHealthApi.1
        @Override // com.keenon.sdk.hedera.model.ApiCallback, com.keenon.sdk.hedera.model.ICallback
        public void onSuccess(String result) {
            LogUtils.i(PeanutConstants.TAG_API, "[MotorHealthApi][onSuccess: " + ByteUtils.bytesToHexStr(result.getBytes()) + "]");
            Bean bean = new Bean();
            bean.setBytes(result.getBytes());
            String jsonResult = GsonUtil.bean2String(bean);
            LogUtils.d(PeanutConstants.TAG_API, "[MotorHealthApi][onSuccess json: " + jsonResult + "]");
            if (MotorHealthApi.this.callBack == null) {
                return;
            }
            MotorHealthApi.this.callBack.success(jsonResult);
        }

        @Override // com.keenon.sdk.hedera.model.ApiCallback, com.keenon.sdk.hedera.model.ICallback
        public void onFail(ApiError error) {
            if (MotorHealthApi.this.callBack == null) {
                return;
            }
            MotorHealthApi.this.callBack.error(error);
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

    /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/MotorHealthApi$Bean.class */
    public static class Bean extends ApiData {
        private List<DataBean> data;

        public List<DataBean> getData() {
            return this.data;
        }

        public void setData(List<DataBean> data) {
            this.data = data;
        }

        protected void setBytes(byte[] bytes) {
            if (bytes == null || bytes.length != 8) {
                setCode(1);
                setStatus(1);
                setMsg(ExceptionMessage.InvalidDataLength);
                return;
            }
            setCode(0);
            setStatus(0);
            setMsg(PeanutConstants.API_SUCCESS_MESSAGE);
            int[] motorList = {ByteUtils.bytesToInt(Arrays.copyOfRange(bytes, 0, 4), 0), ByteUtils.bytesToInt(Arrays.copyOfRange(bytes, 4, 8), 0)};
            List<DataBean> dataList = new ArrayList<>();
            for (int i = 0; i < motorList.length; i++) {
                for (int j = 0; j < 32; j++) {
                    if (((motorList[i] >> j) & 1) == 1) {
                        DataBean bean = new DataBean();
                        bean.setCode(j);
                        bean.setDesc("");
                        bean.setPosition(i);
                        dataList.add(bean);
                    }
                }
            }
            setData(dataList);
        }

        /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/MotorHealthApi$Bean$DataBean.class */
        public class DataBean {
            private int code;
            private String desc;
            private int position;

            public DataBean() {
            }

            public int getCode() {
                return this.code;
            }

            public void setCode(int code) {
                this.code = code;
            }

            public String getDesc() {
                return this.desc;
            }

            public void setDesc(String desc) {
                this.desc = desc;
            }

            public int getPosition() {
                return this.position;
            }

            public void setPosition(int position) {
                this.position = position;
            }
        }
    }
}
