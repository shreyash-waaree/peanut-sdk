package com.keenon.sdk.component.transport;

import android.util.Base64;
import com.keenon.common.constant.PeanutConstants;
import com.keenon.common.error.PeanutError;
import com.keenon.common.utils.CheckUtils;
import com.keenon.common.utils.FileUtil;
import com.keenon.common.utils.GsonUtil;
import com.keenon.common.utils.LogUtils;
import com.keenon.common.utils.MathUtils;
import com.keenon.common.utils.ZipUtils;
import com.keenon.sdk.component.transport.Transport;
import com.keenon.sdk.component.transport.bean.FileInfo;
import com.keenon.sdk.component.transport.bean.FileInfoBean;
import com.keenon.sdk.external.IDataCallback;
import com.keenon.sdk.external.IProgressCallback;
import com.keenon.sdk.external.PeanutSDK;
import com.keenon.sdk.hedera.model.ApiData;
import com.keenon.sdk.hedera.model.ApiError;
import java.io.File;
import java.math.BigDecimal;
import org.eclipse.californium.core.coap.Request;
import org.json.JSONException;
import org.json.JSONObject;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/component/transport/TransportImpl.class */
public class TransportImpl implements Transport {
    public static final int MODE_UPLOAD = 0;
    public static final int MODE_DOWNLOAD = 1;
    public static final int MAP_TYPE_OFFICIAL = 0;
    public static final int MAP_TYPE_TEMP = 1;
    public static final int MAP_TYPE_PROCESS = 2;
    public static final int MINIMUM_ZIP_SIZE = 1024;
    public static final int FILE_BUMP_SIZE = 1024;
    private static TransportImpl sTransportImpl;
    private Request mRequest;
    private PeanutTransport mPeanutTransport;
    private Transport.ITaskCallback callback;
    private File mMapZip;
    private FileInfoBean mMapZipFileInfo;
    private long uploadStartTime = 0;
    private long downloadStartTime = 0;
    private long uploadZipSize = 0;
    IProgressCallback uploadCallback = new IProgressCallback() { // from class: com.keenon.sdk.component.transport.TransportImpl.1
        int mPercent = 0;

        @Override // com.keenon.sdk.external.IProgressCallback
        public void readyToSend(Request request) {
            TransportImpl.this.mRequest = request;
            LogUtils.d(PeanutConstants.TAG_TRANSPORT, "[TransportImpl][upload][readyToSend]");
        }

        @Override // com.keenon.sdk.external.IProgressCallback
        public void progress(int percent) {
            if (this.mPercent == 0) {
                this.mPercent = percent;
            } else if (this.mPercent == percent) {
                return;
            }
            this.mPercent = percent;
            LogUtils.i(PeanutConstants.TAG_TRANSPORT, "[TransportImpl][upload][uploadRobotMap onSendProgress " + this.mPercent + "]");
            TransportImpl.this.notifyProgress(this.mPercent);
        }

        @Override // com.keenon.sdk.external.IDataCallback
        public void success(String result) {
            TransportImpl.this.notifySuccess(result);
            ApiData bean = (ApiData) GsonUtil.gson2Bean(result, ApiData.class);
            if (bean.getStatus() == 1) {
                TransportImpl.this.notifyError(PeanutError.TRANSPORT_ERROR_ROBOT_CHECK_MD5_FAILED);
            }
            double elapsed = BigDecimal.valueOf((System.currentTimeMillis() - TransportImpl.this.uploadStartTime) / 1000.0f).setScale(2, 4).doubleValue();
            double fileLength = BigDecimal.valueOf(TransportImpl.this.uploadZipSize / 1024.0f).setScale(2, 4).doubleValue();
            double speed = new BigDecimal(((double) ((float) fileLength)) / elapsed).setScale(2, 4).doubleValue();
            LogUtils.d(PeanutConstants.TAG_TRANSPORT, "[TransportImpl][upload][uploadRobotMap file length: " + fileLength + "KB]");
            LogUtils.d(PeanutConstants.TAG_TRANSPORT, "[TransportImpl][upload][uploadRobotMap elapsed time: " + elapsed + "s]");
            LogUtils.d(PeanutConstants.TAG_TRANSPORT, "[TransportImpl][upload][uploadRobotMap average speed: " + speed + "KB/s]");
            LogUtils.d(PeanutConstants.TAG_TRANSPORT, "[TransportImpl][upload][uploadRobotMap average speed: " + speed + "KB/s]");
            LogUtils.i(PeanutConstants.TAG_TRANSPORT, "[TransportImpl][upload][result: " + result + "]");
        }

        @Override // com.keenon.sdk.external.IDataCallback
        public void error(ApiError error) {
            if (CheckUtils.isFastSpark()) {
                TransportImpl.this.notifyError(PeanutError.TRANSPORT_ERROR_TIMEOUT);
            }
            LogUtils.e(PeanutConstants.TAG_TRANSPORT, "[TransportImpl][upload][error : " + error.code + "]");
        }
    };

