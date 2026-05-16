package com.keenon.sdk.sensor.lidar.calibration;

import com.keenon.common.utils.GsonUtil;
import com.keenon.common.utils.LogUtils;
import com.keenon.sdk.external.IDataCallback;
import com.keenon.sdk.hedera.model.ApiError;
import com.keenon.sdk.scmIot.SCMIoTSender;
import com.keenon.sdk.scmIot.bean.SCMRequest;
import com.keenon.sdk.sensor.common.Sensor;
import com.keenon.sdk.sensor.common.SensorEvent;
import com.keenon.sdk.sensor.lidar.calibration.PlateCalibrationInterface;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/sensor/lidar/calibration/SensorLidarPlateCalibration.class */
public final class SensorLidarPlateCalibration extends Sensor {
    private static final String TAG = "SensorLidarPlateCalibration";
    private static volatile SensorLidarPlateCalibration sInstance;
    private final int[] devs = {16, 17, 18};
    private final IDataCallback mEventCallback = new IDataCallback() { // from class: com.keenon.sdk.sensor.lidar.calibration.SensorLidarPlateCalibration.1
        @Override // com.keenon.sdk.external.IDataCallback
        public void success(String result) {
            LidarPointResultBean bean;
            LidarPointBean lidarPointBean;
            LogUtils.d(SensorLidarPlateCalibration.TAG, "Event :" + result);
            if (result != null && (bean = (LidarPointResultBean) GsonUtil.gson2Bean(result, LidarPointResultBean.class)) != null && (lidarPointBean = SensorLidarPlateCalibration.this.convert(bean)) != null) {
                SensorLidarPlateCalibration.this.notifyEvent(SensorEvent.GET_PLATE_LIDAR_CB_EVENT, lidarPointBean);
            }
        }

        @Override // com.keenon.sdk.external.IDataCallback
        public void error(ApiError error) {
            LogUtils.e(SensorLidarPlateCalibration.TAG, "Event error :" + error.toString());
            SensorLidarPlateCalibration.this.notifyEvent(SensorEvent.GET_PLATE_LIDAR_CB_FAIL, error);
        }
    };
    private final IDataCallback plateLidarCallBack = new IDataCallback() { // from class: com.keenon.sdk.sensor.lidar.calibration.SensorLidarPlateCalibration.2
        @Override // com.keenon.sdk.external.IDataCallback
        public void success(String result) {
            CalibrationResultBean resultBean;
            LogUtils.d(SensorLidarPlateCalibration.TAG, "Calibration :" + result);
            if (result != null && (resultBean = (CalibrationResultBean) GsonUtil.gson2Bean(result, CalibrationResultBean.class)) != null) {
                CalibrationBean bean = new CalibrationBean();
                bean.setLeftStatus(resultBean.getLeftStatus() & 255);
                bean.setRightStatus(resultBean.getRightStatus() & 255);
                bean.setLidarId(resultBean.getLidarId());
                SensorLidarPlateCalibration.this.notifyEvent(SensorEvent.GET_PLATE_LIDAR_CB_STATE, bean);
            }
        }

        @Override // com.keenon.sdk.external.IDataCallback
        public void error(ApiError error) {
            LogUtils.e(SensorLidarPlateCalibration.TAG, "Calibration error:" + error.toString());
            SensorLidarPlateCalibration.this.notifyEvent(SensorEvent.GET_PLATE_LIDAR_CB_FAIL, error);
        }
    };
    private final IDataCallback plateLidarPropCallBack = new IDataCallback() { // from class: com.keenon.sdk.sensor.lidar.calibration.SensorLidarPlateCalibration.3
        @Override // com.keenon.sdk.external.IDataCallback
        public void success(String result) {
            LidarParamResultBean resultBean;
            LogUtils.d(SensorLidarPlateCalibration.TAG, "Calibration :" + result);
            if (result != null && (resultBean = (LidarParamResultBean) GsonUtil.gson2Bean(result, LidarParamResultBean.class)) != null) {
                SensorLidarPlateCalibration.this.notifyEvent(SensorEvent.GET_PLATE_LIDAR_CB_PROP, resultBean);
            }
        }

        @Override // com.keenon.sdk.external.IDataCallback
        public void error(ApiError error) {
            LogUtils.e(SensorLidarPlateCalibration.TAG, "Calibration error:" + error.toString());
            SensorLidarPlateCalibration.this.notifyEvent(SensorEvent.GET_PLATE_LIDAR_CB_FAIL, error);
        }
    };
    LidarPointBean lidarPointBean;

    public static SensorLidarPlateCalibration getInstance() {
        if (sInstance == null) {
            synchronized (SensorLidarPlateCalibration.class) {
                if (sInstance == null) {
                    sInstance = new SensorLidarPlateCalibration();
                }
            }
        }
        return sInstance;
    }

