package com.keenon.sdk.component;

import com.keenon.common.constant.PeanutConstants;
import com.keenon.common.external.PeanutConfig;
import com.keenon.sdk.component.Component;
import com.keenon.sdk.config.PropertiesValue;
import com.keenon.sdk.external.PeanutSDK;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/component/BaseComponent.class */
public class BaseComponent implements Component {
    public int robotLinkPort;
    private Component.IRobotComponentInitListener mRobotComponentInitListener;
    private int mComponentStatusCode = -1;
    public String robotLink = PropertiesValue.getRobotLinkType();
    public String robotLinkCOM = PropertiesValue.getRobotLinkCOM();
    public String robotLinkIP = PropertiesValue.getRobotLinkIP();
    public String emotionLinkCOM = PropertiesValue.getEmotionLinkCOM();
    public String doorLinkCOM = PropertiesValue.getDoorLinkCOM();

    public BaseComponent() {
        this.robotLinkPort = 0;
        this.robotLinkPort = PropertiesValue.getRobotLinkPort();
    }

    private void parseRobotLink() {
        if (this.robotLink == null || this.robotLink.isEmpty()) {
            return;
        }
        PeanutConfig.getConfig().setLinkType((PeanutConstants.LinkType) PeanutConstants.LinkType.valueOf(PeanutConstants.LinkType.class, this.robotLink));
    }

    private void parseRobotLinkCOM() {
        if (this.robotLinkCOM == null || this.robotLinkCOM.isEmpty()) {
            return;
        }
        PeanutConfig.getConfig().setLinkCOM(this.robotLinkCOM);
    }

    private void parseRobotLinkIP() {
        if (this.robotLinkIP == null || this.robotLinkIP.isEmpty()) {
            return;
        }
        PeanutConfig.getConfig().setLinkIP(this.robotLinkIP);
    }

    private void parseRobotLinkPort() {
        if (this.robotLinkPort == 0) {
            return;
        }
        PeanutConfig.getConfig().setLinkPort(this.robotLinkPort);
    }

    private void parseEmotionLinkCOM() {
        if (this.emotionLinkCOM == null || this.emotionLinkCOM.isEmpty()) {
            return;
        }
        PeanutConfig.getConfig().setEmotionLinkCOM(this.emotionLinkCOM);
    }

    private void parseDoorLinkCOM() {
        if (this.doorLinkCOM == null || this.doorLinkCOM.isEmpty()) {
            return;
        }
        PeanutConfig.getConfig().setDoorLinkCOM(this.doorLinkCOM);
    }

    @Override // com.keenon.sdk.component.Component
    public void init(Component.IRobotComponentInitListener listener) {
        this.mRobotComponentInitListener = listener;
        parseRobotLink();
        parseRobotLinkCOM();
        parseRobotLinkIP();
        parseRobotLinkPort();
        parseEmotionLinkCOM();
        parseDoorLinkCOM();
        this.mComponentStatusCode = PeanutSDK.SDK_INIT_SUCCESS;
        if (this.mRobotComponentInitListener != null) {
            this.mRobotComponentInitListener.onRobotComponentInitFinish(getComponentName(), this.mComponentStatusCode, "");
        }
    }

    @Override // com.keenon.sdk.component.Component
    public String getComponentName() {
        return Component.RobotComponentName.COMPONENT_BASE.getComponentName();
    }

    @Override // com.keenon.sdk.component.Component
    public int getStatusCode() {
        return this.mComponentStatusCode;
    }

    @Override // com.keenon.sdk.component.Component
    public void release() {
        this.mComponentStatusCode = -1;
        this.mRobotComponentInitListener = null;
    }
}
