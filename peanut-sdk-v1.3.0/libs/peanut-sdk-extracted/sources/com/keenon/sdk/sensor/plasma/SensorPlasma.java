package com.keenon.sdk.sensor.plasma;

import com.keenon.common.utils.GsonUtil;
import com.keenon.sdk.constant.TopicName;
import com.keenon.sdk.external.IDataCallback;
import com.keenon.sdk.hedera.model.ApiError;
import com.keenon.sdk.scmIot.SCMIoTSender;
import com.keenon.sdk.scmIot.bean.SCMRequest;
import com.keenon.sdk.scmIot.bean.response.ReportInfo;
import com.keenon.sdk.sensor.common.Sensor;
import com.keenon.sdk.sensor.common.SensorEvent;
import com.keenon.sdk.sensor.common.SensorFaultInfo;
import com.keenon.sdk.sensor.plasma.transfer.PlasmaFanStatusDataTransfer;
import com.keenon.sdk.sensor.plasma.transfer.PlasmaStatusDataTransfer;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/sensor/plasma/SensorPlasma.class */
public class SensorPlasma extends Sensor {
    private static volatile SensorPlasma sInstance;
    IDataCallback mReportCallback = new IDataCallback() { // from class: com.keenon.sdk.sensor.plasma.SensorPlasma.1
        @Override // com.keenon.sdk.external.IDataCallback
        public void success(String result) {
            ReportInfo info = (ReportInfo) GsonUtil.gson2Bean(result, ReportInfo.class);
            if (1 == info.getTopic()) {
                SensorPlasma.this.handlePlasmaFaultReport(info);
            }
        }

        @Override // com.keenon.sdk.external.IDataCallback
        public void error(ApiError error) {
        }
    };

    /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/sensor/plasma/SensorPlasma$FanLeve.class */
    public enum FanLeve {
        FAN_LEVEL_0((byte) 0),
        FAN_LEVEL_1((byte) 1),
        FAN_LEVEL_2((byte) 2),
        FAN_LEVEL_3((byte) 3);

        public byte leve;

        FanLeve(byte leve) {
            this.leve = leve;
        }
    }

    public static SensorPlasma getInstance() {
        if (sInstance == null) {
            synchronized (SensorPlasma.class) {
                if (sInstance == null) {
                    sInstance = new SensorPlasma();
                }
            }
        }
        return sInstance;
    }

    public SensorPlasma() {
        subscribePlasmaEnvironmentInfo();
        SCMIoTSender.addReportDataCallback(this.mReportCallback, 41, 42, 43);
        SCMIoTSender.addSCMThingDataTransfer(18, new PlasmaStatusDataTransfer());
        SCMIoTSender.addSCMThingDataTransfer(20, new PlasmaFanStatusDataTransfer());
    }

    @Override // com.keenon.sdk.sensor.common.Sensor
    public String name() {
        return SensorPlasma.class.getSimpleName();
    }

    @Override // com.keenon.sdk.sensor.common.Sensor
    public void mount() {
    }

    @Override // com.keenon.sdk.sensor.common.Sensor
    public void unMount() {
    }

    @Override // com.keenon.sdk.sensor.common.Sensor
    public void release() {
        SCMIoTSender.removeReportDataCallback(41, 42, 43);
        SCMIoTSender.removeSCMThingDataTransfer(18);
        SCMIoTSender.removeSCMThingDataTransfer(20);
        sInstance = null;
    }

    public void subscribePlasmaEnvironmentInfo() {
        SCMRequest request = new SCMRequest();
        request.setDev(41);
        request.setTopic(18);
        request.setType(1);
        request.setCmd(5);
        request.setUSBDirect(true);
        SCMIoTSender.sendRequest(request, new IDataCallback() { // from class: com.keenon.sdk.sensor.plasma.SensorPlasma.2
            @Override // com.keenon.sdk.external.IDataCallback
            public void success(String result) {
                PlasmaEnvironmentInfoResult info = (PlasmaEnvironmentInfoResult) GsonUtil.gson2Bean(result, PlasmaEnvironmentInfoResult.class);
                SensorPlasma.this.notifySubscribeSuccess(TopicName.PLASMA_ENVIRONMENT, info);
                SensorPlasma.this.notifyEvent(SensorEvent.REPORT_PLASMA_ENVIRONMENT_INFO_ACK, result);
            }

            @Override // com.keenon.sdk.external.IDataCallback
            public void error(ApiError error) {
                SensorPlasma.this.notifyEvent(SensorEvent.REQUEST_FAILED, error);
            }
        });
    }

