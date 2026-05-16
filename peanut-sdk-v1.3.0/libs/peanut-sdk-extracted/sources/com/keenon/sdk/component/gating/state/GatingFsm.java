package com.keenon.sdk.component.gating.state;

import com.keenon.common.constant.PeanutConstants;
import com.keenon.common.utils.LogUtils;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/component/gating/state/GatingFsm.class */
public class GatingFsm {
    public int mGatingId;
    private GatingState mGatingState = GatingState.CLOSED;
    private GatingStateListener mListener;

    /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/component/gating/state/GatingFsm$GatingStateListener.class */
    public interface GatingStateListener {
        void onStateChange(int i, int i2);
    }

    public GatingFsm(int floorId) {
        this.mGatingId = floorId;
    }

    public GatingStateListener getListener() {
        return this.mListener;
    }

    public GatingState getGatingState() {
        return this.mGatingState;
    }

    void setGatingState(GatingState gatingState) {
        this.mGatingState.exit(this);
        LogUtils.d(PeanutConstants.TAG_DOOR, "[GatingFsm][setGatingState]:" + gatingState.toString());
        this.mGatingState = gatingState;
        this.mGatingState.entry(this);
    }

    public void process(GatingEvent event) {
        this.mGatingState.process(this, event);
    }

    public void setGatingStateListener(GatingStateListener listener) {
        this.mListener = listener;
    }
}
