package com.keenon.sdk.component.gating.manager;

import android.content.Context;
import android.os.Handler;
import androidx.annotation.NonNull;
import com.keenon.common.constant.PeanutConstants;
import com.keenon.common.error.PeanutError;
import com.keenon.common.utils.GsonUtil;
import com.keenon.common.utils.LogUtils;
import com.keenon.common.utils.StringUtils;
import com.keenon.sdk.api.DoorCloseApi;
import com.keenon.sdk.api.DoorFaultApi;
import com.keenon.sdk.api.DoorLimitFaultApi;
import com.keenon.sdk.api.DoorOpenApi;
import com.keenon.sdk.api.DoorStatusApi;
import com.keenon.sdk.api.DoorStatusObserveApi;
import com.keenon.sdk.api.DoorTypeApi;
import com.keenon.sdk.api.DoorTypeAutoSetApi;
import com.keenon.sdk.api.DoorTypeDoubleSetApi;
import com.keenon.sdk.api.DoorTypeFourSetApi;
import com.keenon.sdk.api.DoorTypeObserveApi;
import com.keenon.sdk.api.DoorTypeThreeReverseSetApi;
import com.keenon.sdk.api.DoorTypeThreeSetApi;
import com.keenon.sdk.api.door.DoorVersionCompatApi;
import com.keenon.sdk.component.gating.callback.DoorListener;
import com.keenon.sdk.component.gating.data.DoorStatus;
import com.keenon.sdk.component.gating.data.Faults;
import com.keenon.sdk.component.gating.data.GatingType;
import com.keenon.sdk.component.gating.manager.Door;
import com.keenon.sdk.component.gating.state.GatingEvent;
import com.keenon.sdk.component.gating.state.GatingFsm;
import com.keenon.sdk.component.gating.state.GatingState;
import com.keenon.sdk.constant.TopicName;
import com.keenon.sdk.external.IDataCallback;
import com.keenon.sdk.external.PeanutSDK;
import com.keenon.sdk.hedera.model.ApiError;
import java.util.HashMap;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/component/gating/manager/PeanutDoorControl.class */
class PeanutDoorControl implements Door, GatingFsm.GatingStateListener {
    private static final String TAG = "[PeanutDoorControl]";
    private static final int MSG_GATING_FAULT = 1;
    private static final int RETRY_TIMES_LIMIT = 3;
    private static PeanutDoorControl mInstance;
    private static GatingType mGatingType;
    private int mCurState;
    private DoorStatusApi mDoorStatusApi;
    private DoorStatusObserveApi mDoorStatusObserveApi;
    private DoorCloseApi mDoorCloseApi;
    private DoorOpenApi mDoorOpenApi;
    private DoorTypeApi mDoorTypeApi;
    private DoorTypeObserveApi mDoorTypeObserveApi;
    private DoorFaultApi mDoorFaultApi;
    private DoorLimitFaultApi mDoorLimitFaultApi;
    private DoorTypeFourSetApi mDoorTypeFourSetApi;
    private DoorTypeDoubleSetApi mDoorTypeDoubleSetApi;
    private DoorTypeThreeSetApi mDoorTypeThreeSetApi;
    private DoorTypeThreeReverseSetApi mDoorTypeThreeReverseSetApi;
    private DoorTypeAutoSetApi mDoorTypeAutoSetApi;
    private DoorVersionCompatApi mDoorVersionApi;
    private Runnable retryCmdRunnable;
    private DoorVersionCompatApi.VersionInfo mDoorVersion;
    private Handler mainHandler = new Handler();
    private int mCurRetryIndex = 0;
    private IDataCallback mDoorTypeSetListener = new IDataCallback() { // from class: com.keenon.sdk.component.gating.manager.PeanutDoorControl.1
        @Override // com.keenon.sdk.external.IDataCallback
        public void success(String result) {
            for (DoorListener listener : PeanutDoorControl.this.mDoorListenerMap.values()) {
                if (listener != null) {
                    listener.onTypeSetting(true);
                }
            }
        }

        @Override // com.keenon.sdk.external.IDataCallback
        public void error(ApiError error) {
            for (DoorListener listener : PeanutDoorControl.this.mDoorListenerMap.values()) {
                if (listener != null) {
                    listener.onTypeSetting(false);
                }
            }
        }
    };
    IDataCallback doorStatus = new IDataCallback() { // from class: com.keenon.sdk.component.gating.manager.PeanutDoorControl.3
        @Override // com.keenon.sdk.external.IDataCallback
        public void success(String result) {
            LogUtils.d(PeanutConstants.TAG_DOOR, "[PeanutDoorControl][observeDoorStatusChange success ,result : " + result + "]");
            DoorStatusObserveApi.Bean bean = (DoorStatusObserveApi.Bean) GsonUtil.gson2Bean(result, DoorStatusObserveApi.Bean.class);
            if (null != bean && null != bean.getData() && !bean.getData().isEmpty()) {
                for (DoorStatusObserveApi.Bean.DataBean doorStatus : bean.getData()) {
                    if (PeanutDoorControl.this.getDoorStatus(doorStatus.getStatus(), doorStatus.getProgress()) == DoorStatus.OPEN || PeanutDoorControl.this.getDoorStatus(doorStatus.getStatus(), doorStatus.getProgress()) == DoorStatus.CLOSE) {
                        PeanutDoorControl.this.getGatingFsm(doorStatus.getId()).process(GatingEvent.SUCCESS_EVENT);
                    }
                }
            }
        }

        @Override // com.keenon.sdk.external.IDataCallback
        public void error(ApiError error) {
            PeanutDoorControl.this.notifyError(error.code);
        }
    };
    private HashMap<Integer, GatingFsm> mGatingFsmMap = new HashMap<>();
    private HashMap<String, DoorListener> mDoorListenerMap = new HashMap<>();

