package com.keenon.sdk.component.navigation.common;

import com.keenon.common.constant.PeanutConstants;
import com.keenon.common.utils.LogUtils;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/component/navigation/common/NavigationState.class */
public enum NavigationState {
    IDLE { // from class: com.keenon.sdk.component.navigation.common.NavigationState.1
        @Override // com.keenon.sdk.component.navigation.common.NavigationState
        void process(NavigationFsm fsm, int state) {
            switch (state) {
                case 1:
                    fsm.setState(PREPARED);
                    break;
                default:
                    na(fsm, state);
                    break;
            }
        }

        @Override // com.keenon.sdk.component.navigation.common.NavigationState
        void runTask(NavigationFsm fsm) {
            notifyStateChanged(fsm, 0);
        }
    },
    PREPARED { // from class: com.keenon.sdk.component.navigation.common.NavigationState.2
        @Override // com.keenon.sdk.component.navigation.common.NavigationState
        void process(NavigationFsm fsm, int state) {
            switch (state) {
                case 0:
                    fsm.setState(IDLE);
                    break;
                case 1:
                case 5:
                case 6:
                default:
                    na(fsm, state);
                    break;
                case 2:
                    fsm.setState(RUNNING);
                    break;
                case 3:
                    fsm.setState(DESTINATION);
                    break;
                case 4:
                    fsm.setState(PAUSE);
                    break;
                case 7:
                    fsm.setState(STOP);
                    break;
            }
        }

        @Override // com.keenon.sdk.component.navigation.common.NavigationState
        void runTask(NavigationFsm fsm) {
            notifyStateChanged(fsm, 1);
        }
    },
    RUNNING { // from class: com.keenon.sdk.component.navigation.common.NavigationState.3
        @Override // com.keenon.sdk.component.navigation.common.NavigationState
        void process(NavigationFsm fsm, int state) {
            switch (state) {
                case 3:
                    fsm.setState(DESTINATION);
                    break;
                case 4:
                    fsm.setState(PAUSE);
                    break;
                case 5:
                    fsm.setState(COLLISION);
                    break;
                case 6:
                    fsm.setState(BLOCKED);
                    break;
                case 7:
                    fsm.setState(STOP);
                    break;
                case 8:
                default:
                    na(fsm, state);
                    break;
                case 9:
                    fsm.setState(BLOCKING);
                    break;
            }
        }

        @Override // com.keenon.sdk.component.navigation.common.NavigationState
        void runTask(NavigationFsm fsm) {
            notifyStateChanged(fsm, 2);
        }
    },
    DESTINATION { // from class: com.keenon.sdk.component.navigation.common.NavigationState.4
        @Override // com.keenon.sdk.component.navigation.common.NavigationState
        void process(NavigationFsm fsm, int state) {
            switch (state) {
                case 0:
                    fsm.setState(IDLE);
                    break;
                default:
                    na(fsm, state);
                    break;
            }
        }

        @Override // com.keenon.sdk.component.navigation.common.NavigationState
        void runTask(NavigationFsm fsm) {
            notifyStateChanged(fsm, 3);
        }
    },
    BLOCKED { // from class: com.keenon.sdk.component.navigation.common.NavigationState.5
        @Override // com.keenon.sdk.component.navigation.common.NavigationState
        void process(NavigationFsm fsm, int state) {
            switch (state) {
                case 2:
                    fsm.setState(RUNNING);
                    break;
                case 3:
                    fsm.setState(DESTINATION);
                    break;
                case 4:
                    fsm.setState(PAUSE);
                    break;
                case 5:
                    fsm.setState(COLLISION);
                    break;
                case 6:
                case 8:
                default:
                    na(fsm, state);
                    break;
                case 7:
                    fsm.setState(STOP);
                    break;
                case 9:
                    fsm.setState(BLOCKING);
                    break;
            }
        }

        @Override // com.keenon.sdk.component.navigation.common.NavigationState
        void runTask(NavigationFsm fsm) {
            notifyStateChanged(fsm, 6);
        }
    },
    BLOCKING { // from class: com.keenon.sdk.component.navigation.common.NavigationState.6
        @Override // com.keenon.sdk.component.navigation.common.NavigationState
        void process(NavigationFsm fsm, int state) {
            switch (state) {
                case 2:
                    fsm.setState(RUNNING);
                    break;
                case 3:
                    fsm.setState(DESTINATION);
                    break;
                case 4:
                    fsm.setState(PAUSE);
                    break;
                case 5:
                    fsm.setState(COLLISION);
                    break;
                case 6:
                default:
                    na(fsm, state);
                    break;
                case 7:
                    fsm.setState(STOP);
                    break;
            }
        }

        @Override // com.keenon.sdk.component.navigation.common.NavigationState
        void runTask(NavigationFsm fsm) {
            notifyStateChanged(fsm, 9);
        }
    },
    COLLISION { // from class: com.keenon.sdk.component.navigation.common.NavigationState.7
        @Override // com.keenon.sdk.component.navigation.common.NavigationState
        void process(NavigationFsm fsm, int state) {
            switch (state) {
                case 2:
                    fsm.setState(RUNNING);
                    break;
                case 3:
                    fsm.setState(DESTINATION);
                    break;
                case 4:
                    fsm.setState(PAUSE);
                    break;
                case 5:
                case 8:
                default:
                    na(fsm, state);
                    break;
                case 6:
                    fsm.setState(BLOCKED);
                    break;
                case 7:
                    fsm.setState(STOP);
                    break;
                case 9:
                    fsm.setState(BLOCKING);
                    break;
            }
        }

        @Override // com.keenon.sdk.component.navigation.common.NavigationState
        void runTask(NavigationFsm fsm) {
            notifyStateChanged(fsm, 5);
        }
    },
    PAUSE { // from class: com.keenon.sdk.component.navigation.common.NavigationState.8
        @Override // com.keenon.sdk.component.navigation.common.NavigationState
        void process(NavigationFsm fsm, int state) {
            switch (state) {
                case 2:
                    fsm.setState(RUNNING);
                    break;
                case 3:
                    fsm.setState(DESTINATION);
                    break;
                case 7:
                    fsm.setState(STOP);
                    break;
                default:
                    na(fsm, state);
                    break;
            }
        }

        @Override // com.keenon.sdk.component.navigation.common.NavigationState
        void runTask(NavigationFsm fsm) {
            notifyStateChanged(fsm, 4);
        }
    },
    STOP { // from class: com.keenon.sdk.component.navigation.common.NavigationState.9
        @Override // com.keenon.sdk.component.navigation.common.NavigationState
        void process(NavigationFsm fsm, int state) {
            na(fsm, state);
        }

        @Override // com.keenon.sdk.component.navigation.common.NavigationState
        void runTask(NavigationFsm fsm) {
            notifyStateChanged(fsm, 7);
            fsm.setState(IDLE);
        }
    };

    abstract void process(NavigationFsm navigationFsm, int i);

    void runTask(NavigationFsm fsm) {
    }

    void na(NavigationFsm fsm, int event) {
        LogUtils.d(PeanutConstants.TAG_NAVI, "[NavigationState][na][ignore event " + event + " in state " + fsm.getState().name() + "]");
    }

    void notifyStateChanged(NavigationFsm fsm, int state) {
        if (fsm.getListener() != null) {
            fsm.getListener().onStateSafely(state);
        }
    }
}
