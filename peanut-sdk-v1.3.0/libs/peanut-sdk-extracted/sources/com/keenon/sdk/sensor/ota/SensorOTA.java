package com.keenon.sdk.sensor.ota;

import androidx.annotation.NonNull;
import com.keenon.common.error.PeanutError;
import com.keenon.sdk.sensor.common.Sensor;
import com.keenon.sdk.sensor.ota.base.Device;
import com.keenon.sdk.sensor.ota.base.IOta;
import com.keenon.sdk.sensor.ota.base.OTAConfig;
import com.keenon.sdk.sensor.ota.impl.RobotOTAImpl;
import com.keenon.sdk.sensor.ota.impl.RosOTAImpl;
import com.keenon.sdk.sensor.ota.impl.RosOTAImplV2;
import com.keenon.sdk.sensor.ota.impl.scm.DoorOTAImpl;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/sensor/ota/SensorOTA.class */
public class SensorOTA extends Sensor {
    private static volatile SensorOTA sInstance;
    private OTAConfig mConfig;
    private Device mDevice;
    private IOta.Listener mListener;
    private IOta mOTA;

    public static SensorOTA getInstance() {
        if (sInstance == null) {
            synchronized (SensorOTA.class) {
                if (sInstance == null) {
                    sInstance = new SensorOTA();
                }
            }
        }
        return sInstance;
    }

    @Override // com.keenon.sdk.sensor.common.Sensor
    public String name() {
        return SensorOTA.class.getSimpleName();
    }

    @Override // com.keenon.sdk.sensor.common.Sensor
    public void mount() {
    }

    @Override // com.keenon.sdk.sensor.common.Sensor
    public void unMount() {
    }

    @Override // com.keenon.sdk.sensor.common.Sensor
    public void release() {
        if (this.mOTA != null) {
            this.mOTA.release();
        }
        this.mOTA = null;
    }

    public void start(@NonNull OTAConfig config, @NonNull IOta.Listener listener) {
        this.mConfig = config;
        this.mListener = listener;
        stop();
        release();
        this.mOTA = buildOTAProxy();
        if (this.mOTA == null) {
            this.mListener.onError(PeanutError.OTA_DEVICE_NOT_SUPPORT);
        } else {
            this.mOTA.start(this.mConfig, this.mListener);
        }
    }

    public void stop() {
        if (this.mOTA != null) {
            this.mOTA.stop();
        }
    }

    private IOta buildOTAProxy() {
        this.mDevice = this.mConfig.device();
        log("buildOTAProxy", "当前升级设备: " + this.mDevice.toString());
        if (this.mDevice == null) {
            return null;
        }
        switch (this.mDevice) {
            case ROBOT_ARM:
                return new RobotOTAImpl();
            case DOOR:
                return new DoorOTAImpl();
            case ROS:
            case STM32:
            case GENERAL:
                return OTAConfig.OTAVersion.V1 == this.mConfig.otaVersion() ? new RosOTAImpl() : new RosOTAImplV2();
            default:
                return null;
        }
    }
}
