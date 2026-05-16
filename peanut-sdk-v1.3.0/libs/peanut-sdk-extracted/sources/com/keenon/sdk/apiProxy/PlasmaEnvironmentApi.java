package com.keenon.sdk.apiProxy;

import com.keenon.sdk.external.IDataCallback;
import com.keenon.sdk.sensor.plasma.SensorPlasma;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/apiProxy/PlasmaEnvironmentApi.class */
public class PlasmaEnvironmentApi {
    public void observe(IDataCallback callBack) {
        SensorPlasma.getInstance().subscribePlasmaEnvironmentInfo();
    }

    public void cancel() {
    }
}
