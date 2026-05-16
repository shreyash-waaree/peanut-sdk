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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/DoorStatusApi.class */
@LinkAdapter(board = PeanutConstants.BoardType.DOOR, link = PeanutConstants.LinkType.COM)
@SerialCommand(action = "64", body = "20")
public class DoorStatusApi {
    private IDataCallback callBack;
    private RequestEnum requestEnum = RequestEnum.GET;

    @SerialResponse
    ApiCallback serialCallback = new ApiCallback<SerialData>() { // from class: com.keenon.sdk.api.DoorStatusApi.1
        @Override // com.keenon.sdk.hedera.model.ApiCallback, com.keenon.sdk.hedera.model.ICallback
        public void onSuccess(SerialData result) {
            LogUtils.i(PeanutConstants.TAG_API, "[DoorStatusApi][onSuccess raw: " + ByteUtils.toString(result.getData()) + "]");
            Bean bean = new Bean();
            bean.setTopic(getClass());
            bean.setBytes(result.getData());
            String jsonResult = GsonUtil.bean2String(bean);
            LogUtils.d(PeanutConstants.TAG_API, "[DoorStatusApi][onSuccess json: " + jsonResult + "]");
            if (DoorStatusApi.this.callBack == null) {
                return;
            }
            if (DoorStatusApi.this.requestEnum == RequestEnum.GET) {
                DoorStatusApi.this.callBack.success(jsonResult);
            } else if (DoorStatusApi.this.requestEnum == RequestEnum.OBSERVER) {
                PeanutSDK.getInstance().notifyApiSuccess(this, jsonResult);
            }
        }

        @Override // com.keenon.sdk.hedera.model.ApiCallback, com.keenon.sdk.hedera.model.ICallback
        public void onFail(ApiError error) {
            if (DoorStatusApi.this.callBack == null) {
                return;
            }
            if (DoorStatusApi.this.requestEnum == RequestEnum.GET) {
                DoorStatusApi.this.callBack.error(error);
            } else if (DoorStatusApi.this.requestEnum == RequestEnum.OBSERVER) {
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

    /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/DoorStatusApi$Bean.class */
    public static class Bean extends ApiData {
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
            List<DataBean> dataList = new ArrayList<>();
            if (bytes.length == 12) {
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
            } else if (bytes.length == 16) {
                int size2 = bytes.length / 4;
                for (int i2 = 0; i2 < size2; i2++) {
                    int start2 = i2 * 4;
                    int end2 = (i2 + 1) * 4;
                    Byte[] bytesUnit2 = (Byte[]) Arrays.copyOfRange(bytes, start2, end2);
                    DataBean data2 = new DataBean();
                    data2.setId(bytesUnit2[0].byteValue() & 255);
                    data2.setStatus(bytesUnit2[1].byteValue() & 255);
                    data2.setProgress(bytesUnit2[2].byteValue() & 255);
                    data2.setVoltage(ByteUtils.getShort(bytesUnit2[2].byteValue(), bytesUnit2[3].byteValue()));
                    dataList.add(data2);
                }
            } else {
                int size3 = bytes.length / 3;
                for (int i3 = 0; i3 < size3; i3++) {
                    int start3 = i3 * bytes.length;
                    int end3 = (i3 + 1) * bytes.length;
                    Byte[] bytesUnit3 = (Byte[]) Arrays.copyOfRange(bytes, start3, end3);
                    DataBean data3 = new DataBean();
                    data3.setId(bytesUnit3[0].byteValue() & 255);
                    data3.setStatus(bytesUnit3[1].byteValue() & 255);
                    data3.setProgress(bytesUnit3[2].byteValue() & 255);
                    if (bytesUnit3.length > 3) {
                        data3.setVoltage(ByteUtils.getShort(bytesUnit3[2].byteValue(), bytesUnit3[3].byteValue()));
                    }
                    dataList.add(data3);
                }
            }
            setData(dataList);
        }

        /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/DoorStatusApi$Bean$DataBean.class */
        public class DataBean {
            private int id;
            private int status;
            private int progress;
            private int voltage;

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

            public int getVoltage() {
                return this.voltage;
            }

            public void setVoltage(int voltage) {
                this.voltage = voltage;
            }
        }
    }
}
