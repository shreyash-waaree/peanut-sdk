package com.keenon.sdk.scmIot.bean.request;

import com.keenon.sdk.scmIot.protopack.annotation.BeanFieldAno;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/scmIot/bean/request/LEDRGBConfig.class */
public class LEDRGBConfig {

    @BeanFieldAno(order = 1)
    private int rgb;

    public int getRgb() {
        return this.rgb;
    }

    public void setRgb(int rgb) {
        this.rgb = rgb;
    }
}
