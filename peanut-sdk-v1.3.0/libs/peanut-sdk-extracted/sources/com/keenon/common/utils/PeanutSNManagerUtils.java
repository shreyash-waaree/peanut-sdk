package com.keenon.common.utils;

import android.os.Build;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/common/utils/PeanutSNManagerUtils.class */
public class PeanutSNManagerUtils {
    private static final String[] KEENON_RK3288_BOARD_CRITERIA = {"rockchip", "rk3288"};
    private static final String[] SMDT_DS830_BOARD_CRITERIA = {"allwinner", "octopus a83 f1"};
    private static final String[] SMDT_IOT180_BOARD_CRITERIA = {"allwinner", "quad-core r18 ads"};

    public static String getSN() {
        if (matchBoardCriteria(KEENON_RK3288_BOARD_CRITERIA)) {
            return getBoardWlanMac();
        }
        if (matchBoardCriteria(SMDT_DS830_BOARD_CRITERIA) || matchBoardCriteria(SMDT_IOT180_BOARD_CRITERIA)) {
            return getBoardLanMac();
        }
        return getBoardWlanMac();
    }

    private static boolean matchBoardCriteria(String[] criteria) {
        String manufacturer = Build.MANUFACTURER.toLowerCase();
        String model = Build.MODEL.toLowerCase();
        return manufacturer.equals(criteria[0]) && model.equals(criteria[1]);
    }

    /* JADX WARN: Code restructure failed: missing block: B:8:0x0034, code lost:
    
        r4 = r5.trim();
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct code enable 'Show inconsistent code' option in preferences
    */
    private static java.lang.String getBoardLanMac() {
        /*
            java.lang.String r0 = ""
            r4 = r0
            java.lang.String r0 = ""
            r5 = r0
            java.lang.Runtime r0 = java.lang.Runtime.getRuntime()     // Catch: java.io.IOException -> L3f
            java.lang.String r1 = "cat /sys/class/net/eth0/address "
            java.lang.Process r0 = r0.exec(r1)     // Catch: java.io.IOException -> L3f
            r6 = r0
            java.io.InputStreamReader r0 = new java.io.InputStreamReader     // Catch: java.io.IOException -> L3f
            r1 = r0
            r2 = r6
            java.io.InputStream r2 = r2.getInputStream()     // Catch: java.io.IOException -> L3f
            r1.<init>(r2)     // Catch: java.io.IOException -> L3f
            r7 = r0
            java.io.LineNumberReader r0 = new java.io.LineNumberReader     // Catch: java.io.IOException -> L3f
            r1 = r0
            r2 = r7
            r1.<init>(r2)     // Catch: java.io.IOException -> L3f
            r8 = r0
        L25:
            r0 = 0
            r1 = r5
            if (r0 == r1) goto L3c
            r0 = r8
            java.lang.String r0 = r0.readLine()     // Catch: java.io.IOException -> L3f
            r5 = r0
            r0 = r5
            if (r0 == 0) goto L25
            r0 = r5
            java.lang.String r0 = r0.trim()     // Catch: java.io.IOException -> L3f
            r4 = r0
            goto L3c
        L3c:
            goto L44
        L3f:
            r6 = move-exception
            r0 = r6
            r0.printStackTrace()
        L44:
            r0 = r4
            java.lang.String r0 = r0.toUpperCase()
            return r0
        */
        throw new UnsupportedOperationException("Method not decompiled: com.keenon.common.utils.PeanutSNManagerUtils.getBoardLanMac():java.lang.String");
    }

    /* JADX WARN: Code restructure failed: missing block: B:8:0x0034, code lost:
    
        r4 = r5.trim();
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct code enable 'Show inconsistent code' option in preferences
    */
    private static java.lang.String getBoardWlanMac() {
        /*
            java.lang.String r0 = ""
            r4 = r0
            java.lang.String r0 = ""
            r5 = r0
            java.lang.Runtime r0 = java.lang.Runtime.getRuntime()     // Catch: java.io.IOException -> L3f
            java.lang.String r1 = "cat /sys/class/net/wlan0/address "
            java.lang.Process r0 = r0.exec(r1)     // Catch: java.io.IOException -> L3f
            r6 = r0
            java.io.InputStreamReader r0 = new java.io.InputStreamReader     // Catch: java.io.IOException -> L3f
            r1 = r0
            r2 = r6
            java.io.InputStream r2 = r2.getInputStream()     // Catch: java.io.IOException -> L3f
            r1.<init>(r2)     // Catch: java.io.IOException -> L3f
            r7 = r0
            java.io.LineNumberReader r0 = new java.io.LineNumberReader     // Catch: java.io.IOException -> L3f
            r1 = r0
            r2 = r7
            r1.<init>(r2)     // Catch: java.io.IOException -> L3f
            r8 = r0
        L25:
            r0 = 0
            r1 = r5
            if (r0 == r1) goto L3c
            r0 = r8
            java.lang.String r0 = r0.readLine()     // Catch: java.io.IOException -> L3f
            r5 = r0
            r0 = r5
            if (r0 == 0) goto L25
            r0 = r5
            java.lang.String r0 = r0.trim()     // Catch: java.io.IOException -> L3f
            r4 = r0
            goto L3c
        L3c:
            goto L44
        L3f:
            r6 = move-exception
            r0 = r6
            r0.printStackTrace()
        L44:
            r0 = r4
            java.lang.String r0 = r0.toUpperCase()
            return r0
        */
        throw new UnsupportedOperationException("Method not decompiled: com.keenon.common.utils.PeanutSNManagerUtils.getBoardWlanMac():java.lang.String");
    }
}
