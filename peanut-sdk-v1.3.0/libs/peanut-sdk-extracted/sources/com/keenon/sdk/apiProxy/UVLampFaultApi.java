package com.keenon.sdk.apiProxy;

import com.keenon.sdk.external.IDataCallback;
import com.keenon.sdk.sensor.uvlamp.SensorUVLamp;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/apiProxy/UVLampFaultApi.class */
public class UVLampFaultApi {
    public void observe(IDataCallback callBack) {
        SensorUVLamp.getInstance().getUVLampSwitch(44, null);
    }
}
