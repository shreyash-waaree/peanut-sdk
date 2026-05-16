package com.keenon.sdk.component.transport;

import com.keenon.sdk.component.transport.Transport;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/component/transport/PeanutTransport.class */
public class PeanutTransport {
    private static final String TAG = "PeanutTransport";
    public static final int UPLOAD_MAP = 0;
    public static final int UPLOAD_MAP_ZIP = 1;
    public static final int DOWNLOAD_MAP = 2;
    private String filePath;
    private int type;
    private String fileJsonString;
    private Transport.ITaskCallback callback;
    private TransportImpl mTransport;
    private String downloadPath;

    public String getFilePath() {
        return this.filePath == null ? "" : this.filePath;
    }

    public int getType() {
        return this.type;
    }

    public String getFileJsonString() {
        return this.fileJsonString == null ? "" : this.fileJsonString;
    }

    public String getDownloadPath() {
        return this.downloadPath;
    }

    public Transport.ITaskCallback getCallback() {
        return this.callback;
    }

    public void execute(Transport.ITaskCallback callback) {
        this.callback = callback;
        this.mTransport.execute(this);
    }

    public void cancel() {
        this.mTransport.cancel();
    }

    public PeanutTransport() {
        this(new Builder());
    }

    public PeanutTransport(Builder builder) {
        this.filePath = "";
        this.fileJsonString = "";
        this.downloadPath = "";
        this.mTransport = TransportImpl.getInstance();
        this.type = builder.type;
        this.filePath = builder.filePath;
        this.fileJsonString = builder.fileJsonString;
        this.downloadPath = builder.downloadPath;
    }

    /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/component/transport/PeanutTransport$Builder.class */
    public static class Builder {
        private String filePath;
        private int type;
        private String fileJsonString;
        private String downloadPath;

        public Builder setFilePath(String filePath) {
            this.filePath = filePath;
            return this;
        }

        public Builder setType(int type) {
            this.type = type;
            return this;
        }

        public Builder setFileJsonString(String fileJsonString) {
            this.fileJsonString = fileJsonString;
            return this;
        }

        public Builder setDownloadPath(String downloadPath) {
            this.downloadPath = downloadPath;
            return this;
        }

        public PeanutTransport build() {
            return new PeanutTransport(this);
        }
    }
}
