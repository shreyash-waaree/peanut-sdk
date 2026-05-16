package com.keenon.sdk.component.charger;

import com.keenon.sdk.component.charger.common.Charger;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/component/charger/PeanutCharger.class */
public class PeanutCharger {
    public static final int CHARGE_ACTION_AUTO = 1;
    public static final int CHARGE_ACTION_MANUAL = 2;
    public static final int CHARGE_ACTION_ADAPTER = 3;
    public static final int CHARGE_ACTION_STOP = 4;
    private int pile;
    private String TAG = "ChargerTask";
    private Charger charger = Charger.Factory.newInstance();

    PeanutCharger(int pile, Charger.Listener listener) {
        this.charger.addListener(listener);
        this.pile = pile;
    }

    public void setPile(int pile) {
        this.pile = pile;
    }

    public int getPile() {
        return this.pile;
    }

    public void execute() {
        this.charger.execute(this);
    }

    public void performAction(int action) {
        this.charger.perform(action);
    }

    public void release() {
        this.charger.release();
    }

    /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/component/charger/PeanutCharger$Builder.class */
    public static class Builder {
        private int pile;
        private Charger.Listener listener;

        public Builder setPile(int pile) {
            this.pile = pile;
            return this;
        }

        public Builder setListener(Charger.Listener listener) {
            this.listener = listener;
            return this;
        }

        public PeanutCharger build() {
            return new PeanutCharger(this.pile, this.listener);
        }
    }
}
