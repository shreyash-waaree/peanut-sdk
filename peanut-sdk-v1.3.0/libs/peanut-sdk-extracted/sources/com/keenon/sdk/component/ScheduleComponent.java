package com.keenon.sdk.component;

import com.keenon.sdk.api.ScheduleActionApi;
import com.keenon.sdk.api.ScheduleLiveApi;
import com.keenon.sdk.api.ScheduleModuleApi;
import com.keenon.sdk.api.ScheduleStatusActionApi;
import com.keenon.sdk.api.ScheduleStatusApi;
import com.keenon.sdk.component.Component;
import com.keenon.sdk.external.IDataCallback;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/component/ScheduleComponent.class */
public class ScheduleComponent extends BaseComponent implements Component {
    public static final int PAUSE = 80;
    public static final int RESUME = 67;
    public static final int FORBID = 70;
    public static final String DESC_PAUSE = "pause";
    public static final String DESC_RESUME = "continue";
    public static final String DESC_FORBID = "forbid";

    @Override // com.keenon.sdk.component.BaseComponent, com.keenon.sdk.component.Component
    public String getComponentName() {
        return Component.RobotComponentName.COMPONENT_SCHEDULE.getComponentName();
    }

    public void getStatus(IDataCallback callBack) {
        new ScheduleStatusApi().send(callBack);
    }

    public void getLiveStatus(IDataCallback callBack) {
        new ScheduleLiveApi().send(callBack);
    }

    public void pause(IDataCallback callBack) {
        new ScheduleActionApi().send(callBack, 80, DESC_PAUSE);
    }

    public void forbid(IDataCallback callBack) {
        new ScheduleActionApi().send(callBack, 70, DESC_FORBID);
    }

    public void action(int action, String desc, IDataCallback callBack) {
        new ScheduleActionApi().send(callBack, action, desc);
    }

    public void resume(IDataCallback callBack) {
        new ScheduleActionApi().send(callBack, 67, DESC_RESUME);
    }

    public void setMode(IDataCallback callBack, int channel, int number) {
        new ScheduleModuleApi().send(callBack, channel, number);
    }

    public void scheduleStatusAck(IDataCallback callBack) {
        new ScheduleStatusActionApi().send(callBack, 1);
    }
}
