package com.keenon.sdk.component.navigation.common;

import com.keenon.common.constant.PeanutConstants;
import com.keenon.common.utils.LogUtils;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/component/navigation/common/NavigationFsm.class */
public class NavigationFsm {
    private static final long INTERVAL_TIME = 2000;
    private NavigationState state = NavigationState.IDLE;
    private Listener listener;
    private int event;
    private long setEventTime;

    /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/component/navigation/common/NavigationFsm$Listener.class */
    public interface Listener {
        void onStateSafely(int i);
    }

    public NavigationState getState() {
        return this.state;
    }

    protected void setState(NavigationState state) {
        LogUtils.d(PeanutConstants.TAG_NAVI, "[NavigationFsm][setState][navigation state " + this.state.name() + " ---> " + state.name() + "]");
        this.state = state;
        state.runTask(this);
    }

    public void addListener(Listener listener) {
        this.listener = listener;
    }

    public Listener getListener() {
        return this.listener;
    }

    public void setEvent(int event) {
        this.event = event;
    }

    public void process(int event) {
        if (isSameEvent(event, 6) || isSameEvent(event, 5)) {
            return;
        }
        LogUtils.d(PeanutConstants.TAG_NAVI, "[NavigationFsm][process][navigation event " + event + "]");
        this.state.process(this, event);
        setEvent(event);
    }

    public void release() {
        this.state = NavigationState.IDLE;
    }

    private boolean isSameEvent(int entryEvent, int specificEvent) {
        if (entryEvent == specificEvent) {
            long time = System.currentTimeMillis();
            return entryEvent == this.event && time - this.setEventTime >= INTERVAL_TIME;
        }
        this.setEventTime = System.currentTimeMillis();
        return false;
    }
}
