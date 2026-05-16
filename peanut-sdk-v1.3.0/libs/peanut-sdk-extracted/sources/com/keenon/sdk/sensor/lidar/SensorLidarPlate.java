package com.keenon.sdk.sensor.lidar;

import com.keenon.common.constant.PeanutConstants;
import com.keenon.common.utils.GsonUtil;
import com.keenon.sdk.external.IDataCallback;
import com.keenon.sdk.hedera.model.ApiData;
import com.keenon.sdk.hedera.model.ApiError;
import com.keenon.sdk.scmIot.SCMIoTSender;
import com.keenon.sdk.scmIot.bean.SCMRequest;
import com.keenon.sdk.scmIot.bean.response.PlateSensorLidarValue;
import com.keenon.sdk.sensor.common.Sensor;
import com.keenon.sdk.sensor.common.SensorEvent;
import java.util.ArrayList;
import java.util.List;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/sensor/lidar/SensorLidarPlate.class */
public class SensorLidarPlate extends Sensor {
    private static volatile SensorLidarPlate sInstance;
    private IDataCallback detectCallback = new IDataCallback() { // from class: com.keenon.sdk.sensor.lidar.SensorLidarPlate.1
        @Override // com.keenon.sdk.external.IDataCallback
        public void success(String result) {
            SensorLidarPlate.this.notifyEvent(SensorEvent.LIDAR_PLATE_DETECT, GsonUtil.bean2String(SensorLidarPlate.this.formatLidarData(result)));
        }

        @Override // com.keenon.sdk.external.IDataCallback
        public void error(ApiError error) {
            SensorLidarPlate.this.notifyEvent(SensorEvent.REQUEST_FAILED, error);
        }
    };

    public static SensorLidarPlate getInstance() {
        if (sInstance == null) {
            synchronized (SensorLidarPlate.class) {
                if (sInstance == null) {
                    sInstance = new SensorLidarPlate();
                }
            }
        }
        return sInstance;
    }

    @Override // com.keenon.sdk.sensor.common.Sensor
    public String name() {
        return SensorLidarPlate.class.getSimpleName();
    }

    @Override // com.keenon.sdk.sensor.common.Sensor
    public void mount() {
    }

    @Override // com.keenon.sdk.sensor.common.Sensor
    public void unMount() {
    }

    @Override // com.keenon.sdk.sensor.common.Sensor
    public void release() {
    }

    public void detect() {
        log("detect");
        SCMRequest request = new SCMRequest();
        request.setDev(15);
        request.setTopic(13);
        request.setType(0);
        request.setCmd(0);
        request.setUSBDirect(true);
        SCMIoTSender.sendRequest(request, this.detectCallback);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public Info formatLidarData(String result) {
        PlateSensorLidarValue rawBean = (PlateSensorLidarValue) GsonUtil.gson2Bean(result, PlateSensorLidarValue.class);
        Info lidarInfo = new Info();
        List<Info.DataBean> lidarData = new ArrayList<>();
        lidarInfo.setCode(0);
        lidarInfo.setStatus(0);
        lidarInfo.setMsg(PeanutConstants.API_SUCCESS_MESSAGE);
        lidarData.add(new Info.DataBean(1, rawBean.getPlate_1()));
        lidarData.add(new Info.DataBean(2, rawBean.getPlate_2()));
        lidarData.add(new Info.DataBean(3, rawBean.getPlate_3()));
        lidarData.add(new Info.DataBean(4, rawBean.getPlate_4()));
        lidarInfo.setData(lidarData);
        return lidarInfo;
    }

    /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/sensor/lidar/SensorLidarPlate$Info.class */
    public static class Info extends ApiData {
        private List<DataBean> data;

        public List<DataBean> getData() {
            return this.data;
        }

        public void setData(List<DataBean> data) {
            this.data = data;
        }

        /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/sensor/lidar/SensorLidarPlate$Info$DataBean.class */
        public static class DataBean {
            private int id;
            private int status;

            public DataBean(int id, int status) {
                this.id = id;
                this.status = status;
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
        }
    }
}
