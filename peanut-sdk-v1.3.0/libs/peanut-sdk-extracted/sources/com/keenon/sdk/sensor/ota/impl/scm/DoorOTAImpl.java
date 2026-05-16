package com.keenon.sdk.sensor.ota.impl.scm;

import com.keenon.common.error.PeanutError;
import com.keenon.common.utils.ByteUtils;
import com.keenon.common.utils.Dispatcher;
import com.keenon.common.utils.FileUtil;
import com.keenon.common.utils.GsonUtil;
import com.keenon.common.utils.LogUtils;
import com.keenon.common.utils.MathUtils;
import com.keenon.common.utils.StringUtils;
import com.keenon.sdk.api.SCMUpgradeLastPackApi;
import com.keenon.sdk.api.SCMUpgradePackInfoApi;
import com.keenon.sdk.api.SCMUpgradeResetApi;
import com.keenon.sdk.api.SCMUpgradeTransApi;
import com.keenon.sdk.api.door.DoorVersionCompatApi;
import com.keenon.sdk.api2.UpgradeActionApi;
import com.keenon.sdk.external.IDataCallback;
import com.keenon.sdk.hedera.model.ApiError;
import com.keenon.sdk.scmIot.protopack.base.OTAUnpack;
import com.keenon.sdk.sensor.ota.base.IOta;
import com.keenon.sdk.sensor.ota.base.OTAConfig;
import com.keenon.sdk.sensor.ota.impl.scm.AppPackInfo;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/sensor/ota/impl/scm/DoorOTAImpl.class */
public class DoorOTAImpl implements IOta {
    private static final String TAG = "DoorOTAImplV1";
    private static final int PACK_INFO_SIZE = 148;
    private static final int APP_PACK_SIZE = 256;
    private static final double APP_DOUBLE_PACK_SIZE = 256.0d;
    private static final int STATUS_CODE_QUERY_VERSION_ERROR = -10001;
    private static final int STATUS_CODE_CHECK_FW_ERROR = -10002;
    private static final int STATUS_CODE_RESET_ERROR = -10003;
    private static final int STATUS_CODE_SEND_PACK_INFO_ERROR = -10004;
    private static final int STATUS_CODE_SEND_APP_PACK_ERROR = -10005;
    private static final int STATUS_CODE_SEND_LAST_PACK_ERROR = -10006;
    private static final int STATUS_CODE_SEND_RUN_APP_ERROR = -10007;
    private static final int STATUS_CODE_CHECK_DEFAULT = 10000;
    private static final int STATUS_CODE_QUERY_VERSION_SUCCESS = 10001;
    private static final int STATUS_CODE_CHECK_FW_SUCCESS = 10002;
    private static final int STATUS_CODE_RESET_SUCCESS = 10003;
    private static final int STATUS_CODE_SEND_PACK_INFO_SUCCESS = 10004;
    private static final int STATUS_CODE_SEND_APP_PACK_SUCCESS = 10005;
    private static final int STATUS_CODE_SEND_LAST_PACK_SUCCESS = 10006;
    private static final int STATUS_CODE_RUN_APP_SUCCESS = 10007;
    private IOta.Listener mListener;
    private Dispatcher mDispatcher;
    private volatile OTAConfig mConfig;
    private volatile int mState;
    private volatile int mPreCheckStatus;
    private volatile int mPercent;
    private long mStartTimestamp;
    private byte[] mFileData;
    private PackDetail mPackDetail;
    private File mAppFile;
    private AppPackInfo mAppPackInfo;
    private IDataCallback mDoorVersionCallback = new IDataCallback() { // from class: com.keenon.sdk.sensor.ota.impl.scm.DoorOTAImpl.7
        @Override // com.keenon.sdk.external.IDataCallback
        public void success(String result) {
            DoorOTAImpl.this.log("queryVersion", result);
            DoorOTAImpl.this.mListener.onNewVersion(result);
        }

        @Override // com.keenon.sdk.external.IDataCallback
        public void error(ApiError error) {
            DoorOTAImpl.this.loge("查询版本失败", error.toString());
            DoorOTAImpl.this.notifyError(PeanutError.COAP_RESPONSE_ERROR);
        }
    };
    private IDataCallback mResetCallback = new IDataCallback() { // from class: com.keenon.sdk.sensor.ota.impl.scm.DoorOTAImpl.8
        @Override // com.keenon.sdk.external.IDataCallback
        public void success(String result) {
            DoorOTAImpl.this.log("接收Bootloader 状态", result);
            SCMUpgradeResetApi.Bean bean = (SCMUpgradeResetApi.Bean) GsonUtil.gson2Bean(result, SCMUpgradeResetApi.Bean.class);
            if (bean != null) {
                switch (bean.getState()) {
                    case 0:
                        DoorOTAImpl.this.log("固件进入Bootloader");
                        DoorOTAImpl.this.updateCheckStatus(10003, false);
                        break;
                    case 2:
                        if (0 == bean.getErrorCode()) {
                            DoorOTAImpl.this.updateCheckStatus(10007, false);
                        } else {
                            DoorOTAImpl.this.updateCheckStatus(DoorOTAImpl.STATUS_CODE_SEND_RUN_APP_ERROR, true);
                            DoorOTAImpl.this.notifyError(PeanutError.OTA_APP_EXCEPTION_INTO);
                        }
                        DoorOTAImpl.this.log("固件进入舱门APP,升级结束！");
                        break;
                }
            }
            DoorOTAImpl.this.notifyError(PeanutError.COAP_RESPONSE_ERROR);
            DoorOTAImpl.this.updateCheckStatus(DoorOTAImpl.STATUS_CODE_RESET_ERROR, true);
        }

        @Override // com.keenon.sdk.external.IDataCallback
        public void error(ApiError error) {
            DoorOTAImpl.this.loge("发送Reset失败", error.toString());
            DoorOTAImpl.this.notifyError(PeanutError.COAP_RESPONSE_ERROR);
            DoorOTAImpl.this.updateCheckStatus(DoorOTAImpl.STATUS_CODE_RESET_ERROR, true);
        }
    };
    private IDataCallback mPackInfoCallback = new IDataCallback() { // from class: com.keenon.sdk.sensor.ota.impl.scm.DoorOTAImpl.9
        @Override // com.keenon.sdk.external.IDataCallback
        public void success(String result) {
            DoorOTAImpl.this.log("发送固件包信息成功", result);
            SCMUpgradePackInfoApi.Bean bean = (SCMUpgradePackInfoApi.Bean) GsonUtil.gson2Bean(result, SCMUpgradePackInfoApi.Bean.class);
            if (bean == null) {
                DoorOTAImpl.this.notifyError(PeanutError.COAP_RESPONSE_ERROR);
                DoorOTAImpl.this.updateCheckStatus(DoorOTAImpl.STATUS_CODE_SEND_PACK_INFO_ERROR, true);
            } else if (bean.getCode() == 0) {
                DoorOTAImpl.this.updateCheckStatus(10004, false);
            } else {
                DoorOTAImpl.this.updateCheckStatus(DoorOTAImpl.STATUS_CODE_SEND_APP_PACK_ERROR, true);
            }
        }

        @Override // com.keenon.sdk.external.IDataCallback
        public void error(ApiError error) {
            DoorOTAImpl.this.loge("发送固件包信息失败", error.toString());
            DoorOTAImpl.this.notifyError(PeanutError.COAP_RESPONSE_ERROR);
            DoorOTAImpl.this.updateCheckStatus(DoorOTAImpl.STATUS_CODE_SEND_PACK_INFO_ERROR, true);
        }
    };
    private IDataCallback mAppPackCallback = new IDataCallback() { // from class: com.keenon.sdk.sensor.ota.impl.scm.DoorOTAImpl.10
        @Override // com.keenon.sdk.external.IDataCallback
        public void success(String result) {
            DoorOTAImpl.this.log("发送固件包数据成功", result);
            SCMUpgradeTransApi.Bean bean = (SCMUpgradeTransApi.Bean) GsonUtil.gson2Bean(result, SCMUpgradeTransApi.Bean.class);
            if (bean == null) {
                DoorOTAImpl.this.notifyError(PeanutError.COAP_RESPONSE_ERROR);
                DoorOTAImpl.this.updateCheckStatus(DoorOTAImpl.STATUS_CODE_SEND_APP_PACK_ERROR, true);
                return;
            }
            if (6 == bean.getCode()) {
                if (bean.getIndex() < DoorOTAImpl.this.mAppPackInfo.getByteArray().size()) {
                    int percent = MathUtils.onCalculatePercent(DoorOTAImpl.this.mAppPackInfo.getByteArray().size(), bean.getIndex());
                    DoorOTAImpl.this.notifyProgress(percent);
                    DoorOTAImpl.this.sendAppPack(DoorOTAImpl.this.mAppPackInfo.getByteArray().get(bean.getIndex()));
                    return;
                } else {
                    DoorOTAImpl.this.notifyProgress(100);
                    DoorOTAImpl.this.log("发送完毕固件包数据 " + DoorOTAImpl.this.mAppPackInfo.getByteArray().size() + "个");
                    DoorOTAImpl.this.updateCheckStatus(10005, false);
                    return;
                }
            }
            DoorOTAImpl.this.updateCheckStatus(DoorOTAImpl.STATUS_CODE_SEND_APP_PACK_ERROR, true);
        }

        @Override // com.keenon.sdk.external.IDataCallback
        public void error(ApiError error) {
            DoorOTAImpl.this.loge("发送固件包数据失败", error.toString());
            DoorOTAImpl.this.notifyError(PeanutError.COAP_RESPONSE_ERROR);
            DoorOTAImpl.this.updateCheckStatus(DoorOTAImpl.STATUS_CODE_SEND_APP_PACK_ERROR, true);
        }
    };
    private IDataCallback mLastPackCallback = new IDataCallback() { // from class: com.keenon.sdk.sensor.ota.impl.scm.DoorOTAImpl.11
        @Override // com.keenon.sdk.external.IDataCallback
        public void success(String result) {
            DoorOTAImpl.this.log("发送尾包数据成功", result);
        }

        @Override // com.keenon.sdk.external.IDataCallback
        public void error(ApiError error) {
            DoorOTAImpl.this.loge("发送固件包数据失败", error.toString());
            DoorOTAImpl.this.notifyError(PeanutError.COAP_RESPONSE_ERROR);
            DoorOTAImpl.this.updateCheckStatus(DoorOTAImpl.STATUS_CODE_SEND_LAST_PACK_ERROR, true);
        }
    };

