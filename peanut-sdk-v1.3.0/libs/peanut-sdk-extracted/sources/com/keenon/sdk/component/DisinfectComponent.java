package com.keenon.sdk.component;

import com.keenon.sdk.api.DisinfectAtomizerApi;
import com.keenon.sdk.api.DisinfectCabinApi;
import com.keenon.sdk.api.DisinfectDrainageApi;
import com.keenon.sdk.api.DisinfectFanApi;
import com.keenon.sdk.api.DisinfectUltravioletApi;
import com.keenon.sdk.api.NavigationDisinfectActionApi;
import com.keenon.sdk.api.NavigationDisinfectStatusApi;
import com.keenon.sdk.api.NavigationDisinfectionInfoApi;
import com.keenon.sdk.component.Component;
import com.keenon.sdk.external.IDataCallback;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/component/DisinfectComponent.class */
public class DisinfectComponent extends BaseComponent implements Component {
    private static final String TAG = "DisinfectComponent";

    @Override // com.keenon.sdk.component.BaseComponent, com.keenon.sdk.component.Component
    public String getComponentName() {
        return Component.RobotComponentName.COMPONENT_DISINFECT.getComponentName();
    }

    public void enableCabin(IDataCallback callBack, boolean enable) {
        new DisinfectCabinApi().send(callBack, enable);
    }

    public void enableDrainage(IDataCallback callBack, boolean enable) {
        new DisinfectDrainageApi().send(callBack, enable);
    }

    public void enableAtomizer(IDataCallback callBack, boolean enable) {
        new DisinfectAtomizerApi().send(callBack, enable);
    }

    public void enableUltraviolet(IDataCallback callBack, boolean enable) {
        new DisinfectUltravioletApi().send(callBack, enable);
    }

    public void setFanSpeedLevel(IDataCallback callBack, int level) {
        new DisinfectFanApi().send(callBack, level);
    }

    public void getDisinfectionTaskArea(IDataCallback callBack) {
        new NavigationDisinfectionInfoApi().send(callBack);
    }

    public void actionDisinfectionTask(IDataCallback callBack, String action, int id, float speed) {
        new NavigationDisinfectActionApi().send(callBack, action, id, speed);
    }

    public void getDisinfectionStatus(IDataCallback callBack) {
        new NavigationDisinfectStatusApi().send(callBack);
    }
}
