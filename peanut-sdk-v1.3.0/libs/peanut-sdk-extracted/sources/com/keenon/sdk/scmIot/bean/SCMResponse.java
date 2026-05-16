package com.keenon.sdk.scmIot.bean;

import com.keenon.sdk.hedera.model.ApiData;
import com.keenon.sdk.scmIot.protopack.base.ProtoHeader;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/scmIot/bean/SCMResponse.class */
public class SCMResponse<T> extends ApiData {
    private DataBean data;

    public DataBean getData() {
        return this.data;
    }

    public void setData(DataBean data) {
        this.data = data;
    }

    /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/scmIot/bean/SCMResponse$DataBean.class */
    public static class DataBean<T> {
        private int seq;
        private int dev;
        private int topic;
        private int type;
        private int cmd;
        private int status;
        private int length;
        private T data;

        public DataBean(ProtoHeader header) {
            this.seq = header.getSeq();
            this.dev = header.getDev();
            this.topic = header.getTopic();
            this.type = header.getType();
            this.cmd = header.getCmd();
            this.status = header.getStatus();
            this.length = header.getDataLength();
        }

        public int getSeq() {
            return this.seq;
        }

        public void setSeq(int seq) {
            this.seq = seq;
        }

        public int getDev() {
            return this.dev;
        }

        public void setDev(int dev) {
            this.dev = dev;
        }

        public int getTopic() {
            return this.topic;
        }

        public void setTopic(int topic) {
            this.topic = topic;
        }

        public int getType() {
            return this.type;
        }

        public void setType(int type) {
            this.type = type;
        }

        public int getCmd() {
            return this.cmd;
        }

        public void setCmd(int cmd) {
            this.cmd = cmd;
        }

        public int getStatus() {
            return this.status;
        }

        public void setStatus(int status) {
            this.status = status;
        }

        public int getLength() {
            return this.length;
        }

        public void setLength(int length) {
            this.length = length;
        }

        public T getData() {
            return this.data;
        }

        public void setData(T data) {
            this.data = data;
        }
    }
}
