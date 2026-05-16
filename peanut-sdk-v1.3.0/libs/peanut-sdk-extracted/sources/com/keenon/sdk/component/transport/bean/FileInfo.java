package com.keenon.sdk.component.transport.bean;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/component/transport/bean/FileInfo.class */
public class FileInfo {
    private int type;
    private int fileType;
    private FileInfoBean fileInfo;

    public int getType() {
        return this.type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public int getFileType() {
        return this.fileType;
    }

    public void setFileType(int fileType) {
        this.fileType = fileType;
    }

    public FileInfoBean getFileInfo() {
        return this.fileInfo;
    }

    public void setFileInfo(FileInfoBean fileInfo) {
        this.fileInfo = fileInfo;
    }

    /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/component/transport/bean/FileInfo$FileInfoBean.class */
    public static class FileInfoBean {
        private String md5;
        private long size;
        private String md5File;

        public String getMd5File() {
            return this.md5File;
        }

        public void setMd5File(String md5File) {
            this.md5File = md5File;
        }

        public String getMd5() {
            return this.md5;
        }

        public void setMd5(String md5) {
            this.md5 = md5;
        }

        public long getSize() {
            return this.size;
        }

        public void setSize(long size) {
            this.size = size;
        }
    }
}
