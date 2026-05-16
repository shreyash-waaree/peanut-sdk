package com.keenon.sdk.component.navigation;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import androidx.annotation.NonNull;
import com.keenon.common.constant.PeanutConstants;
import com.keenon.common.error.ExceptionMessage;
import com.keenon.common.error.PeanutError;
import com.keenon.common.utils.GsonUtil;
import com.keenon.common.utils.LogUtils;
import com.keenon.common.utils.PeanutMainThreadExecutor;
import com.keenon.sdk.api.NavigationStatusApi;
import com.keenon.sdk.component.NavigationComponent;
import com.keenon.sdk.component.navigation.arrival.ArrivalControl;
import com.keenon.sdk.component.navigation.common.Navigation;
import com.keenon.sdk.component.navigation.common.NavigationFsm;
import com.keenon.sdk.component.navigation.route.RouteLine;
import com.keenon.sdk.component.navigation.route.RouteNode;
import com.keenon.sdk.constant.TopicName;
import com.keenon.sdk.external.IDataCallback;
import com.keenon.sdk.external.PeanutSDK;
import com.keenon.sdk.hedera.model.ApiData;
import com.keenon.sdk.hedera.model.ApiError;
import com.keenon.sdk.scmIot.protopack.base.ProtoDev;
import java.util.concurrent.CopyOnWriteArraySet;
import org.apache.commons.net.tftp.TFTP;
import org.eclipse.californium.core.coap.NoResponseOption;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/component/navigation/NavigationImpl.class */
public class NavigationImpl implements Navigation, Handler.Callback, RouteLine.Listener, NavigationFsm.Listener, ArrivalControl.Listener {
    private static final String TAG = "[NavigationImpl]";
    private static final int MSG_PREPARE = 1;
    private static final int MSG_SET_PILOT_WHEN_READY = 2;
    private static final int MSG_DO_SOME_WORK = 3;
    private static final int MSG_DESTINATION = 4;
    private static final int MSG_STOP = 5;
    private static final int MSG_RESET = 6;
    private static final int MSG_RELEASE = 7;
    private static final int MSG_BLOCKED = 8;
    private static final int MSG_MANUAL = 9;
    private static final int MSG_SET_BLOCK_TIMEOUT = 10;
    private static final int REPEAT_INFINITE = -1;
    private static volatile NavigationImpl sInstance;
    private final Handler handler;
    private final HandlerThread navigationThread;
    private final CopyOnWriteArraySet<Navigation.Listener> listeners;
    private NavigationFsm navigationFsm;
    private ArrivalControl arrivalControl;
    private RouteLine routeLine;
    private RouteNode routeNodeRunning;
    private boolean released;
    private boolean pilotWhenReady;
    private boolean isArrivalEffect;
    private boolean isTakeControl;
    private int state;
    private int index;
    private int repeat;
    private int schedule;
    private PeanutMainThreadExecutor mainThreadExecutor;
    private IDataCallback mStatusCallBack = new IDataCallback() { // from class: com.keenon.sdk.component.navigation.NavigationImpl.1
        @Override // com.keenon.sdk.external.IDataCallback
        public void success(String response) {
            NavigationStatusApi.Bean data = (NavigationStatusApi.Bean) GsonUtil.gson2Bean(response, NavigationStatusApi.Bean.class);
            LogUtils.d(NavigationImpl.TAG, "[mStatusCallBack-onSuccess][bean = " + response + "]");
            if (data == null || data.getData() == null) {
                NavigationImpl.this.notifyError(PeanutError.COAP_RESPONSE_NULL_DATA);
                return;
            }
            NavigationImpl.this.schedule = data.getData().getSchedule();
            NavigationImpl.this.handleRawStatus(data.getData().getStatus());
            if (NavigationImpl.this.routeNodeRunning.getNavigationInfo().getRemainDistance() == null || !NavigationImpl.this.routeNodeRunning.getNavigationInfo().getRemainDistance().equals(Float.valueOf(data.getData().getRemain_length()))) {
                NavigationImpl.this.arrivalControl.notifyDistanceChanged(data.getData().getRemain_length());
            }
            NavigationImpl.this.updateNavigationInfo(data.getData());
        }

        @Override // com.keenon.sdk.external.IDataCallback
        public void error(ApiError error) {
            LogUtils.e(NavigationImpl.TAG, "[navigation-status][error][" + error.toString() + "]");
            NavigationImpl.this.notifyError(error.getCode());
        }
    };

