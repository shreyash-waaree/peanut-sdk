package com.keenon.sdk.sensor.light;

import com.keenon.common.constant.PeanutConstants;
import com.keenon.common.utils.ByteUtils;
import com.keenon.sdk.api.LightDisplayApi;
import com.keenon.sdk.api2.UpgradeActionApi;
import com.keenon.sdk.external.IDataCallback;
import com.keenon.sdk.hedera.model.ApiError;
import com.keenon.sdk.scmIot.SCMIoTSender;
import com.keenon.sdk.scmIot.bean.SCMRequest;
import com.keenon.sdk.scmIot.bean.request.LEDMultiColorConfig;
import com.keenon.sdk.scmIot.bean.request.LEDMultiColorConfigV2;
import com.keenon.sdk.scmIot.bean.request.LEDMultiColorConfigV3;
import com.keenon.sdk.scmIot.bean.request.LEDRGBConfig;
import com.keenon.sdk.scmIot.bean.request.LEDSingleColorConfig;
import com.keenon.sdk.sensor.common.Sensor;
import com.keenon.sdk.sensor.common.SensorEvent;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.eclipse.californium.elements.util.NamedThreadFactory;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/sensor/light/SensorLight.class */
public class SensorLight extends Sensor {
    private static volatile SensorLight sInstance;
    private volatile boolean isStartScheduled = false;
    private volatile boolean isHeartBeatStart = false;
    private int heartBeatDev = -1;
    private ScheduledExecutorService scheduledThreadPool = new ScheduledThreadPoolExecutor(1, new NamedThreadFactory("V3HeartBeat#"));

    public void setHeartBeatDev(int heartBeatDev) {
        this.heartBeatDev = heartBeatDev;
    }

    public static SensorLight getInstance() {
        if (sInstance == null) {
            synchronized (SensorLight.class) {
                if (sInstance == null) {
                    sInstance = new SensorLight();
                }
            }
        }
        return sInstance;
    }

    @Override // com.keenon.sdk.sensor.common.Sensor
    public String name() {
        return SensorLight.class.getSimpleName();
    }

    @Override // com.keenon.sdk.sensor.common.Sensor
    public void mount() {
        log(UpgradeActionApi.ACTION_START);
    }

    @Override // com.keenon.sdk.sensor.common.Sensor
    public void unMount() {
    }

    @Override // com.keenon.sdk.sensor.common.Sensor
    public void release() {
    }

    public synchronized void onStartHeartBeatV3() {
        this.isHeartBeatStart = true;
        if (this.isStartScheduled) {
            return;
        }
        this.isStartScheduled = true;
        this.scheduledThreadPool.scheduleWithFixedDelay(new Runnable() { // from class: com.keenon.sdk.sensor.light.SensorLight.1
            @Override // java.lang.Runnable
            public void run() {
                if (SensorLight.this.isHeartBeatStart) {
                    SensorLight.this.playHeartBeatV3();
                }
            }
        }, 0L, 2L, TimeUnit.SECONDS);
    }

    public synchronized void onStopHeartBeatV3() {
        this.isHeartBeatStart = false;
    }

    public void displayLight(int lightId, Integer intensity, int mode) {
        new LightDisplayApi().send(new IDataCallback() { // from class: com.keenon.sdk.sensor.light.SensorLight.2
            @Override // com.keenon.sdk.external.IDataCallback
            public void success(String result) {
                SensorLight.this.notifyEvent(SensorEvent.PLAY_LIGHT_WARNING_ACK, result);
            }

            @Override // com.keenon.sdk.external.IDataCallback
            public void error(ApiError error) {
                SensorLight.this.notifyEvent(SensorEvent.REQUEST_FAILED, error);
            }
        }, lightId, intensity, mode);
    }

    public void play(LightConfig lightConfig) {
        if (lightConfig == null) {
            return;
        }
        if (lightConfig.getType() == 0) {
            playSingleColor(lightConfig);
        } else {
            playMultiColor(lightConfig);
        }
    }

    private void playSingleColor(LightConfig lightConfig) {
        log("playSingleColor", lightConfig.toString());
        LEDSingleColorConfig inputBean = new LEDSingleColorConfig();
        inputBean.setOnTime((short) lightConfig.getBlink().getOnTime());
        inputBean.setOffTime((short) lightConfig.getBlink().getOffTime());
        inputBean.setRepeat((byte) lightConfig.getBlink().getRepeat());
        SCMRequest request = new SCMRequest();
        request.setDev(lightConfig.getDevId());
        request.setTopic(2);
        request.setType(2);
        request.setCmd(6);
        request.setParams(inputBean);
        request.setSerialDirect(lightConfig.isSerialDirect(), lightConfig.getSerialProtocolVersion());
        request.setUSBDirect(lightConfig.isUSBDirect());
        SCMIoTSender.sendRequest(request, new IDataCallback() { // from class: com.keenon.sdk.sensor.light.SensorLight.3
            @Override // com.keenon.sdk.external.IDataCallback
            public void success(String result) {
                SensorLight.this.notifyEvent(SensorEvent.PLAY_LIGHT_SINGLE_ACK, result);
            }

            @Override // com.keenon.sdk.external.IDataCallback
            public void error(ApiError error) {
                SensorLight.this.notifyEvent(SensorEvent.REQUEST_FAILED, error);
            }
        });
    }

