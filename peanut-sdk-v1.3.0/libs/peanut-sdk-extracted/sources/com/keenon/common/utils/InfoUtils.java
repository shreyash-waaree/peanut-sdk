package com.keenon.common.utils;

import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Point;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.os.SystemClock;
import android.text.format.Formatter;
import android.util.DisplayMetrics;
import android.view.WindowManager;
import com.keenon.common.constant.PeanutConstants;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/common/utils/InfoUtils.class */
public class InfoUtils {
    private static final String TAG_BASE = "[BASE_INFO]";
    public static Context con;
    private static String baseInfo = null;
    private static SimpleDateFormat format;

    public static String getScreenProp(Context con2) {
        WindowManager windowManager = (WindowManager) con2.getSystemService("window");
        Point point = new Point();
        windowManager.getDefaultDisplay().getRealSize(point);
        DisplayMetrics dm = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(dm);
        double x = Math.pow(point.x / dm.xdpi, 2.0d);
        double y = Math.pow(point.y / dm.ydpi, 2.0d);
        double screenInches = Math.sqrt(x + y);
        return "Screen inches: " + String.format("%.2f", Double.valueOf(screenInches)) + ", widthPixels: " + dm.widthPixels + ", heightPixels: " + dm.heightPixels + ", density: " + dm.density;
    }

    public static String getRAMUsage(Context con2) {
        ActivityManager activityManager = (ActivityManager) con2.getApplicationContext().getSystemService("activity");
        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        activityManager.getMemoryInfo(memoryInfo);
        long totalMem = memoryInfo.totalMem;
        long availableMem = memoryInfo.availMem;
        String totalSize = Formatter.formatFileSize(con2.getApplicationContext(), totalMem);
        String availableSize = Formatter.formatFileSize(con2.getApplicationContext(), availableMem);
        return availableSize + "/" + totalSize;
    }

    public static String getSdcardUsage(Context con2) {
        File path = Environment.getExternalStorageDirectory();
        StatFs stat = new StatFs(path.getPath());
        long blockCount = stat.getBlockCount();
        long blockSize = stat.getBlockSize();
        long availableBlocks = stat.getAvailableBlocks();
        String totalSize = Formatter.formatFileSize(con2.getApplicationContext(), blockCount * blockSize);
        String availableSize = Formatter.formatFileSize(con2.getApplicationContext(), availableBlocks * blockSize);
        return availableSize + "/" + totalSize;
    }

    public static String getTimeTag() {
        if (format == null) {
            format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        }
        return String.format("[%s] ", format.format(new Date(System.currentTimeMillis())));
    }

    public static String getFormattedAppLogInfo(ApplogInfo applogInfo) {
        if (baseInfo == null) {
            StringBuilder sb = new StringBuilder();
            sb.append("\r\n");
            sb.append(getTimeTag() + "[BASE_INFO]" + applogInfo.productName);
            sb.append("\r\n");
            sb.append(getTimeTag() + "[BASE_INFO]" + applogInfo.cpuAbiList);
            sb.append("\r\n");
            sb.append(getTimeTag() + "[BASE_INFO]" + applogInfo.osReleaseVersion);
            sb.append("\r\n");
            sb.append(getTimeTag() + "[BASE_INFO]" + applogInfo.buildVersion);
            sb.append("\r\n");
            sb.append(getTimeTag() + "[BASE_INFO]" + applogInfo.romSize);
            sb.append("\r\n");
            sb.append(getTimeTag() + "[BASE_INFO]" + applogInfo.sdcardSize);
            sb.append("\r\n");
            sb.append(getTimeTag() + "[BASE_INFO]Peanut SDK Version: " + applogInfo.robotSDKVersion);
            sb.append("\r\n");
            sb.append(getTimeTag() + "[BASE_INFO]APP " + applogInfo.appVersionName + ", " + applogInfo.appVersionCode);
            sb.append("\r\n");
            sb.append(getTimeTag() + "[BASE_INFO]" + applogInfo.uuid);
            sb.append("\r\n");
            sb.append(getTimeTag() + "[BASE_INFO]" + applogInfo.imei);
            sb.append("\r\n");
            sb.append(getTimeTag() + "[BASE_INFO]IP: " + NetworkUtils.getIPAddress(true));
            sb.append("\r\n");
            sb.append(getTimeTag() + "[BASE_INFO]" + applogInfo.mac);
            sb.append("\r\n");
            sb.append(getTimeTag() + "[BASE_INFO]" + applogInfo.packName);
            sb.append("\r\n");
            String upTime = new java.sql.Date(applogInfo.upTime).toLocaleString();
            String currentTime = new java.sql.Date(applogInfo.currentTime).toLocaleString();
            sb.append(getTimeTag() + "[BASE_INFO]" + String.format("Android uptime: %s, current time: %s", upTime, currentTime));
            sb.append("\r\n");
            sb.append(getTimeTag() + "[BASE_INFO]" + applogInfo.screenStr);
            sb.append("\r\n");
            baseInfo = sb.toString();
            LogUtils.d(PeanutConstants.TAG_UTIL, "deviceInfo:" + baseInfo);
        }
        return baseInfo;
    }

    /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/common/utils/InfoUtils$ApplogInfo.class */
    public static class ApplogInfo {
        public String productName;
        public String cpuAbiList;
        public String buildVersion;
        public String osReleaseVersion;
        public String appVersionName;
        public String appVersionCode;
        public String robotSDKVersion;
        public long upTime;
        public long currentTime;
        public String imei;
        public String uuid;
        public String mac;
        public String screenStr;
        public String romSize;
        public String sdcardSize;
        public String packName;

        public ApplogInfo init(Context context) {
            try {
                this.productName = "Product: " + Build.MANUFACTURER + "|" + Build.MODEL + "|" + Build.DEVICE;
                this.cpuAbiList = "CPU ABI: " + Build.CPU_ABI;
                this.buildVersion = "Build SDK version: " + Build.VERSION.SDK;
                this.osReleaseVersion = "Android OS: " + Build.VERSION.RELEASE;
                this.appVersionCode = "VersionCode: " + PackageUtils.getVersionCode(context);
                this.appVersionName = "VersionName: " + PackageUtils.getVersionName(context);
                this.robotSDKVersion = "1.5.0-bate1";
                this.upTime = SystemClock.elapsedRealtime();
                this.currentTime = System.currentTimeMillis();
                this.imei = String.format("IMEI: %s", NetworkUtils.getIMEI(context));
                this.mac = String.format("MAC: %s", PeanutSNManagerUtils.getSN());
                this.uuid = "UUID: ";
                this.screenStr = InfoUtils.getScreenProp(context);
                this.romSize = "Memory Usage: " + InfoUtils.getRAMUsage(context);
                this.sdcardSize = "SDCard USage: " + InfoUtils.getSdcardUsage(context);
                this.packName = "PackName: " + PackageUtils.getPackName(context);
            } catch (Exception e) {
                LogUtils.e(PeanutConstants.TAG_UTIL, "[BASE_INFO]", e);
            }
            return this;
        }
    }
}
