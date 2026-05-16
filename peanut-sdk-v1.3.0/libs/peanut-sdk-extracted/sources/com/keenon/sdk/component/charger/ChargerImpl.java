package com.keenon.sdk.component.charger;

import com.keenon.common.constant.PeanutConstants;
import com.keenon.common.error.PeanutError;
import com.keenon.common.utils.GsonUtil;
import com.keenon.common.utils.LogUtils;
import com.keenon.common.utils.PeanutMainThreadExecutor;
import com.keenon.sdk.api.BatteryStatusApi;
import com.keenon.sdk.component.charger.common.Charger;
import com.keenon.sdk.component.charger.common.ChargerFsm;
import com.keenon.sdk.component.charger.common.ChargerInfo;
import com.keenon.sdk.component.charger.utils.ChassisEventHelper;
import com.keenon.sdk.constant.TopicName;
import com.keenon.sdk.external.IDataCallback;
import com.keenon.sdk.external.PeanutSDK;
import com.keenon.sdk.hedera.model.ApiData;
import com.keenon.sdk.hedera.model.ApiError;
import com.keenon.sdk.scmIot.protopack.base.ProtoDev;
import java.util.concurrent.CopyOnWriteArraySet;
import org.eclipse.californium.core.coap.OptionNumberRegistry;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/component/charger/ChargerImpl.class */
public class ChargerImpl implements Charger, ChargerFsm.Listener {
    private static final String TAG = "ChargerImpl";
    public static final int EVENT_AUTO_CHARGE = 1;
    public static final int EVENT_MANUAL_CHARGE = 2;
    public static final int EVENT_CHARGE_ADAPTER = 3;
    public static final int EVENT_CANCEL = 4;
    public static final int EVENT_STATE_CHARGING = 5;
    public static final int EVENT_CHARGE_GIVE_UP = 6;
    public static final int EVENT_CHARGE_INTERRUPT = 7;
    public static final int EVENT_ADAPTER_CHARGE_CANCEL = 8;
    public static final int EVENT_CANCEL_SUCCESS = 9;
    private static volatile ChargerImpl sInstance;
    private PeanutCharger peanutCharger;
    private PeanutMainThreadExecutor mainThreadExecutor;
    private IDataCallback subscribeStatusCallBack = new IDataCallback() { // from class: com.keenon.sdk.component.charger.ChargerImpl.1
        @Override // com.keenon.sdk.external.IDataCallback
        public void success(String response) {
            LogUtils.i(PeanutConstants.TAG_CHARGE, "[ChargerImpl][subscribeBatteryStatusApi][response " + response + "]");
            BatteryStatusApi.Bean data = (BatteryStatusApi.Bean) GsonUtil.gson2Bean(response, BatteryStatusApi.Bean.class);
            if (data != null) {
                ChargerImpl.this.updateChargerInfo(data.getData());
            }
        }

        @Override // com.keenon.sdk.external.IDataCallback
        public void error(ApiError error) {
            ChargerImpl.this.notifyError(PeanutError.PM_BATTERY_STATUS_ERROR);
            LogUtils.e(PeanutConstants.TAG_CHARGE, "[ChargerImpl][subscribeBatteryStatusApi][ApiError " + error.getCode() + "]");
        }
    };
    private long lastTime = 0;
    private int state = 1;
    private CopyOnWriteArraySet<Charger.Listener> listeners = new CopyOnWriteArraySet<>();
    private ChargerFsm chargeFsm = new ChargerFsm();
    private ChargerInfo chargerInfo = new ChargerInfo();

    public ChargerImpl() {
        this.chargeFsm.addListener(this);
        this.mainThreadExecutor = PeanutSDK.getInstance().getMainThreadExecutor();
    }

    public static ChargerImpl getInstance() {
        if (sInstance == null) {
            synchronized (ChargerImpl.class) {
                if (sInstance == null) {
                    sInstance = new ChargerImpl();
                }
            }
        }
        return sInstance;
    }

    @Override // com.keenon.sdk.component.charger.common.Charger
    public void addListener(Charger.Listener listener) {
        this.listeners.add(listener);
    }

    @Override // com.keenon.sdk.component.charger.common.Charger
    public void removeListener(Charger.Listener listener) {
        this.listeners.remove(listener);
    }

    @Override // com.keenon.sdk.component.charger.common.Charger
    public void execute(PeanutCharger peanutCharger) {
        this.peanutCharger = peanutCharger;
        PeanutSDK.getInstance().unSubscribe(TopicName.BATTERY_STATUS, this.subscribeStatusCallBack);
        PeanutSDK.getInstance().subscribe(TopicName.BATTERY_STATUS, this.subscribeStatusCallBack);
    }