    private void playMultiColor(LightConfig lightConfig) {
        log("playMultiColor", lightConfig.toString());
        if (lightConfig.getVer().equals("v1.0")) {
            playMultiColorV1(lightConfig);
        } else if (lightConfig.getVer().equals("v2.0")) {
            playMultiColorV2(lightConfig);
        } else if (lightConfig.getVer().equals(PeanutConstants.SCM_VER_3)) {
            playMultiColorV3(lightConfig);
        }
    }

    private void playMultiColorV1(LightConfig lightConfig) {
        log("playMultiColorV1");
        LEDMultiColorConfig config = new LEDMultiColorConfig();
        config.setNum((byte) lightConfig.getBeads().getNum());
        config.setMode((byte) lightConfig.getEffect().getId());
        config.setTime(lightConfig.getEffect().getTime());
        SCMRequest request = new SCMRequest();
        request.setDev(lightConfig.getDevId());
        request.setTopic(3);
        request.setType(2);
        request.setCmd(6);
        request.setParams(config);
        request.setSerialDirect(lightConfig.isSerialDirect(), lightConfig.getSerialProtocolVersion());
        request.setUSBDirect(lightConfig.isUSBDirect());
        SCMIoTSender.sendRequest(request, new IDataCallback() { // from class: com.keenon.sdk.sensor.light.SensorLight.4
            @Override // com.keenon.sdk.external.IDataCallback
            public void success(String result) {
                SensorLight.this.notifyEvent(SensorEvent.PLAY_LIGHT_MULTI_V1_ACK, result);
            }

            @Override // com.keenon.sdk.external.IDataCallback
            public void error(ApiError error) {
                SensorLight.this.notifyEvent(SensorEvent.REQUEST_FAILED, error);
            }
        });
    }

    private void playMultiColorV2(LightConfig lightConfig) {
        log("playMultiColorV2");
        LEDMultiColorConfigV2 config = new LEDMultiColorConfigV2();
        config.setNum((short) lightConfig.getBeads().getNum());
        config.setHead((short) lightConfig.getBeads().getHead());
        config.setMode((short) lightConfig.getEffect().getId());
        config.setTime((short) Math.floor(lightConfig.getEffect().getTime() / 100));
        SCMRequest request = new SCMRequest();
        request.setDev(lightConfig.getDevId());
        request.setTopic(11);
        request.setType(2);
        request.setCmd(6);
        request.setParams(config);
        if (lightConfig.isTrans() || isTrans()) {
            request.setSerialDirect(false, lightConfig.getSerialProtocolVersion());
            request.setTrans(lightConfig.isTrans() || isTrans());
        } else {
            request.setSerialDirect(true, lightConfig.getSerialProtocolVersion());
        }
        request.setUSBDirect(lightConfig.isUSBDirect() || isUsbDirect());
        SCMIoTSender.sendRequest(request, new IDataCallback() { // from class: com.keenon.sdk.sensor.light.SensorLight.5
            @Override // com.keenon.sdk.external.IDataCallback
            public void success(String result) {
                SensorLight.this.notifyEvent(SensorEvent.PLAY_LIGHT_MULTI_V2_ACK, result);
            }

            @Override // com.keenon.sdk.external.IDataCallback
            public void error(ApiError error) {
                SensorLight.this.notifyEvent(SensorEvent.REQUEST_FAILED, error);
            }
        });
    }

