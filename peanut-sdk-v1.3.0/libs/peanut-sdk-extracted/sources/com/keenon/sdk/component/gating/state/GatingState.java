package com.keenon.sdk.component.gating.state;

import com.keenon.common.constant.PeanutConstants;
import com.keenon.common.utils.LogUtils;
import com.keenon.sdk.component.gating.manager.Door;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/component/gating/state/GatingState.class */
public enum GatingState {
    OPENED { // from class: com.keenon.sdk.component.gating.state.GatingState.1
        @Override // com.keenon.sdk.component.gating.state.GatingState
        public void process(GatingFsm fsm, GatingEvent event) {
            switch (AnonymousClass6.$SwitchMap$com$keenon$sdk$component$gating$state$GatingEvent[event.ordinal()]) {
                case 1:
                    fsm.setGatingState(OPENING);
                    break;
                case 2:
                    fsm.setGatingState(CLOSING);
                    break;
                case 3:
                    na(fsm, this, event);
                    break;
                case 4:
                    fsm.setGatingState(EXCEPTION);
                    break;
            }
        }

        @Override // com.keenon.sdk.component.gating.state.GatingState
        public void entry(GatingFsm fsm) {
            notifyStateChange(fsm, Door.STATE_OPENED);
        }
    },
    OPENING { // from class: com.keenon.sdk.component.gating.state.GatingState.2
        @Override // com.keenon.sdk.component.gating.state.GatingState
        public void process(GatingFsm fsm, GatingEvent event) {
            switch (AnonymousClass6.$SwitchMap$com$keenon$sdk$component$gating$state$GatingEvent[event.ordinal()]) {
                case 1:
                    na(fsm, this, event);
                    break;
                case 2:
                    fsm.setGatingState(CLOSING);
                    break;
                case 3:
                    fsm.setGatingState(OPENED);
                    break;
                case 4:
                    fsm.setGatingState(EXCEPTION);
                    break;
            }
        }

        @Override // com.keenon.sdk.component.gating.state.GatingState
        public void entry(GatingFsm fsm) {
            notifyStateChange(fsm, Door.STATE_OPENING);
        }
    },
    CLOSING { // from class: com.keenon.sdk.component.gating.state.GatingState.3
        @Override // com.keenon.sdk.component.gating.state.GatingState
        public void process(GatingFsm fsm, GatingEvent event) {
            switch (AnonymousClass6.$SwitchMap$com$keenon$sdk$component$gating$state$GatingEvent[event.ordinal()]) {
                case 1:
                    fsm.setGatingState(OPENING);
                    break;
                case 2:
                    na(fsm, this, event);
                    break;
                case 3:
                    fsm.setGatingState(CLOSED);
                    break;
                case 4:
                    fsm.setGatingState(EXCEPTION);
                    break;
            }
        }

        @Override // com.keenon.sdk.component.gating.state.GatingState
        public void entry(GatingFsm fsm) {
            notifyStateChange(fsm, Door.STATE_CLOSING);
        }
    },
    CLOSED { // from class: com.keenon.sdk.component.gating.state.GatingState.4
        @Override // com.keenon.sdk.component.gating.state.GatingState
        public void process(GatingFsm fsm, GatingEvent event) {
            switch (AnonymousClass6.$SwitchMap$com$keenon$sdk$component$gating$state$GatingEvent[event.ordinal()]) {
                case 1:
                    fsm.setGatingState(OPENING);
                    break;
                case 2:
                    fsm.setGatingState(CLOSING);
                    break;
                case 3:
                    na(fsm, this, event);
                    break;
                case 4:
                    fsm.setGatingState(EXCEPTION);
                    break;
            }
        }

        @Override // com.keenon.sdk.component.gating.state.GatingState
        public void entry(GatingFsm fsm) {
            notifyStateChange(fsm, Door.STATE_CLOSED);
        }
    },
    EXCEPTION { // from class: com.keenon.sdk.component.gating.state.GatingState.5
        @Override // com.keenon.sdk.component.gating.state.GatingState
        public void process(GatingFsm fsm, GatingEvent event) {
        }

        @Override // com.keenon.sdk.component.gating.state.GatingState
        public void entry(GatingFsm fsm) {
            notifyStateChange(fsm, Door.STATE_EXCEPTION);
        }
    };

    public abstract void process(GatingFsm gatingFsm, GatingEvent gatingEvent);

    /* JADX INFO: renamed from: com.keenon.sdk.component.gating.state.GatingState$6, reason: invalid class name */
    /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/component/gating/state/GatingState$6.class */
    static /* synthetic */ class AnonymousClass6 {
        static final /* synthetic */ int[] $SwitchMap$com$keenon$sdk$component$gating$state$GatingEvent = new int[GatingEvent.values().length];

        static {
            try {
                $SwitchMap$com$keenon$sdk$component$gating$state$GatingEvent[GatingEvent.OPEN_EVENT.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$com$keenon$sdk$component$gating$state$GatingEvent[GatingEvent.CLOSE_EVENT.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                $SwitchMap$com$keenon$sdk$component$gating$state$GatingEvent[GatingEvent.SUCCESS_EVENT.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
            try {
                $SwitchMap$com$keenon$sdk$component$gating$state$GatingEvent[GatingEvent.EXCEPTION_EVENT.ordinal()] = 4;
            } catch (NoSuchFieldError e4) {
            }
        }
    }

    public void entry(GatingFsm fsm) {
    }

    void exit(GatingFsm fsm) {
        LogUtils.d(PeanutConstants.TAG_DOOR, "[GatingState][exit]" + fsm.mGatingId + "exit current state");
    }

    void na(GatingFsm fsm, GatingState floorState, GatingEvent event) {
        LogUtils.d(PeanutConstants.TAG_DOOR, "[GatingState][na]gatingId:" + fsm.mGatingId + " ignore " + event + "  in state " + floorState);
    }

    void notifyStateChange(GatingFsm fsm, int state) {
        if (fsm.getListener() != null) {
            fsm.getListener().onStateChange(fsm.mGatingId, state);
        }
    }
}
