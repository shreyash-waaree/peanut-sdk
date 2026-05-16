package com.keenon.sdk.sensor.door;

import com.keenon.sdk.scmIot.protopack.annotation.BeanFieldAno;
import java.util.Objects;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/sensor/door/DoorStatusInfo.class */
public class DoorStatusInfo {
    private int doorId;

    @BeanFieldAno(order = 1)
    private byte status;

    public DoorStatusInfo() {
    }

    public DoorStatusInfo(int doorId, byte status) {
        this.doorId = doorId;
        this.status = status;
    }

    public int getDoorId() {
        return this.doorId;
    }

    public void setDoorId(int doorId) {
        this.doorId = doorId;
    }

    public int getStatus() {
        return this.status & 255;
    }

    public boolean isOpened() {
        return 255 == getStatus();
    }

    public boolean isCLose() {
        return 0 == getStatus();
    }

    public void setStatus(byte status) {
        this.status = status;
    }

    public String toString() {
        return "DoorStatusInfo{doorId=" + this.doorId + ", status=" + getStatus() + '}';
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DoorStatusInfo that = (DoorStatusInfo) o;
        return this.doorId == that.doorId && this.status == that.status;
    }

    public int hashCode() {
        return Objects.hash(Integer.valueOf(this.doorId), Byte.valueOf(this.status));
    }
}
