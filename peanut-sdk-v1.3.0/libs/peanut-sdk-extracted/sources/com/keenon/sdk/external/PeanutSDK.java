package com.keenon.sdk.external;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Environment;
import com.keenon.common.constant.PeanutConstants;
import com.keenon.common.error.ExceptionMessage;
import com.keenon.common.error.PeanutError;
import com.keenon.common.external.PeanutConfig;
import com.keenon.common.external.iPing;
import com.keenon.common.log.LogManager;
import com.keenon.common.log.PeanutStatistics;
import com.keenon.common.utils.CheckUtils;
import com.keenon.common.utils.InfoUtils;
import com.keenon.common.utils.LogUtils;
import com.keenon.common.utils.PeanutMainThreadExecutor;
import com.keenon.common.utils.ReflectUtil;
import com.keenon.sdk.auth.OfflineAuth;
import com.keenon.sdk.component.BaseComponent;
import com.keenon.sdk.component.BatteryComponent;
import com.keenon.sdk.component.Component;
import com.keenon.sdk.component.ComponentManager;
import com.keenon.sdk.component.DeviceComponent;
import com.keenon.sdk.component.DeviceNodeComponent;
import com.keenon.sdk.component.DisinfectComponent;
import com.keenon.sdk.component.DoorComponent;
import com.keenon.sdk.component.ElevatorComponent;
import com.keenon.sdk.component.MapComponent;
import com.keenon.sdk.component.MotorComponent;
import com.keenon.sdk.component.NavigationComponent;
import com.keenon.sdk.component.RuntimeComponent;
import com.keenon.sdk.component.ScheduleComponent;
import com.keenon.sdk.component.UpdateComponent;
import com.keenon.sdk.component.VendorComponent;
import com.keenon.sdk.hedera.model.ApiError;
import com.keenon.sdk.proxy.adapter.LinkAdapterFactory;
import com.keenon.sdk.proxy.adapter.PingTask;
import com.keenon.sdk.proxy.sender.SenderManager;
import com.keenon.sdk.sensor.common.Event;
import com.keenon.sdk.sensor.common.PeanutSensors;
import com.keenon.sdk.sensor.common.Sensor;
import com.keenon.sdk.sensor.common.SensorObserver;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/external/PeanutSDK.class */
public class PeanutSDK implements SensorObserver {
    private static final String TAG = "[PeanutSDK]";
    public static int SDK_STATUS_DEFAULT = -1;
    public static int SDK_INIT_SUCCESS = 0;
    private static volatile PeanutSDK sInstance;
    private int mSDKStatusCode;
    private PeanutMainThreadExecutor mainThreadExecutor;
    private Context mContext;
    private final List<ErrorListener> mListeners = new ArrayList();
    private ConcurrentHashMap<String, Set<IDataCallback>> subscribeCallbacks = new ConcurrentHashMap<>();

    /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/external/PeanutSDK$ErrorListener.class */
    public interface ErrorListener {
        void onInit(int i);
    }

    PeanutSDK() {
    }

    public void init(Context context, ErrorListener listener) {
        this.mSDKStatusCode = SDK_STATUS_DEFAULT;
        LogUtils.init(PeanutConfig.isLogEnable(), PeanutConfig.getLogLevel(), PeanutConfig.isLogWriteFileEnable());
        if (context == null) {
            LogUtils.e(PeanutConstants.TAG_SDK, 203000);
            throw new RuntimeException(ExceptionMessage.NullContext);
        }
        this.mContext = context.getApplicationContext();
        if (this.mSDKStatusCode == SDK_INIT_SUCCESS) {
            LogUtils.d(PeanutConstants.TAG_SDK, "[PeanutSDK][SDK already init.]");
            return;
        }
        this.mainThreadExecutor = new PeanutMainThreadExecutor();
        registerListener(listener);
        initPermissionCheck(PeanutConstants.SDK_PERMISSION_LIST);
        initLog();
        initAuth();
    }

    public static PeanutSDK getInstance() {
        if (sInstance == null) {
            synchronized (PeanutSDK.class) {
                if (sInstance == null) {
                    sInstance = new PeanutSDK();
                }
            }
        }
        return sInstance;
    }

