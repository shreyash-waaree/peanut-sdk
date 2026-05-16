package com.keenon.sdk.component.charger.common;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/component/charger/common/StateEntity.class */
public enum StateEntity {
    CHARGE_STATE_IDLE { // from class: com.keenon.sdk.component.charger.common.StateEntity.1
        @Override // com.keenon.sdk.component.charger.common.StateEntity
        public void process(ChargerFsm fsm, int event) {
            switch (event) {
                case 1:
                    notifyEventAction(fsm, event);
                    fsm.setState(CHARGE_STATE_GOING_TO_CHARGE);
                    break;
                case 2:
                    notifyEventAction(fsm, event);
                    fsm.setState(CHARGE_STATE_MANUAL_GOING_TO_CHARGE);
                    break;
                case 3:
                    fsm.setState(ADAPTER_CHARGE_STATE_CHARGING);
                    break;
                case 4:
                    fsm.setState(CHARGE_STATE_IDLE);
                    notifyEventAction(fsm, event);
                    break;
                case 5:
                    fsm.setState(CHARGE_STATE_CHARGING);
                    break;
                default:
                    na(fsm, event);
                    break;
            }
        }

        @Override // com.keenon.sdk.component.charger.common.StateEntity
        void runTask(ChargerFsm fsm) {
            notifyStateChanged(fsm, 1);
        }
    },
    CHARGE_STATE_GOING_TO_CHARGE { // from class: com.keenon.sdk.component.charger.common.StateEntity.2
        @Override // com.keenon.sdk.component.charger.common.StateEntity
        public void process(ChargerFsm fsm, int event) {
            switch (event) {
                case 2:
                    notifyEventAction(fsm, event);
                    fsm.setState(CHARGE_STATE_MANUAL_GOING_TO_CHARGE);
                    break;
                case 3:
                    fsm.setState(ADAPTER_CHARGE_STATE_CHARGING);
                    break;
                case 4:
                    notifyEventAction(fsm, event);
                    fsm.setState(CHARGE_STATE_CANCELING_CHARGING);
                    break;
                case 5:
                    fsm.setState(CHARGE_STATE_CHARGING);
                    break;
                case 6:
                    notifyEventAction(fsm, event);
                    fsm.setState(CHARGE_STATE_IDLE);
                    break;
                default:
                    na(fsm, event);
                    break;
            }
        }

        @Override // com.keenon.sdk.component.charger.common.StateEntity
        public void runTask(ChargerFsm fsm) {
            notifyStateChanged(fsm, 2);
        }
    },
    CHARGE_STATE_MANUAL_GOING_TO_CHARGE { // from class: com.keenon.sdk.component.charger.common.StateEntity.3
        @Override // com.keenon.sdk.component.charger.common.StateEntity
        public void process(ChargerFsm fsm, int event) {
            switch (event) {
                case 1:
                    notifyEventAction(fsm, event);
                    fsm.setState(CHARGE_STATE_GOING_TO_CHARGE);
                    break;
                case 2:
                default:
                    na(fsm, event);
                    break;
                case 3:
                    fsm.setState(ADAPTER_CHARGE_STATE_CHARGING);
                    break;
                case 4:
                    notifyEventAction(fsm, event);
                    fsm.setState(CHARGE_STATE_CANCELING_CHARGING);
                    break;
                case 5:
                    fsm.setState(CHARGE_STATE_CHARGING);
                    break;
            }
        }

        @Override // com.keenon.sdk.component.charger.common.StateEntity
        public void runTask(ChargerFsm fsm) {
            notifyStateChanged(fsm, 3);
        }
    },
    CHARGE_STATE_CHARGING { // from class: com.keenon.sdk.component.charger.common.StateEntity.4
        @Override // com.keenon.sdk.component.charger.common.StateEntity
        void process(ChargerFsm fsm, int event) {
            switch (event) {
                case 1:
                case 7:
                    notifyEventAction(fsm, event);
                    fsm.setState(CHARGE_STATE_GOING_TO_CHARGE);
                    break;
                case 2:
                    notifyEventAction(fsm, event);
                    fsm.setState(CHARGE_STATE_MANUAL_GOING_TO_CHARGE);
                    break;
                case 3:
                case 5:
                case 6:
                default:
                    na(fsm, event);
                    break;
                case 4:
                    notifyEventAction(fsm, event);
                    fsm.setState(CHARGE_STATE_CANCELING_CHARGING);
                    break;
            }
        }

        @Override // com.keenon.sdk.component.charger.common.StateEntity
        void runTask(ChargerFsm fsm) {
            notifyStateChanged(fsm, 4);
        }
    },
    CHARGE_STATE_CANCELING_CHARGING { // from class: com.keenon.sdk.component.charger.common.StateEntity.5
        @Override // com.keenon.sdk.component.charger.common.StateEntity
        void process(ChargerFsm fsm, int event) {
            switch (event) {
                case 9:
                    notifyEventAction(fsm, event);
                    fsm.setState(CHARGE_STATE_IDLE);
                    break;
            }
        }

        @Override // com.keenon.sdk.component.charger.common.StateEntity
        void runTask(ChargerFsm fsm) {
            notifyStateChanged(fsm, 6);
        }
    },
    ADAPTER_CHARGE_STATE_CHARGING { // from class: com.keenon.sdk.component.charger.common.StateEntity.6
        @Override // com.keenon.sdk.component.charger.common.StateEntity
        void process(ChargerFsm fsm, int event) {
            switch (event) {
                case 4:
                case 6:
                case 7:
                case 8:
                    notifyEventAction(fsm, event);
                    fsm.setState(CHARGE_STATE_IDLE);
                    break;
                case 5:
                default:
                    na(fsm, event);
                    break;
            }
        }

        @Override // com.keenon.sdk.component.charger.common.StateEntity
        void runTask(ChargerFsm fsm) {
            notifyStateChanged(fsm, 5);
        }
    };

    abstract void process(ChargerFsm chargerFsm, int i);

    void runTask(ChargerFsm fsm) {
    }

    void na(ChargerFsm fsm, int event) {
    }

    void notifyStateChanged(ChargerFsm fsm, int state) {
        if (fsm.getListener() != null) {
            fsm.getListener().onStateSafely(state);
        }
    }

    void notifyEventAction(ChargerFsm fsm, int event) {
        if (fsm.getListener() != null) {
            fsm.getListener().onEventAction(event);
        }
    }
}
