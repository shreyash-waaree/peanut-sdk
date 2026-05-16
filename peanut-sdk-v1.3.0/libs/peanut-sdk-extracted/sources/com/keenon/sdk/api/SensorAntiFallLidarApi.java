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

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/SensorAntiFallLidarApi.class */
@LinkAdapter
@CoapCommond(path = "/sensor/fdl")
public class SensorAntiFallLidarApi {
    private IDataCallback callBack;
    private RequestEnum requestEnum = RequestEnum.GET_RAW;

    @CoapResponse
    ApiCallback coapCallback = new ApiCallback<String>() { // from class: com.keenon.sdk.api.SensorAntiFallLidarApi.1
        @Override // com.keenon.sdk.hedera.model.ApiCallback, com.keenon.sdk.hedera.model.ICallback
        public void onSuccess(String result) {
            LogUtils.i(PeanutConstants.TAG_API, "[SensorAntiFallLidarApi][onSuccess length: " + result.length() + "]");
            Bean bean = new Bean();
            Bean.DataBean data = new Bean.DataBean();
            data.setBytes(ByteUtils.hexStrToBytes(result));
            bean.setData(data);
            bean.setTopic(getClass());
            String jsonResult = GsonUtil.bean2String(bean);
            LogUtils.d(PeanutConstants.TAG_API, "[SensorAntiFallLidarApi][onSuccess json length: " + jsonResult.length() + "]");
            if (SensorAntiFallLidarApi.this.callBack == null) {
                return;
            }
            if (SensorAntiFallLidarApi.this.requestEnum == RequestEnum.GET_RAW || SensorAntiFallLidarApi.this.requestEnum == RequestEnum.GET) {
                SensorAntiFallLidarApi.this.callBack.success(jsonResult);
            } else if (SensorAntiFallLidarApi.this.requestEnum == RequestEnum.OBSERVER_RAW || SensorAntiFallLidarApi.this.requestEnum == RequestEnum.OBSERVER) {
                PeanutSDK.getInstance().notifyApiSuccess(this, jsonResult);
            }
        }

        @Override // com.keenon.sdk.hedera.model.ApiCallback, com.keenon.sdk.hedera.model.ICallback
        public void onFail(ApiError error) {
            if (SensorAntiFallLidarApi.this.callBack == null) {
                return;
            }
            SensorAntiFallLidarApi.this.callBack.error(error);
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

    /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/SensorAntiFallLidarApi$Bean.class */
    public static class Bean extends ApiData implements Serializable {
        private DataBean data;

        public DataBean getData() {
            return this.data;
        }

        public void setData(DataBean data) {
            this.data = data;
        }

        /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/SensorAntiFallLidarApi$Bean$DataBean.class */
        public static class DataBean {
            private byte[] bytes;

            public byte[] getBytes() {
                return this.bytes;
            }

            public void setBytes(byte[] bytes) {
                this.bytes = bytes;
            }
        }
    }

    /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/SensorAntiFallLidarApi$PeanutSensorLidarInfo.class */
    public static class PeanutSensorLidarInfo {
        private Byte[] bytes;
        private List<PointXY> points = new ArrayList();

        /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/SensorAntiFallLidarApi$PeanutSensorLidarInfo$PointXY.class */
        public static class PointXY {
            public float centimeterX;
            public float centimeterY;
            public int type;
        }

        public Byte[] getBytes() {
            return this.bytes;
        }

        public void setBytes(Byte[] bytes) {
            this.bytes = bytes;
        }

        public List<PointXY> getPoints() {
            this.points.clear();
            int len = getBytes().length / 8;
            for (int iPoint = 0; iPoint < len; iPoint++) {
                int iByte = iPoint * 8;
                byte data1 = getBytes()[iByte].byteValue();
                byte data2 = getBytes()[iByte + 1].byteValue();
                byte data3 = getBytes()[iByte + 2].byteValue();
                getBytes()[iByte + 3].byteValue();
                byte data5 = getBytes()[iByte + 4].byteValue();
                byte data6 = getBytes()[iByte + 5].byteValue();
                byte data7 = getBytes()[iByte + 6].byteValue();
                byte data8 = getBytes()[iByte + 7].byteValue();
                int signBitX = (data1 << 4) | ((data2 & 240) >>> 4);
                int dataX = ((data2 & 15) << 8) | (data3 & 255);
                int signBitY = (data5 << 4) | ((data6 & 240) >>> 4);
                int dataY = ((data6 & 15) << 8) | (data7 & 255);
                int type = data8 & 255;
                int int_x = signBitX == 1 ? dataX : dataX - (2 * dataX);
                int int_y = signBitY == 1 ? dataY : dataY - (2 * dataY);
                PointXY point = new PointXY();
                point.centimeterX = int_x;
                point.centimeterY = int_y;
                point.type = type;
                this.points.add(point);
            }
            return this.points;
        }
    }
}