    public void release() {
        try {
            try {
                LogUtils.d(PeanutConstants.TAG_SDK, "[PeanutSDK][release]");
                ComponentManager.getInstance().release();
                PeanutSensors.destroy();
                LinkAdapterFactory.getInstance().release();
                SenderManager.getInstance().release();
                ComponentManager.getInstance().release();
                this.subscribeCallbacks.clear();
                LogUtils.i(PeanutConstants.TAG_SDK, "[PeanutSDK][release][finally]");
                this.mListeners.clear();
                this.mSDKStatusCode = SDK_STATUS_DEFAULT;
            } catch (Exception e) {
                LogUtils.e(PeanutConstants.TAG_SDK, "[PeanutSDK][release][exception:" + e.getMessage() + "]");
                LogUtils.i(PeanutConstants.TAG_SDK, "[PeanutSDK][release][finally]");
                this.mListeners.clear();
                this.mSDKStatusCode = SDK_STATUS_DEFAULT;
            }
        } catch (Throwable th) {
            LogUtils.i(PeanutConstants.TAG_SDK, "[PeanutSDK][release][finally]");
            this.mListeners.clear();
            this.mSDKStatusCode = SDK_STATUS_DEFAULT;
            throw th;
        }
    }

    public boolean registerListener(ErrorListener listener) {
        return this.mListeners.add(listener);
    }

    public boolean removeListener(ErrorListener listener) {
        return this.mListeners.remove(listener);
    }

    private PeanutMainThreadExecutor getDefaultCallbackExecutor() {
        return new PeanutMainThreadExecutor();
    }

    private void initPermissionCheck(String... permissionList) {
        if (this.mSDKStatusCode <= 0) {
            LogUtils.i(PeanutConstants.TAG_SDK, "[PeanutSDK][initPermissionCheck]");
            PackageManager packageManager = this.mContext.getPackageManager();
            String packageName = this.mContext.getPackageName();
            for (String permission : permissionList) {
                int permissionStatus = packageManager.checkPermission(permission, packageName);
                if (-1 == permissionStatus) {
                    notifyStatusChanged(PeanutError.INIT_PERMISSION_DENIED, false);
                    LogUtils.e(TAG, PeanutError.INIT_PERMISSION_DENIED);
                    throw new RuntimeException(ExceptionMessage.PermissionDenied + permission);
                }
            }
        }
    }

    private void initLog() {
        if (this.mSDKStatusCode <= 0) {
            LogUtils.i(PeanutConstants.TAG_SDK, "[PeanutSDK][initLog]");
            if (!LogManager.getInstance().init) {
                LogManager.getInstance().init(PeanutConstants.LOG_PATH, PeanutConfig.isLogbackEnable(), PeanutConfig.isLogbackPrintLogCatEnable(), false, true, this.mContext, Environment.getExternalStorageState().equals("mounted"), InfoUtils.getFormattedAppLogInfo(new InfoUtils.ApplogInfo().init(this.mContext)), PeanutConfig.isLogbackWriteFileEnable());
            }
        }
    }

    private void initAuth() {
        LogUtils.i(PeanutConstants.TAG_SDK, "[PeanutSDK][initAuth]");
        int result = OfflineAuth.getInstance().start(this.mContext, PeanutConfig.getAppId(), PeanutConfig.getSecret());
        LogUtils.i(PeanutConstants.TAG_SDK, "[PeanutSDK][initAuth][Auth :" + result + "]");
        if (result == 0) {
            initUMStatistics();
            initPingTask();
            initSensors();
            initComponent();
            return;
        }
        notifyStatusChanged(result, false);
    }

    private void initComponent() {
        LogUtils.i(PeanutConstants.TAG_SDK, "[PeanutSDK][initComponent]");
        for (Component component : createComponents()) {
            boolean isSuccess = ComponentManager.getInstance().registerComponent(component);
            if (isSuccess && ComponentManager.getInstance().getComponent(component.getComponentName()) != null) {
                LogUtils.i(PeanutConstants.TAG_SDK, "[PeanutSDK][initComponent][" + component.getComponentName() + "]");
                ComponentManager.getInstance().getComponent(component.getComponentName()).init(null);
            }
        }
        notifyStatusChanged(SDK_INIT_SUCCESS, true);
    }

    private void initSensors() {
        PeanutSensors.initSensors(this.mContext);
        PeanutSensors.getInstance().registerGlobalObserver(this);
    }

    private void initUMStatistics() {
        if (PeanutConfig.isUmLogEnable()) {
            PeanutStatistics.getInstance().init(this.mContext, PeanutConfig.isUmLogEnable());
        }
    }

    private void initPingTask() {
        PingTask.getInstance().init(PeanutConfig.getConnectionTimeout(), new iPing.Listener() { // from class: com.keenon.sdk.external.PeanutSDK.1
            @Override // com.keenon.common.external.iPing.Listener
            public void onUpdate(int state) {
                PeanutSDK.this.notifyError(state);
            }
        });
    }

    private ArrayList<Component> createComponents() {
        ArrayList<Component> components = new ArrayList<>();
        components.add(new BaseComponent());
        components.add(new BatteryComponent());
        components.add(new DeviceComponent());
        components.add(new DisinfectComponent());
        components.add(new DoorComponent());
        components.add(new ElevatorComponent());
        components.add(new MapComponent());
        components.add(new MotorComponent());
        components.add(new NavigationComponent());
        components.add(new ScheduleComponent());
        components.add(new RuntimeComponent());
        components.add(new UpdateComponent());
        components.add(new VendorComponent());
        components.add(new DeviceNodeComponent());
        return components;
    }

