package com.keenon.sdk.sensor.ota.impl;

import com.keenon.common.error.PeanutError;
import com.keenon.common.utils.Dispatcher;
import com.keenon.common.utils.FileUtil;
import com.keenon.common.utils.GsonUtil;
import com.keenon.common.utils.LogUtils;
import com.keenon.common.utils.StringUtils;
import com.keenon.sdk.api.DeviceVersionApi;
import com.keenon.sdk.api2.UpgradeActionApi;
import com.keenon.sdk.api2.UpgradeStatusApi;
import com.keenon.sdk.external.IDataCallback;
import com.keenon.sdk.external.PeanutSDK;
import com.keenon.sdk.hedera.model.ApiError;
import com.keenon.sdk.http.ws.PushManager;
import com.keenon.sdk.sensor.ota.base.IOta;
import com.keenon.sdk.sensor.ota.base.OTAConfig;
import com.keenon.sdk.tftp.TFTPServerManager;
import java.io.File;
import java.util.List;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/sensor/ota/impl/RosOTAImpl.class */
public class RosOTAImpl implements IOta {
    private static final String TAG = "RosOTAImpl";
    private static final int STATUS_CODE_QUERY_VERSION_ERROR = -10001;
    private static final int STATUS_CODE_SUB_STATUS_ERROR = -10002;
    private static final int STATUS_CODE_CHECK_FW_ERROR = -10003;
    private static final int STATUS_CODE_START_TFTP_ERROR = -10004;
    private static final int STATUS_CODE_CHECK_DEFAULT = 10000;
    private static final int STATUS_CODE_QUERY_VERSION_SUCCESS = 10001;
    private static final int STATUS_CODE_SUB_STATUS_SUCCESS = 10002;
    private static final int STATUS_CODE_CHECK_FW_SUCCESS = 10003;
    private static final int STATUS_CODE_START_TFTP_SUCCESS = 10004;
    private UpgradeActionApi.Package mPackage;
    private IOta.Listener mListener;
    private volatile OTAConfig mConfig;
    private volatile int mState;
    private volatile int mPreCheckStatus;
    private volatile int mPercent;
    private long mStartTimestamp;
    private IDataCallback mVersionCallback = new IDataCallback() { // from class: com.keenon.sdk.sensor.ota.impl.RosOTAImpl.5
        @Override // com.keenon.sdk.external.IDataCallback
        public void success(String result) {
            RosOTAImpl.this.log("queryVersion success", result);
            String curVersion = null;
            DeviceVersionApi.Bean bean = (DeviceVersionApi.Bean) GsonUtil.gson2Bean(result, DeviceVersionApi.Bean.class);
            if (bean != null && bean.getData() != null && bean.getData().getDevices() != null) {
                List<DeviceVersionApi.Bean.DataBean.DevicesBean> devices = bean.getData().getDevices();
                for (DeviceVersionApi.Bean.DataBean.DevicesBean device : devices) {
                    if (device.getId() == 11) {
                        curVersion = device.getSw();
                    }
                }
            }
            if (curVersion == null || curVersion.length() == 0) {
                RosOTAImpl.this.notifyError(PeanutError.OTA_UNKNOWN_VERSION);
                RosOTAImpl.this.updateCheckStatus(RosOTAImpl.STATUS_CODE_QUERY_VERSION_ERROR, true);
            } else {
                RosOTAImpl.this.mConfig = RosOTAImpl.this.mConfig.newBuilder().withCurVersion(curVersion).build();
                RosOTAImpl.this.updateCheckStatus(10001, false);
            }
        }

        @Override // com.keenon.sdk.external.IDataCallback
        public void error(ApiError error) {
            RosOTAImpl.this.loge("queryVersion error", error.toString());
            RosOTAImpl.this.notifyError(PeanutError.COAP_RESPONSE_ERROR);
            RosOTAImpl.this.updateCheckStatus(RosOTAImpl.STATUS_CODE_QUERY_VERSION_ERROR, true);
        }
    };
    private IDataCallback mStatusCallback = new IDataCallback() { // from class: com.keenon.sdk.sensor.ota.impl.RosOTAImpl.6
        @Override // com.keenon.sdk.external.IDataCallback
        public void success(String result) {
            if (!RosOTAImpl.this.isReleased()) {
                RosOTAImpl.this.log("subscribe upgrade status success", result);
                UpgradeStatusApi.Bean bean = (UpgradeStatusApi.Bean) GsonUtil.gson2Bean(result, UpgradeStatusApi.Bean.class);
                if (bean == null || bean.getData() == null) {
                    return;
                }
                if (bean.getCode() != 0) {
                    RosOTAImpl.this.notifyError(bean.getCode());
                    RosOTAImpl.this.updateCheckStatus(RosOTAImpl.STATUS_CODE_SUB_STATUS_ERROR, true);
                } else {
                    RosOTAImpl.this.updateCheckStatus(10002, false);
                }
                RosOTAImpl.this.notifyProgressChanged(bean.getData().getPercent());
                RosOTAImpl.this.handleDeviceUpgradeStatus(bean.getData().getStatus());
            }
        }

        @Override // com.keenon.sdk.external.IDataCallback
        public void error(ApiError error) {
            RosOTAImpl.this.loge("subscribe upgrade status error", error.toString());
            RosOTAImpl.this.notifyError(error.getCode());
            RosOTAImpl.this.updateCheckStatus(RosOTAImpl.STATUS_CODE_SUB_STATUS_ERROR, true);
        }
    };
    private Dispatcher mDispatcher = new Dispatcher(null);

