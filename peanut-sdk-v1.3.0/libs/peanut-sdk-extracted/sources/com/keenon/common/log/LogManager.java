package com.keenon.common.log;

import android.content.Context;
import android.os.Environment;
import android.text.TextUtils;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.android.LogcatAppender;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy;
import ch.qos.logback.core.util.FileSize;
import ch.qos.logback.core.util.OptionHelper;
import ch.qos.logback.core.util.StatusPrinter;
import com.keenon.common.constant.PeanutConstants;
import com.keenon.common.utils.FileUtil;
import com.keenon.common.utils.LogUtils;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Hashtable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.eclipse.californium.core.coap.CoAP;
import org.slf4j.LoggerFactory;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/common/log/LogManager.class */
public class LogManager {
    public static final int ACTION_APP_START = 1;
    public static final int ACTION_APP_START_OK = 2;
    public static final int ACTION_LOGIN = 3;
    public static final int ACTION_LOGOUT = 4;
    public static final int ACTION_HTTP_REQUEST = 5;
    public static final int ACTION_DOWNLOAD_ERROR = 6;
    public static final int ACTION_ROBOT = 7;
    public static final int ACTION_LOGCAT = 8;
    public static final int ACTION_CRASH = 9;
    public static final int ACTION_SERIAL = 10;
    public static final String TAG_BASE = "[BASE_INFO]";
    public static final String TAG_START = "[START_INFO]";
    public static final String TAG_PASSPORT = "[PASSPORT_INFO]";
    public static final String TAG_PLAY = "[PLAY_INFO]";
    public static final String TAG_DOWNLOAD = "[DOWNLOAD_INFO]";
    public static final String TAG_HTTP = "[HTTP_INFO]";
    public static final String TAG_LOGCAT = "[LOG]";
    public static final String TAG_CRASH = "[LOGCAT_CRASH]";
    public static final String TAG_SERIAL = "[LOGCAT_SERIAL]";
    private static final String TAG = "[LogManager]";
    private static String baseInfo;
    private static LogManager instance;
    public String APP_LOG_DIR;
    public String APP_CRASH_DIR;
    public String APP_ROBOT_DIR;
    public String APP_SERIAL_DIR;
    public boolean mIsLogPathRooted;
    public boolean init;
    private String APP_BASE_DIR;
    private boolean inited;
    private static final int MAX_SIZE = PeanutConstants.LOG_MAX_SIZE;
    private static int LOG_CACHE_COUNT = PeanutConstants.LOG_CACHE_COUNT;
    private static ExecutorService logThreadPool = Executors.newFixedThreadPool(10);
    private ConcurrentHashMap<String, ConcurrentLinkedQueue<String>> listDataToWrite = new ConcurrentHashMap<>();
    private ThreadLocalDateFormatter formatter = new ThreadLocalDateFormatter();
    private volatile Hashtable<String, Boolean> writingFileList = new Hashtable<>();
    private boolean isWriteSdcard = false;

    /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/common/log/LogManager$Formatter.class */
    public interface Formatter {
        String format(Date date);
    }

    private LogManager() {
    }

    private static String getDirPath(Context ctx, boolean save2sdcardIfExist) {
        String packgeName = ctx.getPackageName();
        int index = packgeName.lastIndexOf(".");
        if (index >= 0) {
            String name = packgeName.substring(index + 1);
            LogUtils.d(PeanutConstants.TAG_UTIL, "[LogManager]==init app dir path with packagename:" + name);
            if (name != null) {
                return getAvailableStoragePath(ctx, save2sdcardIfExist) + File.separator + name;
            }
        }
        return getAvailableStoragePath(ctx, save2sdcardIfExist);
    }

    public static String getAvailableStoragePath(Context context, boolean save2Sdcard) {
        if (save2Sdcard) {
            return Environment.getExternalStorageDirectory().getAbsolutePath();
        }
        return context.getFilesDir().getAbsolutePath();
    }

    public static LogManager getInstance() {
        if (instance == null) {
            synchronized (LogManager.class) {
                if (instance == null) {
                    instance = new LogManager();
                }
            }
        }
        return instance;
    }

