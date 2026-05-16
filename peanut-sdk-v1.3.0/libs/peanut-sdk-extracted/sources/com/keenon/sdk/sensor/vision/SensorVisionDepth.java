package com.keenon.sdk.sensor.vision;

import android.util.Base64;
import com.keenon.common.utils.ByteUtils;
import com.keenon.common.utils.GsonUtil;
import com.keenon.sdk.api.SensorDepthApi;
import com.keenon.sdk.constant.TopicName;
import com.keenon.sdk.external.IDataCallback;
import com.keenon.sdk.external.PeanutSDK;
import com.keenon.sdk.hedera.model.ApiData;
import com.keenon.sdk.hedera.model.ApiError;
import com.keenon.sdk.sensor.common.Sensor;
import com.keenon.sdk.sensor.common.SensorEvent;
import java.util.ArrayList;
import java.util.List;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/sensor/vision/SensorVisionDepth.class */
public class SensorVisionDepth extends Sensor {
    private static final int TOTAL_POINT_SIZE = 117;
    private static final int FRONT_MINUS_SIZE = 57;
    private static volatile SensorVisionDepth sInstance;
    private IDataCallback sensorDepthCallback = new IDataCallback() { // from class: com.keenon.sdk.sensor.vision.SensorVisionDepth.1
        @Override // com.keenon.sdk.external.IDataCallback
        public void success(String result) {
            SensorDepthApi.Bean rawData = (SensorDepthApi.Bean) GsonUtil.gson2Bean(result, SensorDepthApi.Bean.class);
            if (rawData != null && rawData.getData().getBase64() != null) {
                byte[] depth = Base64.decode(rawData.getData().getBase64(), 0);
                Byte[] depthByteArray = ByteUtils.ByteToObject(depth);
                if (depthByteArray.length > 0) {
                    SensorVisionDepth.this.notifyEvent(SensorEvent.VISION_DEPTH_CAPTURE, GsonUtil.bean2String(SensorVisionDepth.formatVisionData(depthByteArray)));
                }
            }
        }

        @Override // com.keenon.sdk.external.IDataCallback
        public void error(ApiError error) {
            SensorVisionDepth.this.notifyEvent(SensorEvent.REQUEST_FAILED, error);
        }
    };

    public static SensorVisionDepth getInstance() {
        if (sInstance == null) {
            synchronized (SensorVisionDepth.class) {
                if (sInstance == null) {
                    sInstance = new SensorVisionDepth();
                }
            }
        }
        return sInstance;
    }

    @Override // com.keenon.sdk.sensor.common.Sensor
    public String name() {
        return SensorVisionDepth.class.getSimpleName();
    }

    @Override // com.keenon.sdk.sensor.common.Sensor
    public void mount() {
    }

    @Override // com.keenon.sdk.sensor.common.Sensor
    public void unMount() {
        unsubscribe();
    }

    @Override // com.keenon.sdk.sensor.common.Sensor
    public void release() {
        unMount();
    }

    public void capture() {
        log("capture");
        subscribe();
    }

    private void subscribe() {
        PeanutSDK.getInstance().subscribe(TopicName.SENSOR_DEPTH, this.sensorDepthCallback);
    }

    private void unsubscribe() {
        PeanutSDK.getInstance().unSubscribe(TopicName.SENSOR_DEPTH, this.sensorDepthCallback);
    }

    public static Info formatVisionData(Byte[] bytes) {
        Info visionInfo = new Info();
        List<Info.Point> points = new ArrayList<>(117);
        for (int iPoint = 0; iPoint < 117; iPoint++) {
            int iByte = iPoint * 3;
            Info.Point point = new Info.Point();
            byte data1 = bytes[iByte].byteValue();
            byte data2 = bytes[iByte + 1].byteValue();
            byte data3 = bytes[iByte + 2].byteValue();
            point.setX((data1 << 4) | ((data2 & 240) >>> 4));
            point.setY(((data2 & 15) << 8) | (data3 & 255));
            if (point.getX() != 2001 && point.getY() != 2001) {
                if (iPoint > 57) {
                    point.setX(0 - point.getX());
                }
                points.add(point);
            }
        }
        visionInfo.setData(points);
        return visionInfo;
    }

    /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/sensor/vision/SensorVisionDepth$Info.class */
    public static class Info extends ApiData {
        private List<Point> data;

        public List<Point> getData() {
            return this.data;
        }

        public void setData(List<Point> data) {
            this.data = data;
        }

        /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/sensor/vision/SensorVisionDepth$Info$Point.class */
        public static class Point {
            private int x;
            private int y;

            public int getX() {
                return this.x;
            }

            public void setX(int x) {
                this.x = x;
            }

            public int getY() {
                return this.y;
            }

            public void setY(int y) {
                this.y = y;
            }
        }
    }
}