    public RosOTAImpl() {
        resetInternal();
    }

    @Override // com.keenon.sdk.sensor.ota.base.IOta
    public void start(OTAConfig config, IOta.Listener listener) {
        if (this.mState != 0) {
            notifyError(PeanutError.OTA_IN_PROGRESS);
            return;
        }
        log(UpgradeActionApi.ACTION_START, "config: " + config.toString());
        this.mConfig = config;
        this.mListener = listener;
        this.mPackage = new UpgradeActionApi.Package();
        this.mStartTimestamp = System.currentTimeMillis();
        updateCheckStatus(10000, false);
    }

    @Override // com.keenon.sdk.sensor.ota.base.IOta
    public void stop() {
        stopInternal();
    }

    @Override // com.keenon.sdk.sensor.ota.base.IOta
    public OTAConfig getConfig() {
        return this.mConfig;
    }

    @Override // com.keenon.sdk.sensor.ota.base.IOta
    public void release() {
        finish();
    }

    private void resetInternal() {
        this.mState = 0;
        this.mPreCheckStatus = 10000;
        this.mPercent = 0;
        this.mStartTimestamp = 0L;
    }

    private void finish() {
        if (isReleased()) {
            return;
        }
        long elapse = System.currentTimeMillis() - this.mStartTimestamp;
        log("finish", "此次升级耗时: " + elapse + "ms");
        TFTPServerManager.getInstance().stop();
        unSubscribeStatus();
        resetInternal();
    }

