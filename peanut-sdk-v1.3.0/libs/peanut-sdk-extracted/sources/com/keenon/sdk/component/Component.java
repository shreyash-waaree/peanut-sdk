package com.keenon.sdk.component;

import org.eclipse.californium.core.coap.LinkFormat;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/component/Component.class */
public interface Component {

    /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/component/Component$IRobotComponentInitListener.class */
    public interface IRobotComponentInitListener {
        void onRobotComponentInitFinish(String str, int i, String str2);
    }

    String getComponentName();

    void init(IRobotComponentInitListener iRobotComponentInitListener);

    int getStatusCode();

    void release();

    /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/component/Component$RobotComponentName.class */
    public enum RobotComponentName {
        COMPONENT_BASE(LinkFormat.BASE),
        COMPONENT_BATTERY("battery"),
        COMPONENT_DEVICE("device"),
        COMPONENT_DISINFECT("disinfect"),
        COMPONENT_DOOR("door"),
        COMPONENT_ELEVATOR("elevator"),
        COMPONENT_MAP("map"),
        COMPONENT_MOTOR("motor"),
        COMPONENT_NAVIGATION("navigation"),
        COMPONENT_RUNTIME("runtime"),
        COMPONENT_SCHEDULE("schedule"),
        COMPONENT_UPDATE("update"),
        COMPONENT_VENDOR("vendor"),
        COMPONENT_DEVICE_NODE("device_node");

        String componentName;

        RobotComponentName(String name) {
            this.componentName = name;
        }

        public String getComponentName() {
            return this.componentName;
        }
    }
}
