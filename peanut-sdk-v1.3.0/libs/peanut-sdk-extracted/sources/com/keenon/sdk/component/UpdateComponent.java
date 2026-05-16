package com.keenon.sdk.component;

import com.keenon.sdk.api.UpdateCancelApi;
import com.keenon.sdk.api.UpdateDownloadApi;
import com.keenon.sdk.api.UpdateHandleApi;
import com.keenon.sdk.api.UpdatePackageApi;
import com.keenon.sdk.api.UpdateRebootApi;
import com.keenon.sdk.api.UpdateStartApi;
import com.keenon.sdk.api.UpdateStateApi;
import com.keenon.sdk.component.Component;
import com.keenon.sdk.external.IDataCallback;
import com.keenon.sdk.external.IProgressCallback;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/component/UpdateComponent.class */
public class UpdateComponent extends BaseComponent implements Component {
    @Override // com.keenon.sdk.component.BaseComponent, com.keenon.sdk.component.Component
    public String getComponentName() {
        return Component.RobotComponentName.COMPONENT_UPDATE.getComponentName();
    }

    public void setPackageInfo(IDataCallback callBack, String info) {
        new UpdatePackageApi().send(callBack, info);
    }

    public void uploadPackage(IProgressCallback callBack, byte[] file) {
        new UpdateDownloadApi().send(callBack, file);
    }

    public void handle(IDataCallback callBack) {
        new UpdateHandleApi().send(callBack);
    }

    public void start(IDataCallback callBack, int force) {
        new UpdateStartApi().send(callBack, force);
    }

    public void reboot(IDataCallback callBack) {
        new UpdateRebootApi().send(callBack);
    }

    public void cancel(IDataCallback callBack) {
        new UpdateCancelApi().send(callBack);
    }

    public void getStatus(IDataCallback callBack) {
        new UpdateStateApi().send(callBack);
    }
}
