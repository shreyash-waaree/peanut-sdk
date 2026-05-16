package com.keenon.sdk.component;

import android.text.TextUtils;
import com.keenon.common.constant.PeanutConstants;
import com.keenon.common.utils.LogUtils;
import com.keenon.sdk.external.PeanutSDK;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/component/ComponentManager.class */
public class ComponentManager {
    private static final String TAG = "[ComponentManager]";
    private static volatile ComponentManager sInstance;
    private volatile HashMap<String, Component> mComponentMap = new HashMap<>();

    private ComponentManager() {
    }

    public static ComponentManager getInstance() {
        if (sInstance == null) {
            synchronized (ComponentManager.class) {
                if (sInstance == null) {
                    sInstance = new ComponentManager();
                }
            }
        }
        return sInstance;
    }

    public boolean registerComponent(Component component) {
        boolean isSuccess = false;
        if (this.mComponentMap != null && !this.mComponentMap.containsKey(component.getComponentName()) && component != null) {
            this.mComponentMap.put(component.getComponentName(), component);
            isSuccess = true;
        }
        return isSuccess;
    }

    public void unregisterComponent(String componentName) {
        if (!TextUtils.isEmpty(componentName) && this.mComponentMap != null && this.mComponentMap.containsKey(componentName)) {
            this.mComponentMap.remove(componentName);
        }
    }

    public Component getComponent(String componentName) {
        Component component = null;
        if (!TextUtils.isEmpty(componentName) && this.mComponentMap != null && this.mComponentMap.containsKey(componentName)) {
            component = this.mComponentMap.get(componentName);
        }
        return component;
    }

    public boolean isFinishInit() {
        boolean isFinish = true;
        if (this.mComponentMap != null) {
            Iterator<Map.Entry<String, Component>> it = this.mComponentMap.entrySet().iterator();
            while (true) {
                if (!it.hasNext()) {
                    break;
                }
                Map.Entry<String, Component> entry = it.next();
                Component component = entry.getValue();
                if (component != null && PeanutSDK.SDK_STATUS_DEFAULT == component.getStatusCode()) {
                    isFinish = false;
                    break;
                }
            }
        } else {
            isFinish = false;
        }
        return isFinish;
    }

    public void release() {
        LogUtils.i(PeanutConstants.TAG_SDK, "[ComponentManager][release]");
        if (this.mComponentMap != null) {
            Iterator<Map.Entry<String, Component>> it = this.mComponentMap.entrySet().iterator();
            while (it.hasNext()) {
                try {
                    Map.Entry<String, Component> entry = it.next();
                    Component component = entry.getValue();
                    if (component != null && PeanutSDK.SDK_INIT_SUCCESS != component.getStatusCode()) {
                        component.release();
                        it.remove();
                    }
                } catch (Exception e) {
                    LogUtils.e(PeanutConstants.TAG_SDK, TAG, e);
                }
            }
            this.mComponentMap.clear();
        }
    }
}
