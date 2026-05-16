package com.keenon.sdk.component.disinfection.common;

import com.keenon.common.constant.PeanutConstants;
import com.keenon.common.utils.LogUtils;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/component/disinfection/common/DisinfectFsm.class */
public class DisinfectFsm {
    private Listener listener;
    private DisinfectState state = DisinfectState.S0_DISINFECT_STATE_IDLE;

    /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/component/disinfection/common/DisinfectFsm$Listener.class */
    public interface Listener {
        void onStateSafely(int i);
    }

    public DisinfectState getState() {
        return this.state;
    }

    public void setState(DisinfectState state) {
        LogUtils.d(PeanutConstants.TAG_DISINFECT, "[DisinfectFsm][setState][navigation state " + this.state.name() + " ---> " + state.name() + "]");
        this.state = state;
        this.state.entry(this);
    }

    public void addListener(Listener listener) {
        this.listener = listener;
    }

    public Listener getListener() {
        return this.listener;
    }

    public void process(int event) {
        this.state.process(this, event);
    }
}