    public static TransportImpl getInstance() {
        if (sTransportImpl == null) {
            synchronized (TransportImpl.class) {
                if (sTransportImpl == null) {
                    sTransportImpl = new TransportImpl();
                }
            }
        }
        return sTransportImpl;
    }

    public void download(final long fileSize) {
        LogUtils.i(PeanutConstants.TAG_TRANSPORT, "[TransportImpl][download]");
        this.downloadStartTime = System.currentTimeMillis();
        PeanutSDK.getInstance().map().download(new IProgressCallback() { // from class: com.keenon.sdk.component.transport.TransportImpl.2
            int mPercent = 0;

            @Override // com.keenon.sdk.external.IProgressCallback
            public void readyToSend(Request request) {
                TransportImpl.this.mRequest = request;
                LogUtils.i(PeanutConstants.TAG_TRANSPORT, "[TransportImpl][download][readyToSend]");
            }

            @Override // com.keenon.sdk.external.IProgressCallback
            public void progress(int percent) {
                int realPercent = MathUtils.onCalculatePercent(fileSize, percent);
                if (this.mPercent == 0) {
                    this.mPercent = realPercent;
                } else if (this.mPercent == realPercent) {
                    return;
                }
                this.mPercent = realPercent;
                TransportImpl.this.notifyProgress(this.mPercent);
                LogUtils.i(PeanutConstants.TAG_TRANSPORT, "[TransportImpl][download][downloadRobotMap onSendProgress Size :" + percent + ", percent: " + this.mPercent + "]");
            }

            @Override // com.keenon.sdk.external.IDataCallback
            public void success(String result) {
                LogUtils.i(PeanutConstants.TAG_TRANSPORT, "[TransportImpl][download][result size: " + result.length() + "]");
                TransportImpl.this.notifySuccess(TransportImpl.this.handleDownloadFile(result));
                double elapsed = BigDecimal.valueOf((System.currentTimeMillis() - TransportImpl.this.downloadStartTime) / 1000.0f).setScale(2, 4).doubleValue();
                double fileLength = BigDecimal.valueOf(fileSize / 1024.0f).setScale(2, 4).doubleValue();
                double speed = new BigDecimal(((double) ((float) fileLength)) / elapsed).setScale(2, 4).doubleValue();
                LogUtils.d(PeanutConstants.TAG_TRANSPORT, "[TransportImpl][download][downloadRobotMap file length: " + fileLength + "KB]");
                LogUtils.d(PeanutConstants.TAG_TRANSPORT, "[TransportImpl][download][downloadRobotMap elapsed time: " + elapsed + "s]");
                LogUtils.d(PeanutConstants.TAG_TRANSPORT, "[TransportImpl][download][downloadRobotMap average speed: " + speed + "KB/s]");
            }

            @Override // com.keenon.sdk.external.IDataCallback
            public void error(ApiError error) {
                LogUtils.e(PeanutConstants.TAG_TRANSPORT, "[TransportImpl][download][error : " + error.code + "]");
                if (CheckUtils.isFastSpark()) {
                    TransportImpl.this.notifyError(PeanutError.TRANSPORT_ERROR_TIMEOUT);
                }
            }
        });
    }

