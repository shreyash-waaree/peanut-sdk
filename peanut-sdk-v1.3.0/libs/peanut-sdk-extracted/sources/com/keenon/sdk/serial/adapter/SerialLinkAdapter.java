package com.keenon.sdk.serial.adapter;

import com.keenon.common.constant.PeanutConstants;
import com.keenon.common.utils.ByteUtils;
import com.keenon.common.utils.LogUtils;
import com.keenon.sdk.hedera.base.ILinkAdapter;
import com.keenon.sdk.hedera.base.iHedera;
import com.keenon.sdk.hedera.model.ICallback;
import com.keenon.sdk.hedera.model.RequestEnum;
import com.keenon.sdk.serial.HederaClient;
import com.keenon.sdk.serial.HederaClientFactory;
import com.keenon.sdk.serial.base.SerialCommandClient;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/serial/adapter/SerialLinkAdapter.class */
public class SerialLinkAdapter implements ILinkAdapter {
    private static final String TAG = "[SerialLinkAdapter]";
    private Byte[] requestParams;
    private RequestEnum requestEnum;
    private String action;
    private String body;
    private String status;
    private String tag;
    private ICallback callBack;
    private HederaClient client;
    private String serialPath;

    public SerialLinkAdapter(String serialPath) {
        init(serialPath);
    }

    public void init(String serialPath) {
        LogUtils.d(PeanutConstants.TAG_SDK, "[SerialLinkAdapter][init][serialPath: " + serialPath + "]");
        this.serialPath = serialPath;
        this.client = HederaClientFactory.getInstance().getClient(serialPath, SerialCommandClient.getInstance().getFrameDetector());
        this.client.open();
        this.client.setDataExchangedListener(new iHedera.OnDataExchangedListener() { // from class: com.keenon.sdk.serial.adapter.SerialLinkAdapter.1
            @Override // com.keenon.sdk.hedera.base.iHedera.OnDataExchangedListener
            public void onCmdDataSent(Byte[] data, int size) {
            }

            @Override // com.keenon.sdk.hedera.base.iHedera.OnDataExchangedListener
            public void onCmdDataReceived(Byte[] data, int size) {
            }

            @Override // com.keenon.sdk.hedera.base.iHedera.onFrameDataExchangedListener
            public void onFrameDataCrcErrorDetected(Byte[] data, int size) {
                LogUtils.d(PeanutConstants.TAG_SDK, "[SerialLinkAdapter]Crc Error     :" + ByteUtils.toString(data));
            }

            @Override // com.keenon.sdk.hedera.base.iHedera.onFrameDataExchangedListener
            public void onFrameDataSent(Byte[] data, int size) {
                LogUtils.v(PeanutConstants.TAG_SDK, "[SerialLinkAdapter]Frame Sent    :" + ByteUtils.toString(data));
            }

            @Override // com.keenon.sdk.hedera.base.iHedera.onFrameDataExchangedListener
            public void onFrameDataReceived(Byte[] data, int size) {
                LogUtils.v(PeanutConstants.TAG_SDK, "[SerialLinkAdapter]Frame Received:" + ByteUtils.toString(data));
            }

            @Override // com.keenon.sdk.hedera.base.iHedera.OnRawDataExchangedListener
            public void onRawDataSent(Byte[] data, int size) {
                LogUtils.d(PeanutConstants.TAG_SDK, "[SerialLinkAdapter]Raw Sent      :" + ByteUtils.toString(data));
            }

            @Override // com.keenon.sdk.hedera.base.iHedera.OnRawDataExchangedListener
            public void onRawDataReceived(Byte[] data, int size) {
                LogUtils.d(PeanutConstants.TAG_SDK, "[SerialLinkAdapter]Raw Received  :" + ByteUtils.toString(data));
            }
        });
    }

    public void setRequestParams(Byte[] requestParams) {
        this.requestParams = requestParams;
    }

    public void setRequest(SerialCommand command) {
        this.action = command.action();
        this.body = command.body();
        this.status = command.status();
        this.tag = this.action + this.body;
    }

    private boolean isInvalidCommand() {
        return this.action == null || this.body == null;
    }

    public void setRequestEnum(RequestEnum requestEnum) {
        this.requestEnum = requestEnum;
    }

    public void setCallBack(ICallback callBack) {
        this.callBack = callBack;
    }

    private com.keenon.sdk.serial.base.SerialCommand buildCommand() {
        com.keenon.sdk.serial.base.SerialCommand command = com.keenon.sdk.serial.base.SerialCommand.create(ByteUtils.hexStrToByte(this.action), ByteUtils.hexStrToByte(this.body));
        command.setStatus(ByteUtils.hexStrToByte(this.status));
        if (this.requestParams != null && this.requestParams.length > 0) {
            command.setData(this.requestParams);
        }
        return command;
    }

    @Override // com.keenon.sdk.hedera.base.ILinkAdapter
    public void reset() {
        this.requestParams = null;
        this.requestEnum = null;
        this.action = null;
        this.body = null;
        this.tag = null;
        this.callBack = null;
    }

    @Override // com.keenon.sdk.hedera.base.ILinkAdapter
    public void release() {
        LogUtils.i(PeanutConstants.TAG_SDK, "[SerialLinkAdapter][release]");
        reset();
        SerialCommandClient.getInstance().release();
        if (this.client != null) {
            this.client.close();
        }
    }

    private void cancelSubscribeByTag() {
        if (this.tag != null && !this.tag.isEmpty()) {
            LogUtils.d(PeanutConstants.TAG_SDK, "[SerialLinkAdapter][cancelSubscribeByTag][" + this.tag + "]");
            SerialCommandClient.getInstance().cancelSubscribe(this.tag);
        }
    }

    @Override // com.keenon.sdk.hedera.base.ILinkAdapter
    public void adapt(boolean cancel) {
        SerialCommandClient.getInstance().setClient(this.client);
        if (isInvalidCommand()) {
            LogUtils.w(PeanutConstants.TAG_SDK, "[SerialLinkAdapter][adapt][invalid]");
            return;
        }
        com.keenon.sdk.serial.base.SerialCommand command = buildCommand();
        if (cancel) {
            cancelSubscribeByTag();
        } else {
            logRequest();
            SerialCommandClient.getInstance().send(command, this.requestEnum, this.callBack);
        }
    }

    private void logRequest() {
        LogUtils.d(PeanutConstants.TAG_SDK, "[SerialLinkAdapter][adapt][action: " + this.action + ", body: " + this.body + ", size: " + (this.requestParams == null ? "0" : Integer.valueOf(this.requestParams.length)) + "]");
    }
}
