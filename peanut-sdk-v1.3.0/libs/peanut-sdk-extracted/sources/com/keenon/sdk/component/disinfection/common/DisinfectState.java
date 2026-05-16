package com.keenon.sdk.component.disinfection.common;

import com.keenon.common.constant.PeanutConstants;
import com.keenon.common.utils.LogUtils;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/component/disinfection/common/DisinfectState.class */
public enum DisinfectState {
    S0_DISINFECT_STATE_IDLE { // from class: com.keenon.sdk.component.disinfection.common.DisinfectState.1
        @Override // com.keenon.sdk.component.disinfection.common.DisinfectState
        void process(DisinfectFsm fsm, int event) {
            switch (event) {
                case 1:
                    fsm.setState(S1_DISINFECT_STATE_ATOMIZER);
                    break;
                case 2:
                    fsm.setState(S2_DISINFECT_STATE_ULTRAVIOLET);
                    break;
                case 3:
                    fsm.setState(S3_DISINFECT_STATE_ALL);
                    break;
                case 4:
                case 5:
                default:
                    na(fsm, event);
                    break;
                case 6:
                    fsm.setState(S4_DISINFECT_STATE_ERROR_AT);
                    break;
            }
        }

        @Override // com.keenon.sdk.component.disinfection.common.DisinfectState
        void entry(DisinfectFsm fsm) {
            notifyStateChanged(fsm, 0);
        }
    },
    S1_DISINFECT_STATE_ATOMIZER { // from class: com.keenon.sdk.component.disinfection.common.DisinfectState.2
        @Override // com.keenon.sdk.component.disinfection.common.DisinfectState
        void process(DisinfectFsm fsm, int event) {
            switch (event) {
                case 0:
                case 4:
                    fsm.setState(S0_DISINFECT_STATE_IDLE);
                    break;
                case 1:
                case 3:
                case 5:
                default:
                    na(fsm, event);
                    break;
                case 2:
                    fsm.setState(S3_DISINFECT_STATE_ALL);
                    break;
                case 6:
                    fsm.setState(S4_DISINFECT_STATE_ERROR_AT);
                    break;
            }
        }

        @Override // com.keenon.sdk.component.disinfection.common.DisinfectState
        void entry(DisinfectFsm fsm) {
            notifyStateChanged(fsm, 1);
        }
    },
    S2_DISINFECT_STATE_ULTRAVIOLET { // from class: com.keenon.sdk.component.disinfection.common.DisinfectState.3
        @Override // com.keenon.sdk.component.disinfection.common.DisinfectState
        void process(DisinfectFsm fsm, int event) {
            switch (event) {
                case 0:
                case 5:
                    fsm.setState(S0_DISINFECT_STATE_IDLE);
                    break;
                case 1:
                    fsm.setState(S3_DISINFECT_STATE_ALL);
                    break;
                case 2:
                case 3:
                case 4:
                default:
                    na(fsm, event);
                    break;
                case 6:
                    fsm.setState(S5_DISINFECT_STATE_ERROR_UV);
                    break;
            }
        }

        @Override // com.keenon.sdk.component.disinfection.common.DisinfectState
        void entry(DisinfectFsm fsm) {
            notifyStateChanged(fsm, 2);
        }
    },
    S3_DISINFECT_STATE_ALL { // from class: com.keenon.sdk.component.disinfection.common.DisinfectState.4
        @Override // com.keenon.sdk.component.disinfection.common.DisinfectState
        void process(DisinfectFsm fsm, int event) {
            switch (event) {
                case 0:
                    fsm.setState(S0_DISINFECT_STATE_IDLE);
                    break;
                case 1:
                case 2:
                case 3:
                default:
                    na(fsm, event);
                    break;
                case 4:
                    fsm.setState(S2_DISINFECT_STATE_ULTRAVIOLET);
                    break;
                case 5:
                    fsm.setState(S1_DISINFECT_STATE_ATOMIZER);
                    break;
                case 6:
                    fsm.setState(S5_DISINFECT_STATE_ERROR_UV);
                    break;
            }
        }

        @Override // com.keenon.sdk.component.disinfection.common.DisinfectState
        void entry(DisinfectFsm fsm) {
            notifyStateChanged(fsm, 3);
        }
    },
    S4_DISINFECT_STATE_ERROR_AT { // from class: com.keenon.sdk.component.disinfection.common.DisinfectState.5
        @Override // com.keenon.sdk.component.disinfection.common.DisinfectState
        void process(DisinfectFsm fsm, int event) {
            switch (event) {
                case 2:
                case 3:
                    fsm.setState(S5_DISINFECT_STATE_ERROR_UV);
                    break;
                case 7:
                    fsm.setState(S0_DISINFECT_STATE_IDLE);
                    break;
                default:
                    na(fsm, event);
                    break;
            }
        }

        @Override // com.keenon.sdk.component.disinfection.common.DisinfectState
        void entry(DisinfectFsm fsm) {
            notifyStateChanged(fsm, 6);
        }
    },
    S5_DISINFECT_STATE_ERROR_UV { // from class: com.keenon.sdk.component.disinfection.common.DisinfectState.6
        @Override // com.keenon.sdk.component.disinfection.common.DisinfectState
        void process(DisinfectFsm fsm, int event) {
            switch (event) {
                case 0:
                case 5:
                    fsm.setState(S4_DISINFECT_STATE_ERROR_AT);
                    break;
                case 7:
                    fsm.setState(S2_DISINFECT_STATE_ULTRAVIOLET);
                    break;
                default:
                    na(fsm, event);
                    break;
            }
        }

        @Override // com.keenon.sdk.component.disinfection.common.DisinfectState
        void entry(DisinfectFsm fsm) {
            notifyStateChanged(fsm, 7);
        }
    };

    abstract void process(DisinfectFsm disinfectFsm, int i);

    void entry(DisinfectFsm fsm) {
    }

    void na(DisinfectFsm fsm, int event) {
        LogUtils.d(PeanutConstants.TAG_DISINFECT, "[DisinfectState][na][ignore event " + event + " in state " + fsm.getState().name() + "]");
    }

    void notifyStateChanged(DisinfectFsm fsm, int state) {
        if (fsm.getListener() != null) {
            fsm.getListener().onStateSafely(state);
        }
    }
}
