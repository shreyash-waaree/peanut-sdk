package com.keenon.common.log;

import android.content.Context;
import com.umeng.analytics.MobclickAgent;
import com.umeng.commonsdk.UMConfigure;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/common/log/PeanutStatistics.class */
public class PeanutStatistics {
    public static final String SDK_EVENT_ROBOT_SDK_APIS = "robot_sdk_apis";
    public static final String SDK_EVENT_ROBOT_SDK_INIT = "robot_sdk_init";
    public static final String[] paths = {"/rk/ip", "/runtime/odo", "/charge/status", "/runtime/heartbeat"};
    private static volatile PeanutStatistics sInstance;
    private static Context mContext;
    private Listener mListener;
    private boolean isUmLogEnable;

    /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/common/log/PeanutStatistics$Listener.class */
    interface Listener {
        void onEvent(String str, Map<String, String> map);
    }

    public static PeanutStatistics getInstance() {
        if (sInstance == null) {
            synchronized (PeanutStatistics.class) {
                if (sInstance == null) {
                    sInstance = new PeanutStatistics();
                }
            }
        }
        return sInstance;
    }

    public List<String> getPaths() {
        return Arrays.asList(paths);
    }

    public void setListener(Listener listener) {
        this.mListener = listener;
    }

    public void onEvent(String event, Map<String, String> map) {
        if (this.mListener != null) {
            this.mListener.onEvent(event, map);
        }
    }

    public void onEventSend(String event, Map<String, String> map) {
        if (this.isUmLogEnable) {
            MobclickAgent.onEvent(mContext, event, map);
        }
    }

    public void init(Context context, boolean enable) {
        mContext = context.getApplicationContext();
        this.isUmLogEnable = enable;
        UMConfigure.init(mContext, "5f6c6d6a46549c54f0b743ae", "umeng", 1, (String) null);
        UMConfigure.setProcessEvent(true);
        UMConfigure.setLogEnabled(true);
        onInitEvent();
    }

    private void onInitEvent() {
        HashMap<String, String> map = new HashMap<>();
        map.put("sdk_channel", mContext.getPackageName());
        onEventSend(SDK_EVENT_ROBOT_SDK_INIT, map);
    }
}
