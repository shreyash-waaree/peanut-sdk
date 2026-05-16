package com.keenon.common.utils;

import android.util.Log;
import com.keenon.common.log.LogManager;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/common/utils/LogUtils.class */
public final class LogUtils {
    private static final int EXCEPTION_STACK_INDEX = 2;
    public static int LOG_LEVEL;
    public static boolean logOn;
    public static boolean logWriteFileOn;

    public static void init(boolean enable, int level, boolean isLogWriteFile) {
        LOG_LEVEL = level;
        logOn = enable;
        logWriteFileOn = isLogWriteFile;
    }

    public static String getTag() {
        try {
            Exception exception = new Exception();
            if (exception.getStackTrace() == null || exception.getStackTrace().length <= 2) {
                return "***";
            }
            StackTraceElement element = exception.getStackTrace()[2];
            String className = element.getClassName();
            int index = className.lastIndexOf(".");
            if (index > 0) {
                className = className.substring(index + 1);
            }
            return className + "_" + element.getMethodName() + "_" + element.getLineNumber();
        } catch (Throwable e) {
            e.printStackTrace();
            return "***";
        }
    }

    public static void v(String tag, String msg) {
        if (logOn && 2 >= LOG_LEVEL) {
            String tag2 = buildTag(tag);
            Log.v(tag2, msg);
            writeFilterdLog(tag2, msg);
        }
    }

    private static void writeFilterdLog(String tag, String msg) {
        if (logWriteFileOn) {
            LogManager.getInstance().invokeMethod(8, msg, tag);
        }
    }

    public static void d(String tag, String msg) {
        if (logOn && 3 >= LOG_LEVEL) {
            String tag2 = buildTag(tag);
            Log.d(tag2, msg);
            writeFilterdLog(tag2, msg);
        }
    }

    public static void i(String tag, String msg) {
        if (logOn && 4 >= LOG_LEVEL) {
            String tag2 = buildTag(tag);
            Log.i(tag2, msg);
            writeFilterdLog(tag2, msg);
        }
    }

    public static void w(String tag, String msg) {
        if (logOn && 5 >= LOG_LEVEL) {
            String tag2 = buildTag(tag);
            Log.w(tag2, msg);
            writeFilterdLog(tag2, msg);
        }
    }

    public static void e(String tag, int errorCode) {
        if (logOn) {
            String tag2 = buildTag(tag);
            Log.e(tag2, "[#E_" + errorCode + "#]");
            if (6 >= LOG_LEVEL) {
                writeFilterdLog(tag2, "[" + errorCode + "]");
            }
        }
    }

    public static void e(String tag, String msg) {
        if (logOn && 6 >= LOG_LEVEL) {
            String tag2 = buildTag(tag);
            Log.e(tag2, msg);
            writeFilterdLog(tag2, msg);
        }
    }

    public static void e(String msg, Throwable tr) {
        if (logOn && 6 >= LOG_LEVEL) {
            String tag = buildTag(getTag());
            Log.e(tag, msg, tr);
            writeFilterdLog(tag, msg + Log.getStackTraceString(tr));
        }
    }

    public static void e(String tag, String msg, Throwable tr) {
        if (logOn && 6 >= LOG_LEVEL) {
            String tag2 = buildTag(tag);
            Log.e(tag2, msg, tr);
            writeFilterdLog(tag2, msg + Log.getStackTraceString(tr));
        }
    }

    public static void e(String tag, String msg, Exception e) {
        if (logOn && 6 >= LOG_LEVEL) {
            String tag2 = buildTag(tag);
            Log.e(tag2, msg, e);
            writeFilterdLog(tag2, msg + Log.getStackTraceString(e));
        }
    }

    private static void writeSerialLog(String tag, String msg) {
        if (logWriteFileOn) {
            LogManager.getInstance().invokeMethod(10, msg, tag);
        }
    }

    public static void s(String tag, String msg) {
        if (logOn && 3 >= LOG_LEVEL) {
            writeSerialLog(buildTag(tag), msg);
        }
    }

    private static String buildTag(String tag) {
        return "PeanutSDK:" + tag;
    }
}