    NavigationImpl(ArrivalControl arrivalControl) {
        if (arrivalControl == null) {
            notifyError(PeanutError.NAVI_NO_ARRIVAL);
            throw new RuntimeException(ExceptionMessage.NullArrival);
        }
        this.pilotWhenReady = false;
        this.index = 0;
        this.repeat = 0;
        this.state = 0;
        this.listeners = new CopyOnWriteArraySet<>();
        this.arrivalControl = arrivalControl;
        this.routeNodeRunning = new RouteNode();
        this.navigationFsm = new NavigationFsm();
        this.navigationFsm.addListener(this);
        arrivalControl.addListener(this);
        this.navigationThread = new HandlerThread("NavigationImpl:Handler", -16);
        this.navigationThread.start();
        this.handler = new Handler(this.navigationThread.getLooper(), this);
        this.mainThreadExecutor = PeanutSDK.getInstance().getMainThreadExecutor();
    }

    public static NavigationImpl getInstance(ArrivalControl arrivalControl) {
        if (sInstance == null) {
            synchronized (NavigationImpl.class) {
                if (sInstance == null) {
                    sInstance = new NavigationImpl(arrivalControl);
                }
            }
        }
        return sInstance;
    }

    @Override // com.keenon.sdk.component.navigation.common.Navigation
    public void addListener(Navigation.Listener listener) {
        this.listeners.add(listener);
    }

    @Override // com.keenon.sdk.component.navigation.common.Navigation
    public void removeListener(Navigation.Listener listener) {
        this.listeners.remove(listener);
    }

    @Override // com.keenon.sdk.component.navigation.common.Navigation
    public void prepare(RouteLine routeLine) {
        if (routeLine == null || routeLine.getLength() == 0) {
            notifyError(PeanutError.NAVI_NO_DEST);
            return;
        }
        this.routeLine = routeLine;
        this.routeLine.addListener(this);
        prepareInternal(routeLine);
    }

    @Override // com.keenon.sdk.component.navigation.common.Navigation
    public void setPilotWhenReady(boolean pilotWhenReady) {
        if (this.pilotWhenReady != pilotWhenReady) {
            this.pilotWhenReady = pilotWhenReady;
            LogUtils.d(PeanutConstants.TAG_NAVI, "[NavigationImpl][setPilotWhenReady][pilotWhenReady->" + pilotWhenReady + "]");
            this.handler.obtainMessage(2, pilotWhenReady ? 1 : 0, 0).sendToTarget();
        }
    }

    @Override // com.keenon.sdk.component.navigation.common.Navigation
    public void setTaskControl(boolean isTaskControl) {
        this.isTakeControl = isTaskControl;
        LogUtils.d(PeanutConstants.TAG_NAVI, "[NavigationImpl][setTaskControl][isTaskControl->" + isTaskControl + "]");
    }

    @Override // com.keenon.sdk.component.navigation.common.Navigation
    public void pilotNext() {
        LogUtils.d(PeanutConstants.TAG_NAVI, "[NavigationImpl][pilotNext]");
        prepareNextRouteNode();
    }

    @Override // com.keenon.sdk.component.navigation.common.Navigation
    public RouteNode[] getRouteNodes() {
        return this.routeLine.getRouteNodes();
    }

    @Override // com.keenon.sdk.component.navigation.common.Navigation
    public RouteNode getCurrentNode() {
        return this.routeNodeRunning;
    }

    @Override // com.keenon.sdk.component.navigation.common.Navigation
    public RouteLine getRouteLine() {
        return this.routeLine;
    }