    public DoorOTAImpl() {
        resetInternal();
        this.mDispatcher = new Dispatcher(null);
        this.mPackDetail = new PackDetail();
        this.mAppPackInfo = new AppPackInfo();
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
        notifyStateChanged(1);
        this.mStartTimestamp = System.currentTimeMillis();
        updateCheckStatus(10000, false);
    }

    @Override // com.keenon.sdk.sensor.ota.base.IOta
    public void stop() {
    }

    @Override // com.keenon.sdk.sensor.ota.base.IOta
    public OTAConfig getConfig() {
        return this.mConfig;
    }

    @Override // com.keenon.sdk.sensor.ota.base.IOta
    public void release() {
    }

    private void resetInternal() {
        this.mState = 0;
        this.mPreCheckStatus = 10000;
        this.mPercent = 0;
        this.mStartTimestamp = 0L;
        this.mPackDetail = null;
        this.mFileData = null;
        this.mAppFile = null;
        this.mAppPackInfo = null;
    }

    private void finish() {
        long elapse = System.currentTimeMillis() - this.mStartTimestamp;
        log("finish", "此次升级耗时: " + elapse + "ms");
        resetInternal();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public synchronized void updateCheckStatus(int statusCode, boolean isFinished) {
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
                return;
            }
            return;
        }
        switch (this.mPreCheckStatus) {
            case 10000:
            case 10001:
                checkConfig();
                return;
            case 10002:
                notifyStateChanged(2);
                reboot();
                break;
            case 10003:
                break;
            case 10004:
                sendAppPack(this.mAppPackInfo.getByteArray().get(0));
                return;
            case 10005:
                sendLastPack(this.mPackDetail.getFwMd5());
                return;
            case 10006:
            default:
                return;
            case 10007:
                notifyStateChanged(5);
                queryVersion();
                return;
        }
        sendPackInfoDetail();
    }

    private void queryVersion() {
        this.mDispatcher.enqueue(new Dispatcher.NodeCall("查询版本号", "", true, this.mDispatcher) { // from class: com.keenon.sdk.sensor.ota.impl.scm.DoorOTAImpl.1
            @Override // com.keenon.common.utils.Dispatcher.NodeCall
            protected void execute() {
                new DoorVersionCompatApi().send(DoorOTAImpl.this.mDoorVersionCallback);
            }

            @Override // com.keenon.common.utils.Dispatcher.NodeCall
            protected boolean interceptExecute() {
                return DoorOTAImpl.this.mPreCheckStatus < 10000;
            }
        });
    }

    private void checkConfig() {
        this.mDispatcher.enqueue(new Dispatcher.NodeCall("检查OTA配置", "", true, this.mDispatcher) { // from class: com.keenon.sdk.sensor.ota.impl.scm.DoorOTAImpl.2
            @Override // com.keenon.common.utils.Dispatcher.NodeCall
            protected void execute() {
                if (!DoorOTAImpl.this.checkFile() || !DoorOTAImpl.this.checkVersion()) {
                    DoorOTAImpl.this.updateCheckStatus(DoorOTAImpl.STATUS_CODE_CHECK_FW_ERROR, true);
                } else {
                    DoorOTAImpl.this.updateCheckStatus(10002, false);
                }
            }

            @Override // com.keenon.common.utils.Dispatcher.NodeCall
            protected boolean interceptExecute() {
                return DoorOTAImpl.this.mPreCheckStatus < 10000;
            }
        });
    }

    private void reboot() {
        this.mDispatcher.enqueue(new Dispatcher.NodeCall("Reset进入Bootloader", "", true, this.mDispatcher) { // from class: com.keenon.sdk.sensor.ota.impl.scm.DoorOTAImpl.3
            @Override // com.keenon.common.utils.Dispatcher.NodeCall
            protected void execute() {
                new SCMUpgradeResetApi().send(DoorOTAImpl.this.mResetCallback);
            }

            @Override // com.keenon.common.utils.Dispatcher.NodeCall
            protected boolean interceptExecute() {
                return DoorOTAImpl.this.mPreCheckStatus < 10000;
            }
        });
    }

    private void sendPackInfoDetail() {
        this.mDispatcher.enqueue(new Dispatcher.NodeCall("发送固件包信息", "", true, this.mDispatcher) { // from class: com.keenon.sdk.sensor.ota.impl.scm.DoorOTAImpl.4
            @Override // com.keenon.common.utils.Dispatcher.NodeCall
            protected void execute() {
                new SCMUpgradePackInfoApi().send(DoorOTAImpl.this.mPackInfoCallback, DoorOTAImpl.this.mPackDetail.getPackDetail());
            }

            @Override // com.keenon.common.utils.Dispatcher.NodeCall
            protected boolean interceptExecute() {
                return DoorOTAImpl.this.mPreCheckStatus < 10000;
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void sendAppPack(final byte[] params) {
        this.mDispatcher.enqueue(new Dispatcher.NodeCall("发送固件包", "", true, this.mDispatcher) { // from class: com.keenon.sdk.sensor.ota.impl.scm.DoorOTAImpl.5
            @Override // com.keenon.common.utils.Dispatcher.NodeCall
            protected void execute() {
                new SCMUpgradeTransApi().send(DoorOTAImpl.this.mAppPackCallback, params);
            }

            @Override // com.keenon.common.utils.Dispatcher.NodeCall
            protected boolean interceptExecute() {
                return DoorOTAImpl.this.mPreCheckStatus < 10000;
            }
        });
    }

    private void sendLastPack(final String md5) {
        this.mDispatcher.enqueue(new Dispatcher.NodeCall("发送尾包", "", true, this.mDispatcher) { // from class: com.keenon.sdk.sensor.ota.impl.scm.DoorOTAImpl.6
            @Override // com.keenon.common.utils.Dispatcher.NodeCall
            protected void execute() {
                new SCMUpgradeLastPackApi().send(DoorOTAImpl.this.mLastPackCallback, md5);
            }

            @Override // com.keenon.common.utils.Dispatcher.NodeCall
            protected boolean interceptExecute() {
                return DoorOTAImpl.this.mPreCheckStatus < 10000;
            }
        });
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
        this.mFileData = parsFile(this.mConfig.file());
        if (this.mFileData == null || this.mFileData.length == 0) {
            loge("checkFile", "解析Bin文件为空");
            notifyError(PeanutError.OTA_FW_FILE_INVALID);
            return false;
        }
        parsFileData(this.mFileData);
        if (!checkPackageInfo()) {
            loge("checkFile", "升级包binTail信息数据为空");
            notifyError(PeanutError.OTA_FW_PACK_INFO_NULL);
            return false;
        }
        if (!writeFwApp()) {
            return false;
        }
        if (!checkMD5()) {
            loge("checkFile", "固件MD5验证失败");
            notifyError(PeanutError.OTA_FW_MD5_NOT_MATCH);
            return false;
        }
        parsAppPackInfo();
        return true;
    }

    private boolean checkPackageInfo() {
        if (this.mPackDetail.getFwSize() > 0 && StringUtils.isNotEmpty(this.mPackDetail.getFwMd5()) && StringUtils.isNotEmpty(this.mPackDetail.getFwName()) && StringUtils.isNotEmpty(this.mPackDetail.getFwVersion())) {
            return true;
        }
        return false;
    }

    private boolean checkMD5() {
        return FileUtil.isValidMd5(this.mPackDetail.getFwMd5(), this.mAppFile);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean checkVersion() {
        return true;
    }

    private byte[] parsFile(File file) {
        byte[] bytes = null;
        try {
            bytes = new byte[(int) file.length()];
            DataInputStream read = new DataInputStream(new FileInputStream(file));
            read.read(bytes);
            read.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            loge("parsFile", "解析文件异常");
            notifyError(PeanutError.FileNotFoundException);
        } catch (IOException e2) {
            e2.printStackTrace();
            loge("parsFile", "解析文件异常");
            notifyError(PeanutError.IOException);
        }
        return bytes;
    }

    private PackDetail parsFileData(byte[] fileData) {
        log("parsFileData", "开始解析升级包信息");
        log("parsFileData", ByteUtils.bytesToHexStr(fileData));
        OTAUnpack unpack = new OTAUnpack(fileData, fileData.length - PACK_INFO_SIZE, PACK_INFO_SIZE);
        byte[] appBytes = new byte[fileData.length - PACK_INFO_SIZE];
        System.arraycopy(fileData, 0, appBytes, 0, fileData.length - PACK_INFO_SIZE);
        this.mPackDetail.setAppData(appBytes);
        this.mPackDetail.setFwSize(unpack.popInt().intValue());
        this.mPackDetail.setFwMd5(ByteUtils.bytesToHexStr(unpack.popFetch(16)));
        this.mPackDetail.setFwVersion(unpack.popVarstr(64));
        this.mPackDetail.setFwName(unpack.popVarstr(64));
        parsPackInfo(fileData);
        log("parsFileData", this.mPackDetail.toString());
        return this.mPackDetail;
    }

    private void parsPackInfo(byte[] fileData) {
        OTAUnpack unpack2 = new OTAUnpack(fileData, fileData.length - PACK_INFO_SIZE, PACK_INFO_SIZE);
        byte[] packSize = unpack2.popFetch(4);
        unpack2.popFetch(80);
        byte[] packName = unpack2.popFetch(64);
        byte[] packDetailBytes = new byte[69];
        System.arraycopy(packSize, 0, packDetailBytes, 0, 4);
        packDetailBytes[4] = 3;
        System.arraycopy(packName, 0, packDetailBytes, 5, packName.length);
        this.mPackDetail.setPackDetail(packDetailBytes);
    }

    private void parsAppPackInfo() {
        log("parsAppPackInfo", "处理APP固件分包");
        List<AppPackInfo.Bean> list = new ArrayList<>();
        byte[] appData = this.mPackDetail.getAppData();
        double doubleValue = ((double) appData.length) / APP_DOUBLE_PACK_SIZE;
        int size = (int) Math.ceil(doubleValue);
        for (int i = 0; i < size; i++) {
            AppPackInfo.Bean bean = new AppPackInfo.Bean();
            bean.setIndex(i);
            if (i == size - 1) {
                log("最后一包数据大小：" + (appData.length - (256 * (size - 1))));
                byte[] bytes = new byte[appData.length - (256 * (size - 1))];
                System.arraycopy(appData, 256 * (size - 1), bytes, 0, bytes.length);
                bean.setContent(bytes);
            } else {
                byte[] bytes2 = new byte[256];
                System.arraycopy(appData, i * 256, bytes2, 0, 256);
                bean.setContent(bytes2);
            }
            list.add(bean);
        }
        this.mAppPackInfo.setContentArray(list);
        log("parsAppPackInfo", "固件分包数量 ：" + list.size());
    }

    private boolean writeFwApp() {
        log("saveFwApp", "本地写入固件文件");
        String absolutePath = this.mConfig.file().getAbsolutePath() + "temp.bin";
        this.mAppFile = new File(absolutePath);
        try {
            if (!this.mAppFile.exists()) {
                this.mAppFile.createNewFile();
            }
            DataOutputStream write = new DataOutputStream(new FileOutputStream(this.mAppFile));
            write.write(this.mPackDetail.getAppData());
            write.close();
            if (this.mAppFile == null || this.mAppFile.length() == 0) {
                loge("saveFwApp", "写入固件文件为空");
                notifyError(PeanutError.OTA_FW_APP_FILE_NULL);
                return false;
            }
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            loge("saveFwApp", "写固件文件异常");
            notifyError(PeanutError.IOException);
            return false;
        }
    }

    private synchronized void notifyProgressChanged(int percent) {
        if (this.mPercent != percent && percent != 0) {
            log("notifyProgressChanged", percent);
            this.mPercent = percent;
            this.mListener.onProgress(this.mPercent);
        }
    }

    private synchronized void notifyStateChanged(int state) {
        log("notifyStateChanged", state);
        if (this.mState != state) {
            this.mState = state;
            this.mListener.onState(this.mState);
            if (this.mState == -1 || this.mState == 5 || this.mState == 0) {
                finish();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public synchronized void notifyError(int errorCode) {
        log("notifyError", errorCode);
        this.mListener.onError(errorCode);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public synchronized void notifyProgress(int progress) {
        log("notifyProgress", "progress :" + progress);
        this.mListener.onProgress(progress);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void log(String tag) {
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
