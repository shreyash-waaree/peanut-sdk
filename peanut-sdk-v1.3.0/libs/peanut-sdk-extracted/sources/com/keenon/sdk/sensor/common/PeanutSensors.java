package com.keenon.sdk.sensor.common;

import android.content.Context;
import com.keenon.common.constant.PeanutConstants;
import com.keenon.common.utils.CheckUtils;
import com.keenon.common.utils.FileUtil;
import com.keenon.common.utils.LogUtils;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/sensor/common/PeanutSensors.class */
public class PeanutSensors {
    private static final String TAG = PeanutSensors.class.getSimpleName();
    private static volatile PeanutSensors instance;
    private final Map<String, Sensor> mSensors = new ConcurrentHashMap();
    private List<SensorObserver> globalObservers = Collections.synchronizedList(new ArrayList());

    private PeanutSensors() {
    }

    public static PeanutSensors getInstance() {
        if (instance == null) {
            synchronized (PeanutSensors.class) {
                if (instance == null) {
                    instance = new PeanutSensors();
                }
            }
        }
        return instance;
    }

    public static boolean initSensors(Context context) {
        return initSensors(context, PeanutConstants.SENSOR_CONFIG);
    }

    public static boolean initSensors(Context context, String configFile) {
        PeanutSensors manager = getInstance();
        manager.putSensorFactory(BaseSensorFactory.class);
        return manager.generateAndSetSensors(context, configFile);
    }

    public static void destroy() {
        if (instance != null) {
            instance.onDestroy();
        }
    }

    void onDestroy() {
        for (Sensor sensor : this.mSensors.values()) {
            sensor.clearObserver();
            sensor.release();
        }
        this.mSensors.clear();
        this.globalObservers.clear();
    }

    private boolean generateAndSetSensors(Context context, String configFile) {
        try {
            SensorConfig config = (SensorConfig) FileUtil.getAssetsObject(context, configFile, SensorConfig.class);
            if (CheckUtils.isEmpty(config.getFactories())) {
                return true;
            }
            for (String factory : config.getFactories()) {
                putSensors(((SensorFactory) Class.forName(factory).newInstance()).create());
            }
            return true;
        } catch (IOException e) {
            LogUtils.e(PeanutConstants.TAG_SENSOR, TAG + "初始化传感器失败", (Exception) e);
            return false;
        } catch (ClassNotFoundException e2) {
            LogUtils.e(PeanutConstants.TAG_SENSOR, TAG + "初始化传感器失败", (Exception) e2);
            return false;
        } catch (IllegalAccessException e3) {
            LogUtils.e(PeanutConstants.TAG_SENSOR, TAG + "初始化传感器失败", (Exception) e3);
            return false;
        } catch (InstantiationException e4) {
            LogUtils.e(PeanutConstants.TAG_SENSOR, TAG + "初始化传感器失败", (Exception) e4);
            return false;
        }
    }

    public void putSensorFactory(Class<? extends SensorFactory> factory) {
        try {
            putSensor(factory.newInstance());
        } catch (IllegalAccessException e) {
            LogUtils.e(PeanutConstants.TAG_SENSOR, TAG + "添加传感器失败", (Exception) e);
        } catch (InstantiationException e2) {
            LogUtils.e(PeanutConstants.TAG_SENSOR, TAG + "添加传感器失败", (Exception) e2);
        }
    }

    public void putSensor(SensorFactory factory) {
        putSensors(factory.create());
    }

    public void putSensor(Sensor sensor) {
        if (CheckUtils.isNotEmpty(this.globalObservers)) {
            for (SensorObserver observer : this.globalObservers) {
                sensor.addObserver(observer);
            }
        }
        sensor.mount();
        this.mSensors.put(sensor.name(), sensor);
    }

    public void removeSensorFactory(Class<? extends SensorFactory> factory) {
        try {
            removeSensor(factory.newInstance());
        } catch (IllegalAccessException e) {
            LogUtils.e(PeanutConstants.TAG_SENSOR, TAG + "移除传感器失败", (Exception) e);
        } catch (InstantiationException e2) {
            LogUtils.e(PeanutConstants.TAG_SENSOR, TAG + "移除传感器失败", (Exception) e2);
        }
    }

    public void removeSensor(SensorFactory factory) {
        removeSensor(factory.create());
    }

    public void removeSensor(List<Sensor> sensors) {
        List<Sensor> removeList = new ArrayList<>();
        if (!CheckUtils.isEmpty(sensors)) {
            for (Sensor sensor : sensors) {
                this.mSensors.remove(sensor.name());
                removeList.add(sensor);
            }
        }
        if (!CheckUtils.isEmpty(removeList)) {
            for (Sensor sensor2 : removeList) {
                sensor2.unMount();
                sensor2.clearObserver();
                sensor2.release();
            }
        }
    }

    public void removeSensor(Class<? extends Sensor> clazz) {
        List<Sensor> removeList = new ArrayList<>();
        ArrayList<Sensor> values = new ArrayList<>();
        values.addAll(this.mSensors.values());
        for (Sensor sensor : values) {
            if (clazz.isAssignableFrom(sensor.getClass())) {
                this.mSensors.remove(sensor.name());
                removeList.add(sensor);
            }
        }
        if (!CheckUtils.isEmpty(removeList)) {
            for (Sensor sensor2 : removeList) {
                sensor2.unMount();
                sensor2.clearObserver();
                sensor2.release();
            }
        }
    }

    public void putSensors(List<Sensor> sensors) {
        if (!CheckUtils.isEmpty(sensors)) {
            for (Sensor sensor : sensors) {
                putSensor(sensor);
            }
        }
    }

    public void registerObserver(String sensor, SensorObserver observer) {
        Sensor sensorObj = this.mSensors.get(sensor);
        if (sensorObj == null) {
            LogUtils.w(PeanutConstants.TAG_SENSOR, TAG + "could not register the observer on " + sensor + ", because it is not found!");
        } else {
            sensorObj.addObserver(observer);
        }
    }

    public void registerGlobalObserver(SensorObserver observer) {
        this.globalObservers.add(observer);
        Collection<Sensor> sensors = this.mSensors.values();
        if (!CheckUtils.isEmpty(sensors)) {
            for (Sensor sensor : sensors) {
                sensor.addObserver(observer);
            }
        }
    }

    public void removeGlobalObserver(SensorObserver observer) {
        this.globalObservers.remove(observer);
        Collection<Sensor> sensors = this.mSensors.values();
        if (!CheckUtils.isEmpty(sensors)) {
            for (Sensor sensor : sensors) {
                sensor.removeObserver(observer);
            }
        }
    }
}