    public void init(String mLogPathDir, boolean enable, boolean enablePrintLogCat, boolean isDirRooted, boolean deleteOld, Context ctx, boolean save2sdcardIfExist, String headInfo, boolean isLogBackWriteFile) {
        this.init = true;
        baseInfo = headInfo;
        this.mIsLogPathRooted = isDirRooted;
        if (TextUtils.isEmpty(mLogPathDir)) {
            this.APP_BASE_DIR = getDirPath(ctx, save2sdcardIfExist);
        } else {
            this.APP_BASE_DIR = mLogPathDir;
        }
        if (this.mIsLogPathRooted) {
            LogUtils.d(PeanutConstants.TAG_UTIL, "[LogManager]app base dir:" + this.APP_BASE_DIR);
            this.APP_LOG_DIR = this.APP_BASE_DIR;
            LogUtils.d(PeanutConstants.TAG_UTIL, "[LogManager]app log dir" + this.APP_LOG_DIR);
            this.APP_CRASH_DIR = this.APP_BASE_DIR;
            LogUtils.d(PeanutConstants.TAG_UTIL, "[LogManager]app crash dir" + this.APP_CRASH_DIR);
            this.APP_ROBOT_DIR = this.APP_BASE_DIR;
            LogUtils.d(PeanutConstants.TAG_UTIL, "[LogManager]app robot dir" + this.APP_ROBOT_DIR);
            this.APP_SERIAL_DIR = this.APP_BASE_DIR;
            LogUtils.d(PeanutConstants.TAG_UTIL, "[LogManager]app serial dir" + this.APP_SERIAL_DIR);
        } else {
            LogUtils.d(PeanutConstants.TAG_UTIL, "[LogManager]app base dir:" + this.APP_BASE_DIR);
            this.APP_LOG_DIR = this.APP_BASE_DIR + File.separator + "log";
            LogUtils.d(PeanutConstants.TAG_UTIL, "[LogManager]app log dir" + this.APP_LOG_DIR);
            this.APP_CRASH_DIR = this.APP_LOG_DIR + File.separator + "crash";
            LogUtils.d(PeanutConstants.TAG_UTIL, "[LogManager]app crash dir" + this.APP_CRASH_DIR);
            this.APP_ROBOT_DIR = this.APP_LOG_DIR + File.separator + "robot";
            LogUtils.d(PeanutConstants.TAG_UTIL, "[LogManager]app robot dir" + this.APP_ROBOT_DIR);
            this.APP_SERIAL_DIR = this.APP_LOG_DIR + File.separator + "serial";
            LogUtils.d(PeanutConstants.TAG_UTIL, "[LogManager]app serial dir" + this.APP_SERIAL_DIR);
        }
        init(ctx, this.APP_ROBOT_DIR, deleteOld);
        init(ctx, this.APP_SERIAL_DIR, deleteOld);
        initLogback(this.APP_LOG_DIR + File.separator + CoAP.COAP_URI_SCHEME, enable, enablePrintLogCat, isLogBackWriteFile);
    }

    public void invokeMethod(int action, String value, String tag) {
        if (!this.inited || !this.isWriteSdcard) {
            return;
        }
        switch (action) {
            case 1:
                invokeAppStart();
                break;
            case 2:
                invokeAppStartEnd();
                break;
            case 3:
                invokeLogin();
                break;
            case 4:
                invokeLogout();
                break;
            case 5:
                invokeHttpRequest(value);
                break;
            case 8:
                invokeLogcat(value, tag);
                break;
            case 9:
                invokeCrash(value);
                break;
            case 10:
                invokeSerial(value);
                break;
        }
    }

    private String init(Context context, String logPath, boolean deleteOld) {
        if (logPath == null || logPath.isEmpty()) {
            logPath = context.getCacheDir().getAbsolutePath();
        }
        File dir = new File(logPath);
        if (dir.exists() && deleteOld) {
            FileUtil.deleteDir(dir);
        }
        this.inited = true;
        return logPath;
    }

    private void initLogback(String filePath, boolean enable, boolean enablePrintCat, boolean isLogBackWriteFile) {
        if (enable) {
            LogUtils.d(PeanutConstants.TAG_UTIL, "[LogManager][initLogback][coap log dir: " + filePath + "]");
            configureLogbackDirectly(filePath, enablePrintCat, isLogBackWriteFile, Level.DEBUG);
        }
    }

    private void configureLogbackDirectly(String filePath, boolean printOnLogCat, boolean printOnFile, Level level) {
        LoggerContext context = LoggerFactory.getILoggerFactory();
        context.stop();
        context.reset();
        if (!printOnLogCat && !printOnFile) {
            return;
        }
        Logger root = LoggerFactory.getLogger("ROOT");
        if (printOnLogCat) {
            LogcatAppender logcatAppender = configLogCat(context);
            root.addAppender(logcatAppender);
        }
        if (printOnFile) {
            RollingFileAppender rollingFileAppender = configLogFile(filePath, context);
            root.addAppender(rollingFileAppender);
        }
        root.setLevel(level);
        StatusPrinter.print(context);
    }

