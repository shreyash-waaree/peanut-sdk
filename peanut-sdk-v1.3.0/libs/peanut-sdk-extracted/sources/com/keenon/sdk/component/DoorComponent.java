package com.keenon.sdk.component;

import com.keenon.common.external.PeanutConfig;
import com.keenon.sdk.api.DoorCloseApi;
import com.keenon.sdk.api.DoorFaultApi;
import com.keenon.sdk.api.DoorLimitFaultApi;
import com.keenon.sdk.api.DoorLockAllStatusApi;
import com.keenon.sdk.api.DoorLockSingleStatusApi;
import com.keenon.sdk.api.DoorOpenApi;
import com.keenon.sdk.api.DoorSingleStatusApi;
import com.keenon.sdk.api.DoorStatusApi;
import com.keenon.sdk.api.DoorTypeAutoSetApi;
import com.keenon.sdk.api.DoorTypeDoubleSetApi;
import com.keenon.sdk.api.DoorTypeFourSetApi;
import com.keenon.sdk.api.DoorTypeObserveApi;
import com.keenon.sdk.api.DoorTypeThreeReverseSetApi;
import com.keenon.sdk.api.DoorTypeThreeSetApi;
import com.keenon.sdk.api.door.DoorVersionCompatApi;
import com.keenon.sdk.component.Component;
import com.keenon.sdk.external.IDataCallback;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/component/DoorComponent.class */
public class DoorComponent extends BaseComponent implements Component {
    private static final String TAG = "DoorComponent";
    public int doorNum;

    @Override // com.keenon.sdk.component.BaseComponent, com.keenon.sdk.component.Component
    public String getComponentName() {
        return Component.RobotComponentName.COMPONENT_DOOR.getComponentName();
    }

    public int getDoorNum() {
        return this.doorNum == 0 ? PeanutConfig.getDoorNum() : this.doorNum;
    }

    public void open(IDataCallback callBack, int doorId) {
        new DoorOpenApi().send(callBack, doorId);
    }

    public void close(IDataCallback callBack, int doorId) {
        new DoorCloseApi().send(callBack, doorId);
    }

    public void getState(IDataCallback callBack, int doorId) {
        new DoorSingleStatusApi().send(callBack, doorId);
    }

    public void getLockStatus(IDataCallback callBack, int doorId) {
        new DoorLockSingleStatusApi().send(callBack, doorId);
    }

    public void getAllLockStatus(IDataCallback callBack) {
        new DoorLockAllStatusApi().send(callBack);
    }

    public void getALlDoorStatus(IDataCallback callBack) {
        new DoorStatusApi().send(callBack);
    }

    public void getDoorInfo(IDataCallback callBack, int doorId) {
        new DoorSingleStatusApi().send(callBack, doorId);
    }

    public void setDoorType(IDataCallback callBack, int doorType) {
        switch (doorType) {
            case 1:
                new DoorTypeFourSetApi().send(callBack);
                break;
            case 2:
                new DoorTypeDoubleSetApi().send(callBack);
                break;
            case 3:
                new DoorTypeThreeSetApi().send(callBack);
                break;
            case 4:
                new DoorTypeThreeReverseSetApi().send(callBack);
                break;
            case 5:
                new DoorTypeAutoSetApi().send(callBack);
                break;
        }
    }

    public void observeLimitFault(IDataCallback callBack) {
        new DoorLimitFaultApi().send(callBack);
    }

    public void observeFault(IDataCallback callBack) {
        new DoorFaultApi().send(callBack);
    }

    public void observeDoorType(IDataCallback callBack) {
        new DoorTypeObserveApi().observe(callBack);
    }

    public void getDoorVersionApi(IDataCallback callBack) {
        new DoorVersionCompatApi().send(callBack);
    }
}
