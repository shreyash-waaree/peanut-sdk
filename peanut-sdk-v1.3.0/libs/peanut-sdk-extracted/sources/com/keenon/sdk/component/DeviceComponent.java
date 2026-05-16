package com.keenon.sdk.component;

import com.keenon.common.constant.PeanutConstants;
import com.keenon.common.utils.LogUtils;
import com.keenon.sdk.api.ButtonEnableApi;
import com.keenon.sdk.api.ButtonStatusApi;
import com.keenon.sdk.api.DeviceInfoApi;
import com.keenon.sdk.api.DeviceListApi;
import com.keenon.sdk.api.DeviceRebootApi;
import com.keenon.sdk.api.DeviceVersionApi;
import com.keenon.sdk.api.ParamIntegrateApi;
import com.keenon.sdk.api.ParamIntegrateV2Api;
import com.keenon.sdk.api.ParamUploadApi;
import com.keenon.sdk.api.RealTimeStatusApi;
import com.keenon.sdk.api.RobotDeviceCheckApi;
import com.keenon.sdk.api.RobotDeviceCheckStatusApi;
import com.keenon.sdk.api.RobotDeviceTreeStatusApi;
import com.keenon.sdk.api.SelfCheckApi;
import com.keenon.sdk.component.Component;
import com.keenon.sdk.component.param.ParamSettingHelp;
import com.keenon.sdk.constant.TopicName;
import com.keenon.sdk.external.IDataCallback;
import com.keenon.sdk.external.IDataCallback2;
import com.keenon.sdk.external.PeanutSDK;
import com.keenon.sdk.hedera.model.ApiError;
import com.keenon.sdk.scmIot.SCMIoTSender;
import com.keenon.sdk.scmIot.bean.SCMRequest;
import com.keenon.sdk.scmIot.bean.request.BellConfig;
import com.keenon.sdk.scmIot.bean.request.BellNotifyMsg;
import com.keenon.sdk.scmIot.bean.request.BellNotifyTsk;
import java.util.ArrayList;
import java.util.List;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/component/DeviceComponent.class */
public class DeviceComponent extends BaseComponent implements Component {
    private IDataCallback devicesCallBack;
    private IDataCallback bottomRawCallback = new IDataCallback() { // from class: com.keenon.sdk.component.DeviceComponent.4
        @Override // com.keenon.sdk.external.IDataCallback
        public void success(String result) {
            if (DeviceComponent.this.devicesCallBack != null) {
                DeviceComponent.this.devicesCallBack.success(result);
            }
        }

        @Override // com.keenon.sdk.external.IDataCallback
        public void error(ApiError error) {
            if (DeviceComponent.this.devicesCallBack != null) {
                DeviceComponent.this.devicesCallBack.error(error);
            }
        }
    };
    private IDataCallback deviceCheckCallback = new IDataCallback() { // from class: com.keenon.sdk.component.DeviceComponent.5
        @Override // com.keenon.sdk.external.IDataCallback
        public void success(String result) {
            if (DeviceComponent.this.devicesCallBack != null) {
                DeviceComponent.this.devicesCallBack.success(result);
            }
        }

        @Override // com.keenon.sdk.external.IDataCallback
        public void error(ApiError error) {
            if (DeviceComponent.this.devicesCallBack != null) {
                DeviceComponent.this.devicesCallBack.error(error);
            }
        }
    };

    @Override // com.keenon.sdk.component.BaseComponent, com.keenon.sdk.component.Component
    public String getComponentName() {
        return Component.RobotComponentName.COMPONENT_DEVICE.getComponentName();
    }

    public void getList(IDataCallback callBack) {
        new DeviceListApi().send(callBack);
    }

    public void getBoardInfo(IDataCallback callBack, String board) {
        new DeviceInfoApi().send(callBack, board);
    }

    public void getVersion(IDataCallback callBack) {
        new DeviceVersionApi().send(callBack);
    }

    public void reboot(IDataCallback callBack) {
        reboot(callBack, false);
    }

    public void reboot(IDataCallback callBack, boolean isReposition) {
        DeviceRebootApi.DeviceRebootParam param = new DeviceRebootApi.DeviceRebootParam();
        param.id = 1;
        param.name = "robot-arm";
        param.isReposition = isReposition;
        new DeviceRebootApi().send(callBack, param);
    }

    public void setButtonEnable(IDataCallback callBack, boolean enable) {
        new ButtonEnableApi().send(callBack, enable);
    }

    public void getScramButtonStatus(IDataCallback callBack) {
        new ButtonStatusApi().send(callBack);
    }

    public void getConfig(IDataCallback callBack) {
        new ParamUploadApi().send(callBack);
    }

    public void updateConfig(IDataCallback callBack, String params) {
        new ParamIntegrateApi().send(callBack, params);
    }

    public void updateConfigV2(IDataCallback callBack, String params) {
        new ParamIntegrateV2Api().send(callBack, params);
    }

    public void updateConfigCompat(final IDataCallback callBack, final String params) {
        updateConfigV2(new IDataCallback() { // from class: com.keenon.sdk.component.DeviceComponent.1
            @Override // com.keenon.sdk.external.IDataCallback
            public void success(String result) {
                if (null != callBack) {
                    callBack.success(result);
                }
            }

            @Override // com.keenon.sdk.external.IDataCallback
            public void error(ApiError error) {
                LogUtils.d(PeanutConstants.TAG_API, "[updateConfigCompat][onFail :" + error + "]");
                DeviceComponent.this.updateConfig(callBack, params);
            }
        }, params);
    }