    public void execute(PeanutTransport transport) {
        LogUtils.i(PeanutConstants.TAG_TRANSPORT, "[TransportImpl][execute][PeanutTransport]");
        if (transport == null) {
            LogUtils.e(PeanutConstants.TAG_TRANSPORT, "[TransportImpl][execute][transport is null]");
            return;
        }
        LogUtils.i(PeanutConstants.TAG_TRANSPORT, "[TransportImpl][execute][PeanutTransport : " + transport.toString() + "]");
        this.mPeanutTransport = transport;
        if (this.mRequest != null) {
            this.mRequest.cancel();
        }
        this.callback = transport.getCallback();
        if (transport.getType() != 2 && this.mPeanutTransport.getFileJsonString().isEmpty() && this.mPeanutTransport.getFilePath().isEmpty()) {
            LogUtils.e(PeanutConstants.TAG_TRANSPORT, "[TransportImpl][execute][FileJsonString and FilePath is null]");
            notifyError(PeanutError.TRANSPORT_ERROR_EMPTY_DATA);
            return;
        }
        if (2 == transport.getType()) {
            sendFileInfo(getRequestDownloadFileInfo(), transport);
            return;
        }
        if (1 == transport.getType()) {
            FileInfoBean fileInfoBeanZip = getRequestUploadFileInfoZIP(transport);
            if (fileInfoBeanZip != null) {
                sendFileInfo(fileInfoBeanZip, transport);
                return;
            } else {
                notifyError(PeanutError.TRANSPORT_ERROR_EMPTY_UPLOAD_FILE);
                return;
            }
        }
        if (0 == transport.getType()) {
            FileInfoBean requestUploadFileInfo = getRequestUploadFileInfo(transport);
            if (requestUploadFileInfo != null) {
                sendFileInfo(requestUploadFileInfo, transport);
            } else {
                notifyError(PeanutError.TRANSPORT_ERROR_EMPTY_UPLOAD_FILE);
            }
        }
    }

    private FileInfoBean getRequestUploadFileInfo(PeanutTransport transport) {
        LogUtils.d(PeanutConstants.TAG_TRANSPORT, "[TransportImpl][getRequestUploadFileInfo]");
        FileInfoBean bean = new FileInfoBean();
        File file = null;
        String fileData = transport.getFileJsonString();
        String filePath = transport.getFilePath();
        LogUtils.d(PeanutConstants.TAG_TRANSPORT, "[TransportImpl][getRequestUploadFileInfo][fileData: " + fileData + "]");
        LogUtils.d(PeanutConstants.TAG_TRANSPORT, "[TransportImpl][getRequestUploadFileInfo][filePath: " + filePath + "]");
        if (fileData != null && !fileData.isEmpty()) {
            FileUtil.deleteFile(new File(UPLOAD_PATH));
            FileUtil.mkDirIfNotExist(UPLOAD_PATH);
            FileUtil.writeFile(UPLOAD_PATH + "path.tmp.json", fileData);
            file = new File(UPLOAD_PATH + "path.tmp.json");
        } else if (filePath != null && !filePath.isEmpty()) {
            file = new File(filePath);
        }
        if (file != null) {
            LogUtils.d(PeanutConstants.TAG_TRANSPORT, "[TransportImpl][getRequestUploadFileInfo][file]");
            String fileMD5 = FileUtil.getFileMD5(file);
            LogUtils.d(PeanutConstants.TAG_TRANSPORT, "[TransportImpl][getRequestUploadFileInfo][md51]" + fileMD5);
            long fileSize = FileUtil.getFileSize(file);
            this.mMapZipFileInfo = new FileInfoBean();
            this.mMapZipFileInfo.setMode(0);
            this.mMapZipFileInfo.setType(0);
            this.mMapZipFileInfo.setSize(fileSize);
            this.mMapZipFileInfo.setMd5(fileMD5);
            bean.setMd5(fileMD5);
            bean.setSize(fileSize);
            bean.setMode(0);
            bean.setType(0);
            return bean;
        }
        notifyError(PeanutError.TRANSPORT_ERROR_EMPTY_FILE);
        LogUtils.e(PeanutConstants.TAG_TRANSPORT, "[TransportImpl][getRequestUploadFileInfo][file is null]");
        return null;
    }