    @Override // com.keenon.sdk.component.charger.common.Charger
    public void release() {
        LogUtils.i(PeanutConstants.TAG_CHARGE, "[ChargerImpl][cancel]");
        this.chargeFsm.process(4);
        PeanutSDK.getInstance().unSubscribe(TopicName.BATTERY_STATUS, this.subscribeStatusCallBack);
        if (this.listeners != null) {
            for (Charger.Listener listener : this.listeners) {
                this.listeners.remove(listener);
            }
        }
        this.peanutCharger = null;
    }

    @Override // com.keenon.sdk.component.charger.common.Charger
    public void autoCharge() {
        LogUtils.d(PeanutConstants.TAG_CHARGE, "[ChargerImpl][realAutoCharge][Pile " + this.peanutCharger.getPile() + "]");
        PeanutSDK.getInstance().battery().autoCharge(new IDataCallback() { // from class: com.keenon.sdk.component.charger.ChargerImpl.2
            @Override // com.keenon.sdk.external.IDataCallback
            public void success(String response) {
                LogUtils.i(PeanutConstants.TAG_CHARGE, "[ChargerImpl][realAutoCharge][response " + response + "]");
                ApiData data = (ApiData) GsonUtil.gson2Bean(response, ApiData.class);
                if (null != data && data.getCode() != 0) {
                    if (data.getCode() == 35) {
                        ChargerImpl.this.notifyError(PeanutError.PM_PARAM_NO_DST);
                    } else if (data.getCode() == 81) {
                        ChargerImpl.this.notifyError(PeanutError.PM_NO_PATH);
                    } else {
                        if (data.getCode() == 19) {
                        }
                    }
                }
            }

            @Override // com.keenon.sdk.external.IDataCallback
            public void error(ApiError error) {
                LogUtils.e(PeanutConstants.TAG_CHARGE, "[ChargerImpl][realAutoCharge][error Code " + error.getCode() + "]");
                ChargerImpl.this.notifyError(PeanutError.PM_AUTO_CHARGED_FAILED);
            }
        }, this.peanutCharger.getPile());
    }

    @Override // com.keenon.sdk.component.charger.common.Charger
    public void manualCharge() {
        PeanutSDK.getInstance().battery().manualCharge(null);
    }

    @Override // com.keenon.sdk.component.charger.common.Charger
    public void cancelCharge() {
        PeanutSDK.getInstance().battery().stopCharge(new IDataCallback() { // from class: com.keenon.sdk.component.charger.ChargerImpl.3
            @Override // com.keenon.sdk.external.IDataCallback
            public void success(String result) {
                ChargerImpl.this.updateFsm(9);
            }

            @Override // com.keenon.sdk.external.IDataCallback
            public void error(ApiError error) {
            }
        });
    }

    @Override // com.keenon.sdk.component.charger.common.Charger
    public void adapterCharge() {
        PeanutSDK.getInstance().motor().enable(null, 0);
    }

    @Override // com.keenon.sdk.component.charger.common.Charger
    public void perform(int action) {
        LogUtils.i(PeanutConstants.TAG_CHARGE, "[ChargerImpl][perform][action " + action + "]");
        switch (action) {
            case 1:
                updateFsm(1);
                break;
            case 2:
                updateFsm(2);
                break;
            case 3:
                updateFsm(3);
                break;
            case 4:
                updateFsm(4);
                break;
        }
    }

    @Override // com.keenon.sdk.component.charger.common.ChargerFsm.Listener
    public void onStateSafely(int state) {
        LogUtils.i(PeanutConstants.TAG_CHARGE, "[ChargerImpl][onStateSafely][state " + this.state + " curState " + state + "]");
        if (this.state != state) {
            this.state = state;
            notifyStateChanged(state);
        }
    }

    @Override // com.keenon.sdk.component.charger.common.ChargerFsm.Listener
    public void onEventAction(int event) {
        handleEventAction(event);
    }

