package com.keenon.sdk.api;

import com.keenon.common.utils.ByteUtils;
import com.keenon.sdk.coap.adapter.CoapCommond;
import com.keenon.sdk.coap.adapter.CoapResponse;
import com.keenon.sdk.external.IDataCallback;
import com.keenon.sdk.hedera.model.ApiCallback;
import com.keenon.sdk.hedera.model.ApiError;
import com.keenon.sdk.hedera.model.RequestEnum;
import com.keenon.sdk.proxy.sender.SenderManager;
import com.keenon.sdk.proxy.sender.anno.LinkAdapter;
import com.keenon.sdk.proxy.sender.anno.RequestType;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.eclipse.californium.core.coap.CoAP;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/SensorLidarT2Api.class */
@LinkAdapter
@CoapCommond(path = "/sensor/lidarT2")
public class SensorLidarT2Api {
    private IDataCallback callBack;
    private RequestEnum requestEnum = RequestEnum.GET;

    @CoapResponse
    ApiCallback coapCallback = new ApiCallback<String>() { // from class: com.keenon.sdk.api.SensorLidarT2Api.1
        @Override // com.keenon.sdk.hedera.model.ApiCallback, com.keenon.sdk.hedera.model.ICallback
        public void onSuccess(String result) {
            Bean.LidarBeanT2 lidarBeanT2 = new Bean.LidarBeanT2();
            byte[] lidarData = result.getBytes(CoAP.UTF8_CHARSET);
            Byte[] lidarByteArray = ByteUtils.ByteToObject(lidarData);
            if (lidarByteArray != null && lidarByteArray.length > 2) {
                lidarBeanT2.setBytes((Byte[]) Arrays.copyOfRange(lidarByteArray, 2, lidarByteArray.length));
            }
            if (SensorLidarT2Api.this.callBack == null) {
            }
        }

        @Override // com.keenon.sdk.hedera.model.ApiCallback, com.keenon.sdk.hedera.model.ICallback
        public void onFail(ApiError error) {
            if (SensorLidarT2Api.this.callBack == null) {
                return;
            }
            SensorLidarT2Api.this.callBack.error(error);
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

    /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/SensorLidarT2Api$Bean.class */
    public static class Bean implements Serializable {
        private int status;
        private int code;
        private String msg;
        private DataBean data;

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
            return "Bean{status=" + this.status + ", code=" + this.code + ", msg=" + this.msg + ", data=" + this.data.toString() + '}';
        }

        /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/SensorLidarT2Api$Bean$DataBean.class */
        public static class DataBean {
            private int count;
            private String base64;

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

        /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/SensorLidarT2Api$Bean$LidarBeanT2.class */
        public static class LidarBeanT2 {
            private List<PointXY> points = new ArrayList();
            private Byte[] bytes;

            /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/SensorLidarT2Api$Bean$LidarBeanT2$PointXY.class */
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