    public BatteryComponent battery() {
        BatteryComponent component = (BatteryComponent) ComponentManager.getInstance().getComponent(Component.RobotComponentName.COMPONENT_BATTERY.getComponentName());
        return (BatteryComponent) CheckUtils.checkNotNull(component, ExceptionMessage.NullComponent);
    }

    public DeviceComponent device() {
        DeviceComponent component = (DeviceComponent) ComponentManager.getInstance().getComponent(Component.RobotComponentName.COMPONENT_DEVICE.getComponentName());
        return (DeviceComponent) CheckUtils.checkNotNull(component, ExceptionMessage.NullComponent);
    }

    public DisinfectComponent disinfect() {
        DisinfectComponent component = (DisinfectComponent) ComponentManager.getInstance().getComponent(Component.RobotComponentName.COMPONENT_DISINFECT.getComponentName());
        return (DisinfectComponent) CheckUtils.checkNotNull(component, ExceptionMessage.NullComponent);
    }

    public DoorComponent door() {
        DoorComponent component = (DoorComponent) ComponentManager.getInstance().getComponent(Component.RobotComponentName.COMPONENT_DOOR.getComponentName());
        return (DoorComponent) CheckUtils.checkNotNull(component, ExceptionMessage.NullComponent);
    }

    public ElevatorComponent elevator() {
        ElevatorComponent component = (ElevatorComponent) ComponentManager.getInstance().getComponent(Component.RobotComponentName.COMPONENT_ELEVATOR.getComponentName());
        return (ElevatorComponent) CheckUtils.checkNotNull(component, ExceptionMessage.NullComponent);
    }

    public MapComponent map() {
        MapComponent component = (MapComponent) ComponentManager.getInstance().getComponent(Component.RobotComponentName.COMPONENT_MAP.getComponentName());
        return (MapComponent) CheckUtils.checkNotNull(component, ExceptionMessage.NullComponent);
    }

    public MotorComponent motor() {
        MotorComponent component = (MotorComponent) ComponentManager.getInstance().getComponent(Component.RobotComponentName.COMPONENT_MOTOR.getComponentName());
        return (MotorComponent) CheckUtils.checkNotNull(component, ExceptionMessage.NullComponent);
    }

    public NavigationComponent navigation() {
        NavigationComponent component = (NavigationComponent) ComponentManager.getInstance().getComponent(Component.RobotComponentName.COMPONENT_NAVIGATION.getComponentName());
        return (NavigationComponent) CheckUtils.checkNotNull(component, ExceptionMessage.NullComponent);
    }

    public BaseComponent robot() {
        BaseComponent component = (BaseComponent) ComponentManager.getInstance().getComponent(Component.RobotComponentName.COMPONENT_BASE.getComponentName());
        return (BaseComponent) CheckUtils.checkNotNull(component, ExceptionMessage.NullComponent);
    }

    public RuntimeComponent runtime() {
        RuntimeComponent component = (RuntimeComponent) ComponentManager.getInstance().getComponent(Component.RobotComponentName.COMPONENT_RUNTIME.getComponentName());
        return (RuntimeComponent) CheckUtils.checkNotNull(component, ExceptionMessage.NullComponent);
    }

    public ScheduleComponent schedule() {
        ScheduleComponent component = (ScheduleComponent) ComponentManager.getInstance().getComponent(Component.RobotComponentName.COMPONENT_SCHEDULE.getComponentName());
        return (ScheduleComponent) CheckUtils.checkNotNull(component, ExceptionMessage.NullComponent);
    }

    public UpdateComponent update() {
        UpdateComponent component = (UpdateComponent) ComponentManager.getInstance().getComponent(Component.RobotComponentName.COMPONENT_UPDATE.getComponentName());
        return (UpdateComponent) CheckUtils.checkNotNull(component, ExceptionMessage.NullComponent);
    }

    public VendorComponent vendor() {
        VendorComponent component = (VendorComponent) ComponentManager.getInstance().getComponent(Component.RobotComponentName.COMPONENT_VENDOR.getComponentName());
        return (VendorComponent) CheckUtils.checkNotNull(component, ExceptionMessage.NullComponent);
    }

    public DeviceNodeComponent deviceNode() {
        DeviceNodeComponent component = (DeviceNodeComponent) ComponentManager.getInstance().getComponent(Component.RobotComponentName.COMPONENT_DEVICE_NODE.getComponentName());
        return (DeviceNodeComponent) CheckUtils.checkNotNull(component, ExceptionMessage.NullComponent);
    }

