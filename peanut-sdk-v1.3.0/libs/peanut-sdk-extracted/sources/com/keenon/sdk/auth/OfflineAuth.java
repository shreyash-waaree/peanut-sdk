package com.keenon.sdk.auth;

import android.content.Context;
import android.os.Environment;
import com.keenon.common.constant.PeanutConstants;
import com.keenon.common.error.PeanutError;
import com.keenon.common.utils.DesUtil;
import com.keenon.common.utils.FileUtil;
import com.keenon.common.utils.GsonUtil;
import com.keenon.common.utils.LogUtils;
import com.keenon.common.utils.PeanutSNManagerUtils;
import com.keenon.common.utils.StringUtils;
import com.keenon.common.utils.TimeUtils;
import java.io.File;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import org.eclipse.californium.elements.util.NamedThreadFactory;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/auth/OfflineAuth.class */
public class OfflineAuth {
    private static final String TAG = "[OfflineAuth]";
    private static final long TIME_REFRESH = 30000;
    private static final long TIME_CLOCK = 1000;
    private static final int SUCCESS = 0;
    private static volatile OfflineAuth sInstance;
    private volatile AuthInfo authInfo;
    private volatile LicenseInfo licenseInfo;
    private volatile int authStatus;
    private String authFile;
    private AtomicLong usedTime;
    private final AtomicReference<ScheduledFuture<?>> refreshHandle = new AtomicReference<>();
    private final Runnable refresh = new Runnable() { // from class: com.keenon.sdk.auth.OfflineAuth.1
        @Override // java.lang.Runnable
        public void run() {
            OfflineAuth.this.generateFingerPrint();
        }
    };
    private final Runnable clock = new Runnable() { // from class: com.keenon.sdk.auth.OfflineAuth.2
        @Override // java.lang.Runnable
        public void run() {
            OfflineAuth.this.authInfo.setUsedTime(OfflineAuth.this.usedTime.incrementAndGet());
        }
    };
    private final ScheduledThreadPoolExecutor scheduler = new ScheduledThreadPoolExecutor(2, new NamedThreadFactory("OfflineAuth#"));

    public static OfflineAuth getInstance() {
        if (sInstance == null) {
            synchronized (OfflineAuth.class) {
                if (sInstance == null) {
                    sInstance = new OfflineAuth();
                }
            }
        }
        return sInstance;
    }

    public int start(Context context, String appId, String appSecret) {
        initAuthInfo(context, appId, appSecret);
        initFingerPrint(context);
        String fingerPrintContent = findFingerPrint();
        if (StringUtils.isEmpty(fingerPrintContent)) {
            if (checkAuthSecret(context, appId, appSecret)) {
                LogUtils.i(PeanutConstants.TAG_UTIL, "[OfflineAuth][首次鉴权成功]");
                setStatus(0);
                this.authInfo.setLicense(this.licenseInfo);
                this.authInfo.setExpireTime(this.licenseInfo.getExpireTime());
                this.authInfo.setActivatedTime(TimeUtils.getTime());
                this.usedTime = new AtomicLong();
                refreshAuthInfo();
            }
        } else {
            String authCacheContent = null;
            try {
                authCacheContent = decodeAuthFingerPrint(fingerPrintContent);
            } catch (Exception e) {
                setStatus(PeanutError.AUTH_FP_DECODE_EXCEPTION);
                e.printStackTrace();
            }
            LogUtils.i(PeanutConstants.TAG_UTIL, "[OfflineAuth][本地已有授权信息]");
            LogUtils.i(PeanutConstants.TAG_UTIL, "[OfflineAuth][start content: " + authCacheContent + "]");
            AuthInfo authCache = (AuthInfo) GsonUtil.gson2Bean(authCacheContent, AuthInfo.class);
            if (authCache == null) {
                setStatus(PeanutError.AUTH_INVALID_FP);
            } else {
                this.authInfo.setLicense(authCache.getLicense());
                this.authInfo.setUsedTime(authCache.getUsedTime());
                this.authInfo.setExpireTime(authCache.getExpireTime());
                this.authInfo.setActivatedTime(authCache.getActivatedTime());
                if (!isAuthExpired(authCache.getActivatedTime(), TimeUtils.getTime())) {
                    setStatus(0);
                    this.usedTime = new AtomicLong(this.authInfo.getUsedTime());
                    refreshAuthInfo();
                }
            }
        }
        return this.authStatus;
    }

    private void initAuthInfo(Context context, String appId, String appSecret) {
        this.authInfo = new AuthInfo();
        this.authInfo.setAppName(context.getPackageName());
        this.authInfo.setAppId(appId);
        this.authInfo.setAppSecret(appSecret);
        this.authInfo.setRobotSN(PeanutSNManagerUtils.getSN());
    }

