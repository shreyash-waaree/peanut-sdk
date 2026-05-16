package com.keenon.sdk.sensor.callbell;

import java.util.Objects;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/sensor/callbell/RepeatMsgBean.class */
public class RepeatMsgBean {
    public int msgId;
    public int taskId;

    public RepeatMsgBean(int msgId, int taskId) {
        this.msgId = msgId;
        this.taskId = taskId;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        RepeatMsgBean that = (RepeatMsgBean) o;
        return this.msgId == that.msgId && this.taskId == that.taskId;
    }

    public int hashCode() {
        return Objects.hash(Integer.valueOf(this.msgId), Integer.valueOf(this.taskId));
    }
}
