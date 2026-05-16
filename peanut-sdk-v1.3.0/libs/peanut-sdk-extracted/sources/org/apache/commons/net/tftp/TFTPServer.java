package org.apache.commons.net.tftp;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/apache-common-net.jar:org/apache/commons/net/tftp/TFTPServer.class */
public class TFTPServer implements Runnable {
    private static final int DEFAULT_TFTP_PORT = 69;
    private final HashSet<TFTPTransfer> transfers_;
    private final int port_;
    private final InetAddress laddr_;
    private final ServerMode mode_;
    private volatile boolean shutdownServer;
    private TFTP serverTftp_;
    private File serverReadDirectory_;
    private File serverWriteDirectory_;
    private Exception serverException;
    private PrintStream log_;
    private PrintStream logError_;
    private int maxTimeoutRetries_;
    private int socketTimeout_;
    private Thread serverThread;
    private static final Logger LOGGER = LoggerFactory.getLogger(TFTPServer.class);
    public static boolean isLog = false;
    private static final PrintStream nullStream = new PrintStream(new OutputStream() { // from class: org.apache.commons.net.tftp.TFTPServer.1
        @Override // java.io.OutputStream
        public void write(byte[] b) throws IOException {
        }

        @Override // java.io.OutputStream
        public void write(int b) {
        }
    });

    /* JADX INFO: loaded from: peanut-sdk-release.aar:libs/apache-common-net.jar:org/apache/commons/net/tftp/TFTPServer$ServerMode.class */
    public enum ServerMode {
        GET_ONLY,
        PUT_ONLY,
        GET_AND_PUT
    }

    public TFTPServer(File serverReadDirectory, File serverWriteDirectory, int port, InetAddress localaddr, ServerMode mode, PrintStream log, PrintStream errorLog) throws IOException {
        this.transfers_ = new HashSet<>();
        this.maxTimeoutRetries_ = 50;
        this.port_ = port;
        this.mode_ = mode;
        this.laddr_ = localaddr;
        this.log_ = log == null ? nullStream : log;
        this.logError_ = errorLog == null ? nullStream : errorLog;
        launch(serverReadDirectory, serverWriteDirectory);
    }

    public TFTPServer(File serverReadDirectory, File serverWriteDirectory, int port, NetworkInterface localiface, ServerMode mode, PrintStream log, PrintStream errorLog) throws IOException {
        Enumeration<InetAddress> ifaddrs;
        this.transfers_ = new HashSet<>();
        this.maxTimeoutRetries_ = 50;
        this.mode_ = mode;
        this.port_ = port;
        InetAddress iaddr = null;
        if (localiface != null && (ifaddrs = localiface.getInetAddresses()) != null && ifaddrs.hasMoreElements()) {
            iaddr = ifaddrs.nextElement();
        }
        this.log_ = log == null ? nullStream : log;
        this.logError_ = errorLog == null ? nullStream : errorLog;
        this.laddr_ = iaddr;
        launch(serverReadDirectory, serverWriteDirectory);
    }

    public TFTPServer(File serverReadDirectory, File serverWriteDirectory, int port, ServerMode mode, PrintStream log, PrintStream errorLog) throws IOException {
        this.transfers_ = new HashSet<>();
        this.maxTimeoutRetries_ = 50;
        this.port_ = port;
        this.mode_ = mode;
        this.log_ = log == null ? nullStream : log;
        this.logError_ = errorLog == null ? nullStream : errorLog;
        this.laddr_ = null;
        launch(serverReadDirectory, serverWriteDirectory);
    }

    public TFTPServer(File serverReadDirectory, File serverWriteDirectory, ServerMode mode) throws IOException {
        this(serverReadDirectory, serverWriteDirectory, 69, mode, null, null);
    }

    protected void finalize() throws Throwable {
        shutdown();
    }

    public int getMaxTimeoutRetries() {
        return this.maxTimeoutRetries_;
    }

    public void setMaxTimeoutRetries(int retries) {
        if (retries < 0) {
            throw new RuntimeException("Invalid Value");
        }
        this.maxTimeoutRetries_ = retries;
    }

    public int getSocketTimeout() {
        return this.socketTimeout_;
    }

    public void setSocketTimeout(int timeout) {
        if (timeout < 10) {
            throw new RuntimeException("Invalid Value");
        }
        this.socketTimeout_ = timeout;
    }

    public boolean isRunning() throws Exception {
        if (!this.shutdownServer || this.serverException == null) {
            return !this.shutdownServer;
        }
        throw this.serverException;
    }