    public void checkParamKey(IDataCallback2<Boolean> callback, String paramKey) {
        new ParamSettingHelp().checkParam(paramKey, callback);
    }

    private void checkRobotArm() {
        new SelfCheckApi().send(new IDataCallback() { // from class: com.keenon.sdk.component.DeviceComponent.2
            @Override // com.keenon.sdk.external.IDataCallback
            public void success(String result) {
                LogUtils.d(PeanutConstants.TAG_API, "[checkRobotArm][result :" + result + "]");
            }

            @Override // com.keenon.sdk.external.IDataCallback
            public void error(ApiError error) {
                if (DeviceComponent.this.devicesCallBack != null) {
                    DeviceComponent.this.devicesCallBack.error(error);
                }
            }
        });
    }

    private void checkSt32() {
        SCMRequest request = new SCMRequest();
        request.setDev(0);
        request.setTopic(8);
        request.setType(2);
        request.setCmd(6);
        SCMIoTSender.sendRequest(request, new IDataCallback() { // from class: com.keenon.sdk.component.DeviceComponent.3
            @Override // com.keenon.sdk.external.IDataCallback
            public void success(String result) {
                LogUtils.d(PeanutConstants.TAG_API, "[checkSt32][result :" + result + "]");
            }

            @Override // com.keenon.sdk.external.IDataCallback
            public void error(ApiError error) {
                if (DeviceComponent.this.devicesCallBack != null) {
                    DeviceComponent.this.devicesCallBack.error(error);
                }
            }
        });
    }

    public void checkRobotReport() {
        checkRobotArm();
        checkSt32();
    }

    public void observeCheckRobotReport(IDataCallback callBack) {
        this.devicesCallBack = callBack;
        PeanutSDK.getInstance().subscribe(TopicName.BOTTOM_RAW, this.bottomRawCallback);
        PeanutSDK.getInstance().subscribe(TopicName.DEVICES_CHECK, this.deviceCheckCallback);
    }

    public void unObserveCheckRobotReport() {
        PeanutSDK.getInstance().unSubscribe(TopicName.BOTTOM_RAW, this.bottomRawCallback);
        PeanutSDK.getInstance().unSubscribe(TopicName.DEVICES_CHECK, this.deviceCheckCallback);
        this.devicesCallBack = null;
    }

    public void syncBellConfigurations(byte[] macAddress, byte audioEnable, byte audioVolume, byte audioSongId, byte ledEnable, byte ledBrightness, byte ledColor, byte timeout, IDataCallback callBack) {
        BellConfig bean = new BellConfig();
        bean.setMacAddress(macAddress);
        bean.setAudioEnable(audioEnable);
        bean.setAudioVolume(audioVolume);
        bean.setAudioSongid(audioSongId);
        bean.setLedEnable(ledEnable);
        bean.setLedBrightness(ledBrightness);
        bean.setLedColor(ledColor);
        bean.setLedTime(timeout);
        SCMRequest request = new SCMRequest();
        request.setDev(5);
        request.setTopic(9);
        request.setType(0);
        request.setCmd(1);
        request.setParams(bean);
        request.setChannel(1);
        SCMIoTSender.sendRequest(request, callBack);
    }

    public void sendBellInformation(byte[] macAddress, byte robotStatus, IDataCallback callBack) {
        BellNotifyMsg bellNotifyMsg = new BellNotifyMsg(macAddress, robotStatus);
        SCMRequest request = new SCMRequest();
        request.setDev(5);
        request.setTopic(10);
        request.setType(0);
        request.setCmd(1);
        request.setParams(bellNotifyMsg);
        request.setChannel(1);
        SCMIoTSender.sendRequest(request, callBack);
    }

    public void sendBellInformationForID(byte[] macAddress, short taskId, IDataCallback callBack) {
        BellNotifyTsk bellNotifyTsk = new BellNotifyTsk(macAddress, taskId);
        SCMRequest request = new SCMRequest();
        request.setDev(5);
        request.setTopic(10);
        request.setType(0);
        request.setCmd(1);
        request.setParams(bellNotifyTsk);
        request.setChannel(1);
        SCMIoTSender.sendRequest(request, callBack);
    }

    public void checkRobot(IDataCallback callBack, int type) {
        new RobotDeviceCheckApi().send(callBack, type);
    }

    public void getCheckRobotStatus(IDataCallback callBack) {
        new RobotDeviceCheckStatusApi().send(callBack);
    }

    public void getRobotDevicesTreeStatus(IDataCallback callBack) {
        new RobotDeviceTreeStatusApi().send(callBack);
    }

    public void getAutoCheckStatus(IDataCallback callBack, String jsonString) {
        new RealTimeStatusApi().send(callBack, jsonString);
    }

    public void getAutoCheckStatus(IDataCallback callBack, int id, List<Integer> sceneIdList) {
        new RealTimeStatusApi().send(callBack, id, sceneIdList);
    }

    public void getAutoCheckStatus(IDataCallback callBack, int id, int sceneId) {
        List<Integer> sceneIdList = new ArrayList<>();
        sceneIdList.add(Integer.valueOf(sceneId));
        new RealTimeStatusApi().send(callBack, id, sceneIdList);
    }
}