    private boolean isForceUpgrade() {
        return this.mConfig.isForce();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean isReleased() {
        return this.mState == 0 && this.mPreCheckStatus == 10000;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public synchronized void updateCheckStatus(int statusCode, boolean isFinished) {
        if (this.mState != 0) {
            logw("updateCheckStatus", "非空闲状态");
        }
        log("updateCheckStatus", "statusCode: " + statusCode + ", isFinished: " + isFinished);
        if (this.mPreCheckStatus != 10000 && this.mPreCheckStatus == statusCode) {
            log("updateCheckStatus", "ignore statusCode: " + statusCode);
            return;
        }
        this.mPreCheckStatus = statusCode;
        if (this.mPreCheckStatus < 10000) {
            if (this.mDispatcher != null) {
                this.mDispatcher.release();
            }
            notifyStateChanged(-1);
            return;
        }
        if (isFinished) {
            if (this.mDispatcher != null) {
                this.mDispatcher.release();
            }
            if (this.mPreCheckStatus == 10004) {
                notifyStateChanged(1);
                return;
            } else {
                notifyStateChanged(-1);
                return;
            }
        }
        switch (this.mPreCheckStatus) {
            case 10000:
                if (isForceUpgrade()) {
                    updateCheckStatus(10001, false);
                } else {
                    queryVersion();
                }
                break;
            case 10001:
                queryStatus();
                break;
            case 10002:
                checkConfig();
                break;
            case 10003:
                startTFTPServer();
                break;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public synchronized void notifyProgressChanged(int percent) {
        if (this.mPercent != percent && percent != 0) {
            log("notifyProgressChanged", percent);
            this.mPercent = percent;
            this.mListener.onProgress(percent);
        }
    }

    private synchronized void notifyStateChanged(int state) {
        log("notifyStateChanged", state);
        if (this.mState != state) {
            this.mState = state;
            this.mListener.onState(this.mState);
            if (this.mState == 1) {
                startInternal();
            }
            if (this.mState == -1 || this.mState == 5 || this.mState == 0) {
                finish();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public synchronized void notifyError(int errorCode) {
        this.mListener.onError(errorCode);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleDeviceUpgradeStatus(int status) {
        switch (status) {
            case 1:
                notifyStateChanged(2);
                break;
            case 2:
                notifyStateChanged(3);
                break;
            case 3:
                notifyStateChanged(5);
                break;
            case 4:
                notifyStateChanged(0);
                break;
            case 5:
                notifyStateChanged(-1);
                break;
        }
    }

    private void updatePackage() {
        this.mPackage = new UpgradeActionApi.Package();
        this.mPackage.setObj(this.mConfig.device().name);
        this.mPackage.setUrl(TFTPServerManager.getInstance().getUri() + "/" + this.mConfig.fileName());
        this.mPackage.setMd5(this.mConfig.fileMD5());
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean checkFile() {
        OTAConfig.Builder builder = this.mConfig.newBuilder();
        if (this.mConfig.file() == null) {
            if (StringUtils.isNotEmpty(this.mConfig.fileDirectory()) && StringUtils.isNotEmpty(this.mConfig.fileName())) {
                File fwFile = new File(this.mConfig.fileDirectory() + File.separator + this.mConfig.fileName());
                builder.withFile(fwFile).withFileSize(fwFile.length());
            }
        } else {
            builder.withFileName(this.mConfig.file().getName()).withFileDirectory(this.mConfig.file().getParent()).withFileSize(this.mConfig.file().length());
        }
        this.mConfig = builder.build();
        if (this.mConfig.file() == null || !this.mConfig.file().exists() || this.mConfig.file().isDirectory()) {
            loge("checkFile", "文件不存在或不合法");
            notifyError(PeanutError.OTA_FW_NOT_EXIST);
            return false;
        }
        if (this.mConfig.fileSize() <= 0) {
            loge("checkFile", "文件大小为0");
            notifyError(PeanutError.OTA_FW_FILE_INVALID);
            return false;
        }
        if (StringUtils.isEmpty(this.mConfig.fileMD5())) {
            loge("checkFile", "MD5为空");
            notifyError(PeanutError.OTA_FW_MD5_EMPTY);
            return false;
        }
        if (!checkMD5()) {
            loge("checkFile", "MD5不匹配");
            notifyError(PeanutError.OTA_FW_MD5_NOT_MATCH);
            return false;
        }
        log("checkFile", this.mConfig.toString());
        return true;
    }

    private boolean checkMD5() {
        return FileUtil.isValidMd5(this.mConfig.fileMD5(), this.mConfig.file());
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean checkVersion() {
        if (isForceUpgrade()) {
            loge("checkVersion", "强制升级模式");
            return true;
        }
        if (StringUtils.isEmpty(this.mConfig.curVersion())) {
            loge("checkVersion", "当前版本号为空");
            notifyError(PeanutError.OTA_UNKNOWN_VERSION);
            return false;
        }
        if (!StringUtils.isEmpty(this.mConfig.dependentVer()) && StringUtils.compareVersion(this.mConfig.newVersion(), this.mConfig.dependentVer()) < 0) {
            loge("checkVersion", "固件版本低于依赖版本" + this.mConfig.dependentVer());
            notifyError(PeanutError.OTA_LOW_VERSION_DEPENDENCY);
            return false;
        }
        if (StringUtils.compareVersion(this.mConfig.newVersion(), this.mConfig.curVersion()) == 0) {
            notifyError(PeanutError.OTA_SAME_VERSION);
            return false;
        }
        return true;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean checkWS() {
        if (!PushManager.getInstance().isConnected()) {
            loge("checkWS", "websocket未连接");
            notifyError(PeanutError.WEBSOCKET_NOT_CONNECTED);
            return false;
        }
        return true;
    }

    private void queryVersion() {
        this.mDispatcher.enqueue(new Dispatcher.NodeCall("检查设备版本信息", "", true, this.mDispatcher) { // from class: com.keenon.sdk.sensor.ota.impl.RosOTAImpl.1
            @Override // com.keenon.common.utils.Dispatcher.NodeCall
            protected void execute() {
                new DeviceVersionApi().send(RosOTAImpl.this.mVersionCallback);
            }

            @Override // com.keenon.common.utils.Dispatcher.NodeCall
            protected boolean interceptExecute() {
                return RosOTAImpl.this.mPreCheckStatus < 10000;
            }
        });
    }

    private void queryStatus() {
        this.mDispatcher.enqueue(new Dispatcher.NodeCall("检查升级状态", "", true, this.mDispatcher) { // from class: com.keenon.sdk.sensor.ota.impl.RosOTAImpl.2
            @Override // com.keenon.common.utils.Dispatcher.NodeCall
            protected void execute() {
                PeanutSDK.getInstance().subscribe("UpgradeStatusApi", RosOTAImpl.this.mStatusCallback);
            }

            @Override // com.keenon.common.utils.Dispatcher.NodeCall
            protected boolean interceptExecute() {
                return RosOTAImpl.this.mPreCheckStatus < 10000;
            }
        });
    }

    private void unSubscribeStatus() {
        PeanutSDK.getInstance().unSubscribe("UpgradeStatusApi", this.mStatusCallback);
    }

    private void checkConfig() {
        this.mDispatcher.enqueue(new Dispatcher.NodeCall("检查OTA配置", "", true, this.mDispatcher) { // from class: com.keenon.sdk.sensor.ota.impl.RosOTAImpl.3
            @Override // com.keenon.common.utils.Dispatcher.NodeCall
            protected void execute() {
                if (!RosOTAImpl.this.checkFile() || !RosOTAImpl.this.checkVersion() || !RosOTAImpl.this.checkWS()) {
                    RosOTAImpl.this.updateCheckStatus(RosOTAImpl.STATUS_CODE_CHECK_FW_ERROR, true);
                } else {
                    RosOTAImpl.this.updateCheckStatus(10003, false);
                }
            }

            @Override // com.keenon.common.utils.Dispatcher.NodeCall
            protected boolean interceptExecute() {
                return RosOTAImpl.this.mPreCheckStatus < 10000;
            }
        });
    }

    private void startTFTPServer() {
        this.mDispatcher.enqueue(new Dispatcher.NodeCall("启动文件服务", "", true, this.mDispatcher) { // from class: com.keenon.sdk.sensor.ota.impl.RosOTAImpl.4
            @Override // com.keenon.common.utils.Dispatcher.NodeCall
            protected void execute() {
                if (TFTPServerManager.getInstance().init(RosOTAImpl.this.mConfig.fileDirectory())) {
                    RosOTAImpl.this.updateCheckStatus(10004, true);
                } else {
                    RosOTAImpl.this.updateCheckStatus(RosOTAImpl.STATUS_CODE_START_TFTP_ERROR, true);
                }
            }

            @Override // com.keenon.common.utils.Dispatcher.NodeCall
            protected boolean interceptExecute() {
                return RosOTAImpl.this.mPreCheckStatus < 10000;
            }
        });
    }

    private void startInternal() {
        updatePackage();
        doAction(UpgradeActionApi.ACTION_START, this.mPackage);
    }

    private void stopInternal() {
        doAction(UpgradeActionApi.ACTION_STOP, null);
    }

    private void doAction(final String action, UpgradeActionApi.Package data) {
        new UpgradeActionApi().send(new IDataCallback() { // from class: com.keenon.sdk.sensor.ota.impl.RosOTAImpl.7
            @Override // com.keenon.sdk.external.IDataCallback
            public void success(String result) {
                RosOTAImpl.this.log("doAction success-" + action, result);
            }

            @Override // com.keenon.sdk.external.IDataCallback
            public void error(ApiError error) {
                RosOTAImpl.this.log("doAction error-" + action, error.toString());
            }
        }, action, data);
    }

    private void log(String tag) {
        LogUtils.d(TAG, "[" + tag + "]");
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void log(String tag, String content) {
        LogUtils.d(TAG, "[" + tag + "][" + content + "]");
    }

    private void log(String tag, int content) {
        LogUtils.d(TAG, "[" + tag + "][" + content + "]");
    }

    private void loge(String tag) {
        LogUtils.e(TAG, "[" + tag + "]");
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void loge(String tag, String content) {
        LogUtils.e(TAG, "[" + tag + "][" + content + "]");
    }

    private void logw(String tag, String content) {
        LogUtils.w(TAG, "[" + tag + "][" + content + "]");
    }
}
