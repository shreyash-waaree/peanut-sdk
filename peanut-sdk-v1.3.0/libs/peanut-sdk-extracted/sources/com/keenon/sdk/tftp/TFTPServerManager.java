package com.keenon.sdk.tftp;

import android.util.Log;
import com.keenon.common.constant.PeanutConstants;
import com.keenon.common.utils.FileUtil;
import java.io.File;
import java.io.PrintStream;
import java.net.InetAddress;
import org.apache.commons.net.tftp.TFTPServer;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/tftp/TFTPServerManager.class */
public class TFTPServerManager {
    private static final String TAG = "TFTPServerManager";
    private static final int PORT = 9527;
    private static volatile TFTPServerManager sInstance;
    private String mServerPath;
    private String mServerIP;
    private int mPort;
    private TFTPServer mServer;

    private TFTPServerManager() {
    }

    public static TFTPServerManager getInstance() {
        if (sInstance == null) {
            synchronized (TFTPServerManager.class) {
                if (sInstance == null) {
                    sInstance = new TFTPServerManager();
                }
            }
        }
        return sInstance;
    }

    public boolean init(String directory) {
        return init(directory, null);
    }

    public boolean init(String directory, String ip) {
        return init(directory, ip, 0);
    }

    public boolean init(String directory, String ip, int port) {
        try {
            log("init");
            if (this.mServer != null && this.mServer.isRunning()) {
                log("already inited");
                return true;
            }
            this.mServerPath = directory;
            this.mServerIP = ip;
            this.mPort = port;
            if (ip == null || ip.length() == 0) {
                this.mServerIP = PeanutConstants.LOCAL_ETHER_IP;
            }
            if (directory == null || directory.length() == 0) {
                this.mServerPath = PeanutConstants.PATH_TFTP_SERVER;
            }
            if (port < 1024) {
                this.mPort = 9527;
            }
            log("server ip", this.mServerIP);
            log("server path", this.mServerPath);
            File serverDirectory = new File(this.mServerPath);
            if (!serverDirectory.exists()) {
                log("create server directory");
                FileUtil.createDir(serverDirectory);
            }
            this.mServer = new TFTPServer(serverDirectory, serverDirectory, this.mPort, InetAddress.getByName(this.mServerIP), TFTPServer.ServerMode.GET_AND_PUT, (PrintStream) null, (PrintStream) null);
            Thread.sleep(50L);
            return this.mServer != null && this.mServer.isRunning();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public String getUri() {
        return PeanutConstants.TFTP_PROTOCOL + this.mServerIP + ":9527";
    }

    public void stop() {
        log("stopServer");
        if (this.mServer != null) {
            this.mServer.shutdown();
            this.mServer = null;
        }
    }

    public boolean isRunning() {
        if (this.mServer != null) {
            try {
                return this.mServer.isRunning();
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }
        return false;
    }

    private void log(String tag) {
        Log.d(PeanutConstants.TAG_TFTP, "[TFTPServerManager][" + tag + "]");
    }

    private void log(String tag, String content) {
        Log.d(PeanutConstants.TAG_TFTP, "[TFTPServerManager][" + tag + "][" + content + "]");
    }
}
