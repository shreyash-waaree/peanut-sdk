package com.keenon.sdk.sensor.ota.impl;

import com.keenon.sdk.sensor.ota.base.IOta;
import com.keenon.sdk.sensor.ota.base.OTAConfig;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/sensor/ota/impl/RobotOTAImpl.class */
public class RobotOTAImpl implements IOta {
    private static final String TAG = "RobotOTAImpl";

    @Override // com.keenon.sdk.sensor.ota.base.IOta
    public void start(OTAConfig config, IOta.Listener listener) {
    }

    @Override // com.keenon.sdk.sensor.ota.base.IOta
    public void stop() {
    }

    @Override // com.keenon.sdk.sensor.ota.base.IOta
    public OTAConfig getConfig() {
        return null;
    }

    @Override // com.keenon.sdk.sensor.ota.base.IOta
    public void release() {
    }
}