    static /* synthetic */ int access$808(PeanutDoorControl x0) {
        int i = x0.mCurRetryIndex;
        x0.mCurRetryIndex = i + 1;
        return i;
    }

    private PeanutDoorControl() {
    }

    static PeanutDoorControl getInstance() {
        if (mInstance == null) {
            mInstance = new PeanutDoorControl();
        }
        return mInstance;
    }

    @Override // com.keenon.sdk.component.gating.manager.Door
    public DoorVersionCompatApi.VersionInfo getDoorVersion() {
        return this.mDoorVersion;
    }

    @Override // com.keenon.sdk.component.gating.manager.Door
    public boolean supportDoorTypeSetting() {
        return null == this.mDoorVersion || StringUtils.isEmpty(getDoorVersion().getSw());
    }

    private void setListener(@NonNull String tag, @NonNull DoorListener listener) {
        this.mDoorListenerMap.put(tag, listener);
    }

    private void removeListener(@NonNull String tag) {
        this.mDoorListenerMap.remove(tag);
    }

    @Override // com.keenon.sdk.component.gating.manager.Door
    public void init(Context context) {
        getBaseStatus();
    }

    @Override // com.keenon.sdk.component.gating.manager.Door
    public void setDoorListerner(String tag, DoorListener listener) {
        setListener(tag, listener);
        observeFault();
        observeLimitFault();
    }

    @Override // com.keenon.sdk.component.gating.manager.Door
    public void removeFloorListener(String tag) {
        removeListener(tag);
    }

    @Override // com.keenon.sdk.component.gating.manager.Door
    public int getDoorType() {
        if (mGatingType == null) {
            notifyError(PeanutError.DOOR_TYPE_UNKNOW);
        }
        switch (mGatingType) {
            case FOUR:
            default:
                return 0;
            case DOUBLE:
                return 1;
            case THREE:
                return 2;
            case THREE_REVERSE:
                return 3;
        }
    }

    @Override // com.keenon.sdk.component.gating.manager.Door
    public GatingState getDoorState(int doorId) {
        if (getGatingFsm(doorId) == null) {
            notifyError(PeanutError.DOOR_GATINGFSM_FAIL);
            return null;
        }
        return getGatingFsm(doorId).getGatingState();
    }

    @Override // com.keenon.sdk.component.gating.manager.Door
    public void openDoor(int doorId, boolean single) {
        if (single) {
            for (GatingFsm fsm : this.mGatingFsmMap.values()) {
                if (fsm.getGatingState() != GatingState.CLOSED && doorId != fsm.mGatingId) {
                    controlGating(fsm.mGatingId, GatingEvent.CLOSE_EVENT);
                }
            }
            controlGating(doorId, GatingEvent.OPEN_EVENT);
            return;
        }
        controlGatingOpen(doorId);
        retryGatingCmd(doorId, GatingEvent.OPEN_EVENT);
    }

