package com.keenon.sdk.api;

import android.util.Base64;
import com.keenon.common.error.PeanutError;
import com.keenon.common.utils.ByteUtils;
import com.keenon.common.utils.GsonUtil;
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

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/SensorDepthT2Api.class */
@LinkAdapter
@CoapCommond(path = "/sensor/depthT2")
public class SensorDepthT2Api {
    private IDataCallback callBack;
    private RequestEnum requestEnum = RequestEnum.GET;

    @CoapResponse
    ApiCallback coapCallback = new ApiCallback<String>() { // from class: com.keenon.sdk.api.SensorDepthT2Api.1
        @Override // com.keenon.sdk.hedera.model.ApiCallback, com.keenon.sdk.hedera.model.ICallback
        public void onSuccess(String result) {
            Bean rawData = (Bean) GsonUtil.gson2Bean(result, Bean.class);
            Bean.DepthBeanT2 depthBeanT2 = new Bean.DepthBeanT2();
            if (rawData != null && rawData.getData().getBase64() != null) {
                byte[] depth = Base64.decode(rawData.getData().getBase64(), 0);
                Byte[] depthByteArray = ByteUtils.ByteToObject(depth);
                if (depthByteArray != null && depthByteArray.length > 2) {
                    depthBeanT2.setBytes((Byte[]) Arrays.copyOfRange(depthByteArray, 2, depthByteArray.length));
                }
            }
            if (SensorDepthT2Api.this.callBack == null) {
            }
        }

        @Override // com.keenon.sdk.hedera.model.ApiCallback, com.keenon.sdk.hedera.model.ICallback
        public void onFail(ApiError error) {
            if (SensorDepthT2Api.this.callBack == null) {
                return;
            }
            SensorDepthT2Api.this.callBack.error(error);
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

    /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/SensorDepthT2Api$Bean.class */
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
            return "{status=" + this.status + ", code=" + this.code + ", msg=" + this.msg + ", data=" + this.data.toString() + '}';
        }

        /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/SensorDepthT2Api$Bean$DataBean.class */
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
                return "{count=" + this.count + ", base64=" + this.base64 + '}';
            }
        }

        /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/SensorDepthT2Api$Bean$DepthBeanT2.class */
        public static class DepthBeanT2 {
            private final int TOTAL_POINT_SIZE = PeanutError.CharConversionException;
            private final int FRONT_MINUS_SIZE = 57;
            private List<Point> points = new ArrayList(PeanutError.CharConversionException);
            private Byte[] bytes;

            /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/SensorDepthT2Api$Bean$DepthBeanT2$Point.class */
            public static class Point {
                public int millimeterX;
                public int millimeterY;
            }

            public List<Point> getPoints() {
                bytesToPoints();
                return this.points;
            }

            public void setPoints(List<Point> points) {
                this.points = points;
            }

            public Byte[] getBytes() {
                return this.bytes;
            }

            public void setBytes(Byte[] bytes) {
                this.bytes = bytes;
            }

            private void bytesToPoints() {
                this.points.clear();
                for (int iPoint = 0; iPoint < 117; iPoint++) {
                    int iByte = iPoint * 3;
                    Point point = new Point();
                    byte data1 = getBytes()[iByte].byteValue();
                    byte data2 = getBytes()[iByte + 1].byteValue();
                    byte data3 = getBytes()[iByte + 2].byteValue();
                    point.millimeterX = (data1 << 4) | ((data2 & 240) >>> 4);
                    point.millimeterY = ((data2 & 15) << 8) | (data3 & 255);
                    if (point.millimeterX != 2001 && point.millimeterY != 2001) {
                        if (iPoint > 57) {
                            point.millimeterX = 0 - point.millimeterX;
                        }
                        this.points.add(point);
                    }
                }
            }
        }
    }
}
