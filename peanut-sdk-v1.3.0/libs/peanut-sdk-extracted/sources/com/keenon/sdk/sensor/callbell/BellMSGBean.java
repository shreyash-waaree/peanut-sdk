package com.keenon.sdk.sensor.callbell;

import com.keenon.sdk.scmIot.bean.SCMRequest;
import java.util.Objects;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/sensor/callbell/BellMSGBean.class */
public class BellMSGBean {
    public int msgId;
    public String msgType;
    public SCMRequest request;

    public BellMSGBean(int msgId, SCMRequest request) {
        this.msgId = msgId;
        this.request = request;
    }

    public BellMSGBean(int msgId, String msgType, SCMRequest request) {
        this.msgId = msgId;
        this.msgType = msgType;
        this.request = request;
    }

    public BellMSGBean() {
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        BellMSGBean that = (BellMSGBean) o;
        return this.msgId == that.msgId;
    }

    public int hashCode() {
        return Objects.hash(Integer.valueOf(this.msgId));
    }
}
