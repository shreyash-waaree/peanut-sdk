package com.keenon.sdk.component.charger.common;

import com.keenon.sdk.component.charger.ChargerImpl;
import com.keenon.sdk.component.charger.PeanutCharger;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/component/charger/common/Charger.class */
public interface Charger {
    public static final int STATE_CHARGE_IDLE = 1;
    public static final int STATE_CHARGE_AUTO_GOING_TO_CHARGE = 2;
    public static final int STATE_CHARGE_MANUAL_GOING_TO_CHARGE = 3;
    public static final int STATE_CHARGE_IN_CHARGING = 4;
    public static final int STATE_ADAPTER_CHARGE_IN_CHARGING = 5;
    public static final int STATE_CANCELING_CHARGING = 6;

    /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/component/charger/common/Charger$Listener.class */
    public interface Listener {
        void onChargerInfoChanged(int i, ChargerInfo chargerInfo);

        void onChargerStatusChanged(int i);

        void onError(int i);
    }

    void addListener(Listener listener);

    void removeListener(Listener listener);

    void execute(PeanutCharger peanutCharger);

    void release();

    void autoCharge();

    void manualCharge();

    void cancelCharge();

    void adapterCharge();

    void perform(int i);

    /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/component/charger/common/Charger$Factory.class */
    public static final class Factory {
        private Factory() {
        }

        public static Charger newInstance() {
            return ChargerImpl.getInstance();
        }
    }
}