    @Override // com.keenon.sdk.component.gating.manager.Door
    public void closeDoor(int doorId) {
        controlGatingClose(doorId);
        retryGatingCmd(doorId, GatingEvent.CLOSE_EVENT);
    }

    @Override // com.keenon.sdk.component.gating.manager.Door
    public void closeAllDoor() {
        for (GatingFsm fsm : this.mGatingFsmMap.values()) {
            if (fsm.getGatingState() != GatingState.CLOSED) {
                controlGating(fsm.mGatingId, GatingEvent.CLOSE_EVENT);
            }
        }
    }

    @Override // com.keenon.sdk.component.gating.manager.Door
    public boolean isAllDoorClose() {
        for (GatingFsm fsm : this.mGatingFsmMap.values()) {
            if (fsm.getGatingState() != GatingState.CLOSED) {
                return false;
            }
        }
        return true;
    }

    @Override // com.keenon.sdk.component.gating.manager.Door
    public void setDoorType(int gatingType, String tag, DoorListener listener) {
        this.mDoorListenerMap.put(tag, listener);
        switch (gatingType) {
            case 1:
                getDoorTypeFourSetApi().send(this.mDoorTypeSetListener);
                break;
            case 2:
                getDoorTypeDoubleSetApi().send(this.mDoorTypeSetListener);
                break;
            case 3:
                getDoorTypeThreeSetApi().send(this.mDoorTypeSetListener);
                break;
            case 4:
                getDoorTypeThreeReverseSetApi().send(this.mDoorTypeSetListener);
                break;
            case 5:
                getDoorTypeAutoSetApi().send(this.mDoorTypeSetListener);
                break;
        }
    }

    private DoorTypeApi getDoorTypeApi() {
        if (null == this.mDoorTypeApi) {
            this.mDoorTypeApi = new DoorTypeApi();
        }
        return this.mDoorTypeApi;
    }

    private DoorStatusApi getDoorStatusApi() {
        if (null == this.mDoorStatusApi) {
            this.mDoorStatusApi = new DoorStatusApi();
        }
        return this.mDoorStatusApi;
    }

    private DoorTypeObserveApi getDoorTypeObserveApi() {
        if (null == this.mDoorTypeObserveApi) {
            this.mDoorTypeObserveApi = new DoorTypeObserveApi();
        }
        return this.mDoorTypeObserveApi;
    }

    private DoorStatusObserveApi getDoorStatusObserveApi() {
        if (null == this.mDoorStatusObserveApi) {
            this.mDoorStatusObserveApi = new DoorStatusObserveApi();
        }
        return this.mDoorStatusObserveApi;
    }

    private DoorOpenApi getDoorOpenApi() {
        if (null == this.mDoorOpenApi) {
            this.mDoorOpenApi = new DoorOpenApi();
        }
        return this.mDoorOpenApi;
    }

    private DoorCloseApi getDoorCloseApi() {
        if (null == this.mDoorCloseApi) {
            this.mDoorCloseApi = new DoorCloseApi();
        }
        return this.mDoorCloseApi;
    }

    private DoorFaultApi getDoorFaultApi() {
        if (null == this.mDoorFaultApi) {
            this.mDoorFaultApi = new DoorFaultApi();
        }
        return this.mDoorFaultApi;
    }

    private DoorLimitFaultApi getDoorLimitFaultApi() {
        if (null == this.mDoorLimitFaultApi) {
            this.mDoorLimitFaultApi = new DoorLimitFaultApi();
        }
        return this.mDoorLimitFaultApi;
    }

    private DoorTypeAutoSetApi getDoorTypeAutoSetApi() {
        if (null == this.mDoorTypeAutoSetApi) {
            this.mDoorTypeAutoSetApi = new DoorTypeAutoSetApi();
        }
        return this.mDoorTypeAutoSetApi;
    }

    private DoorVersionCompatApi getDoorVersionApi() {
        if (null == this.mDoorVersionApi) {
            this.mDoorVersionApi = new DoorVersionCompatApi();
        }
        return this.mDoorVersionApi;
    }

