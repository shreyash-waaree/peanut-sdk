package com.keenon.sdk.component.charger.common;

import androidx.annotation.RequiresApi;
import java.io.Serializable;
import java.util.Objects;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/component/charger/common/ChargerInfo.class */
public class ChargerInfo implements Serializable {
    int power;
    int event;

    public int getPower() {
        return this.power;
    }

    public int getEvent() {
        return this.event;
    }

    public void setPower(int power) {
        this.power = power;
    }

    public void setEvent(int event) {
        this.event = event;
    }

    public String toString() {
        return "ChargerInfo{power=" + this.power + ", event=" + this.event + '}';
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ChargerInfo that = (ChargerInfo) o;
        return this.power == that.power && this.event == that.event;
    }

    @RequiresApi(api = 19)
    public int hashCode() {
        return Objects.hash(Integer.valueOf(this.power), Integer.valueOf(this.event));
    }
}
