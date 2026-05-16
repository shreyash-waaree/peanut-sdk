package com.keenon.sdk.component;

import com.keenon.sdk.api.NavigationJackingStateApi;
import com.keenon.sdk.api.NavigationSetTargetApi;
import com.keenon.sdk.api.NavigationVendorActionApi;
import com.keenon.sdk.api.NavigationVendorExitStatusApi;
import com.keenon.sdk.api.NavigationVendorPosition;
import com.keenon.sdk.api.VendorMoveInApi;
import com.keenon.sdk.api.VendorMoveOutApi;
import com.keenon.sdk.api.VendorSetTargetApi;
import com.keenon.sdk.component.Component;
import com.keenon.sdk.external.IDataCallback;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/component/VendorComponent.class */
public class VendorComponent extends BaseComponent implements Component {
    public static final int DELIVERY = 0;
    public static final int PICK_UP = 1;
    public static final String VENDOR_ACTION_EXIT = "exit";

    @Override // com.keenon.sdk.component.BaseComponent, com.keenon.sdk.component.Component
    public String getComponentName() {
        return Component.RobotComponentName.COMPONENT_VENDOR.getComponentName();
    }

    public void setTarget(IDataCallback callBack, int target) {
        new VendorSetTargetApi().send(callBack, target);
    }

    public void moveIn(IDataCallback callBack, int device) {
        new VendorMoveInApi().send(callBack, device);
    }

    public void moveOut(IDataCallback callBack, int device) {
        new VendorMoveOutApi().send(callBack, device);
    }

    public void setVendorAction(IDataCallback callback, String action) {
        new NavigationVendorActionApi().send(callback, action);
    }

    public void setTarget(IDataCallback callBack, int target, int taskType) {
        new NavigationSetTargetApi().send(callBack, target, taskType);
    }

    public void getVendorExitStatus(IDataCallback callBack) {
        new NavigationVendorExitStatusApi().send(callBack);
    }

    public void postJackingStatus(IDataCallback callBack, int status) {
        new NavigationJackingStateApi().send(callBack, status);
    }

    public void getVendorPosition(IDataCallback callBack, String type) {
        new NavigationVendorPosition().send(callBack, type);
    }
}
