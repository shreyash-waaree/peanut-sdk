package com.keenon.sdk.component.transport.bean;

import java.io.Serializable;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/component/transport/bean/FileInfoBean.class */
public class FileInfoBean implements Serializable {
    private int mode;
    private int type;
    private String md5;
    private long size;
    private String md5File;

    public String getMd5File() {
        return this.md5File;
    }

    public void setMd5File(String md5File) {
        this.md5File = md5File;
    }

    public int getMode() {
        return this.mode;
    }

    public void setMode(int mode) {
        this.mode = mode;
    }

    public int getType() {
        return this.type;
    }

    public void setType(int type) {
        this.type = type;
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

    public String toString() {
        return "FileInfoBean{mode=" + this.mode + ", type=" + this.type + ", md5='" + this.md5 + "', size=" + this.size + ", md5File=" + this.md5File + '}';
    }
}