    private void launch(File serverReadDirectory, File serverWriteDirectory) throws IOException {
        log("Starting TFTP Server on port " + this.port_ + ".  Read directory: " + serverReadDirectory + " Write directory: " + serverWriteDirectory + " Server Mode is " + this.mode_);
        this.serverReadDirectory_ = serverReadDirectory.getCanonicalFile();
        if (!this.serverReadDirectory_.exists() || !serverReadDirectory.isDirectory()) {
            throw new IOException("The server read directory " + this.serverReadDirectory_ + " does not exist");
        }
        this.serverWriteDirectory_ = serverWriteDirectory.getCanonicalFile();
        if (!this.serverWriteDirectory_.exists() || !serverWriteDirectory.isDirectory()) {
            throw new IOException("The server write directory " + this.serverWriteDirectory_ + " does not exist");
        }
        this.serverTftp_ = new TFTP();
        this.socketTimeout_ = this.serverTftp_.getDefaultTimeout();
        this.serverTftp_.setDefaultTimeout(0);
        if (this.laddr_ != null) {
            this.serverTftp_.open(this.port_, this.laddr_);
        } else {
            this.serverTftp_.open(this.port_);
        }
        this.serverThread = new Thread(this);
        this.serverThread.setDaemon(true);
        this.serverThread.start();
    }

    TFTP newTFTP() {
        return new TFTP();
    }

    @Override // java.lang.Runnable
    public void run() {
        while (!this.shutdownServer) {
            try {
                try {
                    TFTPPacket tftpPacket = this.serverTftp_.receive();
                    TFTPTransfer tt = new TFTPTransfer(tftpPacket);
                    synchronized (this.transfers_) {
                        this.transfers_.add(tt);
                    }
                    Thread thread = new Thread(tt);
                    thread.setDaemon(true);
                    thread.start();
                } catch (Exception e) {
                    if (!this.shutdownServer) {
                        this.serverException = e;
                        this.logError_.println("Unexpected Error in TFTP Server - Server shut down! + " + e);
                    }
                    this.shutdownServer = true;
                    if (this.serverTftp_ != null && this.serverTftp_.isOpen()) {
                        this.serverTftp_.close();
                        return;
                    }
                    return;
                }
            } catch (Throwable th) {
                this.shutdownServer = true;
                if (this.serverTftp_ != null && this.serverTftp_.isOpen()) {
                    this.serverTftp_.close();
                }
                throw th;
            }
        }
        this.shutdownServer = true;
        if (this.serverTftp_ != null && this.serverTftp_.isOpen()) {
            this.serverTftp_.close();
        }
    }

    void sendData(TFTP tftp, TFTPPacket data) throws IOException {
        tftp.bufferedSend(data);
    }

    public void setLog(PrintStream log) {
        this.log_ = log;
    }

    public void setLogError(PrintStream logError) {
        this.logError_ = logError;
    }

    public void shutdown() {
        this.shutdownServer = true;
        synchronized (this.transfers_) {
            Iterator<TFTPTransfer> it = this.transfers_.iterator();
            while (it.hasNext()) {
                it.next().shutdown();
            }
        }
        try {
            this.serverTftp_.close();
        } catch (RuntimeException e) {
        }
        try {
            this.serverThread.join();
        } catch (InterruptedException e2) {
        }
    }

    /* JADX INFO: loaded from: peanut-sdk-release.aar:libs/apache-common-net.jar:org/apache/commons/net/tftp/TFTPServer$TFTPTransfer.class */
    private class TFTPTransfer implements Runnable {
        private final TFTPPacket tftpPacket_;
        TFTP transferTftp_;
        private boolean shutdownTransfer;

        public TFTPTransfer(TFTPPacket tftpPacket) {
            this.tftpPacket_ = tftpPacket;
        }

        private File buildSafeFile(File serverDirectory, String fileName, boolean createSubDirs) throws IOException {
            File temp = new File(serverDirectory, fileName).getCanonicalFile();
            if (!isSubdirectoryOf(serverDirectory, temp)) {
                throw new IOException("Cannot access files outside of tftp server root.");
            }
            if (createSubDirs) {
                createDirectory(temp.getParentFile());
            }
            return temp;
        }

        private void createDirectory(File file) throws IOException {
            File parent = file.getParentFile();
            if (parent == null) {
                throw new IOException("Unexpected error creating requested directory");
            }
            if (!parent.exists()) {
                createDirectory(parent);
            }
            if (!parent.isDirectory()) {
                throw new IOException("Invalid directory path - file in the way of requested folder");
            }
            if (file.isDirectory()) {
                return;
            }
            boolean result = file.mkdir();
            if (!result) {
                throw new IOException("Couldn't create requested directory");
            }
        }