    private void initFingerPrint(Context context) {
        String packageName = context.getPackageName();
        int index = packageName.lastIndexOf(".");
        String name = packageName.substring(index + 1);
        String authPath = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + name;
        FileUtil.mkDirIfNotExist(authPath);
        this.authFile = authPath + File.separator + this.authInfo.getAppId() + ".dat";
    }

    /* JADX INFO: Access modifiers changed from: private */
    public synchronized boolean generateFingerPrint() {
        String fp = encodeAuthFingerPrint();
        LogUtils.i(PeanutConstants.TAG_UTIL, "[OfflineAuth][generateFingerPrint: " + GsonUtil.bean2String(this.authInfo) + "]");
        LogUtils.i(PeanutConstants.TAG_UTIL, "[OfflineAuth][generateFingerPrint: " + this.authFile + "]");
        LogUtils.i(PeanutConstants.TAG_UTIL, "[OfflineAuth][generateFingerPrint: " + fp + "]");
        if (fp != null && !fp.isEmpty()) {
            FileUtil.writeFile(this.authFile, fp);
            return true;
        }
        setStatus(PeanutError.AUTH_INVALID_FP);
        return false;
    }

    private String findFingerPrint() {
        File file = new File(this.authFile);
        return file.exists() ? new String(FileUtil.getBytes(file)) : "";
    }

    private void setStatus(int authStatus) {
        LogUtils.i(PeanutConstants.TAG_UTIL, "[OfflineAuth][setStatus: " + authStatus + "]");
        this.authStatus = authStatus;
    }

    private boolean isAuthExpired(String activatedTime, String nowTime) {
        long elapsedTime = TimeUtils.getDistanceTime(activatedTime, nowTime);
        LogUtils.i(PeanutConstants.TAG_UTIL, "[OfflineAuth][isAuthExpired: " + elapsedTime + "]");
        if (elapsedTime == 0) {
            setStatus(PeanutError.AUTH_INVALID_DISTANCE);
            return true;
        }
        if (this.authInfo.getUsedTime() >= this.authInfo.getExpireTime() || elapsedTime >= this.authInfo.getExpireTime()) {
            setStatus(PeanutError.AUTH_EXPIRED);
            return true;
        }
        return false;
    }

    private void refreshAuthInfo() {
        this.scheduler.scheduleAtFixedRate(this.refresh, 0L, TIME_REFRESH, TimeUnit.MILLISECONDS);
        this.scheduler.scheduleAtFixedRate(this.clock, 0L, TIME_CLOCK, TimeUnit.MILLISECONDS);
    }

    private void setRefreshHandle(ScheduledFuture<?> reregistrationHandle) {
        ScheduledFuture<?> previousHandle = this.refreshHandle.getAndSet(reregistrationHandle);
        if (previousHandle != null) {
            if (previousHandle instanceof Runnable) {
                this.scheduler.remove((Runnable) previousHandle);
            } else {
                previousHandle.cancel(false);
            }
        }
    }

    private boolean checkAuthSecret(Context context, String appId, String secret) {
        if (context == null || appId == null || appId.isEmpty() || secret == null || secret.isEmpty()) {
            setStatus(PeanutError.AUTH_PARAMS_NULL);
            return false;
        }
        try {
            String licenseString = DesUtil.decrypt(secret);
            LogUtils.d(PeanutConstants.TAG_UTIL, "[OfflineAuth][license:" + licenseString + "]");
            this.licenseInfo = (LicenseInfo) GsonUtil.gson2Bean(licenseString, LicenseInfo.class);
            if (this.licenseInfo == null) {
                setStatus(PeanutError.AUTH_INVALID_LICENSE);
                return false;
            }
            if (!this.licenseInfo.getPackageName().equals(this.authInfo.getAppName())) {
                setStatus(PeanutError.AUTH_INVALID_APP);
                return false;
            }
            if (!this.licenseInfo.getAppId().equals(this.authInfo.getAppId())) {
                setStatus(PeanutError.AUTH_INVALID_APPID);
                return false;
            }
            return true;
        } catch (Exception e) {
            LogUtils.e(PeanutConstants.TAG_UTIL, "[OfflineAuth][checkSecret exception" + e.toString() + "]");
            setStatus(PeanutError.AUTH_INVALID_SECRET);
            return false;
        }
    }

    private String encodeAuthFingerPrint() {
        String fpContent = null;
        try {
            fpContent = DesUtil.encrypt(GsonUtil.bean2String(this.authInfo));
        } catch (Exception e) {
            setStatus(PeanutError.AUTH_FP_ENCODE_EXCEPTION);
            e.printStackTrace();
        }
        return fpContent;
    }

    private String decodeAuthFingerPrint(String encode) {
        String fpContent = null;
        try {
            fpContent = DesUtil.decrypt(encode);
        } catch (Exception e) {
            setStatus(PeanutError.AUTH_FP_DECODE_EXCEPTION);
            e.printStackTrace();
        }
        return fpContent;
    }
}
