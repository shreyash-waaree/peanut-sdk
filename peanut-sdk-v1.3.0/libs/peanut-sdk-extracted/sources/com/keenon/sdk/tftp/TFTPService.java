package com.keenon.sdk.tftp;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import com.keenon.common.constant.PeanutConstants;
import com.keenon.common.utils.FileUtil;
import com.keenon.common.utils.LogUtils;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.net.InetAddress;
import org.apache.commons.net.tftp.TFTPServer;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/tftp/TFTPService.class */
public class TFTPService extends Service {
    public static final int PORT = 9527;
    private static final String TAG = "TFTPService";
    public static String mServerPath;
    public static String mServerIP;
    private TFTPServer mServer;

    @Override // android.app.Service
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override // android.app.Service
    public void onCreate() {
        super.onCreate();
    }

    @Override // android.app.Service
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            mServerPath = intent.getStringExtra(PeanutConstants.TFTP_SERVER_PATH);
            mServerIP = intent.getStringExtra(PeanutConstants.TFTP_SERVER_IP);
        }
        if (null == this.mServer) {
            LogUtils.d(PeanutConstants.TAG_TFTP, "TFTPServiceonStartCommand");
            createServer();
        }
        startServer();
        return 2;
    }

    private void createServer() {
        log("createServer");
        if (mServerIP == null || mServerIP.length() == 0) {
            mServerIP = PeanutConstants.LOCAL_ETHER_IP;
        }
        if (mServerPath == null || mServerPath.length() == 0) {
            mServerPath = PeanutConstants.PATH_TFTP_SERVER;
        }
        log("server ip", mServerIP);
        log("server path", mServerPath);
        File serverDirectory = new File(mServerPath);
        if (!serverDirectory.exists()) {
            log("create server directory");
            FileUtil.createDir(serverDirectory);
        }
        try {
            this.mServer = new TFTPServer(serverDirectory, serverDirectory, 9527, InetAddress.getByName(mServerIP), TFTPServer.ServerMode.GET_AND_PUT, (PrintStream) null, (PrintStream) null);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void startServer() {
        log("startServer");
        if (this.mServer == null) {
            return;
        }
        try {
            if (!this.mServer.isRunning()) {
                this.mServer.run();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void stopServer() {
        log("stopServer");
        if (this.mServer != null) {
            this.mServer.shutdown();
            this.mServer = null;
        }
    }

    @Override // android.app.Service
    public void onDestroy() {
        log("onDestroy");
        stopServer();
        super.onDestroy();
    }

    private void log(String tag) {
        Log.d(PeanutConstants.TAG_TFTP, "[TFTPService][" + tag + "]");
    }

    private void log(String tag, String content) {
        Log.d(PeanutConstants.TAG_TFTP, "[TFTPService][" + tag + "][" + content + "]");
    }
}
