package com.keenon.sdk.component;

import com.keenon.sdk.api.GuideInfoApi;
import com.keenon.sdk.api.GuideSetTargetApi;
import com.keenon.sdk.api.NavigationActionApi;
import com.keenon.sdk.api.NavigationAllApi;
import com.keenon.sdk.api.NavigationCruiseApi;
import com.keenon.sdk.api.NavigationCustomTaskApi;
import com.keenon.sdk.api.NavigationDestInfoApi;
import com.keenon.sdk.api.NavigationDestPoseApi;
import com.keenon.sdk.api.NavigationDestPoseApiV2;
import com.keenon.sdk.api.NavigationPathApi;
import com.keenon.sdk.api.NavigationRemainingPathApi;
import com.keenon.sdk.api.NavigationSetBlockTimeoutApi;
import com.keenon.sdk.api.NavigationSetMultiTargetApi;
import com.keenon.sdk.api.NavigationSetTargetApi;
import com.keenon.sdk.api.NavigationShortPath;
import com.keenon.sdk.api.NavigationSpeedApi;
import com.keenon.sdk.api.NavigationSportModeApi;
import com.keenon.sdk.api.NavigationStableModeApi;
import com.keenon.sdk.api.NavigationStatusApi;
import com.keenon.sdk.api.NavigationTaskInfoApi;
import com.keenon.sdk.api.SetScheduleOriginApi;
import com.keenon.sdk.component.Component;
import com.keenon.sdk.external.IDataCallback;
import java.util.List;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/component/NavigationComponent.class */
public class NavigationComponent extends BaseComponent implements Component {
    public static final int PAUSE = 80;
    public static final int RESUME = 67;
    public static final int STOP = 83;
    public static final int RESET = 82;

    @Override // com.keenon.sdk.component.BaseComponent, com.keenon.sdk.component.Component
    public String getComponentName() {
        return Component.RobotComponentName.COMPONENT_NAVIGATION.getComponentName();
    }

    public void getStatus(IDataCallback callBack) {
        new NavigationStatusApi().send(callBack);
    }

    public void setTarget(IDataCallback callBack, int target) {
        new NavigationSetTargetApi().send(callBack, target);
    }

    public void setTarget(IDataCallback callBack, int target, int takeControl) {
        new NavigationSetTargetApi().send(callBack, target, -1, takeControl);
    }

    public void setTargetList(IDataCallback callBack, List<Integer> targetList) {
        new NavigationSetMultiTargetApi().send(callBack, targetList, false);
    }

    public void setTargetListV2(IDataCallback callBack, List<Integer> targetList) {
        new NavigationSetMultiTargetApi().send(callBack, targetList, true);
    }

    public void setSpeed(IDataCallback callBack, int speed) {
        new NavigationSpeedApi().send(callBack, speed);
    }

    public void setStableMode(IDataCallback callBack, int mode) {
        new NavigationStableModeApi().send(callBack, mode);
    }

    public void setSportMode(IDataCallback callBack, int mode) {
        new NavigationSportModeApi().send(callBack, mode);
    }

    public void pause(IDataCallback callBack) {
        new NavigationActionApi().send(callBack, 80);
    }

    public void resume(IDataCallback callBack) {
        new NavigationActionApi().send(callBack, 67);
    }

    public void stop(IDataCallback callBack) {
        new NavigationActionApi().send(callBack, 83);
    }

    public void reset(IDataCallback callBack) {
        new NavigationActionApi().send(callBack, 82);
    }

    public void setCruise(IDataCallback callBack, int switchState, int from, int to) {
        new NavigationCruiseApi().send(callBack, switchState, from, to);
    }

    public void getAllTargets(IDataCallback callBack) {
        new NavigationAllApi().send(callBack);
    }

    public void getPath(IDataCallback callBack) {
        new NavigationPathApi().send(callBack);
    }

    public void getGuideInfo(IDataCallback callBack) {
        new GuideInfoApi().send(callBack);
    }

    public void setGuideList(IDataCallback callBack, List<Integer> targetList) {
        new GuideSetTargetApi().send(callBack, targetList);
    }

    public void setBlockTimeout(IDataCallback callBack, int timeout) {
        new NavigationSetBlockTimeoutApi().send(callBack, timeout);
    }

    public void getAllDestPose(IDataCallback callback) {
        new NavigationDestPoseApi().send(callback);
    }

    public void getAllDestPoseV2(IDataCallback callback) {
        new NavigationDestPoseApiV2().send(callback);
    }

    public void setShortPath(IDataCallback callback, int state) {
        new NavigationShortPath().send(callback, state);
    }

    public void setCustomTask(IDataCallback callback, int dst, List<Integer> pathList) {
        new NavigationCustomTaskApi().send(callback, dst, pathList);
    }

    public void getRemainingPath(IDataCallback callback) {
        new NavigationRemainingPathApi().send(callback);
    }

    public void getDestInfo(IDataCallback callback, int id) {
        new NavigationDestInfoApi().send(callback, id);
    }

    public void onTaskToRos(IDataCallback callback, String action, String task_id) {
        new NavigationTaskInfoApi().send(callback, action, task_id);
    }

    public void setScheduleOrigin(IDataCallback callback, int dst) {
        new SetScheduleOriginApi().send(callback, dst);
    }
}
