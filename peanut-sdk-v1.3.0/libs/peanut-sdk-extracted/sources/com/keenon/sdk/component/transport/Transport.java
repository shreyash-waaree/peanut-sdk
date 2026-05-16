package com.keenon.sdk.component.transport;

import android.os.Environment;
import com.keenon.sdk.component.transport.bean.FileInfoBean;
import java.io.File;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/component/transport/Transport.class */
public interface Transport {
    public static final String PATH_UPLOAD_JSON_NAME = "path.tmp.json";
    public static final String PATH_UPLOAD_JSON_ZIP = "path.download.json.zip";
    public static final String PATH_DOWNLOAD_JSON_NAME = "path.tmp.json";
    public static final String PATH_DOWNLOAD_JSON_ZIP = "path.download.json.zip";
    public static final String FILE_BUMP = "map.keenon";
    public static final String PATH_MAP_FOLDER = Environment.getExternalStorageDirectory() + File.separator + "peanut" + File.separator + "sdk_map" + File.separator;
    public static final String UPLOAD_PATH = PATH_MAP_FOLDER + "upload" + File.separator;
    public static final String DOWNLOAD_PATH = PATH_MAP_FOLDER + "download" + File.separator;

    /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/component/transport/Transport$ITaskCallback.class */
    public interface ITaskCallback {
        void success(String str);

        void error(int i);

        void progress(int i);

        void info(FileInfoBean fileInfoBean);
    }
}
