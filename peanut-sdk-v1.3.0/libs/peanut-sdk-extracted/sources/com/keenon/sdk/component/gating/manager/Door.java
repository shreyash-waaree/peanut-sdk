package com.keenon.sdk.component.gating.manager;

import android.content.Context;
import com.keenon.sdk.api.door.DoorVersionCompatApi;
import com.keenon.sdk.component.gating.callback.DoorListener;
import com.keenon.sdk.component.gating.state.GatingState;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/component/gating/manager/Door.class */
public interface Door {
    public static final int SET_TYPE_FOUR = 1;
    public static final int SET_TYPE_DOUBLE = 2;
    public static final int SET_TYPE_THREE = 3;
    public static final int SET_TYPE_THREE_REV = 4;
    public static final int SET_TYPE_AUTO = 5;
    public static final int STATE = 200;
    public static final int STATE_OPENING = 201;
    public static final int STATE_OPENED = 202;
    public static final int STATE_CLOSING = 203;
    public static final int STATE_CLOSED = 204;
    public static final int STATE_EXCEPTION = 205;

    /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/component/gating/manager/Door$DoorFault.class */
    public enum DoorFault {
        CLOSING,
        OPENING
    }

    void init(Context context);

    void setDoorListerner(String str, DoorListener doorListener);

    void removeFloorListener(String str);

    int getDoorType();

    GatingState getDoorState(int i);

    void openDoor(int i, boolean z);

    void closeDoor(int i);

    void closeAllDoor();

    boolean isAllDoorClose();

    void setDoorType(int i, String str, DoorListener doorListener);

    DoorVersionCompatApi.VersionInfo getDoorVersion();

    boolean supportDoorTypeSetting();

    void release();
}
