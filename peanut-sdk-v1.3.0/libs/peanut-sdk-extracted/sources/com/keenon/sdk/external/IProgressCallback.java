package com.keenon.sdk.external;

import org.eclipse.californium.core.coap.Request;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/external/IProgressCallback.class */
public interface IProgressCallback extends IDataCallback {
    void readyToSend(Request request);

    void progress(int i);
}
