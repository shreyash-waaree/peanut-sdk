package com.keenon.sdk.sensor.ota.base;

import com.keenon.common.utils.StringUtils;
import java.io.File;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/sensor/ota/base/OTAConfig.class */
public class OTAConfig {
    private Device device;
    private File file;
    private String fileName;
    private String fileDirectory;
    private long fileSize;
    private String fileMD5;
    private String dependentVer;
    private String curVersion;
    private String newVersion;
    private boolean isForce;
    private OTAVersion version;

    /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/sensor/ota/base/OTAConfig$OTAVersion.class */
    public enum OTAVersion {
        V1,
        V2
    }

    public OTAConfig(Builder builder) {
        this.device = builder.device;
        this.file = builder.file;
        this.fileName = builder.fileName;
        this.fileDirectory = builder.fileDirectory;
        this.fileSize = builder.fileSize;
        this.fileMD5 = builder.fileMD5;
        this.dependentVer = builder.dependentVer;
        this.curVersion = builder.curVersion;
        this.newVersion = builder.newVersion;
        this.isForce = builder.isForce;
        this.version = builder.version;
    }

    public Device device() {
        return this.device;
    }

    public File file() {
        return this.file;
    }

    public String fileName() {
        return this.fileName;
    }

    public String fileDirectory() {
        return this.fileDirectory;
    }

    public long fileSize() {
        return this.fileSize;
    }

    public String fileMD5() {
        return this.fileMD5;
    }

    public String dependentVer() {
        return this.dependentVer;
    }

    public String curVersion() {
        return this.curVersion;
    }

    public String newVersion() {
        return this.newVersion;
    }

    public boolean isForce() {
        return this.isForce;
    }

    public OTAVersion otaVersion() {
        return this.version;
    }

    public Builder newBuilder() {
        return new Builder(this);
    }

    public String toString() {
        return "OTAConfig{device=" + this.device.toString() + ", file=" + this.file + ", fileName='" + this.fileName + "', fileDirectory='" + this.fileDirectory + "', fileSize=" + this.fileSize + ", fileMD5='" + this.fileMD5 + "', dependentVer='" + this.dependentVer + "', curVersion='" + this.curVersion + "', newVersion='" + this.newVersion + "', isForce=" + this.isForce + '}';
    }

    /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/sensor/ota/base/OTAConfig$Builder.class */
    public static final class Builder {
        private Device device;
        private File file;
        private String fileName;
        private String fileDirectory;
        private long fileSize;
        private String fileMD5;
        private String dependentVer;
        private String curVersion;
        private String newVersion;
        private boolean isForce;
        private OTAVersion version;

        public Builder() {
            this.version = OTAVersion.V1;
        }

        Builder(OTAConfig config) {
            this.version = OTAVersion.V1;
            this.device = config.device;
            this.file = config.file;
            this.fileName = config.fileName;
            this.fileDirectory = config.fileDirectory;
            this.fileSize = config.fileSize;
            this.fileMD5 = config.fileMD5;
            this.dependentVer = config.dependentVer;
            this.curVersion = config.curVersion;
            this.newVersion = config.newVersion;
            this.isForce = config.isForce;
            this.version = config.version;
        }

        public Builder withDevice(Device device) {
            if (device == null) {
                throw new NullPointerException("device == null");
            }
            this.device = device;
            return this;
        }

        public Builder withFile(File file) {
            this.file = file;
            return this;
        }

        public Builder withFileName(String fileName) {
            this.fileName = fileName;
            return this;
        }

        public Builder withFileDirectory(String fileDirectory) {
            this.fileDirectory = fileDirectory;
            return this;
        }

        public Builder withFileSize(long fileSize) {
            this.fileSize = fileSize;
            return this;
        }

        public Builder withFileMD5(String fileMD5) {
            if (StringUtils.isEmpty(fileMD5)) {
                throw new IllegalStateException("file md5 is empty");
            }
            this.fileMD5 = fileMD5;
            return this;
        }

        public Builder withDependentVer(String dependentVer) {
            this.dependentVer = dependentVer;
            return this;
        }

        public Builder withCurVersion(String curVersion) {
            this.curVersion = curVersion;
            return this;
        }

        public Builder withNewVersion(String newVersion) {
            this.newVersion = newVersion;
            return this;
        }

        public Builder isForce(boolean isForce) {
            this.isForce = isForce;
            return this;
        }

        public Builder withOTAVersion(OTAVersion version) {
            this.version = version;
            return this;
        }

        public OTAConfig build() {
            if (this.file == null && StringUtils.isEmpty(this.fileName) && StringUtils.isEmpty(this.fileDirectory)) {
                throw new NullPointerException("file == null");
            }
            return new OTAConfig(this);
        }
    }
}
