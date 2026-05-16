package com.keenon.sdk.sensor.plasma;

import com.keenon.sdk.scmIot.protopack.annotation.BeanFieldAno;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/sensor/plasma/PlasmaEnvironmentInfoResult.class */
public class PlasmaEnvironmentInfoResult {

    @BeanFieldAno(order = 1)
    private float ozone;

    @BeanFieldAno(order = 2)
    private float temperature;

    @BeanFieldAno(order = 3)
    private float humidity;

    @BeanFieldAno(order = 4)
    private short voc;

    @BeanFieldAno(order = 5)
    private short co2;

    @BeanFieldAno(order = 6)
    private short finDust;

    @BeanFieldAno(order = 7)
    private short airDust;

    public float getOzone() {
        return this.ozone;
    }

    public void setOzone(float ozone) {
        this.ozone = ozone;
    }

    public float getTemperature() {
        return this.temperature;
    }

    public void setTemperature(float temperature) {
        this.temperature = temperature;
    }

    public float getHumidity() {
        return this.humidity;
    }

    public void setHumidity(float humidity) {
        this.humidity = humidity;
    }

    public short getVoc() {
        return this.voc;
    }

    public void setVoc(short voc) {
        this.voc = voc;
    }

    public short getCo2() {
        return this.co2;
    }

    public void setCo2(short co2) {
        this.co2 = co2;
    }

    public short getFinDust() {
        return this.finDust;
    }

    public void setFinDust(short finDust) {
        this.finDust = finDust;
    }

    public short getAirDust() {
        return this.airDust;
    }

    public void setAirDust(short airDust) {
        this.airDust = airDust;
    }
}