    private DoorTypeFourSetApi getDoorTypeFourSetApi() {
        if (null == this.mDoorTypeFourSetApi) {
            this.mDoorTypeFourSetApi = new DoorTypeFourSetApi();
        }
        return this.mDoorTypeFourSetApi;
    }

    private DoorTypeDoubleSetApi getDoorTypeDoubleSetApi() {
        if (null == this.mDoorTypeDoubleSetApi) {
            this.mDoorTypeDoubleSetApi = new DoorTypeDoubleSetApi();
        }
        return this.mDoorTypeDoubleSetApi;
    }

    private DoorTypeThreeSetApi getDoorTypeThreeSetApi() {
        if (null == this.mDoorTypeThreeSetApi) {
            this.mDoorTypeThreeSetApi = new DoorTypeThreeSetApi();
        }
        return this.mDoorTypeThreeSetApi;
    }

    private DoorTypeThreeReverseSetApi getDoorTypeThreeReverseSetApi() {
        if (null == this.mDoorTypeThreeReverseSetApi) {
            this.mDoorTypeThreeReverseSetApi = new DoorTypeThreeReverseSetApi();
        }
        return this.mDoorTypeThreeReverseSetApi;
    }

    private void getBaseStatus() {
        getDoorTypeInfo();
        getDoorVersionInfo();
        observeDoorTypeChange();
        observeDoorStatusChange();
    }

    private void getDoorVersionInfo() {
        getDoorVersionApi().send(new IDataCallback() { // from class: com.keenon.sdk.component.gating.manager.PeanutDoorControl.2
            @Override // com.keenon.sdk.external.IDataCallback
            public void success(String result) {
                DoorVersionCompatApi.Bean bean = (DoorVersionCompatApi.Bean) GsonUtil.gson2Bean(result, DoorVersionCompatApi.Bean.class);
                PeanutDoorControl.this.mDoorVersion = bean.getData();
            }

            @Override // com.keenon.sdk.external.IDataCallback
            public void error(ApiError error) {
                PeanutDoorControl.this.notifyError(error.code);
            }
        });
    }

    private void observeDoorStatusChange() {
        PeanutSDK.getInstance().subscribe(TopicName.DOOR_STATUS_OBSERVE, this.doorStatus);
        PeanutSDK.getInstance().subscribe(TopicName.DOOR_STATUS, this.doorStatus);
    }

    private void observeDoorTypeChange() {
        PeanutSDK.getInstance().subscribe(TopicName.DOOR_TYPE_OBSERVE, new IDataCallback() { // from class: com.keenon.sdk.component.gating.manager.PeanutDoorControl.4
            @Override // com.keenon.sdk.external.IDataCallback
            public void success(String result) {
                LogUtils.d(PeanutConstants.TAG_DOOR, "[PeanutDoorControl][doorTypeApi success ,result :" + result + " ]");
                DoorTypeObserveApi.Bean bean = (DoorTypeObserveApi.Bean) GsonUtil.gson2Bean(result, DoorTypeObserveApi.Bean.class);
                GatingType unused = PeanutDoorControl.mGatingType = GatingType.getGatingType(bean.getData().getStatus());
                for (DoorListener listener : PeanutDoorControl.this.mDoorListenerMap.values()) {
                    listener.onTypeChange(PeanutDoorControl.mGatingType);
                }
            }

            @Override // com.keenon.sdk.external.IDataCallback
            public void error(ApiError error) {
                PeanutDoorControl.this.notifyError(error.code);
            }
        });
    }

