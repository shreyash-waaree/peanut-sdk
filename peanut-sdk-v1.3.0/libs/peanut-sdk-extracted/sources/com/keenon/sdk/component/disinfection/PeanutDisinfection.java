package com.keenon.sdk.component.disinfection;

import com.keenon.common.constant.PeanutConstants;
import com.keenon.common.error.PeanutError;
import com.keenon.common.utils.GsonUtil;
import com.keenon.common.utils.LogUtils;
import com.keenon.common.utils.PeanutMainThreadExecutor;
import com.keenon.sdk.api.DisinfectLiquidApi;
import com.keenon.sdk.api.DisinfectStatusApi;
import com.keenon.sdk.component.disinfection.common.DisinfectFsm;
import com.keenon.sdk.component.disinfection.common.DisinfectInfo;
import com.keenon.sdk.component.disinfection.common.Disinfection;
import com.keenon.sdk.constant.TopicName;
import com.keenon.sdk.external.IDataCallback;
import com.keenon.sdk.external.PeanutSDK;
import com.keenon.sdk.hedera.model.ApiError;
import java.util.ArrayList;
import java.util.List;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/component/disinfection/PeanutDisinfection.class */
public class PeanutDisinfection implements Disinfection, DisinfectFsm.Listener {
    private static final String TAG = "[PeanutDisinfection]";
    private static volatile PeanutDisinfection sInstance;
    private final List<Disinfection.Listener> mListeners = new ArrayList();
    private IDataCallback mStatusCallback = new IDataCallback() { // from class: com.keenon.sdk.component.disinfection.PeanutDisinfection.1
        @Override // com.keenon.sdk.external.IDataCallback
        public void success(String result) {
            DisinfectStatusApi.Bean data = (DisinfectStatusApi.Bean) GsonUtil.gson2Bean(result, DisinfectStatusApi.Bean.class);
            LogUtils.d(PeanutConstants.TAG_DISINFECT, "[PeanutDisinfection][mStatusCallback-onSuccess][bean = " + result + "]");
            if (data == null || data.getData() == null) {
                PeanutDisinfection.this.notifyError(PeanutError.COAP_RESPONSE_NULL_DATA);
            } else {
                PeanutDisinfection.this.handleRawStatus(data.getData().getStatus());
            }
        }

        @Override // com.keenon.sdk.external.IDataCallback
        public void error(ApiError error) {
            PeanutDisinfection.this.notifyError(error.getCode());
        }
    };
    private IDataCallback mLiquidCallback = new IDataCallback() { // from class: com.keenon.sdk.component.disinfection.PeanutDisinfection.2
        @Override // com.keenon.sdk.external.IDataCallback
        public void success(String result) {
            DisinfectLiquidApi.Bean data = (DisinfectLiquidApi.Bean) GsonUtil.gson2Bean(result, DisinfectLiquidApi.Bean.class);
            LogUtils.d(PeanutConstants.TAG_DISINFECT, "[PeanutDisinfection][mLiquidCallback-onSuccess][bean = " + result + "]");
            if (data == null || data.getData() == null) {
                PeanutDisinfection.this.notifyError(PeanutError.COAP_RESPONSE_NULL_DATA);
            } else {
                PeanutDisinfection.this.updateDisinfectInfo(data.getData());
            }
        }

        @Override // com.keenon.sdk.external.IDataCallback
        public void error(ApiError error) {
            PeanutDisinfection.this.notifyError(error.getCode());
        }
    };
    private int mState = 0;
    private int mTankState = 1;
    private DisinfectInfo mDisinfectInfo = new DisinfectInfo();
    private PeanutMainThreadExecutor mainThreadExecutor = PeanutSDK.getInstance().getMainThreadExecutor();
    private DisinfectFsm mDisinfectFsm = new DisinfectFsm();

    private PeanutDisinfection() {
        this.mDisinfectFsm.addListener(this);
    }

    public static PeanutDisinfection getInstance() {
        if (sInstance == null) {
            synchronized (PeanutDisinfection.class) {
                if (sInstance == null) {
                    sInstance = new PeanutDisinfection();
                }
            }
        }
        return sInstance;
    }

