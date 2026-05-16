package com.keenon.sdk.component.gating.callback;

import com.keenon.sdk.component.gating.data.Faults;
import com.keenon.sdk.component.gating.data.GatingType;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/component/gating/callback/DoorListener.class */
public interface DoorListener {
    void onFault(Faults faults, int i);

    void onStateChange(int i, int i2);

    void onTypeChange(GatingType gatingType);

    void onTypeSetting(boolean z);

    void onError(int i);
}
