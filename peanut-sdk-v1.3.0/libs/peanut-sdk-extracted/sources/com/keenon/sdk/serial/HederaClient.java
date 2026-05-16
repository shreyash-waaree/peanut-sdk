package com.keenon.sdk.serial;

import com.keenon.common.constant.PeanutConstants;
import com.keenon.common.utils.LogUtils;
import com.keenon.sdk.hedera.base.HederaFrameDetector;
import com.keenon.sdk.hedera.base.HederaTransceiver;
import com.keenon.sdk.hedera.base.HederaTransportProtocol;
import com.keenon.sdk.hedera.base.iHedera;
import com.keenon.sdk.serial.transciever.SerialPortTransceiver;
import com.keenon.sdk.serial.transport.HederaTransportHeadTailCRC;
import java.util.HashMap;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/serial/HederaClient.class */
public class HederaClient implements iHedera {
    private static final String TAG = "[HederaClient]";
    public HashMap<String, String> parameters;
    public HederaTransportProtocol transportProtocol;
    public HederaTransceiver transceiver;
    public iHedera.OnDataExchangedListener listener;
    protected HederaFrameDetector frameDetector;
    private int transceiverType;
    private HederaTransceiver.OnReceiveListener receiveListener = new HederaTransceiver.OnReceiveListener() { // from class: com.keenon.sdk.serial.HederaClient.1
        @Override // com.keenon.sdk.hedera.base.HederaTransceiver.OnReceiveListener
        public void onReceive(byte[] buffer, int size) {
            HederaClient.this.getTransportProtocol().receiveData(buffer, size);
        }
    };
    private boolean isOpened = false;

    HederaClient(String serialPath, HashMap<String, String> parameters, HederaTransportProtocol transportProtocol, int transceiverType, HederaTransceiver transceiver, HederaFrameDetector frameDetector) {
        this.transportProtocol = new HederaTransportHeadTailCRC();
        this.transceiverType = transceiverType;
        this.parameters = parameters;
        switch (this.transceiverType) {
            case 1:
                SerialPortTransceiver serial = SerialPortTransceiver.getInstanceBySerialPort(serialPath);
                serial.setOnReceiveListener(this.receiveListener);
                this.transceiver = serial;
                break;
        }
        this.frameDetector = frameDetector;
        this.transportProtocol = transportProtocol;
        this.parameters = parameters;
        this.transportProtocol.setFrameDetector(this.frameDetector);
        this.transportProtocol.setTransceiver(this.transceiver);
        this.transportProtocol.setFrameDataExchangedListener(this.listener);
    }

    public HederaTransportProtocol getTransportProtocol() {
        return this.transportProtocol;
    }

    public iHedera.OnDataExchangedListener getDataExchangeListener() {
        return this.listener;
    }

    public void setDataExchangedListener(iHedera.OnDataExchangedListener listener) {
        this.listener = listener;
        if (this.transportProtocol != null) {
            this.transportProtocol.setFrameDataExchangedListener(this.listener);
        }
    }

    @Override // com.keenon.sdk.hedera.base.iHedera
    public void open() {
        if (this.isOpened) {
            LogUtils.i(PeanutConstants.TAG_SDK, "[HederaClient][already open]");
            return;
        }
        LogUtils.i(PeanutConstants.TAG_SDK, "[HederaClient][open]");
        this.transceiver.open();
        this.isOpened = true;
    }

    @Override // com.keenon.sdk.hedera.base.iHedera
    public void close() {
        LogUtils.i(PeanutConstants.TAG_SDK, "[HederaClient][close]");
        this.transceiver.removeReceiveListener(this.receiveListener);
        this.transceiver.close();
        this.isOpened = false;
        this.transportProtocol = null;
    }

    /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/serial/HederaClient$Builder.class */
    public static class Builder {
        protected HashMap<String, String> parameters;
        protected HederaTransportProtocol transportProtocol;
        protected HederaTransceiver transceiver;
        protected HederaFrameDetector frameDetector;
        private int transceiverType;
        private String serialPath;

        public Builder() {
            this(PeanutConstants.COM1);
        }

        Builder(String serialPath) {
            this.serialPath = serialPath;
        }

        public Builder setTransceiverType(int transceiverType) {
            this.transceiverType = transceiverType;
            return this;
        }

        public Builder setParameters(String parameter, String value) {
            if (this.parameters == null) {
                this.parameters = new HashMap<>();
            }
            this.parameters.put(parameter, value);
            return this;
        }

        public String getParameters(String parameter) {
            if (this.parameters == null) {
                return null;
            }
            return this.parameters.get(parameter);
        }

        public HederaTransportProtocol getTransportProtocol() {
            return this.transportProtocol;
        }

        public Builder setTransportProtocol(HederaTransportProtocol transportProtocol) {
            this.transportProtocol = transportProtocol;
            return this;
        }

        public Builder setTransceiver(HederaTransceiver transceiver) {
            this.transceiver = transceiver;
            return this;
        }

        public HederaFrameDetector getFrameDetector() {
            return this.frameDetector;
        }

        public Builder setFrameDetector(HederaFrameDetector frameDetector) {
            this.frameDetector = frameDetector;
            return this;
        }

        public HederaClient build() {
            LogUtils.i(PeanutConstants.TAG_SDK, "[HederaClient][build]");
            String path = "";
            switch (this.transceiverType) {
                case 1:
                    path = getParameters(PeanutConstants.PARAMETERS_SERIAL_PORT);
                    if (path == null || path.equals("")) {
                        path = this.serialPath;
                    }
                    break;
            }
            return new HederaClient(path, this.parameters, this.transportProtocol, this.transceiverType, this.transceiver, this.frameDetector);
        }
    }
}
