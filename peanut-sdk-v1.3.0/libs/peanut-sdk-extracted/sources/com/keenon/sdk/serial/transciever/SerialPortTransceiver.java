package com.keenon.sdk.serial.transciever;

import com.keenon.common.constant.PeanutConstants;
import com.keenon.common.utils.ByteUtils;
import com.keenon.common.utils.LogUtils;
import com.keenon.common.utils.PeanutSdkPoolExecutor;
import com.keenon.sdk.hedera.base.HederaTransceiver;
import com.keenon.sdk.hedera.base.iHedera;
import com.keenon.sdk.serial.internal.SerialPort;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/serial/transciever/SerialPortTransceiver.class */
public class SerialPortTransceiver implements HederaTransceiver {
    private static final String TAG = "[SerialPortTransceiver]";
    private static final String TAG_THREAD = "SerialPortTransceiver#";
    private static HashMap<String, SerialPortTransceiver> instances = new HashMap<>();
    private Set<HederaTransceiver.OnReceiveListener> receiveListenerSet;
    private HederaTransceiver.OnReceiveListener receiveListener;
    private iHedera.OnRawDataExchangedListener rawDataExchangedListener;
    private String path;
    private int baudRate = PeanutConstants.DEFAULT_RATE;
    private SerialPort mSerialPort;
    private OutputStream mOutputStream;
    private InputStream mInputStream;
    private ReadThread mReadThread;
    private ExecutorService executor;

    public static SerialPortTransceiver getInstanceBySerialPort(String serial) {
        SerialPortTransceiver transceiver;
        if (instances.containsKey(serial)) {
            transceiver = instances.get(serial);
        } else {
            transceiver = new SerialPortTransceiver();
            transceiver.setPath(serial);
            if (PeanutConstants.COM3_USB0.equals(serial)) {
                transceiver.setBaudRate(PeanutConstants.COM3RATE);
            }
            instances.put(serial, transceiver);
        }
        return transceiver;
    }

