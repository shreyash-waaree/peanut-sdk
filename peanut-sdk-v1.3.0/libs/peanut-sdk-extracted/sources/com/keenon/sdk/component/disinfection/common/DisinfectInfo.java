package com.keenon.sdk.component.disinfection.common;

import java.io.Serializable;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/component/disinfection/common/DisinfectInfo.class */
public class DisinfectInfo implements Serializable {
    private int status;
    private int liquidPercent;
    private int liquidLevel;

    public DisinfectInfo() {
        this.liquidLevel = -1;
        this.liquidLevel = -1;
    }

    public int getStatus() {
        return this.status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public int getLiquidPercent() {
        return this.liquidPercent;
    }

    public void setLiquidPercent(int liquidPercent) {
        this.liquidPercent = liquidPercent;
    }

    public int getLiquidLevel() {
        return this.liquidLevel;
    }

    public void setLiquidLevel(int liquidLevel) {
        this.liquidLevel = liquidLevel;
    }

    public String toString() {
        return "DisinfectInfo{status=" + this.status + ", liquidPercent=" + this.liquidPercent + ", liquidLevel=" + this.liquidLevel + '}';
    }
}
