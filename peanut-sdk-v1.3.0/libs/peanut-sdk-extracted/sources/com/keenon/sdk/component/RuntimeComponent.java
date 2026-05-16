package com.keenon.sdk.component;

import com.keenon.sdk.api.NavigationPathApi;
import com.keenon.sdk.api.PositionActionApi;
import com.keenon.sdk.api.PositionLocationStatusApi;
import com.keenon.sdk.api.PositionNotifyApi;
import com.keenon.sdk.api.PositionOriginJudgeApi;
import com.keenon.sdk.api.PositionPosInfoApi;
import com.keenon.sdk.api.PositionRequestApi;
import com.keenon.sdk.api.PositionStatusApi;
import com.keenon.sdk.api.RuntimeHeartbeatApi;
import com.keenon.sdk.api.RuntimeOdoApi;
import com.keenon.sdk.api.RuntimeResourceConfigApi;
import com.keenon.sdk.api.RuntimeRkIPApi;
import com.keenon.sdk.api.RuntimeTimeApi;
import com.keenon.sdk.api.RuntimeWifiApi;
import com.keenon.sdk.api.RuntimeWorkModeApi;
import com.keenon.sdk.api.RuntimetLocationApi;
import com.keenon.sdk.api.SystemStartUpApi;
import com.keenon.sdk.api.VslamPictureApi;
import com.keenon.sdk.api.WelcomeSwitchApi;
import com.keenon.sdk.component.Component;
import com.keenon.sdk.external.IDataCallback;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/component/RuntimeComponent.class */
public class RuntimeComponent extends BaseComponent implements Component {
    @Override // com.keenon.sdk.component.BaseComponent, com.keenon.sdk.component.Component
    public String getComponentName() {
        return Component.RobotComponentName.COMPONENT_RUNTIME.getComponentName();
    }

    public void setMode(IDataCallback callBack, int mode) {
        new RuntimeWorkModeApi().send(callBack, mode);
    }

    public void getIPAddress(IDataCallback callBack) {
        new RuntimeRkIPApi().send(callBack);
    }

    public void getOdo(IDataCallback callBack) {
        new RuntimeOdoApi().send(callBack);
    }

    public void location(IDataCallback callBack) {
        new PositionRequestApi().send(callBack);
    }

    public void getLocationStatus(IDataCallback callBack) {
        new PositionStatusApi().send(callBack);
    }

    public void getPositionLocationStatus(IDataCallback callBack) {
        new PositionLocationStatusApi().send(callBack);
    }

    public void getLocationId(IDataCallback callBack) {
        new RuntimetLocationApi().send(callBack);
    }

    public void getHeartBeat(IDataCallback callBack) {
        new RuntimeHeartbeatApi().send(callBack);
    }

    public void sync(IDataCallback callBack, long timeStamp) {
        new RuntimeHeartbeatApi().sync(callBack, timeStamp);
    }

    public void setTime(IDataCallback callBack, long timestamp) {
        new RuntimeTimeApi().send(callBack, timestamp);
    }

    public void getPath(IDataCallback callBack) {
        new NavigationPathApi().send(callBack);
    }

    public void getRobotPosition(IDataCallback callBack) {
        new PositionPosInfoApi().send(callBack);
    }

    public void setPositionAction(String action, IDataCallback callBack) {
        new PositionActionApi().send(action, callBack);
    }

    public void judgeOriginPosition(double distance, IDataCallback callBack) {
        if (distance < 0.0d) {
            distance = 0.5d;
        }
        new PositionOriginJudgeApi().send(distance, callBack);
    }

    public void getRobotWifi(IDataCallback callBack) {
        new RuntimeWifiApi().send(callBack);
    }

    public void getVslamPicture(IDataCallback callBack) {
        new VslamPictureApi().send(callBack);
    }

    public void getSystemStartUpState(IDataCallback callBack) {
        new SystemStartUpApi().send(callBack);
    }

    public void notifyVslamRelocation(IDataCallback callBack) {
        new PositionNotifyApi().send(callBack);
    }

    public void setWelcomeSwitch(boolean isOpen, IDataCallback callBack) {
        new WelcomeSwitchApi().send(isOpen, callBack);
    }

    public void setResourceConfig(String payloadJson, IDataCallback callBack) {
        new RuntimeResourceConfigApi().send(callBack, payloadJson);
    }
}
