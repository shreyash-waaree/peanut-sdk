package com.keenon.sdk.component.charger.common;

import com.keenon.common.constant.PeanutConstants;
import com.keenon.common.utils.LogUtils;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/component/charger/common/ChargerFsm.class */
public class ChargerFsm {
    private Listener listener;
    private StateEntity chargerState = StateEntity.CHARGE_STATE_IDLE;

    /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/component/charger/common/ChargerFsm$Listener.class */
    public interface Listener {
        void onStateSafely(int i);

        void onEventAction(int i);
    }

    void setState(StateEntity chargerState) {
        LogUtils.i(PeanutConstants.TAG_CHARGE, "[ChargerFsm][setState][charge state " + this.chargerState.name() + " ---> " + chargerState.name() + "]");
        this.chargerState = chargerState;
        this.chargerState.runTask(this);
    }

    public StateEntity getChargerState() {
        return this.chargerState;
    }

    public void process(int event) {
        LogUtils.i(PeanutConstants.TAG_CHARGE, "[ChargerFsm][process][charge event " + event + "]");
        this.chargerState.process(this, event);
    }

    public void addListener(Listener listener) {
        this.listener = listener;
    }

    public Listener getListener() {
        return this.listener;
    }
}