    private static LogcatAppender configLogCat(LoggerContext context) {
        PatternLayoutEncoder encoder2 = new PatternLayoutEncoder();
        encoder2.setContext(context);
        encoder2.setPattern("[%-20thread] %msg");
        encoder2.start();
        LogcatAppender logcatAppender = new LogcatAppender();
        logcatAppender.setContext(context);
        logcatAppender.setEncoder(encoder2);
        logcatAppender.start();
        return logcatAppender;
    }

    private static RollingFileAppender configLogFile(String logDir, LoggerContext context) {
        RollingFileAppender fileAppender = new RollingFileAppender();
        fileAppender.setContext(context);
        fileAppender.setFile(OptionHelper.substVars(logDir + File.separator + "coap.log", context));
        SizeAndTimeBasedRollingPolicy policy = new SizeAndTimeBasedRollingPolicy();
        String fp = OptionHelper.substVars(logDir + File.separator + "/%d{yyyy_MM_dd, Asia/Shanghai}-%i.zip", context);
        policy.setMaxFileSize(FileSize.valueOf("200MB"));
        policy.setFileNamePattern(fp);
        policy.setMaxHistory(7);
        policy.setTotalSizeCap(FileSize.valueOf("1GB"));
        policy.setParent(fileAppender);
        policy.setContext(context);
        policy.start();
        fileAppender.setRollingPolicy(policy);
        PatternLayoutEncoder encoder1 = new PatternLayoutEncoder();
        encoder1.setContext(context);
        encoder1.setPattern("%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n");
        encoder1.start();
        fileAppender.setEncoder(encoder1);
        fileAppender.start();
        return fileAppender;
    }

    private void invokeLogcat(String value, String tag) {
        if (!TextUtils.isEmpty(tag)) {
            writeString(getTimeTag() + " " + tag + " " + TAG_LOGCAT + value, this.APP_ROBOT_DIR, "all.txt");
        }
    }

    private void invokeCrash(String value) {
        writeString(getTimeTag() + TAG_CRASH + value, this.APP_CRASH_DIR, "crash.txt");
    }

