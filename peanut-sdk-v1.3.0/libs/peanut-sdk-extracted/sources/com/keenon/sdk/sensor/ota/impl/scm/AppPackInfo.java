package com.keenon.sdk.sensor.ota.impl.scm;

import com.keenon.common.utils.ByteUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/sensor/ota/impl/scm/AppPackInfo.class */
public class AppPackInfo {
    private List<Bean> contentArray;
    private List<byte[]> ByteArray;

    public List<Bean> getContentArray() {
        return this.contentArray;
    }

    public void setContentArray(List<Bean> contentArray) {
        if (this.ByteArray == null) {
            this.ByteArray = new ArrayList();
        }
        if (contentArray != null && contentArray.size() > 0) {
            for (Bean bean : contentArray) {
                byte[] index = ByteUtils.intToBytes(bean.getIndex());
                byte[] value = new byte[index.length + bean.getContent().length];
                System.arraycopy(index, 0, value, 0, index.length);
                System.arraycopy(bean.getContent(), 0, value, index.length, bean.getContent().length);
                this.ByteArray.add(value);
            }
        }
        this.contentArray = contentArray;
    }

    public List<byte[]> getByteArray() {
        return this.ByteArray;
    }

    public void setByteArray(List<byte[]> byteArray) {
        this.ByteArray = byteArray;
    }

    /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/sensor/ota/impl/scm/AppPackInfo$Bean.class */
    public static class Bean {
        private int index;
        private byte[] content;

        public int getIndex() {
            return this.index;
        }

        public void setIndex(int index) {
            this.index = index;
        }

        public byte[] getContent() {
            return this.content;
        }

        public void setContent(byte[] content) {
            this.content = content;
        }

        public String toString() {
            return "Bean{index=" + this.index + ", content=" + Arrays.toString(this.content) + '}';
        }
    }

    public String toString() {
        return "AppPackInfo{contentArray=" + this.contentArray + ", ByteArray=" + this.ByteArray + '}';
    }
}
