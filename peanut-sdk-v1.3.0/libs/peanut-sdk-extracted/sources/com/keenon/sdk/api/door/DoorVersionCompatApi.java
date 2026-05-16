package com.keenon.sdk.api.door;

import android.os.Handler;
import android.os.Looper;
import com.keenon.sdk.external.IDataCallback;
import com.keenon.sdk.hedera.model.ApiData;
import com.keenon.sdk.hedera.model.ApiError;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/door/DoorVersionCompatApi.class */
public class DoorVersionCompatApi {
    private IDataCallback callBack;
    private boolean isVersionBack = false;
    private Handler mHandler = new Handler(Looper.getMainLooper());
    private IDataCallback mOldVersionCallback = new IDataCallback() { // from class: com.keenon.sdk.api.door.DoorVersionCompatApi.2
        @Override // com.keenon.sdk.external.IDataCallback
        public void success(String result) {
            if (null != DoorVersionCompatApi.this.callBack) {
                DoorVersionCompatApi.this.callBack.success(result);
            }
        }

        @Override // com.keenon.sdk.external.IDataCallback
        public void error(ApiError error) {
            if (null != DoorVersionCompatApi.this.callBack) {
                DoorVersionCompatApi.this.callBack.error(error);
            }
        }
    };
    private IDataCallback mNewVersionCallback = new IDataCallback() { // from class: com.keenon.sdk.api.door.DoorVersionCompatApi.3
        @Override // com.keenon.sdk.external.IDataCallback
        public void success(String result) {
            DoorVersionCompatApi.this.isVersionBack = true;
            if (null != DoorVersionCompatApi.this.callBack) {
                DoorVersionCompatApi.this.callBack.success(result);
            }
        }

        @Override // com.keenon.sdk.external.IDataCallback
        public void error(ApiError error) {
            DoorVersionCompatApi.this.isVersionBack = true;
            if (null != DoorVersionCompatApi.this.callBack) {
                DoorVersionCompatApi.this.callBack.error(error);
            }
        }
    };

    public void send(IDataCallback callBack) {
        this.isVersionBack = false;
        this.callBack = callBack;
        this.mHandler.postDelayed(new Runnable() { // from class: com.keenon.sdk.api.door.DoorVersionCompatApi.1
            @Override // java.lang.Runnable
            public void run() {
                if (!DoorVersionCompatApi.this.isVersionBack) {
                    new DoorVersionOldApi().send(DoorVersionCompatApi.this.mOldVersionCallback);
                }
            }
        }, 1500L);
        new DoorVersionNewApi().send(this.mNewVersionCallback);
    }

    /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/door/DoorVersionCompatApi$Bean.class */
    public static class Bean extends ApiData {
        private VersionInfo data;

        public VersionInfo getData() {
            return this.data;
        }

        public void setData(VersionInfo data) {
            this.data = data;
        }
    }

    /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/door/DoorVersionCompatApi$VersionInfo.class */
    public static class VersionInfo {
        private String id;
        private String sw;
        private String hw;
        private String name;

        public String getId() {
            return this.id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getSw() {
            return this.sw;
        }

        public void setSw(String sw) {
            this.sw = sw;
        }

        public String getHw() {
            return this.hw;
        }

        public void setHw(String hw) {
            this.hw = hw;
        }

        public String getName() {
            return this.name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}