    private FileInfoBean getRequestUploadFileInfoZIP(PeanutTransport transport) {
        LogUtils.d(PeanutConstants.TAG_TRANSPORT, "[TransportImpl][getRequestUploadFileInfoZIP]");
        FileInfoBean bean = new FileInfoBean();
        String fileData = transport.getFileJsonString();
        String filePath = transport.getFilePath();
        if (fileData != null && !fileData.isEmpty()) {
            if (!prepareUploadFileInfoJson(fileData)) {
                return null;
            }
        } else if (filePath != null && !filePath.isEmpty() && !prepareUploadFileInfoPath(filePath)) {
            return null;
        }
        if (this.mMapZipFileInfo != null) {
            LogUtils.d(PeanutConstants.TAG_TRANSPORT, "[TransportImpl][getRequestUploadFileInfo][zipInfo :" + this.mMapZipFileInfo.toString() + "]");
            bean.setMd5(this.mMapZipFileInfo.getMd5());
            bean.setSize(this.mMapZipFileInfo.getSize());
            bean.setMode(0);
            bean.setType(0);
            return bean;
        }
        notifyError(PeanutError.TRANSPORT_ERROR_EMPTY_ZIP);
        LogUtils.e(PeanutConstants.TAG_TRANSPORT, "[TransportImpl][getRequestUploadFileInfo][mMapZipFileInfo is null]");
        return null;
    }

    private FileInfoBean getRequestDownloadFileInfo() {
        LogUtils.d(PeanutConstants.TAG_TRANSPORT, "[TransportImpl][getRequestDownloadFileInfo]");
        FileInfoBean bean = new FileInfoBean();
        this.mMapZipFileInfo = new FileInfoBean();
        this.mMapZipFileInfo.setMode(1);
        this.mMapZipFileInfo.setType(0);
        bean.setMode(1);
        bean.setType(0);
        return bean;
    }

    private void sendFileInfo(FileInfoBean fileInfo, final PeanutTransport transport) {
        JSONObject messageObject = makeFileIfoMessage(fileInfo);
        LogUtils.d(PeanutConstants.TAG_TRANSPORT, "[TransportImpl][sendFileInfo][messageObject : " + messageObject.toString() + "]");
        PeanutSDK.getInstance().map().request(new IDataCallback() { // from class: com.keenon.sdk.component.transport.TransportImpl.3
            @Override // com.keenon.sdk.external.IDataCallback
            public void success(String result) {
                LogUtils.d(PeanutConstants.TAG_TRANSPORT, "[TransportImpl][sendFileInfo][result : " + result + "]");
                String jsonResult = GsonUtil.getData(result);
                FileInfo fileInfo2 = (FileInfo) GsonUtil.gson2Bean(jsonResult, FileInfo.class);
                FileInfoBean bean = null;
                if (fileInfo2 != null) {
                    bean = new FileInfoBean();
                    bean.setMode(fileInfo2.getType());
                    bean.setType(fileInfo2.getFileType());
                    bean.setSize(fileInfo2.getFileInfo().getSize());
                    bean.setMd5(fileInfo2.getFileInfo().getMd5());
                    bean.setMd5File(fileInfo2.getFileInfo().getMd5File());
                    if (bean.getMd5().isEmpty() || bean.getSize() == 0) {
                        LogUtils.e(PeanutConstants.TAG_TRANSPORT, "[TransportImpl][sendFileInfo][FileInfoBean : " + bean.toString() + "]");
                        TransportImpl.this.callback.info(bean);
                        return;
                    }
                }
                TransportImpl.this.handleFileInfo(bean, transport);
            }

            @Override // com.keenon.sdk.external.IDataCallback
            public void error(ApiError error) {
                LogUtils.e(PeanutConstants.TAG_TRANSPORT, "[TransportImpl][sendFileInfo][error : " + error.code + "]");
                TransportImpl.this.notifyError(error.code);
            }
        }, messageObject.toString().getBytes());
    }

