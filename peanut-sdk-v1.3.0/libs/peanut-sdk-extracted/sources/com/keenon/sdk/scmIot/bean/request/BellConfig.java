package com.keenon.sdk.scmIot.bean.request;

import com.keenon.sdk.scmIot.protopack.annotation.BeanFieldAno;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/scmIot/bean/request/BellConfig.class */
public class BellConfig {

    @BeanFieldAno(order = 1)
    private byte[] macAddress;

    @BeanFieldAno(order = 2)
    private byte audioEnable;

    @BeanFieldAno(order = 3)
    private byte audioVolume;

    @BeanFieldAno(order = 4)
    private byte audioSongid;

    @BeanFieldAno(order = 5)
    private byte ledEnable;

    @BeanFieldAno(order = 6)
    private byte ledBrightness;

    @BeanFieldAno(order = 7)
    private byte ledColor;

    @BeanFieldAno(order = 8)
    private byte ledTime;

    public byte[] getMacAddress() {
        return this.macAddress;
    }

    public void setMacAddress(byte[] macAddress) {
        this.macAddress = macAddress;
    }

    public byte getAudioEnable() {
        return this.audioEnable;
    }

    public void setAudioEnable(byte audioEnable) {
        this.audioEnable = audioEnable;
    }

    public byte getAudioVolume() {
        return this.audioVolume;
    }

    public void setAudioVolume(byte audioVolume) {
        this.audioVolume = audioVolume;
    }

    public byte getAudioSongid() {
        return this.audioSongid;
    }

    public void setAudioSongid(byte audioSongid) {
        this.audioSongid = audioSongid;
    }

    public byte getLedEnable() {
        return this.ledEnable;
    }

    public void setLedEnable(byte ledEnable) {
        this.ledEnable = ledEnable;
    }

    public byte getLedBrightness() {
        return this.ledBrightness;
    }

    public void setLedBrightness(byte ledBrightness) {
        this.ledBrightness = ledBrightness;
    }

    public byte getLedColor() {
        return this.ledColor;
    }

    public void setLedColor(byte ledColor) {
        this.ledColor = ledColor;
    }

    public byte getLedTime() {
        return this.ledTime;
    }

    public void setLedTime(byte ledTime) {
        this.ledTime = ledTime;
    }
}