    private void invokeSerial(String value) {
        writeString(getTimeTag() + TAG_SERIAL + value, this.APP_SERIAL_DIR, "serial.txt");
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void doFileTask(String str, String fileDir, String fileName, int writeCount) {
        if (this.listDataToWrite.get(fileName) == null) {
            this.listDataToWrite.put(fileName, new ConcurrentLinkedQueue<>());
            this.listDataToWrite.get(fileName).add(baseInfo);
        }
        this.listDataToWrite.get(fileName).add(str);
        if (this.writingFileList.get(fileName) != null || this.listDataToWrite.get(fileName).size() < writeCount) {
            return;
        }
        LogUtils.d(PeanutConstants.TAG_UTIL, TAG + fileName + " is not in writing,going to write");
        this.writingFileList.put(fileName, true);
        File dir = new File(fileDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        File file = new File(fileDir, fileName);
        if (!file.exists()) {
            try {
                file.createNewFile();
                file.setReadable(true, false);
                file.setWritable(true, false);
            } catch (IOException e) {
                LogUtils.e(PeanutConstants.TAG_UTIL, "----create logfile Exception " + fileName, (Exception) e);
                this.writingFileList.remove(fileName);
            }
        }
        if (this.writingFileList.get(fileName) != null) {
            new Thread(new WriteFileThread(file, this.listDataToWrite, this.writingFileList)).start();
        }
    }

    private void writeString(String str, String fileDir, String fileName) {
        logThreadPool.execute(new WriteTask(str, fileDir, fileName, LOG_CACHE_COUNT));
    }

    public void flush() {
        new Thread(new Runnable() { // from class: com.keenon.common.log.LogManager.1
            @Override // java.lang.Runnable
            public void run() {
                if (LogManager.this.listDataToWrite != null && LogManager.this.listDataToWrite.size() > 0) {
                    LogUtils.d(PeanutConstants.TAG_UTIL, "[LogManager] flush all logs with tid:" + Thread.currentThread().getId());
                    LogUtils.d(PeanutConstants.TAG_UTIL, "[LogManager] flush all logs with tid:" + Thread.currentThread().getId());
                    for (String filename : LogManager.this.listDataToWrite.keySet()) {
                        if (((ConcurrentLinkedQueue) LogManager.this.listDataToWrite.get(filename)).size() > 0) {
                            LogUtils.d(PeanutConstants.TAG_UTIL, "[LogManager] flush file:" + filename);
                            LogManager.this.doFileTask("-------flush-----------------", LogManager.this.APP_ROBOT_DIR, filename, 0);
                        }
                    }
                }
            }
        }).start();
    }

    private void invokeAppStart() {
        writeString(getTimeTag() + TAG_START + "app start", this.APP_ROBOT_DIR, "app.txt");
    }

    private void invokeAppStartEnd() {
        writeString(getTimeTag() + TAG_START + "app ok", this.APP_ROBOT_DIR, "app.txt");
    }

    private void invokeLogin() {
    }

    private void invokeLogout() {
        writeString(getTimeTag() + TAG_PASSPORT + "user logout", this.APP_ROBOT_DIR, "user.txt");
    }

    private void invokeHttpRequest(String str) {
        writeString(getTimeTag() + TAG_HTTP + str, this.APP_ROBOT_DIR, "netrequest.txt");
    }

    public String getTimeTag() {
        return String.format("[%s] ", this.formatter.format(new Date(System.currentTimeMillis())));
    }

    private void closeStream(Closeable stream) {
        if (stream != null) {
            try {
                stream.close();
            } catch (Exception e) {
            }
        }
    }

    /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/common/log/LogManager$WriteFileThread.class */
    private static class WriteFileThread implements Runnable {
        ConcurrentHashMap<String, ConcurrentLinkedQueue<String>> listDataToWrite;
        private File file;
        private Hashtable<String, Boolean> writtingFileList;

        public WriteFileThread(File file, ConcurrentHashMap<String, ConcurrentLinkedQueue<String>> listDataToWrite, Hashtable<String, Boolean> writingFileList) {
            this.file = file;
            this.listDataToWrite = listDataToWrite;
            this.writtingFileList = writingFileList;
        }

        public void setFile(File file) {
            this.file = file;
        }

        /* JADX WARN: Incorrect condition in loop: B:5:0x0022 */
        /* JADX WARN: Removed duplicated region for block: B:42:0x00b4 A[EXC_TOP_SPLITTER, SYNTHETIC] */
        /*
            Code decompiled incorrectly, please refer to instructions dump.
            To view partially-correct code enable 'Show inconsistent code' option in preferences
        */
        private void readAndRemoveFirstLines(java.io.File r8, long r9) throws java.io.IOException {
            /*
                r7 = this;
                r0 = 0
                r11 = r0
                java.io.RandomAccessFile r0 = new java.io.RandomAccessFile     // Catch: java.io.IOException -> La8 java.lang.Throwable -> Lad
                r1 = r0
                r2 = r8
                java.lang.String r3 = "rw"
                r1.<init>(r2, r3)     // Catch: java.io.IOException -> La8 java.lang.Throwable -> Lad
                r11 = r0
                r0 = r11
                long r0 = r0.getFilePointer()     // Catch: java.io.IOException -> La8 java.lang.Throwable -> Lad
                r12 = r0
                r0 = 0
                r14 = r0
            L19:
                r0 = r11
                java.lang.String r0 = r0.readLine()     // Catch: java.io.IOException -> La8 java.lang.Throwable -> Lad
                r16 = r0
                r0 = r16
                if (r0 != 0) goto L28
                goto L41
            L28:
                r0 = r11
                long r0 = r0.getFilePointer()     // Catch: java.io.IOException -> La8 java.lang.Throwable -> Lad
                r14 = r0
                r0 = r14
                r1 = r8
                long r1 = r1.length()     // Catch: java.io.IOException -> La8 java.lang.Throwable -> Lad
                r2 = r9
                long r1 = r1 - r2
                int r0 = (r0 > r1 ? 1 : (r0 == r1 ? 0 : -1))
                if (r0 < 0) goto L3e
                goto L41
            L3e:
                goto L19
            L41:
                r0 = 1024(0x400, float:1.435E-42)
                byte[] r0 = new byte[r0]     // Catch: java.io.IOException -> La8 java.lang.Throwable -> Lad
                r16 = r0
            L48:
                r0 = -1
                r1 = r11
                r2 = r16
                int r1 = r1.read(r2)     // Catch: java.io.IOException -> La8 java.lang.Throwable -> Lad
                r2 = r1
                r17 = r2
                if (r0 == r1) goto L81
                r0 = r11
                r1 = r12
                r0.seek(r1)     // Catch: java.io.IOException -> La8 java.lang.Throwable -> Lad
                r0 = r11
                r1 = r16
                r2 = 0
                r3 = r17
                r0.write(r1, r2, r3)     // Catch: java.io.IOException -> La8 java.lang.Throwable -> Lad
                r0 = r14
                r1 = r17
                long r1 = (long) r1     // Catch: java.io.IOException -> La8 java.lang.Throwable -> Lad
                long r0 = r0 + r1
                r14 = r0
                r0 = r12
                r1 = r17
                long r1 = (long) r1     // Catch: java.io.IOException -> La8 java.lang.Throwable -> Lad
                long r0 = r0 + r1
                r12 = r0
                r0 = r11
                r1 = r14
                r0.seek(r1)     // Catch: java.io.IOException -> La8 java.lang.Throwable -> Lad
                goto L48
            L81:
                r0 = r11
                r1 = r12
                r0.setLength(r1)     // Catch: java.io.IOException -> La8 java.lang.Throwable -> Lad
                r0 = r11
                r1 = 0
                r0.seek(r1)     // Catch: java.io.IOException -> La8 java.lang.Throwable -> Lad
                r0 = r11
                java.lang.String r1 = com.keenon.common.log.LogManager.access$200()     // Catch: java.io.IOException -> La8 java.lang.Throwable -> Lad
                r0.writeUTF(r1)     // Catch: java.io.IOException -> La8 java.lang.Throwable -> Lad
                r0 = r11
                if (r0 == 0) goto La0
                r0 = r11
                r0.close()     // Catch: java.io.IOException -> La3
            La0:
                goto Lc4
            La3:
                r12 = move-exception
                r0 = r12
                throw r0
            La8:
                r12 = move-exception
                r0 = r12
                throw r0     // Catch: java.lang.Throwable -> Lad
            Lad:
                r18 = move-exception
                r0 = r11
                if (r0 == 0) goto Lb9
                r0 = r11
                r0.close()     // Catch: java.io.IOException -> Lbc
            Lb9:
                goto Lc1
            Lbc:
                r19 = move-exception
                r0 = r19
                throw r0
            Lc1:
                r0 = r18
                throw r0
            Lc4:
                return
            */
            throw new UnsupportedOperationException("Method not decompiled: com.keenon.common.log.LogManager.WriteFileThread.readAndRemoveFirstLines(java.io.File, long):void");
        }

        /* JADX WARN: Removed duplicated region for block: B:69:0x0288 A[EXC_TOP_SPLITTER, SYNTHETIC] */
        @Override // java.lang.Runnable
        /*
            Code decompiled incorrectly, please refer to instructions dump.
            To view partially-correct code enable 'Show inconsistent code' option in preferences
        */
        public void run() {
            /*
                Method dump skipped, instruction units count: 766
                To view this dump change 'Code comments level' option to 'DEBUG'
            */
            throw new UnsupportedOperationException("Method not decompiled: com.keenon.common.log.LogManager.WriteFileThread.run():void");
        }
    }

    /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/common/log/LogManager$WriteTask.class */
    private class WriteTask implements Runnable {
        String str;
        String fileDir;
        String fileName;
        int writeCount;

        public WriteTask(String str, String fileDir, String fileName, int writeCount) {
            this.str = str;
            this.fileDir = fileDir;
            this.fileName = fileName;
            this.writeCount = writeCount;
        }

        @Override // java.lang.Runnable
        public void run() {
            LogManager.this.doFileTask(this.str, this.fileDir, this.fileName, this.writeCount);
        }
    }

    /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/common/log/LogManager$ThreadLocalDateFormatter.class */
    public static class ThreadLocalDateFormatter implements Formatter {
        ThreadLocal<SimpleDateFormat> formatter = new ThreadLocal<SimpleDateFormat>() { // from class: com.keenon.common.log.LogManager.ThreadLocalDateFormatter.1
            /* JADX INFO: Access modifiers changed from: protected */
            /* JADX WARN: Can't rename method to resolve collision */
            @Override // java.lang.ThreadLocal
            public synchronized SimpleDateFormat initialValue() {
                return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
            }
        };

        @Override // com.keenon.common.log.LogManager.Formatter
        public String format(Date date) {
            return this.formatter.get().format(date);
        }
    }
}