    private void handleEventAction(int event) {
        LogUtils.i(PeanutConstants.TAG_CHARGE, "[ChargerImpl][handleEventAction][event " + event + "]");
        switch (event) {
            case 1:
                autoCharge();
                break;
            case 2:
                manualCharge();
                break;
            case 3:
                adapterCharge();
                break;
            case 4:
                cancelCharge();
                break;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateFsm(int event) {
        LogUtils.i(PeanutConstants.TAG_CHARGE, "[ChargerImpl][updateFsm][event " + event + "]");
        if (null != this.chargeFsm) {
            this.chargeFsm.process(event);
        } else {
            notifyError(PeanutError.PM_FSM_NOT_SET);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateChargerInfo(BatteryStatusApi.Bean.DataBean dataBean) {
        if (dataBean == null || dataBean.getPower() <= 0) {
            return;
        }
        if (isTimeOut() || isDifferent(dataBean)) {
            this.chargerInfo.setPower(dataBean.getPower());
            this.chargerInfo.setEvent(dataBean.getEvent());
            notifyInfoChanged(this.chargerInfo);
            handleChargerEvent(dataBean.getEvent());
        }
    }

    private boolean isTimeOut() {
        long nowTime = System.currentTimeMillis();
        if (this.lastTime == 0 || nowTime - this.lastTime > 1000) {
            this.lastTime = nowTime;
            LogUtils.d(PeanutConstants.TAG_CHARGE, "[ChargerImpl][isTimeOut][isTimeOut true]");
            return true;
        }
        LogUtils.d(PeanutConstants.TAG_CHARGE, "[ChargerImpl][isTimeOut][isTimeOut false]");
        return false;
    }

    private boolean isDifferent(BatteryStatusApi.Bean.DataBean dataBean) {
        ChargerInfo bean = new ChargerInfo();
        bean.setEvent(dataBean.getEvent());
        bean.setPower(dataBean.getPower());
        LogUtils.d(PeanutConstants.TAG_CHARGE, "[ChargerImpl][isDifferent][isDifferent " + (!bean.equals(this.chargerInfo)) + "]");
        return !bean.equals(this.chargerInfo);
    }

    private synchronized void notifyStateChanged(final int state) {
        for (final Charger.Listener listener : this.listeners) {
            this.mainThreadExecutor.execute(new Runnable() { // from class: com.keenon.sdk.component.charger.ChargerImpl.4
                @Override // java.lang.Runnable
                public void run() {
                    listener.onChargerStatusChanged(state);
                }
            });
        }
    }

    private synchronized void notifyInfoChanged(final ChargerInfo chargerInfo) {
        LogUtils.i(PeanutConstants.TAG_CHARGE, "[ChargerImpl][notifyInfoChanged][chargerInfo " + chargerInfo.toString() + "]");
        for (final Charger.Listener listener : this.listeners) {
            this.mainThreadExecutor.execute(new Runnable() { // from class: com.keenon.sdk.component.charger.ChargerImpl.5
                @Override // java.lang.Runnable
                public void run() {
                    listener.onChargerInfoChanged(ChassisEventHelper.getChargeEvent(chargerInfo.getEvent()), chargerInfo);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public synchronized void notifyError(final int error) {
        for (final Charger.Listener listener : this.listeners) {
            this.mainThreadExecutor.execute(new Runnable() { // from class: com.keenon.sdk.component.charger.ChargerImpl.6
                @Override // java.lang.Runnable
                public void run() {
                    listener.onError(error);
                }
            });
        }
    }

    public void handleChargerEvent(int event) {
        LogUtils.d(PeanutConstants.TAG_CHARGE, "[ChargerImpl][handleChargerEvent][event " + event + "]");
        switch (event) {
            case 0:
                updateFsm(8);
                break;
            case 2:
                notifyError(PeanutError.PM_NO_MATCH_TO_CHARGING_PILE_TIMEOUT);
                break;
            case 3:
                notifyError(PeanutError.PM_MATCH_TO_CHARGING_PILE_AT_LEAST_ONE_TIME);
                break;
            case 4:
                notifyError(PeanutError.PM_NO_LABEL);
                break;
            case 5:
                notifyError(PeanutError.PM_NO_RADAR);
                break;
            case 6:
                notifyError(PeanutError.PM_NO_STM32);
                break;
            case 7:
                notifyError(PeanutError.PM_UNDEFINED_ERR);
                break;
            case 8:
                notifyError(PeanutError.PM_NO_5V);
                break;
            case 9:
                notifyError(PeanutError.PM_LOW_CURRENT);
                break;
            case 10:
                notifyError(PeanutError.PM_CHARGE_INTERRUPT);
                updateFsm(7);
                break;
            case 11:
                updateFsm(5);
                break;
            case 12:
                updateFsm(6);
                notifyError(PeanutError.PM_GIVE_UP_CHARGE);
                break;
            case OptionNumberRegistry.BLOCK1 /* 27 */:
                updateFsm(3);
                break;
            case 51:
                notifyError(PeanutError.PM_TIME_OUT);
                break;
            case ProtoDev.SENSOR_CALL_BELL /* 65 */:
                updateFsm(4);
                break;
        }
    }
}