    private JSONObject makeFileIfoMessage(FileInfoBean fileInfo) {
        LogUtils.d(PeanutConstants.TAG_TRANSPORT, "[TransportImpl][makeFileIfoMessage][FileInfoBean : " + fileInfo.toString() + "]");
        JSONObject messageObject = new JSONObject();
        try {
            JSONObject infoObject = new JSONObject();
            infoObject.put("size", fileInfo.getSize());
            infoObject.put("md5", fileInfo.getMd5());
            JSONObject dataObject = new JSONObject();
            dataObject.put("type", fileInfo.getMode());
            dataObject.put("fileType", fileInfo.getType());
            if (fileInfo.getMode() == 0) {
                dataObject.put("fileInfo", infoObject);
            }
            messageObject.put("action", "UpdateFileInfo");
            messageObject.put("messageId", System.currentTimeMillis());
            messageObject.put("data", dataObject);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return messageObject;
    }

    private boolean prepareUploadFileInfoJson(String json) {
        FileUtil.deleteFile(new File(UPLOAD_PATH));
        FileUtil.mkDirIfNotExist(UPLOAD_PATH);
        FileUtil.writeFile(UPLOAD_PATH + "path.tmp.json", json);
        if (!zipMapFiles(UPLOAD_PATH + "path.tmp.json", "path.download.json.zip", UPLOAD_PATH)) {
            LogUtils.e(PeanutConstants.TAG_TRANSPORT, "[TransportImpl][prepareUploadFileInfo][zip failed]");
            notifyError(PeanutError.TRANSPORT_ERROR_ZIP_FAILED);
            return false;
        }
        this.mMapZip = new File(UPLOAD_PATH + "path.download.json.zip");
        if (!this.mMapZip.exists()) {
            LogUtils.e(PeanutConstants.TAG_TRANSPORT, "[TransportImpl][prepareUploadFileInfo][zip path error]");
            notifyError(PeanutError.TRANSPORT_ERROR_ZIP_PATH_INVALID);
            return false;
        }
        if (requireBumpZip(this.mMapZip)) {
            LogUtils.d(PeanutConstants.TAG_TRANSPORT, "[TransportImpl][prepareUploadFileInfo][makeBumpZip...]");
            if (!makeBumpZip("path.download.json.zip", UPLOAD_PATH)) {
                notifyError(PeanutError.TRANSPORT_ERROR_ZIP_FAILED);
                LogUtils.e(PeanutConstants.TAG_TRANSPORT, "[TransportImpl][prepareUploadFileInfo][makeBumpZip error...]");
                return false;
            }
        }
        this.mMapZipFileInfo = new FileInfoBean();
        String fileMD5 = FileUtil.getFileMD5(this.mMapZip);
        long fileSize = FileUtil.getFileSize(this.mMapZip);
        this.mMapZipFileInfo.setMode(0);
        this.mMapZipFileInfo.setType(0);
        this.mMapZipFileInfo.setSize(fileSize);
        this.mMapZipFileInfo.setMd5(fileMD5);
        LogUtils.d(PeanutConstants.TAG_TRANSPORT, "[TransportImpl][prepareUploadFileInfo][file info = " + this.mMapZipFileInfo.toString() + "]");
        return true;
    }

    private boolean prepareUploadFileInfoPath(String filePath) {
        LogUtils.d(PeanutConstants.TAG_TRANSPORT, "[TransportImpl][prepareUploadFileInfo]");
        FileUtil.deleteFile(new File(UPLOAD_PATH));
        FileUtil.mkDirIfNotExist(UPLOAD_PATH);
        File file = new File(filePath);
        LogUtils.d(PeanutConstants.TAG_TRANSPORT, "[TransportImpl][prepareUploadFileInfo][file size : ]" + file.length());
        if (!zipMapFiles(filePath, "path.download.json.zip", UPLOAD_PATH)) {
            LogUtils.e(PeanutConstants.TAG_TRANSPORT, "[TransportImpl][prepareUploadFileInfo][zip failed]");
            notifyError(PeanutError.TRANSPORT_ERROR_ZIP_FAILED);
            return false;
        }
        this.mMapZip = new File(UPLOAD_PATH + "path.download.json.zip");
        if (!this.mMapZip.exists()) {
            LogUtils.e(PeanutConstants.TAG_TRANSPORT, "[TransportImpl][prepareUploadFileInfo][zip path error]");
            notifyError(PeanutError.TRANSPORT_ERROR_ZIP_PATH_INVALID);
            return false;
        }
        if (requireBumpZip(this.mMapZip)) {
            LogUtils.i(PeanutConstants.TAG_TRANSPORT, "[TransportImpl][prepareUploadFileInfo][makeBumpZip...]");
            if (!makeBumpZip("path.download.json.zip", UPLOAD_PATH)) {
                notifyError(PeanutError.TRANSPORT_ERROR_ZIP_FAILED);
                LogUtils.e(PeanutConstants.TAG_TRANSPORT, "[TransportImpl][prepareUploadFileInfo][makeBumpZip error...]");
                return false;
            }
        }
        this.mMapZipFileInfo = new FileInfoBean();
        String fileMD5 = FileUtil.getFileMD5(this.mMapZip);
        long fileSize = FileUtil.getFileSize(this.mMapZip);
        this.mMapZipFileInfo.setMode(0);
        this.mMapZipFileInfo.setType(0);
        this.mMapZipFileInfo.setSize(fileSize);
        this.mMapZipFileInfo.setMd5(fileMD5);
        LogUtils.i(PeanutConstants.TAG_TRANSPORT, "[TransportImpl][prepareUploadFileInfo][file info = " + this.mMapZipFileInfo.toString() + "]");
        return true;
    }

    public String handleDownloadFile(String jsonString) {
        byte[] bytes = Base64.decode(jsonString, 0);
        String path = this.mPeanutTransport.getDownloadPath();
        if (path == null || path.isEmpty()) {
            path = DOWNLOAD_PATH + "path.tmp.json";
        }
        LogUtils.d(PeanutConstants.TAG_TRANSPORT, "[TransportImpl][handleDownloadFile]");
        FileUtil.deleteFile(new File(DOWNLOAD_PATH));
        FileUtil.mkDirIfNotExist(DOWNLOAD_PATH);
        FileUtil.writeFile(DOWNLOAD_PATH + "path.download.json.zip", bytes);
        if (!FileUtil.isValidMd5(this.mMapZipFileInfo.getMd5(), DOWNLOAD_PATH + "path.download.json.zip")) {
            LogUtils.d(PeanutConstants.TAG_TRANSPORT, "[TransportImpl][prepareUploadFileInfo][md5 check failed]");
            notifyError(PeanutError.ERROR_CHECK_MD5_FAILED);
            return "";
        }
        if (!ZipUtils.unzip(DOWNLOAD_PATH + "path.download.json.zip", path)) {
            LogUtils.d(PeanutConstants.TAG_TRANSPORT, "[TransportImpl][prepareUploadFileInfo][unzip failed]");
            notifyError(PeanutError.TRANSPORT_ERROR_UNZIP_FAILED);
            return "";
        }
        File mapFile = new File(path);
        if (!mapFile.exists()) {
            LogUtils.d(PeanutConstants.TAG_TRANSPORT, "[TransportImpl][handleDownloadFile][file not exist]");
            notifyError(PeanutError.TRANSPORT_ERROR_UNZIP_FILE_INVALID);
            return "";
        }
        return path;
    }

    private boolean requireBumpZip(File zip) {
        LogUtils.d(PeanutConstants.TAG_TRANSPORT, "[TransportImpl][requireBumpZip]");
        return FileUtil.getFileSize(zip) < 1024;
    }

    private boolean makeBumpZip(String zipName, String zipPath) {
        boolean result = false;
        if (!FileUtil.createFile(zipPath + Transport.FILE_BUMP, 1024L, FileUtil.FileUnit.KB)) {
            LogUtils.i(PeanutConstants.TAG_TRANSPORT, "[TransportImpl][makeBumpZip][makeBumpZip stop, create file failed]");
            return false;
        }
        try {
            result = ZipUtils.ZipFolder(zipPath, zipName, zipPath);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (!result) {
            FileUtil.deleteFile(new File(zipPath + zipName));
            return false;
        }
        return true;
    }

    private boolean zipMapFiles(String sourcePath, String zipName, String zipPath) {
        LogUtils.d(PeanutConstants.TAG_TRANSPORT, "[TransportImpl][zipMapFiles]");
        boolean result = false;
        try {
            result = ZipUtils.ZipFolder(sourcePath, zipName, zipPath);
        } catch (Exception e) {
            LogUtils.e(PeanutConstants.TAG_TRANSPORT, "[TransportImpl][zipMapFiles][error:" + e.getMessage() + "]");
            e.printStackTrace();
        }
        if (!result) {
            LogUtils.d(PeanutConstants.TAG_TRANSPORT, "[TransportImpl][zipMapFiles][deleteFile]");
            FileUtil.deleteFile(new File(zipPath + zipName));
            return false;
        }
        return true;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleFileInfo(FileInfoBean fileInfoBean, PeanutTransport transport) {
        LogUtils.i(PeanutConstants.TAG_TRANSPORT, "[TransportImpl][handleFileInfo]");
        FileInfoBean mapFile = new FileInfoBean();
        if (fileInfoBean == null) {
            LogUtils.i(PeanutConstants.TAG_TRANSPORT, "[TransportImpl][handleFileInfo][fileInfoBean null]");
            mapFile = this.mMapZipFileInfo;
        } else {
            if (this.mMapZipFileInfo != null) {
                mapFile.setMode(this.mMapZipFileInfo.getMode());
                mapFile.setType(this.mMapZipFileInfo.getType());
            } else {
                mapFile.setMode(fileInfoBean.getMode());
                mapFile.setType(fileInfoBean.getType());
            }
            mapFile.setSize(fileInfoBean.getSize());
            mapFile.setMd5(fileInfoBean.getMd5());
            mapFile.setMd5File(fileInfoBean.getMd5File());
            this.mMapZipFileInfo = mapFile;
        }
        this.callback.info(mapFile);
        realTransmission(mapFile, transport);
        LogUtils.i(PeanutConstants.TAG_TRANSPORT, "[TransportImpl][handleFileInfo][mMapZipFileInfo :" + this.mMapZipFileInfo.toString() + "]");
    }

    private void realTransmission(FileInfoBean fileInfoBean, PeanutTransport transport) {
        LogUtils.d(PeanutConstants.TAG_TRANSPORT, "[TransportImpl][realTransmission][file info = " + fileInfoBean.toString() + "]");
        if (fileInfoBean.getMode() == 0) {
            if (fileInfoBean.getType() == 0) {
                handleUpload(transport, fileInfoBean.getSize());
            }
        } else if (fileInfoBean.getMode() == 1 && fileInfoBean.getType() == 0) {
            download(fileInfoBean.getSize());
        }
    }

    private void handleUpload(PeanutTransport transport, long fileSize) {
        LogUtils.d(PeanutConstants.TAG_TRANSPORT, "[TransportImpl][handleUpload]");
        if (transport == null) {
            LogUtils.d(PeanutConstants.TAG_TRANSPORT, "[TransportImpl][handleUpload][PeanutTransport is null]");
            return;
        }
        String fileJsonString = transport.getFileJsonString();
        String filePath = transport.getFilePath();
        if (1 == transport.getType()) {
            LogUtils.d(PeanutConstants.TAG_TRANSPORT, "[TransportImpl][handleUpload][UPLOAD_WEB_MAP_ZIP]");
            upload(1, FileUtil.getBytes(this.mMapZip), fileSize);
            return;
        }
        if (0 == transport.getType()) {
            LogUtils.d(PeanutConstants.TAG_TRANSPORT, "[TransportImpl][handleUpload][UPLOAD_MAP]");
            if (fileJsonString != null && !fileJsonString.isEmpty()) {
                LogUtils.d(PeanutConstants.TAG_TRANSPORT, "[TransportImpl][handleUpload][UPLOAD_MAP][fileJsonString]");
                upload(0, fileJsonString.getBytes(), fileSize);
            } else {
                if (filePath != null && !filePath.isEmpty()) {
                    LogUtils.d(PeanutConstants.TAG_TRANSPORT, "[TransportImpl][handleUpload][UPLOAD_MAP][filePath]");
                    byte[] data = FileUtil.getBytes(new File(filePath));
                    upload(0, data, fileSize);
                    return;
                }
                LogUtils.d(PeanutConstants.TAG_TRANSPORT, "[TransportImpl][handleUpload][PeanutTransport fileData and filePath is null]");
            }
        }
    }

    public void upload(int type, byte[] file, long fileSize) {
        LogUtils.d(PeanutConstants.TAG_TRANSPORT, "[TransportImpl][handleUpload][type : " + type + ", fileSize : " + fileSize + "]");
        this.uploadZipSize = fileSize;
        switch (type) {
            case 0:
                this.uploadStartTime = System.currentTimeMillis();
                PeanutSDK.getInstance().map().upload(this.uploadCallback, file);
                break;
            case 1:
                PeanutSDK.getInstance().map().uploadNew(this.uploadCallback, file);
                break;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void notifyError(int errorCode) {
        if (this.callback != null) {
            this.callback.error(errorCode);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void notifySuccess(String result) {
        if (this.callback != null) {
            this.callback.success(result);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void notifyProgress(int percent) {
        if (this.callback != null) {
            this.callback.progress(percent);
        }
    }

    public void cancel() {
        if (this.mRequest != null) {
            this.mRequest.cancel();
        }
    }
}
