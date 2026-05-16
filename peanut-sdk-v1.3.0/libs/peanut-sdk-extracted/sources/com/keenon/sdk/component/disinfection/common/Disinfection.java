package com.keenon.sdk.component.disinfection.common;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/component/disinfection/common/Disinfection.class */
public interface Disinfection {
    public static final int STATE_IDLE = 0;
    public static final int STATE_OPEN_ATOMIZER = 1;
    public static final int STATE_OPEN_ULTRAVIOLET = 2;
    public static final int STATE_OPEN_ALL = 3;
    public static final int STATE_CLOSE_ATOMIZER = 4;
    public static final int STATE_CLOSE_ULTRAVIOLET = 5;
    public static final int STATE_LOW_ATOMIZER = 6;
    public static final int STATE_LOW_ATOMIZER_UV = 7;
    public static final int STATE_TANK_ERROR = 0;
    public static final int STATE_TANK_NORMAL = 1;
    public static final int STATE_TANK_FULL = 2;
    public static final int STATE_TANK_EMPTY = 3;

    /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/component/disinfection/common/Disinfection$Listener.class */
    public interface Listener {
        void onStateChanged(int i);

        void onInfoChanged(DisinfectInfo disinfectInfo);

        void onError(int i);
    }

    void addListener(Listener listener);

    void removeListener(Listener listener);

    void start();

    void stop();

    void release();

    void enableCabin(boolean z);

    void enableDrainage(boolean z);

    void enableAtomizer(boolean z);

    void enableUltraviolet(boolean z);

    void setFanSpeed(int i);

    boolean isLowAtomizer();

    boolean isAtomizerEnable();

    boolean isUltravioletEnable();

    DisinfectInfo getInfo();

    int getState();

    int getTankState();
}
