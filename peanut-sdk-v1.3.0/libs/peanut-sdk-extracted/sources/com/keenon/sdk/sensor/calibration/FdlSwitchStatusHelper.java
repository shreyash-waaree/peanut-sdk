package com.keenon.sdk.sensor.calibration;

import android.os.Handler;
import android.os.Looper;
import com.keenon.common.utils.GsonUtil;
import com.keenon.sdk.api.AntiFallStatusApi;
import com.keenon.sdk.external.IDataCallback;
import com.keenon.sdk.external.IDataCallback2;
import com.keenon.sdk.hedera.model.ApiCode;
import com.keenon.sdk.hedera.model.ApiError;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/sensor/calibration/FdlSwitchStatusHelper.class */
public class FdlSwitchStatusHelper {
    public static final int TYPE_GET_STATUS = 1;
    public static final int TYPE_MODIFY_SWITCH = 2;
    private int mType;
    private int mRetryTimes;
    private IDataCallback2<Boolean> mCallback;
    private Handler mHandler = new Handler(Looper.getMainLooper());
    private IDataCallback mCallbackInner = new IDataCallback() { // from class: com.keenon.sdk.sensor.calibration.FdlSwitchStatusHelper.1
        @Override // com.keenon.sdk.external.IDataCallback
        public void success(String result) {
            AntiFallStatusApi.Bean bean = (AntiFallStatusApi.Bean) GsonUtil.gson2Bean(result, AntiFallStatusApi.Bean.class);
            if (null == bean.getData() || 22 != bean.getData().getCode()) {
                if (FdlSwitchStatusHelper.this.mRetryTimes < 3) {
                    FdlSwitchStatusHelper.this.start();
                    return;
                } else {
                    error(new ApiError(ApiCode.COAP_RESPONSE_TIMEOUT));
                    return;
                }
            }
            if (null != FdlSwitchStatusHelper.this.mCallback) {
                if (FdlSwitchStatusHelper.this.mType == 1) {
                    try {
                        FdlSwitchStatusHelper.this.mCallback.success(Boolean.valueOf(Boolean.parseBoolean(bean.getData().getMsg())));
                        return;
                    } catch (Exception e) {
                        e.printStackTrace();
                        error(new ApiError(ApiCode.COAP_RESPONSE_ERROR));
                        return;
                    }
                }
                if (FdlSwitchStatusHelper.this.mType == 2) {
                    FdlSwitchStatusHelper.this.mCallback.success(true);
                }
            }
        }

        @Override // com.keenon.sdk.external.IDataCallback
        public void error(ApiError error) {
            if (null != FdlSwitchStatusHelper.this.mCallback) {
                FdlSwitchStatusHelper.this.mCallback.error(error);
            }
        }
    };

    public FdlSwitchStatusHelper(int type) {
        this.mType = type;
    }

    public void getFdlSwitchStatus(IDataCallback2<Boolean> callback) {
        this.mCallback = callback;
        start();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void start() {
        this.mRetryTimes++;
        this.mHandler.postDelayed(() -> {
            SensorCalibration.getInstance().getAntiFallStatus(this.mCallbackInner);
        }, 1000L);
    }
}