    private void playMultiColorV3(LightConfig lightConfig) {
        log("playMultiColorV3");
        LEDMultiColorConfigV3 config = new LEDMultiColorConfigV3();
        config.setHead((short) lightConfig.getBeads().getHead());
        config.setNum((short) lightConfig.getBeads().getNum());
        config.setRgbs(lightConfig.getBeads().getBytes());
        SCMRequest request = new SCMRequest();
        request.setDev(lightConfig.getDevId());
        request.setTopic(22);
        request.setType(2);
        request.setCmd(6);
        request.setParams(config);
        if (isUsbDirect() || isSerialDirect() || isTrans()) {
            request.setUSBDirect(isUsbDirect());
            request.setTrans(isTrans());
            request.setSerialDirect(isSerialDirect());
        } else {
            request.setUSBDirect(true);
        }
        SCMIoTSender.sendRequest(request, new IDataCallback() { // from class: com.keenon.sdk.sensor.light.SensorLight.6
            @Override // com.keenon.sdk.external.IDataCallback
            public void success(String result) {
                SensorLight.this.notifyEvent(SensorEvent.PLAY_LIGHT_MULTI_V3_ACK, result);
            }

            @Override // com.keenon.sdk.external.IDataCallback
            public void error(ApiError error) {
                SensorLight.this.notifyEvent(SensorEvent.REQUEST_FAILED, error);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void playHeartBeatV3() {
        log("playHeartBeatV3");
        SCMRequest request = new SCMRequest();
        if (this.heartBeatDev == -1) {
            request.setDev(6);
        } else {
            request.setDev(this.heartBeatDev);
        }
        request.setTopic(23);
        request.setType(2);
        request.setCmd(6);
        if (isUsbDirect() || isSerialDirect() || isTrans()) {
            request.setUSBDirect(isUsbDirect());
            request.setTrans(isTrans());
            request.setSerialDirect(isSerialDirect());
        } else {
            request.setUSBDirect(true);
        }
        SCMIoTSender.sendRequest(request, new IDataCallback() { // from class: com.keenon.sdk.sensor.light.SensorLight.7
            @Override // com.keenon.sdk.external.IDataCallback
            public void success(String result) {
                SensorLight.this.notifyEvent(SensorEvent.PLAY_LIGHT_HeartBeat_V3_ACK, result);
            }

            @Override // com.keenon.sdk.external.IDataCallback
            public void error(ApiError error) {
                SensorLight.this.notifyEvent(SensorEvent.REQUEST_FAILED, error);
            }
        });
    }

    public void setRGB(LightConfig lightConfig) {
        log("setRGB", lightConfig.toString());
        if (lightConfig.getVer().equals("v1.0")) {
            setRGBV1(lightConfig);
        } else if (lightConfig.getVer().equals("v2.0")) {
            setRGBV2(lightConfig);
        }
    }

    private void setRGBV1(LightConfig lightConfig) {
        log("setRGBV1");
        byte[] rgb = {(byte) lightConfig.getColor().getMask(), (byte) lightConfig.getColor().getR(), (byte) lightConfig.getColor().getG(), (byte) lightConfig.getColor().getB()};
        LEDRGBConfig inputBean = new LEDRGBConfig();
        inputBean.setRgb(ByteUtils.bytesToInt2(rgb, 0));
        SCMRequest request = new SCMRequest();
        request.setDev(lightConfig.getDevId());
        request.setTopic(3);
        request.setType(0);
        request.setCmd(1);
        request.setParams(inputBean);
        request.setCon(false);
        SCMIoTSender.sendRequest(request, new IDataCallback() { // from class: com.keenon.sdk.sensor.light.SensorLight.8
            @Override // com.keenon.sdk.external.IDataCallback
            public void success(String result) {
                SensorLight.this.notifyEvent(SensorEvent.SET_LIGHT_RGB_V1_ACK, result);
            }

            @Override // com.keenon.sdk.external.IDataCallback
            public void error(ApiError error) {
                SensorLight.this.notifyEvent(SensorEvent.REQUEST_FAILED, error);
            }
        });
    }

    private void setRGBV2(LightConfig lightConfig) {
        log("setRGBV2");
        byte[] rgb = {(byte) lightConfig.getColor().getMask(), (byte) lightConfig.getColor().getR(), (byte) lightConfig.getColor().getG(), (byte) lightConfig.getColor().getB()};
        LEDRGBConfig inputBean = new LEDRGBConfig();
        inputBean.setRgb(ByteUtils.bytesToInt2(rgb, 0));
        SCMRequest request = new SCMRequest();
        request.setDev(lightConfig.getDevId());
        request.setTopic(11);
        request.setType(0);
        request.setCmd(1);
        request.setParams(inputBean);
        request.setSerialDirect(true);
        SCMIoTSender.sendRequest(request, new IDataCallback() { // from class: com.keenon.sdk.sensor.light.SensorLight.9
            @Override // com.keenon.sdk.external.IDataCallback
            public void success(String result) {
                SensorLight.this.notifyEvent(SensorEvent.SET_LIGHT_RGB_V2_ACK, result);
            }

            @Override // com.keenon.sdk.external.IDataCallback
            public void error(ApiError error) {
                SensorLight.this.notifyEvent(SensorEvent.REQUEST_FAILED, error);
            }
        });
    }
}