        /* JADX WARN: Removed duplicated region for block: B:105:0x0430 A[EXC_TOP_SPLITTER, SYNTHETIC] */
        /*
            Code decompiled incorrectly, please refer to instructions dump.
            To view partially-correct code enable 'Show inconsistent code' option in preferences
        */
        private void handleRead(org.apache.commons.net.tftp.TFTPReadRequestPacket r11) throws org.apache.commons.net.tftp.TFTPPacketException, java.io.IOException {
            /*
                Method dump skipped, instruction units count: 1102
                To view this dump change 'Code comments level' option to 'DEBUG'
            */
            throw new UnsupportedOperationException("Method not decompiled: org.apache.commons.net.tftp.TFTPServer.TFTPTransfer.handleRead(org.apache.commons.net.tftp.TFTPReadRequestPacket):void");
        }

        /* JADX WARN: Code restructure failed: missing block: B:53:0x019a, code lost:
        
            if (r8.shutdownTransfer != false) goto L81;
         */
        /* JADX WARN: Code restructure failed: missing block: B:54:0x019d, code lost:
        
            r8.this$0.logError_.println("Unexpected response from tftp client during transfer (" + r14 + ").  Transfer aborted.");
         */
        /*
            Code decompiled incorrectly, please refer to instructions dump.
            To view partially-correct code enable 'Show inconsistent code' option in preferences
        */
        private void handleWrite(org.apache.commons.net.tftp.TFTPWriteRequestPacket r9) throws org.apache.commons.net.tftp.TFTPPacketException, java.io.IOException {
            /*
                Method dump skipped, instruction units count: 704
                To view this dump change 'Code comments level' option to 'DEBUG'
            */
            throw new UnsupportedOperationException("Method not decompiled: org.apache.commons.net.tftp.TFTPServer.TFTPTransfer.handleWrite(org.apache.commons.net.tftp.TFTPWriteRequestPacket):void");
        }

        private boolean isSubdirectoryOf(File parent, File child) {
            File childsParent = child.getParentFile();
            if (childsParent == null) {
                return false;
            }
            if (childsParent.equals(parent)) {
                return true;
            }
            return isSubdirectoryOf(parent, childsParent);
        }

        @Override // java.lang.Runnable
        public void run() {
            try {
                try {
                    this.transferTftp_ = TFTPServer.this.newTFTP();
                    this.transferTftp_.beginBufferedOps();
                    this.transferTftp_.setDefaultTimeout(TFTPServer.this.socketTimeout_);
                    this.transferTftp_.open();
                    if (this.tftpPacket_ instanceof TFTPReadRequestPacket) {
                        handleRead((TFTPReadRequestPacket) this.tftpPacket_);
                    } else if (!(this.tftpPacket_ instanceof TFTPWriteRequestPacket)) {
                        TFTPServer.this.log_.println("Unsupported TFTP request (" + this.tftpPacket_ + ") - ignored.");
                    } else {
                        handleWrite((TFTPWriteRequestPacket) this.tftpPacket_);
                    }
                    try {
                        if (this.transferTftp_ != null && this.transferTftp_.isOpen()) {
                            this.transferTftp_.endBufferedOps();
                            this.transferTftp_.close();
                        }
                    } catch (Exception e) {
                    }
                    synchronized (TFTPServer.this.transfers_) {
                        TFTPServer.this.transfers_.remove(this);
                    }
                } catch (Exception e2) {
                    if (!this.shutdownTransfer) {
                        TFTPServer.this.logError_.println("Unexpected Error in during TFTP file transfer.  Transfer aborted. " + e2);
                    }
                    try {
                        if (this.transferTftp_ != null && this.transferTftp_.isOpen()) {
                            this.transferTftp_.endBufferedOps();
                            this.transferTftp_.close();
                        }
                    } catch (Exception e3) {
                    }
                    synchronized (TFTPServer.this.transfers_) {
                        TFTPServer.this.transfers_.remove(this);
                    }
                }
            } catch (Throwable th) {
                try {
                    if (this.transferTftp_ != null && this.transferTftp_.isOpen()) {
                        this.transferTftp_.endBufferedOps();
                        this.transferTftp_.close();
                    }
                } catch (Exception e4) {
                }
                synchronized (TFTPServer.this.transfers_) {
                    TFTPServer.this.transfers_.remove(this);
                    throw th;
                }
            }
        }

        public void shutdown() {
            this.shutdownTransfer = true;
            try {
                this.transferTftp_.close();
            } catch (RuntimeException e) {
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void log(String msg) {
        if (isLog) {
            System.out.println("TFTP: " + msg);
        }
    }
}
