package com.keenon.sdk.proxy.adapter;

import com.keenon.common.constant.PeanutConstants;
import com.keenon.common.utils.ByteUtils;
import com.keenon.common.utils.LogUtils;
import com.keenon.sdk.hedera.base.HederaFrameDetector;
import com.keenon.sdk.hedera.base.ILinkAdapter;
import com.keenon.sdk.hedera.base.iHedera;
import com.keenon.sdk.serial.HederaClient;
import com.keenon.sdk.serial.HederaClientFactory;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/proxy/adapter/Coap4SerialLinkAdapter.class */
public class Coap4SerialLinkAdapter extends CoapLinkAdapter implements ILinkAdapter {
    private static final String TAG = "[Coap4SerialLinkAdapter]";
    protected HederaFrameDetector frameDetector = new HederaFrameDetector() { // from class: com.keenon.sdk.proxy.adapter.Coap4SerialLinkAdapter.1
        @Override // com.keenon.sdk.hedera.base.HederaFrameDetector
        public void onFrameDetected(Byte[] data, int size) {
            LogUtils.d(PeanutConstants.TAG_SDK, "[Coap4SerialLinkAdapter][onFrameDetected][size: " + size + " Bytes: ][" + ByteUtils.toString(data) + "]");
            LogUtils.s(PeanutConstants.TAG_SDK, "[Coap4SerialLinkAdapter][size: " + size + " Bytes: ][" + ByteUtils.toString(data) + "]");
            HederaUdpServer.getInstance().sendResponse(data, size);
            PingTask.getInstance().active();
        }
    };
    private HederaClient client;

    public Coap4SerialLinkAdapter(String serialPath) {
        init(serialPath);
    }

    public void init(String serialPath) {
        LogUtils.i(PeanutConstants.TAG_SDK, "[Coap4SerialLinkAdapter][init][serialPath: " + serialPath + "]");
        this.client = HederaClientFactory.getInstance().getClient(serialPath, this.frameDetector);
        HederaUdpServer.getInstance().init(this.client.getTransportProtocol());
        this.client.open();
        this.client.setDataExchangedListener(new iHedera.OnDataExchangedListener() { // from class: com.keenon.sdk.proxy.adapter.Coap4SerialLinkAdapter.2
            @Override // com.keenon.sdk.hedera.base.iHedera.OnDataExchangedListener
            public void onCmdDataSent(Byte[] data, int size) {
                LogUtils.d(PeanutConstants.TAG_SDK, "[Coap4SerialLinkAdapter][onCmdDataSent][size: " + size + "][data: " + ByteUtils.toString(data) + "]");
            }

            @Override // com.keenon.sdk.hedera.base.iHedera.OnDataExchangedListener
            public void onCmdDataReceived(Byte[] data, int size) {
                LogUtils.d(PeanutConstants.TAG_SDK, "[Coap4SerialLinkAdapter][onCmdDataReceived][size: " + size + "][data: " + ByteUtils.toString(data) + "]");
            }

            @Override // com.keenon.sdk.hedera.base.iHedera.onFrameDataExchangedListener
            public void onFrameDataCrcErrorDetected(Byte[] data, int size) {
                LogUtils.d(PeanutConstants.TAG_SDK, "[Coap4SerialLinkAdapter][onFrameDataCrcErrorDetected][size: " + size + "][data: " + ByteUtils.toString(data) + "]");
            }

            @Override // com.keenon.sdk.hedera.base.iHedera.onFrameDataExchangedListener
            public void onFrameDataSent(Byte[] data, int size) {
                LogUtils.v(PeanutConstants.TAG_SDK, "[Coap4SerialLinkAdapter][onFrameDataSent][size: " + size + "][data: " + ByteUtils.toString(data) + "]");
            }

            @Override // com.keenon.sdk.hedera.base.iHedera.onFrameDataExchangedListener
            public void onFrameDataReceived(Byte[] data, int size) {
                LogUtils.v(PeanutConstants.TAG_SDK, "[Coap4SerialLinkAdapter][onFrameDataReceived][size: " + size + "][data: " + ByteUtils.toString(data) + "]");
            }

            @Override // com.keenon.sdk.hedera.base.iHedera.OnRawDataExchangedListener
            public void onRawDataSent(Byte[] data, int size) {
                LogUtils.d(PeanutConstants.TAG_SDK, "[Coap4SerialLinkAdapter][onRawDataSent][size: " + size + "][data: " + ByteUtils.toString(data) + "]");
            }

            @Override // com.keenon.sdk.hedera.base.iHedera.OnRawDataExchangedListener
            public void onRawDataReceived(Byte[] data, int size) {
                LogUtils.v(PeanutConstants.TAG_SDK, "[Coap4SerialLinkAdapter][onRawDataReceived][size: " + size + "][data: " + ByteUtils.toString(data) + "]");
            }
        });
    }

    @Override // com.keenon.sdk.proxy.adapter.CoapLinkAdapter, com.keenon.sdk.hedera.base.ILinkAdapter
    public void reset() {
        super.reset();
    }

    @Override // com.keenon.sdk.proxy.adapter.CoapLinkAdapter, com.keenon.sdk.hedera.base.ILinkAdapter
    public void release() {
        LogUtils.i(PeanutConstants.TAG_SDK, "[Coap4SerialLinkAdapter][release]");
        super.release();
        if (this.client != null) {
            this.client.close();
        }
        PingTask.getInstance().stop();
        HederaUdpServer.getInstance().close();
        HederaClientFactory.getInstance().release();
    }

    @Override // com.keenon.sdk.proxy.adapter.CoapLinkAdapter, com.keenon.sdk.hedera.base.ILinkAdapter
    public void adapt(boolean cancel) {
        LogUtils.v(PeanutConstants.TAG_SDK, "[Coap4SerialLinkAdapter][adapt]");
        super.adapt(cancel);
    }
}
