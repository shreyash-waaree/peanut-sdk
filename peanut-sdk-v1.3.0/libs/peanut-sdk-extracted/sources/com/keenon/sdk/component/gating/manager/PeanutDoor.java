package com.keenon.sdk.component.gating.manager;

import android.content.Context;
import androidx.annotation.NonNull;
import com.keenon.sdk.component.gating.callback.DoorListener;
import com.keenon.sdk.component.gating.state.GatingState;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/component/gating/manager/PeanutDoor.class */
public class PeanutDoor {
    private static PeanutDoor mInstance;
    private Door mPeanutDoor = PeanutDoorControl.getInstance();

    private PeanutDoor() {
    }

    public static PeanutDoor getInstance() {
        if (mInstance == null) {
            mInstance = new PeanutDoor();
        }
        return mInstance;
    }

    public void init(Context context) {
        this.mPeanutDoor.init(context);
    }

    public void setDoorListerner(@NonNull String tag, @NonNull DoorListener gatingFaultListener) {
        this.mPeanutDoor.setDoorListerner(tag, gatingFaultListener);
    }

    public void removeFloorListener(@NonNull String tag) {
        this.mPeanutDoor.removeFloorListener(tag);
    }

    public int getDoorType() {
        return this.mPeanutDoor.getDoorType();
    }

    public GatingState getDoorState(int doorId) {
        return this.mPeanutDoor.getDoorState(doorId);
    }

    public void openDoor(int doorId, boolean single) {
        this.mPeanutDoor.openDoor(doorId, single);
    }

    public void closeDoor(int doorId) {
        this.mPeanutDoor.closeDoor(doorId);
    }

    public void closeAllDoor() {
        this.mPeanutDoor.closeAllDoor();
    }

    public boolean isAllDoorClose() {
        return this.mPeanutDoor.isAllDoorClose();
    }

    public void setDoorType(int gatingType, String tag, DoorListener doorListener) {
        this.mPeanutDoor.setDoorType(gatingType, tag, doorListener);
    }

    public String getDoorVersion() {
        return this.mPeanutDoor.getDoorVersion().getSw();
    }

    public boolean supportDoorTypeSetting() {
        return this.mPeanutDoor.supportDoorTypeSetting();
    }

    public void release() {
        this.mPeanutDoor.release();
    }
}