    @Override // com.keenon.sdk.component.navigation.common.Navigation
    public RouteNode getNextNode() {
        LogUtils.d(TAG, "getNextNode :  repeat = " + this.repeat + " , index = " + this.index);
        if (!isInfiniteRepeat() && this.repeat >= this.routeLine.getRepeatCount()) {
            return null;
        }
        if (isLastRepeat() && this.index + 1 >= this.routeLine.getLength()) {
            return null;
        }
        return this.routeLine.getRouteNodes()[(this.index + 1) % this.routeLine.getLength()];
    }

    private boolean isInfiniteRepeat() {
        return this.routeLine.getRepeatCount() == -1;
    }

    @Override // com.keenon.sdk.component.navigation.common.Navigation
    public int getCurrentPosition() {
        return this.index;
    }

    @Override // com.keenon.sdk.component.navigation.common.Navigation
    public void stop() {
        notifyFsm(7);
    }

    @Override // com.keenon.sdk.component.navigation.common.Navigation
    public void manual(int direction) {
        this.handler.obtainMessage(9, direction, 0).sendToTarget();
    }

    @Override // com.keenon.sdk.component.navigation.common.Navigation
    public void setSpeed(int speed) {
        setSpeedInternal(speed);
    }

    @Override // com.keenon.sdk.component.navigation.common.Navigation
    public void setStableMode(int mode) {
        setStableModeInternal(mode);
    }

    @Override // com.keenon.sdk.component.navigation.common.Navigation
    public void setSportMode(int mode) {
        setSportModeInternal(mode);
    }

    @Override // com.keenon.sdk.component.navigation.common.Navigation
    public void setBlockTimeout(int timeout) {
        this.handler.obtainMessage(10, Integer.valueOf(timeout)).sendToTarget();
    }

    @Override // com.keenon.sdk.component.navigation.common.Navigation
    public synchronized void release() {
        if (this.released) {
            return;
        }
        this.handler.sendEmptyMessage(7);
    }

    @Override // com.keenon.sdk.component.navigation.common.Navigation
    public void skipTo(int index) {
        this.index = index;
    }

    @Override // com.keenon.sdk.component.navigation.arrival.ArrivalControl.Listener
    public void onArrived() {
        LogUtils.d(PeanutConstants.TAG_NAVI, "[NavigationImpl][onArrived][近似到达]");
        this.isArrivalEffect = true;
        notifyFsm(3);
    }

    @Override // com.keenon.sdk.component.navigation.route.RouteLine.Listener
    public void onRoutePrepared() {
        notifyRoutePrepared();
    }

    @Override // com.keenon.sdk.component.navigation.route.RouteLine.Listener
    public void onRouteError(int code) {
        LogUtils.d(PeanutConstants.TAG_NAVI, "[NavigationImpl][onRouteError][error " + code + "]");
        notifyError(code);
    }

    @Override // com.keenon.sdk.component.navigation.common.NavigationFsm.Listener
    public void onStateSafely(int state) {
        setState(state);
    }

    @Override // android.os.Handler.Callback
    public boolean handleMessage(@NonNull Message msg) {
        try {
            switch (msg.what) {
                case 1:
                    prepareInternal((RouteLine) msg.obj);
                    break;
                case 2:
                    setPlayWhenReadyInternal(msg.arg1 != 0);
                    break;
                case 3:
                    doSomeWork();
                    break;
                case 4:
                    arrivalInternal();
                    break;
                case 5:
                    stopInternal();
                    break;
                case 6:
                    resetInternal();
                    break;
                case 7:
                    releaseInternal();
                    break;
                case 8:
                    blockInternal();
                    break;
                case 9:
                    manualInternal(msg.arg1);
                    break;
                case 10:
                    setBlockTimeoutInternal(((Integer) msg.obj).intValue());
                    break;
            }
            return true;
        } catch (RuntimeException e) {
            LogUtils.e(PeanutConstants.TAG_NAVI, TAG, (Exception) e);
            resetInternal();
            return true;
        }
    }

