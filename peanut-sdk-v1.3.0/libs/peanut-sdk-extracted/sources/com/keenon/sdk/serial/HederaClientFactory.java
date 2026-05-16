package com.keenon.sdk.serial;

import com.keenon.common.constant.PeanutConstants;
import com.keenon.common.utils.LogUtils;
import com.keenon.sdk.hedera.base.HederaFrameDetector;
import com.keenon.sdk.serial.HederaClient;
import com.keenon.sdk.serial.transport.HederaTransportHeadTailCRC;
import java.util.HashMap;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/serial/HederaClientFactory.class */
public class HederaClientFactory {
    private static HederaClientFactory instance = new HederaClientFactory();
    private static HashMap<String, HederaClient> hederaClientHashMap = new HashMap<>();

    private HederaClientFactory() {
    }

    public static HederaClientFactory getInstance() {
        return instance;
    }

    public HederaClient getClient(String serialPath, HederaFrameDetector frameDetector) {
        if (hederaClientHashMap.get(serialPath) == null) {
            synchronized (HederaClientFactory.class) {
                if (hederaClientHashMap.get(serialPath) == null) {
                    HederaClient.Builder builder = new HederaClient.Builder().setTransceiverType(1).setFrameDetector(frameDetector).setTransportProtocol(new HederaTransportHeadTailCRC()).setParameters(PeanutConstants.PARAMETERS_SERIAL_PORT, serialPath);
                    HederaClient client = builder.build();
                    hederaClientHashMap.put(serialPath, client);
                }
            }
        }
        return hederaClientHashMap.get(serialPath);
    }

    public void release() {
        LogUtils.i(PeanutConstants.TAG_SDK, "[HederaClientFactory][release]");
        hederaClientHashMap.clear();
    }
}
