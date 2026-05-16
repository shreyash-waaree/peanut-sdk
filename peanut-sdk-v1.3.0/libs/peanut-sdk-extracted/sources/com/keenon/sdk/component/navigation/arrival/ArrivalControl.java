package com.keenon.sdk.component.navigation.arrival;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/component/navigation/arrival/ArrivalControl.class */
public interface ArrivalControl {
    public static final float DEFAULT_MARGIN_SCOPE = 1.0f;
    public static final int DEFAULT_BLOCK_DELAY = 5000;
    public static final int DEFAULT_SCOPE_DELAY = 10000;

    /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/component/navigation/arrival/ArrivalControl$Listener.class */
    public interface Listener {
        void onArrived();
    }

    void addListener(Listener listener);

    void removeListener(Listener listener);

    void enable(boolean z);

    void notifyDistanceChanged(float f);

    void notifyBlocked();

    void release();
}