    private void setState(int state) {
        if (this.state != state) {
            this.state = state;
            handleState(state);
            notifyStateChanged(state);
        }
    }

    private void handleState(int state) {
        switch (state) {
            case 1:
                this.handler.sendEmptyMessage(3);
                break;
            case 3:
                this.handler.sendEmptyMessage(4);
                break;
            case 5:
            case 6:
                this.handler.sendEmptyMessage(8);
                break;
            case 7:
                this.handler.sendEmptyMessage(5);
                break;
        }
    }

    private synchronized void notifyStateChanged(final int state) {
        for (final Navigation.Listener listener : this.listeners) {
            this.mainThreadExecutor.execute(new Runnable() { // from class: com.keenon.sdk.component.navigation.NavigationImpl.2
                @Override // java.lang.Runnable
                public void run() {
                    listener.onStateChanged(state, NavigationImpl.this.schedule);
                }
            });
        }
    }

    private synchronized void notifyNodeChanged() {
        for (final Navigation.Listener listener : this.listeners) {
            this.mainThreadExecutor.execute(new Runnable() { // from class: com.keenon.sdk.component.navigation.NavigationImpl.3
                @Override // java.lang.Runnable
                public void run() {
                    listener.onRouteNode(NavigationImpl.this.index, NavigationImpl.this.routeNodeRunning);
                }
            });
        }
    }

    private void notifyRoutePrepared() {
        for (final Navigation.Listener listener : this.listeners) {
            this.mainThreadExecutor.execute(new Runnable() { // from class: com.keenon.sdk.component.navigation.NavigationImpl.4
                @Override // java.lang.Runnable
                public void run() {
                    listener.onRoutePrepared(NavigationImpl.this.getRouteNodes());
                }
            });
        }
    }

