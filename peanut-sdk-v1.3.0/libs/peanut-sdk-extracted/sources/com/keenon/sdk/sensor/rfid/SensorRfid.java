package com.keenon.sdk.sensor.rfid;

import com.keenon.sdk.external.IDataCallback2;
import com.keenon.sdk.sensor.common.Sensor;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/sensor/rfid/SensorRfid.class */
public class SensorRfid extends Sensor {
    private static volatile SensorRfid sInstance;

    public static SensorRfid getInstance() {
        if (sInstance == null) {
            synchronized (SensorRfid.class) {
                if (sInstance == null) {
                    sInstance = new SensorRfid();
                }
            }
        }
        return sInstance;
    }

    @Override // com.keenon.sdk.sensor.common.Sensor
    public String name() {
        return SensorRfid.class.getSimpleName();
    }

    @Override // com.keenon.sdk.sensor.common.Sensor
    public void mount() {
    }

    @Override // com.keenon.sdk.sensor.common.Sensor
    public void unMount() {
    }

    @Override // com.keenon.sdk.sensor.common.Sensor
    public void release() {
    }

    public void readRfid(int dev, IDataCallback2<String> callback) {
    }

    public void writeRfid(int dev, String data, IDataCallback2<String> callback) {
    }
}
