package com.keenon.sdk.sensor.ota.base;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/sensor/ota/base/IOta.class */
public interface IOta {
    public static final int STATE_ERROR = -1;
    public static final int STATE_IDLE = 0;
    public static final int STATE_PREPARED = 1;
    public static final int STATE_UPGRADING = 2;
    public static final int STATE_CANCELING = 3;
    public static final int STATE_CANCELED = 4;
    public static final int STATE_COMPLETE = 5;

    /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/sensor/ota/base/IOta$Listener.class */
    public interface Listener {
        void onProgress(int i);

        void onState(int i);

        void onError(int i);

        void onNewVersion(String str);
    }

    void start(OTAConfig oTAConfig, Listener listener);

    void stop();

    OTAConfig getConfig();

    void release();
}