    @Override // com.keenon.sdk.component.disinfection.common.Disinfection
    public void addListener(Disinfection.Listener listener) {
        this.mListeners.add(listener);
    }

    @Override // com.keenon.sdk.component.disinfection.common.Disinfection
    public void removeListener(Disinfection.Listener listener) {
        this.mListeners.remove(listener);
    }

    @Override // com.keenon.sdk.component.disinfection.common.Disinfection
    public void start() {
        initData();
    }

    @Override // com.keenon.sdk.component.disinfection.common.Disinfection
    public void stop() {
        notifyFsm(0);
        enableCabinInternal(false);
        enableDrainageInternal(false);
    }

    @Override // com.keenon.sdk.component.disinfection.common.Disinfection
    public void release() {
        stopSubscribe();
        stop();
    }

    @Override // com.keenon.sdk.component.disinfection.common.Disinfection
    public void enableCabin(boolean enable) {
        enableCabinInternal(enable);
    }

    @Override // com.keenon.sdk.component.disinfection.common.Disinfection
    public void enableDrainage(boolean enable) {
        enableDrainageInternal(enable);
    }

    @Override // com.keenon.sdk.component.disinfection.common.Disinfection
    public void enableAtomizer(boolean enable) {
        notifyFsm(enable ? 1 : 4);
    }

    @Override // com.keenon.sdk.component.disinfection.common.Disinfection
    public void enableUltraviolet(boolean enable) {
        notifyFsm(enable ? 2 : 5);
    }

    @Override // com.keenon.sdk.component.disinfection.common.Disinfection
    public void setFanSpeed(int level) {
        setFanSpeedInternal(level);
    }

    @Override // com.keenon.sdk.component.disinfection.common.Disinfection
    public boolean isLowAtomizer() {
        return this.mTankState == 3;
    }

    @Override // com.keenon.sdk.component.disinfection.common.Disinfection
    public boolean isAtomizerEnable() {
        return this.mState == 3 || this.mState == 1;
    }

    @Override // com.keenon.sdk.component.disinfection.common.Disinfection
    public boolean isUltravioletEnable() {
        return this.mState == 3 || this.mState == 2;
    }

    @Override // com.keenon.sdk.component.disinfection.common.Disinfection
    public DisinfectInfo getInfo() {
        return this.mDisinfectInfo;
    }

    @Override // com.keenon.sdk.component.disinfection.common.Disinfection
    public int getState() {
        return this.mState;
    }

    @Override // com.keenon.sdk.component.disinfection.common.Disinfection
    public int getTankState() {
        return this.mTankState;
    }

    private void setState(int state) {
        LogUtils.d(PeanutConstants.TAG_DISINFECT, "[PeanutDisinfection][setState][state = " + state + "]");
        if (this.mState != state) {
            this.mState = state;
            handleState(state);
        }
    }

    private void setTankState(int state) {
        LogUtils.d(PeanutConstants.TAG_DISINFECT, "[PeanutDisinfection][setTankState][state = " + state + "]");
        if (this.mTankState != state) {
            this.mTankState = state;
            notifyStateChanged(state);
        }
    }

    @Override // com.keenon.sdk.component.disinfection.common.DisinfectFsm.Listener
    public void onStateSafely(int state) {
        setState(state);
    }

    private void initData() {
        initSubscribeTask();
    }

    private void initSubscribeTask() {
        stopSubscribe();
        startSubscribe();
    }

    private void startSubscribe() {
        PeanutSDK.getInstance().subscribe(TopicName.DISINFECT_STATUS, this.mStatusCallback);
        PeanutSDK.getInstance().subscribe(TopicName.DISINFECT_LIQUID, this.mLiquidCallback);
    }

    private void stopSubscribe() {
        PeanutSDK.getInstance().unSubscribe(TopicName.DISINFECT_STATUS, this.mStatusCallback);
        PeanutSDK.getInstance().unSubscribe(TopicName.DISINFECT_LIQUID, this.mLiquidCallback);
    }

