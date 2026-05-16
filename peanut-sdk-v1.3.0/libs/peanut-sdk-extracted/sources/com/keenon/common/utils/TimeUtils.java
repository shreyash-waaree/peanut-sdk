package com.keenon.common.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/common/utils/TimeUtils.class */
public class TimeUtils {
    public static String getTime() {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return format.format(new Date());
    }

    public static long getDistanceTime(String lastTime, String nowTime) {
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        long diff = 0;
        try {
            Date d1 = df.parse(nowTime);
            Date d2 = df.parse(lastTime);
            if (d1 == null || d2 == null) {
                return 0L;
            }
            long time1 = d1.getTime();
            long time2 = d2.getTime();
            if (time1 > time2) {
                diff = time1 - time2;
            }
            return diff / 1000;
        } catch (ParseException e) {
            return 0L;
        }
    }
}
