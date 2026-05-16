package com.keenon.sdk.api;

import com.keenon.common.constant.PeanutConstants;
import com.keenon.common.utils.ByteUtils;
import com.keenon.common.utils.GsonUtil;
import com.keenon.common.utils.LogUtils;
import com.keenon.sdk.coap.adapter.CoapCommond;
import com.keenon.sdk.coap.adapter.CoapResponse;
import com.keenon.sdk.external.IDataCallback;
import com.keenon.sdk.external.PeanutSDK;
import com.keenon.sdk.hedera.model.ApiCallback;
import com.keenon.sdk.hedera.model.ApiData;
import com.keenon.sdk.hedera.model.ApiError;
import com.keenon.sdk.hedera.model.RequestEnum;
import com.keenon.sdk.proxy.sender.SenderManager;
import com.keenon.sdk.proxy.sender.anno.LinkAdapter;
import com.keenon.sdk.proxy.sender.anno.RequestType;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/SensorLidarApi.class */
@LinkAdapter
@CoapCommond(path = "/sensor/lidar")
public class SensorLidarApi {
    private IDataCallback callBack;
    private RequestEnum requestEnum = RequestEnum.GET_RAW;

    @CoapResponse
    ApiCallback coapCallback = new ApiCallback<String>() { // from class: com.keenon.sdk.api.SensorLidarApi.1
        @Override // com.keenon.sdk.hedera.model.ApiCallback, com.keenon.sdk.hedera.model.ICallback
        public void onSuccess(String result) {
            LogUtils.i(PeanutConstants.TAG_API, "[SensorLidarApi][onSuccess: " + result + "]");
            Bean bean = new Bean();
            Bean.DataBean data = new Bean.DataBean();
            data.setBytes(ByteUtils.hexStrToBytes(result));
            bean.setData(data);
            bean.setTopic(getClass());
            String jsonResult = GsonUtil.bean2String(bean);
            LogUtils.d(PeanutConstants.TAG_API, "[SensorLidarApi][onSuccess json: " + jsonResult + "]");
            if (SensorLidarApi.this.callBack == null) {
                return;
            }
            if (SensorLidarApi.this.requestEnum == RequestEnum.GET_RAW) {
                SensorLidarApi.this.callBack.success(jsonResult);
            } else if (SensorLidarApi.this.requestEnum == RequestEnum.OBSERVER_RAW) {
                PeanutSDK.getInstance().notifyApiSuccess(this, jsonResult);
            }
        }

        @Override // com.keenon.sdk.hedera.model.ApiCallback, com.keenon.sdk.hedera.model.ICallback
        public void onFail(ApiError error) {
            if (SensorLidarApi.this.callBack == null) {
                return;
            }
            SensorLidarApi.this.callBack.error(error);
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
        this.requestEnum = RequestEnum.OBSERVER_RAW;
        send(callBack);
    }

    public void cancel() {
        SenderManager.getInstance().cancel(this);
    }

    /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/SensorLidarApi$Bean.class */
    public static class Bean extends ApiData implements Serializable {
        private DataBean data;

        public DataBean getData() {
            return this.data;
        }

        public void setData(DataBean data) {
            this.data = data;
        }

        /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/SensorLidarApi$Bean$DataBean.class */
        public static class DataBean {
            private int count;
            private String base64;
            private byte[] bytes;

            public byte[] getBytes() {
                return this.bytes;
            }

            public void setBytes(byte[] bytes) {
                this.bytes = bytes;
            }

            public int getCount() {
                return this.count;
            }

            public void setCount(int count) {
                this.count = count;
            }

            public String getBase64() {
                return this.base64;
            }

            public void setBase64(String base64) {
                this.base64 = base64;
            }

            public String toString() {
                return "Point{count=" + this.count + ", base64=" + this.base64 + '}';
            }
        }

        /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/SensorLidarApi$Bean$LidarBean.class */
        public static class LidarBean {
            private List<PointXY> points = new ArrayList();
            private Byte[] bytes;

            /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/SensorLidarApi$Bean$LidarBean$PointXY.class */
            public static class PointXY {
                public float centimeterX;
                public float centimeterY;
            }

            public List<PointXY> getPointXYs() {
                this.points.clear();
                int len = getBytes().length / 7;
                for (int iPoint = 0; iPoint < len; iPoint++) {
                    int iByte = iPoint * 7;
                    PointXY point = new PointXY();
                    byte data1 = getBytes()[iByte].byteValue();
                    byte data2 = getBytes()[iByte + 1].byteValue();
                    byte data3 = getBytes()[iByte + 2].byteValue();
                    getBytes()[iByte + 3].byteValue();
                    byte data5 = getBytes()[iByte + 4].byteValue();
                    byte data6 = getBytes()[iByte + 5].byteValue();
                    byte data7 = getBytes()[iByte + 6].byteValue();
                    int signBitX = (data1 << 4) | ((data2 & 240) >>> 4);
                    int dataX = ((data2 & 15) << 8) | (data3 & 255);
                    int signBitY = (data5 << 4) | ((data6 & 240) >>> 4);
                    int dataY = ((data6 & 15) << 8) | (data7 & 255);
                    point.centimeterX = signBitX == 1 ? dataX : dataX - (2 * dataX);
                    point.centimeterY = signBitY == 1 ? dataY : dataY - (2 * dataY);
                    this.points.add(point);
                }
                return this.points;
            }

            public Byte[] getBytes() {
                return this.bytes;
            }

            public void setBytes(Byte[] bytes) {
                this.bytes = bytes;
            }
        }
    }
}