    private void handleState(int state) {
        switch (state) {
            case 0:
            case 6:
                enableAtomizerInternal(false);
                enableUltravioletInternal(false);
                break;
            case 1:
                enableAtomizerInternal(true);
                enableUltravioletInternal(false);
                break;
            case 2:
            case 7:
                enableAtomizerInternal(false);
                enableUltravioletInternal(true);
                break;
            case 3:
                enableAtomizerInternal(true);
                enableUltravioletInternal(true);
                break;
        }
    }

    private synchronized void notifyStateChanged(final int state) {
        LogUtils.d(PeanutConstants.TAG_DISINFECT, "[PeanutDisinfection][notifyStateChanged][state = " + state + "]");
        for (final Disinfection.Listener listener : this.mListeners) {
            this.mainThreadExecutor.execute(new Runnable() { // from class: com.keenon.sdk.component.disinfection.PeanutDisinfection.3
                @Override // java.lang.Runnable
                public void run() {
                    listener.onStateChanged(state);
                }
            });
        }
    }

    private synchronized void notifyInfoChanged() {
        LogUtils.d(PeanutConstants.TAG_DISINFECT, "[PeanutDisinfection][notifyInfoChanged]");
        for (final Disinfection.Listener listener : this.mListeners) {
            this.mainThreadExecutor.execute(new Runnable() { // from class: com.keenon.sdk.component.disinfection.PeanutDisinfection.4
                @Override // java.lang.Runnable
                public void run() {
                    listener.onInfoChanged(PeanutDisinfection.this.mDisinfectInfo);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void notifyError(final int error) {
        LogUtils.d(PeanutConstants.TAG_DISINFECT, "[PeanutDisinfection][notifyError][error = " + error + "]");
        for (final Disinfection.Listener listener : this.mListeners) {
            this.mainThreadExecutor.execute(new Runnable() { // from class: com.keenon.sdk.component.disinfection.PeanutDisinfection.5
                @Override // java.lang.Runnable
                public void run() {
                    listener.onError(error);
                }
            });
        }
    }

    private void notifyFsm(int event) {
        LogUtils.d(PeanutConstants.TAG_DISINFECT, "[PeanutDisinfection][notifyFsm][event = " + event + "]");
        if (null != this.mDisinfectFsm) {
            this.mDisinfectFsm.process(event);
        } else {
            notifyError(PeanutError.NAVI_NO_FSM);
        }
    }

    private void enableCabinInternal(boolean enable) {
        PeanutSDK.getInstance().disinfect().enableCabin(null, enable);
    }

    private void enableDrainageInternal(boolean enable) {
        PeanutSDK.getInstance().disinfect().enableDrainage(null, enable);
    }

    private void enableAtomizerInternal(boolean enable) {
        PeanutSDK.getInstance().disinfect().enableAtomizer(null, enable);
    }

    private void enableUltravioletInternal(boolean enable) {
        PeanutSDK.getInstance().disinfect().enableUltraviolet(null, enable);
    }

    private void setFanSpeedInternal(int level) {
        PeanutSDK.getInstance().disinfect().setFanSpeedLevel(null, level);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleRawStatus(int status) {
        LogUtils.d(PeanutConstants.TAG_DISINFECT, "[PeanutDisinfection][handleRawStatus][status = " + status + "]");
        this.mDisinfectInfo.setStatus(status);
        switch (status) {
            case 4:
                setTankState(2);
                break;
            case 6:
                setTankState(3);
                notifyFsm(6);
                break;
            case 10:
                setTankState(1);
                notifyFsm(7);
                break;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateDisinfectInfo(DisinfectLiquidApi.Bean.DataBean data) {
        if (this.mDisinfectInfo.getLiquidLevel() != data.getLevel()) {
            this.mDisinfectInfo.setLiquidLevel(data.getLevel());
            this.mDisinfectInfo.setLiquidPercent(data.getPercent());
            notifyInfoChanged();
        }
    }
}