    private void getDoorTypeInfo() {
        getDoorTypeApi().send(new IDataCallback() { // from class: com.keenon.sdk.component.gating.manager.PeanutDoorControl.5
            @Override // com.keenon.sdk.external.IDataCallback
            public void success(String result) {
                LogUtils.d(PeanutConstants.TAG_DOOR, "[PeanutDoorControl][doorTypeApi success ,result : " + result + "]");
                DoorTypeApi.Bean bean = (DoorTypeApi.Bean) GsonUtil.gson2Bean(result, DoorTypeApi.Bean.class);
                GatingType unused = PeanutDoorControl.mGatingType = GatingType.getGatingType(bean.getData().getStatus());
            }

            @Override // com.keenon.sdk.external.IDataCallback
            public void error(ApiError error) {
                PeanutDoorControl.this.notifyError(error.code);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public GatingFsm getGatingFsm(int floorId) {
        if (this.mGatingFsmMap.get(Integer.valueOf(floorId)) != null) {
            return this.mGatingFsmMap.get(Integer.valueOf(floorId));
        }
        GatingFsm gatingFsm = new GatingFsm(floorId);
        if (gatingFsm == null) {
            notifyError(PeanutError.DOOR_DOORID_ILLEGAL);
            return null;
        }
        gatingFsm.setGatingStateListener(this);
        this.mGatingFsmMap.put(Integer.valueOf(floorId), gatingFsm);
        return gatingFsm;
    }

    private void controlGating(int floorId, GatingEvent event) {
        if (event == GatingEvent.OPEN_EVENT) {
            controlGatingOpen(floorId);
            retryGatingCmd(floorId, GatingEvent.OPEN_EVENT);
        } else if (event == GatingEvent.CLOSE_EVENT) {
            controlGatingClose(floorId);
            retryGatingCmd(floorId, GatingEvent.CLOSE_EVENT);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void controlGatingOpen(final int floorId) {
        LogUtils.i(PeanutConstants.TAG_DOOR, "[PeanutDoorControl][controlGatingOpen]");
        getGatingFsm(floorId).process(GatingEvent.OPEN_EVENT);
        getDoorOpenApi().send(new IDataCallback() { // from class: com.keenon.sdk.component.gating.manager.PeanutDoorControl.6
            @Override // com.keenon.sdk.external.IDataCallback
            public void success(String result) {
                LogUtils.d(PeanutConstants.TAG_DOOR, "[PeanutDoorControl][response controlGatingOpen ,doorId : " + floorId + "]");
                if (result != null && PeanutDoorControl.this.retryCmdRunnable != null) {
                    PeanutDoorControl.this.mainHandler.removeCallbacks(PeanutDoorControl.this.retryCmdRunnable);
                }
            }

            @Override // com.keenon.sdk.external.IDataCallback
            public void error(ApiError error) {
                PeanutDoorControl.this.notifyError(error.code);
            }
        }, floorId);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void controlGatingClose(final int floorId) {
        LogUtils.d(PeanutConstants.TAG_DOOR, "[PeanutDoorControl][controlGatingClose]");
        getGatingFsm(floorId).process(GatingEvent.CLOSE_EVENT);
        getDoorCloseApi().send(new IDataCallback() { // from class: com.keenon.sdk.component.gating.manager.PeanutDoorControl.7
            @Override // com.keenon.sdk.external.IDataCallback
            public void success(String result) {
                LogUtils.d(PeanutConstants.TAG_DOOR, "[PeanutDoorControl][response controlGatingClose ,doorId : " + floorId + "]");
                if (result != null && PeanutDoorControl.this.retryCmdRunnable != null) {
                    PeanutDoorControl.this.mainHandler.removeCallbacks(PeanutDoorControl.this.retryCmdRunnable);
                }
            }

            @Override // com.keenon.sdk.external.IDataCallback
            public void error(ApiError error) {
                PeanutDoorControl.this.notifyError(error.code);
            }
        }, floorId);
    }

    private void observeFault() {
        LogUtils.i(PeanutConstants.TAG_DOOR, "[PeanutDoorControl][observeDefault]");
        PeanutSDK.getInstance().subscribe(TopicName.DOOR_FAULT, new IDataCallback() { // from class: com.keenon.sdk.component.gating.manager.PeanutDoorControl.8
            @Override // com.keenon.sdk.external.IDataCallback
            public void success(String result) {
                LogUtils.d(PeanutConstants.TAG_DOOR, "[PeanutDoorControl][response observeDefault , result : " + result + "]");
                DoorFaultApi.Bean bean = (DoorFaultApi.Bean) GsonUtil.gson2Bean(result, DoorFaultApi.Bean.class);
                int doorId = bean.getData().getFloorId();
                Door.DoorFault doorFault = bean.getData().getStatus() == 1 ? Door.DoorFault.OPENING : Door.DoorFault.CLOSING;
                if (doorFault == Door.DoorFault.CLOSING) {
                    for (DoorListener listener : PeanutDoorControl.this.mDoorListenerMap.values()) {
                        listener.onFault(Faults.CLOSE_FAULT, doorId);
                    }
                    return;
                }
                for (DoorListener listener2 : PeanutDoorControl.this.mDoorListenerMap.values()) {
                    listener2.onFault(Faults.OPEN_FAULT, doorId);
                }
            }

            @Override // com.keenon.sdk.external.IDataCallback
            public void error(ApiError error) {
                PeanutDoorControl.this.notifyError(error.code);
            }
        });
    }

    private void observeLimitFault() {
        LogUtils.i(PeanutConstants.TAG_DOOR, "[PeanutDoorControl][observeLimitFault]");
        getDoorLimitFaultApi().observe(new IDataCallback() { // from class: com.keenon.sdk.component.gating.manager.PeanutDoorControl.9
            @Override // com.keenon.sdk.external.IDataCallback
            public void success(String result) {
                LogUtils.d(PeanutConstants.TAG_DOOR, "[PeanutDoorControl][response observeLimitFault,result : " + result + "]");
                for (DoorListener listener : PeanutDoorControl.this.mDoorListenerMap.values()) {
                    listener.onFault(Faults.LIMIT_FAULT, -1);
                }
            }

            @Override // com.keenon.sdk.external.IDataCallback
            public void error(ApiError error) {
                PeanutDoorControl.this.notifyError(error.code);
            }
        });
    }

    private void retryGatingCmd(final int floorId, final GatingEvent event) {
        this.mCurRetryIndex = 0;
        if (this.retryCmdRunnable != null) {
            this.mainHandler.removeCallbacks(this.retryCmdRunnable);
        }
        this.retryCmdRunnable = new Runnable() { // from class: com.keenon.sdk.component.gating.manager.PeanutDoorControl.10
            @Override // java.lang.Runnable
            public void run() {
                if (PeanutDoorControl.this.mCurRetryIndex >= 3) {
                    PeanutDoorControl.this.mainHandler.removeCallbacks(this);
                }
                LogUtils.d(PeanutConstants.TAG_DOOR, "[PeanutDoorControl][retryGatingCmd,doorId : " + floorId + ",event :" + event.name() + "]");
                if (event == GatingEvent.OPEN_EVENT) {
                    PeanutDoorControl.this.controlGatingOpen(floorId);
                } else if (event == GatingEvent.CLOSE_EVENT) {
                    PeanutDoorControl.this.controlGatingClose(floorId);
                }
                PeanutDoorControl.this.mainHandler.postDelayed(this, 1000L);
                PeanutDoorControl.access$808(PeanutDoorControl.this);
            }
        };
        this.mainHandler.postDelayed(this.retryCmdRunnable, 1000L);
    }

    @Override // com.keenon.sdk.component.gating.state.GatingFsm.GatingStateListener
    public void onStateChange(int floorId, int state) {
        if (this.mCurState == state) {
            return;
        }
        this.mCurState = state;
        LogUtils.d(PeanutConstants.TAG_DOOR, "[PeanutDoorControl][onStateChange , floowid ：" + floorId + "    status is :" + state + "]");
        for (DoorListener listener : this.mDoorListenerMap.values()) {
            listener.onStateChange(floorId, state);
        }
    }

    @Override // com.keenon.sdk.component.gating.manager.Door
    public void release() {
        this.mGatingFsmMap = null;
        if (this.mainHandler != null) {
            this.mainHandler.removeMessages(1);
            if (this.retryCmdRunnable != null) {
                this.mainHandler.removeCallbacks(this.retryCmdRunnable);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public DoorStatus getDoorStatus(int status, int progress) {
        DoorStatus doorStatus = DoorStatus.DEFAULT;
        switch (status) {
            case 0:
                if (progress < 100) {
                    doorStatus = DoorStatus.CLOSING;
                } else {
                    doorStatus = DoorStatus.CLOSE;
                }
                break;
            case 255:
                if (progress < 100) {
                    doorStatus = DoorStatus.OPENING;
                } else {
                    doorStatus = DoorStatus.OPEN;
                }
                break;
        }
        return doorStatus;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void notifyError(int error) {
        for (DoorListener listener : this.mDoorListenerMap.values()) {
            listener.onError(error);
        }
    }
}
