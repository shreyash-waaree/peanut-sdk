package com.keenon.sdk.serial.base;

import com.keenon.common.constant.PeanutConstants;
import com.keenon.common.utils.LogUtils;
import com.keenon.sdk.hedera.base.HederaFrameDetector;
import com.keenon.sdk.hedera.model.ICallback;
import com.keenon.sdk.hedera.model.RequestEnum;
import com.keenon.sdk.serial.HederaClient;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/serial/base/SerialCommandClient.class */
public class SerialCommandClient {
    public static final String TAG = "[SerialCommandClient]";
    private static final int INIT_THREAD_COUNT = Runtime.getRuntime().availableProcessors() + 1;
    private static SerialCommandClient mInstance;
    private HederaClient client;
    private Map<String, ICallback> callbackMap = new HashMap();
    private Map<String, ICallback> reportMap = new HashMap();
    protected HederaFrameDetector frameDetector = new HederaFrameDetector() { // from class: com.keenon.sdk.serial.base.SerialCommandClient.1
        @Override // com.keenon.sdk.hedera.base.HederaFrameDetector
        public void onFrameDetected(Byte[] data, int size) {
            ICallback callback;
            SerialCommand serialCommand = SerialCommand.create(data, size);
            String id = "";
            if (null != SerialCommandClient.this.client && null != SerialCommandClient.this.client.getDataExchangeListener()) {
                SerialCommandClient.this.client.getDataExchangeListener().onCmdDataReceived(data, size);
            }
            if (serialCommand == null) {
                return;
            }
            if (serialCommand.isReportCommand()) {
                for (String key : SerialCommandClient.this.reportMap.keySet()) {
                    if (serialCommand.equalsCommandIncludeReport(key)) {
                        id = serialCommand.getCommandId();
                    }
                }
                callback = (ICallback) SerialCommandClient.this.reportMap.get(id);
            } else {
                for (String key2 : SerialCommandClient.this.callbackMap.keySet()) {
                    if (serialCommand.equalsCommandIncludeReport(key2)) {
                        id = serialCommand.getCommandId();
                    }
                }
                callback = (ICallback) SerialCommandClient.this.callbackMap.get(id);
            }
            if (null == callback) {
                return;
            }
            final SerialData result = new SerialData();
            result.setStatus(serialCommand.getStatus());
            result.setData(serialCommand.getData());
            final ICallback iCallback = callback;
            SerialCommandClient.this.execute(new Runnable() { // from class: com.keenon.sdk.serial.base.SerialCommandClient.1.1
                @Override // java.lang.Runnable
                public void run() {
                    try {
                        iCallback.onSuccess(result);
                    } catch (Exception e) {
                        LogUtils.e(PeanutConstants.TAG_SDK, SerialCommandClient.TAG, e);
                    }
                }
            });
        }
    };
    private ExecutorService mExecutor = Executors.newSingleThreadExecutor();

    private SerialCommandClient() {
    }

    public static SerialCommandClient getInstance() {
        if (mInstance == null) {
            synchronized (SerialCommandClient.class) {
                if (mInstance == null) {
                    mInstance = new SerialCommandClient();
                }
            }
        }
        return mInstance;
    }

    public HederaFrameDetector getFrameDetector() {
        return this.frameDetector;
    }

    protected void execute(Runnable job) {
        ExecutorService executor;
        synchronized (this) {
            executor = this.mExecutor;
        }
        if (executor == null) {
            job.run();
            return;
        }
        try {
            executor.execute(job);
        } catch (RejectedExecutionException e) {
            if (!executor.isShutdown()) {
                LogUtils.e(PeanutConstants.TAG_SDK, "[SerialCommandClient][execute][failed to execute job!]");
            }
        }
    }

    public void send(SerialCommand command, RequestEnum requestEnum, ICallback callBack) {
        if (null != this.client && null != this.client.transceiver && null != this.client.transportProtocol) {
            Byte[] data = command.toBytes();
            if (this.client.listener != null) {
                this.client.listener.onCmdDataSent(data, data.length);
            }
            this.client.transportProtocol.sendData(data);
        }
        if (requestEnum == RequestEnum.OBSERVER) {
            this.reportMap.put(command.getCommandId(), callBack);
            return;
        }
        if (command.getCommandId().equals("7c20")) {
            this.callbackMap.put("7c21", callBack);
        }
        if (command.getCommandId().equals("2021")) {
            this.callbackMap.put("2020", callBack);
        } else {
            this.callbackMap.put(command.getCommandId(), callBack);
        }
    }

    public void setClient(HederaClient client) {
        this.client = client;
    }

    public void cancelSubscribe(String tag) {
        if (this.reportMap.get(tag) != null) {
            this.reportMap.remove(tag);
        }
    }

    public void removeAllReportCallBack() {
        this.reportMap.clear();
    }

    public void removeAllCallBack() {
        this.callbackMap.clear();
    }

    public void release() {
        LogUtils.i(PeanutConstants.TAG_SDK, "[SerialCommandClient][release]");
        removeAllCallBack();
        removeAllReportCallBack();
    }
}