    public PeanutMainThreadExecutor getMainThreadExecutor() {
        return CheckUtils.isNull(this.mainThreadExecutor) ? getDefaultCallbackExecutor() : this.mainThreadExecutor;
    }

    public synchronized void subscribe(String topic, IDataCallback callBack) {
        Set<IDataCallback> callbacksForKey = this.subscribeCallbacks.get(topic);
        if (callbacksForKey == null) {
            callbacksForKey = Collections.synchronizedSet(new HashSet());
            this.subscribeCallbacks.put(topic, callbacksForKey);
            subscribeInternal(topic, callBack);
        }
        callbacksForKey.add(callBack);
    }

    public synchronized void unSubscribe(String topic, IDataCallback callBack) {
        Set<IDataCallback> callbacksForKey = this.subscribeCallbacks.get(topic);
        if (callbacksForKey != null) {
            callbacksForKey.remove(callBack);
            if (callbacksForKey.isEmpty()) {
                this.subscribeCallbacks.remove(topic);
                unSubscribeInternal(topic);
            }
        }
    }

    private synchronized void subscribeInternal(String topic, IDataCallback callBack) {
        String path = mathTopicPath(topic);
        try {
            ReflectUtil.executeObjectMethod(ReflectUtil.getInstance(path + topic, null, null), Class.forName(path + topic), new Class[]{IDataCallback.class}, "observe", callBack);
        } catch (ClassNotFoundException e) {
            LogUtils.e(PeanutConstants.TAG_SDK, TAG, (Exception) e);
        }
    }

    private synchronized String mathTopicPath(String topic) {
        String path;
        path = PeanutConstants.API_PACKAGE;
        switch (topic) {
            case "UpgradeStatusApi":
                path = PeanutConstants.API_PACKAGE_V2;
                break;
            case "UVLampFaultApi":
            case "PlasmaFaultApi":
            case "PlasmaEnvironmentApi":
            case "DoorSwitchFaultApi":
            case "DoorSwitchStatusApi":
                path = PeanutConstants.API_PACKAGE_PROXY;
                break;
        }
        return path;
    }

    private synchronized void unSubscribeInternal(String topic) {
        String path = mathTopicPath(topic);
        try {
            ReflectUtil.executeObjectMethod(ReflectUtil.getInstance(path + topic, null, null), Class.forName(path + topic), new Class[0], "cancel", new Object[0]);
        } catch (ClassNotFoundException e) {
            LogUtils.e(PeanutConstants.TAG_SDK, TAG, (Exception) e);
        }
    }

    public synchronized void notifyApiSuccess(Object obj, String response) {
        notifySubscribeSuccess(ReflectUtil.getClassName(obj.getClass()), response);
    }

    public synchronized void notifySubscribeSuccess(String key, String response) {
        Set<IDataCallback> keyListeners = this.subscribeCallbacks.get(key);
        if (keyListeners != null) {
            for (IDataCallback callback : keyListeners) {
                callback.success(response);
            }
        }
    }

    public synchronized void notifyApiError(Object obj, ApiError error) {
        String key = ReflectUtil.getClassName(obj.getClass());
        Set<IDataCallback> keyListeners = this.subscribeCallbacks.get(key);
        if (keyListeners != null) {
            for (IDataCallback callback : keyListeners) {
                callback.error(error);
            }
        }
    }

    private void notifyStatusChanged(int errorCode, boolean isFinished) {
        LogUtils.d(PeanutConstants.TAG_SDK, "[PeanutSDK][notifyStatusChanged][" + errorCode + "][" + isFinished + "]");
        this.mSDKStatusCode = errorCode;
        if (this.mSDKStatusCode >= 203000 || isFinished) {
            if (this.mSDKStatusCode >= 203000 || (isFinished && this.mSDKStatusCode == SDK_INIT_SUCCESS)) {
                notifyError(errorCode);
            }
        }
    }

    public void notifyError(final int errorCode) {
        LogUtils.i(PeanutConstants.TAG_SDK, "[PeanutSDK][notifyError][" + errorCode + "]");
        Iterator<ErrorListener> it = this.mListeners.iterator();
        if (it != null) {
            while (it.hasNext()) {
                final ErrorListener listener = it.next();
                this.mainThreadExecutor.execute(new Runnable() { // from class: com.keenon.sdk.external.PeanutSDK.2
                    @Override // java.lang.Runnable
                    public void run() {
                        listener.onInit(errorCode);
                    }
                });
            }
        }
    }

    @Override // com.keenon.sdk.sensor.common.SensorObserver
    public void onUpdate(Event event, Sensor sensor) {
        LogUtils.i(PeanutConstants.TAG_SDK, "[PeanutSDK][onUpdate][" + sensor.name() + ", event" + event.toString() + "]");
    }
}
