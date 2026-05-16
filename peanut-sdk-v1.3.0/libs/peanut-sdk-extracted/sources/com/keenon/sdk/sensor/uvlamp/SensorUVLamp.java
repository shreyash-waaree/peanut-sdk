package com.keenon.sdk.sensor.uvlamp;

import com.keenon.common.utils.GsonUtil;
import com.keenon.sdk.constant.TopicName;
import com.keenon.sdk.external.IDataCallback;
import com.keenon.sdk.external.IDataCallback2;
import com.keenon.sdk.external.PeanutSDK;
import com.keenon.sdk.hedera.model.ApiError;
import com.keenon.sdk.hedera.model.BaseResponse;
import com.keenon.sdk.scmIot.SCMIoTSender;
import com.keenon.sdk.scmIot.bean.SCMRequest;
import com.keenon.sdk.scmIot.bean.response.ReportInfo;
import com.keenon.sdk.sensor.common.Sensor;
import com.keenon.sdk.sensor.common.SensorEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/sensor/uvlamp/SensorUVLamp.class */
public class SensorUVLamp extends Sensor {
    private static volatile SensorUVLamp sInstance;
    private Map<Integer, Integer> mUvlampStatus = new HashMap();
    private int[] mUvlampDevs = {44, 45, 46, 47, 48};
    private IDataCallback mReportCallback = new IDataCallback() { // from class: com.keenon.sdk.sensor.uvlamp.SensorUVLamp.3
        @Override // com.keenon.sdk.external.IDataCallback
        public void success(String result) {
            ReportInfo status = (ReportInfo) GsonUtil.gson2Bean(result, ReportInfo.class);
            if (SensorUVLamp.this.mUvlampStatus.containsKey(Integer.valueOf(status.getDev()))) {
                SensorUVLamp.this.mUvlampStatus.put(Integer.valueOf(status.getDev()), Integer.valueOf(status.getInfo()));
            }
            UVLampStatusInfo statusInfo = new UVLampStatusInfo();
            List<Integer> devStatus = new ArrayList<>();
            for (int dev : SensorUVLamp.this.mUvlampDevs) {
                devStatus.add((Integer) SensorUVLamp.this.mUvlampStatus.get(Integer.valueOf(dev)));
            }
            statusInfo.setDevStatus(devStatus);
            BaseResponse<UVLampStatusInfo> response = new BaseResponse<>();
            response.setCode(0);
            response.setStatus(0);
            response.setTopic(TopicName.UV_LAMP_FAULT);
            response.setData(statusInfo);
            PeanutSDK.getInstance().notifySubscribeSuccess(TopicName.UV_LAMP_FAULT, GsonUtil.bean2String(response));
        }

        @Override // com.keenon.sdk.external.IDataCallback
        public void error(ApiError error) {
        }
    };

    /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/sensor/uvlamp/SensorUVLamp$UVLampDev.class */
    public @interface UVLampDev {
    }

    public static SensorUVLamp getInstance() {
        if (sInstance == null) {
            synchronized (SensorUVLamp.class) {
                if (sInstance == null) {
                    sInstance = new SensorUVLamp();
                }
            }
        }
        return sInstance;
    }

    private SensorUVLamp() {
        SCMIoTSender.addSCMThingDataTransfer(24, new UVLampDataTransfer());
        initStatus();
    }

    public void setDevs(@UVLampDev int... devs) {
        SCMIoTSender.removeReportDataCallback(this.mUvlampDevs);
        if (null != devs) {
            this.mUvlampDevs = devs;
            initStatus();
        }
    }

    private void initStatus() {
        this.mUvlampStatus.clear();
        SCMIoTSender.addReportDataCallback(this.mReportCallback, this.mUvlampDevs);
        for (int dev : this.mUvlampDevs) {
            this.mUvlampStatus.put(Integer.valueOf(dev), Integer.valueOf(UVLampStatus.NO_FAULT.code()));
        }
    }

    @Override // com.keenon.sdk.sensor.common.Sensor
    public String name() {
        return getClass().getSimpleName();
    }

    @Override // com.keenon.sdk.sensor.common.Sensor
    public void mount() {
    }

    @Override // com.keenon.sdk.sensor.common.Sensor
    public void unMount() {
    }

    @Override // com.keenon.sdk.sensor.common.Sensor
    public void release() {
        SCMIoTSender.removeSCMThingDataTransfer(24);
        SCMIoTSender.removeReportDataCallback(this.mUvlampDevs);
        sInstance = null;
    }

    public void getUVLampSwitch(@UVLampDev int dev, final IDataCallback2<Boolean> callback) {
        SCMRequest request = new SCMRequest();
        request.setDev(dev);
        request.setTopic(24);
        request.setType(0);
        request.setCmd(0);
        request.setSerialDirect(!isUsbDirect());
        request.setUSBDirect(isUsbDirect());
        SCMIoTSender.sendRequest(request, new IDataCallback() { // from class: com.keenon.sdk.sensor.uvlamp.SensorUVLamp.1
            @Override // com.keenon.sdk.external.IDataCallback
            public void success(String result) {
                UVLampResult lampResult = (UVLampResult) GsonUtil.gson2Bean(result, UVLampResult.class);
                if (null != callback) {
                    callback.success(Boolean.valueOf((lampResult.getResult() & 255) == 255));
                }
            }

            @Override // com.keenon.sdk.external.IDataCallback
            public void error(ApiError error) {
                if (null != callback) {
                    callback.error(error);
                }
            }
        });
    }

    public void setUVLampSwitch(@UVLampDev int dev, boolean isOpen) {
        SCMRequest request = new SCMRequest();
        request.setDev(dev);
        request.setTopic(24);
        request.setType(2);
        request.setCmd(6);
        UVLampResult prams = new UVLampResult();
        prams.setResult((byte) (isOpen ? 255 : 0));
        request.setParams(prams);
        request.setSerialDirect(!isUsbDirect());
        request.setUSBDirect(isUsbDirect());
        SCMIoTSender.sendRequest(request, new IDataCallback() { // from class: com.keenon.sdk.sensor.uvlamp.SensorUVLamp.2
            @Override // com.keenon.sdk.external.IDataCallback
            public void success(String result) {
                SensorUVLamp.this.notifyEvent(SensorEvent.SET_UV_LAMP_SWITCH_ACK, result);
            }

            @Override // com.keenon.sdk.external.IDataCallback
            public void error(ApiError error) {
                SensorUVLamp.this.notifyEvent(SensorEvent.SET_UV_LAMP_SWITCH_ACK, error);
            }
        });
    }
}
