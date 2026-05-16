package com.keenon.sdk.sensor.ota.impl.scm;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/sensor/ota/impl/scm/SCMCmd.class */
public class SCMCmd {
    public static final int CMDINFO = 4;
    public static final int CMDSTART = 96;
    public static final int CMDGET = 96;
    public static final int CMDSETP = 97;
    public static final int CMDCTL = 98;
    public static final int CMDBL = 32;
    public static final int BL_STATUS = 32;
    public static final int BL_REBOOT = 33;
    public static final int BL_RUN_APP = 34;
    public static final int BL_DATA_TRANSFER = 35;
    public static final int BL_DT_SOH = 1;
    public static final int BL_DT_STX = 2;
    public static final int BL_DT_EOT = 4;
    public static final int BL_DT_ACK = 6;
    public static final int BL_DT_NAK = 21;
    public static final int BL_DT_CA = 24;
    public static final int BL_STS_BL_START = 0;
    public static final int BL_STS_BL_WAITING = 1;
    public static final int BL_STS_APP_RUN = 2;
    public static final int BL_STS_APP_RUN_FAIL = 3;
}