    public void setPlasmaEnvironmentSwitch(boolean isOpen) {
        SCMRequest request = new SCMRequest();
        request.setDev(41);
        request.setTopic(18);
        request.setType(2);
        request.setCmd(6);
        PlasmaSwitchParam param = new PlasmaSwitchParam();
        param.setIsOpen(isOpen ? (byte) -1 : (byte) 0);
        request.setParams(param);
        request.setUSBDirect(true);
        SCMIoTSender.sendRequest(request, new IDataCallback() { // from class: com.keenon.sdk.sensor.plasma.SensorPlasma.3
            @Override // com.keenon.sdk.external.IDataCallback
            public void success(String result) {
                SensorPlasma.this.notifyEvent(SensorEvent.SET_PLASMA_ENVIRONMENT_SWITCH_ACK, result);
            }

            @Override // com.keenon.sdk.external.IDataCallback
            public void error(ApiError error) {
                SensorPlasma.this.notifyEvent(SensorEvent.REQUEST_FAILED, error);
            }
        });
    }

    public void setPlasmaSwitch(boolean isOpen) {
        SCMRequest request = new SCMRequest();
        request.setDev(42);
        request.setTopic(19);
        request.setType(2);
        request.setCmd(6);
        PlasmaSwitchParam param = new PlasmaSwitchParam();
        param.setIsOpen(isOpen ? (byte) -1 : (byte) 0);
        request.setParams(param);
        request.setUSBDirect(true);
        SCMIoTSender.sendRequest(request, new IDataCallback() { // from class: com.keenon.sdk.sensor.plasma.SensorPlasma.4
            @Override // com.keenon.sdk.external.IDataCallback
            public void success(String result) {
                SensorPlasma.this.notifyEvent(SensorEvent.SET_PLASMA_SWITCH_ACK, result);
            }

            @Override // com.keenon.sdk.external.IDataCallback
            public void error(ApiError error) {
                SensorPlasma.this.notifyEvent(SensorEvent.REQUEST_FAILED, error);
            }
        });
    }

    public void setFanSwitchAndLevel(FanLeve level) {
        SCMRequest request = new SCMRequest();
        request.setDev(43);
        request.setTopic(20);
        request.setType(2);
        request.setCmd(6);
        request.setUSBDirect(true);
        PlasmaFanLeveParam param = new PlasmaFanLeveParam();
        param.setLeve(level.leve);
        request.setParams(param);
        SCMIoTSender.sendRequest(request, new IDataCallback() { // from class: com.keenon.sdk.sensor.plasma.SensorPlasma.5
            @Override // com.keenon.sdk.external.IDataCallback
            public void success(String result) {
                SensorPlasma.this.notifyEvent(SensorEvent.SET_PLASMA_FAN_SWITCH_ACK, result);
            }

            @Override // com.keenon.sdk.external.IDataCallback
            public void error(ApiError error) {
                SensorPlasma.this.notifyEvent(SensorEvent.REQUEST_FAILED, error);
            }
        });
    }

    public void getFanSwitchState(final IDataCallback callback) {
        SCMRequest request = new SCMRequest();
        request.setDev(43);
        request.setTopic(20);
        request.setType(0);
        request.setCmd(0);
        request.setSerialDirect(true);
        SCMIoTSender.sendRequest(request, new IDataCallback() { // from class: com.keenon.sdk.sensor.plasma.SensorPlasma.6
            @Override // com.keenon.sdk.external.IDataCallback
            public void success(String result) {
                if (null != callback) {
                    callback.success(result);
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

    /* JADX INFO: Access modifiers changed from: private */
    public void handlePlasmaFaultReport(ReportInfo info) {
        SensorFaultInfo.FaultEntry entry = new SensorFaultInfo.FaultEntry();
        entry.setDeviceId(info.getDev());
        entry.setFaultCode(info.getInfo());
        notifySubscribeSuccess(TopicName.PLASMA_FAULT, entry);
        notifyEvent(SensorEvent.REPORT_PLASMA_FAULT, GsonUtil.bean2String(entry));
    }
}