    private SensorLidarPlateCalibration() {
        initData();
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
        for (int dev : this.devs) {
            SCMIoTSender.removeSCMThingDataTransferV2(dev, 35, 2);
            SCMIoTSender.removeSCMThingDataTransferV2(dev, 35, 1);
            SCMIoTSender.removeSCMThingDataTransferV2(dev, 35, 0);
            SCMIoTSender.removeSingeReportDataCallback(dev, 35, 1);
            SCMIoTSender.removeSingeReportDataCallback(dev, 35, 0);
            onClosePointReport(dev);
        }
        sInstance = null;
    }

    private void initData() {
        for (int dev : this.devs) {
            SCMIoTSender.addSCMThingDataTransferV2(dev, 35, 2, new PlateCalibrationStatusDataTransfer());
            SCMIoTSender.addSCMThingDataTransferV2(dev, 35, 1, new PlateLidarPointDataTransfer());
            SCMIoTSender.addSCMThingDataTransferV2(dev, 35, 0, new PlateLidarParamDataTransfer());
            SCMIoTSender.addSingeReportDataCallback(this.mEventCallback, dev, 35, 1);
            SCMIoTSender.addSingeReportDataCallback(this.plateLidarPropCallBack, dev, 35, 0);
        }
    }

    public void Calibration(PlateCalibrationInterface.PlateIds dev) {
        SCMRequest request = new SCMRequest();
        request.setDev(dev.getCode());
        request.setTopic(35);
        request.setType(2);
        request.setCmd(6);
        PlateLidarParam param = new PlateLidarParam();
        param.setCode((byte) PlateCalibrationInterface.Opcode.CALIBRATION.code);
        request.setParams(param);
        request.setSerialDirect(isSerialDirect());
        request.setUSBDirect(isUsbDirect());
        request.setTrans(isTrans());
        SCMIoTSender.sendRequest(request, this.plateLidarCallBack);
    }

    public void onOpenPointReport(PlateCalibrationInterface.PlateIds dev) {
        SCMRequest request = new SCMRequest();
        request.setDev(dev.getCode());
        request.setTopic(35);
        request.setType(2);
        request.setCmd(6);
        PlateLidarParam param = new PlateLidarParam();
        param.setCode((byte) PlateCalibrationInterface.Opcode.OPEN_POINT.code);
        request.setParams(param);
        request.setSerialDirect(isSerialDirect());
        request.setUSBDirect(isUsbDirect());
        request.setTrans(isTrans());
        SCMIoTSender.sendRequest(request, this.plateLidarCallBack);
    }

    public void onClosePointReport(PlateCalibrationInterface.PlateIds dev) {
        onClosePointReport(dev.getCode());
    }

    private void onClosePointReport(int dev) {
        SCMRequest request = new SCMRequest();
        request.setDev(dev);
        request.setTopic(35);
        request.setType(2);
        request.setCmd(6);
        PlateLidarParam param = new PlateLidarParam();
        param.setCode((byte) PlateCalibrationInterface.Opcode.CLOSE_POINT.code);
        request.setParams(param);
        request.setSerialDirect(isSerialDirect());
        request.setUSBDirect(isUsbDirect());
        request.setTrans(isTrans());
        SCMIoTSender.sendRequest(request, this.plateLidarCallBack);
    }

    public void onQueryProp(PlateCalibrationInterface.PlateIds dev) {
        SCMRequest request = new SCMRequest();
        request.setDev(dev.getCode());
        request.setTopic(35);
        request.setType(0);
        request.setCmd(0);
        request.setSerialDirect(isSerialDirect());
        request.setUSBDirect(isUsbDirect());
        request.setTrans(isTrans());
        SCMIoTSender.sendRequest(request, this.plateLidarPropCallBack);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public LidarPointBean convert(LidarPointResultBean bean) {
        this.lidarPointBean = new LidarPointBean();
        float[] pointX = new float[bean.getAngles().length];
        float[] pointY = new float[bean.getDistances().length];
        for (int i = 0; i < bean.getAngles().length; i++) {
            pointX[i] = (float) (0.0d + (((double) bean.getDistances()[i]) * Math.cos(Math.toRadians(bean.getAngles()[i]))));
            pointY[i] = (float) (0.0d + (((double) bean.getDistances()[i]) * Math.sin(Math.toRadians(bean.getAngles()[i]))));
        }
        this.lidarPointBean.setLidarId(bean.getLidarId());
        this.lidarPointBean.setType(bean.getType());
        this.lidarPointBean.setPointX(pointX);
        this.lidarPointBean.setPointY(pointY);
        LogUtils.d(TAG, "convert :" + this.lidarPointBean.toString());
        return this.lidarPointBean;
    }
}
