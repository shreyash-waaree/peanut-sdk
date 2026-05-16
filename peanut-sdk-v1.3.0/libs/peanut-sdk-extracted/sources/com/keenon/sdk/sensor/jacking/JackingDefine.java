package com.keenon.sdk.sensor.jacking;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/sensor/jacking/JackingDefine.class */
public class JackingDefine {

    /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/sensor/jacking/JackingDefine$JackingState.class */
    public @interface JackingState {
        public static final byte BOTTOM = 0;
        public static final byte TOP = -1;
        public static final byte RUN = 1;
        public static final byte FAULT = 2;
    }
}
