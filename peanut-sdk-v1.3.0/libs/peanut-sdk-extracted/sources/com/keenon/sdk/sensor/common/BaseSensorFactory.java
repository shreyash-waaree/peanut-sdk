package com.keenon.sdk.sensor.common;

import com.keenon.sdk.sensor.emotion.SensorEmotion;
import com.keenon.sdk.sensor.light.SensorLight;
import com.keenon.sdk.sensor.ota.SensorOTA;
import java.util.ArrayList;
import java.util.List;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/sensor/common/BaseSensorFactory.class */
public class BaseSensorFactory implements SensorFactory {
    @Override // com.keenon.sdk.sensor.common.SensorFactory
    public List<Sensor> create() {
        List<Sensor> sensors = new ArrayList<>();
        sensors.add(SensorOTA.getInstance());
        sensors.add(SensorEmotion.getInstance());
        sensors.add(SensorLight.getInstance());
        return sensors;
    }
}