    public String getPath() {
        return this.path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public int getBaudRate() {
        return this.baudRate;
    }

    public void setBaudRate(int baudRate) {
        this.baudRate = baudRate;
    }

    @Override // com.keenon.sdk.hedera.base.HederaTransceiver
    public int setOnReceiveListener(HederaTransceiver.OnReceiveListener listener) {
        if (this.receiveListenerSet == null) {
            this.receiveListenerSet = new HashSet();
        }
        this.receiveListenerSet.add(listener);
        return 0;
    }

    @Override // com.keenon.sdk.hedera.base.HederaTransceiver
    public void addReceiveListener(HederaTransceiver.OnReceiveListener listener) {
        if (this.receiveListenerSet == null) {
            this.receiveListenerSet = new HashSet();
        }
        this.receiveListenerSet.add(listener);
    }

    @Override // com.keenon.sdk.hedera.base.HederaTransceiver
    public void removeReceiveListener(HederaTransceiver.OnReceiveListener listener) {
        if (this.receiveListenerSet == null) {
            return;
        }
        this.receiveListenerSet.remove(listener);
    }

    @Override // com.keenon.sdk.hedera.base.HederaTransceiver
    public int open() {
        try {
            LogUtils.i(PeanutConstants.TAG_SDK, "[SerialPortTransceiver][open][dev = " + this.path + "]");
            if (this.mSerialPort == null || this.mOutputStream == null) {
                this.mSerialPort = new SerialPort(new File(this.path), this.baudRate, 0);
                this.executor = PeanutSdkPoolExecutor.getInstance(TAG_THREAD);
                this.mOutputStream = this.mSerialPort.getOutputStream();
                this.mInputStream = this.mSerialPort.getInputStream();
                if (this.mReadThread == null) {
                    this.mReadThread = new ReadThread(this);
                    this.mReadThread.start();
                }
            }
            return 0;
        } catch (IOException | SecurityException e) {
            LogUtils.e(PeanutConstants.TAG_SDK, TAG, e);
            return 0;
        }
    }

    @Override // com.keenon.sdk.hedera.base.HederaTransceiver
    public int send(final Byte[] data) {
        if (null == this.executor) {
            LogUtils.e(PeanutConstants.TAG_SDK, "[SerialPortTransceiver][send null executor]");
            return 0;
        }
        if (this.executor.isTerminated()) {
            LogUtils.e(PeanutConstants.TAG_SDK, "[SerialPortTransceiver][executor is terminated]");
            PeanutSdkPoolExecutor.release();
            this.executor = PeanutSdkPoolExecutor.getInstance(TAG_THREAD);
        }
        this.executor.execute(new Runnable() { // from class: com.keenon.sdk.serial.transciever.SerialPortTransceiver.1
            @Override // java.lang.Runnable
            public void run() {
                try {
                    if (SerialPortTransceiver.this.mOutputStream != null && data != null) {
                        byte[] data_bytes = new byte[data.length];
                        for (int i = 0; i < data.length; i++) {
                            data_bytes[i] = data[i].byteValue();
                        }
                        SerialPortTransceiver.this.mOutputStream.write(data_bytes);
                    } else {
                        LogUtils.e(PeanutConstants.TAG_SDK, "[SerialPortTransceiver][send null mOutputStream or data]");
                    }
                } catch (Exception e) {
                    LogUtils.e(PeanutConstants.TAG_SDK, SerialPortTransceiver.TAG, e);
                }
            }
        });
        if (this.rawDataExchangedListener != null) {
            this.rawDataExchangedListener.onRawDataSent(data, data.length);
            return 0;
        }
        return 0;
    }

    @Override // com.keenon.sdk.hedera.base.HederaTransceiver
    public void close() {
        LogUtils.i(PeanutConstants.TAG_SDK, "[SerialPortTransceiver][close]");
        if (this.mSerialPort != null) {
            this.mSerialPort.close();
            this.mSerialPort = null;
        }
        if (this.mReadThread != null) {
            this.mReadThread.interrupt();
            this.mReadThread = null;
        }
        try {
            if (this.mInputStream != null) {
                this.mInputStream.close();
            }
            if (this.mOutputStream != null) {
                this.mOutputStream.close();
            }
        } catch (IOException e) {
            LogUtils.e(PeanutConstants.TAG_SDK, TAG, (Exception) e);
        } finally {
            this.mInputStream = null;
            this.mOutputStream = null;
        }
    }

    @Override // com.keenon.sdk.hedera.base.HederaTransceiver
    public void setRawDataExchangedListener(iHedera.OnRawDataExchangedListener rawDataExchangedListener) {
        this.rawDataExchangedListener = rawDataExchangedListener;
    }

    /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/serial/transciever/SerialPortTransceiver$ReadThread.class */
    private class ReadThread extends Thread {
        SerialPortTransceiver transceiver;

        ReadThread(SerialPortTransceiver transceiver) {
            this.transceiver = transceiver;
        }

        @Override // java.lang.Thread, java.lang.Runnable
        public void run() {
            super.run();
            while (!isInterrupted()) {
                try {
                    if (SerialPortTransceiver.this.mInputStream != null) {
                        if (SerialPortTransceiver.this.mInputStream.available() > 0) {
                            byte[] buffer = new byte[1024];
                            int size = SerialPortTransceiver.this.mInputStream.read(buffer);
                            if (size > 0 && this.transceiver.receiveListenerSet != null && this.transceiver.receiveListenerSet.size() > 0) {
                                if (this.transceiver.rawDataExchangedListener != null) {
                                    this.transceiver.rawDataExchangedListener.onRawDataReceived(ByteUtils.ByteToObject(buffer, size), size);
                                }
                                for (HederaTransceiver.OnReceiveListener listener : this.transceiver.receiveListenerSet) {
                                    listener.onReceive(buffer, size);
                                }
                            }
                        } else {
                            Thread.sleep(30L);
                        }
                    } else {
                        LogUtils.e(PeanutConstants.TAG_SDK, "[SerialPortTransceiver][ReadThread null InputStream]");
                        return;
                    }
                } catch (IOException e) {
                    LogUtils.e(PeanutConstants.TAG_SDK, SerialPortTransceiver.TAG, (Exception) e);
                } catch (InterruptedException e2) {
                    LogUtils.e(PeanutConstants.TAG_SDK, SerialPortTransceiver.TAG, (Exception) e2);
                }
            }
        }
    }
}
