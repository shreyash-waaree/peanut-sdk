package com.keenon.sdk.component;

import com.keenon.sdk.api.ElevatorStatusApi;
import com.keenon.sdk.api.ElevatorToRobotApi;
import com.keenon.sdk.api.ElevatorToServerApi;
import com.keenon.sdk.api.GateToRobotApi;
import com.keenon.sdk.component.Component;
import com.keenon.sdk.external.IDataCallback;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/component/ElevatorComponent.class */
public class ElevatorComponent extends BaseComponent implements Component {
    @Override // com.keenon.sdk.component.BaseComponent, com.keenon.sdk.component.Component
    public String getComponentName() {
        return Component.RobotComponentName.COMPONENT_ELEVATOR.getComponentName();
    }

    public void getStatus(IDataCallback callBack) {
        new ElevatorStatusApi().send(callBack);
    }

    public void receiveMessageFromServer(IDataCallback callBack) {
        new ElevatorToServerApi().send(callBack);
    }

    public void sendMessageToRobot(IDataCallback callBack, String payload) {
        new ElevatorToRobotApi().send(callBack, payload);
    }

    public void sendGateMessageToRobot(IDataCallback callBack, String payload) {
        new GateToRobotApi().send(callBack, payload);
    }
}