    private synchronized void notifyDistanceChanged(final float distance) {
        for (final Navigation.Listener listener : this.listeners) {
            this.mainThreadExecutor.execute(new Runnable() { // from class: com.keenon.sdk.component.navigation.NavigationImpl.5
                @Override // java.lang.Runnable
                public void run() {
                    listener.onDistanceChanged(distance);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void notifyError(final int error) {
        resetInternal();
        for (final Navigation.Listener listener : this.listeners) {
            this.mainThreadExecutor.execute(new Runnable() { // from class: com.keenon.sdk.component.navigation.NavigationImpl.6
                @Override // java.lang.Runnable
                public void run() {
                    listener.onError(error);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void notifyEvent(final int event) {
        for (final Navigation.Listener listener : this.listeners) {
            this.mainThreadExecutor.execute(new Runnable() { // from class: com.keenon.sdk.component.navigation.NavigationImpl.7
                @Override // java.lang.Runnable
                public void run() {
                    listener.onEvent(event);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void notifyFsm(int event) {
        if (null != this.navigationFsm) {
            this.navigationFsm.process(event);
        } else {
            notifyError(PeanutError.NAVI_NO_FSM);
        }
    }

    private void prepareInternal(RouteLine routeLine) {
        LogUtils.d(PeanutConstants.TAG_NAVI, "[NavigationImpl][prepareInternal]");
        routeLine.prepare();
    }

    private void setPlayWhenReadyInternal(boolean pilotWhenReady) {
        LogUtils.d(PeanutConstants.TAG_NAVI, "[NavigationImpl][doSomeWork][setPlayWhenReadyInternal->" + pilotWhenReady + "]");
        if (!pilotWhenReady) {
            if (this.state == 2 || this.state == 1) {
                motionPause();
                return;
            }
            return;
        }
        if (this.state == 0) {
            notifyFsm(1);
        } else {
            motionContinue();
        }
    }

    private void doSomeWork() {
        LogUtils.d(PeanutConstants.TAG_NAVI, "[NavigationImpl][doSomeWork][pilotWhenReady->" + this.pilotWhenReady + "]");
        if (this.pilotWhenReady) {
            prepareRouteNode();
            PeanutSDK.getInstance().subscribe(TopicName.NAVIGATION_STATUS, this.mStatusCallBack);
            navigate();
            return;
        }
        LogUtils.d(PeanutConstants.TAG_NAVI, "[NavigationImpl][doSomeWork][pilotWhenReady not set]");
    }

    private void prepareRouteNode() {
        if (this.index == Integer.MIN_VALUE) {
            this.index = 0;
        }
        if (this.repeat == Integer.MIN_VALUE) {
            this.repeat = 0;
        }
        this.isArrivalEffect = false;
        try {
            this.routeNodeRunning = this.routeLine.getRouteNodes()[this.index];
            notifyNodeChanged();
        } catch (IndexOutOfBoundsException e) {
            notifyError(PeanutError.getExceptionCode(e));
        }
    }

    private void prepareNextRouteNode() {
        if (this.repeat >= this.routeLine.getRepeatCount() && !isInfiniteRepeat()) {
            notifyError(PeanutError.NAVI_REPEAT_LIMIT);
            return;
        }
        this.index++;
        if (this.index >= this.routeLine.getLength()) {
            this.index %= this.routeLine.getLength();
            if (!isInfiniteRepeat()) {
                this.repeat++;
            }
        }
        LogUtils.d(TAG, "index : " + this.index + "  repeat :" + this.repeat);
        this.routeNodeRunning = this.routeLine.getRouteNodes()[this.index];
        this.pilotWhenReady = true;
        notifyFsm(1);
    }

    private void navigate() {
        PeanutSDK.getInstance().navigation().setTarget(new IDataCallback() { // from class: com.keenon.sdk.component.navigation.NavigationImpl.8
            @Override // com.keenon.sdk.external.IDataCallback
            public void success(String response) {
                LogUtils.d(PeanutConstants.TAG_NAVI, "[NavigationImpl][navigate-onSuccess][bean = " + response + "]");
                ApiData data = (ApiData) GsonUtil.gson2Bean(response, ApiData.class);
                if (null != data) {
                    if (data.getCode() == 0) {
                        NavigationImpl.this.notifyEvent(100);
                        return;
                    }
                    if (data.getCode() == 35) {
                        NavigationImpl.this.notifyError(PeanutError.NAVI_NO_DEST);
                    } else if (data.getCode() == 81) {
                        NavigationImpl.this.notifyError(PeanutError.NAVI_NO_PATH);
                    } else if (data.getCode() == 19) {
                        NavigationImpl.this.notifyError(PeanutError.NAVI_IN_CHARGING);
                    }
                }
            }

            @Override // com.keenon.sdk.external.IDataCallback
            public void error(ApiError error) {
                NavigationImpl.this.notifyError(error.getCode());
            }
        }, this.routeNodeRunning.getId().intValue(), this.isTakeControl ? 1 : 0);
    }

    private void arrivalInternal() {
        LogUtils.d(PeanutConstants.TAG_NAVI, "[NavigationImpl][arrivalInternal]");
        arrivalSafely();
        notifyFsm(0);
    }

    private void arrivalSafely() {
        if (this.isArrivalEffect) {
            motionStop();
        }
        motionReset();
        this.arrivalControl.release();
    }

    private void setSpeedInternal(int speed) {
        LogUtils.d(PeanutConstants.TAG_NAVI, "[NavigationImpl][setSpeedInternal][" + speed + "]");
        PeanutSDK.getInstance().navigation().setSpeed(new IDataCallback() { // from class: com.keenon.sdk.component.navigation.NavigationImpl.9
            @Override // com.keenon.sdk.external.IDataCallback
            public void success(String result) {
            }

            @Override // com.keenon.sdk.external.IDataCallback
            public void error(ApiError error) {
            }
        }, speed);
    }

    private void setStableModeInternal(int mode) {
        LogUtils.d(PeanutConstants.TAG_NAVI, "[NavigationImpl][setModeInternal][" + mode + "]");
        PeanutSDK.getInstance().navigation().setStableMode(null, mode);
    }

    private void setSportModeInternal(int mode) {
        LogUtils.d(PeanutConstants.TAG_NAVI, "[NavigationImpl][setSportModeInternal][" + mode + "]");
        PeanutSDK.getInstance().navigation().setSportMode(null, mode);
    }

    private void setBlockTimeoutInternal(int timeout) {
        LogUtils.d(PeanutConstants.TAG_NAVI, "[NavigationImpl][setBlockTimeoutInternal][" + timeout + "]");
        PeanutSDK.getInstance().navigation().setBlockTimeout(new IDataCallback() { // from class: com.keenon.sdk.component.navigation.NavigationImpl.10
            @Override // com.keenon.sdk.external.IDataCallback
            public void success(String result) {
                LogUtils.d(PeanutConstants.TAG_NAVI, "[NavigationImpl][setBlockTimeoutInternal Success]");
            }

            @Override // com.keenon.sdk.external.IDataCallback
            public void error(ApiError error) {
                LogUtils.e(PeanutConstants.TAG_NAVI, "[NavigationImpl][setBlockTimeoutInternal Error][" + error.toString() + "]");
            }
        }, timeout);
    }

    private void blockInternal() {
        this.arrivalControl.notifyBlocked();
    }

    private void stopInternal() {
        LogUtils.d(PeanutConstants.TAG_NAVI, "[NavigationImpl][stopInternal]");
        motionStop();
    }

    private void manualInternal(int direction) {
        PeanutSDK.getInstance().motor().hrc(null, false);
        switch (direction) {
            case 1:
                PeanutSDK.getInstance().motor().forward(null);
                break;
            case 2:
                PeanutSDK.getInstance().motor().backward(null);
                break;
            case 3:
                PeanutSDK.getInstance().motor().turnLeft(null);
                break;
            case 4:
                PeanutSDK.getInstance().motor().turnRight(null);
                break;
        }
    }

    private void releaseInternal() {
        LogUtils.d(PeanutConstants.TAG_NAVI, "[NavigationImpl][releaseInternal]");
        resetInternal();
        setState(0);
        while (!this.released) {
            try {
                wait();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        this.navigationThread.quit();
        synchronized (this) {
            this.released = true;
            notifyAll();
        }
    }

    private void resetInternal() {
        LogUtils.d(PeanutConstants.TAG_NAVI, "[NavigationImpl][resetInternal]");
        PeanutSDK.getInstance().unSubscribe(TopicName.NAVIGATION_STATUS, this.mStatusCallBack);
        this.handler.removeCallbacksAndMessages(null);
        arrivalSafely();
        this.pilotWhenReady = false;
        this.isTakeControl = false;
        this.index = 0;
        this.repeat = 0;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateNavigationInfo(NavigationStatusApi.Bean.DataBean data) {
        if (this.routeNodeRunning.getNavigationInfo().getRemainDistance().floatValue() > 0.0f && this.routeNodeRunning.getNavigationInfo().getRemainDistance().floatValue() != data.getRemain_length()) {
            notifyDistanceChanged(data.getRemain_length());
        }
        this.routeNodeRunning.getNavigationInfo().setStatus(Integer.valueOf(data.getStatus()));
        this.routeNodeRunning.getNavigationInfo().setSchedule(Integer.valueOf(data.getSchedule()));
        this.routeNodeRunning.getNavigationInfo().setDestinationDesc(data.getDesc());
        this.routeNodeRunning.getNavigationInfo().setRemainDistance(Float.valueOf(data.getRemain_length()));
        this.routeNodeRunning.getNavigationInfo().setRemainTime(Float.valueOf(data.getRemain_time()));
        this.routeNodeRunning.getNavigationInfo().setTotalDistance(Float.valueOf(data.getTotal_length()));
        this.routeNodeRunning.getNavigationInfo().setTotalTime(Float.valueOf(data.getTotal_time()));
        notifyNodeChanged();
    }

    private void motionPause() {
        PeanutSDK.getInstance().navigation().pause(new IDataCallback() { // from class: com.keenon.sdk.component.navigation.NavigationImpl.11
            @Override // com.keenon.sdk.external.IDataCallback
            public void success(String response) {
                ApiData data = (ApiData) GsonUtil.gson2Bean(response, ApiData.class);
                if (null != data && data.getStatus() == 0) {
                    LogUtils.d(NavigationImpl.TAG, "[motionPause][success]");
                    NavigationImpl.this.arrivalControl.release();
                    NavigationImpl.this.notifyFsm(4);
                }
            }

            @Override // com.keenon.sdk.external.IDataCallback
            public void error(ApiError error) {
                NavigationImpl.this.notifyError(error.getCode());
            }
        });
    }

    private void motionContinue() {
        PeanutSDK.getInstance().navigation().resume(new IDataCallback() { // from class: com.keenon.sdk.component.navigation.NavigationImpl.12
            @Override // com.keenon.sdk.external.IDataCallback
            public void success(String response) {
                ApiData data = (ApiData) GsonUtil.gson2Bean(response, ApiData.class);
                if (null != data && data.getStatus() == 0) {
                    LogUtils.d(NavigationImpl.TAG, "[motionContinue][success]");
                    NavigationImpl.this.notifyFsm(2);
                }
            }

            @Override // com.keenon.sdk.external.IDataCallback
            public void error(ApiError error) {
                NavigationImpl.this.notifyError(error.getCode());
            }
        });
    }

    private void motionStop() {
        PeanutSDK.getInstance().navigation().stop(new IDataCallback() { // from class: com.keenon.sdk.component.navigation.NavigationImpl.13
            @Override // com.keenon.sdk.external.IDataCallback
            public void success(String response) {
                ApiData data = (ApiData) GsonUtil.gson2Bean(response, ApiData.class);
                if (null != data && data.getStatus() == 0) {
                    LogUtils.d(NavigationImpl.TAG, "[motionStop][success]");
                }
            }

            @Override // com.keenon.sdk.external.IDataCallback
            public void error(ApiError error) {
                NavigationImpl.this.notifyError(error.getCode());
            }
        });
    }

    private void motionReset() {
        PeanutSDK.getInstance().navigation().reset(new IDataCallback() { // from class: com.keenon.sdk.component.navigation.NavigationImpl.14
            @Override // com.keenon.sdk.external.IDataCallback
            public void success(String response) {
                ApiData data = (ApiData) GsonUtil.gson2Bean(response, ApiData.class);
                if (null != data && data.getStatus() == 0) {
                    LogUtils.d(NavigationImpl.TAG, "[motionReset][success]");
                }
            }

            @Override // com.keenon.sdk.external.IDataCallback
            public void error(ApiError error) {
                LogUtils.e(NavigationImpl.TAG, "[motionReset][error][" + error.toString() + "]");
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleRawStatus(int status) {
        switch (status) {
            case 25:
            case ProtoDev.SENSOR_HEAD_MOTOR /* 66 */:
                notifyFsm(6);
                break;
            case NoResponseOption.SUPPRESS_ALL /* 26 */:
            case 67:
                notifyFsm(5);
                break;
            case 68:
                notifyFsm(3);
                break;
            case TFTP.DEFAULT_PORT /* 69 */:
                notifyFsm(10);
                break;
            case 73:
                if (this.state != 1) {
                    notifyFsm(0);
                }
                break;
            case 79:
                notifyFsm(9);
                break;
            case NavigationComponent.RESET /* 82 */:
                notifyFsm(2);
                break;
        }
    }

    @Override // com.keenon.sdk.component.navigation.common.Navigation
    public void cancelArriveControl() {
        this.arrivalControl.release();
    }

    @Override // com.keenon.sdk.component.navigation.common.Navigation
    public void resetNewTargets() {
        resetInternal();
    }

    @Override // com.keenon.sdk.component.navigation.common.Navigation
    public boolean isLastRepeat() {
        return !isInfiniteRepeat() && this.repeat + 1 >= this.routeLine.getRepeatCount();
    }

    @Override // com.keenon.sdk.component.navigation.common.Navigation
    public void arrivalControlEnable(boolean enable) {
        this.arrivalControl.enable(enable);
    }
}
