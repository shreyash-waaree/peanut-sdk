package com.keenon.sdk.component;

import com.keenon.sdk.api.BatteryStatusApi;
import com.keenon.sdk.api.ChargeAutoApi;
import com.keenon.sdk.api.ChargeDefaultApi;
import com.keenon.sdk.api.ChargeManualApi;
import com.keenon.sdk.api.ChargeMatchTimesApi;
import com.keenon.sdk.api.ChargeNumbersApi;
import com.keenon.sdk.api.ChargeStopApi;
import com.keenon.sdk.component.Component;
import com.keenon.sdk.external.IDataCallback;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/component/BatteryComponent.class */
public class BatteryComponent extends BaseComponent implements Component {
    private static final String TAG = "BatteryComponent";

    @Override // com.keenon.sdk.component.BaseComponent, com.keenon.sdk.component.Component
    public String getComponentName() {
        return Component.RobotComponentName.COMPONENT_BATTERY.getComponentName();
    }

    public void getStatus(IDataCallback callBack) {
        new BatteryStatusApi().send(callBack);
    }

    public void autoCharge(IDataCallback callBack, int pile) {
        new ChargeAutoApi().send(callBack, pile);
    }

    public void manualCharge(IDataCallback callBack) {
        new ChargeManualApi().send(callBack);
    }

    public void stopCharge(IDataCallback callBack) {
        new ChargeStopApi().send(callBack);
    }

    public void getChargeMatches(IDataCallback callBack) {
        new ChargeMatchTimesApi().send(callBack);
    }

    public void setChargeNumberNo(IDataCallback callBack, int chargeNo) {
        new ChargeNumbersApi().send(callBack, chargeNo);
    }

    public void chargeDefault(IDataCallback callBack, int pile) {
        new ChargeDefaultApi().send(callBack, pile);
    }
}
