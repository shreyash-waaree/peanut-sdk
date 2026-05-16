package com.keenon.sdk.sensor.common;

import com.keenon.common.constant.PeanutConstants;
import com.keenon.common.utils.GsonUtil;
import com.keenon.common.utils.LogUtils;
import com.keenon.sdk.external.PeanutSDK;
import com.keenon.sdk.hedera.model.BaseResponse;
import java.util.concurrent.CopyOnWriteArrayList;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/sensor/common/Sensor.class */
public abstract class Sensor {
    private boolean mUsbDirect;
    private final CopyOnWriteArrayList<SensorObserver> mObservers = new CopyOnWriteArrayList<>();
    private boolean isTrans;
    private boolean isSerialDirect;

    public abstract String name();

    public abstract void mount();

    public abstract void unMount();

    public abstract void release();

    protected void notifyEvent(Event event, Sensor sensor) {
        log("notifyEvent", event.getName() + sensor.name() + " Size " + this.mObservers.size());
        if (this.mObservers.size() > 0) {
            for (SensorObserver observer : this.mObservers) {
                observer.onUpdate(event, sensor);
            }
        }
    }

    protected synchronized void notifyEvent(String name, Object data) {
        log(name);
        Event event = new Event();
        event.setName(name);
        event.setData(data);
        notifyEvent(event, this);
    }

    protected void notifySubscribeSuccess(String topicName, Object data) {
        BaseResponse response = new BaseResponse();
        response.setCode(0);
        response.setStatus(0);
        response.setTopic(topicName);
        response.setData(data);
        PeanutSDK.getInstance().notifySubscribeSuccess(topicName, GsonUtil.bean2String(response));
    }

    public void log(String tag) {
        LogUtils.d(PeanutConstants.TAG_SENSOR, "[" + name() + "][" + tag + "]");
    }

    public void log(String tag, String content) {
        LogUtils.d(PeanutConstants.TAG_SENSOR, "[" + name() + "][" + tag + "][" + content + "]");
    }

    public void log(String tag, int content) {
        LogUtils.d(PeanutConstants.TAG_SENSOR, "[" + name() + "][" + tag + "][" + content + "]");
    }

    public void setUSBDirect(boolean usbDirect) {
        this.mUsbDirect = usbDirect;
    }

    public boolean isUsbDirect() {
        return this.mUsbDirect;
    }

    public boolean isTrans() {
        return this.isTrans;
    }

    public void setTrans(boolean trans) {
        this.isTrans = trans;
    }

    public boolean isSerialDirect() {
        return this.isSerialDirect;
    }

    public void setSerialDirect(boolean serialDirect) {
        this.isSerialDirect = serialDirect;
    }

    public void addObserver(SensorObserver observer) {
        this.mObservers.add(observer);
    }

    public void clearObserver() {
        this.mObservers.clear();
    }

    public boolean removeObserver(SensorObserver observer) {
        return this.mObservers.remove(observer);
    }

    public boolean contains(SensorObserver observer) {
        return this.mObservers.contains(observer);
    }
}
